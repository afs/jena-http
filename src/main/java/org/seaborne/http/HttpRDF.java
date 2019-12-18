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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.TransactionHandler;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.web.HttpSC;


/** This is the HTTP engine for RDF handling.
 *  It does not cover the  GSP naming (?default, ?graph=).
 */
public class HttpRDF {
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
        httpGetGraph(httpClient, graph, url);
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
    private static void httpGetGraph(HttpClient httpClient, Graph graph, String url) {
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
        HttpResponse<InputStream> response = httpGetToInput(client, url, acceptHeader);
        //response.previousResponse();
        String base = determineBaseURI(url, response);
        Lang lang = determineSyntax(response, Lang.RDFXML);
        try ( InputStream in = response.body() ) {
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

    private static HttpResponse<InputStream> httpGetToInput(HttpClient client, String url, String acceptHeader) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(url);
        Objects.requireNonNull(acceptHeader);
        URI uri = toURI(url);
        HttpRequest requestData = HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .header("Accept", acceptHeader)
            .build();
        HttpResponse<InputStream> response = execute(client, requestData, HttpEnv.getBodyInputStream());
        return response;

    }

    // PUT
    // POST
    // DELETE
    // OPTIONS

    public static void httpPostGraph(String url, Graph graph) {
        httpPostGraph(HttpEnv.getDftHttpClient(), url, graph);
    }

    public static void httpPostGraph(HttpClient httpClient, String url, Graph graph) {
        URI uri = toURI(url);
        RDFFormat fmt = RDFFormat.NT;
        BodyPublisher x = graphToHttpWithLength(graph, fmt);
        HttpRequest requestData = HttpRequest.newBuilder()
            .POST(x)
            .uri(uri)
            .header(HttpNames.hContentType, fmt.getLang().getHeaderString())
            .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(requestData, HttpEnv.getBodyInputStream());
            bodyString(response.headers(), response.body());
            //return body;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // HttpOp
    public static String httpGetString(String url) {
        return httpGetString(HttpEnv.getDftHttpClient(), url);
    }

    public static String httpGetString(HttpClient httpClient, String url) {
        HttpRequest requestData = HttpRequest.newBuilder()
            .GET()
            .uri(toURI(url))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(requestData, BodyHandlers.ofString());
            handleHttpStatusCode(response, bodyFetcher1);
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new HttpException(e);
        }
    }

    /** MUST close the InputStream */
    public static InputStream httpGet(String url) {
        return httpGet(HttpEnv.getDftHttpClient(), url);
    }

    /** MUST close the InputStream */
    public static InputStream httpGet(String url, String acceptHeader) {
        return httpGet(HttpEnv.getDftHttpClient(), url, acceptHeader);
    }

    /** MUST close the InputStream */
    public static InputStream httpGet(HttpClient httpClient, String url) {
        return httpGet(httpClient, url, null);
    }

    /** MUST close the InputStream */
    public static InputStream httpGet(HttpClient httpClient, String url, String acceptHeader) {
        return getWorker(httpClient, url, acceptHeader);
    }

    private static Function<HttpResponse<String>, String> bodyFetcher1 = r-> {
        String msg = r.body();
        if ( msg != null && msg.isEmpty() )
            return null;
        return msg;
    };

    private static Function<HttpResponse<InputStream>, String> bodyFetcher2 = r-> {
        InputStream in = r.body();
        String msg;
        try {
            msg = IO.readWholeFileAsUTF8(in);
            if ( msg.isEmpty() )
                return null;
            return msg;
        } catch (IOException e) { throw new HttpException(e); }
    };

    private static InputStream getWorker(HttpClient httpClient, String url, String acceptHeader) {
        if ( acceptHeader == null )
            acceptHeader = "*/*";
        HttpRequest requestData = HttpRequest.newBuilder()
            .GET()
            .header(HttpNames.hAccept, acceptHeader)
            .uri(toURI(url))
            .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(requestData, BodyHandlers.ofInputStream());
            handleHttpStatusCode(response, bodyFetcher2);
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new HttpException(e);
        }
    }

    // ------------------------------------------------------------------------

//    // Send file.
//

