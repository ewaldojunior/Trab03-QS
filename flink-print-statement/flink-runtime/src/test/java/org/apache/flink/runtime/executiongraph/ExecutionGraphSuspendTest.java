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

package org.apache.flink.runtime.executiongraph;

import org.apache.flink.api.common.JobStatus;
import org.apache.flink.runtime.concurrent.ComponentMainThreadExecutorServiceAdapter;
import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.executiongraph.failover.flip1.TestRestartBackoffTimeStrategy;
import org.apache.flink.runtime.jobgraph.JobGraphTestUtils;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobmanager.slots.TaskManagerGateway;
import org.apache.flink.runtime.scheduler.DefaultSchedulerBuilder;
import org.apache.flink.runtime.scheduler.SchedulerBase;
import org.apache.flink.runtime.scheduler.SchedulerTestingUtils;
import org.apache.flink.runtime.scheduler.TestingPhysicalSlotProvider;
import org.apache.flink.runtime.testtasks.NoOpInvokable;
import org.apache.flink.testutils.TestingUtils;
import org.apache.flink.testutils.executor.TestExecutorResource;
import org.apache.flink.util.TestLogger;
import org.apache.flink.util.concurrent.ManuallyTriggeredScheduledExecutor;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/** Validates that suspending out of various states works correctly. */
public class ExecutionGraphSuspendTest extends TestLogger {

    @ClassRule
    public static final TestExecutorResource<ScheduledExecutorService> EXECUTOR_RESOURCE =
            TestingUtils.defaultExecutorResource();

    /**
     * Going into SUSPENDED out of CREATED should immediately cancel everything and not send out RPC
     * calls.
     */
    @Test
    public void testSuspendedOutOfCreated() throws Exception {
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway();
        final int parallelism = 10;
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        assertEquals(JobStatus.CREATED, eg.getState());

        // suspend

        scheduler.closeAsync();

        assertEquals(JobStatus.SUSPENDED, eg.getState());
        validateAllVerticesInState(eg, ExecutionState.CANCELED);
        validateCancelRpcCalls(gateway, 0);

        ensureCannotLeaveSuspendedState(scheduler, gateway);
    }

    /**
     * Going into SUSPENDED out of DEPLOYING vertices should cancel all vertices once with RPC
     * calls.
     */
    @Test
    public void testSuspendedOutOfDeploying() throws Exception {
        final int parallelism = 10;
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway(parallelism);
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        scheduler.startScheduling();
        assertEquals(JobStatus.RUNNING, eg.getState());
        validateAllVerticesInState(eg, ExecutionState.DEPLOYING);

        // suspend
        scheduler.closeAsync();

        assertEquals(JobStatus.SUSPENDED, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);

        ensureCannotLeaveSuspendedState(scheduler, gateway);
    }

    /**
     * Going into SUSPENDED out of RUNNING vertices should cancel all vertices once with RPC calls.
     */
    @Test
    public void testSuspendedOutOfRunning() throws Exception {
        final int parallelism = 10;
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway(parallelism);
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        scheduler.startScheduling();
        ExecutionGraphTestUtils.switchAllVerticesToRunning(eg);

        assertEquals(JobStatus.RUNNING, eg.getState());
        validateAllVerticesInState(eg, ExecutionState.RUNNING);

        // suspend
        scheduler.closeAsync();

        assertEquals(JobStatus.SUSPENDED, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);

        ensureCannotLeaveSuspendedState(scheduler, gateway);
    }

    /** Suspending from FAILING goes to SUSPENDED and sends no additional RPC calls. */
    @Test
    public void testSuspendedOutOfFailing() throws Exception {
        final int parallelism = 10;
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway(parallelism);
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        scheduler.startScheduling();
        ExecutionGraphTestUtils.switchAllVerticesToRunning(eg);

        scheduler.handleGlobalFailure(new Exception("fail global"));

        assertEquals(JobStatus.FAILING, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);

        // suspend
        scheduler.closeAsync();

        assertEquals(JobStatus.SUSPENDED, eg.getState());
        ensureCannotLeaveSuspendedState(scheduler, gateway);
    }

    /** Suspending from FAILED should do nothing. */
    @Test
    public void testSuspendedOutOfFailed() throws Exception {
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway();
        final int parallelism = 10;
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        scheduler.startScheduling();
        ExecutionGraphTestUtils.switchAllVerticesToRunning(eg);

        scheduler.handleGlobalFailure(new Exception("fail global"));

        assertEquals(JobStatus.FAILING, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);

        ExecutionGraphTestUtils.completeCancellingForAllVertices(eg);
        assertEquals(JobStatus.FAILED, eg.getState());

        // suspend
        scheduler.closeAsync();

        // still in failed state
        assertEquals(JobStatus.FAILED, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);
    }

    /** Suspending from CANCELING goes to SUSPENDED and sends no additional RPC calls. */
    @Test
    public void testSuspendedOutOfCanceling() throws Exception {
        final int parallelism = 10;
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway(parallelism);
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        scheduler.startScheduling();
        ExecutionGraphTestUtils.switchAllVerticesToRunning(eg);

        scheduler.cancel();

        assertEquals(JobStatus.CANCELLING, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);

        // suspend
        scheduler.closeAsync();

        assertEquals(JobStatus.SUSPENDED, eg.getState());

        ensureCannotLeaveSuspendedState(scheduler, gateway);
    }

