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

package org.apache.jena.queryexec;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.EngineLib;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;
import org.apache.jena.sparql.util.Context;

/**
 * Query execution for local datasets - builder style.
 */
public class QExecBuilder {
    // Had been migrated.
    // May have evolved.
    // Improvements to QueryExecutionBuilder

    /** Create a new builder of {@link QueryExecution} for a local dataset. */
    public static QExecBuilder newBuilder() {
        QExecBuilder builder = new QExecBuilder();
        return builder;
    }

    private static final long UNSET         = -1;

    private DatasetGraph dataset            = null;
    private Query        query              = null;
    private Context      context            = null;
    private Binding      initialBinding     = null;
    private long         timeout1           = UNSET;
    private TimeUnit     timeoutTimeUnit1   = null;
    private long         timeout2           = UNSET;
    private TimeUnit     timeoutTimeUnit2   = null;

    public QExecBuilder query(Query query) {
        this.query = query;
        return this;
    }

    public QExecBuilder query(String queryString) {
        query(queryString, Syntax.syntaxARQ);
        return this;
    }

    public QExecBuilder query(String queryString, Syntax syntax) {
        this.query = QueryFactory.create(queryString, syntax);
        return this;
    }

    public QExecBuilder dataset(DatasetGraph dsg) {
        this.dataset = dsg;
        return this;
    }

//    public QExecBuilder dataset(Dataset dataset) {
//        this.dataset = dataset.asDatasetGraph();
//        return this;
//    }

    public QExecBuilder context(Context context) {
        this.context = context;
        return this;
    }

    public QExecBuilder initialBinding(Binding binding) {
        this.initialBinding = binding;
        return this;
    }

    public QExecBuilder timeout(long value, TimeUnit timeUnit) {
        this.timeout1 = value;
        this.timeoutTimeUnit1 = timeUnit;
        this.timeout2 = value;
        this.timeoutTimeUnit2 = timeUnit;
        return this;
    }

    public QExecBuilder initialTimeout(long value, TimeUnit timeUnit) {
        this.timeout1 = value < 0 ? -1L : value ;
        this.timeoutTimeUnit1 = timeUnit;
        return this;
    }

    public QExecBuilder overallTimeout(long value, TimeUnit timeUnit) {
        this.timeout2 = value;
        this.timeoutTimeUnit2 = timeUnit;
        return this;
    }

    // Set times from context if not set directly.
    private static void defaultTimeoutsFromContext(QExecBuilder builder, Context cxt) {
        if ( cxt.isDefined(ARQ.queryTimeout) ) {
            Object obj = cxt.get(ARQ.queryTimeout);
            try {
                if ( obj instanceof Number ) {
                    long x = ((Number)obj).longValue();
                    if ( builder.timeout2 < 0 )
                        builder.overallTimeout(x, TimeUnit.MILLISECONDS);
                } else if ( obj instanceof String ) {
                    String str = obj.toString();
                    Pair<Long, Long> pair = EngineLib.parseTimoutStr(str, TimeUnit.MILLISECONDS);
                    if ( builder.timeout1 < 0 )
                        builder.initialTimeout(pair.getLeft(), TimeUnit.MILLISECONDS);
                    if ( builder.timeout2 < 0 )
                        builder.overallTimeout(pair.getRight(), TimeUnit.MILLISECONDS);
                } else
                    Log.warn(builder, "Can't interpret timeout: " + obj);
            } catch (Exception ex) {
                Log.warn(builder, "Exception setting timeouts (context) from: "+obj);
            }
        }
    }

    public QExec build() {
        Objects.requireNonNull(query, "Query for QueryExecution");

        query.setResultVars();
        Context cxt;

        if ( context == null ) {
            // Default is to take the global context, the copy it and merge in the dataset context.
            // If a context is specified by context(Context), use that as given.
            // The query context is modified to insert the current time.
            cxt = ARQ.getContext();
            cxt = Context.setupContextForDataset(cxt, dataset) ;
        } else {
            // Isolate to snapshot it and to allow it to be  modified.
            cxt = context.copy();
        }
        QueryEngineFactory f = QueryEngineRegistry.get().find(query, dataset, cxt);
        if ( f == null ) {
            Log.warn(QExecBuilder.class, "Failed to find a QueryEngineFactory");
            return null;
        }

        // Initial bindings / parameterized query
        Query queryActual = query;
        if ( initialBinding != null ) {
            Map<Var, Node> substitutions = bindingToMap(initialBinding);
            queryActual = QueryTransformOps.transform(query, substitutions);
        }

        defaultTimeoutsFromContext(this, cxt);
        QExec qExec = new QExecBase(queryActual, dataset, cxt, f, timeout1, timeoutTimeUnit1, timeout2, timeoutTimeUnit2);
        return qExec;
    }

    // ==> BindingUtils
    /** Binding as a Map */
    public static Map<Var, Node> bindingToMap(Binding binding) {
        Map<Var, Node> substitutions = new HashMap<>();
        Iterator<Var> iter = binding.vars();
        while(iter.hasNext()) {
            Var v = iter.next();
            Node n = binding.get(v);
            substitutions.put(v, n);
        }
        return substitutions;
    }

    // (Slightly shorter) abbreviated forms - build-execute now.

    public RowSet select() {
        return build().select();
    }

    public void select(Consumer<Binding> rowAction) {
        if ( !query.isSelectType() )
            throw new QueryExecException("Attempt to execute SELECT for a "+query.queryType()+" query");
        try ( QExec qExec = build() ) {
            forEachRow(qExec.select(), rowAction);
        }
    }

    // Also in RDFLink
    private static void forEachRow(RowSet rowSet, Consumer<Binding> rowAction) {
        rowSet.forEachRemaining(rowAction);
    }

    public Graph construct() {
        if ( !query.isConstructType() )
            throw new QueryExecException("Attempt to execute CONSTRUCT for a "+query.queryType()+" query");
        try ( QExec qExec = build() ) {
            return qExec.construct();
        }
    }

    public Graph describe() {
        if ( !query.isDescribeType() )
            throw new QueryExecException("Attempt to execute DESCRIBE for a "+query.queryType()+" query");
        try ( QExec qExec = build() ) {
            return qExec.describe();
        }
    }

    public boolean ask() {
        if ( !query.isAskType() )
            throw new QueryExecException("Attempt to execute ASK for a "+query.queryType()+" query");
        try ( QExec qExec = build() ) {
            return qExec.ask();
        }
    }
}
