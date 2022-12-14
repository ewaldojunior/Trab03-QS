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

package org.apache.flink.streaming.connectors.kafka.internals;

import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.metrics.groups.UnregisteredMetricsGroup;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.connectors.kafka.testutils.TestSourceContext;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPeriodicWatermarksAdapter;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPunctuatedWatermarksAdapter;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.streaming.runtime.tasks.TestProcessingTimeService;
import org.apache.flink.util.SerializedValue;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for the watermarking behaviour of {@link AbstractFetcher}. */
@SuppressWarnings("serial")
@RunWith(Enclosed.class)
public class AbstractFetcherWatermarksTest {

    /** Tests with watermark generators that have a periodic nature. */
    @RunWith(Parameterized.class)
    public static class PeriodicWatermarksSuite {

        @Parameterized.Parameters
        public static Collection<WatermarkStrategy<Long>> getParams() {
            return Arrays.asList(
                    new AssignerWithPeriodicWatermarksAdapter.Strategy<>(
                            new PeriodicTestExtractor()),
                    WatermarkStrategy.forGenerator((ctx) -> new PeriodicTestWatermarkGenerator())
                            .withTimestampAssigner((event, previousTimestamp) -> event));
        }

        @Parameterized.Parameter public WatermarkStrategy<Long> testWmStrategy;

