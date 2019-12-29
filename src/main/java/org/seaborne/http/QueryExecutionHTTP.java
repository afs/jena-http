/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.http;

import static org.seaborne.http.HttpLib.bodyInputStreamToString;
import static org.seaborne.http.HttpLib.dft;
import static org.seaborne.http.HttpLib.execute;

import java.io.ByteArrayInputStream ;
import java.io.IOException;
import java.io.InputStream ;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit ;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.io.IO ;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Pair ;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Triple ;
import org.apache.jena.query.* ;
import org.apache.jena.rdf.model.Model ;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.ARQException ;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.ResultSetCheckCondition ;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.sparql.engine.http.Service;
import org.apache.jena.sparql.graph.GraphFactory ;
import org.apache.jena.sparql.resultset.ResultSetException;
import org.apache.jena.sparql.util.Context ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

/**
 * A query execution implementation where queries are executed against a remote
 * service over HTTP.
 */
public class QueryExecutionHTTP implements QueryExecution {
    private static Logger log = LoggerFactory.getLogger(QueryExecutionHTTP.class);

    public static final String QUERY_MIME_TYPE = WebContent.contentTypeSPARQLQuery;
    private final Query query;
    private final String queryString;
    private final String service;
    private final Context context;

    // Params
    private Params params = null;

    private SendMode sendMode;
    private int urlLimit = HttpEnv.urlLimit;

    // Protocol
    private List<String> defaultGraphURIs = new ArrayList<>();
    private List<String> namedGraphURIs = new ArrayList<>();

    private boolean closed = false;

    // Timeouts
    private long connectTimeout = -1;
    private TimeUnit connectTimeoutUnit = TimeUnit.MILLISECONDS;
    private long readTimeout = -1;
    private TimeUnit readTimeoutUnit = TimeUnit.MILLISECONDS;

    // Compression Support
    private boolean allowCompression = false;

    // Content Types: these list the standard formats and also include */*.
    // XXX Rename.
    private String selectContentType    = WebContent2.sparqlResultsHeader;
    private String askContentType       = WebContent2.sparqlAskHeader;
    private String describeContentType  = WebContent.defaultGraphAcceptHeader;
    private String constructContentType = WebContent.defaultGraphAcceptHeader;
    private String datasetContentType   = WebContent.defaultDatasetAcceptHeader;
    // CONSTRUCT or DESCRIBE
    private String modelContentType     = WebContent.defaultGraphAcceptHeader;

    // If this is non-null, it overrides the use of any Content-Type above.
    private String acceptHeader         = null;

    // Received content type
    private String httpResponseContentType = null ;
    // Releasing HTTP input streams is important. We remember this for SELECT,
    // and will close when the execution is closed
    private InputStream retainedConnection = null;

    private HttpClient httpClient = HttpEnv.getDftHttpClient();

    private Map<String, String> httpHeaders;




//    public QueryExecutionHTTP(String serviceURI, Query query) {
//        this(serviceURI, query, null, null);
//    }
//
//    public QueryExecutionHTTP(String serviceURI, Query query, HttpClient client) {
//        this(serviceURI, query, null, client);
//    }
//
//    public QueryExecutionHTTP(String serviceURI, String queryString) {
//        this(serviceURI, null, queryString, null);
//    }
//
//    public QueryExecutionHTTP(String serviceURI, String queryString, HttpClient client) {
//        this(serviceURI, null, queryString, client);
//    }
//
//    private QueryExecutionHTTP(String serviceURI, Query query, String queryString, HttpClient client) {
//        this.query = query;
//        this.queryString = queryString;
//        this.service = serviceURI;
//        this.context = ARQ.getContext().copy();
//
//        // Apply service configuration if relevant
//        applyServiceConfig(serviceURI, this);
//
//        // Don't want to overwrite client config we may have picked up from
//        // service context in the parent constructor if the specified
//        // client is null
//        this.httpClient = dft(client, HttpEnv.getDftHttpClient());
//    }

