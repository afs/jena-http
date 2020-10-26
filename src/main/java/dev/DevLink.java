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

import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.seaborne.link.RDFConnectionAdapter;
import org.seaborne.link.RDFLink;
import org.seaborne.link.RDFLinkFactory;

public class DevLink {
    static {
        //System.setProperty("jdk.internal.httpclient.debug", "true");
        System.setProperty("jdk.httpclient.HttpClient.log", "requests,headers");

        RIOT.getContext().set(RIOT.symTurtleDirectiveStyle, "sparql");
        FusekiLogging.setLogging();
        }

//    Oct 26, 2020 3:50:52 PM jdk.internal.net.http.Http1Request headers
//    INFO: REQUEST: http://localhost:34809/ds POST
//    Oct 26, 2020 3:50:52 PM jdk.internal.net.http.Http1Request logHeaders
//    INFO: HEADERS: REQUEST HEADERS:
//    POST /ds HTTP/1.1
//    Connection: Upgrade, HTTP2-Settings
//    Content-Length: 30
//    Host: localhost:34809
//    HTTP2-Settings: AAEAAEAAAAIAAAABAAMAAABkAAQBAAAAAAUAAEAA
//    Upgrade: h2c
//    User-Agent: Java-http-client/11.0.9
//    Content-Type: application/sparql-update

//    Oct 26, 2020 3:53:37 PM jdk.internal.net.http.Http1Response lambda$readHeadersAsync$0
//    INFO: HEADERS: RESPONSE HEADERS:
//        date: Mon, 26 Oct 2020 15:53:37 GMT
//        fuseki-request-id: 1
//        server: Apache Jena Fuseki (3.17.0-SNAPSHOT)
//
//    Oct 26, 2020 3:53:37 PM jdk.internal.net.http.Exchange lambda$wrapForLog$11
//    INFO: RESPONSE: (POST http://localhost:38267/ds) 204 HTTP_1_1 Local port:  37310
//    Oct 26, 2020 3:53:37 PM jdk.internal.net.http.Http1Request headers
//    INFO: REQUEST: http://localhost:38267/ds?default GET
//    Oct 26, 2020 3:53:37 PM jdk.internal.net.http.Http1Request logHeaders
//    INFO: HEADERS: REQUEST HEADERS:
//    GET /ds?default HTTP/1.1
//    Connection: Upgrade, HTTP2-Settings
//    Content-Length: 0
//    Host: localhost:38267
//    HTTP2-Settings: AAEAAEAAAAIAAAABAAMAAABkAAQBAAAAAAUAAEAA
//    Upgrade: h2c
//    User-Agent: Java-http-client/11.0.9
//    Accept: text/turtle,application/n-triples;q=0.9,application/ld+json;q=0.8,application/rdf+xml;q=0.7,*/*;q=0.3


// vs

//    15:51:50 INFO  HttpClient :: REQUEST: http://localhost:41567/ds POST
//    15:51:50 INFO  HttpClient :: HEADERS: REQUEST HEADERS:
//    {0}

//    15:51:50 INFO  HttpClient :: HEADERS: RESPONSE HEADERS:
//        date: Mon, 26 Oct 2020 15:51:50 GMT
//        fuseki-request-id: 1
//        server: Apache Jena Fuseki (3.17.0-SNAPSHOT)

// BUG? JDK11 - it print using {0} but that is specific to JUL


    public static void main(String...args) {

        int port = WebLib.choosePort();
        DatasetGraph dsg =  DatasetGraphFactory.createTxnMem();
        var server = FusekiServer.create()
            .port(port)
            //.verbose(true)
            .enablePing(true)
            .add("/ds", dsg)
            .build();
        server.start();

        try {
            //RDFLink link = RDFLinkFactory.connect(dsg);
            try ( RDFLink link = RDFLinkFactory.connect("http://localhost:"+port+"/ds") ) {
                RDFConnection conn = RDFConnectionAdapter.wrap(link);

                conn.update("INSERT DATA {<x:s> <x:p> 1914}");
                Model model = conn.fetch();
                RDFDataMgr.write(System.out, model, Lang.TTL);

                try ( QueryExecution qExec = conn.query("SELECT * { ?s ?p ?o }") ) {
                    QueryExecUtils.executeQuery(qExec);
                }
            }
        } finally { server.stop(); }
    }
}
