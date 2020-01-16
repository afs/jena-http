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

import static org.apache.jena.sparql.sse.SSE.parseQuad;
import static org.junit.Assert.assertTrue;

import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestUpdateExecutionHTTP {

    private static FusekiServer server = null;
    private static String URL;
    private static String dsName = "/ds";
    private static String dsURL;
    private static Quad q0 = parseQuad("(_ :s :p :o)");
    private static Quad q1 = parseQuad("(:g1 :s :p 1)");
    private static Quad q2 = parseQuad("(:g2 :s :p 2)");

    static {
        if ( false )
            LogCtl.enable(Fuseki.actionLog);
    }

    @BeforeClass public static void beforeClass() {
        int port = WebLib.choosePort();
        URL = "http://localhost:"+port+"/";
        dsURL = "http://localhost:"+port+dsName;
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        server = FusekiServer.create()
            .port(port)
            .verbose(true)
            .add(dsName, dsg)
            .build();
        server.start();
    }

    @AfterClass public static void afterClass() {
        if ( server != null ) {
            try { server.stop(); } finally { server = null; }
        }
    }

    private static void clear() {
        UpdateExecutionHTTP.newBuilder()
            .service(service())
            .update("CLEAR ALL")
            .build()
            .execute();
    }

    private static String service() { return dsURL; }
    private static String serviceQuery() { return dsURL+"/query"; }


    @Test public void update_1() {
        UpdateExecutionHTTP uExec = UpdateExecutionHTTP.newBuilder()
            .service(service())
            .update("INSERT DATA { <x:s> <x:p> 234 } ")
            .build();
        uExec.execute();
        try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                .service(serviceQuery())
                .queryString("ASK { ?s ?p 234 }")
                .build()) {
            boolean b = qExec.execAsk();
            assertTrue(b);
        }
    }

    @Test public void update_form_2() {
        UpdateRequest req = UpdateFactory.create("INSERT DATA { <x:s> <x:p> 567 } ");
        UpdateExecutionHTTP uExec = UpdateExecutionHTTP.newBuilder()
            .service(service())
            .sendHtmlForm(true)
            .update(req)
            .build();
        uExec.execute();
        try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                .service(serviceQuery())
                .queryString("ASK { ?s ?p 567 }")
                .build()) {
            boolean b = qExec.execAsk();
            assertTrue(b);
        }
    }

    // ?user-graph-uri= and ?using-named-graph-uri only apply to the WHERE clause of
    // an update.

    @Test public void update_using_1() {
        try {
            update_using_1_test();
        } finally {
            clear();
        }
    }

    private void update_using_1_test() {
        {
            UpdateRequest req1 = UpdateFactory.create("INSERT DATA { GRAPH <http://example/gg> { <x:s> <x:p> 567 } }");
            UpdateExecutionHTTP uExec1 = UpdateExecutionHTTP.newBuilder()
                .service(service()).update(req1)
                .build();
            uExec1.execute();
        }
        {
            // Should apply the
            UpdateRequest req2 = UpdateFactory.create("INSERT { <x:s1> <x:p1> ?o } WHERE { ?s ?p ?o }");
            UpdateExecutionHTTP uExec2 = UpdateExecutionHTTP.newBuilder()
                .service(service()).update(req2)
                .addUsingGraphURI("http://example/gg")
                .build();
            uExec2.execute();
        }

        try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                .service(serviceQuery())
                .queryString("ASK { ?s ?p 567 }")
                .build()) {
            boolean b = qExec.execAsk();
            assertTrue(b);
        }
    }
}
