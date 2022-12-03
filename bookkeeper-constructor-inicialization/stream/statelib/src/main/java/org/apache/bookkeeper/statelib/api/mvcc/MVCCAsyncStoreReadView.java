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

package org.apache.bookkeeper.statelib.api.mvcc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.bookkeeper.api.kv.op.RangeOp;
import org.apache.bookkeeper.api.kv.result.KeyValue;
import org.apache.bookkeeper.api.kv.result.RangeResult;
import org.apache.bookkeeper.common.annotation.InterfaceAudience.Public;
import org.apache.bookkeeper.common.annotation.InterfaceStability.Evolving;

/**
 * The read view for a {@link MVCCAsyncStore}.
 *
 * @param <K> key type
 * @param <V> value type
 */
@Public
@Evolving
public interface MVCCAsyncStoreReadView<K, V> {


    CompletableFuture<V> get(K key);

    CompletableFuture<KeyValue<K, V>> getKeyValue(K key);

    CompletableFuture<List<KeyValue<K, V>>> range(K key, K endKey);

    CompletableFuture<RangeResult<K, V>> range(RangeOp<K, V> rangeOp);

}
