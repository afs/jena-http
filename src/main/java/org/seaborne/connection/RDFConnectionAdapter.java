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

package org.seaborne.connection;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.update.UpdateRequest;

/** Provide {@link RDFConnection} using a {@link RDFLink} */

public class RDFConnectionAdapter implements RDFConnection {

    RDFConnectionAdapter(RDFLink conn) {
        this.other = conn;
    }

    private final RDFLink other ;
    protected RDFLink get() { return other; }

    private Model graph2model(Graph graph) {
        return ModelFactory.createModelForGraph(graph);
    }

    private Graph model2graph(Model model) {
        return model.getGraph();
    }

    private Dataset asDataset(DatasetGraph dsg) {
        return DatasetFactory.wrap(dsg);
    }

    private DatasetGraph asDatasetGraph(Dataset dataset) {
        return dataset.asDatasetGraph();
    }

    @Override
    public Model fetch() {
        return graph2model(get().fetch());
    }

    @Override
    public Model fetch(String graphName) {
        return  graph2model(get().fetch(graphName));
    }

    @Override
    public Dataset fetchDataset() {
        return asDataset(get().fetchDataset());
    }

    @Override
    public QueryExecution query(Query query) {
        return get().query(query);
    }

    @Override
    public void update(UpdateRequest update) {
        get().update(update);
    }

    @Override
    public void load(String graphName, String file) {
        get().load(graphName, file);
    }

    @Override
    public void load(String file) {
        get().load(file);
    }

    @Override
    public void load(String graphName, Model model) {
        get().load(graphName, model2graph(model));
    }

    @Override
    public void load(Model model) {
        get().load(model2graph(model));
    }

    @Override
    public void put(String graphName, String file) {
        get().put(graphName, file);
    }

    @Override
    public void put(String file) {
        get().put(file);
    }

    @Override
    public void put(String graphName, Model model) {
        get().put(graphName, model2graph(model));
    }

    @Override
    public void put(Model model) {
        get().put(model2graph(model));
    }

    @Override
    public void delete(String graphName) {
        get().delete(graphName);
    }

    @Override
    public void delete() {
        get().delete();
    }

    @Override
    public void loadDataset(String file) {
        get().loadDataset(file);
    }

    @Override
    public void loadDataset(Dataset dataset) {
        get().loadDataset(asDatasetGraph(dataset));
    }

    @Override
    public void putDataset(String file) {
        get().putDataset(file);
    }

    @Override
    public void putDataset(Dataset dataset) {
        get().putDataset(asDatasetGraph(dataset));
    }

    @Override
    public boolean isClosed() {
        return get().isClosed();
    }

    @Override
    public void close() {
        get().close();
    }

    @Override
    public void begin(TxnType type) {
        get().begin(type);
    }

    @Override
    public void begin(ReadWrite readWrite) {
        get().begin(readWrite);
    }

    @Override
    public boolean promote(Promote mode) {
        return get().promote(mode);
    }

    @Override
    public void commit() {
        get().commit();
    }

    @Override
    public void abort() {
        get().abort();
    }

    @Override
    public void end() {
        get().end();
    }

    @Override
    public ReadWrite transactionMode() {
        return get().transactionMode();
    }

    @Override
    public TxnType transactionType() {
        return get().transactionType();
    }

    @Override
    public boolean isInTransaction() {
        return get().isInTransaction();
    }
}
