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
package org.apache.flink.table.planner.plan.nodes.physical.stream

import org.apache.flink.table.api.TableException
import org.apache.flink.table.planner.calcite.FlinkTypeFactory
import org.apache.flink.table.planner.plan.nodes.exec.{ExecNode, InputProperty}
import org.apache.flink.table.planner.plan.nodes.exec.stream.StreamExecPythonCorrelate
import org.apache.flink.table.planner.plan.nodes.logical.FlinkLogicalTableFunctionScan
import org.apache.flink.table.planner.plan.utils.JoinTypeUtil
import org.apache.flink.table.planner.utils.ShortcutUtils.unwrapTableConfig

import org.apache.calcite.plan.{RelOptCluster, RelTraitSet}
import org.apache.calcite.rel.`type`.RelDataType
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.core.JoinRelType
import org.apache.calcite.rex.{RexCall, RexNode}

/** Flink RelNode which matches along with join a python user defined table function. */
class StreamPhysicalPythonCorrelate(
    cluster: RelOptCluster,
    traitSet: RelTraitSet,
    inputRel: RelNode,
    scan: FlinkLogicalTableFunctionScan,
    condition: Option[RexNode],
    outputRowType: RelDataType,
    joinType: JoinRelType)
  extends StreamPhysicalCorrelateBase(
    cluster,
    traitSet,
    inputRel,
    scan,
    condition,
    outputRowType,
    joinType) {

  def copy(traitSet: RelTraitSet, newChild: RelNode, outputType: RelDataType): RelNode = {
    new StreamPhysicalPythonCorrelate(
      cluster,
      traitSet,
      newChild,
      scan,
      condition,
      outputType,
      joinType)
  }

  override def translateToExecNode(): ExecNode[_] = {
    if (condition.orNull != null) {
      if (joinType == JoinRelType.LEFT) {
        throw new TableException(
          "Currently Python correlate does not support conditions" +
            " in left join.")
      }
      throw new TableException("The condition of StreamPhysicalPythonCorrelate should be null.")
    }

    new StreamExecPythonCorrelate(
      unwrapTableConfig(this),
      JoinTypeUtil.getFlinkJoinType(joinType),
      scan.getCall.asInstanceOf[RexCall],
      InputProperty.DEFAULT,
      FlinkTypeFactory.toLogicalRowType(getRowType),
      getRelDetailedDescription)
  }
}
