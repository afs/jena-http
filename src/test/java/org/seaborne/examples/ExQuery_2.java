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

package org.seaborne.examples;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;

/**
 * Example
 */
public class ExQuery_2 {
    public static void main(String ... args) {
        Dataset ds = dataset();
        Query exampleQuery = QueryFactory.create("SELECT * {?s ?p ?o}");

        // To process a SELECT query:
        QueryExecution.create().dataset(ds).query(exampleQuery)
                .select(querySolution->{
                    // Action on a query solution.
                    Resource subject = querySolution.getResource("s");
                    Resource property = querySolution.getResource("p");
                    // Resource (URI, blank node) or literal.
                    RDFNode object = querySolution.get("o");

                    System.out.printf("s = %s\n", subject);
                    System.out.printf("p = %s\n", property);
                    System.out.printf("o = %s\n", object);
                });
    }

    private static Dataset dataset() {
        Dataset dataset = DatasetFactory.createTxnMem();
        String data = "PREFIX : <http://example/> :s :p 123 .";
        // Parse string into the dataset
        RDFParser.fromString(data).lang(Lang.TTL).parse(dataset);
        return dataset;
    }
}
