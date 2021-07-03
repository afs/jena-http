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
import org.junit.Test;

public class TestExecutionBuilders {
    @Test public void builder_update_1() {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        assertTrue(dsg.isEmpty());
        dsg.execute(()->{
            UpdateExecutionBuilder.newBuilder()
                .dataset(dsg)
                .add("INSERT DATA { <x:s> <x:p> <x:o> }")
                .execute();
        });
        assertFalse(dsg.isEmpty());
    }

    @Test public void builder_update_2() {
        DatasetGraph dsg = DatasetGraphFactory.createTxnMem();
        assertTrue(dsg.isEmpty());
        dsg.execute(()->{
            UpdateExecutionBuilder.newBuilder()
                .dataset(dsg)
                .add("INSERT DATA { <x:s> <x:p> <x:o1> }")
                .add("INSERT DATA { <x:s> <x:p> <x:o2> }")
                .execute();
        });
        assertEquals(2, Iter.count(dsg.find()));
    }

}
