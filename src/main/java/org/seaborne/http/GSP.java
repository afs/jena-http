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

import java.io.FileNotFoundException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.shared.NotFoundException;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

/**
 * Client for the
 * <a href="https://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store Protocol</a>.
 * <p>
 * This is extended to include operations GET, POST and PUT on datasets.
 * <p>
 * Examples:
 * <pre>
 *   // Get the default graph.
 *   Graph graph = GSP.service("http://example/dataset").GET();
 * </pre>
 * <pre>
 *   // Get a named graph.
 *   Graph graph = GSP.service("http://example/dataset").GET("http://my/graph");
 * </pre>
 * <pre>
 *   // POST (add) to a named graph.
 *   Graph myData = ...;
 *   GSP.service("http://example/dataset").POST("http://my/graph", myData);
 * </pre>
 */
public class GSP {

    /**
     * Return the URL for a graph named using the
     * <a href="https://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL 1.1 Graph Store Protocol</a>.
     * The {@code graphStoreProtocolService} is the network location of the graph store.
     * The {@code graphName} be a valid, absolute URI (i.e. includes the scheme) or
     * the words "default" (or null) for the defaul graph of the store.
     *
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
        return graphStore + queryStringForGraph(ch, graphName) ;
    }

    private static String dftName =  "default" ;

    /*package*/ static boolean isDefault(String name) {
        return name == null || name.equals(dftName) ;
    }

    private static String queryStringForGraph(String initialSepChar, String graphName) {
        return
            initialSepChar + (isDefault(graphName)
                ? "default"
                : "graph="+graphName) ;
    }

    private static String queryStringForGraph(String graphName) {
        return
            isDefault(graphName)
                ? "default"
                : "graph="+HttpLib.urlEncodeQueryString(graphName);
    }

    private String              serviceEndpoint = null;
    private String              graphName       = null;
    // XXX Currently done per-operations. Could use headers or control specially.
//        private String              acceptHeader    = null;
//        private String              contentType     = null;
    private HttpClient          httpClient      = HttpEnv.getDftHttpClient();
    private Map<String, String> httpHeaders     = null;
    private boolean             allowCompression = false;
    private boolean             defaultGraph = false;

    /** Create a request to the remote service (without GSP naming).
     *  Call {@link #defaultGraph()} or {@link #graphName(String)} to select the target graph.
     * @param service
     */
    public static GSP request(String service) {
        return new GSP().service(service);
    }

    private GSP() {}

    /** Set the URL of the query endpoint.
     *  This replaces any value set in the {@link #request(String)} call. */
    public GSP service(String serviceURL) {
        this.serviceEndpoint = Objects.requireNonNull(serviceURL);
        return this;
    }

    public GSP httpHeader(String headerName, String headerValue) {
        Objects.requireNonNull(headerName);
        Objects.requireNonNull(headerValue);
        this.httpHeaders.put(headerName, headerValue);
        return this;
    }

//    public GSP allowCompression(boolean allowCompression) {
//        this.allowCompression = allowCompression;
//        return this;
//    }

    public GSP graphName(String graphName) {
        this.graphName = graphName;
        this.defaultGraph = false;
        return this;
    }

    public GSP defaultGraph() {
        this.graphName = null;
        this.defaultGraph = true;
        return this;
    }

    private void validateGraphOperation() {
        Objects.requireNonNull(serviceEndpoint);
        if ( ! defaultGraph && graphName == null )
            throw new ARQException("Nedd either default graph or a graph name");
    }

    private void validateDatasetOperation() {
        Objects.requireNonNull(serviceEndpoint);
        if ( defaultGraph || graphName != null )
            throw new ARQException("Default graph or a graph name specified for dataset operation");
    }

    /** Get a graph */
    public Graph GET() {
        return GET(WebContent.defaultGraphAcceptHeader);
    }

