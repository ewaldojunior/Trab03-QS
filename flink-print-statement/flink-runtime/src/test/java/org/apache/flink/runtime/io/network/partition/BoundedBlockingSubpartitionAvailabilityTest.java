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

package org.apache.flink.runtime.io.network.partition;

import org.apache.flink.runtime.io.disk.FileChannelManagerImpl;
import org.apache.flink.runtime.io.network.buffer.BufferBuilderTestUtils;
import org.apache.flink.runtime.io.network.partition.ResultSubpartition.BufferAndBacklog;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.flink.runtime.io.network.partition.PartitionTestUtils.createView;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the availability handling of the BoundedBlockingSubpartitions with not constant
 * availability.
 */
public class BoundedBlockingSubpartitionAvailabilityTest {

    @ClassRule public static final TemporaryFolder TMP_FOLDER = new TemporaryFolder();

    private static final int BUFFER_SIZE = 32 * 1024;

    @Test
    public void testInitiallyNotAvailable() throws Exception {
        final ResultSubpartition subpartition = createPartitionWithData(10);
        final CountingAvailabilityListener listener = new CountingAvailabilityListener();

        // test
        final ResultSubpartitionView subpartitionView = createView(subpartition, listener);

        // assert
        assertEquals(0, listener.numNotifications);

        // cleanup
        subpartitionView.releaseAllResources();
        subpartition.release();
    }

    @Test
    public void testUnavailableWhenBuffersExhausted() throws Exception {
        // setup
        final ResultSubpartition subpartition = createPartitionWithData(100_000);
        final CountingAvailabilityListener listener = new CountingAvailabilityListener();
        final ResultSubpartitionView reader = createView(subpartition, listener);

        // test
        final List<BufferAndBacklog> data = drainAvailableData(reader);

        // assert
        assertFalse(reader.getAvailabilityAndBacklog(Integer.MAX_VALUE).isAvailable());
        assertFalse(data.get(data.size() - 1).isDataAvailable());

        // cleanup
        reader.releaseAllResources();
        subpartition.release();
    }

    @Test
    public void testAvailabilityNotificationWhenBuffersReturn() throws Exception {
        // setup
        final ResultSubpartition subpartition = createPartitionWithData(100_000);
        final CountingAvailabilityListener listener = new CountingAvailabilityListener();
        final ResultSubpartitionView reader = createView(subpartition, listener);

        // test
        final List<ResultSubpartition.BufferAndBacklog> data = drainAvailableData(reader);
        data.get(0).buffer().recycleBuffer();
        data.get(1).buffer().recycleBuffer();

        // assert
        assertTrue(reader.getAvailabilityAndBacklog(Integer.MAX_VALUE).isAvailable());
        assertEquals(1, listener.numNotifications);

        // cleanup
        reader.releaseAllResources();
        subpartition.release();
    }

    @Test
    public void testNotAvailableWhenEmpty() throws Exception {
        // setup
        final ResultSubpartition subpartition = createPartitionWithData(100_000);
        final ResultSubpartitionView reader =
                subpartition.createReadView(new NoOpBufferAvailablityListener());

        // test
        drainAllData(reader);

        // assert
        assertFalse(reader.getAvailabilityAndBacklog(Integer.MAX_VALUE).isAvailable());

        // cleanup
        reader.releaseAllResources();
        subpartition.release();
    }

    // ------------------------------------------------------------------------

    private static ResultSubpartition createPartitionWithData(int numberOfBuffers)
            throws IOException {
        BoundedBlockingResultPartition parent =
                (BoundedBlockingResultPartition)
                        new ResultPartitionBuilder()
                                .setResultPartitionType(ResultPartitionType.BLOCKING_PERSISTENT)
                                .setBoundedBlockingSubpartitionType(
                                        BoundedBlockingSubpartitionType.FILE)
                                .setSSLEnabled(true)
                                .setFileChannelManager(
                                        new FileChannelManagerImpl(
                                                new String[] {TMP_FOLDER.newFolder().toString()},
                                                "data"))
                                .setNetworkBufferSize(BUFFER_SIZE)
                                .build();

        ResultSubpartition partition = parent.getAllPartitions()[0];

        writeBuffers(partition, numberOfBuffers);
        partition.finish();

        return partition;
    }

    private static void writeBuffers(ResultSubpartition partition, int numberOfBuffers)
            throws IOException {
        for (int i = 0; i < numberOfBuffers; i++) {
            partition.add(BufferBuilderTestUtils.createFilledFinishedBufferConsumer(BUFFER_SIZE));
        }
    }

    private static List<BufferAndBacklog> drainAvailableData(ResultSubpartitionView reader)
            throws Exception {
        final ArrayList<BufferAndBacklog> list = new ArrayList<>();

        BufferAndBacklog bab;
        while ((bab = reader.getNextBuffer()) != null) {
            list.add(bab);
        }

        return list;
    }

    private static void drainAllData(ResultSubpartitionView reader) throws Exception {
        BufferAndBacklog bab;
        while ((bab = reader.getNextBuffer()) != null) {
            bab.buffer().recycleBuffer();
        }
    }
}
