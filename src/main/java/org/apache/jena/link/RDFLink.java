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

package org.apache.jena.link;

import java.util.function.Consumer;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.queryexec.QExec;
import org.apache.jena.queryexec.RowSet;
import org.apache.jena.rdfconnection.JenaConnectionException;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionLocal;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.system.Txn;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

/**
 * Interface for SPARQL operations on a datasets, whether local or remote.
 * Operations can performed via this interface or via the various
 * interfaces for a subset of the operations.
 *
 * <ul>
 * <li>query ({@link LinkSparqlQuery})
 * <li>update ({@link LinkSparqlUpdate})
 * <li>graph store protocol ({@link LinkDatasetGraph} and read-only {@link LinkDatasetGraphAccess}).
 * </ul>
 *
 * For remote operations, the
 * <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol</a> is used
 * for query and updates and
 * <a href="http://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL Graph Store
 * Protocol</a> for the graph operations and in addition, there are analogous
 * operations on datasets (fetch, load, put; but not delete).
 *
 * {@code RDFConnection} provides transaction boundaries. If not in a
 * transaction, an implicit transactional wrapper is applied ("autocommit").
 *
 * Remote SPARQL operations are atomic but without additional capabilities from
 * the remote server, multiple operations are not combined into a single
 * transaction.
 *
 * Not all implementations may implement all operations.
 * See the implementation notes for details.
 *
 * @see RDFConnectionFactory
 * @see RDFConnectionLocal
 * @see RDFConnectionRemote
 * @see LinkSparqlQuery
 * @see LinkSparqlUpdate
 * @see LinkDatasetGraph
 */