    /** Get a graph using the accept header provided. */
    public Graph GET(String acceptHeader) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        Graph graph = HttpRDF.httpGetGraph(httpClient, url, acceptHeader);
        return graph;
    }

    /**
     * POST the contents of a file using the filename extension to termine the
     * Content-Type to use.
     * <p>
     * This operation does not parse the file.
     */
    public void POST(String file) {
        String contentType = RDFLanguages.guessContentType(file).getContentTypeStr();
        POST(file, contentType);
    }

    /**
     * POST the content of a file using the given Content-Type.
     * <p>
     * This operation does not parse the file.
     */
    public void POST(String file, String contentType) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        uploadTriples(httpClient, url, file, contentType, Push.POST);
    }

    /** POST a graph. */
    public void POST(Graph graph) {
        POST(graph, HttpEnv.dftTriplesFormat);
    }

    /** POST a graph, using the given {@link RDFFormat} as the serialization. */
    public void POST(Graph graph, RDFFormat format) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        HttpRDF.httpPostGraph(httpClient, url, graph, format);
    }

    /**
     * PUT the contents of a file using the filename extension to determine the
     * Content-Type to use.
     * <p>
     * This operation does not parse the file.
     */
    public void PUT(String file) {
        String contentType = RDFLanguages.guessContentType(file).getContentTypeStr();
        PUT(file, contentType);
    }

    /**
     * POST the content of a file using the given Content-Type.
     * <p>
     * This operation does not parse the file.
     */
    public void PUT(String file, String contentType) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        uploadTriples(httpClient, url, file, contentType, Push.PUT);
    }

    /** PUT a graph. */
    public void PUT(Graph graph) {
        PUT(graph, HttpEnv.dftTriplesFormat);
    }

    /** PUT a graph, using the given {@link RDFFormat} as the serialization. */
    public void PUT(Graph graph, RDFFormat format) {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        HttpRDF.httpPutGraph(httpClient, url, graph, format);
    }

    /** Delete a graph. */
    public void DELETE() {
        validateGraphOperation();
        String url = HttpLib.requestURL(serviceEndpoint, queryStringForGraph(graphName));
        HttpRDF.httpDeleteGraph(url);
    }

    

    //Dataset.

    /**
     * GET dataset.
     * <p>
     * If the remote end is a graph, the result is a dataset with that
     * graph data in the default graph of the dataset.
     */
    public DatasetGraph getDataset() {
        // Triples or quads
        return getDataset(WebContent.defaultRDFAcceptHeader);
    }

    /**
     * GET dataset, using the provided "Accept" header. See {@link #getDataset()} which uses a general purpose setting.
     * <p>
     * If the remote end is a graph, the result is a dataset with that
     * graph data in the default graph of the dataset.
     */
    public DatasetGraph getDataset(String acceptHeader) {
        validateDatasetOperation();
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        HttpRDF.httpGetToStream(httpClient, StreamRDFLib.dataset(dsg), serviceEndpoint, acceptHeader);
        return dsg;
    }

    /**
     * POST the contents of a file using the filename extension to determine the
     * Content-Type to use.
     * <p>
     * This operation does not parse the file.
     */
    public void postDataset(String file) {
        String contentType = RDFLanguages.guessContentType(file).getContentTypeStr();
        postDataset(file, contentType);
    }

    /**
     * POST the contents of a file.
     * <p>
     * This operation does not parse the file.
     */
    public void postDataset(String file, String contentType) {
        validateDatasetOperation();
        uploadQuads(httpClient, serviceEndpoint, file, contentType, Push.POST);
    }

    /** POST a dataset */
    public void postDataset(DatasetGraph dataset) {
        postDataset(dataset, HttpEnv.dftQuadsFormat);
    }

    /** POST a dataset, using the RDFFormat for the content type. */
    public void postDataset(DatasetGraph dataset, RDFFormat format) {
        validateDatasetOperation();
        HttpRDF.httpPostDataset(httpClient, serviceEndpoint, dataset, format);
    }

    /**
     * PUT the contents of a file using the filename extension to determine the
     * Content-Type to use.
     * <p>
     * This operation does not parse the file.
     */
    public void putDataset(String file) {
        String contentType = RDFLanguages.guessContentType(file).getContentTypeStr();
        putDataset(file, contentType);
    }

    /**
     * PUT the contents of a file.
     * <p>
     * This operation does not parse the file.
     */
    public void putDataset(String file, String contentType) {
        validateDatasetOperation();
        uploadQuads(httpClient, serviceEndpoint, file, contentType, Push.PUT);
    }

    /** PUT a dataset */
    public void putDataset(DatasetGraph dataset) {
        putDataset(dataset, HttpEnv.dftQuadsFormat);
    }

    /** PUT a dataset, using the RDFFormat for the content type. */
    public void putDataset(DatasetGraph dataset, RDFFormat format) {
        validateDatasetOperation();
        HttpRDF.httpPutDataset(httpClient, serviceEndpoint, dataset, format);
    }

    /** Send a file of triples to a URL. The Content-Type is inferred from the file extension. */
    private static void uploadTriples(HttpClient httpClient, String gspUrl, String file, String contentType, Push mode) {
        Lang lang = RDFLanguages.contentTypeToLang(contentType);
        if ( RDFLanguages.isQuads(lang) )
            throw new ARQException("Can't load quads into a graph");
        if ( ! RDFLanguages.isTriples(lang) )
            throw new ARQException("Not an RDF format: "+file+" (lang="+lang+")");
        pushFile(httpClient, gspUrl, file, contentType, mode);
    }

    /**
     * Send a file of quads to a URL. The Content-Type is inferred from the file
     * extension.
     */
    private static void uploadQuads(HttpClient httpClient, String endpoint, String file, String contentType, Push mode) {
        Lang lang = RDFLanguages.contentTypeToLang(contentType);
        if ( !RDFLanguages.isQuads(lang) && !RDFLanguages.isTriples(lang) )
            throw new ARQException("Not an RDF format: " + file + " (lang=" + lang + ")");
        pushFile(httpClient, endpoint, file, contentType, mode);
    }

    /** Send a file. The Content-Type is taken from the given {@code Lang}. */
    private static void doPutPost(HttpClient httpClient, String url, String file, Lang lang, Push style) {
        String contentType = lang.getContentType().getContentTypeStr();
        pushFile(httpClient, url, file, contentType, style);
    }

    /** Send a file. */
    protected static void pushFile(HttpClient httpClient, String endpoint, String file, String contentType, Push style) {
        try {
            Path path = Paths.get(file);
            BodyPublisher body = BodyPublishers.ofFile(path);
            HttpOp2.httpPushData(httpClient, style, file, contentType, body);
        } catch (FileNotFoundException ex) {
            throw new NotFoundException(file);
        }
    }
}
