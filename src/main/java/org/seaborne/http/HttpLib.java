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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.web.HttpSC;
import org.seaborne.http.ServiceRegistry.ServiceTuning;

/**
 * Operations related to SPARQL HTTP request - Query, Update and Graph Store protocols.
 */
public class HttpLib {

    public static BodyHandler<Void> noBody() { return BodyHandlers.discarding(); }

    public static BodyPublisher stringBody(String str) { return BodyPublishers.ofString(str); }

    private static BodyHandler<InputStream> bodyHandlerInputStream = buildDftBodyHandlerInputStream();

    private static BodyHandler<InputStream> buildDftBodyHandlerInputStream() {
        return responseInfo -> {
            return BodySubscribers.ofInputStream();
        };
    }

    /** Read the body of a response as a string in UTF-8. */
    private static Function<HttpResponse<InputStream>, String> bodyInputStreamToString = r-> {
        InputStream in = r.body();
        String msg;
        try {
            msg = IO.readWholeFileAsUTF8(in);
            // Convert no body, no Content-Length to null.
//            if ( msg.isEmpty() ) {
//                if ( r.headers().firstValue(HttpNames.hContentLength).isEmpty() )
//                    // No Content-Length -> null
//                    return null;
//            }
            return msg;
        } catch (IOException e) { throw new HttpException(e); }
    };

//    /*package*/ static String asString(HttpResponse<InputStream> response) {
//        return bodyInputStreamToString.apply(response);
//    }

