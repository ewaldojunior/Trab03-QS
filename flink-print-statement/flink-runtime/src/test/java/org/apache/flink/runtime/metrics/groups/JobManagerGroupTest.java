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

package org.apache.flink.runtime.metrics.groups;

import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MetricOptions;
import org.apache.flink.runtime.metrics.MetricRegistryImpl;
import org.apache.flink.runtime.metrics.MetricRegistryTestUtils;
import org.apache.flink.runtime.metrics.dump.QueryScopeInfo;
import org.apache.flink.runtime.metrics.util.DummyCharacterFilter;
import org.apache.flink.util.TestLogger;
import org.apache.flink.util.concurrent.Executors;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for the {@link JobManagerMetricGroup}. */
public class JobManagerGroupTest extends TestLogger {

    // ------------------------------------------------------------------------
    //  adding and removing jobs
    // ------------------------------------------------------------------------

    @Test
    public void addAndRemoveJobs() throws Exception {
        MetricRegistryImpl registry =
                new MetricRegistryImpl(
                        MetricRegistryTestUtils.defaultMetricRegistryConfiguration());
        final JobManagerMetricGroup group =
                JobManagerMetricGroup.createJobManagerMetricGroup(registry, "localhost");

        final JobID jid1 = new JobID();
        final JobID jid2 = new JobID();

        final String jobName1 = "testjob";
        final String jobName2 = "anotherJob";

        JobManagerJobMetricGroup jmJobGroup11 = group.addJob(jid1, jobName1);
        JobManagerJobMetricGroup jmJobGroup12 = group.addJob(jid1, jobName1);
        JobManagerJobMetricGroup jmJobGroup21 = group.addJob(jid2, jobName2);

        assertEquals(jmJobGroup11, jmJobGroup12);

        assertEquals(2, group.numRegisteredJobMetricGroups());

        group.localCleanupAsync(jid1, Executors.directExecutor()).join();

        assertTrue(jmJobGroup11.isClosed());
        assertEquals(1, group.numRegisteredJobMetricGroups());

        group.localCleanupAsync(jid2, Executors.directExecutor()).join();

        assertTrue(jmJobGroup21.isClosed());
        assertEquals(0, group.numRegisteredJobMetricGroups());

        registry.closeAsync().get();
    }

    @Test
    public void testCloseClosesAll() throws Exception {
        MetricRegistryImpl registry =
                new MetricRegistryImpl(
                        MetricRegistryTestUtils.defaultMetricRegistryConfiguration());
        final JobManagerMetricGroup group =
                JobManagerMetricGroup.createJobManagerMetricGroup(registry, "localhost");

        final JobID jid1 = new JobID();
        final JobID jid2 = new JobID();

        final String jobName1 = "testjob";
        final String jobName2 = "anotherJob";

        JobManagerJobMetricGroup jmJobGroup11 = group.addJob(jid1, jobName1);
        JobManagerJobMetricGroup jmJobGroup21 = group.addJob(jid2, jobName2);

        group.close();

        assertTrue(jmJobGroup11.isClosed());
        assertTrue(jmJobGroup21.isClosed());

        registry.closeAsync().get();
    }

    // ------------------------------------------------------------------------
    //  scope name tests
    // ------------------------------------------------------------------------

    @Test
    public void testGenerateScopeDefault() throws Exception {
        MetricRegistryImpl registry =
                new MetricRegistryImpl(
                        MetricRegistryTestUtils.defaultMetricRegistryConfiguration());
        JobManagerMetricGroup group =
                JobManagerMetricGroup.createJobManagerMetricGroup(registry, "localhost");

        assertArrayEquals(new String[] {"localhost", "jobmanager"}, group.getScopeComponents());
        assertEquals("localhost.jobmanager.name", group.getMetricIdentifier("name"));

        registry.closeAsync().get();
    }

    @Test
    public void testGenerateScopeCustom() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setString(MetricOptions.SCOPE_NAMING_JM, "constant.<host>.foo.<host>");
        MetricRegistryImpl registry =
                new MetricRegistryImpl(MetricRegistryTestUtils.fromConfiguration(cfg));

        JobManagerMetricGroup group =
                JobManagerMetricGroup.createJobManagerMetricGroup(registry, "host");

        assertArrayEquals(
                new String[] {"constant", "host", "foo", "host"}, group.getScopeComponents());
        assertEquals("constant.host.foo.host.name", group.getMetricIdentifier("name"));

        registry.closeAsync().get();
    }

    @Test
    public void testCreateQueryServiceMetricInfo() {
        MetricRegistryImpl registry =
                new MetricRegistryImpl(
                        MetricRegistryTestUtils.defaultMetricRegistryConfiguration());
        JobManagerMetricGroup jm =
                JobManagerMetricGroup.createJobManagerMetricGroup(registry, "host");

        QueryScopeInfo.JobManagerQueryScopeInfo info =
                jm.createQueryServiceMetricInfo(new DummyCharacterFilter());
        assertEquals("", info.scope);
    }
}
