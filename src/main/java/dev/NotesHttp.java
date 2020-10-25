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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import org.seaborne.http.HttpEnv;

public class NotesHttp {

    // ToDo

    // Documentation
    //   updates for RDFConnction
    //   Details view
    // Jena networking markdown
    // * RDFConnection
    // * RDFLink
    // * SPARQL : GSP | QueryExecutionHTTP | UpdateExecutionHTTP
    // * HttpRDF
    // * HttpOp2

    // Check for javadoc
    // Examples!
    // Tests : More auth:
    // TestAuthRemote - check early. Two variants for each test, with registry and explicit .httpClient(hc)
    //   GSP delete

    // Do we need ServiceTuning? Minor feature. Exact match.
    //   Same Mechanism for finding as RegistryHttpClient
    //   Change tests - not auth - where?
    // SERVICE: Per-endpoint HttpClient: Global registry.
    //   Tests needed
    // ** HttpEnv.getDftHttpClient() ==> HttpEnv.getHttpClient(url);
    //
    // @@ RegistryHttpClient Bug - dataset with a shorter name.
    // Exact match URL, or per server

    // ** HttpEnv.getHttpClient(url, httpClient);
    //
    // [x] GSP : GSP.service sets the URL.
    // [x] Query : QueryExecutionHTTPBuilder.service
    // [x] Update : UpdateExecutionHTTPBuilder.service (ditto)
    // Tests of HttpEnv.getHttpClient


    static class HttpEnv2 {

        //HttpRequest httpRequest

        public static HttpClient getHttpClient(HttpRequest httpRequest) {
            URI uri = httpRequest.uri();

            // lookup by scheme, host+port, path.

            // System default.
            return HttpEnv.getDftHttpClient();
        }

        private static String server(URI uri) {
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme());
            sb.append("://");
            sb.append(uri.getHost());
            // authority   = [ userinfo "@" ] host [ ":" port ]
            if ( uri.getPort() > 0 ) {
                sb.append(':');
                sb.append(Integer.toString(uri.getPort()));
            }
            sb.append("/");
            return sb.toString();
        }


    }


    //   Add a URL->HttpClient mapping.
    // [ ]    UpdateExecutionHTTP - modifyRequest, altHttpClient mapping
    //        QueryExecutionHTTP- modifyRequest, altHttpClient mapping
    //        TestAuthRemote.auth_link_local_context_1

    // [ ] RequestLogging
    //     Query, Update, GSP. -> all through HttpLib.execute!
    //     Higher level:
    //          QueryExecutionHTTP.executeQueryPush, executeQueryGetForm
    //          UpdateExecutionHTTP.executeUpdate
    //          GSP.(GET,POST,PUT,DELETE), + dataset versions.

    // [ ] Integration
    //     ARQ: Service.java

    // QExec->QueryExec
    // [ ] Getting builders for UpdateExecution, QueryExecution; local and remote.
    //       Factory,library for getting one + QueryExecution : SparqlLib?
    // Service Registry ->
    //   Per dest HttpClient.
    //   Tuning
    // ServiceTuning returns an HttpClient? Ugly!

    // Logging - log every request! Sparql request Log.
    //
    // [] .service vs .destination (check)
    // [] RDFLinkFactory == RDFLinkRemoteBuilder.create().destination(destination)
    // Better builder access (RDFLinkFactory? RDFLink.createRemote()?)


    // Check for javadoc
    // ----------------------------

    // package.html or web page.

    // * Fuseki binary -- add dataset operations.

    // [3]
    // Merge WebContent2 into WebContent (now?)

    // [4]
    // QueryTransformOps.transform - see builders and XXX

    // [5] Service.java

    // [MERGE]
    // getLink in RDFConnection.
    // Remove RDFConnection(Others)
    // Fuseki tests isFuseki

    // ---- Review
    // https://openjdk.java.net/groups/net/httpclient/recipes.html
}
