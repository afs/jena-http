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

package org.seaborne.unused;

import static org.apache.jena.http.HttpLib.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Triple;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.http.Params;
import org.apache.jena.http.QuerySendMode;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetReaderRegistry;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.ResultSetCheckCondition;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.Context;

/**
 * A query execution implementation where queries are executed against a remote
 * service over HTTP.
 */
class QueryExecutionHTTP_Standalone implements QueryExecution {
    // Not Need but retained because of setInitialBinding and setTimeOut ?
    // == QueryEngineHTTP

    // IF USED - restore finish().

    /** @deprecated Use {@link #newBuilder} */
    @Deprecated
    public static QueryExecutionHTTPBuilder create() { return newBuilder() ; }

    public static QueryExecutionHTTPBuilder newBuilder() { return QueryExecutionHTTPBuilder.newBuilder(); }

    public static final String QUERY_MIME_TYPE = WebContent.contentTypeSPARQLQuery;
    private final Query query;
    private final String queryString;
    private final String service;
    private final Context context;

    // Params
    private Params params = null;

    private final QuerySendMode sendMode;
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

    // Compression for response
    private boolean allowCompression = false;

    // Content Types: these list the standard formats and also include */*.
    private String selectAcceptheader    = WebContent.defaultSparqlResultsHeader;
    private String askAcceptHeader       = WebContent.defaultSparqlAskHeader;
    private String describeAcceptHeader  = WebContent.defaultGraphAcceptHeader;
    private String constructAcceptHeader = WebContent.defaultGraphAcceptHeader;
    private String datasetAcceptHeader   = WebContent.defaultDatasetAcceptHeader;

    // If this is non-null, it overrides the use of any Content-Type above.
    private String acceptHeader         = null;

    // Received content type
    private String httpResponseContentType = null;
    // Releasing HTTP input streams is important. We remember this for SELECT result
    // set streaming, and will close it when the execution is closed
    private InputStream retainedConnection = null;

    private HttpClient httpClient = HttpEnv.getDftHttpClient();

    private Map<String, String> httpHeaders;

