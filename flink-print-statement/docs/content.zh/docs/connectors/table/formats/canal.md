---
title: Canal
weight: 6
type: docs
aliases:
  - /zh/dev/table/connectors/formats/canal.html
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

# Canal Format

{{< label "Changelog-Data-Capture Format" >}}
{{< label "Format: Serialization Schema" >}}
{{< label "Format: Deserialization Schema" >}}

[Canal](https://github.com/alibaba/canal/wiki) 是一个 CDC（ChangeLog Data Capture，变更日志数据捕获）工具，可以实时地将 MySQL 变更传输到其他系统。Canal 为变更日志提供了统一的数据格式，并支持使用 JSON 或 [protobuf](https://developers.google.com/protocol-buffers) 序列化消息（Canal 默认使用 protobuf）。

Flink 支持将 Canal 的 JSON 消息解析为 INSERT / UPDATE / DELETE 消息到 Flink SQL 系统中。在很多情况下，利用这个特性非常的有用，例如
 - 将增量数据从数据库同步到其他系统
 - 日志审计
 - 数据库的实时物化视图
 - 关联维度数据库的变更历史，等等。

Flink 还支持将 Flink SQL 中的 INSERT / UPDATE / DELETE 消息编码为 Canal 格式的 JSON 消息，输出到 Kafka 等存储中。
但需要注意的是，目前 Flink 还不支持将 UPDATE_BEFORE 和 UPDATE_AFTER 合并为一条 UPDATE 消息。因此，Flink 将 UPDATE_BEFORE 和 UPDATE_AFTER 分别编码为 DELETE 和 INSERT 类型的 Canal 消息。

*注意：未来会支持 Canal protobuf 类型消息的解析以及输出 Canal 格式的消息。*

依赖
------------

{{< sql_download_table "canal" >}}

*注意：有关如何部署 Canal 以将变更日志同步到消息队列，请参阅 [Canal 文档](https://github.com/alibaba/canal/wiki)。*


如何使用 Canal Format
----------------

Canal 为变更日志提供了统一的格式，下面是一个从 MySQL 库 `products` 表中捕获更新操作的简单示例：

```json
{
  "data": [
    {
      "id": "111",
      "name": "scooter",
      "description": "Big 2-wheel scooter",
      "weight": "5.18"
    }
  ],
  "database": "inventory",
  "es": 1589373560000,
  "id": 9,
  "isDdl": false,
  "mysqlType": {
    "id": "INTEGER",
    "name": "VARCHAR(255)",
    "description": "VARCHAR(512)",
    "weight": "FLOAT"
  },
  "old": [
    {
      "weight": "5.15"
    }
  ],
  "pkNames": [
    "id"
  ],
  "sql": "",
  "sqlType": {
    "id": 4,
    "name": 12,
    "description": 12,
    "weight": 7
  },
  "table": "products",
  "ts": 1589373560798,
  "type": "UPDATE"
}
```

*注意：有关各个字段的含义，请参阅 [Canal 文档](https://github.com/alibaba/canal/wiki)*

MySQL `products` 表有4列（`id`，`name`，`description` 和 `weight`）。上面的 JSON 消息是 `products` 表上的一个更新事件，表示 `id = 111` 的行数据上 `weight` 字段值从`5.15`变更成为 `5.18`。假设消息已经同步到了一个 Kafka 主题：`products_binlog`，那么就可以使用以下DDL来从这个主题消费消息并解析变更事件。

```sql
CREATE TABLE topic_products (
  -- 元数据与 MySQL "products" 表完全相同
  id BIGINT,
  name STRING,
  description STRING,
  weight DECIMAL(10, 2)
) WITH (
 'connector' = 'kafka',
 'topic' = 'products_binlog',
 'properties.bootstrap.servers' = 'localhost:9092',
 'properties.group.id' = 'testGroup',
 'format' = 'canal-json'  -- 使用 canal-json 格式
)
```

将 Kafka 主题注册成 Flink 表之后，就可以将 Canal 消息用作变更日志源。

```sql
-- 关于MySQL "products" 表的实时物化视图
-- 计算相同产品的最新平均重量
SELECT name, AVG(weight) FROM topic_products GROUP BY name;

-- 将 MySQL "products" 表的所有数据和增量更改同步到
-- Elasticsearch "products" 索引以供将来搜索
INSERT INTO elasticsearch_products
SELECT * FROM topic_products;
```

Available Metadata
------------------

The following format metadata can be exposed as read-only (`VIRTUAL`) columns in a table definition.

<span class="label label-danger">Attention</span> Format metadata fields are only available if the
corresponding connector forwards format metadata. Currently, only the Kafka connector is able to expose
metadata fields for its value format.

<table class="table table-bordered">
    <thead>
    <tr>
      <th class="text-left" style="width: 25%">Key</th>
      <th class="text-center" style="width: 40%">Data Type</th>
      <th class="text-center" style="width: 40%">Description</th>
    </tr>
    </thead>
    <tbody>
    <tr>
      <td><code>database</code></td>
      <td><code>STRING NULL</code></td>
      <td>The originating database. Corresponds to the <code>database</code> field in the
      Canal record if available.</td>
    </tr>
    <tr>
      <td><code>table</code></td>
      <td><code>STRING NULL</code></td>
      <td>The originating database table. Corresponds to the <code>table</code> field in the
      Canal record if available.</td>
    </tr>
    <tr>
      <td><code>sql-type</code></td>
      <td><code>MAP&lt;STRING, INT&gt; NULL</code></td>
      <td>Map of various sql types. Corresponds to the <code>sqlType</code> field in the 
      Canal record if available.</td>
    </tr>
    <tr>
      <td><code>pk-names</code></td>
      <td><code>ARRAY&lt;STRING&gt; NULL</code></td>
      <td>Array of primary key names. Corresponds to the <code>pkNames</code> field in the 
      Canal record if available.</td>
    </tr>
    <tr>
      <td><code>ingestion-timestamp</code></td>
      <td><code>TIMESTAMP_LTZ(3) NULL</code></td>
      <td>The timestamp at which the connector processed the event. Corresponds to the <code>ts</code>
      field in the Canal record.</td>
    </tr>
    </tbody>
</table>

The following example shows how to access Canal metadata fields in Kafka:

```sql
CREATE TABLE KafkaTable (
  origin_database STRING METADATA FROM 'value.database' VIRTUAL,
  origin_table STRING METADATA FROM 'value.table' VIRTUAL,
  origin_sql_type MAP<STRING, INT> METADATA FROM 'value.sql-type' VIRTUAL,
  origin_pk_names ARRAY<STRING> METADATA FROM 'value.pk-names' VIRTUAL,
  origin_ts TIMESTAMP(3) METADATA FROM 'value.ingestion-timestamp' VIRTUAL,
  user_id BIGINT,
  item_id BIGINT,
  behavior STRING
) WITH (
  'connector' = 'kafka',
  'topic' = 'user_behavior',
  'properties.bootstrap.servers' = 'localhost:9092',
  'properties.group.id' = 'testGroup',
  'scan.startup.mode' = 'earliest-offset',
  'value.format' = 'canal-json'
);
```

Format 参数
----------------

<table class="table table-bordered">
    <thead>
      <tr>
        <th class="text-left" style="width: 25%">选项</th>
        <th class="text-center" style="width: 8%">要求</th>
        <th class="text-center" style="width: 7%">默认</th>
        <th class="text-center" style="width: 10%">类型</th>
        <th class="text-center" style="width: 50%">描述</th>
      </tr>
    </thead>
    <tbody>
    <tr>
      <td><h5>format</h5></td>
      <td>必填</td>
      <td style="word-wrap: break-word;">(none)</td>
      <td>String</td>
      <td>指定要使用的格式，此处应为 <code>'canal-json'</code>.</td>
    </tr>
    <tr>
      <td><h5>canal-json.ignore-parse-errors</h5></td>
      <td>选填</td>
      <td style="word-wrap: break-word;">false</td>
      <td>Boolean</td>
      <td>当解析异常时，是跳过当前字段或行，还是抛出错误失败（默认为 false，即抛出错误失败）。如果忽略字段的解析异常，则会将该字段值设置为<code>null</code>。</td>
    </tr>
    <tr>
       <td><h5>canal-json.timestamp-format.standard</h5></td>
       <td>选填</td>
       <td style="word-wrap: break-word;"><code>'SQL'</code></td>
       <td>String</td>
       <td>指定输入和输出时间戳格式。当前支持的值是 <code>'SQL'</code> 和 <code>'ISO-8601'</code>:
       <ul>
         <li>选项 <code>'SQL'</code> 将解析 "yyyy-MM-dd HH:mm:ss.s{precision}" 格式的输入时间戳，例如 '2020-12-30 12:13:14.123'，并以相同格式输出时间戳。</li>
         <li>选项 <code>'ISO-8601'</code> 将解析 "yyyy-MM-ddTHH:mm:ss.s{precision}" 格式的输入时间戳，例如 '2020-12-30T12:13:14.123'，并以相同的格式输出时间戳。</li>
       </ul>
       </td>
    </tr>
    <tr>
       <td><h5>canal-json.map-null-key.mode</h5></td>
       <td>选填</td>
       <td style="word-wrap: break-word;"><code>'FAIL'</code></td>
       <td>String</td>
       <td>指定处理 Map 中 key 值为空的方法. 当前支持的值有 <code>'FAIL'</code>, <code>'DROP'</code> 和 <code>'LITERAL'</code>:
       <ul>
         <li>Option <code>'FAIL'</code> 将抛出异常，如果遇到 Map 中 key 值为空的数据。</li>
         <li>Option <code>'DROP'</code> 将丢弃 Map 中 key 值为空的数据项。</li> 
         <li>Option <code>'LITERAL'</code> 将使用字符串常量来替换 Map 中的空 key 值。字符串常量的值由 <code>'canal-json.map-null-key.literal'</code> 定义。</li>
       </ul>
       </td>
    </tr>
    <tr>
      <td><h5>canal-json.map-null-key.literal</h5></td>
      <td>选填</td>
      <td style="word-wrap: break-word;">'null'</td>
      <td>String</td>
      <td>当 <code>'canal-json.map-null-key.mode'</code> 是 LITERAL 的时候，指定字符串常量替换 Map 中的空 key 值。</td>
    </tr>       
    <tr>
      <td><h5>canal-json.encode.decimal-as-plain-number</h5></td>
      <td>选填</td>
      <td style="word-wrap: break-word;">false</td>
      <td>Boolean</td>
      <td>将所有 DECIMAL 类型的数据保持原状，不使用科学计数法表示。例：<code>0.000000027</code> 默认会表示为 <code>2.7E-8</code>。当此选项设为 true 时，则会表示为 <code>0.000000027</code>。</td>
    </tr>
    <tr>
      <td><h5>canal-json.database.include</h5></td>
      <td>optional</td>
      <td style="word-wrap: break-word;">(none)</td>
      <td>String</td>
      <td>一个可选的正则表达式，通过正则匹配 Canal 记录中的 "database" 元字段，仅读取指定数据库的 changelog 记录。正则字符串与 Java 的 <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Pattern</a> 兼容。</td>
    </tr>
    <tr>
      <td><h5>canal-json.table.include</h5></td>
      <td>optional</td>
      <td style="word-wrap: break-word;">(none)</td>
      <td>String</td>
      <td>一个可选的正则表达式，通过正则匹配 Canal 记录中的 "table" 元字段，仅读取指定表的 changelog 记录。正则字符串与 Java 的 <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Pattern</a> 兼容。</td>
    </tr>
    </tbody>
</table>

注意事项
----------------

### 重复的变更事件

在正常的操作环境下，Canal 应用能以 **exactly-once** 的语义投递每条变更事件。在这种情况下，Flink 消费 Canal 产生的变更事件能够工作得很好。
然而，当有故障发生时，Canal 应用只能保证 **at-least-once** 的投递语义。
这也意味着，在非正常情况下，Canal 可能会投递重复的变更事件到消息队列中，当 Flink 从消息队列中消费的时候就会得到重复的事件。
这可能会导致 Flink query 的运行得到错误的结果或者非预期的异常。因此，建议在这种情况下，建议在这种情况下，将作业参数 [`table.exec.source.cdc-events-duplicate`]({{< ref "docs/dev/table/config" >}}#table-exec-source-cdc-events-duplicate) 设置成 `true`，并在该 source 上定义 PRIMARY KEY。
框架会生成一个额外的有状态算子，使用该 primary key 来对变更事件去重并生成一个规范化的 changelog 流。

数据类型映射
----------------

目前，Canal Format 使用 JSON Format 进行序列化和反序列化。 有关数据类型映射的更多详细信息，请参阅 [JSON Format 文档]({{< ref "docs/connectors/table/formats/json" >}}#data-type-mapping)。

