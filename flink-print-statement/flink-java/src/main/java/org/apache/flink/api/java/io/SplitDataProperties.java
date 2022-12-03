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

package org.apache.flink.api.java.io;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.InvalidProgramException;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.common.operators.GenericDataSourceBase;
import org.apache.flink.api.common.operators.Keys;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.common.operators.Ordering;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.operators.DataSource;

import java.util.Arrays;

/**
 * SplitDataProperties define data properties on {@link org.apache.flink.core.io.InputSplit}
 * generated by the {@link org.apache.flink.api.common.io.InputFormat} of a {@link DataSource}.
 *
 * <p>InputSplits are units of input which are distributed among and assigned to parallel data
 * source subtasks. SplitDataProperties can define that the elements which are generated by the
 * associated InputFormat are
 *
 * <ul>
 *   <li>Partitioned on one or more fields across InputSplits, i.e., all elements with the same
 *       (combination of) key(s) are located in the same input split.
 *   <li>Grouped on one or more fields within an InputSplit, i.e., all elements of an input split
 *       that have the same (combination of) key(s) are emitted in a single sequence one after the
 *       other.
 *   <li>Ordered on one or more fields within an InputSplit, i.e., all elements within an input
 *       split are in the defined order.
 * </ul>
 *
 * <p><b>IMPORTANT: SplitDataProperties can improve the execution of a program because certain data
 * reorganization steps such as shuffling or sorting can be avoided. HOWEVER, if SplitDataProperties
 * are not correctly defined, the result of the program might be wrong!</b>
 *
 * @param <T> The type of the DataSource on which the SplitDataProperties are defined.
 * @see org.apache.flink.core.io.InputSplit
 * @see org.apache.flink.api.common.io.InputFormat
 * @see org.apache.flink.api.java.operators.DataSource
 */
@PublicEvolving
public class SplitDataProperties<T> implements GenericDataSourceBase.SplitDataProperties<T> {

    private TypeInformation<T> type;

    private int[] splitPartitionKeys;

    private Partitioner<T> splitPartitioner;

    private int[] splitGroupKeys;

    private Ordering splitOrdering;

    /**
     * Creates SplitDataProperties for the given data types.
     *
     * @param type The data type of the SplitDataProperties.
     */
    public SplitDataProperties(TypeInformation<T> type) {
        this.type = type;
    }

    /**
     * Creates SplitDataProperties for the given data types.
     *
     * @param source The DataSource for which the SplitDataProperties are created.
     */
    public SplitDataProperties(DataSource<T> source) {
        this.type = source.getType();
    }

