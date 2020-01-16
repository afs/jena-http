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
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;

import org.apache.jena.atlas.lib.DateTimeUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.atlas.web.AuthScheme;
import org.apache.jena.fuseki.auth.Auth;
import org.apache.jena.fuseki.jetty.JettyLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.seaborne.http.*;
import org.seaborne.http.ServiceRegistry.ServiceTuning;

public class DevAuthHTTP {
    static { LogCtl.setLog4j(); }

    public static void main(String...args) throws IOException, InterruptedException {
//        System.setProperty("jdk.internal.httpclient.debug", "true");
//        System.setProperty("jdk.httpclient.HttpClient.log", "requests");

//        -Djdk.httpclient.HttpClient.log=
//            errors,requests,headers,
//            frames[:control:data:window:all],content,ssl,trace,channel

//        String g = HttpRDF.httpGetString("http://www.sparql.org/D.ttl");
//        System.out.print(g);
//        System.exit(0);

        // Connection caching and pooling?

        UserStore userStore = JettyLib.makeUserStore("u", "p");
        SecurityHandler sh = JettyLib.makeSecurityHandler("TripleStore",  userStore, AuthScheme.BASIC);

        FusekiServer server = FusekiServer.create()
            //.parseConfigFile("/home/afs/tmp/config.ttl")
            .add("/ds", DatasetGraphFactory.createTxnMem())
            .port(3030)
            .securityHandler(sh)
            .serverAuthPolicy(Auth.policyAllowSpecific("u"))
            //.verbose(true)
            .addServlet("/data", new StringHolderServlet())
            .build();
        server.start();
        //server.start().join();


        try {
            //clientBasic();
            clientQueryExec();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } finally  { server.stop(); }
    }



    private static void clientQueryExec() {

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                //System.err.println("**** getPasswordAuthentication");
                return new PasswordAuthentication("u", "p".toCharArray());
            }
        };

        HttpClient hc = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            //.authenticator(authenticator)
            .build();


        // Query
        ServiceTuning mods = (params, headers) ->
             headers.put(HttpNames.hAuthorization, HttpLib.basicAuth("u", "p"));

        // Need to do that for update.
        ServiceRegistry svcReg = new ServiceRegistry();
        svcReg.add("http://localhost:3030/ds", mods);
        ARQ.getContext().put(ARQ.serviceParams, svcReg);


        UpdateExecutionHTTP uExec = UpdateExecutionHTTP.newBuilder()
            .service("http://localhost:3030/ds")
            .update("INSERT DATA { <x:s> <x:q> 123}")
            .httpHeader(HttpNames.hAuthorization, HttpLib.basicAuth("u", "p"))
            //.httpClient(hc)
            .build();
        uExec.execute();


        String[] x = {
            "SELECT * { ?s ?p ?o }"
            , "ASK  {}"
            , "CONSTRUCT WHERE { ?s ?p ?o }"
        };

        for ( var qs : x ) {
            try ( QueryExecution qexec = QueryExecutionHTTP.newBuilder()
                    .httpClient(hc)
                    .service("http://localhost:3030/ds/query")
                    .queryString(qs)
                    .build()) {
                QueryExecUtils.executeQuery(qexec);
            }
        }

//        if ( true ) return;
//        try ( QueryExecution qexec = new QueryExecutionHTTP("http://localhost:3030/ds/query", "SELECT * { BIND(1 AS ?X) }") ) {
//            ResultSet rs = qexec.execSelect();
//            ResultSetFormatter.out(rs);
//        }
//        try ( QueryExecution qexec = new QueryExecutionHTTP("http://localhost:3030/ds/query", "ASK  {}" ) ) {
//            boolean b = qexec.execAsk();
//        }
//        try ( QueryExecution qexec = new QueryExecutionHTTP("http://localhost:3030/ds/query", "CONSTRUCT {<x:s> <x:p> 123 } WHERE {}" ) ) {
//            Model m = qexec.execConstruct();
//            RDFDataMgr.write(System.out,  m, Lang.RDFXML);
//        }

    }

    private static void clientBasic() {

        // 404 - test needed
        //String x = HttpRDF.httpGetString("http://localhost:3030/no");


        HttpRDF.httpGetGraph("http://localhost:3030/no");

        if ( true ) return;

        HttpOp2.httpPut("http://localhost:3030/data", "text/plain", BodyPublishers.ofString("TEST"));
        printServletGet("http://localhost:3030/data");

        HttpOp2.httpPost("http://localhost:3030/data", "text/plain", BodyPublishers.ofString("2"));
        printServletGet("http://localhost:3030/data");

        HttpOp2.httpDelete("http://localhost:3030/data");
        printServletGet("http://localhost:3030/data");

        if ( true ) return;

        Graph g1 = SSE.parseGraph("(graph (:s :p '"+DateTimeUtils.nowAsString()+"'))");
        HttpRDF.httpPostGraph("http://localhost:3030/ds?default", g1);
        Graph g2 = HttpRDF.httpGetGraph("http://localhost:3030/ds?default");
        System.out.println();
        RDFDataMgr.write(System.out, g2, Lang.TTL);
        System.out.println();
    }

    public static void printServletGet(String url) {
        String s = HttpOp2.httpGetString(url);
        if ( s.isEmpty() )
            System.out.println("<empty>");
        else
            System.out.println(s);
    }
}