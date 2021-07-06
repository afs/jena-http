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

    void migration() {
        // Delete DatasetAccessor
        // G2 merge to G etc
        /*
        Packages:
        1:: jena-arq

        org.apache.jena.sparql.exec
            QueryExec, QueryExecBuilder, QueryExecAdpater, QueryExecDataset
            RowSet (ResultSetAdapter?)

        org.apache.jena.riot.web == org.apache.jena.http
              - HttpEnv, HttpLib, HttpOp2 (rename, moved), old HttpOp->HttpOp1 - call through to HttpOp2.

        org.apache.jena.sparql.exec.http (or org.apache.jena.riot.http)
            GSP?
            HTTP variants.

        Alt:
          org.apache.jena.queryexec (no)
          org.apache.jena.engine.http

        4:: jena-rdfconnection:
            org.apache.jena.rdflink
            Alt: org.apache.jena.rdfconnection.link
         */
        // [ ] Remove DatasetAccessor!
    }

    void misc() {
        // [ ] *Remote vs *HTTP
        // [ ] Where do builders come from? Factory?
        //     QueryExec.newLocalBuilder newRemoteBuilder?
        //     SPARQL.newQueryExec()?
        //     Local.newQueryBuilder, HTTP.newBuilder.

        //Misc
        // [-] EnvTest to org.apache.jena.test (integration testing)
        // [ ] QueryExecUtils.executeQuery to work on GPI. Deprecate rest. Redo.
        // [-] Move builders into classes?? Or move all out
        // [ ] RequestLogging : See LogIt

        // 1 - make new world client work with compression (send only)
        // 5 - Consider Content-Length calculation form.
    }

    void documentation() {
        // [ ] Document!
        //     Global -> dataset -> [fuseki service?] -> execution -> freeze
        //     StreamRDF
    }

    void compression() {
        // [x] Off for responses in Fuseki. On for sending?
        //     Compression settings:
        //       HttpQuery, HttpOp. Update?
        // [?] Compression class of system constants
        //     Compression QueryEngineHTTP (pass through) and HttpQuery (decide here)
        // [ ] TestUsageHTTP
    }

    void GSP() {
    }

    void HttpOp2() {
    }

    void HTTP() {
        // [ ] Run some integration tests in HTTP 1.1 mode
        // [ ] See what headers are not allowed in HTTP/2.
        // [ ] TestUsageHTTP : compression and no compression
        // [ ] HTTP version
    }

    void SERVICE() {
        // [ ] Undo BraveNewWorld.
        // [-] Do not optimize SERVICE - substitution only.
    }

    void RDFConnection() {
        // [x] Adapter : link/RDFConnectionAdapter
        // [ ] RDFConnectionFactory - de-emphasise - refer to builders
        // [?] RDFConnectionDatasetBuilder
    }

    void RDFLink() {
    }

    void RowSet() {
        // [ ] reader, writer
        // [ ] SSE : RowSetformatter
        // [ ] SPARQLResult
        // [ ] ResultSet as adapter : resultSetTream goes away
        //   Deprecate -- ResultSetReader.read -> ResultSet
        //   Add -- ResultSetReader.readRowSet
        //   Add -- ResultSetWriter.write(,RowSet,)
        //   Deprecate -- ResultSetWriter.write(,ResultSet,)
        // [ ] RowSetFormatter => RowSetOps
        // [ ] ResultSetReader -> RowSetReader, RowSetWriter
        // [ ] ResultsWriter.builder.prefixMap(PrefixMap) - only needed for text
    }

    void query() {
        // [ ] QueryExecutionFactory -> deprecate and refer to builder.
        // [ ] Leave/deprecate QueryEngineHttp using ApcheHttpClient for one or two releases?
        // [ ] Deprecation of QueryExecution.setTimeout (use a builder). QueryExecutionBuilder
        // [-] Old use of QueryEngineHTTP, HttpQuery (leave, inc Apache HttpClient4 - deprecate - delete)
        // [ ] Deprecate all QueryExecutionFactory.sparqlService, createServiceRequest - reference builders.
        // [ ]  Deprecate of QueryExecution.setTimeout (leave for now) setIntialBindings
        //      T there is a QueryExecutionBuilder for local datasets
        // [ ] javadoc of QueryExecutionFactory to refer to builders.
        // [ ] javadoc of UpdateExecutionFactory to refer to builders.
        // [ ] QueryExecutionBase -> QueryExecutionDataset (?? too late ??)
    }

    void HttpOp() {
    }

    void update() {
        // Is there an abstraction for a "ready to run" UpdateExecBuilder - only has ".execute(dsg)"
        // [ ] UpdateProc or UpdateExec
        // [-] Later

        // Modified UpdateProcessor? UpdateProc?

        // [ ] UpdateExecutionFactory
        // [ ] UpdateProcessorBase becomes "UpdateExecDataset (keep UpdateProcessor interface)
        // [-] UpdateProcessor for resource level.
        // [x] UpdateExec for graph level inc builder.
        // [ ] It's called UpdateExecHTTP
        // [ ] UpdateExecutionHTTP - same except Dataset
        // [ ] UpdateProcessor(migrate name) -> UpdateExecution, UpdateExecutionHTTP, UpdateExecutionHTTPBuilder
        // [?] UpdateProcessorBase -> UpdateExecutionDataset
        // [?] UpdateProcessorRemoteBase -> UpdateExecutionDataset
        // [ ] UpdateProcessRemote, leave but deprecate - put UpdateExecutionHTTP along side + builder.
    }

    void tests() {
        // [ ] UpdateExecBuilder
        // [x] UpdateExecHTTPBuilder
    }

    void examples() {
        // ** examples
        // [ ] See RDFConnection and other examples.
        // [ ] Survey examples.
        // [ ] Modification - RegistryByServiceURL
        // [ ] See ExGSP

    }

    void jetty() {
        // == Jetty
        // See NotesJetty
        // [ ] Configure Jetty for HTTP/2.
        // [ ] Configure Jetty for PROXY
        // https://www.eclipse.org/jetty/documentation/jetty-10/programming-guide/index.html#pg-server-http-connector
    }
}
