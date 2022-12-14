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

package org.apache.flink.table.runtime.operators.python.scalar;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.fnexecution.v1.FlinkFnApi;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.python.PythonFunctionInfo;
import org.apache.flink.table.runtime.generated.GeneratedProjection;
import org.apache.flink.table.runtime.typeutils.PythonTypeUtils;
import org.apache.flink.table.types.logical.RowType;

import java.io.IOException;

import static org.apache.flink.python.util.ProtoUtils.createFlattenRowTypeCoderInfoDescriptorProto;
import static org.apache.flink.python.util.ProtoUtils.createRowTypeCoderInfoDescriptorProto;

/** The Python {@link ScalarFunction} operator. */
@Internal
public class PythonScalarFunctionOperator extends AbstractPythonScalarFunctionOperator {

    private static final long serialVersionUID = 1L;

    /** The TypeSerializer for udf execution results. */
    private transient TypeSerializer<RowData> udfOutputTypeSerializer;

    /** The TypeSerializer for udf input elements. */
    private transient TypeSerializer<RowData> udfInputTypeSerializer;

    public PythonScalarFunctionOperator(
            Configuration config,
            PythonFunctionInfo[] scalarFunctions,
            RowType inputType,
            RowType udfInputType,
            RowType udfOutputType,
            GeneratedProjection udfInputGeneratedProjection,
            GeneratedProjection forwardedFieldGeneratedProjection) {
        super(
                config,
                scalarFunctions,
                inputType,
                udfInputType,
                udfOutputType,
                udfInputGeneratedProjection,
                forwardedFieldGeneratedProjection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void open() throws Exception {
        super.open();
        udfInputTypeSerializer = PythonTypeUtils.toInternalSerializer(udfInputType);
        udfOutputTypeSerializer = PythonTypeUtils.toInternalSerializer(udfOutputType);
    }

    @Override
    public FlinkFnApi.CoderInfoDescriptor createInputCoderInfoDescriptor(RowType runnerInputType) {
        for (PythonFunctionInfo pythonFunctionInfo : scalarFunctions) {
            if (pythonFunctionInfo.getPythonFunction().takesRowAsInput()) {
                return createRowTypeCoderInfoDescriptorProto(
                        runnerInputType, FlinkFnApi.CoderInfoDescriptor.Mode.MULTIPLE, false);
            }
        }
        return createFlattenRowTypeCoderInfoDescriptorProto(
                runnerInputType, FlinkFnApi.CoderInfoDescriptor.Mode.MULTIPLE, false);
    }

    @Override
    public FlinkFnApi.CoderInfoDescriptor createOutputCoderInfoDescriptor(RowType runnerOutType) {
        return createFlattenRowTypeCoderInfoDescriptorProto(
                runnerOutType, FlinkFnApi.CoderInfoDescriptor.Mode.SINGLE, false);
    }

    @Override
    public void processElementInternal(RowData value) throws Exception {
        udfInputTypeSerializer.serialize(getFunctionInput(value), baosWrapper);
        pythonFunctionRunner.process(baos.toByteArray());
        baos.reset();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void emitResult(Tuple3<String, byte[], Integer> resultTuple) throws IOException {
        byte[] rawUdfResult = resultTuple.f1;
        int length = resultTuple.f2;
        RowData input = forwardedInputQueue.poll();
        reuseJoinedRow.setRowKind(input.getRowKind());
        bais.setBuffer(rawUdfResult, 0, length);
        RowData udfResult = udfOutputTypeSerializer.deserialize(baisWrapper);
        rowDataWrapper.collect(reuseJoinedRow.replace(input, udfResult));
    }
}
