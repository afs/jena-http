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

package org.seaborne.http;

import static org.seaborne.http.HttpLib.copyArray;

import java.net.http.HttpClient;
import java.util.*;

import org.apache.jena.query.QueryException;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.update.UpdateRequest;
import org.seaborne.http.UpdateExecutionHTTP.UpdateSendMode;

public class UpdateExecutionHTTPBuilder {

    static { JenaSystem.init(); }

    private String serviceURL;
    private UpdateRequest update;
    private String updateString;
    private Params params = new Params();
    private boolean allowCompression;
    private Map<String, String> httpHeaders = new HashMap<>();
    private HttpClient httpClient;
    private UpdateSendMode sendMode = UpdateExecutionHTTP.defaultSendMode;
    private UpdateRequest updateRequest;

    private List<String> usingGraphURIs = null;
    private List<String> usingNamedGraphURIs = null;

    public UpdateExecutionHTTPBuilder() {}


    public UpdateExecutionHTTPBuilder service(String serviceURL) {
        this.serviceURL = serviceURL;
        return this;
    }

    /** Set the update - this also sets the update string to agree with the setting. */
    public UpdateExecutionHTTPBuilder update(UpdateRequest updateRequest) {
        this.updateRequest = Objects.requireNonNull(updateRequest);
        this.updateString = updateRequest.toString();
        return this;
    }

    public UpdateExecutionHTTPBuilder updateString(String updateRequestString) {
        this.updateRequest = null;
        this.updateString = Objects.requireNonNull(updateRequestString);
        return this;
    }

    public UpdateExecutionHTTPBuilder httpClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
        return this;
    }

    /**
     * Whether to send the update request using POST and an HTML form, content type
     * "application/x-www-form-urlencoded".
     *
     * If false (the default), send as "application/sparql-query" (default).
     */
    public UpdateExecutionHTTPBuilder sendHtmlForm(boolean htmlForm) {
        this.sendMode =  htmlForm ? UpdateSendMode.asPostForm : UpdateSendMode.asPostBody;
        return this;
    }

    // The old code, UpdateProcessRemote, didn't support this so may be not
    // provide it as its not being used.

    public UpdateExecutionHTTPBuilder addUsingGraphURI(String uri) {
        if (this.usingGraphURIs == null)
            this.usingGraphURIs = new ArrayList<>();
        this.usingGraphURIs.add(uri);
        return this;
    }

    public UpdateExecutionHTTPBuilder addUsingNamedGraphURI(String uri) {
        if (this.usingNamedGraphURIs == null)
            this.usingNamedGraphURIs = new ArrayList<>();
        this.usingNamedGraphURIs.add(uri);
        return this;
    }

    public UpdateExecutionHTTPBuilder param(String name) {
        Objects.requireNonNull(name);
        this.params.addParam(name);
        return this;
    }

    public UpdateExecutionHTTPBuilder param(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.params.addParam(name, value);
        return this;
    }

    public UpdateExecutionHTTPBuilder httpHeader(String headerName, String headerValue) {
        Objects.requireNonNull(headerName);
        Objects.requireNonNull(headerValue);
        this.httpHeaders.put(headerName, headerValue);
        return this;
    }

    public UpdateExecutionHTTP build() {
        Objects.requireNonNull(serviceURL, "No service URL");
        if ( update == null && updateString == null )
            throw new QueryException("No update for UpdateExecutionHTTP");
        HttpClient hClient = HttpEnv.getHttpClient(serviceURL, httpClient);
        return new UpdateExecutionHTTP(serviceURL, update, updateString, hClient, params,
                                       copyArray(usingGraphURIs),
                                       copyArray(usingNamedGraphURIs),
                                       new HashMap<>(httpHeaders),
                                       sendMode);
    }
}
