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

package org.apache.flink.orc.nohive.vector;

import org.apache.flink.table.data.TimestampData;

import org.apache.orc.storage.ql.exec.vector.TimestampColumnVector;

import java.sql.Timestamp;

/**
 * This column vector is used to adapt hive's TimestampColumnVector to Flink's
 * TimestampColumnVector.
 */
public class OrcNoHiveTimestampVector extends AbstractOrcNoHiveVector
        implements org.apache.flink.table.data.columnar.vector.TimestampColumnVector {

    private TimestampColumnVector vector;

    public OrcNoHiveTimestampVector(TimestampColumnVector vector) {
        super(vector);
        this.vector = vector;
    }

    @Override
    public TimestampData getTimestamp(int i, int precision) {
        int index = vector.isRepeating ? 0 : i;
        Timestamp timestamp = new Timestamp(vector.time[index]);
        timestamp.setNanos(vector.nanos[index]);
        return TimestampData.fromTimestamp(timestamp);
    }
}
