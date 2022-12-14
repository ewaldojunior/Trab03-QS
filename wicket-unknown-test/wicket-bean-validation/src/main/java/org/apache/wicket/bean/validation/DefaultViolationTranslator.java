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

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.ValidationError;

/**
 * A default implementation of {@link IViolationTranslator}. The validation error is created with
 * the constraint violation's default error message. Further, the violation is checked for a message
 * key and if is found it is also added as a message key to the validation error. The keys are only
 * used if they are in the bean validation's default format of '{key}'.
 * 
 * @author igor
 */
public class DefaultViolationTranslator implements IViolationTranslator
{
	@Override
	public <T> ValidationError convert(ConstraintViolation<T> violation)
	{
		ConstraintDescriptor<?> desc = violation.getConstraintDescriptor();

		ValidationError error = new ValidationError();
		error.setMessage(violation.getMessage());

		List<String> messages = getViolationMessages(violation, desc);
		addErrorKeys(error, violation.getInvalidValue(), messages);

		for (String key : desc.getAttributes().keySet())
		{
			error.setVariable(key, desc.getAttributes().get(key));
		}
		
		return error;
	}

	private List<String> getViolationMessages(ConstraintViolation<?> violation,
		ConstraintDescriptor<?> desc)
	{
		String defaultMessage = (String)desc.getAttributes().get("message");
		String violationMessage = violation.getMessage();
		String violationMessageTemplate = violation.getMessageTemplate();		
		List<String> messages = new ArrayList<String>();

		//violation message is considered only if it is different from
		//the interpolated message
		if (!Strings.isEqual(violationMessage, violationMessageTemplate))
		{
			messages.add(violationMessageTemplate);
		}
		
		messages.add(violationMessage);
		
		//the default message is considered only if it is different from
		//the violation message template
		if (!Strings.isEqual(defaultMessage, violationMessageTemplate))
		{
			messages.add(defaultMessage);
		}

		return messages;
	}

	private void addErrorKeys(ValidationError error, Object invalidValue, List<String> messages)
	{
		for (String message : messages)
		{
			String messageKey = getMessageKey(message);

			if (messageKey != null)
			{
				if (invalidValue != null)
				{
					error.addKey(messageKey + "." + invalidValue.getClass().getSimpleName());
				}

				error.addKey(messageKey);
			}
		}
	}

	private String getMessageKey(String message)
	{
		if (!Strings.isEmpty(message) && message.startsWith("{") && message.endsWith("}"))
		{
			return message.substring(1, message.length() - 1);
		}
	
		return null;
	}
}
