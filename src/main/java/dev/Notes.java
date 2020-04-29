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

public class Notes {

    // package.html or web page.

    // Per endpoint modification.
    //   ServiceRegistry and per endpoint tuning. Now outside? HttpClient per link.
    //   QueryExecutionHTTP.applyServiceConfig
    //   UpdateExecutionHTTP.modifyByService
    // Need to influence choice of HttpClient.
    //
    // All XXX and TODO

    // 1) QueryEecutionHTTP, UpdateEecutionHTTP - are headers copied into the request?

    // Merge:
    //   Merge WebContent2 into WebContent (now?)
    //   Merge: G2,G to Glib (now?)
    //   Remove RDFConnectionFuseki

    // 2) QueryExecutionHTTP.modifyByService
    //    UpdateExecutionHTTP.modifyByService
    //      Per destination setup./ SERVICE

    // 3) QueryExecutionHTTP DRY

    // 4) ** RDFLinkremote - use content Type
    // outputTriples vs defaults on HttpEnv. (fixed?)

    // 5) GSP does not use headers
    //    GSP.request().accept(string).GET().
    //    GSP.request().contentType(string).POST(), ?? contentType(RDFFormat)
    //       and less operations.

    // ** RDFLinkFuseki

    // RDFLink :
    //    RDFLinkFactory and authentication (more tests)

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
