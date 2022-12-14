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

package org.apache.flink.table.types.inference.strategies;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.inference.CallContext;
import org.apache.flink.table.types.inference.TypeStrategy;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/** Type strategy that returns a {@link DataTypes#ROW()} with fields types equal to input types. */
@Internal
class RowTypeStrategy implements TypeStrategy {

    @Override
    public Optional<DataType> inferType(CallContext callContext) {
        List<DataType> argumentDataTypes = callContext.getArgumentDataTypes();
        DataTypes.Field[] fields =
                IntStream.range(0, argumentDataTypes.size())
                        .mapToObj(idx -> DataTypes.FIELD("f" + idx, argumentDataTypes.get(idx)))
                        .toArray(DataTypes.Field[]::new);

        return Optional.of(DataTypes.ROW(fields).notNull());
    }
}
