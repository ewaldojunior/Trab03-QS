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

package org.apache.flink.table.runtime.operators.over.frame;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.apache.flink.table.runtime.context.ExecutionContext;
import org.apache.flink.table.runtime.dataview.PerKeyStateDataViewStore;
import org.apache.flink.table.runtime.generated.AggsHandleFunction;
import org.apache.flink.table.runtime.generated.GeneratedAggsHandleFunction;
import org.apache.flink.table.runtime.util.ResettableExternalBuffer;

/**
 * The UnboundedPreceding window frame. See {@link RowUnboundedPrecedingOverFrame} and {@link
 * RangeUnboundedPrecedingOverFrame}.
 */
public abstract class UnboundedPrecedingOverFrame implements OverWindowFrame {

    private GeneratedAggsHandleFunction aggsHandleFunction;

    AggsHandleFunction processor;
    RowData accValue;

    /** An iterator over the input. */
    ResettableExternalBuffer.BufferIterator inputIterator;

    /** The next row from `input`. */
    BinaryRowData nextRow;

    public UnboundedPrecedingOverFrame(GeneratedAggsHandleFunction aggsHandleFunction) {
        this.aggsHandleFunction = aggsHandleFunction;
    }

    @Override
    public void open(ExecutionContext ctx) throws Exception {
        ClassLoader cl = ctx.getRuntimeContext().getUserCodeClassLoader();
        processor = aggsHandleFunction.newInstance(cl);
        processor.open(new PerKeyStateDataViewStore(ctx.getRuntimeContext()));
        this.aggsHandleFunction = null;
    }

    @Override
    public void prepare(ResettableExternalBuffer rows) throws Exception {
        if (inputIterator != null) {
            inputIterator.close();
        }
        inputIterator = rows.newIterator();
        if (inputIterator.advanceNext()) {
            nextRow = inputIterator.getRow().copy();
        }
        processor.setWindowSize(rows.size());
        // reset the accumulators value
        processor.setAccumulators(processor.createAccumulators());
    }
}
