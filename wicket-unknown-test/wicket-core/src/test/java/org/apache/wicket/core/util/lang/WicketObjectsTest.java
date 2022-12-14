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
package org.apache.wicket.core.util.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;

import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * Tests the WicketObjects class.
 * 
 * @author Martijn Dashorst
 */
class WicketObjectsTest extends WicketTestCase
{
	/**
	 * Test method for WicketObjects.cloneModel(null)
	 */
	@Test
	void cloneModelNull()
	{
		Object clone = WicketObjects.cloneObject(null);
		assertEquals(null, clone);
	}

	/**
	 * Test method for WicketObjects.cloneObject(null)
	 */
	@Test
	void cloneObjectNull()
	{
		Object clone = WicketObjects.cloneObject(null);
		assertEquals(null, clone);
	}

	/**
	 * Test method for WicketObjects.cloneModel(String)
	 */
	@Test
	void cloneModelString()
	{
		String cloneMe = "Mini-me";

		Object clone = WicketObjects.cloneObject(cloneMe);
		assertEquals(cloneMe, clone);
		assertNotSame(cloneMe, clone);
	}

	/**
	 * Test method for WicketObjects.cloneObject(String)
	 */
	@Test
	void cloneObjectString()
	{
		String cloneMe = "Mini-me";

		Object clone = WicketObjects.cloneObject(cloneMe);
		assertEquals(cloneMe, clone);
		assertNotSame(cloneMe, clone);
	}

	/**
	 * Test method for WicketObjects.cloneModel(nonSerializableObject)
	 */
	@Test
	void cloneModelNonSerializableObject()
	{
		Object cloneMe = new Object();

		try
		{
			WicketObjects.cloneObject(cloneMe);
			fail("Exception expected");
		}
		catch (RuntimeException e)
		{
			assertTrue(true);
		}
	}

	/**
	 * Test method for WicketObjects.cloneObject(nonSerializableObject)
	 */
	@Test
	void cloneObjectNonSerializableObject()
	{
		Object cloneMe = new Object();

		try
		{
			WicketObjects.cloneObject(cloneMe);
			fail("Exception expected");
		}
		catch (RuntimeException e)
		{
			assertTrue(true);
		}
	}

	/**
	 * Test method for component cloning
	 */
	@Test
	void componentClone()
	{
		PropertyModel<String> pm = new PropertyModel<>(new TextField<>("test", Model.of("test")), "modelObject");
		PropertyModel<String> pm2 = WicketObjects.cloneObject(pm);
		assertEquals(pm.getObject(), pm2.getObject());
	}

	/**
	 * Test method for 'org.apache.wicket.util.lang.Objects.clone(Object)'
	 */
	@Test
	void cloneCloneObject()
	{
		CloneObject cloneMe = new CloneObject();
		cloneMe.nr = 1;

		Object clone = WicketObjects.cloneObject(cloneMe);
		assertEquals(cloneMe, clone);
		assertNotSame(cloneMe, clone);
	}

	/**
	 * Used for testing the clone function.
	 */
	private static final class CloneObject implements Serializable
	{
		private static final long serialVersionUID = 1L;

		/**
		 * int for testing equality.
		 */
		private int nr;

		/**
		 * @see Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o)
		{
			CloneObject other = (CloneObject)o;
			return other.nr == nr;
		}
	}
}
