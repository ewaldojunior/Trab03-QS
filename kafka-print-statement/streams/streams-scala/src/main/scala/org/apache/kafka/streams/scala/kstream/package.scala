/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.streams.scala

import org.apache.kafka.streams.processor.StateStore

package object kstream {
  type Materialized[K, V, S <: StateStore] = org.apache.kafka.streams.kstream.Materialized[K, V, S]
  type Grouped[K, V] = org.apache.kafka.streams.kstream.Grouped[K, V]
  type Consumed[K, V] = org.apache.kafka.streams.kstream.Consumed[K, V]
  type Produced[K, V] = org.apache.kafka.streams.kstream.Produced[K, V]
  type Repartitioned[K, V] = org.apache.kafka.streams.kstream.Repartitioned[K, V]
  type Joined[K, V, VO] = org.apache.kafka.streams.kstream.Joined[K, V, VO]
  type StreamJoined[K, V, VO] = org.apache.kafka.streams.kstream.StreamJoined[K, V, VO]
  type Named = org.apache.kafka.streams.kstream.Named
  type Branched[K, V] = org.apache.kafka.streams.kstream.Branched[K, V]
}
