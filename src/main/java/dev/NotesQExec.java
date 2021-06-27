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

    // **** TODO
    // ** Pre-integration:
    // [x] Tests - using registries SERVCE
    // [ ]    Manual test with https and unsigned certificate.
    // [ ]    Manual test AsyncHttpRDF
    // [ ]    Manual test SERVICE in jena-integration-tests
    // [ ] Documentation
    // Look for [QExec]
    // [ ] ResultSetReader -> RowSetReader, RowSetWriter

    // [ ] rename UpdateExecution -> UpdateExec.

    // [x] Restored: RegistryServiceModifier
    // [ ] Undeprecate? Service2.queryClient
    //     ARQ.serviceParams: overloaded - old world "Map<Map<List", new world RegistryServiceModifier
    //       Support both - ask Rob.
    //     ARQ.serviceAllowed
    // [ ] HttpLib.modifyByService
    //     Used by QueryExec and UpdateExec but not GSP.
    //     Add to GSP? But it uses HttpRDF directly. Not for v1. (Only really for Virtuoso unique usage for query and update.)
    //     Document!

    // [ ] Sketch documentation.

    // ** Integration into Jena:

    // ----

    // [ ] QueryExecutionFactory
    // ?leave/deprecate QueryEngineHttp using ApcheHttpLicent for one or two releases?
    // [ ] UpdateExecutionFactory

    // [ ] Deprecation of QueryExecution.setTimeout (use a builder). QueryExecutionBuilder
    // [ ] Switch to rewrite for initial bindings (and this will work for remote!)
    // [-] Old use of QueryEngineHTTP, HttpQuery (leave, inc Apache HttpClient4 - deprecate - delete)
    // [ ] Deprecate all QueryExecutionFactory.sparqlService, createServiceRequest - reference builders.
    //  Deprecate of QueryExecution.setTimeout (leave for now) setIntialBindings - there is a QueryExecutionBuilder for local datasets
    // HttpOp : Direct use of java.net.http covers the complex cases so new HttpOp is smaller

    // ** Legacy : Leave QueryEnginHTTP(etc)in place using Apache HttpClkient - deprecated, removed soon.

    // ** Design
    // + java.net.http
    //     RDFConnection, RDFLink - Builders for remote (local version is simple)
    //     QueryExec and QueryExecution. Builders
    // + GSP engine, Support for quads.
    // + SERVICE rewrite
    // + RowSet - ResultSet for Nodes
    // + Utilities: HttpRDF, AsyncHttpRDF, HttpOp(2)
    // * HttpOp : smaller and mainly in support of HttpRDF which in turn is used by GSP
    // * New HttpRDF (GET/POST/PUT/DELETE graphs and datasets): AsyncHttpRDF (Async GET)

    // ** examples
    // [ ] See RDFConnection and other examples.
    // [ ] Survey examples.
    // [ ] Modification - RegistryByServiceURL
    // [ ] See ExGSP

    // [ ] No RowSetMgr: Reader/Write set
    // [ ] RowSetformatter - text and SSE only.
    // [ ] SPARQLResult

    // == ResultsReader, ResultsWriter
    // [ ] ResultSetReader -> RowSetReader, RowSetWriter + adapters?
    //   ResultSet.adapt(RowSet)
    //   ResultSet.asRowSet()
    // -- ResultSetReader / ResultsReader
    // -- ResultSetWriter / ResultsWriter
    //   Deprecate -- ResultSetReader.read -> ResultSet
    //   Add -- ResultSetReader.readRowSet
    //   Add -- ResultSetWriter.write(,RowSet,)
    //   Deprecate -- ResultSetWriter.write(,ResultSet,)
    // EnvTest to org.apache.jena.test (integration testing)

    // == SPARQLresult - carry a RowSet (+ adapter + deprecated to get ResultSet)

    // == Context
    // Revise
    // [ ] Deprecate put, set to return "this" for chaining.
    // [ ] Context.create().
    // [ ] Document!
    //     Global - dataset - execution - freeze

    // == SERVICE
    // No first timeout (now in HttpClient)
    // [ ] Service2 - switch to registries for params? - retain old world.
    // [x] Service includes "?special=foo"
    // [ ] Undo BraveNewWorld.
    // [ ] Do not optimize SERVICE - substitution only.
    // [ ] Context symbols: Old and new
    // [ ] Test with service not allowed in the query/dataset context.
    // [ ] Test modification (WIP)

    // == RegistryRequestModifier, RegistryHttpClient
    // [ ] Global vs context. RegistryRequestModifier is context passed, RegistryHttpClient is global.
    //     ?? context ARQ.httpRequestModifer is the function, not the registry.
    //     Modifier can always
    //     Use for logging?

    // ResultSetReaderJSON - not streaming.

    // ** Quick and dirty : adapt(ResultSet)
    // Assuming no extension, ResultSetReader/ResultSetWrite is not API.
    // ResultSetReaderFactory => ResultSetReader.Factory (old, extends new, and deprecate)
    // ResultSetWriterFactory => ResultSetWriter.Factory
    // [ ] ResultsWriter.builder.prefixMap(PrefixMap) - only needed for text

    // [ ] Deprecate all QueryExecutionFactory.sparqlService, createServiceRequest
    // [ ] Deprecate of QueryExecution.setTimeout
    // [ ] javadoc of QueryExecutionFactory to refer to builders.
    // [ ] javadoc of UpdateExecutionFactory to refer to builders.

    // == QueryExecution
    // [ ] QueryExecutionBase -> QueryExecutionDataset (?? too late ??)
    // [x] QueryExecutionBuilder - already right name.
    // [ ] QueryExecution setter, getter : deprecate in favour of builders.

    // == RDFConnection
    // [ ] RDFLinkFactory -> builder?

    // == UpdateProcessor
    // [ ] It's called UpdateExecutionHTTP
    // [ ] UpdateProcessor(migrate name) -> UpdateExecution, UpdateExecutionHTTP, UpdateExecutionHTTPBuilder
    // [?] UpdateProcessorBase -> UpdateExecutionDataset
    // [?] UpdateProcessorRemoteBase -> UpdateExecutionDataset
    // [ ] UpdateProcessRemote, leave but deprecate - put UpdateExecutionHTTP along side + builder.

    // Migration:
    // [ ] ResultSet adapter: new ResultSetAdapter -> ResultSet.adapt(RowSet rowSet)
    // [ ]    Or like "Prefixes" --> "Results"? SPARQL? GPI? Adapt.
    // [ ] getLink in RDFConnection.
    // [ ] Remove RDFConnection(clone from others)
    // [ ] Fuseki tests isFuseki
    // [ ] G2 merge to G
    // [ ] QueryExecUtils.executeQuery to work on GPI. Deprecate rest. Redo.

    // == Tests
    // [ ] Coverage
    // [ ] Coverage request modifier (service)
    // [x] Coverage - custom httpClient (auth does this).

    // == Other
    // Fuseki: BodyHandlers.ofFileDownload

    // [ ] QueryExecUtils - both forms.
    // [ ] Move builders into classes
    // [ ] RowSet and SSE
    // [ ] HttpLib : former "package" scope.
    // [ ] RDFConnection over RDFLinkLocal, RDFLinkHTTP,
    // [ ] QueryExecution over QueryExecLocal, QueryExecHTTP,
    // [?] UpdateExecution over ?
    // [x] create() vs newBuilder: newBuilder
    // [ ] Check old code switched to adapters
    // [ ] RequestLogging : See LogIt
    // [x] Does anything respect RegistryRequestModifier? No - delete.
    //     But hide from builder/inline code? HttpClient.
    //     GSP and ExecBuilderQueryHttp have in their builders.
    //     Using builder with params
    //     Variable headers not supported
    // [ ] HttpClient -- connectTimeout = timeout1.

    // --------

    // [ ] *HTTP vs *Http
    // [ ] UpdateExecution - leave as UpdateProcessor or make super type of UpdateProcessor
    // [ ] create() vs newBuilder(). "localBuilder()", "remoteBuilder" / "newBuilder", "newBuilderHTTP"
    // [ ] RDFConnectionFactory - de-emphasise
    // [ ] Builders for RDFLinkDataset, RDFConnectionDataset

    /*
    Packages:
    Radical:
    1:: jena-arq: org.apache.jena.sparql.queryexec
      Ready
        QueryExec, QueryExecBuilder, QueryExecAdpater, QueryExecDataset
        RowSet (ResultSetAdapter?)
      org.apache.jena.http - HttpEnv, HttpLib, HttpOp2 (rename, moved), old HttpOp->HttpOp1 - call through to HttpOp2.
         ==> org.apache.jena.riot.web == org.apache.jena.http
      org.apache.jena.sparql.http  - execHTTP, GSP registries., HttpRDF?

      org.apache.jena.queryexec
      org.apache.jena.engine.http

    4:: jena-rdfconnection:
        org.apache.jena.rdflink
        Alt: org.apache.jena.rdfconnection.link

    RDFLinkLocal - no builder
    RDFConnectionLocal - no builder
     */

    // Documentation : documentation.md
    //   updates for RDFConnection
    //   Details view
    // Jena networking markdown

    // ---- Review
    // https://openjdk.java.net/groups/net/httpclient/recipes.html
}
