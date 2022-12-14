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
import org.apache.flink.table.planner.plan.nodes.common.CommonIntermediateTableScan
import org.apache.flink.table.planner.plan.nodes.exec.ExecNode
import org.apache.flink.table.planner.plan.schema.IntermediateRelTable

import org.apache.calcite.plan.{RelOptCluster, RelOptTable, RelTraitSet}
import org.apache.calcite.rel.`type`.RelDataType
import org.apache.calcite.rel.RelNode

import java.util

/** Stream physical TableScan that wraps [[IntermediateRelTable]] */
class StreamPhysicalIntermediateTableScan(
    cluster: RelOptCluster,
    traitSet: RelTraitSet,
    table: RelOptTable,
    outputRowType: RelDataType)
  extends CommonIntermediateTableScan(cluster, traitSet, table)
  with StreamPhysicalRel {

  override def requireWatermark: Boolean = false

  override def deriveRowType(): RelDataType = outputRowType

  override def copy(traitSet: RelTraitSet, inputs: util.List[RelNode]): RelNode = {
    new StreamPhysicalIntermediateTableScan(cluster, traitSet, getTable, outputRowType)
  }

  override def translateToExecNode(): ExecNode[_] = {
    throw new TableException(
      "This should not happen. " +
        "\nThis physical rel is the intermediate result of the optimization," +
        " and it can not be translated to ExecNode.")
  }
}
