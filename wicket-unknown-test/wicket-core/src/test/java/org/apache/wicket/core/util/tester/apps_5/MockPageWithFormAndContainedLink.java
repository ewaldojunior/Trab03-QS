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
package org.apache.wicket.core.util.tester.apps_5;

import org.apache.wicket.markup.html.link.AbstractLink;

/**
 * Contains a form with a textfield on it and a link inside the form. Use the
 * {@link #addLink(AbstractLink)} method to add a link to the form.
 * 
 * @author Gerolf Seitz
 */
public class MockPageWithFormAndContainedLink extends MockPageWithFormAndLink
{

	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 * 
	 * @param mockPojo
	 */
	public MockPageWithFormAndContainedLink(MockPojo mockPojo)
	{
		super(mockPojo);
	}

	/**
	 * @param link
	 */
	public void addLink(AbstractLink link)
	{
		getForm().add(link);
	}

}
