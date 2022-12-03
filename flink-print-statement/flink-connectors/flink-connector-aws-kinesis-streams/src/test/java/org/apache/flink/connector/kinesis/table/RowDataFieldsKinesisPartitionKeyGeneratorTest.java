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

package org.apache.flink.connector.kinesis.table;

import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.factories.TableOptionsBuilder;
import org.apache.flink.table.factories.TestFormatFactory;
import org.apache.flink.table.types.logical.RowType;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.apache.flink.connector.kinesis.table.RowDataFieldsKinesisPartitionKeyGenerator.MAX_PARTITION_KEY_LENGTH;
import static org.apache.flink.table.utils.EncodingUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Test for {@link RowDataFieldsKinesisPartitionKeyGenerator}. */
class RowDataFieldsKinesisPartitionKeyGeneratorTest {

    /** Table name to use for the tests. */
    private static final String TABLE_NAME = "click_stream";

    /** Row type to use for the tests. */
    private static final RowType ROW_TYPE =
            (RowType)
                    DataTypes.ROW(
                                    DataTypes.FIELD("time", DataTypes.TIMESTAMP(3)),
                                    DataTypes.FIELD("ip", DataTypes.VARCHAR(16)),
                                    DataTypes.FIELD("route", DataTypes.STRING()),
                                    DataTypes.FIELD("date", DataTypes.STRING()),
                                    DataTypes.FIELD("year", DataTypes.STRING()),
                                    DataTypes.FIELD("month", DataTypes.STRING()),
                                    DataTypes.FIELD("day", DataTypes.STRING()))
                            .getLogicalType();

    /** A list of field delimiters to use in the tests. */
    private static final List<String> FIELD_DELIMITERS = Arrays.asList("", "|", ",", "--");

    /** A {@code PARTITION BY(date, ip)} clause to use for the positive tests. */
    private static final List<String> PARTITION_BY_DATE_AND_IP = Arrays.asList("date", "ip");

    /** A {@code PARTITION BY(year, month, day)} clause to use for the positive tests. */
    private static final List<String> PARTITION_BY_DATE = Arrays.asList("year", "month", "day");

    /** A {@code PARTITION BY(route)} clause to use for the positive tests. */
    private static final List<String> PARTITION_BY_ROUTE = Collections.singletonList("route");

    /**
     * Some not-so-random {@link LocalDateTime} instances to use for sample {@link RowData} elements
     * in the tests.
     */
    private static final List<LocalDateTime> DATE_TIMES =
            Arrays.asList(
                    LocalDateTime.of(2014, 10, 22, 12, 0),
                    LocalDateTime.of(2015, 11, 13, 10, 0),
                    LocalDateTime.of(2015, 12, 14, 14, 0),
                    LocalDateTime.of(2018, 10, 31, 15, 0));

    /** A default IP to use for sample {@link RowData} elements in the tests. */
    private static final String IP = "255.255.255.255";

    // --------------------------------------------------------------------------------------------
    // Positive tests
    // --------------------------------------------------------------------------------------------

    @Test
    void testGoodPartitioner() {
        for (String delimiter : FIELD_DELIMITERS) {
            RowDataFieldsKinesisPartitionKeyGenerator partitioner =
                    new RowDataFieldsKinesisPartitionKeyGenerator(
                            ROW_TYPE, PARTITION_BY_DATE_AND_IP, delimiter);

            for (LocalDateTime time : DATE_TIMES) {
                String expectedKey = String.join(delimiter, String.valueOf(days(time)), IP);
                String actualKey = partitioner.apply(createElement(time, IP));

                assertThat(actualKey).isEqualTo(expectedKey);
            }
        }
    }

    @Test
    void testGoodPartitionerExceedingMaxLength() {
        RowDataFieldsKinesisPartitionKeyGenerator partitioner =
                new RowDataFieldsKinesisPartitionKeyGenerator(ROW_TYPE, PARTITION_BY_ROUTE);

        String ip = "255.255.255.255";
        String route = "http://www.very-" + repeat("long-", 50) + "address.com/home";
        String expectedKey = route.substring(0, MAX_PARTITION_KEY_LENGTH);

        for (LocalDateTime time : DATE_TIMES) {
            String actualKey = partitioner.apply(createElement(time, ip, route));
            assertThat(actualKey).isEqualTo(expectedKey);
        }
    }

    @Test
    void testGoodPartitionerWithStaticPrefix() {
        // fixed prefix
        String year = String.valueOf(year(DATE_TIMES.get(0)));
        String month = String.valueOf(monthOfYear(DATE_TIMES.get(0)));

        for (String delimiter : FIELD_DELIMITERS) {
            RowDataFieldsKinesisPartitionKeyGenerator partitioner =
                    new RowDataFieldsKinesisPartitionKeyGenerator(
                            ROW_TYPE, PARTITION_BY_DATE, delimiter);

            partitioner.setStaticFields(
                    new HashMap<String, String>() {
                        {
                            put("year", year);
                            put("month", month);
                        }
                    });

            for (LocalDateTime time : DATE_TIMES) {
                String day = String.valueOf(dayOfMonth(time));
                String expectedKey = String.join(delimiter, year, month, day);
                String actualKey = partitioner.apply(createElement(time, IP));

                assertThat(actualKey).isEqualTo(expectedKey);
            }
        }
    }

