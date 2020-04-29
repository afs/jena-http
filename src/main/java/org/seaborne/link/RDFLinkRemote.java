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

import static org.seaborne.link.LibRDFLink.name;

import java.net.http.HttpClient;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdfconnection.JenaConnectionException;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalLock;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.system.Txn;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.seaborne.http.GSP;
import org.seaborne.http.HttpEnv;
import org.seaborne.http.QueryExecutionHTTP;
import org.seaborne.http.UpdateExecutionHTTP;

/**
 * Implementation of the {@link RDFLink} interface using remote SPARQL operations.
 */
public class RDFLinkRemote implements RDFLink {
    // Adds a Builder to help with HTTP details.

    private static final String fusekiDftSrvQuery   = "sparql";
    private static final String fusekiDftSrvUpdate  = "update";
    private static final String fusekiDftSrvGSP     = "data";

    private boolean isOpen = true;
    protected final String destination;
    protected final String svcQuery;
    protected final String svcUpdate;
    protected final String svcGraphStore;

    protected final Transactional txnLifecycle;
    protected final HttpClient httpClient;

    // On-the-wire settings.
    protected final RDFFormat outputQuads;
    protected final RDFFormat outputTriples;
    protected final String acceptGraph;
    protected final String acceptDataset;
    protected final String acceptSelectResult;
    protected final String acceptAskResult;
    // All purpose SPARQL results header used if above specific cases do not apply.
    protected final String acceptSparqlResults;

    // Whether to check SPARQL queries given as strings by parsing them.
    protected final boolean parseCheckQueries;
    // Whether to check SPARQL updates given as strings by parsing them.
    protected final boolean parseCheckUpdates;

    /** Create a {@link RDFLinkRemoteBuilder}. */
    public static RDFLinkRemoteBuilder create() {
        return new RDFLinkRemoteBuilder();
    }

    /**
     * Create a {@link RDFLinkRemoteBuilder} initialized with the
     * settings of another {@code RDFLinkRemote}.
     */
    public static RDFLinkRemoteBuilder create(RDFLinkRemote base) {
        return new RDFLinkRemoteBuilder(base);
    }

    // Used by the builder.
    protected RDFLinkRemote(Transactional txnLifecycle, HttpClient httpClient, String destination,
                                   String queryURL, String updateURL, String gspURL, RDFFormat outputQuads, RDFFormat outputTriples,
                                   String acceptDataset, String acceptGraph,
                                   String acceptSparqlResults,
                                   String acceptSelectResult, String acceptAskResult,
                                   boolean parseCheckQueries, boolean parseCheckUpdates) {
        // Any defaults.
        HttpClient hc =  httpClient!=null ? httpClient : HttpEnv.getDftHttpClient();

        this.httpClient = hc;
        this.destination = destination;
        this.svcQuery = queryURL;
        this.svcUpdate = updateURL;
        this.svcGraphStore = gspURL;
        if ( txnLifecycle == null )
            txnLifecycle  = TransactionalLock.createMRPlusSW();
        this.txnLifecycle = txnLifecycle;
        this.outputQuads = outputQuads;
        this.outputTriples = outputTriples;
        this.acceptDataset = acceptDataset;
        this.acceptGraph = acceptGraph;
        this.acceptSparqlResults = acceptSparqlResults;
        this.acceptSelectResult = acceptSelectResult;
        this.acceptAskResult = acceptAskResult;
        this.parseCheckQueries = parseCheckQueries;
        this.parseCheckUpdates = parseCheckUpdates;
    }

    /** Return the {@link HttpClient} in-use. */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /** Return the destination URL for the connection. */
    public String getDestination() {
        return destination;
    }

    // For custom content negotiation.

    // This class overrides each of these to pass down the query type as well.
    // Then we can derive the accept header if customized without needing to parse
    // the query. This allows an arbitrary string for a query and allows the remote
    // server to have custom syntax extensions or interpretations of comments.

    /**
     * Execute a SELECT query and process the ResultSet with the handler code.
     * @param queryString
     * @param resultSetAction
     */
    @Override
    public void queryResultSet(String queryString, Consumer<ResultSet> resultSetAction) {
        Txn.executeRead(this, ()->{
            try ( QueryExecution qExec = query(queryString, QueryType.SELECT) ) {
                ResultSet rs = qExec.execSelect();
                resultSetAction.accept(rs);
            }
        } );
    }

    /**
     * Execute a SELECT query and process the rows of the results with the handler code.
     * @param queryString
     * @param rowAction
     */
    @Override
    public void querySelect(String queryString, Consumer<Binding> rowAction) {
        Txn.executeRead(this, ()->{
            try ( QueryExecution qExec = query(queryString, QueryType.SELECT) ) {
                ResultSet rs = qExec.execSelect();
                while(rs.hasNext() ) {
                    rowAction.accept(rs.nextBinding());
                }
            }
        } );
    }

