/**
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

package org.apache.jena.sparql.exec.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.http.HttpClient;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.conn.test.EnvTest;
import org.apache.jena.link.RDFLink;
import org.apache.jena.link.RDFLinkFactory;
import org.apache.jena.sparql.algebra.op.OpService ;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.sparql.engine.http.Service;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.Context ;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test Service implementation code -- Service.exec */
public class TestServiceAuth {

    static { Service.braveNewWorld = (op,cxt)->Service2.exec(op, cxt); }

    private static String SERVICE;
    private static final String USER = "user13";
    private static final String PASSWORD = "pw13";
    private static EnvTest env;

    @BeforeClass public static void beforeClass() {
        env = EnvTest.createAuth("/ds", DatasetGraphFactory.createTxnMem(), USER, PASSWORD);
        SERVICE = env.datasetURL();
    }

    @Before public void before() {
        env.clear();
    }

    @AfterClass public static void afterClass() {
        EnvTest.stop(env);
    }

    @Test(expected=QueryExceptionHTTP.class)
    public void service_auth_none() {
        OpService op = TestService2.makeOp(env);
        QueryIterator qIter = Service2.exec(op, new Context());
    }

    @Test public void service_auth_good_registry_1() {
        HttpClient hc = env.httpClientAuthGood();
        RegistryHttpClient.get().add(SERVICE, hc);
        try {

        OpService op = TestService2.makeOp(env);
        QueryIterator qIter = Service2.exec(op, null);
        assertNotNull(qIter);
        qIter.hasNext();
        } finally {
            RegistryHttpClient.get().remove(SERVICE);
        }
    }

    @Test public void service_auth_good_registry_2() {
        runWithPrefix(env.serverBaseURL(), env.httpClientAuthGood(), ()->{
            OpService op = TestService2.makeOp(env);
            QueryIterator qIter = Service2.exec(op, null);
            assertNotNull(qIter);
            qIter.hasNext();
        });
    }

    private static void runWith(String key, HttpClient hc, Runnable action) {
        RegistryHttpClient.get().add(SERVICE, hc);
        try {
            action.run();
        } finally {
            RegistryHttpClient.get().remove(SERVICE);
        }
    }

    private static void runWithPrefix(String prefix, HttpClient hc, Runnable action) {
        RegistryHttpClient.get().addPrefix(prefix, hc);
        try {
            action.run();
        } finally {
            RegistryHttpClient.get().remove(prefix);
        }
    }

    @Test public void service_auth_good_cxt() {
        OpService op = TestService2.makeOp(env);
        Context context = new Context();
        HttpClient hc = env.httpClientAuthGood();
        context.set(Service2.httpQueryClient, hc);
        QueryIterator qIter = Service2.exec(op, context);
    }


    @Test(expected=QueryExceptionHTTP.class)
    public void service_auth_bad_retries() {
        runWith(SERVICE, env.httpClientAuthBadRetry(), ()->{
            OpService op = TestService2.makeOp(env);
            Context context = new Context();
            QueryIterator qIter = Service2.exec(op, context);
        });
    }

    @Test public void service_query_auth_context() {
        DatasetGraph dsg = env.dsg();

        String queryString = "ASK { SERVICE <"+SERVICE+"> { BIND (123 AS ?X) }} ";

        // Context dataset graph.
        DatasetGraph local = DatasetGraphZero.create();
        Context context = local.getContext();
        HttpClient hc = env.httpClientAuthGood();
        context.set(Service2.httpQueryClient, hc);

        // Connect to local, unused, permanently empty dataset
        try ( RDFLink link = RDFLinkFactory.connect(local) ) {
            try ( QueryExec qExec = link.query(queryString) ) {
                boolean b = qExec.ask();
                assertTrue(b);
            }
        }
    }

    @Test(expected=QueryExceptionHTTP.class)
    public void service_query_auth_context_bad() {
        DatasetGraph dsg = env.dsg();
        dsg.executeWrite(()->dsg.add(SSE.parseQuad("(_ :s :p :o)")));

        String queryString = "SELECT * { SERVICE <"+SERVICE+"> { ?s ?p ?o }} ";

        // Context dataset graph.
        DatasetGraph local = DatasetGraphZero.create();
        Context context = local.getContext();
        HttpClient hc = env.httpClientAuthBad(); // Bad user/password.
        context.set(Service2.httpQueryClient, hc);

        RDFLink link = RDFLinkFactory.connect(local);
        try ( link ) {
            try ( QueryExec qExec = link.query(queryString) ) {
                RowSet rs = qExec.select();
                // OK here. Next line causes the SERVICE clause to execute.
                long x = Iter.count(rs);
                fail("Should not get this far");
            }
        }
    }

    public void service_query_auth_context_silent() {
        DatasetGraph dsg = env.dsg();
        dsg.executeWrite(()->dsg.add(SSE.parseQuad("(_ :s :p :o)")));

        String queryString = "SELECT * { SERVICE SILENT <"+SERVICE+"> { ?s ?p ?o }} ";

        // Context dataset graph.
        DatasetGraph local = DatasetGraphZero.create();
        Context context = local.getContext();
        HttpClient hc = env.httpClientAuthBad(); // Bad user/password.
        context.set(Service2.httpQueryClient, hc);

        RDFLink link = RDFLinkFactory.connect(local);
        try ( link ) {
            try ( QueryExec qExec = link.query(queryString) ) {
                RowSet rs = qExec.select();
                // OK here. Next line causes the SERVICE clause to execute.
                long x = Iter.count(rs);
                fail("Should not get this far");
            }
        }
    }

}
