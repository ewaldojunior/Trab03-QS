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

package org.apache.flink.runtime.taskexecutor.slot;

import org.apache.flink.api.common.JobID;
import org.apache.flink.core.testutils.OneShotLatch;
import org.apache.flink.runtime.clusterframework.types.AllocationID;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;

import java.util.concurrent.CompletableFuture;

import static org.apache.flink.runtime.executiongraph.ExecutionGraphTestUtils.createExecutionAttemptId;

class TestingTaskSlotPayload implements TaskSlotPayload {
    private final JobID jobId;
    private final ExecutionAttemptID executionAttemptID;
    private final AllocationID allocationID;
    private final CompletableFuture<Void> terminationFuture = new CompletableFuture<>();
    private final OneShotLatch failLatch = new OneShotLatch();

    TestingTaskSlotPayload() {
        this(new JobID(), createExecutionAttemptId(), new AllocationID());
    }

    TestingTaskSlotPayload(
            JobID jobId, ExecutionAttemptID executionAttemptID, AllocationID allocationID) {
        this.jobId = jobId;
        this.executionAttemptID = executionAttemptID;
        this.allocationID = allocationID;
    }

    @Override
    public JobID getJobID() {
        return jobId;
    }

    @Override
    public ExecutionAttemptID getExecutionId() {
        return executionAttemptID;
    }

    @Override
    public AllocationID getAllocationId() {
        return allocationID;
    }

    @Override
    public CompletableFuture<Void> getTerminationFuture() {
        return terminationFuture;
    }

    @Override
    public void failExternally(Throwable cause) {
        failLatch.trigger();
    }

    void waitForFailure() throws InterruptedException {
        failLatch.await();
    }

    TestingTaskSlotPayload terminate() {
        terminationFuture.complete(null);
        return this;
    }
}
