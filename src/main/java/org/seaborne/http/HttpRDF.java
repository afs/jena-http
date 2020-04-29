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

import static org.seaborne.http.HttpLib.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Objects;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.TransactionHandler;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * HTTP level operations for RDF related tasks.
 * This does not include GSP naming which is in {@link HttpRDF}.
 */
public class HttpRDF {

    // ---- GET
    /**
     * GET a graph from a URL
     * @throws HttpException
     */
    public static Graph httpGetGraph(String url) {
        return httpGetGraph(HttpEnv.getDftHttpClient(), url);
    }

    /**
     * GET a graph from a URL using the {@link HttpClient} provided.
     *
     * @throws HttpException
     */
    public static Graph httpGetGraph(HttpClient httpClient, String url) {
        Graph graph = GraphFactory.createDefaultGraph();
        execGetGraph(httpClient, graph, url);
        return graph;
    }

    /**
     * GET a graph from a URL using the {@link HttpClient} provided
     * and the "Accept" header.
     *
     * @throws HttpException
     */
    public static Graph httpGetGraph(HttpClient httpClient, String url, String acceptHeader) {
        Graph graph = GraphFactory.createDefaultGraph();
        execGetGraph(httpClient, graph, url, acceptHeader);
        return graph;
    }

    // Not API? Put pattern in documentation, with warnings!
    /**
     * GET RDF data and place in the given graph.
     * Sending straight to an existing graph carries the risk of a parse error mid-operation.
     * @throws HttpException
     */
    // Because of the issues with bad data, may not be good to have this in the "easy" API.
    // Using graph txn when the graph transaction API becomes "Transactional".
    private static void execGetGraph(HttpClient httpClient, Graph graph, String url) {
        execGetGraph(httpClient, graph, url, WebContent.defaultGraphAcceptHeader);
    }

    private static void execGetGraph(HttpClient httpClient, Graph graph, String url, String acceptHeader) {
        TransactionHandler th = graph.getTransactionHandler();
        if ( th.transactionsSupported() ) {
            th.execute(() -> httpGetToStream(httpClient, StreamRDFLib.graph(graph), url, acceptHeader));
        } else
            httpGetToStream(httpClient, StreamRDFLib.graph(graph), url, acceptHeader);
    }

    /**
     * Send the RDF data from the resource at the URL to the StreamRDF.
     * Beware of parse errors!
     * @throws HttpException
     */
    public static void httpGetToStream(HttpClient client, StreamRDF dest, String url, String acceptHeader) {
        if ( acceptHeader == null )
            acceptHeader = "*/*";
        HttpResponse<InputStream> response = execGetToInput(client, url, acceptHeader);
        String base = determineBaseURI(url, response);
        Lang lang = determineSyntax(response, Lang.RDFXML);
        try (InputStream in = getInputStream(response)) {
            try {
                RDFParser.create()
                    .base(base)
                    .source(in)
                    .lang(lang)
                    .parse(dest);
            } catch (RiotParseException ex) {
                // We only read part of the input stream.
                finish(response);
                throw ex;
            }
        } catch (IOException e) {
            throw new HttpException(response.request().method() + " " + response.request().uri().toString(), e);
        }
    }

