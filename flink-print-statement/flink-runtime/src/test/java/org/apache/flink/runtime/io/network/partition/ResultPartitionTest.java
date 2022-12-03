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

import org.apache.flink.runtime.checkpoint.channel.ResultSubpartitionInfo;
import org.apache.flink.runtime.io.disk.FileChannelManager;
import org.apache.flink.runtime.io.disk.FileChannelManagerImpl;
import org.apache.flink.runtime.io.network.NettyShuffleEnvironment;
import org.apache.flink.runtime.io.network.NettyShuffleEnvironmentBuilder;
import org.apache.flink.runtime.io.network.api.EndOfData;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.StopMode;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.util.EnvironmentInformation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.apache.flink.runtime.io.network.buffer.LocalBufferPoolDestroyTest.isInBlockingBufferRequest;
import static org.apache.flink.runtime.io.network.partition.PartitionTestUtils.createPartition;
import static org.apache.flink.runtime.io.network.partition.PartitionTestUtils.verifyCreateSubpartitionViewThrowsException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for {@link ResultPartition}. */
class ResultPartitionTest {

    private static final String tempDir = EnvironmentInformation.getTemporaryFileDirectory();

    private static FileChannelManager fileChannelManager;

    private final int bufferSize = 1024;

    @BeforeAll
    static void setUp() {
        fileChannelManager = new FileChannelManagerImpl(new String[] {tempDir}, "testing");
    }

    @AfterAll
    static void shutdown() throws Exception {
        fileChannelManager.close();
    }

    @Test
    void testResultSubpartitionInfo() {
        final int numPartitions = 2;
        final int numSubpartitions = 3;

        for (int i = 0; i < numPartitions; i++) {
            final PipelinedResultPartition partition =
                    (PipelinedResultPartition)
                            new ResultPartitionBuilder()
                                    .setResultPartitionIndex(i)
                                    .setNumberOfSubpartitions(numSubpartitions)
                                    .build();

            ResultSubpartition[] subpartitions = partition.getAllPartitions();
            for (int j = 0; j < subpartitions.length; j++) {
                ResultSubpartitionInfo subpartitionInfo = subpartitions[j].getSubpartitionInfo();

                assertThat(subpartitionInfo.getPartitionIdx()).isEqualTo(i);
                assertThat(subpartitionInfo.getSubPartitionIdx()).isEqualTo(j);
            }
        }
    }

    @Test
    void testAddOnFinishedPipelinedPartition() throws Exception {
        testAddOnFinishedPartition(ResultPartitionType.PIPELINED);
    }

    @Test
    void testAddOnFinishedBlockingPartition() throws Exception {
        testAddOnFinishedPartition(ResultPartitionType.BLOCKING);
    }

    @Test
    void testBlockingPartitionIsConsumableMultipleTimesIfNotReleasedOnConsumption()
            throws IOException {
        ResultPartitionManager manager = new ResultPartitionManager();

        final ResultPartition partition =
                new ResultPartitionBuilder()
                        .setResultPartitionManager(manager)
                        .setResultPartitionType(ResultPartitionType.BLOCKING)
                        .setFileChannelManager(fileChannelManager)
                        .build();

        manager.registerResultPartition(partition);
        partition.finish();

        assertThat(manager.getUnreleasedPartitions()).contains(partition.getPartitionId());

        // a blocking partition that is not released on consumption should be consumable multiple
        // times
        for (int x = 0; x < 2; x++) {
            ResultSubpartitionView subpartitionView1 =
                    partition.createSubpartitionView(0, () -> {});
            subpartitionView1.releaseAllResources();

            // partition should not be released on consumption
            assertThat(manager.getUnreleasedPartitions()).contains(partition.getPartitionId());
            assertThat(partition.isReleased()).isFalse();
        }
    }

    /**
     * Tests {@link ResultPartition#emitRecord} on a partition which has already finished.
     *
     * @param partitionType the result partition type to set up
     */
    private void testAddOnFinishedPartition(final ResultPartitionType partitionType)
            throws Exception {
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(partitionType);
        assertThatThrownBy(
                        () -> {
                            bufferWritingResultPartition.finish();
                            // partitionWriter.emitRecord() should fail
                            bufferWritingResultPartition.emitRecord(
                                    ByteBuffer.allocate(bufferSize), 0);
                        })
                .isInstanceOf(IllegalStateException.class);

        assertThat(bufferWritingResultPartition.numBuffersOut.getCount()).isZero();
        assertThat(bufferWritingResultPartition.numBytesOut.getCount()).isZero();
        assertThat(bufferWritingResultPartition.getBufferPool().bestEffortGetNumOfUsedBuffers())
                .isZero();
    }

