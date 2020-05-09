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

import org.apache.jena.riot.WebContent;

public class WebContent2 {

    public static final String acceptEncoding        = "gzip, deflate" ;

    public static final String sparqlResults = initSelectContentTypes();
    public static final String sparqlResultsHeader = sparqlResults+
        ","+WebContent.contentTypeJSON+";q=0.2"+
        ","+WebContent.contentTypeXML+";q=0.2"+
        ",*/*;q=0.1";

    public static final String sparqlAsk = "application/sparql-results+json,application/sparql-results++xml;q=0.9";
    public static final String sparqlAskHeader = sparqlAsk+
        ","+WebContent.contentTypeJSON+";q=0.2"+
        ","+WebContent.contentTypeXML+";q=0.2"+
        ",*/*;q=0.1";


    private static String initSelectContentTypes() {
        StringBuilder sBuff = new StringBuilder() ;
        accumulateContentTypeString(sBuff, WebContent.contentTypeResultsJSON,  1.0);
        accumulateContentTypeString(sBuff, WebContent.contentTypeResultsXML,   0.9);     // Less efficient

        accumulateContentTypeString(sBuff, WebContent.contentTypeTextTSV,      0.7);
        accumulateContentTypeString(sBuff, WebContent.contentTypeTextCSV,      0.5);

//        // We try to parse these in the hope they are right.
//        accumulateContentTypeString(sBuff, WebContent.contentTypeJSON,         0.2);    // We try to parse these in
//        accumulateContentTypeString(sBuff, WebContent.contentTypeXML,          0.2);    // the hope they are right.
//        accumulateContentTypeString(sBuff, "*/*",                              0.1);    // Get something!
        return sBuff.toString() ;
    }

    private static void accumulateContentTypeString(StringBuilder sBuff, String str, double v) {
        if ( sBuff.length() != 0 )
            sBuff.append(", ") ;
        sBuff.append(str) ;
        if ( v < 1 )
            sBuff.append(";q=").append(v) ;
    }
}
