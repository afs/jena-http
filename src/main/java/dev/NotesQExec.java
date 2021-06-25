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

public class NotesQExec {

    // Test with https and unsigned certificate.

    // [QExec]
    // in "exec"

    // ** Not closing?
    // *8 Wrong conneg

    // [x] QExec is the Node level version. QExec.Builder.
    // [x] QueryExecutionAdapter -- concrete adapter, not abstract.
    // [x] Use rewrite for initial bindings.
    // [?] QueryEngineFactory API change (migration).

    // Sharable between QueryExecution*Builer and QExec*Builder
    // AbstractQExecXXXBuilder<X> -- build is

    // [ ] No RowSetMgr: Reader/Write set
    //     RowSetformatter - text and SSE only.

    // [ ] Eliminate choosePort.

    // ResultsWriter.builder.prefixMap(PrefixMap) - only needed for text

    // ** Impact
    // * No use of Apache httpClient - needs upgrading anyway
    // * Old use of QueryEngineHTTP (deprecate now, then delete).
    // *    QueryEngineHTTP --QueryExecutionHTTPBuilder
    // * Deprecation of QueryExecution.setTimeout
    // * Switch to rewrite for initial bindings.

    // If big bang:
    //   Just leave QueryEngineHTTP as legacy.
    //   Using org.apache.http.client


    // [ ] ResultsReaders, ResultsWriter

    // Remove/deprecate: org.apache.jena.sparql.engine.http.Params;

    // Migration
    // [ ] ResultSet adapter: new ResultSetAdapter -> ResultSet.adapt(RowSet rowSet)
    // [ ]    Or like "Prefxes" --> Results? SPARQL? GPI? Adapt.
    // [ ] ResultSetWriter -> RowSetWriter + default method, prefixes.
    // [ ] ResultSetReader/Writer to work on RowSets
    // [ ] QueryExecution setter, getter : deprecate in favour of builder.
    // [x] Merge WebContent2 into WebContent (now?)
    // [ ] getLink in RDFConnection.
    // [ ] Remove RDFConnection(Others)
    // [ ] Fuseki tests isFuseki
    // [ ] G2 merge to G
    // QueryExecUtils.executeQuery to work on GPI. Deprectae rest. Redo.

    // Destination:
    // [no] New module jena-http? jena-gpi? Replaces RDFConnection?

    // Tests QueryExecutionHTTP (basic only needed)

    // jena-arq:
    //   org.apache.jena.http - HttpEnv, HttpLib, HttpOp2 (rename), HttpRDF?
    //   org.apache.jena.sparql.http  - execHTTP, GSP registries.

    // jena-rdfconnection:
    //    org.apache.jena.link (rdflink)

    // [ ] QueryExecUtils
    // [ ] Service.java

    // [ ] QExec renamed as QueryExec?
    // [ ] QExecBase renamed as QueryExecLocal
    // [ ] QueryExecHTTP vs QueryExecRemote
    // [ ] QueryExec tests. Or complete switch over!

    // [x] RowSetFormatter (text!)
    // [ ] RowSet and SSE

    // [ ] Naming: *Remote vs *HTTP
    // [x] QExec - get timeouts? no. local != remote
    // [ ] HttpLib : former "package" scope.
    //       Move HttpLib in qexec package? When adapter based.

    // Don't use:
    // QueryExecutionHTTP

    // [ ] Adapters:
    // [ ] Put statics in target class not the Adapter. Or a "Adapt" class. "Results"?
    // [x] RDFConnection over RDFLinkLocal, RDFLinkHTTP,
    // [ ] QueryExecution over QueryExecLocal, QueryExecHTTP,
    // [?] UpdateExecution over ?
    // [ ] create() vs newBuilder: create for API, newBuilder for SPI. (GPI?)

    // [ ] Tests : esp remote, at Link/Connection and QueryExecution/QExec level.

    // ----
    // [ ] Switch to QExec as basic with adapters for RDFConnection, QueryExecution.
    //     ?? QueryExecutionHTTP, RDFConnectionHTTP
    // [ ] Consider having an "immediately consuming" option for incoming result sets to
    //       be safer for connection management.
    // Twin with "buffer, send with Content-length" option in Fuseki?
    //   Still an issue if client does not read the data.

    // [x] QuerySendMode handling : switch from GET to POST application/sparql-query not form.
    //     Already does this.
    // [ ] QuerySendMode.getOrPOST
    //     asGetWithLimit => asGetOrPostForm; asGetOrPostBody
    //     Default asGetWithLimitForm => asGetWithLimitBody

/*
    Packages:

    org.apache.jena.sparql.exec
      QExec

    org.apache.jena.rdfconnection.link
      RDFLink

    I - QueryExecution
    I - QExec
    QueryExecutionLocal = QueryExecutionBase
    QExecLocal = QueryExecutionBase
    QueryExecutionHTTP - replaces QueryEngineHTTP
    QExecHTTP
    AbstractExecHTTPBuilder<X> > QExecHTTPBuilder, QueryExecutionHTTPBuilder

    I - RDFConnection
    I - RDFLink
    RDFLinkLocal - no builder
    RDFConnectionLocal - no builder
    AbstractLinkHTTPBuilder > RDFLinkRemoteBuilder, RDFConnectionRemoteBuilder
    RDFConnectionFuseki - no builder
*/
    // ----

/*
Documentation
   HttpClient setup
 */


    // Documentation
    //   updates for RDFConnection
    //   Details view
    // Jena networking markdown
    // Table of API and GPI
    //
    // * RDFConnection
    // * RDFLink
    // * QueryExecution and QExec
    // * HTTP
    // * SPARQL : GSP | QueryExecutionHTTP | UpdateExecutionHTTP
    // * HttpRDF
    // * HttpOp2
    // * Auth: Client lib e.g. user+password setup

    // Check for javadoc
    // Tests : More auth:

    // Tests of HttpEnv.getHttpClient
    // [ ] RequestLogging : See LogIt
    //     Query, Update, GSP. -> all through HttpLib.execute!
    //     Higher level:
    //          GSP: ??
    //            GSP.(GET,POST,PUT,DELETE), + dataset versions.
    //          QueryExecutionHTTP.executeQueryPush, executeQueryGetForm
    //            --> QueryExecutionHTTP.logQuery
    //          UpdateExecutionHTTP.executeUpdate
    //            --> UpdateExecutionHTTP.logUpdate

    // [ ] Integration
    //     ARQ: Service.java

    // QExec->QueryExec

    // ----------------------------

    // package.html or web page.

    // * Fuseki binary -- add dataset operations.

    // QueryTransformOps.transform - see builders and XXX



    // ---- Review
    // https://openjdk.java.net/groups/net/httpclient/recipes.html
}
