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

package org.seaborne;

import org.apache.jena.http.TS_JenaHttp;
import org.apache.jena.integration.TS_RDFLinkIntegration;
import org.apache.jena.link.TS_RDFLink;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.seaborne.improvements.TestExecutionBuilders;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
    TS_RDFLink.class
    , TS_RDFLinkIntegration.class
    , TS_JenaHttp.class
    , TestExecutionBuilders.class
})
public class TC_NewLink {

}
