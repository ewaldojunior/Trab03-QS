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
package org.apache.wicket.core.random;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.wicket.WicketRuntimeException;

/**
 * A very simple {@link ISecureRandomSupplier} that holds a {@code SecureRandom} using
 * {@code SHA1PRNG}. This {@code SecureRandom} is strong enough for generation of nonces with a
 * short lifespan, but might not be strong enough for generating long-lived keys. When your
 * application has stronger requirements on the random implementation, you should replace this class
 * by your own implementation.
 * 
 * @author papegaaij
 */
public class DefaultSecureRandomSupplier implements ISecureRandomSupplier
{
	private SecureRandom random;

	public DefaultSecureRandomSupplier()
	{
		try
		{
			random = SecureRandom.getInstance("SHA1PRNG");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new WicketRuntimeException(e);
		}
	}

	@Override
	public SecureRandom getRandom()
	{
		return random;
	}
}
