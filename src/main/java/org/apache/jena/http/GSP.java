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

package org.apache.jena.http;

import java.io.FileNotFoundException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.shared.NotFoundException;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * Client for the
 * <a href="https://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store Protocol</a>.
 * <p>
 * This is extended to include operations GET, POST and PUT on datasets.
 * <p>
 * Examples:
 * <pre>
 *   // Get the default graph.
 *   Graph graph = GSP.request("http://example/dataset").defaultGraph().GET();
 * </pre>
 * <pre>
 *   // Get a named graph.
 *   Graph graph = GSP.request("http://example/dataset").namedGraph("http://my/graph").GET();
 * </pre>
 * <pre>
 *   // POST (add) to a named graph.
 *   Graph myData = ...;
 *   GSP.request("http://example/dataset").namedGraph("http://my/graph").POST(myData);
 * </pre>
 */
public class GSP {
    /**
     * Return the URL for a graph named using the
     * <a href="https://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store Protocol</a>.
     * The {@code graphStore} is the URL of the network location of the graph store.
     * The {@code graphName} be a valid, absolute URI (i.e. includes the scheme)
     * or the word "default", or null, for the default graph of the store
     * or the word "union" for the union graph of the store (this is a Jena extension).
     * @param graphStore
     * @param graphName
     * @return String
     */
    public static String urlForGraph(String graphStore, String graphName) {
        // If query string already has a "?...", assume we are appending with "&".
        String ch = "?";
        if ( graphStore.contains("?") )
            // Already has a query string, append with "&"
            ch = "&";
        return graphStore + ch + queryStringForGraph(graphName) ;
    }

    private static String dftName =  "default" ;

//    /*package*/ static boolean isDefault(String name) {
//        return name == null || name.equals(dftName) ;
//    }

    private static String queryStringForGraph(String graphName) {
        if ( graphName== null )
            return HttpNames.paramGraphDefault;
        switch (graphName) {
            case HttpNames.graphTargetDefault:
                return HttpNames.paramGraphDefault;
            case HttpNames.graphTargetUnion:
                return HttpNames.paramGraph+"=union";
            default:
                return HttpNames.paramGraph+"="+HttpLib.urlEncodeQueryString(graphName);
        }
    }

    private String              serviceEndpoint = null;
    // Need to keep this separately from contentType because it affects the choice of writer.
    private RDFFormat           rdfFormat       = null;
    private HttpClient          httpClient      = null;
    private Map<String, String> httpHeaders     = new HashMap<>();
    private boolean             allowCompression = false;

    // One, and only one of these, must be set at the point the terminating operation is called.
    private boolean             datasetGraph    = false;
    private boolean             defaultGraph    = false;
    private String              graphName       = null;

    /** Create a request to the remote service (without GSP naming).
     *  Call {@link #defaultGraph()} or {@link #graphName(String)} to select the target graph.
     * @param service
     */
    public static GSP request(String service) {
        return new GSP().service(service);
    }

    protected GSP() {}

    /**
     * Set the URL of the query endpoint. This replaces any value set in the
     * {@link #request(String)} call.
     */
    public GSP service(String serviceURL) {
        this.serviceEndpoint = Objects.requireNonNull(serviceURL);
        return this;
    }

