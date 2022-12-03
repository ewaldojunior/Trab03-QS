/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.sql.parser.dml;

import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.parser.SqlParserPos;

/**
 * Defines the keywords that can occur immediately after the "INSERT" keyword.
 *
 * <p>Standard SQL has no such keywords, but extension projects may define them.
 *
 * <p>This class is almost an extension of {@link org.apache.calcite.sql.SqlInsertKeyword}.
 */
public enum RichSqlInsertKeyword {
    OVERWRITE;

    /**
     * Creates a parse-tree node representing an occurrence of this keyword at a particular position
     * in the parsed text.
     */
    public SqlLiteral symbol(SqlParserPos pos) {
        return SqlLiteral.createSymbol(this, pos);
    }
}