    @Test
    void testAddOnReleasedPipelinedPartition() throws Exception {
        testAddOnReleasedPartition(ResultPartitionType.PIPELINED);
    }

    @Test
    void testAddOnReleasedBlockingPartition() throws Exception {
        testAddOnReleasedPartition(ResultPartitionType.BLOCKING);
    }

    /**
     * Tests {@link ResultPartition#emitRecord} on a partition which has already been released.
     *
     * @param partitionType the result partition type to set up
     */
    private void testAddOnReleasedPartition(ResultPartitionType partitionType) throws Exception {
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(partitionType);

        try {
            bufferWritingResultPartition.release(null);
            // partitionWriter.emitRecord() should silently drop the given record
            bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
        } finally {
            assertThat(bufferWritingResultPartition.numBuffersOut.getCount()).isEqualTo(1);
            assertThat(bufferWritingResultPartition.numBytesOut.getCount()).isEqualTo(bufferSize);
            // the buffer should be recycled for the result partition has already been released
            assertThat(bufferWritingResultPartition.getBufferPool().bestEffortGetNumOfUsedBuffers())
                    .isZero();
        }
    }

    @Test
    void testAddOnPipelinedPartition() throws Exception {
        testAddOnPartition(ResultPartitionType.PIPELINED);
    }

    @Test
    void testAddOnBlockingPartition() throws Exception {
        testAddOnPartition(ResultPartitionType.BLOCKING);
    }

    /**
     * Tests {@link ResultPartitionManager#createSubpartitionView(ResultPartitionID, int,
     * BufferAvailabilityListener)} would throw a {@link PartitionNotFoundException} if the
     * registered partition was released from manager via {@link ResultPartition#fail(Throwable)}
     * before.
     */
    @Test
    void testCreateSubpartitionOnFailingPartition() throws Exception {
        final ResultPartitionManager manager = new ResultPartitionManager();
        final ResultPartition partition =
                new ResultPartitionBuilder().setResultPartitionManager(manager).build();

        manager.registerResultPartition(partition);

        partition.fail(null);

        verifyCreateSubpartitionViewThrowsException(manager, partition.getPartitionId());
    }

    /**
     * Tests {@link ResultPartition#emitRecord} on a working partition.
     *
     * @param partitionType the result partition type to set up
     */
    private void testAddOnPartition(final ResultPartitionType partitionType) throws Exception {
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(partitionType);

        try {
            // partitionWriter.emitRecord() will allocate a new buffer and copies the record to it
            bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
        } finally {
            assertThat(bufferWritingResultPartition.numBuffersOut.getCount()).isEqualTo(1);
            assertThat(bufferWritingResultPartition.numBytesOut.getCount()).isEqualTo(bufferSize);
            assertThat(bufferWritingResultPartition.getBufferPool().bestEffortGetNumOfUsedBuffers())
                    .isEqualTo(1);
        }
    }

    /**
     * Tests {@link ResultPartition#close()} and {@link ResultPartition#release()} on a working
     * pipelined partition.
     */
    @Test
    void testReleaseMemoryOnPipelinedPartition() throws Exception {
        final int numAllBuffers = 10;
        final NettyShuffleEnvironment network =
                new NettyShuffleEnvironmentBuilder()
                        .setNumNetworkBuffers(numAllBuffers)
                        .setBufferSize(bufferSize)
                        .build();
        final ResultPartition resultPartition =
                createPartition(network, ResultPartitionType.PIPELINED, 1);
        try {
            resultPartition.setup();

            // take all buffers (more than the minimum required)
            for (int i = 0; i < numAllBuffers; ++i) {
                resultPartition.emitRecord(ByteBuffer.allocate(bufferSize - 1), 0);
            }
            assertThat(resultPartition.getBufferPool().getNumberOfAvailableMemorySegments())
                    .isZero();

            resultPartition.close();
            assertThat(resultPartition.getBufferPool().isDestroyed()).isTrue();
            assertThat(network.getNetworkBufferPool().getNumberOfUsedMemorySegments())
                    .isEqualTo(numAllBuffers);

            resultPartition.release();
            assertThat(network.getNetworkBufferPool().getNumberOfUsedMemorySegments()).isZero();
        } finally {
            network.close();
        }
    }

