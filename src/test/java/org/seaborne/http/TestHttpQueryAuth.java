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

import static org.junit.Assert.fail;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.atlas.web.AuthScheme;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.auth.Auth;
import org.apache.jena.fuseki.jetty.JettyLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.test.FusekiTest;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.seaborne.unused.QueryExecutionHTTP;

public class TestHttpQueryAuth {
    // Superseded by TestAuthRemote

    private static FusekiServer server = null;
    private static String URL;
    private static String dsName = "/ds";
    private static String dsURL;

    @BeforeClass public static void beforeClass() {
        int port = WebLib.choosePort();
        URL = "http://localhost:"+port+"/";
        dsURL = "http://localhost:"+port+dsName;

        UserStore userStore = JettyLib.makeUserStore("u", "p");
        SecurityHandler sh = JettyLib.makeSecurityHandler("TripleStore",  userStore, AuthScheme.BASIC);
        server = FusekiServer.create()
            .add("/ds", DatasetGraphFactory.createTxnMem())
            .port(port)
            .securityHandler(sh)
            .serverAuthPolicy(Auth.policyAllowSpecific("u"))
            .build();
        server.start();
    }

    @AfterClass public static void afterClass() {
        if ( server != null ) {
            try { server.stop(); } finally { server = null; }
        }
    }

    @Test public void query_no_auth() {
        FusekiTest.expect401(()->{
            try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                    .service(dsURL).queryString("SELECT * { ?s ?p ?o }").build() ) {
                qExec.execSelect();
            }
        });
    }

    @Test public void query_one_time_auth() {
        // One time use.
        Authenticator authenticator = new Authenticator() {

            Map<URL, PasswordAuthentication> x = new ConcurrentHashMap<>();

            int retries = 0;

            // One time try.
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                URL url = this.getRequestingURL();
                x.computeIfAbsent(url, u->new PasswordAuthentication("wrongUser", "wrongPassword".toCharArray() ));
//
//                if ( this.x.containsKey(url) ) {}

//                if ( retries > 0 )
//                    throw new HttpException(401, "Failed Password Authentication (2 attempts)", null);
//                    //return null;
//                retries++;
                return new PasswordAuthentication("wrongUser", "wrongPassword".toCharArray());
            }
        };
        HttpClient httpClientAuth = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .authenticator(authenticator)
            .build();
        try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
            .httpClient(httpClientAuth)
            .service(dsURL).queryString("SELECT * { ?s ?p ?o }").build() ) {
            try {
                qExec.execSelect();
                fail("Did not trigger bad authentication");
            } catch (HttpException ex) {}
        }
    }

    @Test public void query_auth_httpClient() {
        // Configured HttpClient
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("u", "p".toCharArray());
            }
        };

        HttpClient httpClientAuth = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .authenticator(authenticator)
            .build();

        try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                .httpClient(httpClientAuth)
                .service(dsURL).queryString("SELECT * { ?s ?p ?o }").build() ) {
            qExec.execSelect();
        }
    }

    @Test public void query_auth_by_header() {
        // Manual setting, good.
        String x = HttpLib.basicAuth("u", "p");
        try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                .httpHeader(HttpNames.hAuthorization, x)
                .service(dsURL).queryString("SELECT * { ?s ?p ?o }").build() ) {
            qExec.execSelect();
        }
    }

    @Test public void query_auth_by_header_bad() {
        // Manual setting, bad.
        FusekiTest.expect401(()->{
            String x = HttpLib.basicAuth("u", "wrong");
            try ( QueryExecutionHTTP qExec = QueryExecutionHTTP.newBuilder()
                .httpHeader(HttpNames.hAuthorization, x)
                .service(dsURL).queryString("SELECT * { ?s ?p ?o }").build() ) {
                qExec.execSelect();
            }
        });
    }
}
