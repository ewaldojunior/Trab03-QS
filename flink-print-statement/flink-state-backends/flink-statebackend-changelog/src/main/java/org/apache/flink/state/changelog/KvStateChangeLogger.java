/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.state.changelog;

import org.apache.flink.annotation.Internal;

import java.io.IOException;
import java.util.Collection;

/**
 * A logger for some key-partitioned state (not only {@link
 * org.apache.flink.api.common.state.ValueState}.
 *
 * @param <Value> the type of state values
 * @param <Namespace> the type of namespace
 */
@Internal
public interface KvStateChangeLogger<Value, Namespace> extends StateChangeLogger<Value, Namespace> {

    void namespacesMerged(Namespace target, Collection<Namespace> sources) throws IOException;
}