    /** Suspending from CANCELLED should do nothing. */
    @Test
    public void testSuspendedOutOfCanceled() throws Exception {
        final InteractionsCountingTaskManagerGateway gateway =
                new InteractionsCountingTaskManagerGateway();
        final int parallelism = 10;
        final SchedulerBase scheduler = createScheduler(gateway, parallelism);
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        scheduler.startScheduling();
        ExecutionGraphTestUtils.switchAllVerticesToRunning(eg);

        scheduler.cancel();

        assertEquals(JobStatus.CANCELLING, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);

        ExecutionGraphTestUtils.completeCancellingForAllVertices(eg);
        assertEquals(JobStatus.CANCELED, eg.getTerminationFuture().get());

        // suspend
        scheduler.closeAsync();

        // still in failed state
        assertEquals(JobStatus.CANCELED, eg.getState());
        validateCancelRpcCalls(gateway, parallelism);
    }

    /** Tests that we can suspend a job when in state RESTARTING. */
    @Test
    public void testSuspendWhileRestarting() throws Exception {
        final ManuallyTriggeredScheduledExecutor taskRestartExecutor =
                new ManuallyTriggeredScheduledExecutor();
        final SchedulerBase scheduler =
                new DefaultSchedulerBuilder(
                                JobGraphTestUtils.emptyJobGraph(),
                                ComponentMainThreadExecutorServiceAdapter.forMainThread(),
                                EXECUTOR_RESOURCE.getExecutor())
                        .setRestartBackoffTimeStrategy(
                                new TestRestartBackoffTimeStrategy(true, Long.MAX_VALUE))
                        .setDelayExecutor(taskRestartExecutor)
                        .build();

        scheduler.startScheduling();

        final ExecutionGraph eg = scheduler.getExecutionGraph();

        assertEquals(JobStatus.RUNNING, eg.getState());
        ExecutionGraphTestUtils.switchAllVerticesToRunning(eg);

        scheduler.handleGlobalFailure(new Exception("test"));
        assertEquals(JobStatus.RESTARTING, eg.getState());

        ExecutionGraphTestUtils.completeCancellingForAllVertices(eg);
        assertEquals(JobStatus.RESTARTING, eg.getState());

        scheduler.closeAsync();

        assertEquals(JobStatus.SUSPENDED, eg.getState());

        taskRestartExecutor.triggerScheduledTasks();
        assertEquals(JobStatus.SUSPENDED, eg.getState());
    }

    // ------------------------------------------------------------------------
    //  utilities
    // ------------------------------------------------------------------------

    private static void ensureCannotLeaveSuspendedState(
            SchedulerBase scheduler, InteractionsCountingTaskManagerGateway gateway) {
        final ExecutionGraph eg = scheduler.getExecutionGraph();

        gateway.waitUntilAllTasksAreSubmitted();
        assertEquals(JobStatus.SUSPENDED, eg.getState());
        gateway.resetCounts();

        scheduler.handleGlobalFailure(new Exception("fail"));
        assertEquals(JobStatus.SUSPENDED, eg.getState());
        validateNoInteractions(gateway);

        scheduler.cancel();
        assertEquals(JobStatus.SUSPENDED, eg.getState());
        validateNoInteractions(gateway);

        scheduler.closeAsync();
        assertEquals(JobStatus.SUSPENDED, eg.getState());
        validateNoInteractions(gateway);

        for (ExecutionVertex ev : eg.getAllExecutionVertices()) {
            assertEquals(0, ev.getCurrentExecutionAttempt().getAttemptNumber());
        }
    }

    private static void validateNoInteractions(InteractionsCountingTaskManagerGateway gateway) {
        assertThat(gateway.getInteractionsCount(), is(0));
    }

    private static void validateAllVerticesInState(ExecutionGraph eg, ExecutionState expected) {
        for (ExecutionVertex ev : eg.getAllExecutionVertices()) {
            assertEquals(expected, ev.getCurrentExecutionAttempt().getState());
        }
    }

    private static void validateCancelRpcCalls(
            InteractionsCountingTaskManagerGateway gateway, int num) {
        assertThat(gateway.getCancelTaskCount(), is(num));
    }

    private static SchedulerBase createScheduler(TaskManagerGateway gateway, int parallelism)
            throws Exception {
        final JobVertex vertex = new JobVertex("vertex");
        vertex.setInvokableClass(NoOpInvokable.class);
        vertex.setParallelism(parallelism);

        final SchedulerBase scheduler =
                new DefaultSchedulerBuilder(
                                JobGraphTestUtils.streamingJobGraph(vertex),
                                ComponentMainThreadExecutorServiceAdapter.forMainThread(),
                                EXECUTOR_RESOURCE.getExecutor())
                        .setExecutionSlotAllocatorFactory(
                                SchedulerTestingUtils.newSlotSharingExecutionSlotAllocatorFactory(
                                        TestingPhysicalSlotProvider
                                                .createWithLimitedAmountOfPhysicalSlots(
                                                        parallelism, gateway)))
                        .build();
        return scheduler;
    }
}
