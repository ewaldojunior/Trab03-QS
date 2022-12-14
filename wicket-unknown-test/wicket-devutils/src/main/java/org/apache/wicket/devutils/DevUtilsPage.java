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
package org.apache.wicket.devutils;

import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.debug.PageView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

/**
 * All pages in the wicket-devutils package should extend this page so that they automatically get
 * checked to make sure that the utilities are enabled in the application debug settings.
 * 
 * @author Jeremy Thomerson
 */
public class DevUtilsPage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 */
	public DevUtilsPage()
	{
		super();
	}

	/**
	 * Construct.
	 * 
	 * @param model
	 */
	public DevUtilsPage(final IModel<?> model)
	{
		super(model);
	}

	/**
	 * Construct.
	 * 
	 * @param parameters
	 */
	public DevUtilsPage(final PageParameters parameters)
	{
		super(parameters);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new DebugBar("debug"));
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		DevelopmentUtilitiesNotEnabledException.check();
	}
	
	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(
			CssHeaderItem.forReference(new CssResourceReference(PageView.class, "pageview.css")));
	}
}