    public GSP httpClient(HttpClient httpClient) {
        Objects.requireNonNull(httpClient, "HttpClient");
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Set an HTTP header that is added to the request.
     * See {@link #accept(Lang)}, {@link #accept(Lang)}, {@link #contentType(RDFFormat)} and {@link #contentTypeFromFilename(String)}
     * for specific handling of {@code Accept:} and {@code Content-Type}.
     */
    public GSP httpHeader(String headerName, String headerValue) {
        Objects.requireNonNull(headerName);
        Objects.requireNonNull(headerValue);
        if ( httpHeaders == null )
            httpHeaders = new HashMap<>();
        httpHeaders.put(headerName, headerValue);
        return this;
    }

    // Private - no getters.
    private String httpHeader(String header) {
        Objects.requireNonNull(header);
        if ( httpHeaders == null )
            return null;
        return httpHeaders.get(header);
    }

    /** Enable a request for compression to be used for the response (i.e. Accept-Encoding)*/
    public GSP allowCompression(boolean allowCompression) {
        this.allowCompression = allowCompression;
        return this;
    }

    /** Send request for a named graph (used in {@code ?graph=}) */
    public GSP graphName(String graphName) {
        Objects.requireNonNull(graphName);
        this.graphName = graphName;
        this.defaultGraph = false;
        return this;
    }

    /** Send request for a named graph (used in {@code ?graph=}) */
    public GSP graphName(Node graphName) {
        Objects.requireNonNull(graphName);
        if ( ! graphName.isURI() && ! graphName.isBlank() )
            throw exception("Not an acceptable graph name: "+this.graphName);
        Node gn = RiotLib.blankNodeToIri(graphName);
        this.graphName = gn.getURI();
        this.defaultGraph = false;
        this.datasetGraph = false;
        return this;
    }

    /** Send request for the default graph (that is, {@code ?default}) */
    public GSP defaultGraph() {
        this.graphName = null;
        this.defaultGraph = true;
        this.datasetGraph = false;
        return this;
    }

    /** Send request for the dataset. */
    public GSP dataset() {
        this.graphName = null;
        this.defaultGraph = false;
        this.datasetGraph = true;
        return this;
    }

    /** Set the accept header on GET requests. Optional; if not set, a system default is used. */
    public GSP acceptHeader(String acceptHeader) {
        httpHeader(HttpNames.hAccept, acceptHeader);
        return this;
    }

    // No getters.
    private String acceptHeader() {
        return httpHeader(HttpNames.hAccept);
    }

    /** Set the accept header on GET requests. Optional; if not set, a system default is used. */
    public GSP accept(Lang lang) {
        String acceptHeader = (lang != null ) ? lang.getContentType().getContentTypeStr() : null;
        httpHeader(HttpNames.hAccept, acceptHeader);
        return this;
    }

    /**
     * Set the Content-type for a POST, PUT request of a file
     * or serialization of a graph opf dataset is necessary.
     * Optional; if not set, the file extension is used or the
     * system default RDF syntax encoding.
     */
    public GSP contentTypeHeader(String contentType) {
        httpHeader(HttpNames.hContentType, contentType);
        return this;
    }

    // No getters.
    private String contentType() {
        return httpHeader(HttpNames.hContentType);
    }

    /**
     * Set the Content-type for a POST, PUT request of a file
     * or serialization of a graph opf dataset is necessary.
     * Optional; if not set, the file extension is used or the
     * system default RDF syntax encoding.
     */
    public GSP contentType(RDFFormat rdfFormat) {
        this.rdfFormat = rdfFormat;
        String contentType = rdfFormat.getLang().getContentType().getContentTypeStr();
        httpHeader(HttpNames.hContentType, contentType);
        return this;
    }

    private void validateGraphOperation() {
        Objects.requireNonNull(serviceEndpoint);
        if ( ! defaultGraph && graphName == null )
            throw exception("Need either default graph or a graph name");
        if ( datasetGraph )
            throw exception("Dataset request specified for graph operation");
    }

    private void internalDataset() {
        // Set dataset request.
        // Checking is done by validateDatasetOperation.
        // The dataset operations have "Dataset" in the name, so less point having
        // required dataset(). We can't use GET() because the return type
        // would be "Graph or DatasetGraph"
        // Reconsider if graph synonyms provided.
        this.datasetGraph = true;
    }

    private void validateDatasetOperation() {
        Objects.requireNonNull(serviceEndpoint);
        if ( defaultGraph )
            throw exception("Default graph specified for dataset operation");
        if ( graphName != null )
            throw exception("A graph name specified for dataset operation");
        if ( ! datasetGraph )
            throw exception("Dataset request not specified for dataset operation");
    }

    /**
     * Choose the HttpClient to use.
     * The requestURL includes the query string (for graph GSP operations).
     * If explicit set with {@link #httpClient(HttpClient)}, use that;
     * other use the system registry and default {@code HttpClient} settings
     * in {@link HttpEnv}.
     */
    private HttpClient requestHttpClient(String serviceURL, String requestURL) {
        if ( httpClient != null )
            return httpClient;
        return HttpEnv.getHttpClient(serviceURL, httpClient);
    }

    // Setup problems.
    private static RuntimeException exception(String msg) {
        return new HttpException(msg);
    }

    // Synonyms mirror the dataset names, so getGraph/getDataset

    /** Get a graph */
    public Graph GET() {
        validateGraphOperation();
        ensureAcceptHeader(WebContent.defaultGraphAcceptHeader);
        requestCompression();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        Graph graph = GraphFactory.createDefaultGraph();
        HttpClient hc = requestHttpClient(serviceEndpoint, url);
        HttpRDF.httpGetToStream(hc, url, httpHeaders, StreamRDFLib.graph(graph));
        return graph;
    }

//    /**
//     * Get a graph.
//     * <p>
//     * Synonym for {@link #GET()}.
//     */
//    public Graph getGraph() {
//        // Synonym
//        return GET();
//    }

    /**
     * POST the contents of a file using the filename extension to determine the
     * Content-Type to use if it is not already set.
     * <p>
     * This operation does not parse the file.
     */
    public void POST(String file) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        String fileExtContentType = contentTypeFromFilename(file);
        HttpClient hc = requestHttpClient(serviceEndpoint, url);
        uploadTriples(hc, url, file, fileExtContentType, httpHeaders, Push.POST);
    }

//    /**
//     * Load the contents of a file into the target graph using the filename extension to determine the
//     * Content-Type to use if it is not already set.
//     * <p>
//     * Synonym for {@link #POST(String)}.
//     * <p>
//     * This operation does not parse the file.
//     * <p>
//     * If the data may have quads (named graphs), use {@link #postDataset(String)}.
//     *
//     */
//    public void postGraph(String file) {
//        POST(file);
//    }

    /** POST a graph. */
    public void POST(Graph graph) {
        validateGraphOperation();
        RDFFormat requestFmt = rdfFormat(HttpEnv.dftTriplesFormat);
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        HttpClient hc = requestHttpClient(serviceEndpoint, url);
        HttpRDF.postGraph(hc, url, graph, requestFmt, httpHeaders);
    }

//    /**
//     * POST a graph.
//     * <p>
//     * Synonym for {@link #POST(Graph)}.
//     */
//    public void postGraph(Graph graph) {
//        // Synonym
//        POST(graph);
//    }

    /**
     * PUT the contents of a file using the filename extension to determine the
     * Content-Type to use if it is not already set.
     * <p>
     * This operation does not parse the file.
     * <p>
     * If the data may have quads (named graphs), use {@link #putDataset(String)}.
     */
    public void PUT(String file) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        String fileExtContentType = contentTypeFromFilename(file);
        HttpClient hc = requestHttpClient(serviceEndpoint, url);
        uploadTriples(hc, url, file, fileExtContentType, httpHeaders, Push.PUT);
    }

//    /**
//     * PUT the contents of a file using the filename extension to determine the
//     * Content-Type to use if it is not already set.
//     * <p>
//     * Synonym for {@link #PUT(String)}.
//     * <p>
//     * This operation does not parse the file.
//     * <p>
//     * If the data may have quads (named graphs), use {@link #putDataset(String)}.
//     */
//    public void putGraph(String file) {
//        // Synonym
//        PUT(file);
//    }

