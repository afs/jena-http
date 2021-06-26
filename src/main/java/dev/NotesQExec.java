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

    // ** Impact
    // * No use of Apache httpClient - needs upgrading anyway
    // * Old use of QueryEngineHTTP (deprecate now, then delete).
    // *    QueryEngineHTTP --QueryExecutionHTTPBuilder
    // * Deprecation of QueryExecution.setTimeout
    // * Switch to rewrite for initial bindings.

    // Test with https and unsigned certificate.

    // Look for [QExec]

    // [ ] No RowSetMgr: Reader/Write set
    //     RowSetformatter - text and SSE only.
    // SPARQLResult
    // ResultsReader, ResultsWriter

    // == ResultSetReader / ResultsReader
    // == ResultSetWriter / ResultsWriter
    // .read -> ResultSet (and deprecate)
    //    ResultSetReader.readRowSet
    // .write(,RowSet,) (and deprecate)
    // ResultSetReaderJSON - not streaming.
    // ** Quick and dirty : adapt(ResultSet)
    // Assuming no extension, ResultSetReader/ResultSetWrite is not API.
    // ResultSetReaderFactory => ResultSetReader.Factory
    // ResultSetWriterFactory => ResultSetWriter.Factory
    // [ ] ResultsWriter.builder.prefixMap(PrefixMap) - only needed for text

    // == SPARQLresult - carry a RowSet (+ adapter + deprecated to get ResultSet)

    // == ResultSet
    // ResultSet.asRowSet =dft= RowSet.adapt(this)

    // --------


    // If big bang:
    //   Just leave QueryEngineHTTP as legacy, deprecated.
    //   Using org.apache.http.client

    // == Params
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

    // Migration
    // [ ] ResultSet adapter: new ResultSetAdapter -> ResultSet.adapt(RowSet rowSet)
    // [ ]    Or like "Prefixes" --> "Results"? SPARQL? GPI? Adapt.
    // [ ] getLink in RDFConnection.
    // [ ] Remove RDFConnection(clone from others)
    // [ ] Fuseki tests isFuseki
    // [ ] G2 merge to G
    // [ ] QueryExecUtils.executeQuery to work on GPI. Deprecate rest. Redo.

    // Tests
    // [ ] QueryExecutionHTTP (basic only needed)

    // -- Destination:
    // [no] New module jena-http? jena-gpi? Replaces RDFConnection?

    // Other
    // [ ] UpdateProcessor(migrate name):: UpdateExecution, UpdateExecutionHTTP, UpdateExecutionHTTPBuilder
    // --

    // [ ] QueryExecUtils - both forms.
    // [ ] Service.java

    // [ ] QExecBase renamed as QueryExecLocal (QueryExecutionBase)
    // [x] QueryExecHTTP vs QueryExecRemote
    // [ ] QueryExec tests. Or complete switch over!

    // [ ] Move builders into classes
    // [ ] QuerySendMode into QueryExecHTTP.Builder
    // [ ] RowSet and SSE

    // [ ] HttpLib : former "package" scope.
    //       Move HttpLib in queryexec package? When adapter based.

    // Don't use:
    // QueryExecutionHTTP

    // [ ] Adapters:
    //     Put statics in target class not the Adapter. Or a "Adapt" class. "Results"?
    // [x] RDFConnection over RDFLinkLocal, RDFLinkHTTP,
    // [ ] QueryExecution over QueryExecLocal, QueryExecHTTP,
    // [?] UpdateExecution over ?
    // [ ] create() vs newBuilder: create for API, newBuilder for SPI. (GPI?)
    // [ ] Check old code switched to adapters

    // [ ] Tests : esp remote, at Link/Connection and QueryExecution/QExec level.

    // ----
    // [ ] Switch to QExec as basic with adapters for RDFConnection, QueryExecution.
    //     ?? QueryExecutionHTTP, RDFConnectionHTTP
    // [ ] Consider having an "immediately consuming" option for incoming result sets to
    //       be safer for connection management.
    // Twin with "buffer, send with Content-length" option in Fuseki?
    //   Still an issue if client does not read the data.

/*
    Packages:

    jena-arq:
      org.apache.jena.http - HttpEnv, HttpLib, HttpOp2 (rename), old HttpOp->HttpOp1.
         ==>org.apache.jena.riot.web == org.apache.jena.http
      org.apache.jena.sparql.http  - execHTTP, GSP registries., HttpRDF?
      org.apache.jena.queryexec

    jena-rdfconnection:
        org.apache.jena.rdfconnection.link (rdflink)
        org.apache.jena.rdflink

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
