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
package org.apache.wicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Serializable;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Tests for converter locators.
 * 
 * @author Eelco Hillenius
 */
final class ConverterLocatorTest
{
	private final ConverterLocator locator = new ConverterLocator();

	/**
	 * Test generalized conversion
	 */
	@Test
	void test()
	{
		assertNotNull(locator.getConverter(Integer.class));
		assertNotNull(locator.getConverter(Double.class));

		// default converter
		assertNotNull(locator.getConverter(String.class).convertToObject("", Locale.US));
	}

	/**
	 * WICKET-4755
	 */
	@Test
	void isInstance()
	{
		assertEquals("test",
			locator.getConverter(Serializable.class).convertToObject("test", Locale.US));
		assertEquals("test", locator.getConverter(Object.class).convertToObject("test", Locale.US));
	}

}