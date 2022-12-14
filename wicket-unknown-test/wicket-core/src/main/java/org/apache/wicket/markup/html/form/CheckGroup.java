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
package org.apache.wicket.markup.html.form;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.lang.Generics;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Component used to connect instances of Check components into a group. Instances of Check have to
 * be in the component hierarchy somewhere below the group component. The model of the CheckGroup
 * component has to be an instance of java.util.Collection. The model collection of the group is
 * filled with model objects of all selected Check components.
 *
 * <p>
 * i.e. <code>
 * &lt;span wicket:id="checkboxgroup"&gt;
 *   ...
 *   &lt;input type="checkbox" wicket:id="checkbox1"&gt;choice 1&lt;/input&gt;
 *   ...
 *   &lt;input type="checkbox" wicket:id="checkbox2"&gt;choice 2&lt;/input&gt;
 *   ...
 * &lt;/span&gt;
 * </code>
 * </p>
 * 
 * @see org.apache.wicket.markup.html.form.Check
 * @see org.apache.wicket.markup.html.form.CheckGroupSelector <p>
 *      Note: This component does not support cookie persistence
 * 
 * @author Igor Vaynberg
 * 
 * @param <T>
 *            The model object type
 */
public class CheckGroup<T> extends FormComponent<Collection<T>>
{
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(CheckGroup.class);

	/**
	 * Constructor that will create a default model collection
	 * 
	 * @param id
	 *            component id
	 */
	public CheckGroup(String id)
	{
		super(id);
		setRenderBodyOnly(true);
	}

	/**
	 * Constructor that wraps the provided collection with the org.apache.wicket.model.Model object
	 * 
	 * @param id
	 *            component id
	 * @param collection
	 *            collection to be used as the model
	 * 
	 */
	public CheckGroup(String id, Collection<T> collection)
	{
		this(id, new CollectionModel<T>(collection));
	}

	/**
	 * @param id
	 * @param model
	 * @see WebMarkupContainer#WebMarkupContainer(String, IModel)
	 */
	@SuppressWarnings("unchecked")
	public CheckGroup(String id, IModel<? extends Collection<T>> model)
	{
		super(id, (IModel<Collection<T>>)model);
		setRenderBodyOnly(true);
	}

	/**
	 * @see FormComponent#getModelValue()
	 */
	@Override
	protected String getModelValue()
	{
		final StringBuilder builder = new StringBuilder();

		final Collection<T> ts = getModelObject();

		visitChildren(Check.class, new IVisitor<Check<T>, Void>()
		{
			@Override
			public void component(Check<T> check, IVisit<Void> visit)
			{
				if (ts.contains(check.getModelObject()))
				{
					if (builder.length() > 0)
					{
						builder.append(VALUE_SEPARATOR);
					}
					builder.append(check.getValue());
				}
			}
		});

		return builder.toString();
	}

	@Override
	protected Collection<T> convertValue(String[] values) throws ConversionException
	{
		List<T> collection = Generics.newArrayList();

		/*
		 * if the input is null we do not need to do anything since the model collection has already
		 * been cleared
		 */

		if (values != null && values.length > 0)
		{
			for (final String value : values)
			{
				if (value != null)
				{
					Check<T> checkbox = visitChildren(Check.class,
						new org.apache.wicket.util.visit.IVisitor<Check<T>, Check<T>>()
						{
							@Override
							public void component(final Check<T> check, final IVisit<Check<T>> visit)
							{
								if (String.valueOf(check.getValue()).equals(value))
								{
									visit.stop(check);
								}
							}
						});

					if (checkbox == null)
					{
						throw new WicketRuntimeException(
							"submitted http post value [" +
								Strings.join(",", values) +
								"] for CheckGroup component [" +
								getPath() +
								"] contains an illegal value [" +
								value +
								"] which does not point to a Check component. Due to this the CheckGroup component cannot resolve the selected Check component pointed to by the illegal value. A possible reason is that component hierarchy changed between rendering and form submission.");
					}

					// assign the value of the group's model
					collection.add(checkbox.getModelObject());
				}
			}
		}
		return collection;
	}

	/**
	 * See {@link FormComponent#updateCollectionModel(FormComponent)} for details on how the model
	 * is updated.
	 */
	@Override
	public void updateModel()
	{
		FormComponent.updateCollectionModel(this);
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);

		// No longer applicable, breaks XHTML validation.
		tag.remove("disabled");
		tag.remove("name");
	}
}
