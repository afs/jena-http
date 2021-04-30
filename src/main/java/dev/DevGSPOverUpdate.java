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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.Target;
import org.apache.jena.sparql.modify.request.UpdateClear;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;

public class DevGSPOverUpdate {
    // GSP POST as INSERT DATA.

    static boolean IS_PUT = false;

    public static void main(String...args) {
        System.out.println("## Graph");
        UpdateRequest req1 = graphGSP();
        System.out.println(req1);

        System.out.println("## Dataset");

        UpdateRequest req2 = datasetGSP();
        System.out.println(req2);
    }

    private static UpdateRequest graphGSP() {
        Graph graph = SSE.parseGraph("(graph (:s :p :o) (:s :q 123 ))");

        // GSP target
        Node gn = Quad.defaultGraphIRI; //SSE.parseNode(":g1");

        List<Quad> quads = new ArrayList<>();
        // graph.stream()
        Iter.asStream(graph.find()).map(t->Quad.create(gn, t)).forEach(quads::add);
        QuadDataAcc qData = new QuadDataAcc(quads);
        Update update = new UpdateDataInsert(qData);
        UpdateRequest req = new UpdateRequest();
        req.getPrefixMapping().setNsPrefix("",  "http://example/");

        if ( IS_PUT ) {
            Target target = Target.DEFAULT;
            if ( gn != null && ! Quad.isDefaultGraph(gn) )
                target = Target.create(gn);
            req.add(new UpdateClear(target));
        }

        req.add(update);
        return req;
    }


    private static UpdateRequest datasetGSP() {
        Graph graph0 = SSE.parseGraph("(graph (:s :p :o) (:s :q 123 ))");
        Graph graph1 = SSE.parseGraph("(graph (:a :b :c) )");
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();

        Node gn = SSE.parseNode(":g1");

        GraphUtil.addInto(dsg.getDefaultGraph(), graph0);
        GraphUtil.addInto(dsg.getGraph(gn), graph1);

        List<Quad> quads = Iter.toList(dsg.find());
        QuadDataAcc qData = new QuadDataAcc(quads);
        Update update = new UpdateDataInsert(qData);
        UpdateRequest req = new UpdateRequest();
        req.getPrefixMapping().setNsPrefix("",  "http://example/");

        if ( IS_PUT ) {
            Target target = Target.ALL;
            req.add(new UpdateClear(target));
        }

        req.add(update);
        return req;
    }
}
