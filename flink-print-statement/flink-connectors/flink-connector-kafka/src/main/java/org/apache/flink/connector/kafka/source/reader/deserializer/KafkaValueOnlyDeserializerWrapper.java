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

package org.apache.flink.connector.kafka.source.reader.deserializer;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.util.Collector;
import org.apache.flink.util.InstantiationUtil;
import org.apache.flink.util.TemporaryClassLoaderContext;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/** A package private class to wrap {@link Deserializer}. */
class KafkaValueOnlyDeserializerWrapper<T> implements KafkaRecordDeserializationSchema<T> {

    private static final long serialVersionUID = 5409547407386004054L;

    private static final Logger LOG =
            LoggerFactory.getLogger(KafkaValueOnlyDeserializerWrapper.class);

    private final Class<? extends Deserializer<T>> deserializerClass;

    private final Map<String, String> config;

    private transient Deserializer<T> deserializer;

    KafkaValueOnlyDeserializerWrapper(
            Class<? extends Deserializer<T>> deserializerClass, Map<String, String> config) {
        this.deserializerClass = deserializerClass;
        this.config = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void open(DeserializationSchema.InitializationContext context) throws Exception {
        ClassLoader userCodeClassLoader = context.getUserCodeClassLoader().asClassLoader();
        try (TemporaryClassLoaderContext ignored =
                TemporaryClassLoaderContext.of(userCodeClassLoader)) {
            deserializer =
                    (Deserializer<T>)
                            InstantiationUtil.instantiate(
                                    deserializerClass.getName(),
                                    Deserializer.class,
                                    getClass().getClassLoader());

            if (deserializer instanceof Configurable) {
                ((Configurable) deserializer).configure(config);
            } else {
                // Always be false since this Deserializer is only used for value.
                deserializer.configure(config, false);
            }
        } catch (Exception e) {
            throw new IOException(
                    "Failed to instantiate the deserializer of class " + deserializerClass, e);
        }
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<T> collector)
            throws IOException {
        if (deserializer == null) {
            throw new IllegalStateException(
                    "The deserializer has not been created. Make sure the open() method has been "
                            + "invoked.");
        }

        T value = deserializer.deserialize(record.topic(), record.value());
        LOG.trace(
                "Deserialized [partition: {}-{}, offset: {}, timestamp: {}, value: {}]",
                record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp(),
                value);
        collector.collect(value);
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeExtractor.createTypeInfo(Deserializer.class, deserializerClass, 0, null, null);
    }
}