    // SattusCode to exception.
    private static <T> void handleHttpStatusCode(HttpResponse<T> response, Function<HttpResponse<T>, String> bodyFetcher) {
        int httpStatusCode = response.statusCode();
        // No status message in HTTP/2.
        //HttpHeaders headers = responseInfo.headers();
        //headers.map().forEach((x,y) -> System.out.printf("%-20s %s\n", x, y));
        if ( ! inRange(httpStatusCode, 100, 599) )
            throw new HttpException("Status code out of range: "+httpStatusCode);
        else if ( inRange(httpStatusCode, 100, 199) ) {
            // Informational
        }
        else if ( inRange(httpStatusCode, 200, 299) ) {
            // Success. Continue processing.
        }
        else if ( inRange(httpStatusCode, 300, 399) ) {
            // We had follow redirects on (default client) so it's http->https,
            // or the application passed on a HttpClient with redirects off.
            // Either way, we should not continue processing.
            try {
                BodySubscribers.discarding().getBody().toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new HttpException("Error discarding body of "+httpStatusCode , e);
            }
            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), null);
        }
        else if ( inRange(httpStatusCode, 400, 499) ) {
            String msg = bodyFetcher.apply(response);
            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), msg);
        }
        else if ( inRange(httpStatusCode, 500, 599) ) {
            String msg = bodyFetcher.apply(response);
            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), msg);
        }
    }


    private static String bodyString(ResponseInfo responseInfo) {
        try {
            BodySubscriber<InputStream> x = BodySubscribers.ofInputStream();
            return bodyString(responseInfo.headers(), x.getBody().toCompletableFuture().get());
        } catch (InterruptedException | ExecutionException e) {
            throw new HttpException("Error capturing body of "+responseInfo.statusCode(), e);
        }
    }

    private static String bodyString(HttpHeaders httpHeaders, InputStream input) {
        // XXX Missing : Conneg on MIME type/charset
        byte[] bytes = IO.readWholeFile(input);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static URI toURI(String uriStr) {
        try {
            return new URI(uriStr);
        } catch (URISyntaxException ex) {
            int idx = ex.getIndex();
            String msg = (idx<0)
                ? String.format("Bad URL: %s", uriStr)
                : String.format("Bad URL: %s starting at character %d", uriStr, idx);
            throw new HttpException(msg, ex);
        }
    }

    // PUT
    // POST
    // DELETE
    // OPTIONS

    private static BodyPublisher graphToHttpWithLength(Graph graph, RDFFormat syntax) {
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

    private static HttpResponse<InputStream> execute(HttpClient httpClient, HttpRequest httpRequest, BodyHandler<InputStream> bodyHandler) {
        HttpResponse<InputStream> x;
        try {
            x = httpClient.send(httpRequest, HttpEnv.getBodyInputStream());
        } catch (IOException | InterruptedException e) {
            throw new HttpException(httpRequest.method()+" "+httpRequest.uri().toString(), e);
        }
        return x;
    }

    /** Test x:int in [min, max] */
    private static boolean inRange(int x, int min, int max) { return min < x && x <= max; }

    /**
     * Content-Type, without charset.
     * <p>
     * RDF formats are either UTF-8 or XML , where the charset is determined by the
     * processing instruction at the start of the content. Parsing is on byte
     * streams.
     */
    private static <T> String determineContentType(HttpResponse<T> response) {
        String ctStr = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        if ( ctStr != null ) {
            int i = ctStr.indexOf(';');
            if ( i >= 0 )
                ctStr = ctStr.substring(0, i);
        }
        return ctStr;
    }

    /** RDF {@link Lang}. */
    private static <T> Lang determineSyntax(HttpResponse<T> response, Lang dftSyntax) {
        String ctStr = response.headers().firstValue(HttpNames.hContentType).orElse(null);
        if ( ctStr != null ) {
            int i = ctStr.indexOf(';');
            if ( i >= 0 )
                ctStr = ctStr.substring(0, i);
        }
        Lang lang = RDFLanguages.contentTypeToLang(ctStr);
        return lang != null ? lang : dftSyntax;
    }

    private static <T> String determineBaseURI(String url, HttpResponse<T> response) {
        // RFC 7231: 3.1.4.2. and Appendix B: Content-Location does not affect base URI. // SKW.
        URI uri = response.uri();
        return uri.toString();
    }
}
