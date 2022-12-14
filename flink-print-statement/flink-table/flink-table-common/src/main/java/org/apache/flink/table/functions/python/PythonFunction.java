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

package org.apache.flink.table.functions.python;

import org.apache.flink.annotation.Internal;

import java.io.Serializable;

/**
 * The base interface of a wrapper of a Python function. It wraps the serialized Python function and
 * the execution environment.
 */
@Internal
public interface PythonFunction extends Serializable {

    /** Returns the serialized representation of the user-defined python function. */
    byte[] getSerializedPythonFunction();

    /** Returns the Python execution environment. */
    PythonEnv getPythonEnv();

    /** Returns the kind of the user-defined python function. */
    default PythonFunctionKind getPythonFunctionKind() {
        return PythonFunctionKind.GENERAL;
    }

    /** Returns Whether the Python function takes row as input instead of each columns of a row. */
    default boolean takesRowAsInput() {
        return false;
    }
}