public interface RDFLink extends
        LinkSparqlQuery, LinkSparqlUpdate, LinkDatasetGraph,
        Transactional, AutoCloseable {
    // Default implementations could be pushed up but then they can't be mentioned here
    // and the javadoc for RDFConnection is not in one place.
    // Inheriting interfaces and re-mentioning gets the javadoc in one place.

    // ---- SparqlQueryConnection
    // Where the argument is a query string, this code avoids simply parsing it and calling
    // the Query object form. This allows RDFConnectionRemote to pass the query string
    // untouched to the connection depending in the internal setting to parse/check
    // queries.
    // Java9 introduces private methods for interfaces which could clear the duplication up by passing in a Creator<QueryExecution>.
    // (Alternatively, add RDFConnectionBase with protected query(String, Query)
    // See RDFConnectionRemote.

    /**
     * Execute a SELECT query and process the RowSet with the handler code.
     * @param queryString
     * @param rowSetAction
     */
    @Override
    public default void queryRowSet(String queryString, Consumer<RowSet> rowSetAction) {
        Txn.executeRead(this, ()->{
            try ( QExec qExec = query(queryString) ) {
                RowSet rs = qExec.select();
                rowSetAction.accept(rs);
            }
        } );
    }

    /**
     * Execute a SELECT query and process the RowSet with the handler code.
     * @param query
     * @param rowSetAction
     */
    @Override
    public default void queryRowSet(Query query, Consumer<RowSet> rowSetAction) {
        if ( ! query.isSelectType() )
            throw new JenaConnectionException("Query is not a SELECT query");
        Txn.executeRead(this, ()->{
            try ( QExec qExec = query(query) ) {
                RowSet rs = qExec.select();
                rowSetAction.accept(rs);
            }
        } );
    }

    private static void forEachRow(RowSet rowSet, Consumer<Binding> rowAction) {
        rowSet.forEachRemaining(rowAction);
    }

    /**
     * Execute a SELECT query and process the rows of the results with the handler code.
     * @param queryString
     * @param rowAction
     */
    @Override
    public default void querySelect(String queryString, Consumer<Binding> rowAction) {
        Txn.executeRead(this, ()->{
            try ( QExec qExec = query(queryString) ) {
                forEachRow(qExec.select(), rowAction);
            }
        } );
    }

    /**
     * Execute a SELECT query and process the rows of the results with the handler code.
     * @param query
     * @param rowAction
     */
    @Override
    public default void querySelect(Query query, Consumer<Binding> rowAction) {
        if ( ! query.isSelectType() )
            throw new JenaConnectionException("Query is not a SELECT query");
        Txn.executeRead(this, ()->{
            try ( QExec qExec = query(query) ) {
                forEachRow(qExec.select(), rowAction);
            }
        } );
    }

    /** Execute a CONSTRUCT query and return as a Graph */
    @Override
    public default Graph queryConstruct(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(queryString) ) {
                    return qExec.construct();
                }
            } );
    }

    /** Execute a CONSTRUCT query and return as a DatasetGraph */
    //@Override
    public default DatasetGraph queryConstructDataset(Query query) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(query) ) {
                    return qExec.constructDataset();
                }
            } );
    }

    /** Execute a CONSTRUCT query and return as a Graph */
    //@Override
    public default DatasetGraph queryConstructDataset(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(queryString) ) {
                    return qExec.constructDataset();
                }
            } );
    }

    /** Execute a CONSTRUCT query and return as a Graph */
    @Override
    public default Graph queryConstruct(Query query) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(query) ) {
                    return qExec.construct();
                }
            } );
    }



    /** Execute a DESCRIBE query and return as a Graph */
    @Override
    public default Graph queryDescribe(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(queryString) ) {
                    return qExec.describe();
                }
            } );
    }

    /** Execute a DESCRIBE query and return as a Graph */
    @Override
    public default Graph queryDescribe(Query query) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(query) ) {
                    return qExec.describe();
                }
            } );
    }

    /** Execute a ASK query and return a boolean */
    @Override
    public default boolean queryAsk(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(queryString) ) {
                    return qExec.ask();
                }
            } );
    }

    /** Execute a ASK query and return a boolean */
    @Override
    public default boolean queryAsk(Query query) {
        return
            Txn.calculateRead(this, ()->{
                try ( QExec qExec = query(query) ) {
                    return qExec.ask();
                }
            } );
    }

    /** Setup a SPARQL query execution.
     *
     *  See also {@link #querySelect(Query, Consumer)}, {@link #queryConstruct(Query)},
     *  {@link #queryDescribe(Query)}, {@link #queryAsk(Query)}
     *  for ways to execute queries for of a specific form.
     *
     * @param query
     * @return QueryExecution
     */
    @Override
    public QExec query(Query query);

    /** Setup a SPARQL query execution.
     * This is a low-level operation.
     * Handling the {@link QueryExecution} should be done with try-resource.
     * Some {@link QueryExecution QueryExecutions}, such as ones connecting to a remote server,
     * need to be properly closed to release system resources.
     *
     *  See also {@link #querySelect(String, Consumer)}, {@link #queryConstruct(String)},
     *  {@link #queryDescribe(String)}, {@link #queryAsk(String)}
     *  for ways to execute queries of a specific form.
     *
     * @param queryString
     * @return QueryExecution
     */
    @Override
    public default QExec query(String queryString) {
        return query(QueryFactory.create(queryString));
    }

    // ---- SparqlUpdateConnection

    /** Execute a SPARQL Update.
     *
     * @param update
     */
    @Override
    public default void update(Update update) {
        update(new UpdateRequest(update));
    }

    /**
     * Execute a SPARQL Update.
     * @param update
     */
    @Override
    public void update(UpdateRequest update);

    /**
     * Execute a SPARQL Update.
     * @param updateString
     */
    @Override
    public default void update(String updateString) {
        update(UpdateFactory.create(updateString));
    }

    // ---- RDFDatasetConnection

    /** Load (add, append) RDF into the default graph of a dataset.
     * This is SPARQL Graph Store Protocol HTTP POST or equivalent.
     * <p>
     * If this is a remote connection:
     * <ul>
     * <li> The file is sent as-is and not parsed in the RDFLink
     * <li> The Content-Type is determined by the filename
     * </ul>
     *
     * @param file File of the data.
     */
    @Override
    public void load(String file);

    /** Load (add, append) RDF into a named graph in a dataset.
     * This is SPARQL Graph Store Protocol HTTP POST or equivalent.
     * <p>
     * If this is a remote connection:
     * <ul>
     * <li> The file is sent as-is and not parsed in the RDFLink
     * <li> The Content-Type is determined by the filename
     * </ul>
     *
     * @param graphName Graph name (null or {@link Quad#defaultGraphIRI} for the default graph)
     * @param file File of the data.
     */
    @Override
    public void load(Node graphName, String file);

    /** Load (add, append) RDF into the default graph of a dataset.
     * This is SPARQL Graph Store Protocol HTTP POST or equivalent.
     *
     * @param graph Data.
     */
    @Override
    public void load(Graph graph);

    /** Load (add, append) RDF into a named graph in a dataset.
     * This is SPARQL Graph Store Protocol HTTP POST or equivalent.
     *
     * @param graphName Graph name (null or {@link Quad#defaultGraphIRI} for the default graph)
     * @param graph Data.
     */
    @Override
    public void load(Node graphName, Graph graph);

    /** Set the contents of the default graph of a dataset.
     * Any existing data is lost.
     * This is SPARQL Graph Store Protocol HTTP PUT or equivalent.
     * <p>
     * If this is a remote connection:
     * <ul>
     * <li> The file is sent as-is and not parsed in the RDFLink
     * <li> The Content-Type is determined by the filename
     * </ul>
     *
     * @param file File of the data.
     */
    @Override
    public void put(String file);

    /** Set the contents of a named graph of a dataset.
     * Any existing data is lost.
     * This is SPARQL Graph Store Protocol HTTP PUT or equivalent.
     *
     * @param graphName Graph name (null or {@link Quad#defaultGraphIRI} for the default graph)
     * @param file File of the data.
     */
    @Override
    public void put(Node graphName, String file);

    /** Set the contents of the default graph of a dataset.
     * Any existing data is lost.
     * This is SPARQL Graph Store Protocol HTTP PUT or equivalent.
     * <p>
     * If this is a remote connection:
     * <ul>
     * <li> The file is sent as-is and not parsed in the RDFLink
     * <li> The Content-Type is determined by the filename
     * </ul>
     *
     * @param graph Data.
     */
    @Override
    public void put(Graph graph);

    /** Set the contents of a named graph of a dataset.
     * Any existing data is lost.
     * This is SPARQL Graph Store Protocol HTTP PUT or equivalent.
     *
     * @param graphName Graph name (null or {@link Quad#defaultGraphIRI} for the default graph)
     * @param graph Data.
     */
    @Override
    public void put(Node graphName, Graph graph);

    /**
     * Delete a graph from the dataset.
     * Null or {@link Quad#defaultGraphIRI} means the default graph, which is cleared, not removed.
     *
     * @param graphName
     */
    @Override
    public void delete(Node graphName);

    /**
     * Remove all data from the default graph.
     */
    @Override
    public void delete();

    /* Load (add, append) RDF triple or quad data into a dataset. Triples wil go into the default graph.
     * This is not a SPARQL Graph Store Protocol operation.
     * It is an HTTP POST equivalent to the dataset.
     */
    @Override
    public void loadDataset(String file);

    /* Load (add, append) RDF triple or quad data into a dataset. Triples wil go into the default graph.
     * This is not a SPARQL Graph Store Protocol operation.
     * It is an HTTP POST equivalent to the dataset.
     */
    @Override
    public void loadDataset(DatasetGraph dataset);

    /* Set RDF triple or quad data as the dataset contents.
     * Triples will go into the default graph, quads in named graphs.
     * This is not a SPARQL Graph Store Protocol operation.
     * It is an HTTP PUT equivalent to the dataset.
     */
    @Override
    public void putDataset(String file);

    /* Set RDF triple or quad data as the dataset contents.
     * Triples will go into the default graph, quads in named graphs.
     * This is not a SPARQL Graph Store Protocol operation.
     * It is an HTTP PUT equivalent to the dataset.
     */
    @Override
    public void putDataset(DatasetGraph dataset);

    /** Clear the dataset - remove all named graphs, clear the default graph. */
    public void clearDataset();

    /** Test whether this connection is closed or not */
    @Override
    public boolean isClosed();

    /** Whether this RDFLink is to a remote server or not. */
    public default boolean isRemote() { return false; }

    /** Close this connection.  Use with try-resource. */
    @Override
    public void close();
}
