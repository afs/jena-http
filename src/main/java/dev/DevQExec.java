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

import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.link.RDFLink;
import org.apache.jena.link.RDFLinkFactory;
import org.apache.jena.query.*;
import org.apache.jena.queryexec.QueryExec;
import org.apache.jena.queryexec.RowSet;
import org.apache.jena.queryexec.RowSetFormatter;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sys.JenaSystem;

public class DevQExec {
    static {
        JenaSystem.init();
        RIOT.getContext().set(RIOT.symTurtleDirectiveStyle, "sparql");
        FusekiLogging.setLogging();
    }

    public static void main(String... args){
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        Query query = QueryFactory.create("SELECT * { VALUES (?x ?y) { (1 2) (3 '4') } }");

        try ( QueryExecution qExec = QueryExecution.create()
                .dataset(dsg)
                .query(query)
                .build(); ) {
            ResultSet rs = qExec.execSelect();
            ResultSetFormatter.out(rs);
        }

        try ( RDFLink link = RDFLinkFactory.connect(dsg) ) {
            link.queryRowSet(query, rowSet -> RowSetFormatter.out(rowSet));
        }

        RowSet rowSet = QueryExec.newBuilder().dataset(dsg).query(query).select();
        RowSetFormatter.out(rowSet);
    }
}
