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

package org.seaborne.integration;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.time.Duration;

import org.apache.jena.fuseki.test.FusekiTest;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.seaborne.conn.EnvTest;
import org.seaborne.http.GSP;
import org.seaborne.http.QueryExecutionHTTP;
import org.seaborne.http.UpdateExecutionHTTP;
import org.seaborne.link.RDFLink;
import org.seaborne.link.RDFLinkFactory;
import org.seaborne.link.RDFLinkRemote;

/**
 * This is more than just RDFLinkRemote - it covers the components
 */
public class TestAuthRemote {
    private static String user = "user";
    private static String password = "passwrd";

    private static EnvTest env;
    @BeforeClass public static void beforeClass() {
        //FusekiLogging.setLogging();
        env = EnvTest.createAuth("/ds", DatasetGraphFactory.createTxnMem(), user, password);
    }

    @Before public void before() {
        env.clear();
    }

    @AfterClass public static void afterClass() {
        EnvTest.stop(env);
    }

    private static Authenticator authenticator = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(env.user(), env.password().toCharArray());
        }
    };

    private static HttpClient httpClientGood = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(authenticator)
        .build();

    private static Authenticator authenticatorBad = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("u", "p".toCharArray());
        }
    };

    private static HttpClient httpClientBad = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .authenticator(authenticatorBad)
        .build();

    // ---- QueryExecutionHTTP

    @Test
    public void auth_qe_no_auth() {
        FusekiTest.expect401(()->{
        try ( QueryExecution qexec = QueryExecutionHTTP.newBuilder()
                    //.httpClient(hc)
                    .service(env.datasetURL())
                    .queryString("ASK{}")
                    .build()) {
            qexec.execAsk();
            }
        });
    }

    @Test
    public void auth_qe_good_auth() {
        try ( QueryExecution qexec = QueryExecutionHTTP.newBuilder()
                        .httpClient(httpClientGood)
                        .service(env.datasetURL())
                        .queryString("ASK{}")
                        .build()) {
            qexec.execAsk();
        }
    }

    @Test
    public void auth_qe_bad_auth() {
        FusekiTest.expect403(()->{
            try ( QueryExecution qexec = QueryExecutionHTTP.newBuilder()
                            .httpClient(httpClientBad)
                            .service(env.datasetURL())
                            .queryString("ASK{}")
                            .build()) {
                qexec.execAsk();
            }
        });
    }

    // ---- UpdateExecutionHTTP

    @Test
    public void auth_update_no_auth() {
        FusekiTest.expect401(()->
            UpdateExecutionHTTP.newBuilder()
                .service(env.datasetURL())
                .updateString("INSERT DATA { <x:s> <x:p> <x:o> }")
                .build()
                .execute()
        );
    }

    @Test
    public void auth_update_good_auth() {
        UpdateExecutionHTTP.newBuilder()
            .httpClient(httpClientGood)
            .service(env.datasetURL())
            .updateString("INSERT DATA { <x:s> <x:p> <x:o> }")
            .build()
            .execute();
    }

    @Test
    public void auth_update_bad_auth() {
        FusekiTest.expect403(()->
            UpdateExecutionHTTP.newBuilder()
                .httpClient(httpClientBad)
                .service(env.datasetURL())
                .updateString("INSERT DATA { <x:s> <x:p> <x:o> }")
                .build()
                .execute()
        );
    }

    // ---- GSP

    @Test
    public void auth_gsp_no_auth() {
        FusekiTest.expect401(()->{
            GSP.request(env.datasetURL()).defaultGraph().GET();
        });
    }

    @Test
    public void auth_gsp_good_auth() {
        GSP.request(env.datasetURL()).httpClient(httpClientGood).defaultGraph().GET();
    }

    @Test
    public void auth_gsp_bad_auth() {
        FusekiTest.expect403(()->
            GSP.request(env.datasetURL()).httpClient(httpClientBad).defaultGraph().GET()
        );
    }

    // RDFLink

    @Test
    public void auth_link_no_auth_1() {
        FusekiTest.expect401(()->{
            try ( RDFLink link = RDFLinkFactory.connect(env.datasetURL()) ) {
                link.queryAsk("ASK{}");
            }
        });
    }

    @Test
    public void auth_link_no_auth_2() {
        FusekiTest.expect401(()->{
            try ( RDFLink link = RDFLinkFactory.connect(env.datasetURL()) ) {
                link.update("INSERT DATA { <x:s> <x:p> <x:o> }");
            }
        });
    }

    @Test
    public void auth_link_no_auth_3() {
        FusekiTest.expect401(()->{
            try ( RDFLink link = RDFLinkFactory.connect(env.datasetURL()) ) {
                link.fetch();
            }
        });
    }

    @Test
    public void auth_link_good_auth() {
        try ( RDFLink link = RDFLinkRemote.create()
                    .destination(env.datasetURL())
                    .httpClient(httpClientGood)
                    .build()) {
            link.queryAsk("ASK{}");
            link.update("INSERT DATA { <x:s> <x:p> <x:o> }");
            link.fetch();
        }
    }

    @Test
    public void auth_link_bad_auth_1() {
        FusekiTest.expect403(()->{
            try ( RDFLink link = RDFLinkRemote.create()
                    .destination(env.datasetURL())
                    .httpClient(httpClientBad)
                    .build()) {
            link.queryAsk("ASK{}");
            }
        });
    }

    @Test
    public void auth_link_bad_auth_2() {
        FusekiTest.expect403(()->{
            try ( RDFLink link = RDFLinkRemote.create()
                    .destination(env.datasetURL())
                    .httpClient(httpClientBad)
                    .build()) {
            link.update("INSERT DATA { <x:s> <x:p> <x:o> }");
            }
        });
    }

    @Test
    public void auth_link_bad_auth_3() {
        FusekiTest.expect403(()->{
            try ( RDFLink link = RDFLinkRemote.create()
                        .destination(env.datasetURL())
                        .httpClient(httpClientBad)
                        .build()) {
                link.fetch();
            }
        });
    }
}