    /**
     * Defines that data is partitioned across input splits on the fields defined by field
     * positions. All records sharing the same key (combination) must be contained in a single input
     * split.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param partitionFields The field positions of the partitioning keys.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsPartitionedBy(int... partitionFields) {
        return this.splitsPartitionedBy(null, partitionFields);
    }

    /**
     * Defines that data is partitioned using a specific partitioning method across input splits on
     * the fields defined by field positions. All records sharing the same key (combination) must be
     * contained in a single input split.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param partitionMethodId An ID for the method that was used to partition the data across
     *     splits.
     * @param partitionFields The field positions of the partitioning keys.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsPartitionedBy(
            String partitionMethodId, int... partitionFields) {

        if (partitionFields == null) {
            throw new InvalidProgramException("PartitionFields may not be null.");
        } else if (partitionFields.length == 0) {
            throw new InvalidProgramException("PartitionFields may not be empty.");
        }

        this.splitPartitionKeys = getAllFlatKeys(partitionFields);
        if (partitionMethodId != null) {
            this.splitPartitioner = new SourcePartitionerMarker<>(partitionMethodId);
        } else {
            this.splitPartitioner = null;
        }

        return this;
    }

    /**
     * Defines that data is partitioned across input splits on the fields defined by field
     * expressions. Multiple field expressions must be separated by the semicolon ';' character. All
     * records sharing the same key (combination) must be contained in a single input split.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param partitionFields The field expressions of the partitioning keys.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsPartitionedBy(String partitionFields) {
        return this.splitsPartitionedBy(null, partitionFields);
    }

    /**
     * Defines that data is partitioned using an identifiable method across input splits on the
     * fields defined by field expressions. Multiple field expressions must be separated by the
     * semicolon ';' character. All records sharing the same key (combination) must be contained in
     * a single input split.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param partitionMethodId An ID for the method that was used to partition the data across
     *     splits.
     * @param partitionFields The field expressions of the partitioning keys.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsPartitionedBy(
            String partitionMethodId, String partitionFields) {

        if (partitionFields == null) {
            throw new InvalidProgramException("PartitionFields may not be null.");
        }

        String[] partitionKeysA = partitionFields.split(";");
        if (partitionKeysA.length == 0) {
            throw new InvalidProgramException("PartitionFields may not be empty.");
        }

        this.splitPartitionKeys = getAllFlatKeys(partitionKeysA);
        if (partitionMethodId != null) {
            this.splitPartitioner = new SourcePartitionerMarker<>(partitionMethodId);
        } else {
            this.splitPartitioner = null;
        }

        return this;
    }

    /**
     * Defines that the data within an input split is grouped on the fields defined by the field
     * positions. All records sharing the same key (combination) must be subsequently emitted by the
     * input format for each input split.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param groupFields The field positions of the grouping keys.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsGroupedBy(int... groupFields) {

        if (groupFields == null) {
            throw new InvalidProgramException("GroupFields may not be null.");
        } else if (groupFields.length == 0) {
            throw new InvalidProgramException("GroupFields may not be empty.");
        }

        if (this.splitOrdering != null) {
            throw new InvalidProgramException("DataSource may either be grouped or sorted.");
        }

        this.splitGroupKeys = getAllFlatKeys(groupFields);

        return this;
    }

    /**
     * Defines that the data within an input split is grouped on the fields defined by the field
     * expressions. Multiple field expressions must be separated by the semicolon ';' character. All
     * records sharing the same key (combination) must be subsequently emitted by the input format
     * for each input split.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param groupFields The field expressions of the grouping keys.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsGroupedBy(String groupFields) {

        if (groupFields == null) {
            throw new InvalidProgramException("GroupFields may not be null.");
        }

        String[] groupKeysA = groupFields.split(";");
        if (groupKeysA.length == 0) {
            throw new InvalidProgramException("GroupFields may not be empty.");
        }

        if (this.splitOrdering != null) {
            throw new InvalidProgramException("DataSource may either be grouped or sorted.");
        }

        this.splitGroupKeys = getAllFlatKeys(groupKeysA);

        return this;
    }

    /**
     * Defines that the data within an input split is sorted on the fields defined by the field
     * positions in the specified orders. All records of an input split must be emitted by the input
     * format in the defined order.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param orderFields The field positions of the grouping keys.
     * @param orders The orders of the fields.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsOrderedBy(int[] orderFields, Order[] orders) {

        if (orderFields == null || orders == null) {
            throw new InvalidProgramException("OrderFields or Orders may not be null.");
        } else if (orderFields.length == 0) {
            throw new InvalidProgramException("OrderFields may not be empty.");
        } else if (orders.length == 0) {
            throw new InvalidProgramException("Orders may not be empty");
        } else if (orderFields.length != orders.length) {
            throw new InvalidProgramException("Number of OrderFields and Orders must match.");
        }

        if (this.splitGroupKeys != null) {
            throw new InvalidProgramException("DataSource may either be grouped or sorted.");
        }

        this.splitOrdering = new Ordering();

        for (int i = 0; i < orderFields.length; i++) {
            int pos = orderFields[i];
            int[] flatKeys = this.getAllFlatKeys(new int[] {pos});

            for (int key : flatKeys) {
                // check for duplicates
                for (int okey : splitOrdering.getFieldPositions()) {
                    if (key == okey) {
                        throw new InvalidProgramException(
                                "Duplicate field in the field expression " + pos);
                    }
                }
                // append key
                this.splitOrdering.appendOrdering(key, null, orders[i]);
            }
        }
        return this;
    }

    /**
     * Defines that the data within an input split is sorted on the fields defined by the field
     * expressions in the specified orders. Multiple field expressions must be separated by the
     * semicolon ';' character. All records of an input split must be emitted by the input format in
     * the defined order.
     *
     * <p><b> IMPORTANT: Providing wrong information with SplitDataProperties can cause wrong
     * results! </b>
     *
     * @param orderFields The field expressions of the grouping key.
     * @param orders The orders of the fields.
     * @return This SplitDataProperties object.
     */
    public SplitDataProperties<T> splitsOrderedBy(String orderFields, Order[] orders) {

        if (orderFields == null || orders == null) {
            throw new InvalidProgramException("OrderFields or Orders may not be null.");
        }

        String[] orderKeysA = orderFields.split(";");
        if (orderKeysA.length == 0) {
            throw new InvalidProgramException("OrderFields may not be empty.");
        } else if (orders.length == 0) {
            throw new InvalidProgramException("Orders may not be empty");
        } else if (orderKeysA.length != orders.length) {
            throw new InvalidProgramException("Number of OrderFields and Orders must match.");
        }

        if (this.splitGroupKeys != null) {
            throw new InvalidProgramException("DataSource may either be grouped or sorted.");
        }

        this.splitOrdering = new Ordering();

        for (int i = 0; i < orderKeysA.length; i++) {
            String keyExp = orderKeysA[i];
            Keys.ExpressionKeys<T> ek = new Keys.ExpressionKeys<>(keyExp, this.type);
            int[] flatKeys = ek.computeLogicalKeyPositions();

            for (int key : flatKeys) {
                // check for duplicates
                for (int okey : splitOrdering.getFieldPositions()) {
                    if (key == okey) {
                        throw new InvalidProgramException(
                                "Duplicate field in field expression " + keyExp);
                    }
                }
                // append key
                this.splitOrdering.appendOrdering(key, null, orders[i]);
            }
        }
        return this;
    }

