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

    // ToDo
    // travis-ci - does not work with Jena snapshots



    // Try log4j-jul (JUL adapter)

    // Documentation
    //   updates for RDFConnction
    //   Details view
    // Jena networking markdown
    // * RDFConnection
    // * RDFLink
    // * SPARQL : GSP | QueryExecutionHTTP | UpdateExecutionHTTP
    // * HttpRDF
    // * HttpOp2
    // Client lib e.g. user+password setup

    // Check for javadoc
    // Examples!
    // Tests : More auth:

    // Tests of HttpEnv.getHttpClient



    // [ ] RequestLogging
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
    // [ ] Getting builders for UpdateExecution, QueryExecution; local and remote.
    //       Factory,library for getting one + QueryExecution : SparqlLib?

    // [ ] RDFLinkFactory == RDFLinkRemoteBuilder.create().destination(destination)
    // Better builder access (RDFLinkFactory? RDFLink.createRemote()?)


    // ----------------------------

    // package.html or web page.

    // * Fuseki binary -- add dataset operations.

    // [3]
    // Merge WebContent2 into WebContent (now?)

    // [4]
    // QueryTransformOps.transform - see builders and XXX

    // [5] Service.java

    // [MERGE]
    // getLink in RDFConnection.
    // Remove RDFConnection(Others)
    // Fuseki tests isFuseki

    // ---- Review
    // https://openjdk.java.net/groups/net/httpclient/recipes.html
}
