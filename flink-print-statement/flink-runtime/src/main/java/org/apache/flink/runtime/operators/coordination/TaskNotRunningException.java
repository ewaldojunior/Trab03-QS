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

package org.apache.flink.runtime.operators.coordination;

import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.util.FlinkException;

/** An exception indicating that a target task is not running. */
public class TaskNotRunningException extends FlinkException {

    private static final long serialVersionUID = 1L;

    public TaskNotRunningException(String message) {
        super(message);
    }

    public TaskNotRunningException(String taskDescription, ExecutionState state) {
        super(taskDescription + " is currently not RUNNING but in state " + state);
    }
}
