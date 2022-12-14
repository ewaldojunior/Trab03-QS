################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
#################################################################################

import glob
import os
import unittest
from py4j.java_gateway import java_import

from pyflink.find_flink_home import _find_flink_source_root
from pyflink.java_gateway import get_gateway
from pyflink.table.sinks import TableSink
from pyflink.table.types import _to_java_data_type
from pyflink.util import java_utils


class TestTableSink(TableSink):
    """
    Base class for test table sink.
    """

    _inited = False

    def __init__(self, j_table_sink):
        super(TestTableSink, self).__init__(j_table_sink)

    @classmethod
    def _ensure_initialized(cls):
        if TestTableSink._inited:
            return

        FLINK_SOURCE_ROOT_DIR = _find_flink_source_root()
        filename_pattern = (
            "flink-python/target/flink-python*-tests.jar")
        if not glob.glob(os.path.join(FLINK_SOURCE_ROOT_DIR, filename_pattern)):
            raise unittest.SkipTest(
                "'flink-python*-tests.jar' is not available. Will skip the related tests.")

        gateway = get_gateway()
        java_import(gateway.jvm, "org.apache.flink.table.utils.TestingSinks$TestAppendingSink")
        java_import(gateway.jvm, "org.apache.flink.table.utils.TestingSinks$RowCollector")

        TestTableSink._inited = True


class TestAppendSink(TestTableSink):
    """
    A test append table sink.
    """

    def __init__(self, field_names, field_types):
        TestTableSink._ensure_initialized()
        gateway = get_gateway()
        j_field_names = java_utils.to_jarray(gateway.jvm.String, field_names)
        j_field_types = java_utils.to_jarray(
            gateway.jvm.DataType,
            [_to_java_data_type(field_type) for field_type in field_types])
        super(TestAppendSink, self).__init__(gateway.jvm.org.apache.flink.table.utils.TestingSinks
                                             .TestAppendingSink(j_field_names, j_field_types))


class TestRetractSink(TestTableSink):
    """
    A test retract table sink.
    """

    def __init__(self, field_names, field_types):
        TestTableSink._ensure_initialized()
        gateway = get_gateway()
        j_field_names = java_utils.to_jarray(gateway.jvm.String, field_names)
        j_field_types = java_utils.to_jarray(
            gateway.jvm.DataType,
            [_to_java_data_type(field_type) for field_type in field_types])
        super(TestRetractSink, self).__init__(gateway.jvm.org.apache.flink.table.utils.TestingSinks
                                              .TestAppendingSink(j_field_names, j_field_types))


class TestUpsertSink(TestTableSink):
    """
    A test upsert table sink.
    """

    def __init__(self, field_names, field_types, keys, is_append_only):
        TestTableSink._ensure_initialized()

        gateway = get_gateway()
        j_keys = gateway.new_array(gateway.jvm.String, len(keys))
        for i in range(0, len(keys)):
            j_keys[i] = keys[i]

        super(TestUpsertSink, self).__init__(
            gateway.jvm.TestUpsertSink(j_keys, is_append_only), field_names, field_types)


def results():
    """
    Retrieves the results from an append table sink.
    """
    return retract_results()


def retract_results():
    """
    Retrieves the results from a retract table sink.
    """
    gateway = get_gateway()
    results = gateway.jvm.org.apache.flink.table.utils.TestingSinks.RowCollector.getAndClearValues()
    return gateway.jvm\
        .org.apache.flink.table.utils.TestingSinks.RowCollector.retractResults(results)


def upsert_results(keys):
    """
    Retrieves the results from an upsert table sink.
    """
    gateway = get_gateway()
    j_keys = gateway.new_array(gateway.jvm.int, len(keys))
    for i in range(0, len(keys)):
        j_keys[i] = keys[i]

    results = gateway.jvm.RowCollector.getAndClearValues()
    return gateway.jvm.RowCollector.upsertResults(results, j_keys)
