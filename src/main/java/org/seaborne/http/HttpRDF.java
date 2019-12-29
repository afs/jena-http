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
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * HTTP level operations for RDF related tasks.
 * This does not include GSP naming which is in {@link HttpRDF}.
 */
public class HttpRDF {

    // ---- GET
    // Accept: versions.
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

    // Not API? Put pattern in documentation, with warnings!
    /**
     * GET RDF data and place in the given graph.
     * Sending straight to an existing graph carries the risk of a parse error mid-operation.
     * @throws HttpException
     */
    // Because of the issues with bad data, may not be good to have this in the "easy" API.
    // Using graph txn when the graph transaction API becomes "Transactional".
    static void execGetGraph(HttpClient httpClient, Graph graph, String url) {
        TransactionHandler th = graph.getTransactionHandler();
        if ( th.transactionsSupported() ) {
            th.execute(() -> httpGetToStream(httpClient, StreamRDFLib.graph(graph), url, WebContent.defaultGraphAcceptHeader));
        } else
            httpGetToStream(httpClient, StreamRDFLib.graph(graph), url, WebContent.defaultGraphAcceptHeader);
    }

    /**
         * Send the RDF data from the resource at the URL to the StreamRDF.
         * Beware of parse errors!
         * @throws HttpException
         */
        public static void httpGetToStream(HttpClient client, StreamRDF dest, String url, String acceptHeader) {
            HttpResponse<InputStream> response = execGetToInput(client, url, acceptHeader);
            //response.previousResponse();
            String base = determineBaseURI(url, response);
            Lang lang = determineSyntax(response, Lang.RDFXML);
            InputStream in = HttpLib.getInputStream(response);
            try ( in ) {
                RDFParser.create()
                    .base(base)
                    .source(in)
                    .lang(lang)
                    .parse(dest);
                // No need to exhaust InputStream?
            } catch (RiotParseException ex) {
    //          // Unclear: need to flush out the body unparsed junk.
    //          IO.close(x.body());
                throw ex;
            } catch (IOException e) {
                throw new HttpException(response.request().method()+" "+response.request().uri().toString(), e);
            }
        }

    // Should be same as HttpOp2 code? execGet->InputStream.
    // XXX Compression.
    static HttpResponse<InputStream> execGetToInput(HttpClient client, String url, String acceptHeader) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(url);
        URI uri = HttpLib.toURI(url);
        HttpRequest requestData = HttpOp2.newGetRequest(client, url, acceptHeader);
        HttpResponse<InputStream> response = HttpLib.execute(client, requestData, HttpLib.getBodyInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
        return response;
    }

    public static void httpPostGraph(String url, Graph graph) {
        httpPostGraph(HttpEnv.getDftHttpClient(), url, graph);
    }

    public static void httpPostGraph(HttpClient httpClient, String url, Graph graph) {
        URI uri = HttpLib.toURI(url);
        RDFFormat fmt = RDFFormat.NT;
        BodyPublisher x = graphToHttpWithLength(graph, fmt);
        HttpRequest request = HttpRequest.newBuilder()
            .POST(x)
            .uri(uri)
            .header(HttpNames.hContentType, fmt.getLang().getHeaderString())
            .build();
        HttpResponse<InputStream> response = HttpLib.execute(httpClient, request, HttpLib.getBodyInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
    }

    public static void httpPutGraph(String url, Graph graph) {
        httpPutGraph(HttpEnv.getDftHttpClient(), url, graph);
    }

    public static void httpPutGraph(HttpClient httpClient, String url, Graph graph) {
        URI uri = HttpLib.toURI(url);
        RDFFormat fmt = RDFFormat.NT;
        BodyPublisher x = graphToHttpWithLength(graph, fmt);
        HttpRequest requestData = HttpRequest.newBuilder()
            .PUT(x)
            .uri(uri)
            .header(HttpNames.hContentType, fmt.getLang().getHeaderString())
            .build();
        HttpResponse<InputStream> response = HttpLib.execute(httpClient, requestData, HttpLib.getBodyInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
    }

    public static void httpDeleteGraph(String url) {
        httpDeleteGraph(HttpEnv.getDftHttpClient(), url);
    }

    public static void httpDeleteGraph(HttpClient httpClient, String url) {
        HttpOp2.httpDelete(httpClient, url);
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

    static BodyPublisher graphToHttpWithLength(Graph graph, RDFFormat syntax) {
        String ct = syntax.getLang().getContentType().toHeaderString();
        ByteArrayOutputStream out = new ByteArrayOutputStream(128*1024);
        RDFDataMgr.write(out, graph, syntax);
        byte[] bytes = out.toByteArray();
        IO.close(out);
        // This has a Content-Length but required the entire graph being serialized
        // to know the serialization size.
        // An alternative is to stream the output but then the HTTP connection can't
        // be reused (don't know when the request finishes, only closing the connection
        // indicates that).
        return BodyPublishers.ofByteArray(bytes);
    }

}
