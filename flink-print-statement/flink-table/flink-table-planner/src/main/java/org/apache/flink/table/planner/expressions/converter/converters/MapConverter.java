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

package org.apache.flink.table.planner.expressions.converter.converters;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.expressions.CallExpression;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.BuiltInFunctionDefinitions;
import org.apache.flink.table.planner.expressions.converter.CallExpressionConvertRule;
import org.apache.flink.table.planner.functions.sql.FlinkSqlOperatorTable;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.List;

import static org.apache.flink.table.planner.expressions.converter.ExpressionConverter.toRexNodes;

/** Conversion for {@link BuiltInFunctionDefinitions#MAP}. */
@Internal
class MapConverter extends CustomizedConverter {
    @Override
    public RexNode convert(CallExpression call, CallExpressionConvertRule.ConvertContext context) {
        List<Expression> children = call.getChildren();
        checkArgument(call, !children.isEmpty() && children.size() % 2 == 0);
        List<RexNode> childrenRexNode = toRexNodes(context, children);
        RelDataType mapType =
                context.getTypeFactory()
                        .createFieldTypeFromLogicalType(call.getOutputDataType().getLogicalType());
        return context.getRelBuilder()
                .getRexBuilder()
                .makeCall(mapType, FlinkSqlOperatorTable.MAP_VALUE_CONSTRUCTOR, childrenRexNode);
    }
}
