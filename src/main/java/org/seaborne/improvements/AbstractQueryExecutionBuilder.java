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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

// Commonality between QueryExecutionHTTP and QueryExecutionLocal
// Is it worth it?

public abstract class AbstractQueryExecutionBuilder<B extends AbstractQueryExecutionBuilder<B>> {

    protected Query        query              = null;
    protected Context      context            = null;
    protected Binding      initialBinding     = null;

    public AbstractQueryExecutionBuilder<B> query(Query query) {
        this.query = query;
        return this;
    }

    public AbstractQueryExecutionBuilder<B> query(String queryString) {
        query(queryString, Syntax.syntaxARQ);
        return this;
    }

    public AbstractQueryExecutionBuilder<B> query(String queryString, Syntax syntax) {
        this.query = QueryFactory.create(queryString, syntax);
        return this;
    }

    public AbstractQueryExecutionBuilder<B> context(Context context) {
        this.context = context;
        return this;
    }

    public AbstractQueryExecutionBuilder<B> initialBinding(Binding binding) {
        this.initialBinding = binding;
        return this;
    }

}

