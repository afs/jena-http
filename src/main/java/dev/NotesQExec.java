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

    // ** Discuss

    // - HttpClient
    //   HTTP/2, async
    //     HttpClient not HttpClient/HttpConfig pair.
    //     BodyHandlers.ofFileDownload
    // - Builder style
    // - Model and Graph / Statement and Triple level APIs
    //   (Model-level being adapters of Graph level engines)
    // - Changes:
    //     Most use - none.
    //     HttpClient and e.g. authorization

    // [ ] QueryExecutionFactory
    // ?leave/deprecate QueryEngineHttp using ApcheHttpLicent for one or two releases?
    // [ ] UpdateExecutionFactory

    // ** Impact
    // * No use of Apache httpClient - needs upgrading anyway
    // * Old use of QueryEngineHTTP (deprecate now, then delete).
    // *    QueryEngineHTTP --QueryExecutionHTTPBuilder
    // * Deprecation of QueryExecution.setTimeout
    // * Switch to rewrite for initial bindings.

    // === HttpClient auth issues
    // -- QueryEngineHTTP : timeouts and initialBinding not supported. Only auth issues.
    // -- RDFConnection

    // ** examples
    // [ ] See RDFConnection and other examples.
    // [ ] Survey examples.

    // Test with https and unsigned certificate.
    // Test AsyncHttpRDF

    // Look for [QExec]

    // [ ] No RowSetMgr: Reader/Write set
    //     RowSetformatter - text and SSE only.
    // SPARQLResult
    // ResultsReader, ResultsWriter

    // == ResultSet
    //   ResultSet.adapt(RowSet)
    //   ResultSet.asRowSet()
    // == ResultSetReader / ResultsReader
    // == ResultSetWriter / ResultsWriter
    //   Deprecate -- ResultSetReader.read -> ResultSet
    //   Add -- ResultSetReader.readRowSet
    //
    //   Add -- ResultSetWriter.write(,RowSet,)
    //   Deprecate -- ResultSetWriter.write(,ResultSet,)

    // ResultSetReaderJSON - not streaming.

    // ** Quick and dirty : adapt(ResultSet)
    // Assuming no extension, ResultSetReader/ResultSetWrite is not API.
    // ResultSetReaderFactory => ResultSetReader.Factory (old, extends new, and deprecate)
    // ResultSetWriterFactory => ResultSetWriter.Factory
    // [ ] ResultsWriter.builder.prefixMap(PrefixMap) - only needed for text

    // == SPARQLresult - carry a RowSet (+ adapter + deprecated to get ResultSet)

    // [ ] Deprecate all QueryExecutionFactory.sparqlService, createServiceRequest
    // [ ] Deprecate of QueryExecution.setTimeout
    // [ ] javadoc of QueryExecutionFactory to refer to builders.
    // [ ] javadoc of UpdateExecutionFactory to refer to builders.

    // == Other
    // Fuseki: BodyHandlers.ofFileDownload

    // [ ]HttpRDf : prefixed Map for accept graph, etc.
    //   Use HttpLib.setAcceptHeader - unnecessary Map<String, String>
    // Test AsyncHttpRDF.

    // --------

    // QueryEngineHTTP2 - hidden builder.

    // [ ] UpdateExecution - leave as UpdateProcessor or make super type of UpdateProcessor

    // HttpClient/interface: remove!

    // [ ] BodyHandlerRDF? For async

    /*
    Packages:
    Radical:
    1:: jena-http:org.apache.jena.http
        All the org.apache.jena.http package?
    ??
        Need to init/inject functions for indirections for
        QueryExecutionFactory.sparqlService -- javadoc alternatives. deprecate!
        UpdateExecutionFactory.createRemote

        GSP client RDFConnection - document GSP.

    2:: jena-arq: org.apache.jena.sparql.queryexec
      Ready
        QueryExec, QueryExecBuilder, QueryExecAdpater, QueryExecDataset
        RowSet (ResultSetAdapter?)

    3:: jena-arq: Alt to 1
      org.apache.jena.http - HttpEnv, HttpLib, HttpOp2 (rename, moved), old HttpOp->HttpOp1 - call through to HttpOp2.
         ==> org.apache.jena.riot.web == org.apache.jena.http
      org.apache.jena.sparql.http  - execHTTP, GSP registries., HttpRDF?

      org.apache.jena.queryexec
      org.apache.jena.engine.http
        Old code.

    4:: jena-rdfconnection:
        org.apache.jena.rdflink
        Alt: org.apache.jena.rdfconnection.link

    I - QueryExecution
    I - QueryExec
    QueryExecutionLocal = QueryExecutionBase
    QueryExecLocal = QueryExecutionBase
    QueryExecutionHTTP - replaces QueryEngineHTTP
    QueryExecHTTP
    ExecHTTPBuilder<X> > QueryExecHTTPBuilder, QueryExecutionHTTPBuilder!

    I - RDFConnection
    I - RDFLink
    RDFLinkLocal - no builder
    RDFConnectionLocal - no builder
    AbstractLinkHTTPBuilder > RDFLinkRemoteBuilder, RDFConnectionRemoteBuilder
    RDFConnectionFuseki - no builder
*/

    // If big bang:
    //   Just leave QueryEngineHTTP as legacy, deprecated.
    //   Using org.apache.http.client

    // == Params
    // org.apache.jena.sparql.engine.http
    // New Params: Like old params except that no dependence on ApacheHttpClient utility code.
    //   Uses HttpLib.
    // [ ] Copy new to old.
    //     Used by QueryEnginHTTP
    //     Used by HttpOp(old)
    //     HttpOp2(new) - use only in post where the form body is needed - package scope.

    // == QueryEngineHTTP / QueryExecutionHTTP
    // org.apache.jena.sparql.engine.http.QueryEngineHTTP
    //   Place for QueryExecHTTP? And registries?
    // QueryEngineHTTP replaced by QueryExecutionHTTP (is it just a rename?)
    // [ ] Leave QueyEngineHTTP, deprecate and reference QueryExecutionHTTP(Builder.)/QueryExecHTTP
    //   Merge new Accept strings into QueryEngineHTTP.
    // Location of QueryExecutionHTTP?

    // == QueryExecution
    // [ ] QueryExecutionBase -> QueryExecutionDataset (?? too late ??)
    // [x] QueryExecutionBuilder - already right name.
    // [ ] QueryExecution setter, getter : deprecate in favour of builders.

    // == Service.java

    // Migration:
    // [ ] ResultSet adapter: new ResultSetAdapter -> ResultSet.adapt(RowSet rowSet)
    // [ ]    Or like "Prefixes" --> "Results"? SPARQL? GPI? Adapt.
    // [ ] getLink in RDFConnection.
    // [ ] Remove RDFConnection(clone from others)
    // [ ] Fuseki tests isFuseki
    // [ ] G2 merge to G
    // [ ] QueryExecUtils.executeQuery to work on GPI. Deprecate rest. Redo.

    // Tests
    // [ ] QueryExecutionHTTP (basic only needed)

    // [ ] UpdateProcessor(migrate name) -> UpdateExecution, UpdateExecutionHTTP, UpdateExecutionHTTPBuilder
    // [?] UpdateProcessorBase -> UpdateExecutionDataset
    // [?] UpdateProcessorRemoteBase -> UpdateExecutionDataset
    // [ ] UpdateProcessRemote, leave but deprecate - put UpdateExecutionHTTP along side + builder.

    // --

    // [ ] QueryExecUtils - both forms.

    // [ ] QueryExecBase renamed as QueryExecLocal (QueryExecutionBase)
    // [x] QueryExecHTTP vs QueryExecRemote
    // [ ] QueryExec tests. Or complete switch over!

    // [ ] Move builders into classes
    // [ ] QuerySendMode into QueryExecHTTP.Builder
    // [ ] RowSet and SSE

    // [ ] HttpLib : former "package" scope.

    // [x] Don't need QueryExecutionHTTP: equals adapter = QueryExecHTTP

    // [ ] Adapters: Put statics in a single place: "Adapt" | "Results" vs in target class. ** In target class.
    // [x] RDFConnection over RDFLinkLocal, RDFLinkHTTP,
    // [ ] QueryExecution over QueryExecLocal, QueryExecHTTP,
    // [?] UpdateExecution over ?
    // [ ] create() vs newBuilder: create for API, newBuilder for SPI. (GPI?)
    // [ ] Check old code switched to adapters

    // [ ] Tests : esp remote, at Link/Connection and QueryExecution/QueryExec level.

    // ----
    // [ ] Switch to QueryExec as basic with adapters for RDFConnection, QueryExecution.
    //     Old style? QueryEngineHTTP
    // [ ] Consider having an "immediately consuming" option for incoming result sets to
    //       be safer for connection management.

    // Documentation
    //   updates for RDFConnection
    //   Details view
    // Jena networking markdown
    // Table of API and GPI
    //
    // * RDFConnection
    // * RDFLink
    // * QueryExecution and QueryExec
    // * HTTP
    // * SPARQL : GSP | QueryExecutionHTTP | UpdateExecutionHTTP
    // * HttpRDF
    // * HttpOp2
    // * Auth: Client lib e.g. user+password setup

    // Check for javadoc

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

    // ---- Review
    // https://openjdk.java.net/groups/net/httpclient/recipes.html
}
