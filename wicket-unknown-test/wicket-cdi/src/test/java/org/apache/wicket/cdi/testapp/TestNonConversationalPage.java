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
package org.apache.wicket.cdi.testapp;

import jakarta.inject.Inject;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author jsarman
 */
public class TestNonConversationalPage extends WebPage
{
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(TestConversationPage.class);
	@Inject
	TestConversationBean counter;

	public TestNonConversationalPage()
	{
		logger.debug("Starting TestConversationalPage");
		add(new Label("count", new PropertyModel<String>(this, "counter.countStr")));

		add(new Link<Void>("increment")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick()
			{
				counter.increment();
			}
		});
	}
}
