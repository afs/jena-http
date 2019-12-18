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

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;

/**
 * JVM wide setting used when the application does not provide an{@link HttpClient} and other useful things.
 */
public class HttpEnv {

    private static HttpClient httpClient = buildDftHttpClient();
    private static BodyHandler<InputStream> bodyHandlerInputStream = buildDftBodyHandlerInputStream();

    public static HttpClient getDftHttpClient() { return httpClient; }

    public static BodyHandler<InputStream> getBodyInputStream() { return bodyHandlerInputStream; }

    private static HttpClient buildDftHttpClient() {
        return HttpClient.newBuilder()
            // By default, the client has polling and connection-caching.
            // Version HTTP/2 is the default, negoitiating up from HTTP 1.1.
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(Redirect.NORMAL)
            //.sslContext
            //.sslParameters
            //.proxy
            //.authenticator
            .build();
    }

    private static BodyHandler<InputStream> buildDftBodyHandlerInputStream() {
        return responseInfo -> {
            // For simplicity, deal with at the start of body processing.
            // handleHttpStatusCode(responseInfo);
            // XXX Add compression decode. here or later?
            return BodySubscribers.ofInputStream();
        };
    }

    // StatusCode to exception, ResponseInfo handling.

//    static void handleHttpStatusCode(ResponseInfo responseInfo) {
//        int httpStatusCode = responseInfo.statusCode();
//        // No status message in HTTP/2.
//        //HttpHeaders headers = responseInfo.headers();
//        //headers.map().forEach((x,y) -> System.out.printf("%-20s %s\n", x, y));
//        if ( ! inRange(httpStatusCode, 100, 599) )
//            throw new HttpException("Status code out of range: "+httpStatusCode);
//        else if ( inRange(httpStatusCode, 100, 199) ) {
//            // Informational
//        }
//        else if ( inRange(httpStatusCode, 200, 299) ) {
//            // Success. Continue processing.
//        }
//        else if ( inRange(httpStatusCode, 300, 399) ) {
//            // We had follow redirects on (default client) so it's http->https,
//            // or the application passed on a HttpClient with redirects off.
//            // Either way, we should not continue processing.
//            try {
//                BodySubscribers.discarding().getBody().toCompletableFuture().get();
//            } catch (InterruptedException | ExecutionException e) {
//                throw new HttpException("Error discarding body of "+httpStatusCode , e);
//            }
//            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), null);
//        }
//        else if ( inRange(httpStatusCode, 400, 499) ) {
//            String response = bodyString(responseInfo);
//            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), response);
//        }
//        else if ( inRange(httpStatusCode, 500, 599) ) {
//            String response = bodyString(responseInfo);
//            throw new HttpException(httpStatusCode, HttpSC.getMessage(httpStatusCode), response);
//        }
//    }

}
