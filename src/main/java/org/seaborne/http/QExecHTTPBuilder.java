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
import java.util.HashMap;
import java.util.Objects;

import org.apache.jena.query.QueryException;
import org.apache.jena.sparql.engine.http.Params;
import org.seaborne.qexec.QExec;

public class QExecHTTPBuilder extends ExecBuilderQueryHTTP<QExec, QExecHTTPBuilder> {

    public static QExecHTTPBuilder newBuilder() { return new QExecHTTPBuilder(); }

    private QExecHTTPBuilder() {}

    @Override
    public QExecHTTP build() {
      Objects.requireNonNull(serviceURL, "No service URL");
      if ( queryString == null && query == null )
          throw new QueryException("No query for QueryExecHTTP");
      HttpClient hClient = HttpEnv.getHttpClient(serviceURL, httpClient);
      return new QExecHTTP(serviceURL, query, queryString, urlLimit,
                           hClient, new HashMap<>(httpHeaders), new Params(params),
                           copyArray(defaultGraphURIs),
                           copyArray(namedGraphURIs),
                           sendMode, acceptHeader, allowCompression,
                           timeout, timeoutUnit);
    }

//    private QueryExecutionHTTP build2() {
//        Objects.requireNonNull(serviceURL, "No service URL");
//        if ( queryString == null && query == null )
//            throw new QueryException("No query for QueryExecutionHTTP");
//        HttpClient hClient = HttpEnv.getHttpClient(serviceURL, httpClient);
//        return new QueryExecutionHTTP(serviceURL, query, queryString, urlLimit,
//                                      hClient, new HashMap<>(httpHeaders), new Params(params),
//                                      copyArray(defaultGraphURIs),
//                                      copyArray(namedGraphURIs),
//                                      sendMode, acceptHeader, allowCompression,
//                                      timeout, timeoutUnit);
//    }

    @Override
    protected QExecHTTPBuilder thisBuilder() {
        return this;
    }
}
