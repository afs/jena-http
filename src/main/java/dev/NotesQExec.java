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
         */
    }

    void misc() {
        //Misc
        // EnvTest to org.apache.jena.test (integration testing)
        // [ ] QueryExecUtils.executeQuery to work on GPI. Deprecate rest. Redo.
        // [ ] Move builders into classes?? Or move all out
        // [ ] HttpLib : former "package" scope.
        // [ ] RequestLogging : See LogIt
        // [ ] create() vs newBuilder(). "localBuilder()", "remoteBuilder" / "newBuilder", "newBuilderHTTP"
        // 1 - make new world client work with compression (send only)
        // 2 - are the added "close" need? or is it a jetty buglet?
        // 3 - No default compression
        // 5 - Consider Content-Length calculation form.
    }

    void documentation() {
        // [ ] Document!
        //     Global -> dataset -> [fuseki service?] -> execution -> freeze
    }

    void compression() {
        // [ ] Off for responses in Fuseki. On for sending?
        //     Compression settings:
        //     HttpQuery, HttpOp.
        // [ ] Compression class of system constants
        //     Compression QueryEngineHTTP (pass through) and HttpQuery (decide here)
        // [ ] TestUsageHTTP
        // [ ] Then turn off in Fuseki (output stream never compressed)
    }

    void GSP() {
        // [x] Compression options? PUT, POST? HTTP/2 only?
        //     HttpLib body subscriber ->
        //     Done in HttpLib.handleResponseInputStream/getInputStream.
        //     But google for examples for jena.net.http
        // [ ] HTTP version
        // [ ] Always handle compressed responses (Content-Encoding)
        // [ ] Remove DatasetAccessor
    }

    void HttpOp2() {
        // [ ] Compression off (check)
        // [ ] Default configuration.
    }

    void HTTP() {
        // [ ] Run some integration tests in HTTP 1.1 mode
        // [ ] See what headers are not allowed in HTTP/2.
        // [ ] TestUsageHTTP : compression and no compression
    }

    void SERVICE() {
        // No first timeout (now in HttpClient)
        // [ ] Undo BraveNewWorld.
        // [ ] Do not optimize SERVICE - substitution only.
        // [ ] Context symbols: Old and new
        // [ ] Clean Service2
    }

    void RDFConnection() {
        // [ ] Adapter
        // [ ] RDFConnectionFactory - de-emphasise - refer to builders
        // [ ] RDFConnectionDatasetBuilder
    }

    void RDFLink() {
        // [ ] fetch -> GET etc
        // [ ] RDFLink .allowCompression does nothing. Remove
        //     Rename. .compressRespones(true), .compressDataSent(true)
        // [ ] Builders for RDFLinkDataset, RDFConnectionDataset
        // [ ] Use UpdateExecBuilder.
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

    void queryExecution() {
        // [ ] QueryExecutionFactory
        // [ ] ?leave/deprecate QueryEngineHttp using ApcheHttpClient for one or two releases?
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
        // HttpOp : Direct use of java.net.http covers the complex cases so new HttpOp is smaller
    }

    void update() {
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
        // UpdateExecBuilder
        // UpdateExecHTTPBuilder
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
