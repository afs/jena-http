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

import org.junit.Test;
import static org.apache.jena.fuseki.test.FusekiTest.expect404;
import static org.junit.Assert.assertFalse;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.sse.SSE;

public class TestGSP extends AbstractTestRDF  {

    private static Graph graph1 = SSE.parseGraph("(graph (:s :p :x) (:s :p 1))");
    private static Graph graph2 = SSE.parseGraph("(graph (:s :p :x) (:s :p 2))");

    @Test public void gsp_get_dft() {
        GSP.request(url("/ds")).defaultGraph().GET();
        GSP.request(url("/ds")).defaultGraph().PUT(graph1);
        Graph g = GSP.request(url("/ds")).defaultGraph().GET();
        assertFalse(g.isEmpty());
    }

    @Test public void gsp_named_1() {
        expect404(()->GSP.request(url("/ds")).graphName("http://host/graph").GET());
        GSP.request(url("/ds")).graphName("http://host/graph").POST(graph1);
        GSP.request(url("/ds")).graphName("http://host/graph").GET();
        GSP.request(url("/ds")).graphName("http://host/graph").DELETE();
        expect404(()->GSP.request(url("/ds")).graphName("http://host/graph").GET());
    }

    // XXX TestGSP : more tests, inc files.

}