    /** Tests {@link ResultPartition#getAvailableFuture()}. */
    @Test
    void testIsAvailableOrNot() throws IOException {
        final int numAllBuffers = 10;
        final int bufferSize = 1024;
        final NettyShuffleEnvironment network =
                new NettyShuffleEnvironmentBuilder()
                        .setNumNetworkBuffers(numAllBuffers)
                        .setBufferSize(bufferSize)
                        .build();
        final ResultPartition resultPartition =
                createPartition(network, ResultPartitionType.PIPELINED, 1);

        try {
            resultPartition.setup();

            resultPartition.getBufferPool().setNumBuffers(2);

            assertThat(resultPartition.getAvailableFuture()).isDone();

            resultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
            resultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
            assertThat(resultPartition.getAvailableFuture()).isNotDone();
        } finally {
            resultPartition.release();
            network.close();
        }
    }

    @Test
    void testPipelinedPartitionBufferPool() throws Exception {
        testPartitionBufferPool(ResultPartitionType.PIPELINED_BOUNDED);
    }

    @Test
    void testBlockingPartitionBufferPool() throws Exception {
        testPartitionBufferPool(ResultPartitionType.BLOCKING);
    }

    private void testPartitionBufferPool(ResultPartitionType type) throws Exception {
        // setup
        final int networkBuffersPerChannel = 2;
        final int floatingNetworkBuffersPerGate = 8;
        final NetworkBufferPool globalPool = new NetworkBufferPool(20, 1);
        final ResultPartition partition =
                new ResultPartitionBuilder()
                        .setResultPartitionType(type)
                        .setFileChannelManager(fileChannelManager)
                        .setNetworkBuffersPerChannel(networkBuffersPerChannel)
                        .setFloatingNetworkBuffersPerGate(floatingNetworkBuffersPerGate)
                        .setNetworkBufferPool(globalPool)
                        .build();

        try {
            partition.setup();
            BufferPool bufferPool = partition.getBufferPool();
            // verify the amount of buffers in created local pool
            assertThat(bufferPool.getNumberOfRequiredMemorySegments())
                    .isEqualTo(partition.getNumberOfSubpartitions() + 1);
            if (type.isBounded()) {
                final int maxNumBuffers =
                        networkBuffersPerChannel * partition.getNumberOfSubpartitions()
                                + floatingNetworkBuffersPerGate;
                assertThat(bufferPool.getMaxNumberOfMemorySegments()).isEqualTo(maxNumBuffers);
            } else {
                assertThat(bufferPool.getMaxNumberOfMemorySegments()).isEqualTo(Integer.MAX_VALUE);
            }

        } finally {
            // cleanup
            globalPool.destroyAllBufferPools();
            globalPool.destroy();
        }
    }

    private BufferWritingResultPartition createResultPartition(ResultPartitionType partitionType)
            throws IOException {
        NettyShuffleEnvironment network =
                new NettyShuffleEnvironmentBuilder()
                        .setNumNetworkBuffers(10)
                        .setBufferSize(bufferSize)
                        .build();
        ResultPartition resultPartition =
                createPartition(network, fileChannelManager, partitionType, 2);
        resultPartition.setup();
        return (BufferWritingResultPartition) resultPartition;
    }

    @Test
    void testIdleAndBackPressuredTime() throws IOException, InterruptedException {
        // setup
        int bufferSize = 1024;
        NetworkBufferPool globalPool = new NetworkBufferPool(10, bufferSize);
        BufferPool localPool = globalPool.createBufferPool(1, 1, 1, Integer.MAX_VALUE, 0);
        BufferWritingResultPartition resultPartition =
                (BufferWritingResultPartition)
                        new ResultPartitionBuilder().setBufferPoolFactory(() -> localPool).build();
        resultPartition.setup();

        resultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
        ResultSubpartitionView readView =
                resultPartition.createSubpartitionView(0, new NoOpBufferAvailablityListener());
        Buffer buffer = readView.getNextBuffer().buffer();
        assertThat(buffer).isNotNull();

        // back-pressured time is zero when there is buffer available.
        assertThat(resultPartition.getHardBackPressuredTimeMsPerSecond().getValue()).isZero();

        CountDownLatch syncLock = new CountDownLatch(1);
        final Thread requestThread =
                new Thread(
                        () -> {
                            try {
                                // notify that the request thread start to run.
                                syncLock.countDown();
                                // wait for buffer.
                                resultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
                            } catch (Exception e) {
                            }
                        });
        requestThread.start();

        // wait until request thread start to run.
        syncLock.await();

        // wait until request buffer blocking.
        while (!isInBlockingBufferRequest(requestThread.getStackTrace())) {
            Thread.sleep(50);
        }
        // recycle the buffer
        buffer.recycleBuffer();
        requestThread.join();

        assertThat(resultPartition.getHardBackPressuredTimeMsPerSecond().getCount())
                .isGreaterThan(0L);
        assertThat(readView.getNextBuffer().buffer()).isNotNull();
    }

