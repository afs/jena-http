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

package org.seaborne.examples;

import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.http.GSP;
import org.apache.jena.http.UpdateExecutionHTTP;
import org.apache.jena.http.UpdateExecutionHTTPBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.sse.SSE;

public class ExGSP {

    public static String URL;

    public static UpdateExecutionHTTPBuilder factoryClearAll;

    public static void main(String ...a) {
        FusekiLogging.setLogging();
        String dsPath = "/ds";
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        FusekiServer server = FusekiServer.make(0, dsPath, dsg);

        server.start();
        // Find port allocated by OS
        int port = WebLib.choosePort();
        URL = "http://localhost:"+port+dsPath;
        factoryClearAll = UpdateExecutionHTTP.newBuilder().service(URL).updateString("CLEAR ALL");

        try {
            ExGSP_addTriple();
            ExGSP_getDefaultGraph();

            clear();

            ExGSP_addTripleNamedGraph();
            ExGSP_getNamedGraph();

            clear();

            ExGSP_addQuad();
            ExGSP_getDataset();


        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            server.stop();
            System.exit(0);
        }
    }

    static void clear() {
        factoryClearAll.build().execute();
    }

    // -- Basic GSP - default graph

    public static void ExGSP_addTriple() {
        Triple triple = SSE.parseTriple("(:s :p 234)");
        Graph graph = GraphFactory.createDefaultGraph();
        graph.add(triple);

        // POST ("append to", "add triples")
        GSP.request(URL).defaultGraph().POST(graph);
    }

    public static void ExGSP_getDefaultGraph() {
        Graph graph = GSP.request(URL).defaultGraph().GET();
        System.out.println();
        RDFDataMgr.write(System.out, graph, Lang.TTL);
        System.out.println();
    }

    // -- Named graph

    public static void ExGSP_addTripleNamedGraph() {
        Node gn = SSE.parseNode(":g");
        Triple triple = SSE.parseTriple("(:s :p 234)");
        Graph graph = GraphFactory.createDefaultGraph();
        graph.add(triple);

        GSP.request(URL).graphName(gn).POST(graph);
    }

    public static void ExGSP_getNamedGraph() {
        Node gn = SSE.parseNode(":g");
        Graph graph = GSP.request(URL).graphName(gn).GET();
        System.out.println();
        RDFDataMgr.write(System.out, graph, Lang.TTL);
        System.out.println();
    }

    // -- Quad - dataset operations

    public static void ExGSP_addQuad() {
        Quad quad1 = SSE.parseQuad("(_ :s :p 567)");
        Quad quad2 = SSE.parseQuad("(:g :s :p 234)");
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        dsg.add(quad1);
        dsg.add(quad2);
        GSP.request(URL).postDataset(dsg);
    }

    public static void ExGSP_getDataset() {
        DatasetGraph dsg = GSP.request(URL).getDataset();
        System.out.println();
        RDFDataMgr.write(System.out, dsg, Lang.TRIG);
        System.out.println();
    }
}

