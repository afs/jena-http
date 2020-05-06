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

    // All XXX and TODO

    // WIP:
    // **** Binding version of a ResultSet -> Add forEachBinding() to ResultSet interface?

    //   QueryExecutionHTTP/SendMode.

    // Merge:
    //  [MERGE]
    //     Merge WebContent2 into WebContent (now?)
    //     Merge: G2, G to Glib (now?)
    //     getLink in RDFConnection.
    //     Remove RDFConnection(Others)
    //     Fuseki tests isFuseki

    // 2)  Fuseki binary -- add dataset operations.

    // 3) QueryExecutionHTTP.modifyByService
    //    UpdateExecutionHTTP.modifyByService
    //         modifyByService - choose HttpClient (security), headers?, rewrite URL?
    //    Per destination setup / SERVICE
    //
    // 5) GSP
    //      HttpLib
    //      Javadoc
    //      Connection Timeouts
    // 6) Javadoc: HttpOp2, HttpRDF

    // ----------------------------------------

    // Jena networking markdown
    //  * RDFConnection
    //  * RDFLink
    //  * SPARQL : GSP | QueryExecutionHTTP | UpdateExecutionHTTP
    //  * HttpRDF
    //  * HttpOp2


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