    /** Execute a CONSTRUCT query and return as a Graph */
    @Override
    public Graph queryConstruct(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QueryExecution qExec = query(queryString, QueryType.CONSTRUCT) ) {
                    return qExec.execConstruct().getGraph();
                }
            } );
    }

    /** Execute a DESCRIBE query and return as a Graph */
    @Override
    public Graph queryDescribe(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QueryExecution qExec = query(queryString, QueryType.DESCRIBE) ) {
                    return qExec.execDescribe().getGraph();
                }
            } );
    }

    /** Execute a ASK query and return a boolean */
    @Override
    public boolean queryAsk(String queryString) {
        return
            Txn.calculateRead(this, ()->{
                try ( QueryExecution qExec = query(queryString, QueryType.ASK) ) {
                    return qExec.execAsk();
                }
            } );
    }

    /**
     * Operation that passed down the query type so the accept header can be set without parsing the query string.
     * @param queryString
     * @param queryType
     * @return QueryExecution
     */
    protected QueryExecution query(String queryString, QueryType queryType) {
        Objects.requireNonNull(queryString);
        return queryExec(null, queryString, queryType);
    }

    @Override
    public QueryExecution query(String queryString) {
        Objects.requireNonNull(queryString);
        return queryExec(null, queryString, null);
    }

    @Override
    public QueryExecution query(Query query) {
        Objects.requireNonNull(query);
        return queryExec(query, null, null);
    }

    private QueryExecution queryExec(Query query, String queryString, QueryType queryType) {
        checkQuery();
        if ( query == null && queryString == null )
            throw new InternalErrorException("Both query and query string are null");
        if ( query == null ) {
            if ( parseCheckQueries )
                QueryFactory.create(queryString);
        }

        // Use the query string as provided if possible, otherwise serialize the query.
        String queryStringToSend = ( queryString != null ) ? queryString : query.toString();
        return createQueryExecution(query, queryStringToSend, queryType);
    }

    // Create the QueryExecution
    private QueryExecution createQueryExecution(Query query, String queryStringToSend, QueryType queryType) {
        QueryExecutionHTTP.Builder builder = QueryExecutionHTTP.newBuilder()
            .service(svcQuery)
            .httpClient(httpClient)
            .queryString(queryStringToSend);

        QueryType qt = queryType;
        if ( query != null && qt == null )
            qt = query.queryType();
        if ( qt == null )
            qt = QueryType.UNKNOWN;
        // Set the accept header - use the most specific method.
        String requestAcceptHeader = null;
        switch(qt) {
            case SELECT :
                if ( acceptSelectResult != null )
                    requestAcceptHeader = acceptSelectResult;
                break;
            case ASK :
                if ( acceptAskResult != null )
                    requestAcceptHeader = acceptAskResult;
                break;
            case DESCRIBE :
            case CONSTRUCT :
                if ( acceptGraph != null )
                    requestAcceptHeader = acceptGraph;
                break;
            case UNKNOWN:
                // All-purpose content type.
                if ( acceptSparqlResults != null )
                    requestAcceptHeader = acceptSparqlResults;
                else
                    // No idea! Set an "anything" and hope.
                    // (Reasonable chance this is going to end up as HTML though.)
                    requestAcceptHeader = "*/*";
            default :
                break;
        }

        // Make sure it was set somehow.
        if ( requestAcceptHeader == null )
            throw new JenaConnectionException("No Accept header");
        return builder.acceptHeader(requestAcceptHeader).build();
    }

    private void acc(StringBuilder sBuff, String acceptString) {
        if ( acceptString == null )
            return;
        if ( sBuff.length() != 0 )
            sBuff.append(", ");
        sBuff.append(acceptString);
    }

    @Override
    public void update(String updateString) {
        Objects.requireNonNull(updateString);
        updateExec(null, updateString);
    }

    @Override
    public void update(UpdateRequest update) {
        Objects.requireNonNull(update);
        updateExec(update, null);
    }

    private void updateExec(UpdateRequest update, String updateString ) {
        checkUpdate();
        if ( update == null && updateString == null )
            throw new InternalErrorException("Both update request and update string are null");
        if ( update == null ) {
            if ( parseCheckUpdates )
                UpdateFactory.create(updateString);
        }
        // Use the update string as provided if possible, otherwise serialize the update.
        String updateStringToSend = ( updateString != null ) ? updateString  : update.toString();
        UpdateExecutionHTTP.newBuilder()
            .service(svcUpdate)
            .httpClient(httpClient)
            .updateString(updateStringToSend)
            .build()
            .execute();
    }

