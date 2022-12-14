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
package org.apache.wicket.resource.aggregator;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.resource.JQueryPluginResourceReference;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

/**
 * Dummy page used for testing the {@link org.apache.wicket.markup.head.ResourceAggregator}.
 * 
 * @author Hielke Hoeve
 */
public class ResourceAggregatorTest2Page extends ResourceAggregatorTest1Page
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 */
	public ResourceAggregatorTest2Page()
	{
		super();
	}


	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);

		response.render(JavaScriptHeaderItem.forReference(new JQueryPluginResourceReference(
			ResourceAggregatorTest2Page.class, "ResourceAggregatorTest2Page.js")));
	}
}
