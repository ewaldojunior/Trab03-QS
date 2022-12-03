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

package org.apache.flink.state.changelog;

import org.apache.flink.runtime.state.HashMapStateBackendTest;
import org.apache.flink.runtime.state.TestTaskStateManager;
import org.apache.flink.runtime.state.changelog.inmemory.InMemoryStateChangelogStorage;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link ChangelogStateBackend} using {@link InMemoryStateChangelogStorage} and
 * delegating {@link HashMapStateBackendTest}.
 */
public class ChangelogDelegateHashMapInMemoryTest extends ChangelogDelegateHashMapTest {

    @Rule public final TemporaryFolder temp = new TemporaryFolder();

    protected TestTaskStateManager getTestTaskStateManager() {
        return TestTaskStateManager.builder().build();
    }
}
