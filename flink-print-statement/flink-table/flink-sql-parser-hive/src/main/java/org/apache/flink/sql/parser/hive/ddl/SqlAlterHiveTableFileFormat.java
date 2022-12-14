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

package org.apache.flink.sql.parser.hive.ddl;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import static org.apache.flink.sql.parser.hive.ddl.SqlAlterHiveTable.AlterTableOp.CHANGE_FILE_FORMAT;

/** ALTER TABLE DDL to change a Hive table/partition's file format. */
public class SqlAlterHiveTableFileFormat extends SqlAlterHiveTable {

    private final SqlIdentifier format;

    public SqlAlterHiveTableFileFormat(
            SqlParserPos pos, SqlIdentifier tableName, SqlNodeList partSpec, SqlIdentifier format) {
        super(CHANGE_FILE_FORMAT, pos, tableName, partSpec, createPropList(format));
        this.format = format;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        super.unparse(writer, leftPrec, rightPrec);
        writer.keyword("SET FILEFORMAT");
        format.unparse(writer, leftPrec, rightPrec);
    }

    private static SqlNodeList createPropList(SqlIdentifier format) {
        SqlNodeList res = new SqlNodeList(format.getParserPosition());
        res.add(
                HiveDDLUtils.toTableOption(
                        SqlCreateHiveTable.HiveTableStoredAs.STORED_AS_FILE_FORMAT,
                        format.getSimple(),
                        format.getParserPosition()));
        return res;
    }
}