    /**
     * PUT a graph.
     */
    public void PUT(Graph graph) {
        validateGraphOperation();
        RDFFormat requestFmt = rdfFormat(HttpEnv.dftTriplesFormat);
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        HttpClient hc = requestHttpClient(serviceEndpoint, url);
        HttpRDF.putGraph(hc, url, graph, requestFmt, httpHeaders);
    }

//    /**
//     * Put a graph - replace the previous contents.
//     * <p>
//     * Synonym for {@link #PUT(Graph)}.
//     */
//    public void putGraph(Graph graph) {
//        // Synonym
//        PUT(graph);
//    }

    /** Delete a graph. */
    public void DELETE() {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        HttpClient hc = requestHttpClient(serviceEndpoint, url);
        HttpRDF.httpDeleteGraph(hc, url);
    }

//    /**
//     * Delete a graph.
//     * <p>
//     * Synonym for {@link #DELETE()}.
//     */
//    public void deleteGraph() {
//        // Synonym
//        DELETE();
//    }

    /**
     * GET dataset.
     * <p>
     * If the remote end is a graph, the result is a dataset with that
     * graph data in the default graph of the dataset.
     */
    public DatasetGraph getDataset() {
        internalDataset();
        validateDatasetOperation();
        ensureAcceptHeader(WebContent.defaultRDFAcceptHeader);
        requestCompression();
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        HttpClient hc = requestHttpClient(serviceEndpoint, serviceEndpoint);
        HttpRDF.httpGetToStream(hc, serviceEndpoint, httpHeaders, StreamRDFLib.dataset(dsg));
        return dsg;
    }

    private void ensureAcceptHeader(String dftAcceptheader) {
        String requestAccept = header(acceptHeader(), WebContent.defaultRDFAcceptHeader);
        acceptHeader(requestAccept);
    }

    private void requestCompression() {
        if ( allowCompression )
            httpHeader(HttpNames.hAcceptEncoding, WebContent.acceptEncoding);
    }

    /**
     * POST the contents of a file using the filename extension to determine the
     * Content-Type to use if not already set.
     * <p>
     * This operation does not parse the file.
     */
    public void postDataset(String file) {
        internalDataset();
        validateDatasetOperation();
        String fileExtContentType = contentTypeFromFilename(file);
        HttpClient hc = requestHttpClient(serviceEndpoint, serviceEndpoint);
        uploadQuads(hc, serviceEndpoint, file, fileExtContentType, httpHeaders, Push.POST);
    }

    /** POST a dataset */
    public void postDataset(DatasetGraph dataset) {
        internalDataset();
        validateDatasetOperation();
        RDFFormat requestFmt = rdfFormat(HttpEnv.dftQuadsFormat);
        HttpClient hc = requestHttpClient(serviceEndpoint, serviceEndpoint);
        HttpRDF.httpPostDataset(hc, serviceEndpoint, dataset, requestFmt);
    }

