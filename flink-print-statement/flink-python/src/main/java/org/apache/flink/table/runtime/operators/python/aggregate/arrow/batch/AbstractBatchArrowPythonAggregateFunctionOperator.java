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

package org.apache.flink.table.runtime.operators.python.aggregate.arrow.batch;

import org.apache.flink.annotation.Internal;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.data.binary.BinaryRowDataUtil;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.python.PythonFunctionInfo;
import org.apache.flink.table.runtime.generated.GeneratedProjection;
import org.apache.flink.table.runtime.generated.Projection;
import org.apache.flink.table.runtime.operators.python.aggregate.arrow.AbstractArrowPythonAggregateFunctionOperator;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Preconditions;

/** The Abstract class of Batch Arrow Aggregate Operator for Pandas {@link AggregateFunction}. */
@Internal
abstract class AbstractBatchArrowPythonAggregateFunctionOperator
        extends AbstractArrowPythonAggregateFunctionOperator {

    private static final long serialVersionUID = 1L;

    private final GeneratedProjection groupKeyGeneratedProjection;
    private final GeneratedProjection groupSetGeneratedProjection;

    /** Last group key value. */
    transient BinaryRowData lastGroupKey;

    /** Last group set value. */
    transient BinaryRowData lastGroupSet;

    /** The Projection which projects the group key fields from the input row. */
    transient Projection<RowData, BinaryRowData> groupKeyProjection;

    /**
     * The Projection which projects the group set fields (group key and aux group key) from the
     * input row.
     */
    transient Projection<RowData, BinaryRowData> groupSetProjection;

    AbstractBatchArrowPythonAggregateFunctionOperator(
            Configuration config,
            PythonFunctionInfo[] pandasAggFunctions,
            RowType inputType,
            RowType udfInputType,
            RowType udfOutputType,
            GeneratedProjection inputGeneratedProjection,
            GeneratedProjection groupKeyGeneratedProjection,
            GeneratedProjection groupSetGeneratedProjection) {
        super(
                config,
                pandasAggFunctions,
                inputType,
                udfInputType,
                udfOutputType,
                inputGeneratedProjection);
        this.groupKeyGeneratedProjection = Preconditions.checkNotNull(groupKeyGeneratedProjection);
        this.groupSetGeneratedProjection = Preconditions.checkNotNull(groupSetGeneratedProjection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void open() throws Exception {
        super.open();
        groupKeyProjection =
                groupKeyGeneratedProjection.newInstance(
                        Thread.currentThread().getContextClassLoader());
        groupSetProjection =
                groupSetGeneratedProjection.newInstance(
                        Thread.currentThread().getContextClassLoader());
        lastGroupKey = null;
        lastGroupSet = null;
    }

    @Override
    public void endInput() throws Exception {
        invokeCurrentBatch();
        super.endInput();
    }

    @Override
    public void finish() throws Exception {
        invokeCurrentBatch();
        super.finish();
    }

    protected abstract void invokeCurrentBatch() throws Exception;

    boolean isNewKey(BinaryRowData currentKey) {
        return lastGroupKey == null
                || (lastGroupKey.getSizeInBytes() != currentKey.getSizeInBytes())
                || !(BinaryRowDataUtil.byteArrayEquals(
                        currentKey.getSegments()[0].getHeapMemory(),
                        lastGroupKey.getSegments()[0].getHeapMemory(),
                        currentKey.getSizeInBytes()));
    }
}
