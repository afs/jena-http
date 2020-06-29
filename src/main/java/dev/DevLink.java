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

import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.QueryExecUtils;
import org.seaborne.link.RDFConnectionAdapter;
import org.seaborne.link.RDFLink;
import org.seaborne.link.RDFLinkFactory;

public class DevLink {
    static {
        RIOT.getContext().set(RIOT.symTurtleDirectiveStyle, "sparql");
//        LogCtl.setLog4j2();
        }

    public static void main(String...args) {
        //QE.local().query("ASK{}").dataset(DatasetGraphFactory.create()).ask();

        Lang[] langs = { Lang.RDFTHRIFT, Lang.TTL, Lang.NQ };
        for ( Lang lang : langs ) {
            System.out.println(lang);
            System.out.println("  "+RDFLanguages.isTriples(lang)+" " +RDFLanguages.isQuads(lang));
        }
        System.exit(0);


        mainDev();
        //mainEncode();
    }

    public static void mainDev(String...args) {
        FusekiLogging.setLogging();

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
            RDFLink link = RDFLinkFactory.connect("http://localhost:"+port+"/ds");
            RDFConnection conn = RDFConnectionAdapter.wrap(link);

            conn.putDataset("D.ttl");

            conn.update("INSERT DATA {<x:s> <x:p> 1914}");
            Model model = conn.fetch();
            RDFDataMgr.write(System.out, model, Lang.TTL);

            try ( QueryExecution qExec = conn.query("SELECT * { ?s ?p ?o }") ) {
                QueryExecUtils.executeQuery(qExec);
            }
        } finally { server.stop(); }
    }

    public static void mainEncode() {
        String[] x = {
            "http://example/graph",
            "http://example/graph/-àá-/foo#bar",
            "http://example/graph/::/foo#bar/baz",
            "http://example/graph?name=value#zzzz"
        } ;
        for ( String s : x ) {
            String e = URLEncoder.encode(s, StandardCharsets.UTF_8);
            System.out.printf("%s  ==>  %s\n", s, e);
            String e2 = IRILib.encodeUriQueryFrag(s);
            System.out.printf("%s  ==>  %s\n", s, e2);
//
//            URI uri = null;
//            try {
//                uri = new URI(s);
//            } catch (URISyntaxException e1) {
//                e1.printStackTrace();
//            }
//            System.out.println(uri);
//            System.out.println(uri.toASCIIString());
        }


    }
}
