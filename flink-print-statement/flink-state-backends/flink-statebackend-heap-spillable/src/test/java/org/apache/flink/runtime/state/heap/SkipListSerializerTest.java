/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.flink.runtime.state.heap;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.memory.ByteArrayInputStreamWithPos;
import org.apache.flink.core.memory.DataInputViewStreamWrapper;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.core.memory.MemorySegmentFactory;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for {@link SkipListKeySerializer}. */
class SkipListSerializerTest {
    private static final TypeSerializer<String> keySerializer = StringSerializer.INSTANCE;
    private static final TypeSerializer<String> namespaceSerializer = StringSerializer.INSTANCE;
    private static final SkipListKeySerializer<String, String> skipListKeySerializer =
            new SkipListKeySerializer<>(keySerializer, namespaceSerializer);
    private static final TypeSerializer<String> stateSerializer = StringSerializer.INSTANCE;
    private static final SkipListValueSerializer<String> skipListValueSerializer =
            new SkipListValueSerializer<>(stateSerializer);

    @Test
    void testSkipListKeySerializerBasicOp() throws IOException {
        testSkipListKeySerializer(0);
    }

    @Test
    void testSkipListKeySerializerStateless() throws IOException {
        for (int i = 0; i < 10; i++) {
            testSkipListKeySerializer(i);
        }
    }

    private void testSkipListKeySerializer(int delta) throws IOException {
        String key = "key-abcdedg" + delta;
        String namespace = "namespace-dfsfdafd" + delta;

        byte[] skipListKey = skipListKeySerializer.serialize(key, namespace);
        int offset = 10;
        byte[] data = new byte[10 + skipListKey.length];
        System.arraycopy(skipListKey, 0, data, offset, skipListKey.length);
        MemorySegment skipListKeySegment = MemorySegmentFactory.wrap(data);
        assertThat(
                        skipListKeySerializer.deserializeKey(
                                skipListKeySegment, offset, skipListKey.length))
                .isEqualTo(key);
        assertThat(
                        skipListKeySerializer.deserializeNamespace(
                                skipListKeySegment, offset, skipListKey.length))
                .isEqualTo(namespace);

        Tuple2<byte[], byte[]> serializedKeyAndNamespace =
                skipListKeySerializer.getSerializedKeyAndNamespace(skipListKeySegment, offset);
        assertThat(deserialize(keySerializer, serializedKeyAndNamespace.f0)).isEqualTo(key);
        assertThat(deserialize(namespaceSerializer, serializedKeyAndNamespace.f1))
                .isEqualTo(namespace);

        byte[] serializedNamespace = skipListKeySerializer.serializeNamespace(namespace);
        assertThat(deserialize(namespaceSerializer, serializedNamespace)).isEqualTo(namespace);
    }

    @Test
    void testSkipListValueSerializerBasicOp() throws IOException {
        testSkipListValueSerializer(0);
    }

    @Test
    void testSkipListValueSerializerStateless() throws IOException {
        for (int i = 0; i < 10; i++) {
            testSkipListValueSerializer(i);
        }
    }

    private void testSkipListValueSerializer(int i) throws IOException {
        String state = "value-" + i;
        byte[] value = skipListValueSerializer.serialize(state);
        int offset = 10;
        byte[] data = new byte[10 + value.length];
        System.arraycopy(value, 0, data, offset, value.length);
        assertThat(deserialize(stateSerializer, value)).isEqualTo(state);
        assertThat(
                        skipListValueSerializer.deserializeState(
                                MemorySegmentFactory.wrap(data), offset, value.length))
                .isEqualTo(state);
    }

    private <T> T deserialize(TypeSerializer<T> serializer, byte[] data) throws IOException {
        ByteArrayInputStreamWithPos inputStream = new ByteArrayInputStreamWithPos(data);
        DataInputViewStreamWrapper inputView = new DataInputViewStreamWrapper(inputStream);
        return serializer.deserialize(inputView);
    }
}