    // [QExec]
    public
    QueryExecutionHTTP_Standalone(String serviceURL, Query query, String queryString, int urlLimit,
                                   HttpClient httpClient, Map<String, String> httpHeaders, Params params,
                                   List<String> defaultGraphURIs, List<String> namedGraphURIs,
                                   QuerySendMode sendMode, String acceptHeader, boolean allowCompression,
                                   long timeout, TimeUnit timeoutUnit) {
        this.context = ARQ.getContext().copy();
        this.service = serviceURL;
        this.query = query;
        this.queryString = queryString;
        this.urlLimit = urlLimit;
        this.httpHeaders = httpHeaders;
        this.defaultGraphURIs = defaultGraphURIs;
        this.namedGraphURIs = namedGraphURIs;
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

    /** The Content-Type response header received (null before the remote operation is attempted). */
    public String getHttpResponseContentType() {
		return httpResponseContentType;
	}

    // ---- Builder

	@Override
    public ResultSet execSelect() {
        checkNotClosed();
        check(QueryType.SELECT);
        ResultSet rs = execResultSet();
        return new ResultSetCheckCondition(rs, this);
    }

	private ResultSet execResultSet() {
        String thisAcceptHeader = dft(acceptHeader, selectAcceptheader);

        HttpResponse<InputStream> response = query(thisAcceptHeader);
        InputStream in = HttpLib.handleResponseInputStream(response);
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

        if (actualContentType == null || actualContentType.equals(""))
            actualContentType = WebContent.contentTypeResultsXML;

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

    @Override
    public boolean execAsk() {
        checkNotClosed();
        check(QueryType.ASK);
        String thisAcceptHeader = dft(acceptHeader, askAcceptHeader);
        HttpResponse<InputStream> response = query(thisAcceptHeader);
        InputStream in = HttpLib.handleResponseInputStream(response);

        String actualContentType = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        httpResponseContentType = actualContentType;
        actualContentType = removeCharset(actualContentType);

        // If the server fails to return a Content-Type then we will assume
        // the server returned the type we asked for
        if (actualContentType == null || actualContentType.equals(""))
            actualContentType = askAcceptHeader;

        Lang lang = RDFLanguages.contentTypeToLang(actualContentType);
        if ( lang == null ) {
            // Any specials :
            // application/xml for application/sparql-results+xml
            // application/json for application/sparql-results+json
            if (actualContentType.equals(WebContent.contentTypeXML))
                lang = ResultSetLang.RS_XML;
            else if ( actualContentType.equals(WebContent.contentTypeJSON))
                lang = ResultSetLang.RS_JSON;
        }
        if ( lang == null )
            throw new QueryException("Endpoint returned Content-Type: " + actualContentType + " which is not supported for ASK queries");
        boolean result = ResultSetMgr.readBoolean(in, lang);
        //XXXfinish(response);
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
        checkNotClosed();
        return execConstruct(GraphFactory.makeJenaDefaultModel());
    }

    @Override
    public Model execConstruct(Model model) {
        checkNotClosed();
        check(QueryType.CONSTRUCT);
        return execModel(model, constructAcceptHeader);
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        checkNotClosed();
        check(QueryType.CONSTRUCT);
        return execTriples(constructAcceptHeader);
    }

    @Override
    public Iterator<Quad> execConstructQuads(){
        checkNotClosed();
    	return execQuads();
    }

    @Override
    public Dataset execConstructDataset(){
        checkNotClosed();
        return execConstructDataset(DatasetFactory.createTxnMem());
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset){
        checkNotClosed();
        check(QueryType.CONSTRUCT_QUADS);
        return execDataset(dataset);
    }

    @Override
    public Model execDescribe() {
        checkNotClosed();
        return execDescribe(GraphFactory.makeJenaDefaultModel());
    }

    @Override
    public Model execDescribe(Model model) {
        checkNotClosed();
        check(QueryType.DESCRIBE);
        return execModel(model, describeAcceptHeader);
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        checkNotClosed();
        return execTriples(describeAcceptHeader);
    }

    private Model execModel(Model model, String acceptHeader) {
        Pair<InputStream, Lang> p = execRdfWorker(acceptHeader, WebContent.contentTypeRDFXML);
        InputStream in = p.getLeft();
        Lang lang = p.getRight();
        try {
            RDFDataMgr.read(model, in, lang);
        } catch (RiotException ex) {
            //XXX finish(in);
            throw ex;
        }
        return model;
    }

    private Dataset execDataset(Dataset dataset) {
        Pair<InputStream, Lang> p = execRdfWorker(datasetAcceptHeader, WebContent.contentTypeNQuads);
        InputStream in = p.getLeft();
        Lang lang = p.getRight();
        try {
            RDFDataMgr.read(dataset, in, lang);
        } catch (RiotException ex) {
            //XXX finish(in);
            throw ex;
        }
        return dataset;
    }

    private Iterator<Triple> execTriples(String acceptHeader) {
        Pair<InputStream, Lang> p = execRdfWorker(acceptHeader, WebContent.contentTypeRDFXML);
        InputStream in = p.getLeft();
        Lang lang = p.getRight();
        // Base URI?
        return RDFDataMgr.createIteratorTriples(in, lang, null);
    }

    private Iterator<Quad> execQuads() {
        checkNotClosed();
        Pair<InputStream, Lang> p = execRdfWorker(datasetAcceptHeader, WebContent.contentTypeNQuads);
        InputStream in = p.getLeft();
        Lang lang = p.getRight();
        // Base URI?
        return RDFDataMgr.createIteratorQuads(in, lang, null);
    }

    // Any RDF data back (CONSTRUCT, DESCRIBE, QUADS)
    // ifNoContentType - some wild guess at the content type.
    private Pair<InputStream, Lang> execRdfWorker(String contentType, String ifNoContentType) {
        checkNotClosed();
        String thisAcceptHeader = dft(acceptHeader, contentType);
        HttpResponse<InputStream> response = query(thisAcceptHeader);
        InputStream in = HttpLib.handleResponseInputStream(response);

        // Don't assume the endpoint actually gives back the content type we asked for
        String actualContentType = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        httpResponseContentType = actualContentType;
        actualContentType = removeCharset(actualContentType);

        // If the server fails to return a Content-Type then we will assume
        // the server returned the type we asked for
        if (actualContentType == null || actualContentType.equals(""))
            actualContentType = ifNoContentType;

        Lang lang = RDFLanguages.contentTypeToLang(actualContentType);
        if ( ! RDFLanguages.isQuads(lang) && ! RDFLanguages.isTriples(lang) )
            throw new QueryException("Endpoint returned Content Type: "
                                     + actualContentType
                                     + " which is not a valid RDF syntax");
        return Pair.create(in, lang);
    }

    @Override
    public JsonArray execJson() {
        checkNotClosed();
        check(QueryType.CONSTRUCT_JSON);
        String thisAcceptHeader = dft(acceptHeader, WebContent.contentTypeJSON);
        HttpResponse<InputStream> response = query(thisAcceptHeader);
        InputStream in = HttpLib.handleResponseInputStream(response);
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
            throw new QueryExecException("HTTP QueryExecution has been closed");
    }

    private void check(QueryType queryType) {
        if ( query == null ) {
            // Pass through the queryString.
            return;
        }
        if ( query.queryType() != queryType )
            throw new QueryExecException("Not the right form of query. Expected "+queryType+" but got "+query.queryType());
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
            // Object not created with a Query object, may be because there is foreign
            // syntax in the query or may be because the query string was available and the app
            // didn't want the overhead of parsing it every time.
            // Try to parse it else return null;
            try { return QueryFactory.create(queryString, Syntax.syntaxARQ); }
            catch (QueryParseException ex) {}
            return null;
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

    // Make a query.
    private HttpResponse<InputStream> query(String reqAcceptHeader) {
        if (closed)
            throw new ARQException("HTTP execution already closed");

        //  SERVICE specials.

        Params thisParams = Params.create(params);

        if ( defaultGraphURIs != null ) {
            for ( String dft : defaultGraphURIs )
                thisParams.add( HttpParams.pDefaultGraph, dft );
        }
        if ( namedGraphURIs != null ) {
            for ( String name : namedGraphURIs )
                thisParams.add( HttpParams.pNamedGraph, name );
        }

        // Same as UpdateExecutionHTTP
        HttpLib.modifyByService(service, context, thisParams,  httpHeaders);

        QuerySendMode actualSendMode = actualSendMode();
        HttpRequest.Builder builder;
        switch(actualSendMode) {
            case asGetAlways :
                builder = executeQueryGet(thisParams, reqAcceptHeader);
                break;
            case asPostForm :
                builder = executeQueryPostForm(thisParams, reqAcceptHeader);
                break;
            case asPostBody :
                builder = executeQueryPostBody(thisParams, reqAcceptHeader);
                break;
            default :
                // Should not happen!
                throw new HttpException("Send mode not recognized for query request: "+sendMode);
        }

        if ( allowCompression )
            acceptEncoding(builder);
        HttpRequest request = builder.build();
        // Status code has not been processed on the return from execute*
        return executeQuery(request);
    }

    private QuerySendMode actualSendMode() {
        int thisLengthLimit = urlLimit;
        switch(sendMode) {
            // Not switchable.
            case asGetAlways :
            case asPostForm :
            case asPostBody :
                return sendMode;
            case asGetWithLimitBody :
            case asGetWithLimitForm :
                break;
            default :
                throw new HttpException("Send mode not recognized for query request: "+sendMode);
        }

        // Only QuerySendMode.asGetWithLimitBody and QuerySendMode.asGetWithLimitForm here.
        String requestURL = service;
        // Don't add yet
        //thisParams.addParam(HttpParams.pQuery, queryString);
        String qs = params.httpString();
        // ?query=

        // URL Length, including service (for safety)
        int length = service.length()+1+HttpParams.pQuery.length()+1+qs.length();
        if ( length <= thisLengthLimit )
            return QuerySendMode.asGetAlways;

        return (sendMode==QuerySendMode.asGetWithLimitBody) ? QuerySendMode.asPostBody : QuerySendMode.asPostForm;
    }

    private HttpRequest.Builder executeQueryGet(Params thisParams, String acceptHeader) {
        // This may still be  POST and an HTML form
        Objects.requireNonNull(service);
        Objects.requireNonNull(params);
        Objects.requireNonNull(httpClient);

        String requestURL = service;
        thisParams.add(HttpParams.pQuery, queryString);
        String qs = params.httpString();

        HttpRequest.Builder builder = HttpLib.newBuilder(requestURL, httpHeaders, allowCompression, readTimeout, readTimeoutUnit);
        acceptHeader(builder, acceptHeader);
        return builder.GET();
    }

    private HttpRequest.Builder executeQueryPostForm(Params thisParams, String acceptHeader) {
        String requestURL = service;
        thisParams.add(HttpParams.pQuery, queryString);
        String qs = params.httpString();

        HttpRequest.Builder builder = HttpLib.newBuilder(requestURL, httpHeaders, allowCompression, readTimeout, readTimeoutUnit);
        acceptHeader(builder, acceptHeader);
        // Use an HTML form.
        contentTypeHeader(builder, WebContent.contentTypeHTMLForm);
        // Already UTF-8 encoded to ASCII.
        return builder.POST(BodyPublishers.ofString(qs, StandardCharsets.US_ASCII));
    }

    // Use SPARQL query body and MIME type.
    private HttpRequest.Builder executeQueryPostBody(Params thisParams, String acceptHeader) {
        if (closed)
            throw new ARQException("HTTP execution already closed");

        // Use thisParams (for default-graph-uri etc)
        String url = service;
        if ( thisParams.count() > 0 )
            url = url + "&"+thisParams.httpString();

        HttpRequest.Builder builder = HttpLib.newBuilder(service, httpHeaders, allowCompression, readTimeout, readTimeoutUnit);
        if ( allowCompression )
            acceptEncoding(builder);
        contentTypeHeader(builder, WebContent.contentTypeSPARQLQuery);
        acceptHeader(builder, acceptHeader);
        return builder.POST(BodyPublishers.ofString(queryString));
    }


    private HttpResponse<InputStream> executeQuery(HttpRequest request) {
        logQuery(queryString, request);
        return execute(httpClient, request);
    }

    private static void logQuery(String queryString, HttpRequest request) {}

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
            Log.warn(this, "Error during abort", ex);
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
                    Log.warn(this, "HTTP response not fully consumed, if HTTP Client is reusing connections (its default behaviour) then it will consume the remaining response data which may take a long time and cause this application to become unresponsive");
                retainedConnection.close();
            } catch (RuntimeIOException | java.io.IOException e) {
                // If we are closing early and the underlying stream is chunk encoded
                // the close() can result in a IOException. TypedInputStream catches
                // and re-wraps that and we want to suppress both forms.
            } finally {
                retainedConnection = null;
            }
        }
    }

    @Override
    public boolean isClosed() { return closed; }

}