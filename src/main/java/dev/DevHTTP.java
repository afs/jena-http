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

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.sparq.exec.QueryExec;
import org.apache.jena.sparq.exec.QueryExecutionAdapter;
import org.apache.jena.sparq.exec.http.QueryExecHTTP;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

public class DevHTTP {
    static {
        FusekiLogging.setLogging();
        //LogCtl.setLog4j2();
    }

    public static void main(String...args) {
        FusekiLogging.setLogging();

        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        int port = 3330;   // Default port.
        FusekiServer server = FusekiServer.create()
                .add("/ds", dsg)
                .verbose(true)
                .build();
        server.start();
        String URL = "http://localhost:"+port+"/ds/query";
        String queryString = "SELECT * { ?s ?p ?o }";

        try {
//            // Polling client - Transfer-encoding: chunked.
//            //HttpOp.getDefaultHttpClient();
//
//            HttpClient hc = HttpClient.
//                    .chunkedTransfer(false)
//                    .build();
//            hc.getConnectionManager().c
//            HttpOp.setDefaultHttpClient(hc);

            // Transfer-encoding: chunked because of streaming.
            // But does not work (easily) with gzip.
            // so don't.

            // ** Disable compression in old QueryEngineHTTP
            // ** Disable compression output in Fuseki + HTTP 1.1
            try (QueryExecution qExec = QueryExecutionFactory.sparqlService(URL, queryString) ) {
                ResultSet rs = qExec.execSelect();
                ResultSetFormatter.consume(rs);
        }

        // OK
//        QueryExec qExec = QueryExecHTTP.newBuilder()
//                .service("http://localhost:"+port+"/ds/query")
//                .queryString( "SELECT * { ?s ?p ?o}")
//                .build();
        } finally {
            server.stop();
        }
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

//        for ( var qs : x ) {
//            try ( QueryExecution qexec = QueryExecutionHTTP.newBuilder()
//                    .service("http://localhost:3030/ds/query")
//                    .queryString(qs)
//                    .build()) {
//                QueryExecUtils.executeQuery(qexec);
//            }
//        }

      for ( var qs : x ) {
      try ( QueryExec qexec = QueryExecHTTP.newBuilder()
              .service("http://localhost:3030/ds/query")
              .queryString(qs)
              .build()) {
          QueryExecUtils.executeQuery(QueryExecutionAdapter.adapt(qexec));
      }
  }

    }
}
