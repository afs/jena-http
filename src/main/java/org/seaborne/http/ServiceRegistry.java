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

import java.util.Map;

import org.apache.jena.atlas.lib.Trie;
import org.apache.jena.sparql.engine.http.Params;

/**
 * A service registry is a set of actions to take to modify an HTTP request before
 * sending it to a specific endpoint.
 * The key can be a prefix.
 */
public class ServiceRegistry {
    public interface ServiceTuning { void modify(Params params, Map<String, String> httpHeaders) ; }

    public ServiceRegistry() { super(); }

    Trie<ServiceTuning> trie = new Trie<>();

    public void add(String key, ServiceTuning action) {
        trie.add(key, action);
    }

    public ServiceTuning find(String key) {
        return trie.longestMatch(key);
    }

    public void remove(String key) {
        trie.remove(key);
    }
}
