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
package org.apache.wicket.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.wicket.IInitializer;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.jmx.wrapper.Application;
import org.apache.wicket.jmx.wrapper.ApplicationSettings;
import org.apache.wicket.jmx.wrapper.DebugSettings;
import org.apache.wicket.jmx.wrapper.MarkupSettings;
import org.apache.wicket.jmx.wrapper.PageSettings;
import org.apache.wicket.jmx.wrapper.RequestCycleSettings;
import org.apache.wicket.jmx.wrapper.RequestLogger;
import org.apache.wicket.jmx.wrapper.ResourceSettings;
import org.apache.wicket.jmx.wrapper.SecuritySettings;
import org.apache.wicket.jmx.wrapper.SessionSettings;
import org.apache.wicket.jmx.wrapper.StoreSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Registers Wicket's MBeans.
 * <p>
 * Users can specify the MBeanServer implementation in which to register the MBeans by setting the
 * <code>org.apache.wicket.mbean.server.agentid</code> property to the agent id of the MBeanServer
 * implementation they want, or by setting <code>org.apache.wicket.mbean.server.class</code> to the
 * mbean server class they want (if both are provided, and the agent id returns a server, that one
 * is used). This initializer will log an error when no mbean server with the provided agent id can
 * be found, and will then fall back to use the platform mbean server. When no agent id is provided,
 * the platform mbean server will be used.
 * 
 * @author eelcohillenius
 * @author David Hosier
 */
public class Initializer implements IInitializer
{
	private static final Logger log = LoggerFactory.getLogger(Initializer.class);

	// It's best to store a reference to the MBeanServer rather than getting it
	// over and over
	private MBeanServer mbeanServer = null;

	/**
	 * List of registered names
	 */
	private final List<ObjectName> registered = new ArrayList<>();

	@Override
	public void destroy(final org.apache.wicket.Application application)
	{
		for (ObjectName objectName : registered)
		{
			try
			{
				mbeanServer.unregisterMBean(objectName);
			}
			catch (InstanceNotFoundException | MBeanRegistrationException e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void init(final org.apache.wicket.Application application)
	{
		try
		{
			String name = application.getName();

			String agentId = null;
			try
			{
				agentId = System.getProperty("wicket.mbean.server.agentid");
			}
			catch (SecurityException e)
			{
				// Ignore - we're not allowed to read this property.
				log.warn("not allowed to read property wicket.mbean.server.agentid due to security settings; ignoring");
			}
			if (agentId != null)
			{
				ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer(agentId);
				if (!mbeanServers.isEmpty())
				{
					mbeanServer = mbeanServers.get(0); // get first
				}
				else
				{
					log.error("unable to find mbean server with agent id {}", agentId);
				}
			}
			if (mbeanServer == null)
			{
				String impl = null;
				try
				{
					impl = System.getProperty("wicket.mbean.server.class");
				}
				catch (SecurityException e)
				{
					// Ignore - we're not allowed to read this property.
					log.warn("Not allowed to read property wicket.mbean.server.class due to security settings: {}. Ignoring",
							e.getMessage());
				}
				if (impl != null)
				{
					ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer(null);
					if (!mbeanServers.isEmpty())
					{
						for (MBeanServer mbs : mbeanServers)
						{
							if (mbs.getClass().getName().equals(impl))
							{
								mbeanServer = mbs;
								break;
							}
						}
					}
					if (mbeanServer == null)
					{
						log.error("unable to find mbean server of type '{}'", impl);
					}
				}
			}
			if (mbeanServer == null)
			{
				mbeanServer = ManagementFactory.getPlatformMBeanServer();
				// never null
			}

			log.info("registering Wicket mbeans with server '{}'", mbeanServer);

			// register top level application object, but first check whether
			// multiple instances of the same application (name) are running and
			// if so adjust the name
			String domain = "org.apache.wicket.app." + name;
			ObjectName appBeanName = new ObjectName(domain + ":type=Application");
			String tempDomain = domain;
			int i = 0;
			while (mbeanServer.isRegistered(appBeanName))
			{
				tempDomain = name + "-" + i++;
				appBeanName = new ObjectName(tempDomain + ":type=Application");
			}
			domain = tempDomain;

			Application appBean = new Application(application);
			register(application, appBean, appBeanName);

			register(application, new ApplicationSettings(application), new ObjectName(domain
				+ ":type=Application,name=ApplicationSettings"));
			register(application, new DebugSettings(application), new ObjectName(domain
				+ ":type=Application,name=DebugSettings"));
			register(application, new MarkupSettings(application), new ObjectName(domain
				+ ":type=Application,name=MarkupSettings"));
			register(application, new ResourceSettings(application), new ObjectName(domain
				+ ":type=Application,name=ResourceSettings"));
			register(application, new PageSettings(application), new ObjectName(domain
				+ ":type=Application,name=PageSettings"));
			register(application, new RequestCycleSettings(application), new ObjectName(domain
				+ ":type=Application,name=RequestCycleSettings"));
			register(application, new SecuritySettings(application), new ObjectName(domain
				+ ":type=Application,name=SecuritySettings"));
			register(application, new SessionSettings(application), new ObjectName(domain
				+ ":type=Application,name=SessionSettings"));
			register(application, new StoreSettings(application), new ObjectName(domain
				+ ":type=Application,name=StoreSettings"));

			RequestLogger sessionsBean = new RequestLogger(application);
			ObjectName sessionsBeanName = new ObjectName(domain + ":type=RequestLogger");
			register(application, sessionsBean, sessionsBeanName);
		}
		catch (MalformedObjectNameException | InstanceAlreadyExistsException |
				MBeanRegistrationException | NotCompliantMBeanException e)
		{
			throw new WicketRuntimeException(e);
		}
	}

	@Override
	public String toString()
	{
		return "Wicket JMX initializer";
	}

	/**
	 * Register MBean.
	 * 
	 * @param o
	 *            MBean
	 * @param objectName
	 *            Object name
	 * 
	 * @throws NotCompliantMBeanException
	 * @throws MBeanRegistrationException
	 * @throws InstanceAlreadyExistsException
	 */
	private <T> void register(final org.apache.wicket.Application application, final T o,
		final ObjectName objectName) throws InstanceAlreadyExistsException,
		MBeanRegistrationException, NotCompliantMBeanException
	{
		StandardMBean bean = new StandardMBean(o, (Class<T>)o.getClass().getInterfaces()[0]) {
			@Override
			public Object getAttribute(String attribute)
			{
				return withApplication(() -> super.getAttribute(attribute));
			}
			
			@Override
			public void setAttribute(Attribute attribute)
			{
				withApplication(() -> {
					super.setAttribute(attribute);
					return null;
				});
			}
			
			@Override
			public AttributeList setAttributes(AttributeList attributes)
			{
				return withApplication(() -> super.setAttributes(attributes));
			}
			
			@Override
			public Object invoke(String actionName, Object[] params, String[] signature)
			{
				return withApplication(() -> super.invoke(actionName, params, signature));
			}
			
			private <R> R withApplication(Callable<R> callable)
			{
				boolean existed = ThreadContext.exists();

				if (existed == false)
				{
					ThreadContext.setApplication(application);
				}

				try
				{
					return callable.call();
				}
				catch (Exception ex)
				{
					throw new WicketRuntimeException(ex);
				}
				finally
				{
					if (existed == false)
					{
						ThreadContext.detach();
					}
				}
			}
		};
		mbeanServer.registerMBean(bean, objectName);
		registered.add(objectName);
	}
}
