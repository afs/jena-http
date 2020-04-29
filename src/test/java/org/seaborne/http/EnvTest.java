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

import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

public class EnvTest {

    public final FusekiServer server;
    private final String dsName;

    public EnvTest(String dsName) {
        this(dsName, DatasetGraphFactory.createTxnMem());
    }

    public EnvTest(String path, DatasetGraph dsg) {
        if ( ! path.startsWith("/") )
            path = "/"+path;
        this.dsName = path;
        server = startServer(dsName, dsg);
    }



//    public static EnvTest env;
//    @BeforeClass public static void beforeClass() {
//        env = new EnvTest("/ds");
//    }
//
//    @AfterClass public static void afterClass() {
//        Envtest.stop();
//    }

    private static FusekiServer startServer(String dsName, DatasetGraph dsg) {
        int port = WebLib.choosePort();
        FusekiServer server = FusekiServer.create()
            .port(port)
            //.verbose(true)
            .enablePing(true)
            .add(dsName, dsg)
            .build();
        server.start();
        return server;
    }

    public String serverBaseURL() {
        return "http://localhost:"+server.getPort()+"/";
    }

    // Typicsally path includes dsName
    public String url(String path) {
        String url = "http://localhost:"+server.getPort();
        if ( ! path.startsWith(dsName) )
            path = dsName+path;
        return url+path;
    }

    public static void stop(EnvTest env) {
        if ( env != null && env.server != null )
            env.server.stop();
    }
}
