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

package org.seaborne.improvements;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.exec.UpdateExecBuilder;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.junit.Test;

public class TestUpdateBuilder {
    // Look for UpdateBuilder

    private static String PREFIX = "PREFIX : <http://example/> ";

    @Test public void update_1() {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        assertTrue(dsg.isEmpty());
        UpdateProcessor uExec = UpdateExecBuilder.newBuilder().dataset(dsg).update(PREFIX+"INSERT DATA { :x :p :o }").build();
        uExec.execute();
        assertFalse(dsg.isEmpty());
    }

    @Test public void update_2() {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        assertTrue(dsg.isEmpty());

        UpdateExecBuilder.newBuilder().update(PREFIX+"INSERT DATA { :x :p :o }").execute(dsg);

        assertFalse(dsg.isEmpty());
    }

    @Test public void update_3() {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        assertTrue(dsg.isEmpty());

        UpdateRequest update = UpdateFactory.create(PREFIX+"INSERT DATA { :x :p 123 } ; INSERT { ?x ?p 456 } WHERE { ?x ?p 123 }");
        UpdateExecBuilder.newBuilder().update(update).execute(dsg);

        assertEquals(2, dsg.getDefaultGraph().size());
    }



    private static long count(DatasetGraph dsg) { return Iter.count(dsg.find()); }
}
