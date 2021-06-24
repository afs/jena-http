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
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sys.JenaSystem;
import org.seaborne.wip.QExec;
import org.seaborne.wip.ResultSetAdapter;
import org.seaborne.wip.RowSet;

public class DevQExec {
    static {
        JenaSystem.init();
        RIOT.getContext().set(RIOT.symTurtleDirectiveStyle, "sparql");
        FusekiLogging.setLogging();
        }

    // [QExec]
    // in "wip"

    // [x] QExec is the Node level version.
    // [x] QExec.Builder.
    // [x] QueryExecutionAdapter -- concrete adapter, not abstract.
    // [x] Use rewrite for initial bindings.
    // [?] QueryEngineFactory API change (migration).
    // [ ] Relation to RDFLink.
    // [ ] QExec tests. Or complete switch over!
    // [ ] The other adpater.

    // ----

    // Base level : Iterator<Binding> iter
    //   ResultTable = Iterator<Binding> + vars
    //   QueryEngineBase.evaluate produces QueryIterator: hide and only Iterator<Binding>

    // 1 - ResultSet reader-writers
    // Bridge? ResultSetFactory.create(QueryIterator, List<vars>)
    //
    // Where does an app get them from? QueryExecution simpler equivalent.
    // QExec.create().


    public static void main(String... args){

        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        Query query = QueryFactory.create("SELECT * { VALUES (?x ?y) { (1 2) (3 '4') } }");
//        QueryExecution.create()
//            .dataset(dsg)
//            .query(query)
//            .select(System.out::println);

//        try ( qExec ) {
//            ResultSet rs = qExec.execSelect();
//            ResultSetFormatter.out(rs);
//        }

        //try ( RowSet rs = new QExecBuilder().dataset(dsg).query(query).select() ) {
        RowSet rowSet = QExec.create().dataset(dsg).query(query).select();
        ResultSet resultSet = new ResultSetAdapter(rowSet);
        ResultSetFormatter.out(resultSet);
    }
}