    // MUST close the input stream
    private static HttpResponse<InputStream> execGetToInput(HttpClient client, String url, String acceptHeader) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(url);
        HttpRequest requestData = HttpOp2.newGetRequest(client, url, acceptHeader);
        HttpResponse<InputStream> response = HttpLib.execute(client, requestData);
        handleHttpStatusCode(response);
        return response;
    }


    public static void httpPostGraph(String url, Graph graph) {
        httpPostGraph(HttpEnv.getDftHttpClient(), url, graph, HttpEnv.dftTriplesFormat);
    }

    public static void httpPostGraph(HttpClient httpClient, String url, Graph graph, RDFFormat format) {
        postGraph(httpClient, url, graph, format);
    }

    public static void httpPostDataset(HttpClient httpClient, String url, DatasetGraph dataset, RDFFormat format) {
        postDataset(httpClient, url, dataset, format);
    }

    private static void postGraph(HttpClient httpClient, String url, Graph graph, RDFFormat format) {
        BodyPublisher x = graphToHttpBody(graph, format);
        postBody(httpClient, url, x, format);
    }

    private static void postDataset(HttpClient httpClient, String url, DatasetGraph dataset, RDFFormat format) {
        BodyPublisher x = datasetToHttpBody(dataset, format);
        postBody(httpClient, url, x, format);
    }

    private static void postBody(HttpClient httpClient, String url, BodyPublisher x, RDFFormat format) {
        String contentType = format.getLang().getHeaderString();
        HttpOp2.httpPost(url, contentType, x);
//        URI uri = toRequestURI(url);
//        String contentType = format.getLang().getHeaderString();
//        HttpRequest request = HttpRequest.newBuilder()
//            .POST(x)
//            .uri(uri)
//            .header(HttpNames.hContentType, contentType)
//            .build();
//        HttpResponse<InputStream> response = HttpLib.execute(httpClient, request);
//        handleResponseNoBody(response);
    }

    public static void httpPutGraph(String url, Graph graph) {
        httpPutGraph(HttpEnv.getDftHttpClient(), url, graph, HttpEnv.dftTriplesFormat);
    }

    public static void httpPutGraph(HttpClient httpClient, String url, Graph graph, RDFFormat fmt) {
        putGraph(httpClient, url, graph, fmt);
    }

    public static void httpPutDataset(HttpClient httpClient, String url, DatasetGraph dataset, RDFFormat format) {
        putDataset(httpClient, url, dataset, format);
    }

    private static void putGraph(HttpClient httpClient, String url, Graph graph, RDFFormat format) {
        BodyPublisher x = graphToHttpBody(graph, format);
        putBody(httpClient, url, x, format);
    }

    private static void putDataset(HttpClient httpClient, String url, DatasetGraph dataset, RDFFormat format) {
        BodyPublisher x = datasetToHttpBody(dataset, format);
        putBody(httpClient, url, x, format);
    }

    private static void putBody(HttpClient httpClient, String url, BodyPublisher x, RDFFormat format) {
        String contentType = format.getLang().getHeaderString();
        HttpOp2.httpPut(url, contentType, x);
    }

    public static void httpDeleteGraph(String url) {
        httpDeleteGraph(HttpEnv.getDftHttpClient(), url);
    }

    public static void httpDeleteGraph(HttpClient httpClient, String url) {
        URI uri = toRequestURI(url);
        HttpRequest requestData = HttpRequest.newBuilder()
            .DELETE()
            .uri(uri)
            .build();
        HttpResponse<InputStream> response = execute(httpClient, requestData);
        handleResponseNoBody(response);
    }

    /** RDF {@link Lang}. */
    static <T> Lang determineSyntax(HttpResponse<T> response, Lang dftSyntax) {
        String ctStr = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        if ( ctStr != null ) {
            int i = ctStr.indexOf(';');
            if ( i >= 0 )
                ctStr = ctStr.substring(0, i);
        }
        Lang lang = RDFLanguages.contentTypeToLang(ctStr);
        return dft(lang, dftSyntax);
    }

    static <T> String determineBaseURI(String url, HttpResponse<T> response) {
        // RFC 7231: 3.1.4.2. and Appendix B: Content-Location does not affect base URI. // SKW.
        URI uri = response.uri();
        return uri.toString();
    }

    // This sets Content-Length but requires the entire graph being serialized
    // to get the serialization size.
    //
    // An alternative is to stream the output but then the HTTP connection can't
    // be reused (don't know when the request finishes, only closing the connection
    // indicates that).

    static BodyPublisher graphToHttpBody(Graph graph, RDFFormat syntax) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(128*1024);
        RDFDataMgr.write(out, graph, syntax);
        byte[] bytes = out.toByteArray();
        IO.close(out);
        return BodyPublishers.ofByteArray(bytes);
    }

    /*package*/ static BodyPublisher datasetToHttpBody(DatasetGraph dataset, RDFFormat syntax) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(128*1024);
        RDFDataMgr.write(out, dataset, syntax);
        byte[] bytes = out.toByteArray();
        IO.close(out);
        return BodyPublishers.ofByteArray(bytes);
    }
}
