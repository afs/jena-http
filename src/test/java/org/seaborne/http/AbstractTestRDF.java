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
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractTestRDF {
    protected static FusekiServer server;
    protected static DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
    protected static int port = -1;
    protected static String dsName = "/ds";
    protected static String URL;

    // Includes the trailing "/" so it is correct in itself.
    protected static String urlRoot() { return URL; }

    protected static String url(String path) {
        if ( path.startsWith("/") )
            path = path.substring(1);
        return URL+path;
    }

    protected static String srvUrl(String path) {
        if ( ! path.startsWith("/") )
            path = "/"+path;
        return url(dsName)+path;
    }

    @BeforeClass public static void beforeClass() {
        port = WebLib.choosePort();
        server = FusekiServer.create()
            .port(port)
            .verbose(true)
            .enablePing(true)
            .add(dsName, dsg)
            .build();
        server.start();
        URL = "http://localhost:"+port+"/";
    }

    @AfterClass public static void afterClass() {
        if ( server != null )
            server.stop();
    }
}
