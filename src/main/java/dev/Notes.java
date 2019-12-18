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

package dev;

import java.net.http.HttpClient;

public class Notes {
    // New!

    // Replacement for QueryExecution using a builder style.

    // What does an empty body return? "" ?

    // All Nodes.
    // Set initial bindings: simple version
    // RFC 7231: 3.1.4.2. and Appendix B: Content-Location does not affect base URI. // SKW.
    /*
     * Local:
     *
     * ResultSet rs = QE.query(Query).source(Dataset).setTimeout().execSelect();
     *
     * Avoid the try-resource:
     * QE....select(Consumer<Binding> rowHandler)
     * QE....construct() -> Model
     * QE....describe()
     * QE....ask() -> boolean
     * Query
     *
     *
     */

    // https://openjdk.java.net/groups/net/httpclient/recipes.html

//  public static InputStream getDecodedInputStream(HttpResponse<InputStream> httpResponse) {
//      String encoding = determineContentEncoding(httpResponse);
//      try {
//          switch (encoding) {
//              case "" :
//                  return httpResponse.body();
//              case "gzip" :
//                  return new GZIPInputStream(httpResponse.body());
//              default :
//                  throw new UnsupportedOperationException("Unexpected Content-Encoding: " + encoding);
//          }
//      } catch (IOException ioe) {
//          throw new UncheckedIOException(ioe);
//      }
//  }
//
//  public static String determineContentEncoding(HttpResponse<? > httpResponse) {
//      return httpResponse.headers().firstValue("Content-Encoding").orElse("");
//  }

    // XXX Maybe: A BodySubscriber that is "direct to StreamRDF"
    // User/password?
    static {

        HttpClient.newBuilder()
//         .authenticator(/*Authenticator*/null)
//         .sslParameters(null)
//         .sslContext(null)
        .build();
    }

  /*
  // Design Notes:
   * Use Java11 HTTP code
   *
   * HttpOp - have common uses cases, but less coverage.
   *   httpGet(args) and httpGet(HttpClient, args) versions.
   *   expose java.net.http.HttpClient for any special setup.
   *   retain the idea of one default "system wide" HttpClient so common uses cases "Just work".
   *   No "HttpResponseHandler" variants. POST PUT in thsi "common case" library assume no response other than error code.
   *   No "httpPostForm"
   *     That's about 50% of the execs.
   *   Separate HttpOpEnv to put all the
   *   This is a helper class - not the required/expected way to do all HTTP
   *   It is not async.
   *     If you are writing a spider with async requests, you'll want control of the HttpClient.
   *
   * Library of functions, RDF-centric BodyHandler/BodyPublishers, deal with compression on input stream.
   *   Should be useful for sync and async.
   *
   * RDF-specific functions REST-ish operation GET, POST, PUT.,DELETE graphs/datasets.
   *    This is sub-GSP - does not include GSP naming.
   *    Avoids the trap of applications not handling connections correctly and HTTP locking up.
   *    This is partly design (e.g. no TypeInputs=Stream, which must be closed else delayed problems) and partly documentation.
   *    In other words, to get cleaver,m the app writer needs to engage with java.net.http.
   *
   * GSP in RDFConnection.
   *   (will now include compression handling)
   *
   * ResultSet via an HttpQuery using java.net.http.
   *   Again, with default global setup.
   *   Builder pattern for the per object setters.
   *   This may use HttpOp or directly use the java.net.http code. Worth doing it the best way for the long term.
   *
   * New RDFConn (other name?) same operations as RDFConnection but at the Graph/Node level.
   *   based around Java JDK HttpClient.
   * RDFConnection is an adapter to Resource/Model.
   */


}
