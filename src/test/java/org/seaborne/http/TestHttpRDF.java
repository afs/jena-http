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

package org.seaborne.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;

import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.sse.SSE;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestHttpRDF {
    private static FusekiServer server;
    private static DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
    private static int port = -1;
    private static String dsName = "/ds";
    private static String URL;

    // Includes the trailing "/" so it is correct in itself.
    private static String urlRoot() { return URL; }

    private static String url(String path) {
        if ( path.startsWith("/") )
            path = path.substring(1);
        return URL+path;
    }

    private static String srvUrl(String path) {
        if ( ! path.startsWith("/") )
            path = "/"+path;
        return url(dsName)+path;
    }

    @BeforeClass public static void beforeClass() {
        port = WebLib.choosePort();
        server = FusekiServer.create()
            .port(port)
            .verbose(true)
            .enablePing(true)
            .add(dsName, dsg)
            .build();
        server.start();
        URL = "http://localhost:"+port+"/";
    }
    // -- XXX Common
    static BodyPublisher graphString() { return BodyPublishers.ofString("PREFIX : <http://example/> :s :p :o ."); }

    static BodyPublisher datasetString() {return BodyPublishers.ofString("PREFIX : <http://example/> :s :p :o . :g { :sg :pg :og }"); }

    @Test public void httpRDF_01() {
        var graph = HttpRDF.httpGetGraph(url("/ds?default"));
        assertNotNull(graph);
        assertTrue(graph.isEmpty());
    }

    // Control conneg.
//    @Test public void httpRDF_02() {
//        Graph graph = HttpRDF.httpGetGraph(url("/data"), WebContent.contentTypeTurtle);
//        assertNotNull(graph);
//        assertTrue(graph.isEmpty());
//    }

    @Test public void httpRDF_03() {
        Graph graph1 = SSE.parseGraph("(graph (:s :p 1) (:s :p 2))");
        HttpRDF.httpPutGraph(url("/ds?default"), graph1);
        Graph graph2 = HttpRDF.httpGetGraph(url("/ds?default"));
        assertTrue(graph1.isIsomorphicWith(graph2));
    }

    // POST graph

    // DELETE
}
