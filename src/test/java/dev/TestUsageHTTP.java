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

import static org.junit.Assert.assertNotNull;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.conn.test.EnvTest;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.link.RDFLink;
import org.apache.jena.link.RDFLinkRemote;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparq.exec.QueryExec;
import org.apache.jena.sparq.exec.RowSet;
import org.apache.jena.sparq.exec.RowSetFormatter;
import org.apache.jena.sparq.exec.http.GSP;
import org.apache.jena.sparq.exec.http.QueryExecHTTP;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sys.JenaSystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration testing of each way to speak to the server.
 *  With and without compression.
 */
public class TestUsageHTTP {
    static {
        JenaSystem.init();
        RIOT.getContext().set(RIOT.symTurtleDirectiveStyle, "sparql");
        FusekiLogging.setLogging();
    }

    public static EnvTest env;

    @BeforeClass public static void beforeClass() {
        EnvTest.VERBOSE = true;
        env = EnvTest.create("/ds");
        Graph data = SSE.parseGraph("(graph (:s :p :o))");
        GraphUtil.addInto(env.dsg().getDefaultGraph(), data);
    }

    @AfterClass public static void afterClass() {
        env.stop();
    }

    @Test
    public void httpop_old() {
        String URL = env.datasetURL()+"/?default";
        // Default may not turn off ContentCompression
        // and send "Accept-Encoding: gzip,deflate".
        // But that, plus streaming is complicated for unclear gains
        // on one-time objects as responses (compression is not free)
        HttpClient dftHC = HttpOp.getDefaultHttpClient();
        try {
            HttpClient hc = HttpClientBuilder
                    .create()
                    .disableContentCompression()
                    .build();
            HttpOp.setDefaultHttpClient(hc);
            String x1 = HttpOp.execHttpGetString(URL);
            assertNotNull(x1);
        } finally {
            HttpOp.setDefaultHttpClient(dftHC);
        }
    }

    @Test
    public void gsp_rdfconnection_old() {
        RDFConnection conn = RDFConnectionRemote.newBuilder()
                .destination(env.datasetURL())
                .build();
        //No compression.
        try ( conn ) {
            Model model = conn.fetch();
            assertNotNull(model);
        }
    }

    @Test
    public void queryexecutiuon_resultset_old() {
        QueryExecution qExec = QueryExecutionFactory.sparqlService(env.datasetURL(), "SELECT * {}");
        try ( qExec ) {
            ResultSet resultSet = qExec.execSelect();
            ResultSetFormatter.consume(resultSet);
        }
    }

    @Test
    public void queryexecutiuon_boolean_old() {
        QueryExecution qExec = QueryExecutionFactory.sparqlService(env.datasetURL(), "ASK {}");
        try ( qExec ) {
            qExec.execAsk();
        }
    }

    @Test
    public void rdfconnection_resultset_old() {
        try ( RDFConnection conn = RDFConnectionFactory.connect(env.datasetURL()) ) {
            conn.queryResultSet("SELECT * {}", ResultSetFormatter::consume);
        }
    }

    @Test
    public void rdfconnection_boolean_old() {
        try ( RDFConnection conn = RDFConnectionFactory.connect(env.datasetURL()) ) {
            conn.queryAsk("ASK {}");
        }
    }

    @Test
    public void rdflink_rowset() {
        try ( RDFLink conn = RDFLinkRemote.newBuilder()
                .destination(env.datasetURL())
                .build() ) {
//            Graph dftGraph = conn.fetch();
//            RDFDataMgr.write(System.out, dftGraph, Lang.TTL);
            conn.queryRowSet("SELECT * {}", RowSetFormatter::consume);
        }
    }

    @Test
    public void queryExecHTTP() {
        QueryExec qExec =
                QueryExecHTTP.newBuilder()
                .service(env.datasetURL())
                // [QExec] Does nothing?
                .allowCompression(true)
                .queryString("SELECT * {}")
                .build();

        try ( qExec ) {
            RowSet rs = qExec.select();
            RowSetFormatter.consume(rs);
        }
    }

    @Test
    public void gsp(){
        Graph graph = GSP.request(env.datasetURL()).defaultGraph().allowCompression(true).GET();
        assertNotNull(graph);
    }
}