    public int[] getSplitPartitionKeys() {
        return this.splitPartitionKeys;
    }

    public Partitioner<T> getSplitPartitioner() {
        return this.splitPartitioner;
    }

    public int[] getSplitGroupKeys() {
        return this.splitGroupKeys;
    }

    public Ordering getSplitOrder() {
        return this.splitOrdering;
    }

    /////////////////////// FLAT FIELD EXTRACTION METHODS

    private int[] getAllFlatKeys(String[] fieldExpressions) {

        int[] allKeys = null;

        for (String keyExp : fieldExpressions) {
            Keys.ExpressionKeys<T> ek = new Keys.ExpressionKeys<>(keyExp, this.type);
            int[] flatKeys = ek.computeLogicalKeyPositions();

            if (allKeys == null) {
                allKeys = flatKeys;
            } else {
                // check for duplicates
                for (int key1 : flatKeys) {
                    for (int key2 : allKeys) {
                        if (key1 == key2) {
                            throw new InvalidProgramException(
                                    "Duplicate fields in field expression " + keyExp);
                        }
                    }
                }
                // append flat keys
                int oldLength = allKeys.length;
                int newLength = oldLength + flatKeys.length;
                allKeys = Arrays.copyOf(allKeys, newLength);
                System.arraycopy(flatKeys, 0, allKeys, oldLength, flatKeys.length);
            }
        }

        return allKeys;
    }

    private int[] getAllFlatKeys(int[] fieldPositions) {
        Keys.ExpressionKeys<T> ek = new Keys.ExpressionKeys<>(fieldPositions, this.type);
        return ek.computeLogicalKeyPositions();
    }

    /**
     * A custom partitioner to mark compatible split partitionings.
     *
     * @param <T> The type of the partitioned data.
     */
    public static class SourcePartitionerMarker<T> implements Partitioner<T> {
        private static final long serialVersionUID = -8554223652384442571L;

        String partitionMarker;

        public SourcePartitionerMarker(String partitionMarker) {
            this.partitionMarker = partitionMarker;
        }

        @Override
        public int partition(T key, int numPartitions) {
            throw new UnsupportedOperationException(
                    "The SourcePartitionerMarker is only used as a marker for compatible partitioning. "
                            + "It must not be invoked.");
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SourcePartitionerMarker) {
                return this.partitionMarker.equals(
                        ((SourcePartitionerMarker<?>) o).partitionMarker);
            } else {
                return false;
            }
        }
    }
}
