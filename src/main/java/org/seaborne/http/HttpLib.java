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
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.web.HttpSC;

/**
 * Operations related to SPARQL HTTP request - Query, Update and Graph Store protocols.
 */
public class HttpLib {

    public static BodyHandler<Void> noBody() { return BodyHandlers.discarding(); }

    public static BodyPublisher stringBody(String str) { return BodyPublishers.ofString(str); }

    /**
     * Get the InputStream from an HttpResponse, handling possible compression settings.
     */
    static InputStream getInputStream(HttpResponse<InputStream> httpResponse) {
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

    private static BodyHandler<InputStream> bodyHandlerInputStream = buildDftBodyHandlerInputStream();

    private static BodyHandler<InputStream> buildDftBodyHandlerInputStream() {
        return responseInfo -> {
            // For simplicity, deal with at the start of body processing.
            // handleHttpStatusCode(responseInfo);
            // XXX Add compression decode. here or later?
            return BodySubscribers.ofInputStream();
        };
    }

    static BodyHandler<InputStream> getBodyInputStream() { return HttpLib.bodyHandlerInputStream; }

    static Function<HttpResponse<String>, String> bodyStringFetcher = r-> {
        String msg = r.body();
        if ( msg != null && msg.isEmpty() )
            return null;
        return msg;
    };

    static Function<HttpResponse<InputStream>, String> bodyInputStreamToString = r-> {
        InputStream in = r.body();
        String msg;
        try {
            msg = IO.readWholeFileAsUTF8(in);
            if ( msg.isEmpty() )
                return null;
            return msg;
        } catch (IOException e) { throw new HttpException(e); }
    };

    /** Calculate basic auth header. */
    static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }



    // StatusCode to exception, at the start of response handling.
    static <T> void handleHttpStatusCode(HttpResponse<T> response, Function<HttpResponse<T>, String> bodyFetcher) {
        int httpStatusCode = response.statusCode();
        // There is no status message in HTTP/2.
        if ( ! HttpLib.inRange(httpStatusCode, 100, 599) )
            throw new HttpException("Status code out of range: "+httpStatusCode);
        else if ( HttpLib.inRange(httpStatusCode, 100, 199) ) {
            // Informational
        }
        else if ( HttpLib.inRange(httpStatusCode, 200, 299) ) {
            // Success. Continue processing.
        }
        else if ( HttpLib.inRange(httpStatusCode, 300, 399) ) {
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
        else if ( HttpLib.inRange(httpStatusCode, 400, 499) ) {
            String msg = bodyFetcher.apply(response);
            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), msg);
        }
        else if ( HttpLib.inRange(httpStatusCode, 500, 599) ) {
            String msg = bodyFetcher.apply(response);
            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), msg);
        }
    }

    /** Test x:int in [min, max] */
    private static boolean inRange(int x, int min, int max) { return min <= x && x <= max; }

    static URI toURI(String uriStr) {
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

    static <X> X dft(X value, X dftValue) {
        return (value != null) ? value : dftValue;
    }

    /*package*/ static Builder newBuilder(String url, Map<String, String> httpHeaders, String acceptHeader, boolean allowCompression, long readTimeout, TimeUnit readTimeoutUnit) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(toURI(url));
        if ( acceptHeader != null )
            builder.header(HttpNames.hAccept, acceptHeader);
        if ( readTimeout >= 0 )
            builder.timeout(Duration.ofMillis(readTimeoutUnit.toMillis(readTimeout)));
        if ( allowCompression )
            builder.header(HttpNames.hAcceptEncoding, "gzip,inflate");
        httpHeaders.forEach(builder::header);
        return builder;
    }

    static <T> HttpResponse<T> execute(HttpClient httpClient, HttpRequest httpRequest, BodyHandler<T> bodyHandler) {
        try {
            return httpClient.send(httpRequest, bodyHandler);
        } catch (IOException | InterruptedException e) {
            throw new HttpException(httpRequest.method()+" "+httpRequest.uri().toString(), e);
        }
    }
}
