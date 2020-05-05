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

package org.seaborne.link;

import static org.seaborne.link.LibRDFLink.asDatasetGraph;
import static org.seaborne.link.LibRDFLink.graph2model;
import static org.seaborne.link.LibRDFLink.model2graph;
import static org.seaborne.link.LibRDFLink.name;

import java.util.function.Consumer;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateRequest;

/** Provide {@link RDFConnection} using a {@link RDFLink} */

public class RDFConnectionAdapter implements RDFConnection {

    public static RDFConnection wrap(RDFLink link) {
        return new RDFConnectionAdapter(link);
    }

    private final RDFLink other ;
    // Class/subclass access. Ideally, call only once per method (for swappable links).
    protected RDFLink get() { return other; }

    /** Return the {@link RDFLink} for this connection. */
    public RDFLink getLink() { return get(); }

    private RDFConnectionAdapter(RDFLink conn) {
        this.other = conn;
    }

    @Override
    public void queryResultSet(String queryString, Consumer<ResultSet> resultSetAction) {
        get().queryResultSet(queryString, resultSetAction);
    }

    @Override
    public void queryResultSet(Query query, Consumer<ResultSet> resultSetAction) {
        get().queryResultSet(query, resultSetAction);
    }

    // Rely on the default methods for these rather than RDFLink.querySelect because
    // of the action on QuerySolution being tied to the dataset model sometimes.

//    public void querySelect(String queryString, Consumer<QuerySolution> rowAction) {
//    }
//    public void querySelect(Query query, Consumer<QuerySolution> rowAction) {
//        get().querySelect(queryString, rowAction);
//    }

    @Override
    public Model queryConstruct(String queryString) {
        return graph2model(get().queryConstruct(queryString));
    }

    @Override
    public Model queryConstruct(Query query) {
        return graph2model(get().queryConstruct(query));
    }

    @Override
    public Model queryDescribe(String queryString) {
        return graph2model(get().queryDescribe(queryString));
    }

    @Override
    public Model queryDescribe(Query query) {
        return graph2model(get().queryDescribe(query));
    }

    @Override
    public boolean queryAsk(String queryString) {
        return get().queryAsk(queryString);
    }

    @Override
    public boolean queryAsk(Query query) {
        return get().queryAsk(query);
    }

    @Override
    public QueryExecution query(Query query) {
        return get().query(query);
    }

    @Override
    public QueryExecution query(String queryString) {
        return get().query(queryString);
    }

    @Override
    public void update(UpdateRequest update) {
        get().update(update);
    }

    @Override
    public void update(String update) {
        get().update(update);
    }

    @Override
    public Model fetch() {
        return graph2model(get().fetch());
    }

    @Override
    public Model fetch(String graphName) {
        return  graph2model(get().fetch(name(graphName)));
    }

    @Override
    public void load(String graphName, String file) {
        get().load(name(graphName), file);
    }

    @Override
    public void load(String file) {
        get().load(file);
    }

    @Override
    public void load(String graphName, Model model) {
        get().load(name(graphName), model2graph(model));
    }

    @Override
    public void load(Model model) {
        get().load(model2graph(model));
    }

    @Override
    public void put(String graphName, String file) {
        get().put(name(graphName), file);
    }

    @Override
    public void put(String file) {
        get().put(file);
    }

    @Override
    public void put(String graphName, Model model) {
        get().put(name(graphName), model2graph(model));
    }

    @Override
    public void put(Model model) {
        get().put(model2graph(model));
    }

    @Override
    public void delete(String graphName) {
        get().delete(name(graphName));
    }

    @Override
    public void delete() {
        get().delete();
    }

    @Override
    public Dataset fetchDataset() {
        return LibRDFLink.asDataset(get().fetchDataset());
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
