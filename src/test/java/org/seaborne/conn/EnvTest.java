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

package org.seaborne.conn;

import org.apache.jena.atlas.web.AuthScheme;
import org.apache.jena.atlas.web.WebLib;
import org.apache.jena.fuseki.auth.Auth;
import org.apache.jena.fuseki.jetty.JettyLib;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.system.Txn;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;

/**
 * Helper for testing against a Fuseki server.
 * <p>
 * Example usage:
 * <pre>
 *    private static EnvTest env;
 *    {@literal @BeforeClass} public static void beforeClass() {
 *       env = EnvTest.create("/ds");
 *    }
 *
 *    {@literal @Before} public void before() {
 *       env.clear();
 *    }
 *
 *    {@literal @AfterClass} public static void afterClass() {
 *       EnvTest.stop(env);
 *    }
 * </pre>
 */
public class EnvTest {

/* Cut&Paste
    private static EnvTest env;
    @BeforeClass public static void beforeClass() {
        //FusekiLogging.setLogging();
        env = EnvTest.create("/ds");
    }

    @Before public void before() {
        env.clear();
    }

    @AfterClass public static void afterClass() {
        EnvTest.stop(env);
    }
*/
    public  final FusekiServer server;
    private final String dsName;
    private final DatasetGraph dataset;
    private final StringHolderServlet holder;
    private final String user;
    private final String password;

    public static EnvTest create(String dsName) {
        return new EnvTest(dsName, null, null, null);
    }

    public static EnvTest create(String dsName, DatasetGraph dsg) {
        return new EnvTest(dsName, dsg, null, null);
    }

    public static EnvTest createAuth(String dsName, DatasetGraph dsg, String user, String password) {
        return new EnvTest(dsName, dsg, user, password);
    }

    private EnvTest(String path, DatasetGraph dsg, String user, String password) {
        if ( ! path.startsWith("/") )
            path = "/"+path;
        if ( dsg == null )
            dsg = DatasetGraphFactory.createTxnMem();
        this.dsName = path;
        this.dataset = dsg;
        this.holder = new StringHolderServlet();
        this.user = user;
        this.password = password;
        server = startServer(dsName, dsg, holder, user, password);
    }

    private static FusekiServer startServer(String dsName, DatasetGraph dsg, StringHolderServlet holder, String user, String password) {
        if ( user != null && password == null )
            throw new IllegalArgumentException("User, not null, but  password null");
        if ( user != null ) {}

        String data = "/data";
        int port = WebLib.choosePort();
        FusekiServer.Builder builder = FusekiServer.create()
            .port(port)
            //.verbose(true)
            .enablePing(true)
            .addServlet(data, holder)
            .add(dsName, dsg);
        if ( user != null ) {
            UserStore userStore = JettyLib.makeUserStore(user, password);
            SecurityHandler sh = JettyLib.makeSecurityHandler("TripleStore",  userStore, AuthScheme.BASIC);
            builder.securityHandler(sh)
                   .serverAuthPolicy(Auth.policyAllowSpecific(user));
        }

        FusekiServer server = builder.build();
        server.start();
        return server;
    }

    public StringHolderServlet stringHolder() { return holder; }

    public String stringHolderPath() { return serverPath("data"); }

    public String dsName()      { return dsName; }
    public DatasetGraph dsg()   { return dataset; }
    public String user()        { return user; }
    public String password()    { return password; }


    public String serverPath(String path) {
        if ( path.startsWith("/") )
            path = path.substring(1);
        return serverBaseURL()+path;
    }

    public String serverBaseURL() {
        return "http://localhost:"+server.getPort()+"/";
    }

    public String datasetURL() {
        return serverPath(dsName());
    }

    public String datasetPath(String path) {
        String url = "http://localhost:"+server.getPort();
        if ( ! path.startsWith(dsName()) )
            path = dsName()+path;
        return url+path;
    }

    public void clear() {
        // Clear dataset before every test
        Txn.executeWrite(dsg(), ()->dsg().clear());
        stringHolder().clear();
    }

    /** Stop a test evironment - can pass a null as env */
    public static void stop(EnvTest env) {
        if ( env != null && env.server != null )
            env.server.stop();
    }
}
