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
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.sparql.engine.http.Params;
import org.seaborne.http.QueryExecutionHTTP.SendMode;

public class QueryExecutionHTTPBuilder {
    private String serviceURL = null;
    private Query query = null;
    private String queryString = null;
    private HttpClient httpClient = null;
    private Params params = new Params();
    // Accept: Handled as special case because the defaults varies by query type.
    private String acceptHeader;
    private boolean allowCompression;
    private Map<String, String> httpHeaders = new HashMap<>();
    private long timeout = -1;
    private TimeUnit timeoutUnit = null;

    private int urlLimit = HttpEnv.urlLimit;
    private SendMode sendMode = QueryExecutionHTTP.defaultSendMode;
    private List<String> defaultGraphURIs = new ArrayList<>();
    private List<String> namedGraphURIs = new ArrayList<>();

    /** Set the URL of the query endpoint. */
    public QueryExecutionHTTPBuilder service(String serviceURL) {
        this.serviceURL = Objects.requireNonNull(serviceURL);
        return this;
    }

    /** Set the query - this also sets the query string to agree with the query argument. */
    public QueryExecutionHTTPBuilder query(Query query) {
        this.query = Objects.requireNonNull(query);
        this.queryString = query.toString();
        return this;
    }

    /** Set the query string - this also clears any Query already set. */
    public QueryExecutionHTTPBuilder queryString(String queryString) {
        this.query = null;
        this.queryString = Objects.requireNonNull(queryString);
        return this;
    }

    public QueryExecutionHTTPBuilder addDefaultGraphURI(String uri) {
        if (this.defaultGraphURIs == null)
            this.defaultGraphURIs = new ArrayList<>();
        this.defaultGraphURIs.add(uri);
        return this;
    }

    public QueryExecutionHTTPBuilder addNamedGraphURI(String uri) {
        if (this.namedGraphURIs == null)
            this.namedGraphURIs = new ArrayList<>();
        this.namedGraphURIs.add(uri);
        return this;
    }

    public QueryExecutionHTTPBuilder httpClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
        return this;
    }

    /**
     * Send the query using HTTP POST with HTML form-encoded data.
     * If set false, the URL limit still applies.
     */
    public QueryExecutionHTTPBuilder sendHtmlForm(boolean htmlForm) {
        this.sendMode =  htmlForm ? SendMode.asPostForm : SendMode.asGetWithLimit;
        return this;
    }

    /**
     * Send the query using HTTP GET and the HTTP URL query string,
     * unless the request exceeds the {@link #urlGetLimit}
     * (system default {@link HttpEnv#urlLimit}).
     * <p>
     * If it exceeds the limit, switch to using a HTML form and POST request.
     * By default, queries with a log URL are sent in an HTTP form with POST.
     * <p>
     * This is the default setting.
     *
     * @see #urlGetLimit
     * @see #useGet
     * @see #postQuery
     */
    public QueryExecutionHTTPBuilder useGetWithLimit() {
        this.sendMode = SendMode.asGetWithLimit;
        return this;
    }

    /**
     * Send the query using HTTP GET and the HTTP URL query string regardless of length.
     * By default, queries with a log URL are sent in an HTTP form with POST.
     */
    public QueryExecutionHTTPBuilder useGet() {
        this.sendMode = SendMode.asGetAlways;
        return this;
    }

    /**
     * Send the query request using POST with a Content-Type of as a
     * "application/sparql-query"
     */
    public QueryExecutionHTTPBuilder postQuery() {
        this.sendMode = SendMode.asPostBody;
        return this;
    }

    /**
     * Maximum length for a GET request URL, this includes the length of the
     * service endpoint URL - longer than this and the request will use
     * POST/Form.
     * <p>
     * Long URLs can be silently truncated by intermediate systems and proxies.
     * Use of the URL query string means that request are not cached.
     * <p>
     * See also {@link #postQuery} to send the request using HTTP POST with the
     * query in the POST body using {@code Content-Type} "application/sparql-query"
     * <p>
     * See also {@link #sendHtmlForm(boolean)} to send a request as an HTML form.
     */
    public QueryExecutionHTTPBuilder urlGetLimit(int urlLimit) {
        this.urlLimit = urlLimit;
        return this;
    }

    public QueryExecutionHTTPBuilder param(String name) {
        Objects.requireNonNull(name);
        this.params.addParam(name);
        return this;
    }

    public QueryExecutionHTTPBuilder param(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.params.addParam(name, value);
        return this;
    }

    public QueryExecutionHTTPBuilder acceptHeader(String acceptHeader) {
        Objects.requireNonNull(acceptHeader);
        this.acceptHeader = acceptHeader;
        return this;
    }

    public QueryExecutionHTTPBuilder httpHeader(String headerName, String headerValue) {
        Objects.requireNonNull(headerName);
        Objects.requireNonNull(headerValue);
        this.httpHeaders.put(headerName, headerValue);
        return this;
    }

    public QueryExecutionHTTPBuilder allowCompression(boolean allowCompression) {
        this.allowCompression = allowCompression;
        return this;
    }

    /**
     * Set a timeout to the overall overall operation.
     * Time-to-connect can be set with a custom {@link HttpClient} - see {@link java.net.http.HttpClient.Builder#connectTimeout(java.time.Duration)}.
     */
    public QueryExecutionHTTPBuilder timeout(long timeout, TimeUnit timeoutUnit) {
        if ( timeout < 0 ) {
            this.timeout = -1;
            this.timeoutUnit = null;
        } else {
            this.timeout = timeout;
            this.timeoutUnit = Objects.requireNonNull(timeoutUnit);
        }
        return this;
    }

    public QueryExecutionHTTP build() {
        Objects.requireNonNull(serviceURL, "No service URL");
        if ( queryString == null && query == null )
            throw new QueryException("No query for QueryExecutionHTTP");
        HttpClient hClient = HttpEnv.getHttpClient(serviceURL, httpClient);
        return new QueryExecutionHTTP(serviceURL, query, queryString, urlLimit,
                                      hClient, new HashMap<>(httpHeaders), new Params(params),
                                      copyArray(defaultGraphURIs),
                                      copyArray(namedGraphURIs),
                                      sendMode, acceptHeader, allowCompression,
                                      timeout, timeoutUnit);
    }
}
