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
package org.apache.wicket.arquillian.testing;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.wicket.Page;
import org.apache.wicket.arquillian.testing.pages.InsertContact;
import org.apache.wicket.arquillian.testing.pages.ListContacts;
import org.apache.wicket.arquillian.testing.util.ResourceWebApplicationPath;
import org.apache.wicket.cdi.CdiConfiguration;
import org.apache.wicket.cdi.ConversationPropagation;
import org.apache.wicket.protocol.http.WebApplication;


/**
 * Modified to create test with Arquillian looking for resources in an ServletContext.
 * 
 * @author Ondrej Zizka
 * @author felipecalmeida
 * 			Modified to create test with Arquillian looking for resources in an ServletContext.
 * @since 06/23/2015
 * 
 */
public class WicketJavaEEApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return ListContacts.class;
    }

    @Override
    protected void init() {
        super.init();

        // Enable CDI
        BeanManager bm;
        try {
            bm = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        } catch (NamingException e) {
            throw new IllegalStateException("Unable to obtain CDI BeanManager", e);
        }

        initResourceFinder();
        
        // Configure CDI, disabling Conversations as we aren't using them
        CdiConfiguration cdiConfiguration = new CdiConfiguration();
		cdiConfiguration.setPropagation(ConversationPropagation.NONE).configure(this);
        cdiConfiguration.setFallbackBeanManager(bm);
		
        // Mount the InsertContact page at /insert
        mountPage("/insert", InsertContact.class);
    }
    
    /**
     * Adding resource finder as we need (container web or during the phase test that reuses the container web).
     */
    protected void initResourceFinder() {
    	getResourceSettings().getResourceFinders().add(new ResourceWebApplicationPath(WicketJavaEEApplication.class.getPackage().getName(), getServletContext()));
    }

    
}