    @Test
    void testGoodPartitionerWithStaticSuffix() {
        // fixed suffix
        String month = String.valueOf(monthOfYear(DATE_TIMES.get(0)));
        String day = String.valueOf(dayOfMonth(DATE_TIMES.get(0)));

        for (String delimiter : FIELD_DELIMITERS) {
            RowDataFieldsKinesisPartitionKeyGenerator partitioner =
                    new RowDataFieldsKinesisPartitionKeyGenerator(
                            ROW_TYPE, PARTITION_BY_DATE, delimiter);

            partitioner.setStaticFields(
                    new HashMap<String, String>() {
                        {
                            put("month", month);
                            put("day", day);
                        }
                    });

            for (LocalDateTime time : DATE_TIMES) {
                String year = String.valueOf(year(time));
                String expectedKey = String.join(delimiter, year, month, day);
                String actualKey = partitioner.apply(createElement(time, IP));

                assertThat(actualKey).isEqualTo(expectedKey);
            }
        }
    }

    @Test
    void testGoodPartitionerWithStaticInfix() {
        // fixed infix
        String month = String.valueOf(monthOfYear(DATE_TIMES.get(0)));

        for (String delimiter : FIELD_DELIMITERS) {
            RowDataFieldsKinesisPartitionKeyGenerator partitioner =
                    new RowDataFieldsKinesisPartitionKeyGenerator(
                            ROW_TYPE, PARTITION_BY_DATE, delimiter);

            partitioner.setStaticFields(
                    new HashMap<String, String>() {
                        {
                            put("month", month);
                        }
                    });

            for (LocalDateTime time : DATE_TIMES) {
                String year = String.valueOf(year(time));
                String day = String.valueOf(dayOfMonth(time));
                String expectedKey = String.join(delimiter, year, month, day);
                String actualKey = partitioner.apply(createElement(time, IP));

                assertThat(actualKey).isEqualTo(expectedKey);
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Negative tests
    // --------------------------------------------------------------------------------------------

    @Test
    void testBadPartitionerWithEmptyPrefix() {
        assertThatThrownBy(
                        () ->
                                new RowDataFieldsKinesisPartitionKeyGenerator(
                                        ROW_TYPE, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Cannot create a RowDataFieldsKinesisPartitioner for a non-partitioned table");
    }

    @Test
    void testBadPartitionerWithDuplicatePartitionKeys() {
        assertThatThrownBy(
                        () ->
                                new RowDataFieldsKinesisPartitionKeyGenerator(
                                        ROW_TYPE, Arrays.asList("ip", "ip")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The sequence of partition keys cannot contain duplicates");
    }

    @Test
    void testBadPartitionerWithBadFieldFieldNames() {
        assertThatThrownBy(
                        () ->
                                new RowDataFieldsKinesisPartitionKeyGenerator(
                                        ROW_TYPE, Arrays.asList("ip", "abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "The following partition keys are not present in the table: abc");
    }

    @Test
    void testBadPartitionerWithBadFieldFieldTypes() {
        assertThatThrownBy(
                        () ->
                                new RowDataFieldsKinesisPartitionKeyGenerator(
                                        ROW_TYPE, Arrays.asList("time", "ip")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "The following partition keys have types that are not supported by Kinesis: time");
    }

    // --------------------------------------------------------------------------------------------
    // Utilities
    // --------------------------------------------------------------------------------------------

    private RowData createElement(LocalDateTime time, String ip) {
        return createElement(time, ip, "https://flink.apache.org/home");
    }

    private RowData createElement(LocalDateTime time, String ip, String route) {
        GenericRowData element = new GenericRowData(ROW_TYPE.getFieldCount());
        element.setField(0, TimestampData.fromLocalDateTime(time));
        element.setField(1, StringData.fromString(ip));
        element.setField(2, StringData.fromString(route));
        element.setField(3, StringData.fromString(String.valueOf(days(time))));
        element.setField(4, StringData.fromString(String.valueOf(year(time))));
        element.setField(5, StringData.fromString(String.valueOf(monthOfYear(time))));
        element.setField(6, StringData.fromString(String.valueOf(dayOfMonth(time))));
        return element;
    }

    private int days(LocalDateTime time) {
        return (int) ChronoUnit.DAYS.between(LocalDate.ofEpochDay(0), time);
    }

    private int year(LocalDateTime time) {
        return time.get(ChronoField.YEAR);
    }

    private int monthOfYear(LocalDateTime time) {
        return time.get(ChronoField.MONTH_OF_YEAR);
    }

    private int dayOfMonth(LocalDateTime time) {
        return time.get(ChronoField.DAY_OF_MONTH);
    }

    private TableOptionsBuilder defaultTableOptions() {
        String connector = KinesisDynamicTableSinkFactory.IDENTIFIER;
        String format = TestFormatFactory.IDENTIFIER;
        return new TableOptionsBuilder(connector, format)
                // default table options
                .withTableOption(KinesisConnectorOptions.STREAM, TABLE_NAME)
                .withTableOption("properties.aws.region", "us-west-2")
                // default format options
                .withFormatOption(TestFormatFactory.DELIMITER, ",");
    }
}
