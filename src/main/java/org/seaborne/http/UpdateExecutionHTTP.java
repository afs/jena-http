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

import static org.seaborne.http.HttpLib.bodyStringFetcher;
import static org.seaborne.http.HttpLib.dft;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.query.ARQ;
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

        // Protocol
        //using-graph-uri (0 or more)
        //using-named-graph-uri

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient);
            return this;
        }

        /**
         * Whether to send the update request using POST and an HTML form, content type
         * "application/x-www-form-urlencoded".
         *
         * If false, send as "application/sparql-query" (default).
         */
        public Builder sendHtmlForm(boolean htmlForm) {
            this.sendMode =  htmlForm ? SendMode.asPostForm : SendMode.asPostBody;
            return this;
        }

//        /**
//         * Whether to send the update request using POST with a Content-Type of as a
//         * "application/sparql-query".
//         *
//         * If true, send as "application/sparql-query" (default); if false, send using an HTML form.
//         * @see #sendHtmlForm(boolean)
//         */
//        public Builder postUpdate(boolean post) {
//            this.sendMode = post ? SendMode.asPostBody : SendMode.asPostForm;
//            return this;
//        }

//        public Builder param(String name) {
//            Objects.requireNonNull(name);
//            this.params.addParam(name);
//            return this;
//        }
//
//        public Builder param(String name, String value) {
//            Objects.requireNonNull(name);
//            Objects.requireNonNull(value);
//            this.params.addParam(name, value);
//            return this;
//        }

        public Builder httpHeader(String headerName, String headerValue) {
            Objects.requireNonNull(headerName);
            Objects.requireNonNull(headerValue);
            this.httpHeaders.put(headerName, headerValue);
            return this;
        }

        public UpdateExecutionHTTP build() {
            Objects.requireNonNull("No service URL", serviceURL);
            return new UpdateExecutionHTTP(serviceURL, update, updateString, httpClient, params, httpHeaders, sendMode);
        }
    }

    private Context context;
    private String service;
    private UpdateRequest update;
    private String updateString;
    private Map<String, String> httpHeaders;
    private HttpClient httpClient;
    private SendMode sendMode;
    private Params params;

    private UpdateExecutionHTTP(String serviceURL, UpdateRequest update, String updateString,
                                HttpClient httpClient, Params params, Map<String, String> httpHeaders, SendMode sendMode) {
        this.context = ARQ.getContext().copy();
        this.service = serviceURL;
        this.update = update;
        this.updateString = updateString;
        this.httpHeaders = new HashMap<>(httpHeaders);
        this.httpClient = dft(httpClient, HttpEnv.getDftHttpClient());
        this.params = params;
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
        switch(sendMode) {
            case asPostBody :
                executePostBody(); break;
            case asPostForm :
                executePostForm(); break;
        }
    }

    private void executePostBody() {
        String str = (updateString != null) ? updateString : update.toString();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(HttpLib.toURI(service))
            .POST(BodyPublishers.ofString(str))
            .header(HttpNames.hContentType, WebContent.contentTypeSPARQLUpdate);
        httpHeaders.forEach((k,v)->builder.header(k,v));
        HttpRequest request = builder.build();
        executeUpdate(httpClient, request);
    }

    private HttpResponse<String> executeUpdate(HttpClient httpClient2, HttpRequest request) {
        HttpResponse<String> response = HttpLib.execute(httpClient, request, BodyHandlers.ofString());
        HttpLib.handleHttpStatusCode(response, bodyStringFetcher);
        return response;
    }

    private void executePostForm() {
        Params thisParams = new Params(params);
        String postStr = params.toString();
        thisParams.addParam(HttpParams.pUpdate, updateString);
        String formString = thisParams.httpString();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(HttpLib.toURI(service))
            .POST(BodyPublishers.ofString(formString, StandardCharsets.US_ASCII))
            .header(HttpNames.hContentType, WebContent.contentTypeHTMLForm);
        httpHeaders.forEach((k,v)->builder.header(k,v));
        HttpRequest request = builder.build();
        executeUpdate(httpClient, request);
    }
}