    public enum SendMode { asGetWithLimit, asGetAlways, asPostForm, asPostBody }

    public static Builder newBuilder() { return new Builder(); }
    public static class Builder {
        private String serviceURL = null;
        private Query query = null;
        private String queryString = null;
        private HttpClient httpClient = HttpEnv.getDftHttpClient();
        private Params params = new Params();
        // Accept: Handled as special case because the defaults varies by query type.
        private String acceptHeader;
        private boolean allowCompression;
        private Map<String, String> httpHeaders = new HashMap<>();
        private long timeout = -1;
        private TimeUnit timeoutUnit = null;

        private int urlLimit = -1;
        private SendMode sendMode = SendMode.asGetWithLimit;

        // XXX default and named graph URIs
        // defaultGraphURIs.
        // namedGraphURIs
        // (initial bindings)
        // ----
        // Special: content-type.
        // No - one setting.
        // selectContentType(String)
        // askContentType(String)
        // modelContentType
        // datasetContentType


        /** Set the URL of the query endpoint. */
        public Builder service(String serviceURL) {
            this.serviceURL = Objects.requireNonNull(serviceURL);
            return this;
        }

        /** Set the query - this also sets the query string to agree with the query argument. */
        public Builder query(Query query) {
            this.query = query;
            this.queryString = query.toString();
            return this;
        }

        /** Set the query string - this also clears any Query already set. */
        public Builder queryString(String queryString) {
            this.query = null;
            this.queryString = Objects.requireNonNull(queryString);
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient);
            return this;
        }

        /**
         * Send the query using HTTP POST with HTML form-encoded data.
         * If set false, the URL limit still applies.
         */
        public Builder sendHtmlForm(boolean htmlForm) {
            this.sendMode =  htmlForm ? SendMode.asPostForm : SendMode.asGetWithLimit;
            return this;
        }

        /**
         * Send the query using HTTP GET and the HTTP URL query string regardless of length.
         * By default, queries with a log URL are sent in an HTTP form with POST.
         * @see #urlGetLimit
         */

        public Builder useGet(boolean postForm) {
            this.sendMode = SendMode.asGetAlways;
            return this;
        }

        /**
         * Send the query request using POST with a Content-Type of as a
         * "application/sparql-query"
         */
        public Builder postQuery(boolean post) {
            this.sendMode = SendMode.asPostBody;
            return this;
        }

        /**
         * Maximum length for a GET request URL, this includes the length of the
         * service endpoint URL - longer than this and the request will use
         * POST/Form.
         * <p>
         * Long URLs can be silently truncated by intermediate systems and proxies.
         * Use of the URL query string means that request are not cached.
         * <p>
         * See also {@link #postQuery} to send the request using HTTP POST with the
         * query in the POST body using {@code Content-Type} "application/sparql-query"
         * <p>
         * See also {@link #sendHtmlForm(boolean)} to send a request as an HTML form.
         */
        public Builder urlGetLimit(int urlLimit) {
            this.urlLimit = urlLimit;
            return this;
        }

        public Builder param(String name) {
            Objects.requireNonNull(name);
            this.params.addParam(name);
            return this;
        }

        public Builder param(String name, String value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
            this.params.addParam(name, value);
            return this;
        }

        public Builder acceptHeader(String acceptHeader) {
            Objects.requireNonNull(acceptHeader);
            this.acceptHeader = acceptHeader;
            return this;
        }

        public Builder httpHeader(String headerName, String headerValue) {
            Objects.requireNonNull(headerName);
            Objects.requireNonNull(headerValue);
            this.httpHeaders.put(headerName, headerValue);
            return this;
        }

        public Builder allowCompression(boolean allowCompression) {
            this.allowCompression = allowCompression;
            return this;
        }