    /** Calculate basic auth header. */
    public static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the InputStream from an HttpResponse, handling possible compression settings.
     * The application must consume or close the {@code InputStream} (see {@link #finish(InputStream)}).
     * Closing the InputStream may close the HTTP connection.
     */
    private static InputStream getInputStream(HttpResponse<InputStream> httpResponse) {
        String encoding = httpResponse.headers().firstValue("Content-Encoding").orElse("");
        InputStream responseInput = httpResponse.body();
        try {
            switch (encoding) {
                case "" :
                case "identity" : // Proper name for no compression.
                    return responseInput;
                case "gzip" :
                    return new GZIPInputStream(responseInput, 2*1024);
                case "inflate" :
                    return new InflaterInputStream(responseInput);
                case "br" : // RFC7932
                default :
                    throw new UnsupportedOperationException("Not supported: Content-Encoding: " + encoding);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Deal with status code and any error message sent as a body in the response.
     * <p>
     * It is this handling 4xx/5xx error messages in the body that forces the use of
     * {@code InputStream}, not generic {@code T}. We don't know until we see the
     * status code how we are going to process the response body.
     * <p>
     * Exits normally without processing the body if the response is 200.
     * <p>
     * Throws {@link HttpException} for 3xx (redirection should have happened by
     * now), 4xx and 5xx, having consumed the body input stream.
     */
    static void handleHttpStatusCode(HttpResponse<InputStream> response) {
        int httpStatusCode = response.statusCode();
        // There is no status message in HTTP/2.
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
                HttpLib.finish(response.body());
                //BodySubscribers.discarding().getBody().toCompletableFuture().get();
            //} catch (InterruptedException | ExecutionException | IOException ex) {
            } catch (Exception ex) {
                throw new HttpException("Error discarding body of "+httpStatusCode , ex);
            }
            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), null);
        }
        else if ( inRange(httpStatusCode, 400, 499) ) {
            throw exception(response, httpStatusCode);
        }
        else if ( inRange(httpStatusCode, 500, 599) ) {
            throw exception(response, httpStatusCode);
        }
    }

    /**
     * Handle the HTTP response and return the body {@code InputStream} if a 200.
     * Otherwise, throw an {@link HttpExpection}.
     * @param response
     * @return InputStream
     */
    static InputStream handleResponseInputStream(HttpResponse<InputStream> httpResponse) {
        handleHttpStatusCode(httpResponse);
        return getInputStream(httpResponse);
    }

    /**
     * Handle the HTTP response and consume the body if a 200.
     * Otherwise, throw an {@link HttpExpection}.
     * @param response
     */
    static void handleResponseNoBody(HttpResponse<InputStream> response) {
        handleHttpStatusCode(response);
        finish(response);
    }

    /**
     * Handle the HTTP response and read the body to produce a string if a 200.
     * Otherwise, throw an {@link HttpExpection}.
     * @param response
     * @return String
     */
    static String handleResponseRtnString(HttpResponse<InputStream> response) {
        InputStream input = handleResponseInputStream(response);
        try {
            String string = IO.readWholeFileAsUTF8(input);
            // Convert no body, no Content-Length to null.
//            if ( msg.isEmpty() ) {
//                if ( r.headers().firstValue(HttpNames.hContentLength).isEmpty() )
//                    // No Content-Length -> null
//                    return null;
//            }
            // Finished, don't close.
            return string;
        } catch (IOException e) { throw new HttpException(e); }
    }

    static HttpException exception(HttpResponse<InputStream> response, int httpStatusCode) {
        InputStream in = response.body();
        String msg = null;
        try {
            msg = IO.readWholeFileAsUTF8(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), msg);
    }

    /** Test x:int in [min, max] */
    private static boolean inRange(int x, int min, int max) { return min <= x && x <= max; }

    /** Finish with {@code HttpResponse<InputStream>}.
     * This read and drops any remaining bytes in the response body.
     * {@code close} may close the underlying HTTP connection.
     *  See {@link BodySubscribers#ofInputStream()}.
     */
    /*package*/ static void finish(HttpResponse<InputStream> response) {
        finish(response.body());
    }

    /** Read to end of {@link InputStream}.
     *  {@code close} may close the underlying HTTP connection.
     *  See {@link BodySubscribers#ofInputStream()}.
     */
    /*package*/ static void finish(InputStream input) {
        consume(input);
    }

    // This is extracted from commons-io, IOUtils.skip.
    // Changes:
    // * No exception.
    // * Always consumes to the end of stream (or stream throws IOException)
    // * Larger buffer
    private static int SKIP_BUFFER_SIZE = 8*1024;
    private static byte[] SKIP_BYTE_BUFFER = null;

    private static void consume(final InputStream input) {
        /*
         * N.B. no need to synchronize this because: - we don't care if the buffer is created multiple times (the data
         * is ignored) - we always use the same size buffer, so if it it is recreated it will still be OK (if the buffer
         * size were variable, we would need to synch. to ensure some other thread did not create a smaller one)
         */
        if (SKIP_BYTE_BUFFER == null) {
            SKIP_BYTE_BUFFER = new byte[SKIP_BUFFER_SIZE];
        }
        int bytesRead = 0; // Informational
        try {
            for(;;) {
                // See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
                final long n = input.read(SKIP_BYTE_BUFFER, 0, SKIP_BUFFER_SIZE);
                if (n < 0) { // EOF
                    break;
                }
                bytesRead += n;
            }
        } catch (IOException ex) { /*ignore*/ }
    }

    /** String to {@link URI}. Throws {@link HttpException} on bad syntax or if the URI isn't absolute. */
    static URI toRequestURI(String uriStr) {
        try {
            URI uri = new URI(uriStr);
            if ( ! uri.isAbsolute() )
                throw new HttpException("Not an absolute URL: "+uriStr);
            return uri;
        } catch (URISyntaxException ex) {
            int idx = ex.getIndex();
            String msg = (idx<0)
                ? String.format("Bad URL: %s", uriStr)
                : String.format("Bad URL: %s starting at character %d", uriStr, idx);
            throw new HttpException(msg, ex);
        }
    }

    static <X> X dft(X value, X dftValue) {
        return (value != null) ? value : dftValue;
    }

    static <X> List<X> copyArray(List<X> array) {
        if ( array == null )
            return null;
        return new ArrayList<>(array);
    }

    /** Encode a string suitable for use in an URL query string */
    public static String urlEncodeQueryString(String str) {
        // java.net.URLEncoder is excessive - it encodes / and : which
        // is not necessary in a query string or fragement.
        return IRILib.encodeUriQueryFrag(str);
    }

    /** Query string is assumed to already be encoded. */
    static String requestURL(String url, String queryString) {
        String sep =  url.contains("?") ? "&" : "?";
        String requestURL = url+sep+queryString;
        return requestURL;
    }

//    /*package*/ static Builder xnewBuilder(String url, boolean allowCompression, long readTimeout, TimeUnit readTimeoutUnit) {
//        return newBuilder(url, null, allowCompression, readTimeout, readTimeoutUnit);
//    }

    /*package*/ static Builder newBuilder(String url, Map<String, String> httpHeaders, boolean allowCompression, long readTimeout, TimeUnit readTimeoutUnit) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        headers(builder, httpHeaders);
        builder.uri(toRequestURI(url));
        if ( readTimeout >= 0 )
            builder.timeout(Duration.ofMillis(readTimeoutUnit.toMillis(readTimeout)));
        if ( allowCompression )
            builder.header(HttpNames.hAcceptEncoding, "gzip,inflate");
        return builder;
    }

