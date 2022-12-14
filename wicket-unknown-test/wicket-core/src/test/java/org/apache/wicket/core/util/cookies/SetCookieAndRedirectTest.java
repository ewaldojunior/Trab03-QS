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
package org.apache.wicket.core.util.cookies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import jakarta.servlet.http.Cookie;

import org.apache.wicket.Page;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * Tests setting a cookie and calling setResponsePage() in the same request.
 * 
 * @author Bertrand Guay-Paquet
 */
class SetCookieAndRedirectTest extends WicketTestCase
{
	private static final String cookieValue = "cookieValue";

	/**
	 * Validate proper cookie value set with stateful page
	 */
	@Test
	void statefulPage()
	{
		tester.startPage(SetCookieAndRedirectStatefullTestPage.class);
		FormTester formTester = tester.newFormTester("form");
		formTester.setValue("input", cookieValue);
		formTester.submit();
		Page page = tester.getLastRenderedPage();

		List<Cookie> cookies = tester.getLastResponse().getCookies();
		assertEquals(1, cookies.size());
		assertEquals(cookieValue, cookies.get(0).getValue());
	}

	/**
	 * Validate proper cookie value set with stateless page
	 */
	@Test
	void statelessPage()
	{
		tester.startPage(SetCookieAndRedirectStatelessTestPage.class);
		FormTester formTester = tester.newFormTester("form");
		formTester.setValue("input", cookieValue);
		formTester.submit();
		Page page = tester.getLastRenderedPage();

		List<Cookie> cookies = tester.getLastResponse().getCookies();
		assertEquals(1, cookies.size());
		assertEquals(cookieValue, cookies.get(0).getValue());
	}
}