        /**
         * Set a timeout to the overall overall operation.
         * Time-to-connect can be set with a customer {@link HttpClient} - see {@link java.net.http.HttpClient.Builder#connectTimeout(java.time.Duration)}.
         */
        public Builder timeout(long timeout, TimeUnit timeoutUnit) {
            if ( timeout < 0 ) {
                this.timeout = -1;
                this.timeoutUnit = null;
            } else {
                this.timeout = timeout;
                this.timeoutUnit = Objects.requireNonNull(timeoutUnit);
            }
            return this;
        }


        public QueryExecutionHTTP build() {
            Objects.requireNonNull("No service URL", serviceURL);
            // urlLimit
            // this.asForm;
            // this.asBody;
            // XXX sendMode
            return new QueryExecutionHTTP(serviceURL, query, queryString, httpClient, httpHeaders, params,
                                          sendMode, acceptHeader, allowCompression,
                                          timeout, timeoutUnit);
        }
    }

    private QueryExecutionHTTP(String serviceURL, Query query, String queryString,
                               HttpClient httpClient, Map<String, String> httpHeaders, Params params,
                               SendMode sendMode, String acceptHeader, boolean allowCompression,
                               long timeout, TimeUnit timeoutUnit) {
        this.context = ARQ.getContext().copy();
        this.service = serviceURL;
        this.query = query;
        this.queryString = queryString;
        this.httpHeaders = new HashMap<>(httpHeaders);
        this.sendMode = sendMode;
        this.acceptHeader = acceptHeader;
        // Important - handled as special case because the defaults vary by query type.
        if ( httpHeaders.containsKey(HttpNames.hAccept) ) {
            this.acceptHeader = httpHeaders.get(HttpNames.hAccept);
            this.httpHeaders.remove(HttpNames.hAccept);
        }
        this.httpHeaders = httpHeaders;
        //this.allowCompression = false;
        this.params = params;
        this.readTimeout = timeout;
        this.readTimeoutUnit = timeoutUnit;
        this.httpClient = dft(httpClient, HttpEnv.getDftHttpClient());

    }

    /**
     * <p>
     * Helper method which applies configuration from the Context to the query
     * engine if a service context exists for the given URI
     * </p>
     * <p>
     * Based off proposed patch for JENA-405 but modified to apply all relevant
     * configuration, this is in part also based off of the private
     * {@code configureQuery()} method of the {@link Service} class though it
     * omits parameter merging since that will be done automatically whenever
     * the {@link QueryExecutionHTTP} instance makes a query for remote submission.
     * </p>
     *
     * @param serviceURI
     *            Service URI
     */
    private static void applyServiceConfig(String serviceURI, QueryExecutionHTTP engine) {
        if (engine.context == null)
            return;

        @SuppressWarnings("unchecked")
        Map<String, Context> serviceContextMap = (Map<String, Context>) engine.context.get(Service.serviceContext);
        if (serviceContextMap != null && serviceContextMap.containsKey(serviceURI)) {
            Context serviceContext = serviceContextMap.get(serviceURI);
            if (log.isDebugEnabled())
                log.debug("Endpoint URI {} has SERVICE Context: {} ", serviceURI, serviceContext);

            // Apply behavioral options
            //engine.setAllowCompression(serviceContext.isTrueOrUndef(Service.queryCompression));
            applyServiceTimeouts(engine, serviceContext);

            // Apply context-supplied client settings
            HttpClient client = serviceContext.get(Service.queryClient);

//            if (client != null) {
//                if (log.isDebugEnabled())
//                    log.debug("Using context-supplied HTTP client for endpoint URI {}", serviceURI);
//                engine.setClient(client);
//            }
        }
    }

    /**
     * Applies context provided timeouts to the given engine
     *
     * @param engine
     *            Engine
     * @param context
     *            Context
     */
    private static void applyServiceTimeouts(QueryExecutionHTTP engine, Context context) {
        if (context.isDefined(Service.queryTimeout)) {
            Object obj = context.get(Service.queryTimeout);
            if (obj instanceof Number) {
                int x = ((Number) obj).intValue();
                engine.setTimeout(-1, x);
            } else if (obj instanceof String) {
                try {
                    String str = obj.toString();
                    if (str.contains(",")) {

                        String[] a = str.split(",");
                        int connect = Integer.parseInt(a[0]);
                        int read = Integer.parseInt(a[1]);
                        engine.setTimeout(read, connect);
                    } else {
                        int x = Integer.parseInt(str);
                        engine.setTimeout(-1, x);
                    }
                } catch (NumberFormatException ex) {
                    throw new QueryExecException("Can't interpret string for timeout: " + obj);
                }
            } else {
                throw new QueryExecException("Can't interpret timeout: " + obj);
            }
        }
    }

    // public void setParams(Params params)
    // { this.params = params ; }

    @Override
    public void setInitialBinding(QuerySolution binding) {
        throw new UnsupportedOperationException(
                "Initial bindings not supported for remote queries, consider using a ParameterizedSparqlString to prepare a query for remote execution");
    }

    @Override
    public void setInitialBinding(Binding binding) {
        throw new UnsupportedOperationException(
                "Initial bindings not supported for remote queries, consider using a ParameterizedSparqlString to prepare a query for remote execution");
    }

    /**
     * @param defaultGraphURIs
     *            The defaultGraphURIs to set.
     */
    public void setDefaultGraphURIs(List<String> defaultGraphURIs) {
        this.defaultGraphURIs = defaultGraphURIs;
    }

    /**
     * @param namedGraphURIs
     *            The namedGraphURIs to set.
     */
    public void setNamedGraphURIs(List<String> namedGraphURIs) {
        this.namedGraphURIs = namedGraphURIs;
    }