        @Test
        public void testPeriodicWatermarks() throws Exception {
            final String testTopic = "test topic name";
            Map<KafkaTopicPartition, Long> originalPartitions = new HashMap<>();
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 7),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 13),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 21),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);

            TestSourceContext<Long> sourceContext = new TestSourceContext<>();

            TestProcessingTimeService processingTimeService = new TestProcessingTimeService();

            TestFetcher<Long> fetcher =
                    new TestFetcher<>(
                            sourceContext,
                            originalPartitions,
                            new SerializedValue<>(testWmStrategy),
                            processingTimeService,
                            10);

            final KafkaTopicPartitionState<Long, Object> part1 =
                    fetcher.subscribedPartitionStates().get(0);
            final KafkaTopicPartitionState<Long, Object> part2 =
                    fetcher.subscribedPartitionStates().get(1);
            final KafkaTopicPartitionState<Long, Object> part3 =
                    fetcher.subscribedPartitionStates().get(2);

            // elements generate a watermark if the timestamp is a multiple of three

            // elements for partition 1
            emitRecord(fetcher, 1L, part1, 1L);
            emitRecord(fetcher, 1L, part1, 1L);
            emitRecord(fetcher, 2L, part1, 2L);
            emitRecord(fetcher, 3L, part1, 3L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(3L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(3L);

            // elements for partition 2
            emitRecord(fetcher, 12L, part2, 1L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(12L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(12L);

            // elements for partition 3
            emitRecord(fetcher, 101L, part3, 1L);
            emitRecord(fetcher, 102L, part3, 2L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(102L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(102L);

            processingTimeService.setCurrentTime(10);

            // now, we should have a watermark (this blocks until the periodic thread emitted the
            // watermark)
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(3L);

            // advance partition 3
            emitRecord(fetcher, 1003L, part3, 3L);
            emitRecord(fetcher, 1004L, part3, 4L);
            emitRecord(fetcher, 1005L, part3, 5L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(1005L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(1005L);

            // advance partition 1 beyond partition 2 - this bumps the watermark
            emitRecord(fetcher, 30L, part1, 4L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(30L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(30L);

            processingTimeService.setCurrentTime(20);

            // this blocks until the periodic thread emitted the watermark
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(12L);

            // advance partition 2 again - this bumps the watermark
            emitRecord(fetcher, 13L, part2, 2L);
            emitRecord(fetcher, 14L, part2, 3L);
            emitRecord(fetcher, 15L, part2, 3L);

            processingTimeService.setCurrentTime(30);
            // this blocks until the periodic thread emitted the watermark
            long watermarkTs = sourceContext.getLatestWatermark().getTimestamp();
            assertThat(watermarkTs >= 13L && watermarkTs <= 15L).isTrue();
        }

        @Test
        public void testSkipCorruptedRecordWithPeriodicWatermarks() throws Exception {
            final String testTopic = "test topic name";
            Map<KafkaTopicPartition, Long> originalPartitions = new HashMap<>();
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 1),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);

            TestSourceContext<Long> sourceContext = new TestSourceContext<>();

            TestProcessingTimeService processingTimeProvider = new TestProcessingTimeService();

            TestFetcher<Long> fetcher =
                    new TestFetcher<>(
                            sourceContext,
                            originalPartitions,
                            new SerializedValue<>(testWmStrategy),
                            processingTimeProvider,
                            10);

            final KafkaTopicPartitionState<Long, Object> partitionStateHolder =
                    fetcher.subscribedPartitionStates().get(0);

            // elements generate a watermark if the timestamp is a multiple of three
            emitRecord(fetcher, 1L, partitionStateHolder, 1L);
            emitRecord(fetcher, 2L, partitionStateHolder, 2L);
            emitRecord(fetcher, 3L, partitionStateHolder, 3L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(3L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(3L);
            assertThat(partitionStateHolder.getOffset()).isEqualTo(3L);

            // advance timer for watermark emitting
            processingTimeProvider.setCurrentTime(10L);
            assertThat(sourceContext.hasWatermark()).isTrue();
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(3L);

            // emit no records
            fetcher.emitRecordsWithTimestamps(
                    emptyQueue(), partitionStateHolder, 4L, Long.MIN_VALUE);

            // no elements should have been collected
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(3L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(3L);
            // the offset in state still should have advanced
            assertThat(partitionStateHolder.getOffset()).isEqualTo(4L);

            // no watermarks should be collected
            processingTimeProvider.setCurrentTime(20L);
            assertThat(sourceContext.hasWatermark()).isFalse();
        }

        @Test
        public void testPeriodicWatermarksWithNoSubscribedPartitionsShouldYieldNoWatermarks()
                throws Exception {
            final String testTopic = "test topic name";
            Map<KafkaTopicPartition, Long> originalPartitions = new HashMap<>();

            TestSourceContext<Long> sourceContext = new TestSourceContext<>();

            TestProcessingTimeService processingTimeProvider = new TestProcessingTimeService();

            TestFetcher<Long> fetcher =
                    new TestFetcher<>(
                            sourceContext,
                            originalPartitions,
                            new SerializedValue<>(testWmStrategy),
                            processingTimeProvider,
                            10);

            processingTimeProvider.setCurrentTime(10);
            // no partitions; when the periodic watermark emitter fires, no watermark should be
            // emitted
            assertThat(sourceContext.hasWatermark()).isFalse();

            // counter-test that when the fetcher does actually have partitions,
            // when the periodic watermark emitter fires again, a watermark really is emitted
            fetcher.addDiscoveredPartitions(
                    Collections.singletonList(new KafkaTopicPartition(testTopic, 0)));
            emitRecord(fetcher, 100L, fetcher.subscribedPartitionStates().get(0), 3L);
            processingTimeProvider.setCurrentTime(20);
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(100);
        }
    }

    /** Tests with watermark generators that have a punctuated nature. */
    public static class PunctuatedWatermarksSuite {

        @Test
        public void testSkipCorruptedRecordWithPunctuatedWatermarks() throws Exception {
            final String testTopic = "test topic name";
            Map<KafkaTopicPartition, Long> originalPartitions = new HashMap<>();
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 1),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);

            TestSourceContext<Long> sourceContext = new TestSourceContext<>();

            TestProcessingTimeService processingTimeProvider = new TestProcessingTimeService();

            AssignerWithPunctuatedWatermarksAdapter.Strategy<Long> testWmStrategy =
                    new AssignerWithPunctuatedWatermarksAdapter.Strategy<>(
                            new PunctuatedTestExtractor());

            TestFetcher<Long> fetcher =
                    new TestFetcher<>(
                            sourceContext,
                            originalPartitions,
                            new SerializedValue<>(testWmStrategy),
                            processingTimeProvider,
                            0);

            final KafkaTopicPartitionState<Long, Object> partitionStateHolder =
                    fetcher.subscribedPartitionStates().get(0);

            // elements generate a watermark if the timestamp is a multiple of three
            emitRecord(fetcher, 1L, partitionStateHolder, 1L);
            emitRecord(fetcher, 2L, partitionStateHolder, 2L);
            emitRecord(fetcher, 3L, partitionStateHolder, 3L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(3L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(3L);
            assertThat(sourceContext.hasWatermark()).isTrue();
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(3L);
            assertThat(partitionStateHolder.getOffset()).isEqualTo(3L);

            // emit no records
            fetcher.emitRecordsWithTimestamps(emptyQueue(), partitionStateHolder, 4L, -1L);

            // no elements or watermarks should have been collected
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(3L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(3L);
            assertThat(sourceContext.hasWatermark()).isFalse();
            // the offset in state still should have advanced
            assertThat(partitionStateHolder.getOffset()).isEqualTo(4L);
        }

        @Test
        public void testPunctuatedWatermarks() throws Exception {
            final String testTopic = "test topic name";
            Map<KafkaTopicPartition, Long> originalPartitions = new HashMap<>();
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 7),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 13),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);
            originalPartitions.put(
                    new KafkaTopicPartition(testTopic, 21),
                    KafkaTopicPartitionStateSentinel.LATEST_OFFSET);

            TestSourceContext<Long> sourceContext = new TestSourceContext<>();

            TestProcessingTimeService processingTimeProvider = new TestProcessingTimeService();

            AssignerWithPunctuatedWatermarksAdapter.Strategy<Long> testWmStrategy =
                    new AssignerWithPunctuatedWatermarksAdapter.Strategy<>(
                            new PunctuatedTestExtractor());

            TestFetcher<Long> fetcher =
                    new TestFetcher<>(
                            sourceContext,
                            originalPartitions,
                            new SerializedValue<>(testWmStrategy),
                            processingTimeProvider,
                            0);

            final KafkaTopicPartitionState<Long, Object> part1 =
                    fetcher.subscribedPartitionStates().get(0);
            final KafkaTopicPartitionState<Long, Object> part2 =
                    fetcher.subscribedPartitionStates().get(1);
            final KafkaTopicPartitionState<Long, Object> part3 =
                    fetcher.subscribedPartitionStates().get(2);

            // elements generate a watermark if the timestamp is a multiple of three

            // elements for partition 1
            emitRecords(fetcher, Arrays.asList(1L, 2L), part1, 1L);
            emitRecord(fetcher, 2L, part1, 2L);
            emitRecords(fetcher, Arrays.asList(2L, 3L), part1, 3L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(3L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(3L);
            assertThat(sourceContext.hasWatermark()).isFalse();

            // elements for partition 2
            emitRecord(fetcher, 12L, part2, 1L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(12L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(12L);
            assertThat(sourceContext.hasWatermark()).isFalse();

            // elements for partition 3
            emitRecord(fetcher, 101L, part3, 1L);
            emitRecord(fetcher, 102L, part3, 2L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(102L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(102L);

            // now, we should have a watermark
            assertThat(sourceContext.hasWatermark()).isTrue();
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(3L);

            // advance partition 3
            emitRecord(fetcher, 1003L, part3, 3L);
            emitRecord(fetcher, 1004L, part3, 4L);
            emitRecord(fetcher, 1005L, part3, 5L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(1005L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(1005L);

            // advance partition 1 beyond partition 2 - this bumps the watermark
            emitRecord(fetcher, 30L, part1, 4L);
            assertThat(sourceContext.getLatestElement().getValue().longValue()).isEqualTo(30L);
            assertThat(sourceContext.getLatestElement().getTimestamp()).isEqualTo(30L);
            assertThat(sourceContext.hasWatermark()).isTrue();
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(12L);

            // advance partition 2 again - this bumps the watermark
            emitRecord(fetcher, 13L, part2, 2L);
            assertThat(sourceContext.hasWatermark()).isFalse();
            emitRecord(fetcher, 14L, part2, 3L);
            assertThat(sourceContext.hasWatermark()).isFalse();
            emitRecord(fetcher, 15L, part2, 3L);
            assertThat(sourceContext.hasWatermark()).isTrue();
            assertThat(sourceContext.getLatestWatermark().getTimestamp()).isEqualTo(15L);
        }
    }

    private static final class TestFetcher<T> extends AbstractFetcher<T, Object> {
        TestFetcher(
                SourceContext<T> sourceContext,
                Map<KafkaTopicPartition, Long> assignedPartitionsWithStartOffsets,
                SerializedValue<WatermarkStrategy<T>> watermarkStrategy,
                ProcessingTimeService processingTimeProvider,
                long autoWatermarkInterval)
                throws Exception {
            super(
                    sourceContext,
                    assignedPartitionsWithStartOffsets,
                    watermarkStrategy,
                    processingTimeProvider,
                    autoWatermarkInterval,
                    TestFetcher.class.getClassLoader(),
                    new UnregisteredMetricsGroup(),
                    false);
        }

        public void runFetchLoop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancel() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void doCommitInternalOffsetsToKafka(
                Map<KafkaTopicPartition, Long> offsets,
                @Nonnull KafkaCommitCallback commitCallback) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Object createKafkaPartitionHandle(KafkaTopicPartition partition) {
            return new Object();
        }
    }

    private static <T, KPH> void emitRecord(
            AbstractFetcher<T, KPH> fetcher,
            T record,
            KafkaTopicPartitionState<T, KPH> partitionState,
            long offset) {
        ArrayDeque<T> recordQueue = new ArrayDeque<>();
        recordQueue.add(record);

        fetcher.emitRecordsWithTimestamps(recordQueue, partitionState, offset, Long.MIN_VALUE);
    }

    private static <T, KPH> void emitRecords(
            AbstractFetcher<T, KPH> fetcher,
            List<T> records,
            KafkaTopicPartitionState<T, KPH> partitionState,
            long offset) {
        ArrayDeque<T> recordQueue = new ArrayDeque<>(records);

        fetcher.emitRecordsWithTimestamps(recordQueue, partitionState, offset, Long.MIN_VALUE);
    }

    private static <T> Queue<T> emptyQueue() {
        return new ArrayDeque<>();
    }

    @SuppressWarnings("deprecation")
    private static class PeriodicTestExtractor implements AssignerWithPeriodicWatermarks<Long> {

        private volatile long maxTimestamp = Long.MIN_VALUE;

        @Override
        public long extractTimestamp(Long element, long previousElementTimestamp) {
            maxTimestamp = Math.max(maxTimestamp, element);
            return element;
        }

        @Nullable
        @Override
        public Watermark getCurrentWatermark() {
            return new Watermark(maxTimestamp);
        }
    }

    @SuppressWarnings("deprecation")
    private static class PunctuatedTestExtractor implements AssignerWithPunctuatedWatermarks<Long> {

        @Override
        public long extractTimestamp(Long element, long previousElementTimestamp) {
            return element;
        }

        @Nullable
        @Override
        public Watermark checkAndGetNextWatermark(Long lastElement, long extractedTimestamp) {
            return extractedTimestamp % 3 == 0 ? new Watermark(extractedTimestamp) : null;
        }
    }

    private static class PeriodicTestWatermarkGenerator implements WatermarkGenerator<Long> {

        private volatile long maxTimestamp = Long.MIN_VALUE;

        @Override
        public void onEvent(Long event, long eventTimestamp, WatermarkOutput output) {
            maxTimestamp = Math.max(maxTimestamp, event);
        }

        @Override
        public void onPeriodicEmit(WatermarkOutput output) {
            output.emitWatermark(new org.apache.flink.api.common.eventtime.Watermark(maxTimestamp));
        }
    }
}
