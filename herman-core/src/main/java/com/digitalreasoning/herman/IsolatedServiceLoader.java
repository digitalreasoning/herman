/**
 * Copyright 2013 Digital Reasoning Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.digitalreasoning.herman;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class IsolatedServiceLoader<S> implements Iterable<S>
{
	private static final String ISOLATED_INTERFACE_PREFIX = "META-INF/isolated/";
	private final Class<S> service;
	private final ClassLoader classLoader;
	private final Map<URL, List<URL>> serviceJars;
	private final Map<URL, URLClassLoader> serviceClassLoaders;
	private final String[] excludes;
	private final String[] includes;

	private IsolatedServiceLoader(Class<S> service, ClassLoader classLoader, Map<URL, List<URL>> serviceJars, String[] excludes, final String[] includes)
	{
		this.service = service;
		this.classLoader = classLoader;
		this.serviceJars = serviceJars;
		this.excludes = excludes;
		this.includes = includes;
		this.serviceClassLoaders = new HashMap<URL, URLClassLoader>();
	}

	@Override
	public Iterator<S> iterator()
	{
		return new Iterator<S>() {
			Iterator<Map.Entry<URL, List<URL>>> iterator = serviceJars.entrySet().iterator();
			Iterator<S> serviceIterator = null;

			@Override
			public boolean hasNext()
			{
				return iterator.hasNext() || (serviceIterator != null && serviceIterator.hasNext());
			}

			@Override
			public S next()
			{
				if(serviceIterator != null)
				{
					if(serviceIterator.hasNext())
					{
						return serviceIterator.next();
					}
					else
					{
						serviceIterator = null;
					}
				}
				if (!iterator.hasNext())
				{
					throw new NoSuchElementException();
				}
				Map.Entry<URL, List<URL>> entry = iterator.next();
				
				URLClassLoader ucl;
				if(serviceClassLoaders.containsKey(entry.getKey()))
				{
					ucl = serviceClassLoaders.get(entry.getKey());
				}else
				{
					List<URL> jarUrls = entry.getValue();
					ucl = new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]), new FilteredClassLoader(classLoader, includes, excludes));
					serviceClassLoaders.put(entry.getKey(), ucl);
				}

				serviceIterator = ServiceLoader.load(service, ucl).iterator();
				if (!serviceIterator.hasNext())
				{
					throw new ServiceConfigurationError(service.getName());
				}
				return serviceIterator.next();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();

			}
		};
	}

	public static <S> Builder<S> builder(Class<S> service)
	{
		return new Builder<S>(service);
	}

	public static class Builder<S>
	{
		private Class<S> service;
		private String[] excludes;
		private String[] includes;
		private ClassLoader classLoader;

		private Builder(Class<S> service)
		{
			this.service = service;
		}

		private String[] asArray(final Iterable<String> filters)
		{
			List<String> list = new ArrayList();
			for(String filter: filters)
			{
				list.add(filter);
			}
			return list.toArray(new String[list.size()]);
		}

		public Builder<S> excludes(Iterable<String> filters)
		{
			this.excludes = asArray(filters);
			return this;
		}

		public Builder<S> includes(Iterable<String> filters)
		{
			this.includes = asArray(filters);
			return this;
		}

		public Builder<S> excludes(String... filters)
		{
			this.excludes = filters;
			return this;
		}

		public Builder<S> includes(String... filters)
		{
			this.includes = filters;
			return this;
		}

		public Builder<S> classLoader(ClassLoader classLoader)
		{
			this.classLoader = classLoader;
			return this;
		}

		public IsolatedServiceLoader<S> build() throws IOException
		{
			this.excludes = this.excludes == null ? new String[0] : this.excludes;
			this.includes = this.includes == null ? new String[0] : this.includes;
			this.classLoader = this.classLoader == null ? Thread.currentThread().getContextClassLoader() : this.classLoader;
			ResourceFinder resourceFinder = new ResourceFinder(this.classLoader);
			Map<URL, List<URL>> serviceJars = resourceFinder.getNestedJars(ISOLATED_INTERFACE_PREFIX + this.service.getName());
			return new IsolatedServiceLoader<S>(this.service, this.classLoader, serviceJars, this.excludes, this.includes);
		}
	}
}
