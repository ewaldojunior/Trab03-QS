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

package org.apache.flink.runtime.heartbeat;

import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.util.concurrent.FutureUtils;

import java.util.concurrent.CompletableFuture;

/**
 * The sender implementation of {@link HeartbeatTarget}, which mutes the {@link
 * HeartbeatTarget#receiveHeartbeat(ResourceID, I)}. The extender only has to care about the sending
 * logic.
 *
 * @param <I> Type of the payload which is sent to the heartbeat target
 */
public abstract class HeartbeatSender<I> implements HeartbeatTarget<I> {

    @Override
    public final CompletableFuture<Void> receiveHeartbeat(
            ResourceID heartbeatOrigin, I heartbeatPayload) {
        return FutureUtils.unsupportedOperationFuture();
    }
}
