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
package org.apache.wicket.core.request.mapper;

import org.apache.wicket.MockPage;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.markup.MarkupParser;
import org.apache.wicket.mock.MockPageContext;
import org.apache.wicket.page.IPageManager;
import org.apache.wicket.page.PageManager;
import org.apache.wicket.pageStore.CachingPageStore;
import org.apache.wicket.pageStore.IPageContext;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.pageStore.InMemoryPageStore;
import org.apache.wicket.pageStore.InSessionPageStore;
import org.apache.wicket.pageStore.RequestPageStore;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceReference.Key;
import org.apache.wicket.request.resource.ResourceReferenceRegistry;
import org.apache.wicket.serialize.java.JavaSerializer;

/**
 * Simple {@link IMapperContext} implementation for testing purposes
 *
 * @author Matej Knopp
 */
public class TestMapperContext implements IMapperContext
{
	private static final String APP_NAME = "test_app";
	private static int count;

	IPageStore pageStore;
	MockPageContext pageContext;
	IPageManager pageManager;
	private String appName;
	private boolean createMockPageIfInstanceNotFound = true;

	/**
	 * Construct.
	 */
	public TestMapperContext()
	{
		appName = APP_NAME + count++;
		
		pageContext = new MockPageContext();
		
		InMemoryPageStore inMemoryPageStore = new InMemoryPageStore(appName, Integer.MAX_VALUE);
		InSessionPageStore inSessionPageStore = new InSessionPageStore(4, new JavaSerializer(appName));
		pageStore = new CachingPageStore(inMemoryPageStore, inSessionPageStore);
		pageManager = new PageManager(new RequestPageStore(pageStore)) {
			@Override
			protected IPageContext createPageContext()
			{
				return pageContext;
			}
		};
	}

	/**
	 * just making sure the session cache will be empty by simulating an intermezzo request
	 */
	public void cleanSessionCache()
	{
		pageContext.clearRequest();
		MockPage other = new MockPage();
		other.setPageId(Integer.MAX_VALUE);
		getPageManager().touchPage(other);
		pageManager.detach();
	}

	/**
	 * @return pageManager
	 */
	public IPageManager getPageManager()
	{
		return pageManager;
	}

	@Override
	public String getBookmarkableIdentifier()
	{
		return "bookmarkable";
	}

	@Override
	public String getNamespace()
	{
		return MarkupParser.WICKET;
	}

	@Override
	public String getPageIdentifier()
	{
		return "page";
	}

	@Override
	public String getResourceIdentifier()
	{
		return "resource";
	}

	@Override
	public ResourceReferenceRegistry getResourceReferenceRegistry()
	{
		return registry;
	}

	private final ResourceReferenceRegistry registry = new ResourceReferenceRegistry()
	{
		@Override
		protected ResourceReference createDefaultResourceReference(Key key)
		{
			// Do not create package resource here because it requires "real" application
			return null;
		}
	};

	private boolean bookmarkable = true;

	/**
	 * Determines whether the newly created page will have bookmarkable flag set
	 *
	 * @param bookmarkable
	 */
	public void setBookmarkable(boolean bookmarkable)
	{
		this.bookmarkable = bookmarkable;
	}

	private boolean createdBookmarkable = true;

	/**
	 * Determines whether the newly created page will have createdBookmarkable flag set
	 *
	 * @param createdBookmarkable
	 */
	public void setCreatedBookmarkable(boolean createdBookmarkable)
	{
		this.createdBookmarkable = createdBookmarkable;
	}

	private int nextPageRenderCount = 0;

	/**
	 *
	 * @param nextPageRenderCount
	 */
	public void setNextPageRenderCount(int nextPageRenderCount)
	{
		this.nextPageRenderCount = nextPageRenderCount;
	}

	@Override
	public IRequestablePage getPageInstance(int pageId)
	{

		IRequestablePage requestablePage = (IRequestablePage)pageManager.getPage(pageId);
		if (requestablePage == null && createMockPageIfInstanceNotFound)
		{
			MockPage page = new MockPage();
			page.setPageId(pageId);
			page.setBookmarkable(bookmarkable);
			page.setCreatedBookmarkable(createdBookmarkable);
			page.setRenderCount(nextPageRenderCount);
			requestablePage = page;
		}
		return requestablePage;

	}

	int idCounter = 0;

	@Override
	public IRequestablePage newPageInstance(Class<? extends IRequestablePage> pageClass,
		PageParameters pageParameters)
	{
		try
		{
			MockPage page;
			page = (MockPage)pageClass.getDeclaredConstructor().newInstance();
			page.setPageId(++idCounter);
			page.setBookmarkable(true);
			page.setCreatedBookmarkable(true);
			if (pageParameters != null)
			{
				page.getPageParameters().overwriteWith(pageParameters);
			}
			return page;
		}
		catch (Exception e)
		{
			throw new WicketRuntimeException(e);
		}
	}

	@Override
	public Class<? extends IRequestablePage> getHomePageClass()
	{
		return MockPage.class;
	}

	/**
	 *
	 * Adapts {@link PageProvider} to this {@link IMapperContext}
	 *
	 * @author Pedro Santos
	 */
	public class TestPageProvider extends PageProvider
	{

		/**
		 * Construct.
		 *
		 * @param pageId
		 * @param renderCount
		 */
		public TestPageProvider(int pageId, Integer renderCount)
		{
			super(pageId, renderCount);
		}

		@Override
		protected IPageSource getPageSource()
		{
			return TestMapperContext.this;
		}
	}

}
