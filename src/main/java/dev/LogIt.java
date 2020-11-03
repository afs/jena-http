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

package dev;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.jena.atlas.logging.FmtLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogIt {
    private static Logger LOG = LoggerFactory.getLogger("HTTP");

    /** Request */
    private static void logRequest(HttpRequest httpRequest) {
        if ( false ) {
            FmtLog.info(LOG, "%s %s", httpRequest.method(), httpRequest.uri());
            String s = headersToString(httpRequest.headers().map());
            FmtLog.info(LOG, "Headers:\n"+s);
        }

        // Uses the SystemLogger which defaults to JUL.
        // Add org.apache.jena.logging:log4j-jpl
        // (java11 : 11.0.9, if using log4j-jpl, logging prints the request as {0} but response OK)
//        httpRequest.uri();
//        httpRequest.method();
//        httpRequest.headers();
    }

    /** Response (do not touch the body!)  */
    private static void logResponse(HttpResponse<?> httpResponse) {
//        httpResponse.uri();
//        httpResponse.statusCode();
//        httpResponse.headers();
//        httpResponse.previousResponse();
    }

    private static String headersToString( Map<String, List<String>> headers) {
        StringJoiner sj = new StringJoiner("\n", "", "\n");
        headers.forEach((k,list)->{
            list.stream().map(v->String.format("    %s: %s", k, v)).forEach(sj::add);
        });
        return sj.toString();
    }

}

