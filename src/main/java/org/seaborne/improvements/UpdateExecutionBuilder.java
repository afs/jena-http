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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.update.*;

/**
 * Build a {@link UpdateProcessor}.
 */
public class UpdateExecutionBuilder {

    // [QExec] retired.

    static { JenaSystem.init(); }

    private UpdateRequest updateRequest = new UpdateRequest();
    private DatasetGraph dataset;
    private Binding initialBinding;
    private Context context = null;

    public static UpdateExecutionBuilder newBuilder() {
        return new UpdateExecutionBuilder();
    }

    public UpdateExecutionBuilder() {}

    public UpdateExecutionBuilder add(UpdateRequest update) {
        Objects.requireNonNull(update);
        return  add$(update);
    }

    private UpdateExecutionBuilder add$(UpdateRequest update) {
        Objects.requireNonNull(update);
        update.forEach(updateRequest::add);
        return this;
    }

    public UpdateExecutionBuilder add(Update update) {
        Objects.requireNonNull(update);
        this.updateRequest.add(update);
        return this;
    }

    /** Add an update - one or more SPARQL Update operations - provided in string form. */
    public UpdateExecutionBuilder add(String updateStr) {
        Objects.requireNonNull(updateStr);
        UpdateRequest req = UpdateFactory.create(updateStr, Syntax.syntaxARQ);
        add$(req);
        return this;
    }

    public UpdateExecutionBuilder dataset(DatasetGraph dataset) {
        Objects.requireNonNull(dataset);
        this.dataset = dataset;
        return this;
    }

    public UpdateExecutionBuilder dataset(Dataset dataset) {
        Objects.requireNonNull(dataset);
        dataset(dataset.asDatasetGraph());
        return this;
    }

    public UpdateExecutionBuilder context(Context context) {
        if ( context == null )
            return this;
        ensureContext();
        this.context.putAll(context);
        return this;
    }

    private void ensureContext() {
        if ( context == null )
            context = new Context();
    }

    public UpdateProcessor build() {
        Objects.requireNonNull(dataset, "No DatasetGraph for update execution");
        return UpdateExecutionFactory.create(updateRequest, dataset, initialBinding, context);
    }

    public void execute() {
        build().execute();
    }

    // Abbreviated forms.

    public void execute(Dataset dataset) {
        Objects.requireNonNull(dataset);
        dataset(dataset).build().execute();
    }

    public void execute(DatasetGraph dataset) {
        Objects.requireNonNull(dataset);
        dataset(dataset).build().execute();
    }

}
