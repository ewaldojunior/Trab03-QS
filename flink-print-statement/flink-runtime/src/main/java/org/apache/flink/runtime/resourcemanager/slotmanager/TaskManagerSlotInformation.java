/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.resourcemanager.slotmanager;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.clusterframework.types.AllocationID;
import org.apache.flink.runtime.clusterframework.types.ResourceProfile;
import org.apache.flink.runtime.clusterframework.types.SlotID;
import org.apache.flink.runtime.instance.InstanceID;
import org.apache.flink.runtime.resourcemanager.registration.TaskExecutorConnection;

import javax.annotation.Nullable;

/** Basic information about a TaskManager slot. */
public interface TaskManagerSlotInformation {

    SlotID getSlotId();

    @Nullable
    AllocationID getAllocationId();

    @Nullable
    JobID getJobId();

    SlotState getState();

    InstanceID getInstanceId();

    TaskExecutorConnection getTaskManagerConnection();

    /**
     * Returns true if the required {@link ResourceProfile} can be fulfilled by this slot.
     *
     * @param required resources
     * @return true if the this slot can fulfill the resource requirements
     */
    default boolean isMatchingRequirement(ResourceProfile required) {
        return getResourceProfile().isMatching(required);
    }

    /**
     * Get resource profile of this slot.
     *
     * @return resource profile of this slot
     */
    ResourceProfile getResourceProfile();
}
