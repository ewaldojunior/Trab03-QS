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
package org.apache.flink.table.planner.plan.nodes.physical.batch

import org.apache.flink.table.functions.UserDefinedFunction
import org.apache.flink.table.planner.calcite.FlinkTypeFactory
import org.apache.flink.table.planner.plan.`trait`.{FlinkRelDistribution, FlinkRelDistributionTraitDef}
import org.apache.flink.table.planner.plan.nodes.exec.{ExecNode, InputProperty}
import org.apache.flink.table.planner.plan.nodes.exec.batch.BatchExecSortAggregate
import org.apache.flink.table.planner.plan.utils.{FlinkRelOptUtil, RelExplainUtil}
import org.apache.flink.table.planner.utils.ShortcutUtils.unwrapTableConfig

import org.apache.calcite.plan.{RelOptCluster, RelOptRule, RelTraitSet}
import org.apache.calcite.rel._
import org.apache.calcite.rel.`type`.RelDataType
import org.apache.calcite.rel.RelDistribution.Type
import org.apache.calcite.rel.core.AggregateCall
import org.apache.calcite.util.ImmutableIntList

import java.util

import scala.collection.JavaConversions._

/**
 * Batch physical RelNode for local sort-based aggregate operator.
 *
 * @see
 *   [[BatchPhysicalGroupAggregateBase]] for more info.
 */
class BatchPhysicalLocalSortAggregate(
    cluster: RelOptCluster,
    traitSet: RelTraitSet,
    inputRel: RelNode,
    outputRowType: RelDataType,
    inputRowType: RelDataType,
    grouping: Array[Int],
    auxGrouping: Array[Int],
    aggCallToAggFunction: Seq[(AggregateCall, UserDefinedFunction)])
  extends BatchPhysicalSortAggregateBase(
    cluster,
    traitSet,
    inputRel,
    outputRowType,
    grouping,
    auxGrouping,
    aggCallToAggFunction,
    isMerge = false,
    isFinal = false) {

  override def copy(traitSet: RelTraitSet, inputs: util.List[RelNode]): RelNode = {
    new BatchPhysicalLocalSortAggregate(
      cluster,
      traitSet,
      inputs.get(0),
      outputRowType,
      inputRowType,
      grouping,
      auxGrouping,
      aggCallToAggFunction)
  }

  override def explainTerms(pw: RelWriter): RelWriter = {
    super
      .explainTerms(pw)
      .itemIf("groupBy", RelExplainUtil.fieldToString(grouping, inputRowType), grouping.nonEmpty)
      .itemIf(
        "auxGrouping",
        RelExplainUtil.fieldToString(auxGrouping, inputRowType),
        auxGrouping.nonEmpty)
      .item(
        "select",
        RelExplainUtil.groupAggregationToString(
          inputRowType,
          outputRowType,
          grouping,
          auxGrouping,
          aggCallToAggFunction,
          isMerge = false,
          isGlobal = false))
  }

  override def satisfyTraits(requiredTraitSet: RelTraitSet): Option[RelNode] = {
    // Does not to try to satisfy requirement by localAgg's input if enforce to use two-stage agg.
    if (isEnforceTwoStageAgg) {
      return None
    }
    val groupCount = grouping.length
    val requiredDistribution = requiredTraitSet.getTrait(FlinkRelDistributionTraitDef.INSTANCE)
    val canSatisfy = requiredDistribution.getType match {
      case Type.HASH_DISTRIBUTED | Type.RANGE_DISTRIBUTED =>
        // Cannot satisfy distribution if keys are not group keys of agg
        requiredDistribution.getKeys.forall(_ < groupCount)
      case _ => false
    }
    if (!canSatisfy) {
      return None
    }

    val keys = requiredDistribution.getKeys.map(grouping(_))
    val inputRequiredDistributionKeys = ImmutableIntList.of(keys: _*)
    val inputRequiredDistribution = requiredDistribution.getType match {
      case Type.HASH_DISTRIBUTED =>
        FlinkRelDistribution.hash(inputRequiredDistributionKeys, requiredDistribution.requireStrict)
      case Type.RANGE_DISTRIBUTED =>
        FlinkRelDistribution.range(inputRequiredDistributionKeys)
    }
    val requiredCollation = requiredTraitSet.getTrait(RelCollationTraitDef.INSTANCE)
    val providedFieldCollations = (0 until groupCount).map(FlinkRelOptUtil.ofRelFieldCollation)
    val providedCollation = RelCollations.of(providedFieldCollations)
    val newProvidedTraits = if (providedCollation.satisfies(requiredCollation)) {
      getTraitSet.replace(requiredDistribution).replace(requiredCollation)
    } else {
      getTraitSet.replace(requiredDistribution)
    }
    val inputRequiredTraits = getInput.getTraitSet.replace(inputRequiredDistribution)
    val newInput = RelOptRule.convert(getInput, inputRequiredTraits)
    Some(copy(newProvidedTraits, Seq(newInput)))
  }

  override def translateToExecNode(): ExecNode[_] = {
    new BatchExecSortAggregate(
      unwrapTableConfig(this),
      grouping,
      auxGrouping,
      getAggCallList.toArray,
      FlinkTypeFactory.toLogicalRowType(inputRowType),
      false, // isMerge is always false
      false, // isFinal is always false
      getInputProperty,
      FlinkTypeFactory.toLogicalRowType(getRowType),
      getRelDetailedDescription)
  }

  private def getInputProperty: InputProperty = {
    if (grouping.length == 0) {
      InputProperty.builder().damBehavior(InputProperty.DamBehavior.END_INPUT).build()
    } else {
      InputProperty.DEFAULT
    }
  }
}