//    /** Convert HTTP status codes to exceptions */
//    static protected void exec(Runnable action)  {
//        try { action.run(); }
//        catch (HttpException ex) { handleHttpException(ex, false); }
//    }
//
//    /** Convert HTTP status codes to exceptions */
//    static protected <X> X exec(Supplier<X> action)  {
//        try { return action.get(); }
//        catch (HttpException ex) { handleHttpException(ex, true); return null;}
//    }
//
//    private static void handleHttpException(HttpException ex, boolean ignore404) {
//        if ( ex.getStatusCode() == HttpSC.NOT_FOUND_404 && ignore404 )
//            return ;
//        throw ex;
//    }

    /** {@inheritDoc} */
    @Override
    public Graph fetch(Node graphName) {
        checkGSP();
        return GSP.request(svcGraphStore).graphName(graphName.getURI()).GET(acceptGraph);
    }

    @Override
    public Graph fetch() {
        checkGSP();
        return GSP.request(svcGraphStore).defaultGraph().GET(acceptGraph);
    }

    // "load" => POST
    @Override
    public void load(Node graphName, String file) {
        checkGSP();
        gsp(graphName).POST(file, ct(outputTriples));
    }

    @Override
    public void load(String file) {
        checkGSP();
        gsp().POST(file, ct(outputTriples));
    }

    @Override
    public void load(Graph graph) {
        gsp().POST(graph, outputTriples);
    }

    @Override
    public void load(Node graphName, Graph graph) {
        gsp(graphName).POST(graph, outputTriples);
    }

    @Override
    public void put(Node graphName, String file) {
        checkGSP();
        gsp(graphName).PUT(file, ct(outputTriples));
    }

    @Override
    public void put(String file) {
        checkGSP();
        gsp().PUT(file, ct(outputTriples));
    }

    @Override
    public void put(Node graphName, Graph graph) {
        checkGSP();
        gsp(graphName).PUT(graph, outputTriples);
    }

    @Override
    public void put(Graph graph) {
        checkGSP();
        gsp().PUT(graph, outputTriples);
    }

    // ---- GSP requests
    private String ct(RDFFormat format) { return format.getLang().getHeaderString(); }

    private GSP gsp() {
        return gspRequest().defaultGraph();
    }

    private GSP gsp(Node graphName) {
        if ( LibRDFLink.isDefault(graphName) )
            return gspRequest().defaultGraph();
        else
            return gspRequest().graphName(name(graphName));
    }

    private GSP gspRequest() {
        return GSP.request(svcGraphStore);
    }

    @Override
    public void delete(Node graphName) {
        checkGSP();
        gsp(graphName).DELETE();
    }

    @Override
    public void delete() {
        checkGSP();
        gsp().DELETE();
    }

    @Override
    public DatasetGraph fetchDataset() {
        checkDataset();
        return gspRequest().getDataset();
    }

    @Override
    public void loadDataset(String file) {
        checkDataset();
        gspRequest().postDataset(file);
    }

    @Override
    public void loadDataset(DatasetGraph dataset) {
        checkDataset();
        gspRequest().postDataset(dataset);
    }

    @Override
    public void putDataset(String file) {
        checkDataset();
        gspRequest().putDataset(file);
    }

    @Override
    public void putDataset(DatasetGraph dataset) {
        checkDataset();
        gspRequest().putDataset(dataset);
    }

    // -- Internal.

    protected void checkQuery() {
        checkOpen();
        if ( svcQuery == null )
            throw new ARQException("No query service defined for this RDFLink");
    }

    protected void checkUpdate() {
        checkOpen();
        if ( svcUpdate == null )
            throw new ARQException("No update service defined for this RDFLink");
    }

    protected void checkGSP() {
        checkOpen();
        if ( svcGraphStore == null )
            throw new ARQException("No SPARQL Graph Store service defined for this RDFLink");
    }

    protected void checkDataset() {
        checkOpen();
        if ( destination == null )
            throw new ARQException("Dataset operations not available - no dataset URL provided");
    }

    protected void checkOpen() {
        if ( ! isOpen )
            throw new ARQException("closed");
    }

    @Override
    public void close() {
        isOpen = false;
    }

    @Override
    public boolean isClosed() {
        return ! isOpen;
    }

    @Override public void begin()                       { txnLifecycle.begin(); }
    @Override public void begin(TxnType txnType)        { txnLifecycle.begin(txnType); }
    @Override public void begin(ReadWrite mode)         { txnLifecycle.begin(mode); }
    @Override public boolean promote(Promote promote)   { return txnLifecycle.promote(promote); }
    @Override public void commit()                      { txnLifecycle.commit(); }
    @Override public void abort()                       { txnLifecycle.abort(); }
    @Override public boolean isInTransaction()          { return txnLifecycle.isInTransaction(); }
    @Override public void end()                         { txnLifecycle.end(); }
    @Override public ReadWrite transactionMode()        { return txnLifecycle.transactionMode(); }
    @Override public TxnType transactionType()          { return txnLifecycle.transactionType(); }
}