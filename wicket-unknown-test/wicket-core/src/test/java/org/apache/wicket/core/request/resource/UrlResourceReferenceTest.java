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
package org.apache.wicket.core.request.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * @since 6.0
 */
class UrlResourceReferenceTest extends WicketTestCase
{
	@Test
	void relativeUrl()
	{
		Url url = Url.parse("some/relative/url");
		UrlResourceReference reference = new UrlResourceReference(url);
		assertEquals(url, reference.getUrl());

		CharSequence _url = tester.getRequestCycle().urlFor(reference, null);
		assertEquals(url.toString(), _url);
		assertNull(reference.getResource());
	}

	@Test
	void absoluteUrl()
	{
		Url url = Url.parse("http://www.example.com/some/path.ext");
		UrlResourceReference reference = new UrlResourceReference(url);
		assertEquals(url, reference.getUrl());
		assertNull(reference.getResource());

		CharSequence _url = tester.getRequestCycle().urlFor(reference, null);
		assertEquals(url.toString(Url.StringMode.FULL), _url);
		assertNull(reference.getResource());
	}

	@Test
	void contextAbsoluteUrl()
	{
		Url url = Url.parse("/some/path.ext");
		UrlResourceReference reference = new UrlResourceReference(url);
		assertEquals(url, reference.getUrl());
		assertNull(reference.getResource());

		CharSequence _url = tester.getRequestCycle().urlFor(reference, null);
		assertEquals(url.toString(), _url);
		assertNull(reference.getResource());
	}

	@Test
	void cannotMakeAnAbsoluteUrlContextRelative()
	{
		Url url = Url.parse("http://www.example.com/some/path.ext");
		UrlResourceReference reference = new UrlResourceReference(url);

		assertThrows(IllegalStateException.class, () -> {
			reference.setContextRelative(true);
		});
	}

	@Test
	void contextRelativeUrl()
	{
		tester.getApplication().mountPage("/some/mount/path", TestPage.class);
		tester.startPage(new TestPage());

		tester.assertContains("<script type=\"text/javascript\" src=\"../../some/relative/url\"></script>");
	}

	/**
	 * A test page for #contextRelativeUrl()
	 */
    public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);

			Url url = Url.parse("some/relative/url");
			UrlResourceReference reference = new UrlResourceReference(url);
			reference.setContextRelative(true);
			response.render(JavaScriptHeaderItem.forReference(reference));
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container, Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head></html>");
		}
	}
}
