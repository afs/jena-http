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

    // Security for scripts
    //    Per destination setup./ SERVICE
    //      QueryExecutionHTTP
    //      UpdateExecutionHTTP
    //      HttpOp2, HttpRDF, HttpGSP

    //    Setting User/password for first use calls. / basic.
    //    Registry : inc prefix of URL.

    // XXX markers.

    // ----

    // ConnRDF - RDFConnection for graphs.
    // What about a QueryExecution but for graphs/nodes?

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
