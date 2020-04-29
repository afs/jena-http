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

    // RDFLink :
    //    GSP.POST(,contentType) ignores content type.
    //    RDFLinkFactory and authentication

    // Binding version of a ResultSet -> Add forEachBinding() to ResultSet interface?


    // **** RDFLinkFactory and authentication

    // URLEncodedUtils.format better than URLEncoder?

    // Check RDFConnectionAdapter for using defaults - wrapper all? queryAsk(String) etc

    // No autoparse of RDFConnection.quert(string), update(string)

    // HttpEnv.dft settings vs RDFLinkRemoteBuilder output* settings.

    // GSP dataset operation naming is odd.

    // Instead of copy, use /** {@inheritDoc} */
    // Does not seem to pop-up in Eclipse.

    // RDFLink
    //   Review

    // All XXX and TODO

    // Security for scripts
    //    Per destination setup./ SERVICE
    //      QueryExecutionHTTP
    //      UpdateExecutionHTTP
    //      HttpOp2, HttpRDF, HttpGSP

    //    Setting User/password for first use calls. / basic.
    //    Registry : inc prefix of URL.

    // XXX markers.

    // ----

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
