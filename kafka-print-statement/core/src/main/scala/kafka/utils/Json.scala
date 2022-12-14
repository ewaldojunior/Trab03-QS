/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kafka.utils

import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.MissingNode
import kafka.utils.json.JsonValue

import scala.reflect.ClassTag

/**
 * Provides methods for parsing JSON with Jackson and encoding to JSON with a simple and naive custom implementation.
 */
object Json {

  private val mapper = new ObjectMapper()

  /**
   * Parse a JSON string into a JsonValue if possible. `None` is returned if `input` is not valid JSON.
   */
  def parseFull(input: String): Option[JsonValue] = tryParseFull(input).toOption

  /**
   * Parse a JSON string into either a generic type T, or a JsonProcessingException in the case of
   * exception.
   */
  def parseStringAs[T](input: String)(implicit tag: ClassTag[T]): Either[JsonProcessingException, T] = {
    try Right(mapper.readValue(input, tag.runtimeClass).asInstanceOf[T])
    catch { case e: JsonProcessingException => Left(e) }
  }

  /**
   * Parse a JSON byte array into a JsonValue if possible. `None` is returned if `input` is not valid JSON.
   */
  def parseBytes(input: Array[Byte]): Option[JsonValue] =
    try Option(mapper.readTree(input)).map(JsonValue(_))
    catch { case _: JsonProcessingException => None }

  def tryParseBytes(input: Array[Byte]): Either[JsonProcessingException, JsonValue] =
    try Right(mapper.readTree(input)).map(JsonValue(_))
    catch { case e: JsonProcessingException => Left(e) }

  /**
   * Parse a JSON byte array into either a generic type T, or a JsonProcessingException in the case of exception.
   */
  def parseBytesAs[T](input: Array[Byte])(implicit tag: ClassTag[T]): Either[JsonProcessingException, T] = {
    try Right(mapper.readValue(input, tag.runtimeClass).asInstanceOf[T])
    catch { case e: JsonProcessingException => Left(e) }
  }

  /**
   * Parse a JSON string into a JsonValue if possible. It returns an `Either` where `Left` will be an exception and
    * `Right` is the `JsonValue`.
   * @param input a JSON string to parse
   * @return An `Either` which in case of `Left` means an exception and `Right` is the actual return value.
   */
  def tryParseFull(input: String): Either[JsonProcessingException, JsonValue] =
    if (input == null || input.isEmpty)
      Left(new JsonParseException(MissingNode.getInstance().traverse(), "The input string shouldn't be empty"))
    else
      try Right(mapper.readTree(input)).map(JsonValue(_))
      catch { case e: JsonProcessingException => Left(e) }

  /**
   * Encode an object into a JSON string. This method accepts any type supported by Jackson's ObjectMapper in
   * the default configuration. That is, Java collections are supported, but Scala collections are not (to avoid
   * a jackson-scala dependency).
   */
  def encodeAsString(obj: Any): String = mapper.writeValueAsString(obj)

  /**
   * Encode an object into a JSON value in bytes. This method accepts any type supported by Jackson's ObjectMapper in
   * the default configuration. That is, Java collections are supported, but Scala collections are not (to avoid
   * a jackson-scala dependency).
   */
  def encodeAsBytes(obj: Any): Array[Byte] = mapper.writeValueAsBytes(obj)
}
