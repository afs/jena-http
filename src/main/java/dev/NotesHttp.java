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

public class NotesHttp {

    // package.html or web page.

    // Per endpoint modification.
    //   ServiceRegistry and per endpoint tuning. Now outside? HttpClient per link.
    //   QueryExecutionHTTP.applyServiceConfig
    //   UpdateExecutionHTTP.modifyByService
    // Need to influence choice of HttpClient.
    //
    // All XXX and TODO
    // Fuseki binary -- add dataset operations.

    // Merge:
    //  [MERGE]
    //     Merge WebContent2 into WebContent (now?)
    //     Merge: G2, G to Glib (now?)
    //     getLink in RDFConnection.
    //     Remove RDFConnection(Others)

    // 2) QueryExecutionHTTP.setTimeout :: query timeout, not connection.

    // 3) QueryExecutionHTTP.modifyByService
    //    UpdateExecutionHTTP.modifyByService
    //      Per destination setup / SERVICE
    //
    // 4) QueryExecutionHTTP DRY
    //
    // 5) GSP
    //      HttpLib
    //      Javadoc
    //      Connection Timeouts
    // 6) Javadoc: HttpOp2, HttpRDF

    // 9) RDFLink :
    //    RDFLinkFactory and authentication (more tests)

    // )) HTTP headers [DONE]
    //    QueryExecutionHTTP [DONE]
    //    UpdateExecutionHTTP [DONE]
    //    GSP [DONE]
    //    RDFLink [DONE - files use extension, not controllable. Use GSP or HttoOp2 directly]

    // ----------------------------------------

    // Jena networking markdown
    //  * RDFConnection
    //  * RDFLink
    //  * SPARQL : GSP | QueryExecutionHTTP | UpdateExecutionHTTP
    //  * HttpRDF
    //  * HttpOp2

    // Binding version of a ResultSet -> Add forEachBinding() to ResultSet interface?

    // ---- Review
    // Check read to end or close on input streams. try-resource? -> use HttpLib.finish

    // Set initial bindings: simple version
    //   Local builder?

    /*
     * Local:
     *
     * ResultSet rs = QE.query(Query).source(Dataset).setTimeout().execSelect();
     *
     * Avoid the try-resource:
     * QE....select(Consumer<Binding> rowHandler)
     * QE....construct() -> Model
     * QE....describe()
     * QE....ask() -> boolean
     * Query
     */
    // https://openjdk.java.net/groups/net/httpclient/recipes.html
}
