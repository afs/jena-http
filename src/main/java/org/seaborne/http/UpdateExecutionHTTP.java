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

import static org.seaborne.http.HttpLib.*;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.jena.query.ARQ;
import org.apache.jena.query.QueryException;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class UpdateExecutionHTTP implements UpdateProcessor {


    public enum SendMode { asPostForm, asPostBody }

    public static Builder newBuilder() { return new Builder(); }
    public static class Builder {

        private String serviceURL;
        private UpdateRequest update;
        private String updateString;
        private Params params = new Params();
        private boolean allowCompression;
        private Map<String, String> httpHeaders = new HashMap<>();
        private HttpClient httpClient;
        private SendMode sendMode = SendMode.asPostBody;
        private UpdateRequest updateRequest;

        private List<String> usingGraphURIs = null;
        private List<String> usingNamedGraphURIs = null;

        public Builder service(String serviceURL) {
            this.serviceURL = serviceURL;
            return this;
        }

        public Builder update(UpdateRequest updateRequest) {
            this.updateRequest = updateRequest;
            this.updateString = updateRequest.toString();
            return this;
        }

        public Builder updateString(String updateRequestString) {
            this.updateRequest = null;
            this.updateString = updateRequestString;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient);
            return this;
        }

        /**
         * Whether to send the update request using POST and an HTML form, content type
         * "application/x-www-form-urlencoded".
         *
         * If false (the default), send as "application/sparql-query" (default).
         */
        public Builder sendHtmlForm(boolean htmlForm) {
            this.sendMode =  htmlForm ? SendMode.asPostForm : SendMode.asPostBody;
            return this;
        }

        // The old code, UpdateProcessRemote, didn't support this so may be not
        // provide it as its not being used.

        public Builder addUsingGraphURI(String uri) {
            if (this.usingGraphURIs == null)
                this.usingGraphURIs = new ArrayList<>();
            this.usingGraphURIs.add(uri);
            return this;
        }

        public Builder addUsingNamedGraphURI(String uri) {
            if (this.usingNamedGraphURIs == null)
                this.usingNamedGraphURIs = new ArrayList<>();
            this.usingNamedGraphURIs.add(uri);
            return this;
        }

        public Builder param(String name) {
            Objects.requireNonNull(name);
            this.params.addParam(name);
            return this;
        }

        public Builder param(String name, String value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
            this.params.addParam(name, value);
            return this;
        }

        public Builder httpHeader(String headerName, String headerValue) {
            Objects.requireNonNull(headerName);
            Objects.requireNonNull(headerValue);
            this.httpHeaders.put(headerName, headerValue);
            return this;
        }

        public UpdateExecutionHTTP build() {
            Objects.requireNonNull(serviceURL, "No service URL");
            if ( update == null && updateString == null )
                throw new QueryException("No update for UpdateExecutionHTTP");
            return new UpdateExecutionHTTP(serviceURL, update, updateString, httpClient, params,
                                           copyArray(usingGraphURIs),
                                           copyArray(usingNamedGraphURIs),
                                           new HashMap<>(httpHeaders),
                                           sendMode);
        }
    }

    private final Context context;
    private final String service;
    private final UpdateRequest update;
    private final String updateString;
    private final Map<String, String> httpHeaders;
    private final HttpClient httpClient;
    private final SendMode sendMode;
    private final Params params;
    private final List<String> usingGraphURIs;
    private final List<String> usingNamedGraphURIs;

    private UpdateExecutionHTTP(String serviceURL, UpdateRequest update, String updateString,
                                HttpClient httpClient, Params params,
                                List<String> usingGraphURIs,
                                List<String> usingNamedGraphURIs,
                                Map<String, String> httpHeaders, SendMode sendMode) {
        this.context = ARQ.getContext().copy();
        this.service = serviceURL;
        this.update = update;
        this.updateString = updateString;
        this.httpClient = dft(httpClient, HttpEnv.getDftHttpClient());
        this.params = params;
        this.usingGraphURIs = usingGraphURIs;
        this.usingNamedGraphURIs = usingNamedGraphURIs;
        this.httpHeaders = httpHeaders;
        this.sendMode = sendMode;
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public DatasetGraph getDatasetGraph() {
        return null;
    }

    @Override
    public void execute() {

        Params thisParams = new Params(params);
        if ( usingGraphURIs != null ) {
            for ( String uri : usingGraphURIs )
                thisParams.addParam(HttpNames.paramUsingGraphURI, uri);
        }
        if ( usingNamedGraphURIs != null ) {
            for ( String uri : usingNamedGraphURIs )
                thisParams.addParam(HttpNames.paramUsingNamedGraphURI, uri);
        }

        modifyByService(service, context, params, httpHeaders);

        switch(sendMode) {
            case asPostBody :
                executePostBody(thisParams); break;
            case asPostForm :
                executePostForm(thisParams); break;
        }
    }

    private void executePostBody(Params thisParams) {
        String str = (updateString != null) ? updateString : update.toString();
        String requestURL = service;
        HttpLib.modifyByService(requestURL, context, thisParams, httpHeaders);
        if ( thisParams.count() > 0 ) {
            String qs = thisParams.httpString();
            requestURL = requestURL(requestURL, qs);
        }
        execute(requestURL, BodyPublishers.ofString(str), WebContent.contentTypeSPARQLUpdate);
    }

    private void executePostForm(Params thisParams) {
        String requestURL = service;
        thisParams.addParam(HttpParams.pUpdate, updateString);
        HttpLib.modifyByService(requestURL, context, thisParams, httpHeaders);
        String formString = thisParams.httpString();
        // Everything goes into the form body, no use of the request URI query string.
        execute(requestURL, BodyPublishers.ofString(formString, StandardCharsets.US_ASCII), WebContent.contentTypeHTMLForm);
    }

    private String execute(String requestURL, BodyPublisher body, String contentType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(HttpLib.toRequestURI(requestURL))
            .POST(body)
            .header(HttpNames.hContentType, contentType);
        httpHeaders.forEach((k,v)->builder.header(k,v));
        HttpRequest request = builder.build();
        HttpResponse<InputStream> response = HttpLib.execute(httpClient, request);
        return handleResponseRtnString(response);
    }
}
