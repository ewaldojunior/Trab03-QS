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
import json

from pyflink.testing.test_case_utils import PyFlinkStreamTableTestCase
from pyflink.table.explain_detail import ExplainDetail


class StreamTableExplainTests(PyFlinkStreamTableTestCase):

    def test_explain(self):
        t = self.t_env.from_elements([(1, 'Hi', 'Hello')], ['a', 'b', 'c'])
        result = t.group_by(t.c).select(t.a.sum, t.c.alias('b')).explain(
            ExplainDetail.CHANGELOG_MODE)

        assert isinstance(result, str)

        result = t.group_by(t.c).select(t.a.sum, t.c.alias('b')).explain(
            ExplainDetail.JSON_EXECUTION_PLAN)
        assert isinstance(result, str)
        try:
            json.loads(result.split('== Physical Execution Plan ==')[1])
        except:
            self.fail('The execution plan of explain detail is not in json format.')


if __name__ == '__main__':
    import unittest

    try:
        import xmlrunner
        testRunner = xmlrunner.XMLTestRunner(output='target/test-reports')
    except ImportError:
        testRunner = None
    unittest.main(testRunner=testRunner, verbosity=2)
