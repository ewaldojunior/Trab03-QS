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
# test pyflink shell environment
from pyflink.table.expressions import lit
from pyflink.shell import s_env, st_env, DataTypes
from pyflink.table.schema import Schema
from pyflink.table.table_descriptor import TableDescriptor, FormatDescriptor

import tempfile
import os
import shutil

sink_path = tempfile.gettempdir() + '/batch.csv'
if os.path.exists(sink_path):
    if os.path.isfile(sink_path):
        os.remove(sink_path)
    else:
        shutil.rmtree(sink_path)
s_env.set_parallelism(1)
t = st_env.from_elements([(1, 'hi', 'hello'), (2, 'hi', 'hello')], ['a', 'b', 'c'])

st_env.create_temporary_table("csv_sink", TableDescriptor.for_connector("filesystem")
    .schema(Schema.new_builder()
        .column("a", DataTypes.BIGINT())
        .column("b", DataTypes.STRING())
        .column("c", DataTypes.STRING())
        .build())
    .option("path", sink_path)
    .format(FormatDescriptor.for_format("csv")
        .option("field-delimiter", ",")
        .build())
    .build())

t.select(t.a + lit(1), t.b, t.c).execute_insert("csv_sink").wait()

with open(os.path.join(sink_path, os.listdir(sink_path)[0]), 'r') as f:
    lines = f.read()
    assert lines == '2,hi,hello\n' + '3,hi,hello\n'

print('pip_test_code.py success!')