//    /**
//     * Sets whether the HTTP requests will permit compressed encoding
//     */
//    public void setAllowCompression(boolean allowed) {
//        allowCompression = allowed;
//    }

    public void addParam(String field, String value) {
        if (params == null)
            params = new Params();
        params.addParam(field, value);
    }

    /**
     * @param defaultGraph
     *            The defaultGraph to add.
     */
    public void addDefaultGraph(String defaultGraph) {
        if (defaultGraphURIs == null)
            defaultGraphURIs = new ArrayList<>();
        defaultGraphURIs.add(defaultGraph);
    }

    /**
     * @param name
     *            The URI to add.
     */
    public void addNamedGraph(String name) {
        if (namedGraphURIs == null)
            namedGraphURIs = new ArrayList<>();
        namedGraphURIs.add(name);
    }

    /** The Content-Type response header received (null before the remote operation is attempted). */
    public String getHttpResponseContentType() {
		return httpResponseContentType;
	}

    // ---- Builder

	@Override
    public ResultSet execSelect() {
        checkNotClosed() ;
        ResultSet rs = execResultSet() ;
        return new ResultSetCheckCondition(rs, this) ;
    }

	private ResultSet execResultSet() {
        String thisAcceptHeader = dft(acceptHeader, selectContentType);

        HttpResponse<InputStream> response = query(thisAcceptHeader);

        InputStream in = HttpLib.getInputStream(response);
        // Don't assume the endpoint actually gives back the content type we asked for
        String actualContentType = response.headers().firstValue(HttpNames.hContentType).orElse(null);

        // Remember the response.
        httpResponseContentType = actualContentType;

        // More reliable to use the format-defined charsets e.g. JSON -> UTF-8
        actualContentType = removeCharset(actualContentType);

        if (false) {
            byte b[] = IO.readWholeFile(in);
            String str = new String(b);
            System.out.println(str);
            in = new ByteArrayInputStream(b);
        }

        retainedConnection = in; // This will be closed on close()


        // If the server fails to return a Content-Type then we will assume
        // the server returned the type we asked for.
        if (actualContentType == null || actualContentType.equals(""))
            actualContentType = selectContentType;

        // Map to lang, with pragmatic alternatives.
        Lang lang = WebContent.contentTypeToLangResultSet(actualContentType);
        if ( lang == null )
            throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not recognized for SELECT queries");
        if ( !ResultSetReaderRegistry.isRegistered(lang) )
            throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not supported for SELECT queries");
        // This returns a streaming result set for some formats.
        // Do not close the InputStream at this point.
        ResultSet result = ResultSetMgr.read(in, lang);
        return result;
    }

    private String removeCharset(String contentType) {
        int idx = contentType.indexOf(';');
        if ( idx < 0 )
            return contentType;
        return contentType.substring(0,idx);
    }

    @Override
    public Model execConstruct() {
        checkNotClosed() ;
        return execConstruct(GraphFactory.makeJenaDefaultModel());
    }

    @Override
    public Model execConstruct(Model model) {
        checkNotClosed() ;
        return execModel(model);
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        checkNotClosed() ;
        return execTriples();
    }

    @Override
    public Iterator<Quad> execConstructQuads(){
        checkNotClosed() ;
    	return execQuads();
    }

    @Override
    public Dataset execConstructDataset(){
        checkNotClosed() ;
        return execConstructDataset(DatasetFactory.createTxnMem());
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset){
        checkNotClosed() ;
        return execDataset(dataset) ;
    }

    @Override
    public Model execDescribe() {
        checkNotClosed() ;
        return execDescribe(GraphFactory.makeJenaDefaultModel());
    }

    @Override
    public Model execDescribe(Model model) {
        checkNotClosed() ;
        return execModel(model);
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        checkNotClosed() ;
        return execTriples();
    }

    private Model execModel(Model model) {
        Pair<InputStream, Lang> p = execConstructWorker(modelContentType) ;
        try(InputStream in = p.getLeft()) {
            Lang lang = p.getRight() ;
            RDFDataMgr.read(model, in, lang);
        } catch (IOException ex) { IO.exception(ex); }
        finally { this.close(); }
        return model;
    }

    private Dataset execDataset(Dataset dataset) {
        Pair<InputStream, Lang> p = execConstructWorker(datasetContentType);
        try(InputStream in = p.getLeft()) {
            Lang lang = p.getRight() ;
            RDFDataMgr.read(dataset, in, lang);
        } catch (IOException ex) { IO.exception(ex); }
        finally { this.close(); }
        return dataset;
    }

    private Iterator<Triple> execTriples() {
        Pair<InputStream, Lang> p = execConstructWorker(modelContentType) ;
        InputStream in = p.getLeft() ;
        Lang lang = p.getRight() ;
        // Base URI?
        return RDFDataMgr.createIteratorTriples(in, lang, null);
    }

    private Iterator<Quad> execQuads() {
        checkNotClosed() ;
        Pair<InputStream, Lang> p = execConstructWorker(datasetContentType) ;
        InputStream in = p.getLeft() ;
        Lang lang = p.getRight() ;
        // Base URI?
        return RDFDataMgr.createIteratorQuads(in, lang, null);
    }

    private Pair<InputStream, Lang> execConstructWorker(String contentType) {
        checkNotClosed() ;
        String thisAcceptHeader = dft(acceptHeader, contentType);
        HttpResponse<InputStream> response = query(thisAcceptHeader);
        InputStream in = HttpLib.getInputStream(response);

        // XXX DRY
        // Don't assume the endpoint actually gives back the content type we
        // asked for
        String actualContentType = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        httpResponseContentType = actualContentType;
        actualContentType = removeCharset(actualContentType);

        // If the server fails to return a Content-Type then we will assume
        // the server returned the type we asked for
        if (actualContentType == null || actualContentType.equals("")) {
            actualContentType = WebContent.defaultDatasetAcceptHeader;
        }

        Lang lang = RDFLanguages.contentTypeToLang(actualContentType);
        if ( ! RDFLanguages.isQuads(lang) && ! RDFLanguages.isTriples(lang) )
            throw new QueryException("Endpoint returned Content Type: "
                                     + actualContentType
                                     + " which is not a valid RDF syntax");
        return Pair.create(in, lang) ;
    }

    @Override
    public boolean execAsk() {
        checkNotClosed() ;
        String thisAcceptHeader = dft(acceptHeader, askContentType);
        HttpResponse<InputStream> response = query(thisAcceptHeader);

        InputStream in = HttpLib.getInputStream(response);
        // XXX DRY
        String actualContentType = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        httpResponseContentType = actualContentType;
        actualContentType = removeCharset(actualContentType);

        // If the server fails to return a Content-Type then we will assume
        // the server returned the type we asked for
        if (actualContentType == null || actualContentType.equals("")) {
            actualContentType = WebContent.defaultDatasetAcceptHeader;
        }

        try( in ) {

            Lang lang = RDFLanguages.contentTypeToLang(actualContentType);
            if ( lang == null ) {
                // Any specials :
                // application/xml for application/sparql-results+xml
                // application/json for application/sparql-results+json
                if (actualContentType.equals(WebContent.contentTypeXML))
                    lang = ResultSetLang.SPARQLResultSetXML;
                else if ( actualContentType.equals(WebContent.contentTypeJSON))
                    lang = ResultSetLang.SPARQLResultSetJSON;
            }
            if ( lang == null )
                throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not supported for ASK queries");
            boolean result = ResultSetMgr.readBoolean(in, lang);
            return result;
        } catch (ResultSetException e) {
            log.warn("Returned content is not a boolean result", e);
            throw e;
        } catch (QueryExceptionHTTP e) {
            throw e ;
        }
        catch (java.io.IOException e) {
            log.warn("Failed to close connection", e);
            return false ;
        }
        finally { this.close(); }
    }

    @Override
    public JsonArray execJson() {
        checkNotClosed();
        String thisAcceptHeader = dft(acceptHeader, WebContent.contentTypeJSON);
        HttpResponse<InputStream> response = query(thisAcceptHeader);
        InputStream in = HttpLib.getInputStream(response);
        return JSON.parseAny(in).getAsArray();
    }

    @Override
    public Iterator<JsonObject> execJsonItems() {
        JsonArray array = execJson().getAsArray();
        List<JsonObject> x = new ArrayList<>(array.size());
        array.forEach(elt->{
            if ( ! elt.isObject())
                throw new QueryExecException("Item in an array from a JSON query isn't an object");
            x.add(elt.getAsObject());
        });
        return x.iterator();
    }

    private void checkNotClosed() {
        if ( closed )
            throw new QueryExecException("HTTP QueryExecution has been closed") ;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public Dataset getDataset() {
        return null;
    }

    // This may be null - if we were created form a query string,
    // we don't guarantee to parse it so we let through non-SPARQL
    // extensions to the far end.
    @Override
    public Query getQuery() {
        if ( query != null )
            return query;
        if ( queryString != null ) {
            // Object not created with a Query object, may be because there is forgein
            // syntax in the query or may be because the query string was available and the app
            // didn't want the overhead of parsing it every time.
            // Try to parse it else return null;
            try { return QueryFactory.create(queryString, Syntax.syntaxARQ); }
            catch (QueryParseException ex) {}
            return null ;
        }
        return null;
    }

    /**
     * Return the query string. If this was supplied in a constructor, there is no
     * guarantee this is legal SPARQL syntax.
     */
    public String getQueryString() {
        return queryString;
    }

    @Override
    public void setTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
        this.readTimeoutUnit = TimeUnit.MILLISECONDS;
    }

    @Override
    public void setTimeout(long readTimeout, long connectTimeout) {
        this.readTimeout = readTimeout;
        this.readTimeoutUnit = TimeUnit.MILLISECONDS;
        this.connectTimeout = connectTimeout;
        this.connectTimeoutUnit = TimeUnit.MILLISECONDS;
    }

    @Override
    public void setTimeout(long readTimeout, TimeUnit timeoutUnits) {
        this.readTimeout = readTimeout;
        this.readTimeoutUnit = timeoutUnits;
    }

    @Override
    public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
        this.readTimeout = timeout1;
        this.readTimeoutUnit = timeUnit1;
        this.connectTimeout = timeout2;
        this.connectTimeoutUnit = timeUnit2;
    }

    @Override
    public long getTimeout1() {
        return asMillis(readTimeout, readTimeoutUnit);
    }

    @Override
    public long getTimeout2() {
        return asMillis(connectTimeout, connectTimeoutUnit);
    }

    /**
     * Gets whether HTTP requests will indicate to the remote server that
     * compressed encoding of responses is accepted
     *
     * @return True if compressed encoding will be accepted
     */
    public boolean getAllowCompression() {
        return allowCompression;
    }

    private static long asMillis(long duration, TimeUnit timeUnit) {
        return (duration < 0) ? duration : timeUnit.toMillis(duration);
    }

    // Use SPARQL query body and MIME type.
    // XXX Use!
    // XXX Extract and share with SPARQL Update.
    // Common steps with query()
    private HttpResponse<InputStream> queryPush(String acceptHeader) {
        if (closed)
            throw new ARQException("HTTP execution already closed");

        Params thisParams = new Params();
        if ( params != null )
            thisParams.merge(params);

        thisParams.merge(getServiceParams(service, context));

        // XXX Params to URL
        for ( String dft : defaultGraphURIs )
            thisParams.addParam( HttpParams.pDefaultGraph, dft );
        for ( String name : namedGraphURIs )
            thisParams.addParam( HttpParams.pNamedGraph, name );

        HttpRequest request = HttpLib.newBuilder(service, httpHeaders, acceptHeader, allowCompression, readTimeout, readTimeoutUnit)
            .POST(BodyPublishers.ofString(queryString))
            .header(HttpNames.hContentType, WebContent.contentTypeSPARQLQuery)
            .build();

        HttpResponse<InputStream> response = execute(httpClient, request, BodyHandlers.ofInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
        return response;
    }

    // Use GET/POST-form.
    private HttpResponse<InputStream> query(String acceptHeader) {
        if (closed)
            throw new ARQException("HTTP execution already closed");

        //  SERVICE specials.

        Params thisParams = params;

        for ( String dft : defaultGraphURIs )
            thisParams.addParam( HttpParams.pDefaultGraph, dft );
        for ( String name : namedGraphURIs )
            thisParams.addParam( HttpParams.pNamedGraph, name );

        if ( httpHeaders != null ) {
            if ( acceptHeader != null )
                httpHeaders.put(HttpNames.hAccept, acceptHeader);
            if ( allowCompression )
                httpHeaders.put(HttpNames.hAcceptEncoding, "gzip,inflate");
        }

        // Query string or HTML form.
        if ( sendMode == SendMode.asPostBody )
            return queryPush(acceptHeader);

        thisParams.merge(getServiceParams(service, context));
        thisParams.addParam(HttpParams.pQuery, queryString);

        int thisLengthLimit = urlLimit;
        switch(sendMode) {
            case asGetAlways :
                thisLengthLimit = Integer.MAX_VALUE;
                break;
            case asGetWithLimit :
                break;
            case asPostForm :
                // Force form use.
                thisLengthLimit = 0;
                break;
            case asPostBody : // Already handled.
            default :
                throw new HttpException("Send mode not recognized for query string based request: "+sendMode);
        }

        // XXX
//        // check for service context overrides
//        if (context.isDefined(Service.serviceContext)) {
//            Map<String, Context> servicesContext = context.get(Service.serviceContext);
//            if (servicesContext.containsKey(service)) {
//                Context serviceContext = servicesContext.get(service);
//                if (serviceContext.isDefined(Service.queryClient)) client = serviceContext.get(Service.queryClient);
//            }
//        }

        return execRequestQS(service, httpClient, sendMode, thisLengthLimit, httpHeaders, thisParams, acceptHeader, allowCompression, readTimeout, readTimeoutUnit);
    }

    /**
     * Make request using a query string, or, if too long, an HTTP HTML form.
     * @param url
     * @param sendMode
     * @param params -- The HTTP query string in the form of (name,value) pairs but not "query="
     * @param httpClient
     * @param timeout -- use -1 for no timeout.
     * @param timeoutTimeUnit
     * @return HttpResponse<InputStream>
     */
    private static HttpResponse<InputStream> execRequestQS(String url, HttpClient httpClient,
                                                           SendMode sendMode, int lengthLimit,
                                                           Map<String, String> httpHeaders, Params params,
                                                           String acceptHeader, boolean allowCompression,
                                                           long timeout, TimeUnit timeoutTimeUnit) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(params);
        Objects.requireNonNull(httpClient);
        boolean useGET;
        String qs = params.httpString();
        String requestURL = url;
        if ( params.count() > 0 ) {
            if ( url.length()+qs.length()+1 > lengthLimit ) {
                useGET = false;
            } else {
                useGET = true;
                // Use GET with a query string.
                requestURL = url.contains("?") ? url+"&"+qs: url+"?"+qs;
            }
        } else
            useGET = true;

        HttpRequest.Builder builder = HttpLib.newBuilder(requestURL, httpHeaders, acceptHeader, allowCompression, timeout, timeoutTimeUnit);

        if ( useGET )
            builder.GET();
        else {
            // Already UTF-8 encoded to ASCII.
            builder.POST(BodyPublishers.ofString(qs, StandardCharsets.US_ASCII))
                .header(HttpNames.hContentType, WebContent.contentTypeHTMLForm);
        }

        HttpRequest request = builder.build();

        HttpResponse<InputStream> response = execute(httpClient, request, BodyHandlers.ofInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
        return response;
    }



    // This is to allow setting additional/optional query parameters on a per
    // SERVICE level, see: JENA-195
    protected static Params getServiceParams(String serviceURI, Context context) throws QueryExecException {
        Params params = new Params();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, List<String>>> serviceParams = (Map<String, Map<String, List<String>>>) context
                .get(ARQ.serviceParams);
        if (serviceParams != null) {
            Map<String, List<String>> paramsMap = serviceParams.get(serviceURI);
            if (paramsMap != null) {
                for (String param : paramsMap.keySet()) {
                    if (HttpParams.pQuery.equals(param))
                        throw new QueryExecException("ARQ serviceParams overrides the 'query' SPARQL protocol parameter");

                    List<String> values = paramsMap.get(param);
                    for (String value : values)
                        params.addParam(param, value);
                }
            }
        }
        return params;
    }

    /**
     * Cancel query evaluation
     */
    public void cancel() {
        closed = true;
    }

    @Override
    public void abort() {
        try {
            close();
        } catch (Exception ex) {
            log.warn("Error during abort", ex);
        }
    }

    @Override
    public void close() {
        closed = true;
        if (retainedConnection != null) {
            try {
                // This call may take a long time if the response has not been consumed
                // as HTTP client will consume the remaining response so it can re-use the
                // connection. If we're closing when we're not at the end of the stream then
                // issue a warning to the logs
                if (retainedConnection.read() != -1)
                    log.warn("HTTP response not fully consumed, if HTTP Client is reusing connections (its default behaviour) then it will consume the remaining response data which may take a long time and cause this application to become unresponsive");
                retainedConnection.close();
            } catch (RuntimeIOException e) {
                // If we are closing early and the underlying stream is chunk encoded
                // the close() can result in a IOException.  Unfortunately our TypedInputStream
                // catches and re-wraps that and we want to suppress it when we are cleaning up
                // and so we catch the wrapped exception and log it instead
                log.debug("Failed to close connection", e);
            } catch (java.io.IOException e) {
                log.debug("Failed to close connection", e);
            } finally {
                retainedConnection = null;
            }
        }
    }

    @Override
    public boolean isClosed() { return closed ; }

}