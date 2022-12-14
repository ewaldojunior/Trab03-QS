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
package org.apache.wicket.cdi;

import jakarta.inject.Inject;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.cdi.testapp.TestAppScope;
import org.apache.wicket.cdi.testapp.TestCdiApplication;
import org.apache.wicket.cdi.testapp.TestConversationBean;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.jglue.cdiunit.AdditionalClasses;
import org.jglue.cdiunit.CdiRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * @author jsarman
 */
@RunWith(CdiRunner.class) // TODO upgrade to junit 5 ExtendWith when Cdi project has Junit 5 support
@AdditionalClasses({ CdiWicketTester.class, BehaviorInjector.class, CdiConfiguration.class,
		CdiShutdownCleaner.class, ComponentInjector.class, ConversationExpiryChecker.class,
		ConversationPropagator.class, DetachEventEmitter.class, SessionInjector.class,
		TestAppScope.class, TestConversationBean.class, TestCdiApplication.class,
		AutoConversation.class })
public abstract class WicketCdiTestCase
{
	@Inject
	private ContextManager contextManager;
	/** */
	protected CdiWicketTester tester;

	protected CdiWicketTester newWicketTester(WebApplication app)
	{
		return new CdiWicketTester(app);
	}

	public void configure(CdiConfiguration configuration)
	{
		configuration.configure(tester.getApplication());
	}

	@After
	public void end()
	{
		if (contextManager.isRequestActive())
		{
			contextManager.deactivateContexts();
			contextManager.destroy();
		}
	}

	@Before
	public void commonBefore()
	{
		// make sure no leaked threadlocals are present
		ThreadContext.detach();

		WebApplication application = newApplication();
		tester = newWicketTester(application);
	}

	/**
	 * @return the application that should be used for the test
	 */
	protected WebApplication newApplication()
	{
		return new MockApplication();
	}

	/**
	 * 
	 */
	@After
	public void commonAfter()
	{
		tester.destroy();
	}

	/**
	 * Use <code>-Dwicket.replace.expected.results=true</code> to automatically
	 * replace the expected output file.
	 * 
	 * @param <T>
	 * 
	 * @param pageClass
	 * @param filename
	 * @throws Exception
	 */
	protected <T extends Page> void executeTest(final Class<T> pageClass, final String filename)
			throws Exception
	{
		tester.executeTest(getClass(), pageClass, filename);
	}

	/**
	 * Use <code>-Dwicket.replace.expected.results=true</code> to automatically
	 * replace the expected output file.
	 * 
	 * @param page
	 * @param filename
	 * @throws Exception
	 */
	protected void executeTest(final Page page, final String filename) throws Exception
	{
		tester.executeTest(getClass(), page, filename);
	}

	/**
	 * Use <code>-Dwicket.replace.expected.results=true</code> to automatically
	 * replace the expected output file.
	 * 
	 * @param <T>
	 * 
	 * @param pageClass
	 * @param parameters
	 * @param filename
	 * @throws Exception
	 */
	protected <T extends Page> void executeTest(final Class<T> pageClass,
			PageParameters parameters, final String filename) throws Exception
	{
		tester.executeTest(getClass(), pageClass, parameters, filename);
	}

	/**
	 * 
	 * @param component
	 * @param filename
	 * @throws Exception
	 */
	protected void executeListener(final Component component, final String filename)
			throws Exception
	{
		tester.executeListener(getClass(), component, filename);
	}

	/**
	 * 
	 * @param behavior
	 * @param filename
	 * @throws Exception
	 */
	protected void executeBehavior(final AbstractAjaxBehavior behavior, final String filename)
			throws Exception
	{
		tester.executeBehavior(getClass(), behavior, filename);
	}

	/**
	 * Returns the current Maven build directory taken from the <tt>basedir</tt>
	 * system property, or null if not set
	 * 
	 * @return path with a trailing slash
	 */
	public String getBasedir()
	{
		return WicketTester.getBasedir();
	}
}
