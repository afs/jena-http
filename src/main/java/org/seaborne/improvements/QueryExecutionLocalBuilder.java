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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

/**
 * Query Execution for local datasets - builder style.
 */
public class QueryExecutionLocalBuilder {
    // Improvements to QueryExecutionBuilder

    public static QueryExecutionLocalBuilder create() {
        QueryExecutionLocalBuilder builder = new QueryExecutionLocalBuilder();
        return builder;
    }

    private DatasetGraph dataset            = null;
    private Query        query              = null;
    private Context      context            = null;
    private Binding      initialBinding     = null;
    private long         timeout1           = -1;
    private TimeUnit     timeoutTimeUnit1   = null;
    private long         timeout2           = -1;
    private TimeUnit     timeoutTimeUnit2   = null;

    public QueryExecutionLocalBuilder query(Query query) {
        this.query = query;
        return this;
    }

    public QueryExecutionLocalBuilder query(String query) {
        this.query = QueryFactory.create(query, Syntax.syntaxARQ);
        return this;
    }

    public QueryExecutionLocalBuilder dataset(DatasetGraph dsg) {
        this.dataset = dsg;
        return this;
    }

    public QueryExecutionLocalBuilder context(Context context) {
        this.context = context;
        return this;
    }

    public QueryExecutionLocalBuilder initialBinding(Binding binding) {
        this.initialBinding = binding;
        return this;
    }

    public QueryExecutionLocalBuilder timeout(long value, TimeUnit timeUnit) {
        this.timeout1 = value;
        this.timeoutTimeUnit1 = timeUnit;
        this.timeout2 = value;
        this.timeoutTimeUnit2 = timeUnit;
        return this;
    }

    public QueryExecutionLocalBuilder initialTimeout(long value, TimeUnit timeUnit) {
        this.timeout1 = value;
        this.timeoutTimeUnit1 = timeUnit;
        return this;
    }

    public QueryExecutionLocalBuilder overallTimeout(long value, TimeUnit timeUnit) {
        this.timeout2 = value;
        this.timeoutTimeUnit2 = timeUnit;
        return this;
    }

    public QueryExecution build() {
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
            Log.warn(QueryExecutionLocalBuilder.class, "Failed to find a QueryEngineFactory");
            return null;
        }

        // QueryExecutionBase set up the final context, merging in the dataset context and setting the current time.
        QueryExecution qExec = new QueryExecutionBase(query, dataset, cxt, f);
        if ( initialBinding != null )
            qExec.setInitialBinding(initialBinding);
        if ( timeoutTimeUnit1 != null && timeout1 > 0 ) {
            if ( timeoutTimeUnit2 != null  && timeout2 > 0 )
                qExec.setTimeout(timeout1, timeoutTimeUnit1, timeout2, timeoutTimeUnit2);
            else
                // XXX CHECK
                qExec.setTimeout(timeout1, timeoutTimeUnit1);
        }
        return qExec;
    }


}

