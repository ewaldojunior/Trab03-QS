---
title: Orc
weight: 10
type: docs
aliases:
  - /zh/dev/table/connectors/formats/orc.html
---
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Orc Format

{{< label "Format: Serialization Schema" >}}
{{< label "Format: Deserialization Schema" >}}


[Apache Orc](https://orc.apache.org/) Format 允许读写 ORC 数据。

依赖
------------

{{< sql_download_table "orc" >}}


如何用 Orc 格式创建一个表格
----------------

下面是一个用 Filesystem connector 和 Orc format 创建表格的例子

```sql
CREATE TABLE user_behavior (
  user_id BIGINT,
  item_id BIGINT,
  category_id BIGINT,
  behavior STRING,
  ts TIMESTAMP(3),
  dt STRING
) PARTITIONED BY (dt) WITH (
 'connector' = 'filesystem',
 'path' = '/tmp/user_behavior',
 'format' = 'orc'
)
```

Format 参数
----------------

<table class="table table-bordered">
    <thead>
      <tr>
        <th class="text-left" style="width: 25%">参数</th>
        <th class="text-center" style="width: 10%">是否必选</th>
        <th class="text-center" style="width: 10%">默认值</th>
        <th class="text-center" style="width: 10%">类型</th>
        <th class="text-center" style="width: 45%">描述</th>
      </tr>
    </thead>
    <tbody>
    <tr>
      <td><h5>format</h5></td>
      <td>必选</td>
      <td style="word-wrap: break-word;">(none)</td>
      <td>String</td>
      <td>指定要使用的格式，这里应该是 'orc'。</td>
    </tr>
    </tbody>
</table>

Orc 格式也支持来源于 [Table properties](https://orc.apache.org/docs/hive-config.html#table-properties) 的表属性。 举个例子，你可以设置 `orc.compress=SNAPPY` 来允许spappy压缩。

数据类型映射
----------------

Orc 格式类型的映射和 Apache Hive 是兼容的。下面的表格列出了 Flink 类型的数据和 Orc 类型的数据的映射关系。
<table class="table table-bordered">
    <thead>
      <tr>
        <th class="text-left">Flink 数据类型</th>
        <th class="text-center">Orc 物理类型</th>
        <th class="text-center">Orc 逻辑类型</th>
      </tr>
    </thead>
    <tbody>
    <tr>
      <td>CHAR</td>
      <td>bytes</td>
      <td>CHAR</td>
    </tr>
    <tr>
      <td>VARCHAR</td>
      <td>bytes</td>
      <td>VARCHAR</td>
    </tr>
    <tr>
      <td>STRING</td>
      <td>bytes</td>
      <td>STRING</td>
    </tr>
    <tr>
      <td>BOOLEAN</td>
      <td>long</td>
      <td>BOOLEAN</td>
    </tr>
    <tr>
      <td>BYTES</td>
      <td>bytes</td>
      <td>BINARY</td>
    </tr>
    <tr>
      <td>DECIMAL</td>
      <td>decimal</td>
      <td>DECIMAL</td>
    </tr>
    <tr>
      <td>TINYINT</td>
      <td>long</td>
      <td>BYTE</td>
    </tr>
    <tr>
      <td>SMALLINT</td>
      <td>long</td>
      <td>SHORT</td>
    </tr>
    <tr>
      <td>INT</td>
      <td>long</td>
      <td>INT</td>
    </tr>
    <tr>
      <td>BIGINT</td>
      <td>long</td>
      <td>LONG</td>
    </tr>
    <tr>
      <td>FLOAT</td>
      <td>double</td>
      <td>FLOAT</td>
    </tr>
    <tr>
      <td>DOUBLE</td>
      <td>double</td>
      <td>DOUBLE</td>
    </tr>
    <tr>
      <td>DATE</td>
      <td>long</td>
      <td>DATE</td>
    </tr>
    <tr>
      <td>TIMESTAMP</td>
      <td>timestamp</td>
      <td>TIMESTAMP</td>
    </tr>
    <tr>
      <td>ARRAY</td>
      <td>-</td>
      <td>LIST</td>
    </tr>
    <tr>
      <td>MAP</td>
      <td>-</td>
      <td>MAP</td>
    </tr>
    <tr>
      <td>ROW</td>
      <td>-</td>
      <td>STRUCT</td>
    </tr>
    </tbody>
</table>