    @Test
    void testFlushBoundedBlockingResultPartition() throws IOException {
        int value = 1024;
        ResultPartition partition = createResultPartition(ResultPartitionType.BLOCKING);

        ByteBuffer record = ByteBuffer.allocate(4);
        record.putInt(value);

        record.rewind();
        partition.emitRecord(record, 0);
        partition.flush(0);

        record.rewind();
        partition.emitRecord(record, 0);

        record.rewind();
        partition.broadcastRecord(record);
        partition.flushAll();

        record.rewind();
        partition.broadcastRecord(record);
        partition.finish();
        record.rewind();

        ResultSubpartitionView readView1 =
                partition.createSubpartitionView(0, new NoOpBufferAvailablityListener());
        for (int i = 0; i < 4; ++i) {
            assertThat(readView1.getNextBuffer().buffer().getNioBufferReadable()).isEqualTo(record);
        }
        assertThat(readView1.getNextBuffer().buffer().isBuffer()).isFalse();
        assertThat(readView1.getNextBuffer()).isNull();

        ResultSubpartitionView readView2 =
                partition.createSubpartitionView(1, new NoOpBufferAvailablityListener());
        for (int i = 0; i < 2; ++i) {
            assertThat(readView2.getNextBuffer().buffer().getNioBufferReadable()).isEqualTo(record);
        }
        assertThat(readView2.getNextBuffer().buffer().isBuffer()).isFalse();
        assertThat(readView2.getNextBuffer()).isNull();
    }

    @Test
    void testEmitRecordWithRecordSpanningMultipleBuffers() throws Exception {
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED);
        PipelinedSubpartition pipelinedSubpartition =
                (PipelinedSubpartition) bufferWritingResultPartition.subpartitions[0];
        int partialLength = bufferSize / 3;

