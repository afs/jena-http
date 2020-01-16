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
import static org.seaborne.http.HttpLib.bodyStringFetcher;
import static org.seaborne.http.HttpLib.execute;
import static org.seaborne.http.HttpLib.urlEncode;

import java.io.InputStream;
import java.net.URI;
//import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.engine.http.Params;


/**
 * This is a collection of convenience operations for HTTP requests
 * mostly in support of RDF handling and common, basic use case for HTTP.
 * It is not comprehensive.
 *
 *
 *
 * @see HttpRDF
 * @see GSP
 */
public class HttpOp2 {

    public static String httpGetString(String url) {
        return httpGetString(HttpEnv.getDftHttpClient(), url, null);
    }

    public static String httpGetString(String url, String acceptHeader) {
        return httpGetString(HttpEnv.getDftHttpClient(), url, acceptHeader);
    }

    public static String httpGetString(HttpClient httpClient, String url, String acceptHeader) {
        HttpRequest request = newGetRequest(httpClient, url, acceptHeader);
        HttpResponse<String> response = execute(httpClient, request, BodyHandlers.ofString());
        HttpLib.handleHttpStatusCode(response, bodyStringFetcher);
        return response.body();
    }

    static HttpRequest newGetRequest(HttpClient httpClient, String url, String acceptHeader) {
//        if ( acceptHeader == null )
//            acceptHeader = "*/*";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .GET().uri(HttpLib.toURI(url));
        if ( acceptHeader != null )
            builder.header(HttpNames.hAccept, acceptHeader);
        HttpRequest request = builder.build();
        return request;
    }

    /** POST - like httpGetString but uses POST - expects a response */
    public static String httpPostRtnString(String url) {
        return httpPostRtnString(HttpEnv.getDftHttpClient(), url);
    }

    /** POST - like httpGetString but uses POST - expects a response */
    public static String httpPostRtnString(HttpClient httpClient, String url) {
        HttpRequest requestData = HttpRequest.newBuilder()
            .POST(BodyPublishers.noBody())
            .uri(HttpLib.toURI(url))
            .build();
        HttpResponse<String> response = execute(httpClient, requestData, BodyHandlers.ofString());
        HttpLib.handleHttpStatusCode(response, bodyStringFetcher);
        return response.body();
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
        return execGet(httpClient, url, acceptHeader);
    }

    private static InputStream execGet(HttpClient httpClient, String url, String acceptHeader) {
        if ( acceptHeader == null )
            acceptHeader = "*/*";
        HttpRequest request = newGetRequest(httpClient, url, acceptHeader);
        return execGet(httpClient, request);
    }

    private static InputStream execGet(HttpClient httpClient, HttpRequest request) {
        HttpResponse<InputStream> response = execute(httpClient, request, BodyHandlers.ofInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
        return HttpLib.getInputStream(response);
    }

    /** POST
     * @see BodyPublishers
     * @see BodyPublishers#ofFile
     * @see BodyPublishers#ofString
     */
    public static void httpPost(String url, String contentType, BodyPublisher body) {
        httpPost(HttpEnv.getDftHttpClient(), url, contentType, body);
    }

    /** POST
     * @see BodyPublishers
     * @see BodyPublishers#ofFile
     * @see BodyPublishers#ofString
     */
    public static void httpPost(HttpClient httpClient, String url, String contentType, BodyPublisher body) {
        httpPushData(httpClient, true, url, contentType, body);
    }

    public static void httpPut(String url, String contentType, BodyPublisher body) {
        httpPut(HttpEnv.getDftHttpClient(), url, contentType, body);
    }

    /** PUT
     * <p>
     * {@link HttpEnv#getDftHttpClient()}
     * @see BodyPublishers
     * @see BodyPublishers#ofFile
     * @see BodyPublishers#ofString
     */
    public static void httpPut(HttpClient httpClient, String url, String contentType, BodyPublisher body) {
        httpPushData(httpClient, false, url, contentType, body);
    }

    /** Push data. POST or PUT request with no response body data. */
    private static void httpPushData(HttpClient httpClient, boolean isPost, String url, String contentType, BodyPublisher body) {
        URI uri = HttpLib.toURI(url);
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(uri);
        if ( isPost )
            builder.POST(body);
        else
            builder.PUT(body);
        if ( contentType != null )
            builder.header(HttpNames.hContentType, contentType);
        HttpRequest request = builder.build();
        HttpResponse<String> response = execute(httpClient, request, BodyHandlers.ofString());
        HttpLib.handleHttpStatusCode(response, bodyStringFetcher);
    }

    // POST form - probably not needed in this convenience class.
    // Retain for reference.

    /*package*/ static HttpResponse<InputStream> httpPostForm(String url, Params params, String acceptString) {
        return httpPostForm(HttpEnv.getDftHttpClient(), url, params, acceptString);
    }

    /*package*/  static HttpResponse<InputStream> httpPostForm(HttpClient httpClient, String url, Params params, String acceptString) {
        Objects.requireNonNull(url);
        acceptString = HttpLib.dft(acceptString, "*/*");
        URI uri = HttpLib.toURI(url);
        String formData =
            params.pairs().stream()
                .map(p->urlEncode(p.getLeft())+"="+urlEncode(p.getRight()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .POST(BodyPublishers.ofString(formData))
            .header(HttpNames.hContentType, WebContent.contentTypeHTMLForm)
            .header(HttpNames.hAccept, acceptString)
            .build();

        HttpResponse<InputStream> response = execute(httpClient, request, BodyHandlers.ofInputStream());
        HttpLib.handleHttpStatusCode(response, bodyInputStreamToString);
        return response;
    }

    /** DELETE */
    public static void httpDelete(String url) {
        httpDelete(HttpEnv.getDftHttpClient(), url);
    }

    /** DELETE */
    public static void httpDelete(HttpClient httpClient, String url) {
        URI uri = HttpLib.toURI(url);
        HttpRequest requestData = HttpRequest.newBuilder()
            .DELETE()
            .uri(uri)
            .build();
        HttpResponse<String> response = execute(httpClient, requestData, BodyHandlers.ofString());
        HttpLib.handleHttpStatusCode(response, bodyStringFetcher);
    }

    private static String bodyString(ResponseInfo responseInfo) {
        try {
            BodySubscriber<InputStream> bodySubscriber = BodySubscribers.ofInputStream();
            return bodyString(responseInfo.headers(), bodySubscriber.getBody().toCompletableFuture().get());
        } catch (InterruptedException | ExecutionException e) {
            throw new HttpException("Error capturing body of "+responseInfo.statusCode(), e);
        }
    }

    private static String bodyString(HttpHeaders httpHeaders, InputStream input) {
        // XXX Missing : Conneg on MIME type/charset
        byte[] bytes = IO.readWholeFile(input);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // OPTIONS
    // ------------------------------------------------------------------------

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
}
