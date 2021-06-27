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

import java.util.concurrent.CompletableFuture;

import org.apache.jena.graph.Graph;
import org.apache.jena.http.AsyncHttpRDF;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

public class DevAsync {

    // Run async
//    CompletableFuture.supplyAsync(() -> [result]).thenAccept(result -> [action]);
//
//    Or if you need error handling:
//
//    CompletableFuture.supplyAsync(() -> [result]).whenComplete((result, exception) ->
//
    public static void main(String... args) {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        StreamRDF dest = StreamRDFLib.dataset(dsg);
        CompletableFuture<Void> cfFetcher = AsyncHttpRDF.asyncGetToStream(HttpEnv.getDftHttpClient(),
                                                         "http://localhost:3030/ds",
                                                         null, dest, dsg);
        //cfFetcher.join();
        AsyncHttpRDF.syncOrElseThrow(cfFetcher);
        RDFDataMgr.write(System.out, dsg, Lang.TRIG);

        // This allocates a graph - ensures thread safe isolation.
        CompletableFuture<Graph> cfGraph = AsyncHttpRDF.asyncGetGraph(HttpEnv.getDftHttpClient(),
                                                                      "http://localhost:3030/ds?default");
        System.out.println();
        Graph graph = AsyncHttpRDF.getOrElseThrow(cfGraph);
        RDFDataMgr.write(System.out, graph, Lang.TTL);
        System.out.println("DONE");
    }


}
