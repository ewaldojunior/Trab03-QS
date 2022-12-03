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
################################################################################
import filecmp
import os

from pyflink.gen_protos import generate_proto_files
from pyflink.testing.test_case_utils import PyFlinkTestCase


class FlinkFnExecutionTests(PyFlinkTestCase):
    """
    Tests whether flink_fn_exeution_pb2.py is synced with flink-fn-execution.proto.
    """

    flink_fn_execution_pb2_file_name = "flink_fn_execution_pb2.py"
    gen_protos_script = "gen_protos.py"
    flink_fn_execution_proto_file_name = "flink-fn-execution.proto"

    def test_flink_fn_execution_pb2_synced(self):
        generate_proto_files('True', self.tempdir)
        expected = os.path.join(self.tempdir, self.flink_fn_execution_pb2_file_name)
        actual = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..',
                              self.flink_fn_execution_pb2_file_name)
        self.assertTrue(filecmp.cmp(expected, actual),
                        'File %s should be re-generated by executing %s as %s has changed.'
                        % (self.flink_fn_execution_pb2_file_name,
                           self.gen_protos_script,
                           self.flink_fn_execution_proto_file_name))

    def test_state_ttl_config_proto(self):
        from pyflink.datastream.state import StateTtlConfig
        from pyflink.common.time import Time
        state_ttl_config = StateTtlConfig \
            .new_builder(Time.milliseconds(1000)) \
            .set_update_type(StateTtlConfig.UpdateType.OnCreateAndWrite) \
            .set_state_visibility(StateTtlConfig.StateVisibility.NeverReturnExpired) \
            .cleanup_full_snapshot() \
            .cleanup_incrementally(10, True) \
            .cleanup_in_rocksdb_compact_filter(1000) \
            .build()
        state_ttl_config_proto = state_ttl_config._to_proto()
        state_ttl_config = StateTtlConfig._from_proto(state_ttl_config_proto)
        self.assertEqual(state_ttl_config.get_ttl(), Time.milliseconds(1000))
        self.assertEqual(
            state_ttl_config.get_update_type(), StateTtlConfig.UpdateType.OnCreateAndWrite)
        self.assertEqual(
            state_ttl_config.get_state_visibility(),
            StateTtlConfig.StateVisibility.NeverReturnExpired)
        self.assertEqual(
            state_ttl_config.get_ttl_time_characteristic(),
            StateTtlConfig.TtlTimeCharacteristic.ProcessingTime)

        cleanup_strategies = state_ttl_config.get_cleanup_strategies()
        self.assertTrue(cleanup_strategies.is_cleanup_in_background())
        self.assertTrue(cleanup_strategies.in_full_snapshot())

        incremental_cleanup_strategy = cleanup_strategies.get_incremental_cleanup_strategy()
        self.assertEqual(incremental_cleanup_strategy.get_cleanup_size(), 10)
        self.assertTrue(incremental_cleanup_strategy.run_cleanup_for_every_record())

        rocksdb_compact_filter_cleanup_strategy = \
            cleanup_strategies.get_rocksdb_compact_filter_cleanup_strategy()
        self.assertEqual(
            rocksdb_compact_filter_cleanup_strategy.get_query_time_after_num_entries(), 1000)
