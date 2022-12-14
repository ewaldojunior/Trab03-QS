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

package org.apache.flink.runtime.persistence;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.util.FlinkException;

/**
 * {@code PossibleInconsistentStateException} represents errors that might have lead to an
 * inconsistent state within the HA resources.
 */
public class PossibleInconsistentStateException extends FlinkException {

    private static final long serialVersionUID = 364105635349022882L;

    @VisibleForTesting
    public PossibleInconsistentStateException() {
        super("The system might be in an inconsistent state.");
    }

    public PossibleInconsistentStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public PossibleInconsistentStateException(Throwable cause) {
        super(cause);
    }
}
