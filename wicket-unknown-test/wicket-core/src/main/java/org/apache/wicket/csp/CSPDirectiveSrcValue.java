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
package org.apache.wicket.csp;

import org.apache.wicket.request.cycle.RequestCycle;

/**
 * An enum holding the default values for -src directives including the mandatory single quotes
 */
public enum CSPDirectiveSrcValue implements CSPRenderable
{
	NONE("'none'"),
	WILDCARD("*"),
	SELF("'self'"),
	UNSAFE_INLINE("'unsafe-inline'"),
	UNSAFE_EVAL("'unsafe-eval'"),
	STRICT_DYNAMIC("'strict-dynamic'"),
	NONCE("'nonce-%1$s'")
	{
		@Override
		public String render(ContentSecurityPolicySettings settings, RequestCycle cycle)
		{
			return String.format(getValue(), settings.getNonce(cycle));
		}
	};

	private final String value;

	CSPDirectiveSrcValue(String value)
	{
		this.value = value;
	}

	@Override
	public String render(ContentSecurityPolicySettings settings, RequestCycle cycle)
	{
		return value;
	}

	public String getValue()
	{
		return value;
	}
}
