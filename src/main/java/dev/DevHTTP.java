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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.Syntax;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.UsingList;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.seaborne.http.QueryExecutionHTTP;

public class DevHTTP {
    static { LogCtl.setLog4j(); }

    public static void main(String...args) {
        //String updateString = "INSERT DATA { <x:s> <x:q> 123}";
        String updateString = "INSERT { <x:s> <x:q> ?o} WHERE { ?s ?p ?o }";

        UsingList usingList = new UsingList();
        //usingList.addUsing(NodeFactory.createURI("http://example/gx"));
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        Quad q = SSE.parseQuad("(:g :s :p 456)");
        dsg.add(q);

        // Not updating the underlying graph!
        //DatasetGraph dsg = DatasetGraphFactory.createGeneral();

        byte[] b = StrUtils.asUTF8bytes(updateString);
        ByteArrayInputStream input = new ByteArrayInputStream(b);

        UpdateAction.parseExecute(usingList, dsg, input, "http://server/unset-base/", Syntax.syntaxARQ);

        RDFDataMgr.write(System.out,  dsg, Lang.TRIG);
        System.out.println("----");
    }

    public static void mainX(String...args) throws IOException, InterruptedException {
        FusekiServer server = FusekiServer.create()
            //.parseConfigFile("/home/afs/tmp/config.ttl")
            .add("/ds", DatasetGraphFactory.createTxnMem())
            .port(3030)
            //.verbose(true)
            .addServlet("/data", new TestServlet())
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