    /**
     * PUT the contents of a file using the filename extension to determine the
     * Content-Type to use if not already set.
     * <p>
     * This operation does not parse the file.
     */
    public void putDataset(String file) {
        internalDataset();
        validateDatasetOperation();
        String fileExtContentType = contentTypeFromFilename(file);
        HttpClient hc = requestHttpClient(serviceEndpoint, serviceEndpoint);
        uploadQuads(hc, serviceEndpoint, file, fileExtContentType, httpHeaders, Push.PUT);
    }

    /** PUT a dataset */
    public void putDataset(DatasetGraph dataset) {
        internalDataset();
        validateDatasetOperation();
        RDFFormat requestFmt = rdfFormat(HttpEnv.dftQuadsFormat);
        HttpClient hc = requestHttpClient(serviceEndpoint, serviceEndpoint);
        HttpRDF.httpPutDataset(hc, serviceEndpoint, dataset, requestFmt);
    }

    // SPARQL "CLEAR ALL"
//    /** Clear - delete named graphs, empty the default graph */
//    public void clearDataset() {
//        validateDatasetOperation();
//        String url = serviceEndpoint;
//        HttpOp2.httpDelete(url);
//    }

    /** Send a file of triples to a URL. */
    private static void uploadTriples(HttpClient httpClient, String gspUrl, String file, String fileExtContentType,
                                      Map<String, String> headers, Push mode) {
        Lang lang = RDFLanguages.contentTypeToLang(fileExtContentType);
        if ( lang == null )
            throw new ARQException("Not a recognized as an RDF format: "+fileExtContentType);
        if ( RDFLanguages.isQuads(lang) && ! RDFLanguages.isTriples(lang) )
            throw new ARQException("Can't load quads into a graph");
        if ( ! RDFLanguages.isTriples(lang) )
            throw new ARQException("Not an RDF format: "+file+" (lang="+lang+")");
        pushFile(httpClient, gspUrl, file, fileExtContentType, headers, mode);
    }

    /**
     * Send a file of quads to a URL. The Content-Type is inferred from the file
     * extension.
     */
    private static void uploadQuads(HttpClient httpClient, String endpoint, String file, String fileExtContentType, Map<String, String> headers, Push mode) {
        Lang lang = RDFLanguages.contentTypeToLang(fileExtContentType);
        if ( !RDFLanguages.isQuads(lang) && !RDFLanguages.isTriples(lang) )
            throw new ARQException("Not an RDF format: " + file + " (lang=" + lang + ")");
        pushFile(httpClient, endpoint, file, fileExtContentType, headers, mode);
    }

    /** Header string or default value. */
    private static String header(String choice, String dftString) {
        return choice != null ? choice : dftString;
    }

    /** Choose the format to write in.
     * <ol>
     * <li> {@code rdfFormat}
     * <li> {@code contentType} setting, choosing streaming
     * <li> {@code contentType} setting, choosing pretty
     * <li> HttpEnv.dftTriplesFormat / HttpEnv.dftQuadsFormat /
     * </ol>
     */
    private RDFFormat rdfFormat(RDFFormat dftFormat) {
        if ( rdfFormat != null )
            return rdfFormat;

        if ( contentType() == null )
            return dftFormat;

        Lang lang = RDFLanguages.contentTypeToLang(contentType());
        RDFFormat streamFormat = StreamRDFWriter.defaultSerialization(null);
        if ( streamFormat != null )
            return streamFormat;
        return RDFWriterRegistry.defaultSerialization(lang);
    }

    /** Choose the Content-Type header for sending a file unless overridden. */
    private String contentTypeFromFilename(String filename) {
        String ctx = contentType();
        if ( ctx != null )
            return ctx;
        ContentType ct = RDFLanguages.guessContentType(filename);
        return ct == null ? null : ct.getContentTypeStr();
    }

    /** Send a file. fileContentType takes precedence over this.contentType.*/
    protected static void pushFile(HttpClient httpClient, String endpoint, String file, String fileContentType,
                                   Map<String, String> httpHeaders, Push style) {
        try {
            Path path = Paths.get(file);
            if ( fileContentType != null )
            //if ( ! httpHeaders.containsKey(HttpNames.hContentType) )
                httpHeaders.put(HttpNames.hContentType, fileContentType);
            BodyPublisher body = BodyPublishers.ofFile(path);
            HttpOp2.httpPushData(httpClient, style, endpoint, HttpLib.setHeaders(httpHeaders), body);
        } catch (FileNotFoundException ex) {
            throw new NotFoundException(file);
        }
    }
}
