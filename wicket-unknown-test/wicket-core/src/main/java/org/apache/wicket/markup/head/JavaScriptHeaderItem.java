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
package org.apache.wicket.markup.head;

import java.util.Objects;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * Base class for all {@link HeaderItem}s that represent javascripts. This class mainly contains
 * factory methods.
 * 
 * @author papegaaij
 */
public abstract class JavaScriptHeaderItem extends AbstractCspHeaderItem
{
	private static final long serialVersionUID = 1L;

	/**
	 * An optional markup id to set on the rendered &lt;script&gt; HTML element for
	 * this header item
	 */
	private String markupId;

	/**
	 * @return unique id for the javascript element.
	 */
	public String getId()
	{
		return markupId;
	}

	/**
	 * Sets the markup id for this header item
	 * @param markupId
	 *            the markup id
	 * @return {@code this} object, for method chaining
	 */
	public JavaScriptHeaderItem setId(String markupId)
	{
		this.markupId = markupId;
		return this;
	}

	/**
	 * Creates a {@link JavaScriptReferenceHeaderItem} for the given reference.
	 * 
	 * @param reference
	 *            resource reference pointing to the javascript resource
	 * @param pageParameters
	 *            the parameters for this Javascript resource reference
	 * @param id
	 *            id that will be used to filter duplicate reference (it's still filtered by URL
	 *            too)
	 * @return A newly created {@link JavaScriptReferenceHeaderItem} for the given reference.
	 */
	public static JavaScriptReferenceHeaderItem forReference(ResourceReference reference,
		PageParameters pageParameters, String id)
	{
		return new JavaScriptReferenceHeaderItem(reference, pageParameters, id);
	}

	/**
	 * Creates a {@link JavaScriptReferenceHeaderItem} for the given reference.
	 * 
	 * @param reference
	 *            resource reference pointing to the JavaScript resource
	 * @param id
	 *            id that will be used to filter duplicate reference (it's still filtered by URL
	 *            too)
	 * @return A newly created {@link JavaScriptReferenceHeaderItem} for the given reference.
	 */
	public static JavaScriptReferenceHeaderItem forReference(ResourceReference reference,
		String id)
	{
		return forReference(reference, null, id);
	}

	/**
	 * Creates a {@link JavaScriptReferenceHeaderItem} for the given reference.
	 *
	 * @param reference
	 *            resource reference pointing to the JavaScript resource
	 * @return A newly created {@link JavaScriptReferenceHeaderItem} for the given reference.
	 */
	public static JavaScriptReferenceHeaderItem forReference(ResourceReference reference)
	{
		return forReference(reference, null, null);
	}

	/**
	 * Creates a {@link JavaScriptContentHeaderItem} for the given content.
	 * 
	 * @param javascript
	 *            javascript content to be rendered.
	 * @param id
	 *            unique id for the javascript element. This can be null, however in that case the
	 *            ajax header contribution can't detect duplicate script fragments.
	 * @return A newly created {@link JavaScriptContentHeaderItem} for the given content.
	 */
	public static JavaScriptContentHeaderItem forScript(CharSequence javascript, String id)
	{
		return new JavaScriptContentHeaderItem(javascript, id);
	}

	/**
	 * Creates a {@link JavaScriptUrlReferenceHeaderItem} for the given url.
	 * 
	 * @param url
	 *            context-relative url of the the javascript resource
	 * @return A newly created {@link JavaScriptUrlReferenceHeaderItem} for the given url.
	 */
	public static JavaScriptUrlReferenceHeaderItem forUrl(String url)
	{
		return forUrl(url, null);
	}

	/**
	 * Creates a {@link JavaScriptUrlReferenceHeaderItem} for the given url.
	 * 
	 * @param url
	 *            context-relative url of the the javascript resource
	 * @param id
	 *            id that will be used to filter duplicate reference (it's still filtered by URL
	 *            too)
	 * @return A newly created {@link JavaScriptUrlReferenceHeaderItem} for the given url.
	 */
	public static JavaScriptUrlReferenceHeaderItem forUrl(String url, String id)
	{
		return new JavaScriptUrlReferenceHeaderItem(url, id);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JavaScriptHeaderItem that = (JavaScriptHeaderItem) o;
		return Objects.equals(markupId, that.markupId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(markupId);
	}
}
