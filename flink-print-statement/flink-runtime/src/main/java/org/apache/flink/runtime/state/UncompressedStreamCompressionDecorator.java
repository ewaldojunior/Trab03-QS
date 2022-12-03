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

package org.apache.flink.runtime.state;

import org.apache.flink.annotation.Internal;
import org.apache.flink.runtime.util.NonClosingInputStreamDecorator;
import org.apache.flink.runtime.util.NonClosingOutputStreamDecorator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** This implementation does not decorate the stream with any compression. */
@Internal
public class UncompressedStreamCompressionDecorator extends StreamCompressionDecorator {

    public static final StreamCompressionDecorator INSTANCE =
            new UncompressedStreamCompressionDecorator();

    private static final long serialVersionUID = 1L;

    @Override
    protected OutputStream decorateWithCompression(NonClosingOutputStreamDecorator stream)
            throws IOException {
        return stream;
    }

    @Override
    protected InputStream decorateWithCompression(NonClosingInputStreamDecorator stream)
            throws IOException {
        return stream;
    }
}