    /** Set the headers from the Map if the map is not null. Returns the Builder. */
    static Builder headers(Builder builder, Map<String, String> httpHeaders) {
        if ( httpHeaders != null )
            httpHeaders.forEach(builder::header);
        return builder;
    }


    /** Set the "Accept" header if value is not null. Returns the builder. */
    static Builder acceptHeader(Builder builder, String acceptHeader) {
        if ( acceptHeader != null )
            builder.header(HttpNames.hAccept, acceptHeader);
        return builder;
    }

    /** Set the "Content-Type" header if value is not null. Returns the builder. */
    static Builder contentTypeHeader(Builder builder, String contentType) {
        if ( contentType != null )
            builder.header(HttpNames.hContentType, contentType);
        return builder;
    }

    /** Execute a request, return a {@code HttpResponse<InputStream>} which can be passed to
     * {@link #handleHttpStatusCode(HttpResponse)}.
     * @param httpClient
     * @param httpRequest
     * @return
     */
    static HttpResponse<InputStream> execute(HttpClient httpClient, HttpRequest httpRequest) {
        return execute(httpClient, httpRequest, BodyHandlers.ofInputStream());
    }

    /**
     * Execute request and return a response - error messages as JSON in 4xx and 5xx
     * can not be handled if {@code <T>} is not {@code InputStream}. See
     * {@link #execute(HttpClient, HttpRequest)} because we need an
     * {@code InputStream} response.
     * @param httpClient
     * @param httpRequest
     * @param bodyHandler
     * @return
     */
    static <T> HttpResponse<T> execute(HttpClient httpClient, HttpRequest httpRequest, BodyHandler<T> bodyHandler) {
        try {
            return httpClient.send(httpRequest, bodyHandler);
        } catch (IOException | InterruptedException ex) {
            // This is silly.
            // Rather than an HTTP exception, a 403 becomes IOException("too many authentication attempts");
            if ( ex.getMessage().contains("too many authentication attempts") ) {
                throw new HttpException(403, HttpSC.getMessage(403), null);
            }
            throw new HttpException(httpRequest.method()+" "+httpRequest.uri().toString(), ex);
        }
    }

    // This is to allow setting additional/optional query parameters on a per remote service (including for SERVICE).
    protected static void modifyByService(String serviceURI, Context context, Params params, Map<String, String> httpHeaders) {
        // Old Constant.
        ServiceRegistry srvReg = context.get(ARQ.serviceParams);
        if ( srvReg != null ) {
            ServiceTuning mods = srvReg.find(serviceURI);
            if ( mods != null )
                mods.modify(params, httpHeaders);
        }
    }

    /**
     * Return a modifier that will set the Accept header to the value.
     * An argument of "null" means "no action".
     */
    static Consumer<HttpRequest.Builder> setHeaders(Map<String, String> headers) {
        if ( headers == null )
            return (x)->{};
        return x->headers.forEach(x::header);
    }

    /**
     * Return a modifier that will set the Accept header to the value.
     * An argument of "null" means "no action".
     */
    static Consumer<HttpRequest.Builder> setAcceptHeader(String acceptHeader) {
        if ( acceptHeader == null )
            return (x)->{};
        return header(HttpNames.hAccept, acceptHeader);
    }

    /**
     * Return a modifier that will set the Content-Type header to the value.
     * An argument of "null" means "no action".
     */
    static Consumer<HttpRequest.Builder> setContentTypeHeader(String contentType) {
        if ( contentType == null )
            return (x)->{};
        return header(HttpNames.hContentType, contentType);
    }

    /**
     * Return a modifier that will set the named header to the value.
     */
    static Consumer<HttpRequest.Builder> header(String headerName, String headerValue) {
        return x->x.header(headerName, headerValue);
    }
}
