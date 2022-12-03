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

package org.apache.flink.runtime.rest;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.io.network.netty.OutboundChannelHandlerFactory;
import org.apache.flink.util.ConfigurationException;

import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandler;

import java.util.Optional;

/** Test outbound channel handler factory. */
public class Prio1OutboundChannelHandlerFactory implements OutboundChannelHandlerFactory {
    @Override
    public int priority() {
        return 1;
    }

    @Override
    public Optional<ChannelHandler> createHandler(Configuration configuration)
            throws ConfigurationException {
        return Optional.empty();
    }
}
