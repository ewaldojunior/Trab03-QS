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

package org.apache.flink.runtime.util;

import org.apache.flink.core.testutils.CommonTestUtils;
import org.apache.flink.testutils.ClassLoaderUtils;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.InstantiationUtil;
import org.apache.flink.util.SerializedThrowable;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests for {@link SerializedThrowable}. */
public class SerializedThrowableTest {

    @Test
    public void testIdenticalMessageAndStack() {
        try {
            IllegalArgumentException original = new IllegalArgumentException("test message");
            SerializedThrowable serialized = new SerializedThrowable(original);

            assertEquals(
                    ExceptionUtils.stringifyException(original),
                    ExceptionUtils.stringifyException(serialized));

            assertArrayEquals(original.getStackTrace(), serialized.getStackTrace());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSerialization() {
        try {
            // We need an exception whose class is not in the core class loader
            final ClassLoaderUtils.ObjectAndClassLoader<Exception> outsideClassLoading =
                    ClassLoaderUtils.createExceptionObjectFromNewClassLoader();
            ClassLoader loader = outsideClassLoading.getClassLoader();
            Exception userException = outsideClassLoading.getObject();
            Class<?> clazz = userException.getClass();

            // check that we cannot simply copy the exception
            try {
                byte[] serialized = InstantiationUtil.serializeObject(userException);
                InstantiationUtil.deserializeObject(serialized, getClass().getClassLoader());
                fail("should fail with a class not found exception");
            } catch (ClassNotFoundException e) {
                // as we want it
            }

            // validate that the SerializedThrowable mimics the original exception
            SerializedThrowable serialized = new SerializedThrowable(userException);
            assertEquals(
                    ExceptionUtils.stringifyException(userException),
                    ExceptionUtils.stringifyException(serialized));
            assertArrayEquals(userException.getStackTrace(), serialized.getStackTrace());

            // validate the detailMessage of SerializedThrowable contains the class name of original
            // exception
            Exception userException2 = new Exception("error");
            SerializedThrowable serialized2 = new SerializedThrowable(userException2);
            String result =
                    String.format(
                            "%s: %s",
                            userException2.getClass().getName(), userException2.getMessage());
            assertEquals(serialized2.getMessage(), result);

            // copy the serialized throwable and make sure everything still works
            SerializedThrowable copy = CommonTestUtils.createCopySerializable(serialized);
            assertEquals(
                    ExceptionUtils.stringifyException(userException),
                    ExceptionUtils.stringifyException(copy));
            assertArrayEquals(userException.getStackTrace(), copy.getStackTrace());

            // deserialize the proper exception
            Throwable deserialized = copy.deserializeError(loader);
            assertEquals(clazz, deserialized.getClass());

            // deserialization with the wrong classloader does not lead to a failure
            Throwable wronglyDeserialized = copy.deserializeError(getClass().getClassLoader());
            assertEquals(
                    ExceptionUtils.stringifyException(userException),
                    ExceptionUtils.stringifyException(wronglyDeserialized));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testCauseChaining() {
        Exception cause2 = new Exception("level2");
        Exception cause1 = new Exception("level1", cause2);
        Exception root = new Exception("level0", cause1);

        SerializedThrowable st = new SerializedThrowable(root);

        assertEquals("java.lang.Exception: level0", st.getMessage());

        assertNotNull(st.getCause());
        assertEquals("java.lang.Exception: level1", st.getCause().getMessage());

        assertNotNull(st.getCause().getCause());
        assertEquals("java.lang.Exception: level2", st.getCause().getCause().getMessage());
    }

    @Test
    public void testCyclicCauseChaining() {
        Exception cause3 = new Exception("level3");
        Exception cause2 = new Exception("level2", cause3);
        Exception cause1 = new Exception("level1", cause2);
        Exception root = new Exception("level0", cause1);

        // introduce a cyclic reference
        cause3.initCause(cause1);

        SerializedThrowable st = new SerializedThrowable(root);

        assertArrayEquals(root.getStackTrace(), st.getStackTrace());
        assertEquals(
                ExceptionUtils.stringifyException(root), ExceptionUtils.stringifyException(st));
    }

    @Test
    public void testCopyPreservesCause() {
        Exception original = new Exception("original message");
        Exception parent = new Exception("parent message", original);

        SerializedThrowable serialized = new SerializedThrowable(parent);
        assertNotNull(serialized.getCause());

        SerializedThrowable copy = new SerializedThrowable(serialized);
        assertEquals(
                "org.apache.flink.util.SerializedThrowable: java.lang.Exception: parent message",
                copy.getMessage());
        assertNotNull(copy.getCause());
        assertEquals("java.lang.Exception: original message", copy.getCause().getMessage());
    }

    @Test
    public void testSuppressedTransferring() {
        Exception root = new Exception("root");
        Exception suppressed = new Exception("suppressed");
        root.addSuppressed(suppressed);

        SerializedThrowable serializedThrowable = new SerializedThrowable(root);

        assertEquals(1, serializedThrowable.getSuppressed().length);
        Throwable actualSuppressed = serializedThrowable.getSuppressed()[0];
        assertTrue(actualSuppressed instanceof SerializedThrowable);
        assertEquals("java.lang.Exception: suppressed", actualSuppressed.getMessage());
    }

    @Test
    public void testCopySuppressed() {
        Exception root = new Exception("root");
        Exception suppressed = new Exception("suppressed");
        root.addSuppressed(suppressed);

        SerializedThrowable serializedThrowable = new SerializedThrowable(root);
        SerializedThrowable copy = new SerializedThrowable(serializedThrowable);

        assertEquals(1, copy.getSuppressed().length);
        Throwable actualSuppressed = copy.getSuppressed()[0];
        assertTrue(actualSuppressed instanceof SerializedThrowable);
        assertEquals("java.lang.Exception: suppressed", actualSuppressed.getMessage());
    }
}
