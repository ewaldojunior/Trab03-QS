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

package org.apache.flink.connector.pulsar.testutils.runtime.mock;

import io.netty.channel.EventLoopGroup;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.EnsemblePlacementPolicy;
import org.apache.bookkeeper.common.util.OrderedExecutor;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.pulsar.broker.BookKeeperClientFactory;
import org.apache.pulsar.broker.ServiceConfiguration;
import org.apache.pulsar.metadata.api.extended.MetadataStoreExtended;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/** A BookKeeperClientFactory implementation which returns a mocked bookkeeper. */
public class MockBookKeeperClientFactory implements BookKeeperClientFactory {

    private final OrderedExecutor executor =
            OrderedExecutor.newBuilder().numThreads(1).name("mock-pulsar-bookkeeper").build();

    private final BookKeeper bookKeeper = NonClosableMockBookKeeper.create(executor);

    @Override
    public BookKeeper create(
            ServiceConfiguration conf,
            MetadataStoreExtended store,
            EventLoopGroup eventLoopGroup,
            Optional<Class<? extends EnsemblePlacementPolicy>> ensemblePlacementPolicyClass,
            Map<String, Object> ensemblePlacementPolicyProperties)
            throws IOException {
        return bookKeeper;
    }

    @Override
    public BookKeeper create(
            ServiceConfiguration conf,
            MetadataStoreExtended store,
            EventLoopGroup eventLoopGroup,
            Optional<Class<? extends EnsemblePlacementPolicy>> ensemblePlacementPolicyClass,
            Map<String, Object> ensemblePlacementPolicyProperties,
            StatsLogger statsLogger)
            throws IOException {
        return bookKeeper;
    }

    @Override
    public void close() {
        try {
            bookKeeper.close();
            executor.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
