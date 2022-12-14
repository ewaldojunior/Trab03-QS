/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.core.util.resource;

import static org.apache.wicket.core.util.resource.ResourceStreamLocatorTest.getFilename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class ClassPathResourceFinderTest extends WicketTestCase
{
	@Test
	void loadStartingFromClasspathRoot() throws Exception
	{
		ClassPathResourceFinder finder = new ClassPathResourceFinder("");
		String filename = ClassPathResourceFinderTest.class.getName().replace('.',
			File.separatorChar) +
			".class";
		IResourceStream rs = finder.find(WebApplication.class, filename);
		assertNotNull(rs);
		assertEquals(ClassPathResourceFinderTest.class.getSimpleName() + ".class", getFilename(rs));
	}

	@Test
	void loadStartingFromPrefix() throws Exception
	{
		ClassPathResourceFinder finder = new ClassPathResourceFinder(
			ClassPathResourceFinderTest.class.getPackage()
				.getName()
				.replace('.', File.separatorChar));
		String filename = ClassPathResourceFinderTest.class.getSimpleName() + ".class";
		IResourceStream rs = finder.find(WebApplication.class, filename);
		assertNotNull(rs);
		assertEquals(ClassPathResourceFinderTest.class.getSimpleName() + ".class", getFilename(rs));
	}
}
