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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.seaborne.http.GSP;
import org.seaborne.http.HttpOp2;
import org.seaborne.http.QueryExecutionHTTP;

public class DevHTTP {
    static { LogCtl.setLog4j2(); }

    public static void main(String...args) {

        FusekiServer server = FusekiServer.create()
            //.parseConfigFile("/home/afs/tmp/config.ttl")
            .add("/ds", DatasetGraphFactory.createTxnMem())
            .port(3030)
            //.verbose(true)
            .build();
        server.start();
        //server.start().join();

        try {
            //clientBasic();
            //clientQueryExec();

            GSP.request("http://localhost:3030/ds")
                .defaultGraph()
                .POST("/home/afs/tmp/D.ttl");
            Graph graph = GSP.request("http://localhost:3030/ds")
                .defaultGraph()
                .GET();
            RDFDataMgr.write(System.out, graph, Lang.TTL);
            GSP.request("http://localhost:3030/ds")
                .graphName("http;//graph/")
                .POST("/home/afs/tmp/D.ttl");
            String x = HttpOp2.httpGetString("http://localhost:3030/ds");
            System.out.print(x);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } finally  { server.stop(); }
    }

    private static void clientQueryExec() {

        //String updateString = "INSERT DATA { <x:s> <x:q> 123}";
        String updateString = "INSERT { <x:s> <x:q> 123} WHERE {}";
        UpdateRequest req = UpdateFactory.create(updateString);

        String URL = "http://localhost:3030/ds/update";
        String graph = "http://example/gx";
        URL = URL+"?"+HttpNames.paramUsingGraphURI+"="+URLEncoder.encode(graph, StandardCharsets.UTF_8);

        //UpdateExecutionFactory.createRemote(req, URL).execute();
        HttpOp.execHttpPost(URL, WebContent.contentTypeSPARQLUpdate, updateString);


//        UpdateExecutionHTTP uExec = UpdateExecutionHTTP.newBuilder()
//            .service("http://localhost:3030/ds")
//            .updateString("INSERT DATA { <x:s> <x:q> 123}")
//            .build();
//        uExec.execute();

        String[] x = {
            "SELECT * { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } } }"
//            , "ASK  {}"
//            , "CONSTRUCT WHERE { ?s ?p ?o }"
        };

        for ( var qs : x ) {
            try ( QueryExecution qexec = QueryExecutionHTTP.newBuilder()
                    .service("http://localhost:3030/ds/query")
                    .queryString(qs)
                    .build()) {
                QueryExecUtils.executeQuery(qexec);
            }
        }
    }
}