        try {
            // emit the first record, record length = partialLength
            bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(partialLength), 0);
            // emit the second record, record length = bufferSize
            bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
        } finally {
            assertThat(pipelinedSubpartition.getNumberOfQueuedBuffers()).isEqualTo(2);
            assertThat(pipelinedSubpartition.getNextBuffer().getPartialRecordLength()).isZero();
            assertThat(pipelinedSubpartition.getNextBuffer().getPartialRecordLength())
                    .isEqualTo(partialLength);
        }
    }

    @Test
    void testBroadcastRecordWithRecordSpanningMultipleBuffers() throws Exception {
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED);
        int partialLength = bufferSize / 3;

        try {
            // emit the first record, record length = partialLength
            bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(partialLength));
            // emit the second record, record length = bufferSize
            bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(bufferSize));
        } finally {
            for (ResultSubpartition resultSubpartition :
                    bufferWritingResultPartition.subpartitions) {
                PipelinedSubpartition pipelinedSubpartition =
                        (PipelinedSubpartition) resultSubpartition;
                assertThat(pipelinedSubpartition.getNumberOfQueuedBuffers()).isEqualTo(2);
                assertThat(pipelinedSubpartition.getNextBuffer().getPartialRecordLength()).isZero();
                assertThat(pipelinedSubpartition.getNextBuffer().getPartialRecordLength())
                        .isEqualTo(partialLength);
            }
        }
    }

    @Test
    public void testWaitForAllRecordProcessed() throws IOException {
        // Creates a result partition with 2 channels.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED_BOUNDED);

        bufferWritingResultPartition.notifyEndOfData(StopMode.DRAIN);
        CompletableFuture<Void> allRecordsProcessedFuture =
                bufferWritingResultPartition.getAllDataProcessedFuture();
        assertThat(allRecordsProcessedFuture).isNotDone();
        for (ResultSubpartition resultSubpartition : bufferWritingResultPartition.subpartitions) {
            assertThat(resultSubpartition.getTotalNumberOfBuffersUnsafe()).isEqualTo(1);
            Buffer nextBuffer = ((PipelinedSubpartition) resultSubpartition).pollBuffer().buffer();
            assertThat(nextBuffer.isBuffer()).isFalse();
            assertThat(EventSerializer.fromBuffer(nextBuffer, getClass().getClassLoader()))
                    .isEqualTo(new EndOfData(StopMode.DRAIN));
        }

        for (int i = 0; i < bufferWritingResultPartition.subpartitions.length; ++i) {
            ((PipelinedSubpartition) bufferWritingResultPartition.subpartitions[i])
                    .acknowledgeAllDataProcessed();

            if (i < bufferWritingResultPartition.subpartitions.length - 1) {
                assertThat(allRecordsProcessedFuture).isNotDone();
            } else {
                assertThat(allRecordsProcessedFuture).isDone();
                assertThat(allRecordsProcessedFuture).isNotCompletedExceptionally();
            }
        }
    }

    @Test
    void testDifferentBufferSizeForSubpartitions() throws IOException {
        // given: Configured pipelined result with 2 subpartitions.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED_BOUNDED);

        ResultSubpartition[] subpartitions = bufferWritingResultPartition.subpartitions;
        assertThat(subpartitions.length).isEqualTo(2);

        PipelinedSubpartition subpartition0 = (PipelinedSubpartition) subpartitions[0];
        PipelinedSubpartition subpartition1 = (PipelinedSubpartition) subpartitions[1];

        // when: Set the different buffers size.
        subpartition0.bufferSize(10);
        subpartition1.bufferSize(6);

        // and: Add the buffer.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(2), 0);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(10), 0);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(2), 1);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(10), 1);

        // then: The buffer is less or equal to configured.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(10);
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(2);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(6);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(6);

        // when: Reset the buffer size.
        subpartition0.bufferSize(13);
        subpartition1.bufferSize(5);

        // and: Add the buffer.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(12), 0);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(8), 0);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(2), 1);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(7), 1);

        // then: The buffer less or equal to configured.
        // 8 bytes which fitted to the previous unfinished buffer(10 - 2).
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(8);
        // 12 rest bytes which fitted to a new buffer which has 13 bytes.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(12);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(5);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(4);
    }

    @Test
    void testBufferSizeGreaterOrEqualToFirstRecord() throws IOException {
        // given: Configured pipelined result with 2 subpartitions.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED_BOUNDED);

        ResultSubpartition[] subpartitions = bufferWritingResultPartition.subpartitions;
        assertThat(subpartitions).hasSize(2);

        PipelinedSubpartition subpartition0 = (PipelinedSubpartition) subpartitions[0];
        PipelinedSubpartition subpartition1 = (PipelinedSubpartition) subpartitions[1];

        // when: Set the different buffers size.
        subpartition0.bufferSize(10);
        subpartition1.bufferSize(7);

        // and: Add the buffer.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(12), 0);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(111), 1);

        // then: The buffer can not be less than first record.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(12);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(111);
    }

    @Test
    void testDynamicBufferSizeForBroadcast() throws IOException {
        // given: Configured pipelined result with 2 subpartitions.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED_BOUNDED);

        ResultSubpartition[] subpartitions = bufferWritingResultPartition.subpartitions;
        assertThat(subpartitions).hasSize(2);

        PipelinedSubpartition subpartition0 = (PipelinedSubpartition) subpartitions[0];
        PipelinedSubpartition subpartition1 = (PipelinedSubpartition) subpartitions[1];

        // when: Set the different buffers size.
        subpartition0.bufferSize(6);
        subpartition1.bufferSize(10);

        // and: Add the buffer.
        bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(6));

        // then: The buffer less or equal to configured.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(6);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(6);

        // when: Set the different buffers size.
        subpartition0.bufferSize(4);
        subpartition1.bufferSize(12);

        // and: Add the buffer.
        bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(3));
        bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(7));

        // then: The buffer less or equal to configured.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(4);
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(6);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(4);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(6);

        // when: Set the different buffers size.
        subpartition0.bufferSize(8);
        subpartition1.bufferSize(5);

        // and: Add the buffer.
        bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(3));

        // then: The buffer less or equal to configured.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(3);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(3);
    }

    @Test
    void testBufferSizeGreaterOrEqualToFirstBroadcastRecord() throws IOException {
        // given: Configured pipelined result with 2 subpartitions.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED_BOUNDED);

        ResultSubpartition[] subpartitions = bufferWritingResultPartition.subpartitions;

        PipelinedSubpartition subpartition0 = (PipelinedSubpartition) subpartitions[0];
        PipelinedSubpartition subpartition1 = (PipelinedSubpartition) subpartitions[1];

        // when: Set the different buffers size.
        subpartition0.bufferSize(6);
        subpartition1.bufferSize(10);

        // and: Add the buffer.
        bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(31));

        // then: The buffer can not be less than first record.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(31);
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(31);
    }

    @Test
    void testBufferSizeNotChanged() throws IOException {
        // given: Configured pipelined result with 2 subpartitions.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED_BOUNDED);

        ResultSubpartition[] subpartitions = bufferWritingResultPartition.subpartitions;
        assertThat(subpartitions).hasSize(2);

        PipelinedSubpartition subpartition0 = (PipelinedSubpartition) subpartitions[0];
        PipelinedSubpartition subpartition1 = (PipelinedSubpartition) subpartitions[1];

        // when: Set the different buffers size.
        subpartition0.bufferSize(bufferSize + 1);
        subpartition1.bufferSize(Integer.MAX_VALUE);

        // and: Add the buffer.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 1);

        // then: The buffer has initial size because new buffer was greater than max.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(bufferSize);

        // and: The buffer has initial size because new buffer was less than 0.
        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(bufferSize);
    }

    @Test
    void testNumBytesProducedCounterForUnicast() throws IOException {
        testNumBytesProducedCounter(false);
    }

    @Test
    void testNumBytesProducedCounterForBroadcast() throws IOException {
        testNumBytesProducedCounter(true);
    }

    private void testNumBytesProducedCounter(boolean isBroadcast) throws IOException {
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.BLOCKING);

        if (isBroadcast) {
            bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(bufferSize));
            assertThat(bufferWritingResultPartition.numBytesProduced.getCount())
                    .isEqualTo(bufferSize);
            assertThat(bufferWritingResultPartition.numBytesOut.getCount())
                    .isEqualTo(2 * bufferSize);
        } else {
            bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(bufferSize), 0);
            assertThat(bufferWritingResultPartition.numBytesProduced.getCount())
                    .isEqualTo(bufferSize);
            assertThat(bufferWritingResultPartition.numBytesOut.getCount()).isEqualTo(bufferSize);
        }
    }

    @Test
    void testSizeOfQueuedBuffers() throws IOException {
        // given: Configured pipelined result with 2 subpartitions.
        BufferWritingResultPartition bufferWritingResultPartition =
                createResultPartition(ResultPartitionType.PIPELINED);

        ResultSubpartition[] subpartitions = bufferWritingResultPartition.subpartitions;
        assertThat(subpartitions).hasSize(2);

        PipelinedSubpartition subpartition0 = (PipelinedSubpartition) subpartitions[0];
        PipelinedSubpartition subpartition1 = (PipelinedSubpartition) subpartitions[1];

        // and: Set the buffers size.
        subpartition0.bufferSize(10);
        subpartition1.bufferSize(10);

        // when: Emit different records into different subpartitions.
        // Emit the record less than buffer size.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(3), 0);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(3);

        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(3), 1);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(6);

        // Emit the record the equal to buffer size.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(10), 0);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(16);

        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(10), 1);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(26);

        // Broadcast event.
        bufferWritingResultPartition.broadcastEvent(EndOfPartitionEvent.INSTANCE, false);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(34);

        // Emit one more record to the one subpartition.
        bufferWritingResultPartition.emitRecord(ByteBuffer.allocate(5), 0);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(39);

        // Broadcast record.
        bufferWritingResultPartition.broadcastRecord(ByteBuffer.allocate(7));
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(53);

        // when: Poll finished buffers.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(10);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(43);

        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(10);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(33);

        // Poll records which were unfinished because of broadcasting event.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(3);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(30);

        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(3);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(27);

        // Poll the event.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(4);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(23);

        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(4);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(19);

        // Poll the unfinished buffer.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(5);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(14);

        // Poll broadcasted record.
        assertThat(subpartition0.pollBuffer().buffer().getSize()).isEqualTo(7);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(7);

        assertThat(subpartition1.pollBuffer().buffer().getSize()).isEqualTo(7);
        assertThat(bufferWritingResultPartition.getSizeOfQueuedBuffersUnsafe()).isEqualTo(0);
    }
}
