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
package org.apache.wicket.markup.html;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Test page for <a href="https://issues.apache.org/jira/browse/WICKET-3719">WICKET-3719</a>
 * 
 * @see TransparentWebMarkupContainerTest#ajaxUpdate()
 */
public class TransparentWithAjaxUpdatePage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 * 
	 * @param parameters
	 */
	public TransparentWithAjaxUpdatePage(PageParameters parameters)
	{
		super(parameters);

		add(new TransparentWebMarkupContainer("html-tag"));

		final TransparentWithAjaxUpdatePanel panel = new TransparentWithAjaxUpdatePanel("panel");
		add(panel);

		add(new AjaxLink<Void>("link")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				target.add(panel);
			}
		});
	}
}
