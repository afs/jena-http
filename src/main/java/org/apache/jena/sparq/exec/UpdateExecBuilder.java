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

package org.apache.jena.sparq.exec;

import java.util.Objects;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.UpdateEngineFactory;
import org.apache.jena.sparql.modify.UpdateEngineRegistry;
import org.apache.jena.sparql.modify.UpdateProcessorBase;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class UpdateExecBuilder {

    public static UpdateExecBuilder newBuilder() { return new UpdateExecBuilder(); }

    private DatasetGraph dataset            = null;
    private Query        query              = null;
    private Context      context            = null;
    private Binding      initialBinding     = null;
    private UpdateRequest update            = null;
    private UpdateRequest updateRequest     = new UpdateRequest();

    private UpdateExecBuilder() {}

    /** Append the updates in an {@link UpdateRequest} to the {@link UpdateRequest} being built. */
    public UpdateExecBuilder update(UpdateRequest updateRequest) {
        Objects.requireNonNull(updateRequest);
        add(updateRequest);
        return this;
    }

    /** Add the {@link Update} to the {@link UpdateRequest} being built. */
    public UpdateExecBuilder update(Update update) {
        Objects.requireNonNull(update);
        add(update);
        return this;
    }

    /** Parse and update operations to the {@link UpdateRequest} being built. */
    public UpdateExecBuilder update(String updateRequestString) {
        UpdateRequest more = UpdateFactory.create(updateRequestString);
        add(more);
        return this;
    }

    public UpdateExecBuilder dataset(DatasetGraph dsg) {
        this.dataset = dsg;
        return this;
    }

    /** Set the {@link Context}.
     *  This defaults to the global settings of {@code ARQ.getContext()}.
     *  If there was a previous call of {@code context} the multiple contexts are merged.
     * */
    public UpdateExecBuilder context(Context context) {
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

    // [QExec] Becomes UpdateExec
    public UpdateProcessor build() {
        return build(dataset);
    }

    // [QExec] Becomes UpdateExec
    private UpdateProcessor build(DatasetGraph dsg) {
        Objects.requireNonNull(dsg, "No datset for update");
        Context cxt = Context.setupContextForDataset(context, dsg);
        UpdateEngineFactory f = UpdateEngineRegistry.get().find(dsg, cxt);
        if ( f == null )
            return null;
        UpdateProcessorBase uProc = new UpdateProcessorBase(updateRequest, dsg, initialBinding, cxt, f);
        return uProc;
    }

    // Abbreviated forms
    public void execute(DatasetGraph dsg) {
        dataset(dsg);
        UpdateProcessor uProc = build(dsg);
        uProc.execute();
    }

    private void add(UpdateRequest request) {
        request.getOperations().forEach(this::add);
    }

    private void add(Update update) {
        this.updateRequest.add(update);
    }


}
