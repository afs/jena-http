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

// Either
//  1 - Accept AbstractBuilder<X> in code
//  2 - as()
//  3 - Type games.


// case 2 - hide
public abstract class AbstractBuilder<X, Y> {

    public Y setter() { return as(); }

    public abstract X build();

    // Using this pattern means the return is the subclass and if an app needs to
    // explicit put the builder in a variable, it has the type of the subclass.
    protected abstract Y as();

}

class ActualBuilder extends AbstractBuilder<String, ActualBuilder> {

    @Override
    public String build() {
        return null;
    }

    @Override
    protected ActualBuilder as() {
        return null;
    }}

class Foo {
    static void main() {
        ActualBuilder builder = new ActualBuilder();
        ActualBuilder builder2 = builder.setter();
        String x = builder.build();
    }
}