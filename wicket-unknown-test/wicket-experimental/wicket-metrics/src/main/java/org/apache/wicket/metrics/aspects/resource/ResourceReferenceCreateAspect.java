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
package org.apache.wicket.metrics.aspects.resource;

import org.apache.wicket.metrics.WicketMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Collects basic information about pages
 * 
 * @author Tobias Soloschenko
 *
 */
@Aspect
public class ResourceReferenceCreateAspect extends WicketMetrics
{

	/**
	 * Collects data how often a resource reference is created
	 * 
	 * @param joinPoint
	 *            the join point (resource reference) which is created
	 * @return the result of constructor
	 * @throws Throwable
	 *             might occur while creating a new resource reference
	 */
	@Around("execution(org.apache.wicket.request.resource.ResourceReference.new(..))")
	public Object aroundNew(ProceedingJoinPoint joinPoint) throws Throwable
	{
		return mark("core/resource/reference/create", joinPoint);
	}
}
