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
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ARQ;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.engine.http.Params;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.UpdateRequest;
import org.seaborne.improvements.UpdateExecution;

public class UpdateExecutionHTTP implements /* UpdateProcessor old world, */ UpdateExecution {

    enum SendMode {
        // POST HTML forms (update=...)
        asPostForm,
        // POST application/sparql-update
        asPostBody
        }

    /*package*/static SendMode defaultSendMode = SendMode.asPostBody;

    public static UpdateExecutionHTTPBuilder newBuilder() { return new UpdateExecutionHTTPBuilder(); }

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

    /*package*/ UpdateExecutionHTTP(String serviceURL, UpdateRequest update, String updateString,
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

//    @Override
//    public Context getContext() {
//        return null;
//    }
//
//    @Override
//    public DatasetGraph getDatasetGraph() {
//        return null;
//    }

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

        // Same as QueryExecutionHTTP
        modifyByService(service, context, thisParams, httpHeaders);

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
        if ( thisParams.count() > 0 ) {
            String qs = thisParams.httpString();
            requestURL = requestURL(requestURL, qs);
        }
        executeUpdate(requestURL, BodyPublishers.ofString(str), WebContent.contentTypeSPARQLUpdate);
    }

    private void executePostForm(Params thisParams) {
        String requestURL = service;
        thisParams.addParam(HttpParams.pUpdate, updateString);
        String formString = thisParams.httpString();
        // Everything goes into the form body, no use of the request URI query string.
        executeUpdate(requestURL, BodyPublishers.ofString(formString, StandardCharsets.US_ASCII), WebContent.contentTypeHTMLForm);
    }

    private String executeUpdate(String requestURL, BodyPublisher body, String contentType) {
        HttpRequest.Builder builder = HttpLib.newBuilder(requestURL, httpHeaders, false, -1L, null);
        builder = contentTypeHeader(builder, contentType);
        HttpRequest request = builder.POST(body).build();
        HttpResponse<InputStream> response = HttpLib.execute(httpClient, request);
        return handleResponseRtnString(response);
    }
}
