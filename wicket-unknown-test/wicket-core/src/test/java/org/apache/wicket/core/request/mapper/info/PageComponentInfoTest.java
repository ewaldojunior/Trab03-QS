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
package org.apache.wicket.core.request.mapper.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Matej Knopp
 */
class PageComponentInfoTest
{

	private void testPageInfoOnly(PageComponentInfo info, Integer pageId)
	{
		assertNull(info.getComponentInfo());
		assertNotNull(info.getPageInfo());

		assertEquals(pageId, info.getPageInfo().getPageId());
	}

	private void testPageComponentInfo(PageComponentInfo info, Integer pageId, String componentPath)
	{
		assertNotNull(info.getComponentInfo());
		assertNotNull(info.getPageInfo());

		assertEquals(pageId, info.getPageInfo().getPageId());

		assertEquals(componentPath, info.getComponentInfo().getComponentPath());
	}

	/**
	 * 
	 */
	@Test
	void test1()
	{
		String s = "2--foo-bar-baz";
		PageComponentInfo info = PageComponentInfo.parse(s);
		testPageComponentInfo(info, 2, "foo:bar:baz");
		assertEquals(s, info.toString());
	}

	/**
	 * 
	 */
	@Test
	void test2()
	{
		String s = "2";
		PageComponentInfo info = PageComponentInfo.parse(s);
		testPageInfoOnly(info, 2);
		assertEquals(s, info.toString());
	}

	/**
	 * <a href="https://issues.apache.org/jira/browse/WICKET-3490">WICKET-3490</a>
	 */
	@Test
	void parsePageInfo()
	{
		PageComponentInfo pageComponentInfo = PageComponentInfo
			.parse("99999999999999999999999999999999999999999999999999999999999999999999999");
		assertNull(pageComponentInfo);
	}
}
