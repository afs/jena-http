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

import java.io.IOException;

import org.apache.jena.atlas.lib.DateTimeUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.sse.SSE;
import org.seaborne.http.HttpRDF;

public class DevGSP {
    static { LogCtl.setLog4j(); }

    public static void main(String...args) throws IOException, InterruptedException {
//        String g = HttpRDF.httpGetString("http://www.sparql.org/D.ttl");
//        System.out.print(g);
//        System.exit(0);

        // Connection caching and pooling?

        FusekiServer server = FusekiServer.create()
            //.parseConfigFile("/home/afs/tmp/config.ttl")
            .add("/ds", DatasetGraphFactory.createTxnMem())
            .port(3030)
            //.verbose(true)
            //.addServlet("/ds/plain", new Foo())
            //.staticFileBase("Files")
            .build();
        server.start();
        //server.start().join();


        try { clientMain(); }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally  { server.stop(); }
    }


    private static void clientMain() {
        Graph g1 = SSE.parseGraph("(graph (:s :p '"+DateTimeUtils.nowAsString()+"'))");
        HttpRDF.httpPostGraph("http://localhost:3030/ds?default", g1);
        Graph g2 = HttpRDF.httpGetGraph("http://localhost:3030/ds?default");
        System.out.println();
        RDFDataMgr.write(System.out, g2, Lang.TTL);
        System.out.println();
    }
}
