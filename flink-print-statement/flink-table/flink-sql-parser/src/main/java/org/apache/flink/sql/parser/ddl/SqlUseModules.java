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

package org.apache.flink.sql.parser.ddl;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/** USE MODULES sql call. */
public class SqlUseModules extends SqlCall {
    public static final SqlSpecialOperator OPERATOR =
            new SqlSpecialOperator("USE MODULES", SqlKind.OTHER);

    private final List<SqlIdentifier> moduleNames;

    public SqlUseModules(SqlParserPos pos, List<SqlIdentifier> moduleNames) {
        super(pos);
        this.moduleNames = requireNonNull(moduleNames, "moduleNames cannot be null");
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return ImmutableNullableList.copyOf(moduleNames);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        writer.keyword("USE MODULES");
        for (int i = 0; i < moduleNames.size(); i++) {
            moduleNames.get(i).unparse(writer, leftPrec, rightPrec);
            if (i < moduleNames.size() - 1) {
                writer.keyword(",");
            }
        }
    }

    public List<String> moduleNames() {
        return moduleNames.stream().map(SqlIdentifier::getSimple).collect(Collectors.toList());
    }
}
