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
package org.apache.wicket.bean.validation;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IPropertyReflectionAwareModel;
import org.apache.wicket.model.PropertyModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Default property resolver. This resolver supports common Wicket models like the
 * {@link PropertyModel}, and other implementations of {@link IPropertyReflectionAwareModel}
 * 
 * @author igor
 * 
 */
public class DefaultPropertyResolver implements IPropertyResolver
{

	@Override
	public Property resolveProperty(FormComponent<?> component)
	{
		IPropertyReflectionAwareModel<?> delegate = ValidationModelResolver.resolvePropertyModelFrom(component);
		if (delegate == null)
		{
			return null;
		}
		
		String name;
		Method getter = delegate.getPropertyGetter();
		if (getter != null)
		{
			String methodName = getter.getName();
			if (methodName.startsWith("get"))
			{
				name = methodName.substring(3, 4).toLowerCase(Locale.ROOT) +
					methodName.substring(4);
			}
			else if (methodName.startsWith("is"))
			{
				name = methodName.substring(2, 3).toLowerCase(Locale.ROOT) +
						methodName.substring(3);
			}
			else
			{
				throw new WicketRuntimeException("Invalid name for a getter method: '"
						+ methodName + "'. It must start either with 'get' or 'is'.");
			}
			return new Property(getter.getDeclaringClass(), name);
		}

		Field field = delegate.getPropertyField();
		if (field != null)
		{
			return new Property(field.getDeclaringClass(), field.getName());
		}

		return null;
	}

}
