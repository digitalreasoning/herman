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
	private final ServiceLoaderStrategy<S> serviceLoaderStrategy;
	private final boolean loadFromLocal;

	private IsolatedServiceLoader(Class<S> service, ClassLoader classLoader, Map<URL, List<URL>> serviceJars, String[] excludes, final String[] includes,
	                              final ServiceLoaderStrategy<S> serviceLoaderStrategy, final boolean loadFromLocal)
	{
		this.service = service;
		this.classLoader = classLoader;
		this.serviceJars = serviceJars;
		this.excludes = excludes;
		this.includes = includes;
		this.serviceLoaderStrategy = serviceLoaderStrategy;
		this.loadFromLocal = loadFromLocal;
		this.serviceClassLoaders = new HashMap<URL, URLClassLoader>();
	}

	@Override
	public Iterator<S> iterator()
	{
		return new Iterator<S>() {
			final Iterator<ClassLoaderSource> iterator;
			Iterator<S> serviceIterator = null;

			{
				List<ClassLoaderSource> sources = new ArrayList<ClassLoaderSource>();
				for(Map.Entry<URL, List<URL>> entry: serviceJars.entrySet())
				{
					sources.add(new IsolatedClassLoaderSource(classLoader, entry, serviceClassLoaders, includes, excludes));
				}
				if(loadFromLocal)
				{
					sources.add(new SimpleClassLoaderSource(classLoader));
				}
				iterator = sources.iterator();
				updateServiceIterator();
			}

			private void updateServiceIterator()
			{
				if(iterator.hasNext())
				{
					ClassLoaderSource classLoaderSource = iterator.next();

					ClassLoader previousContextClassloader = Thread.currentThread().getContextClassLoader();
                    ClassLoader serviceClassloader = classLoaderSource.getClassLoader();
					try
					{
						Thread.currentThread().setContextClassLoader(serviceClassloader);
						serviceIterator = serviceLoaderStrategy.runLoader(service, classLoaderSource.getClassLoader()).iterator();
					}
                    catch (Exception e)
                    {
                        throw new RuntimeException("Tried to load from classloader " + serviceClassloader, e);
                    }
                    catch (LinkageError e)
                    {
                        throw new RuntimeException("Tried to load from classloader " + serviceClassloader, e);
                    }
					finally
					{
						Thread.currentThread().setContextClassLoader(previousContextClassloader);
					}
				}
				else
				{
					serviceIterator = null;
				}
			}

			@Override
			public boolean hasNext()
			{
				return serviceIterator != null && serviceIterator.hasNext();
			}

			@Override
			public S next()
			{
				if(serviceIterator == null)
				{
					throw new NoSuchElementException("No more services.");
				}
				S next = serviceIterator.next();
				if(!serviceIterator.hasNext())
				{
					updateServiceIterator();
				}
				return next;
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
		private ServiceLoaderStrategy<S> serviceLoaderStrategy = new ServiceLoaderStrategy.Default<S>();
		private boolean loadFromLocal = false;

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

		public Builder<S> strategy(ServiceLoaderStrategy<S> serviceLoaderStrategy)
		{
			this.serviceLoaderStrategy = serviceLoaderStrategy;
			return this;
		}

		public Builder<S> loadFromLocal()
		{
			this.loadFromLocal = true;
			return this;
		}

		public Builder<S> noLoadFromLocal()
		{
			this.loadFromLocal = false;
			return this;
		}

		public IsolatedServiceLoader<S> build() throws IOException
		{
			this.excludes = this.excludes == null ? new String[0] : this.excludes;
			this.includes = this.includes == null ? new String[0] : this.includes;
			this.classLoader = this.classLoader == null ? Thread.currentThread().getContextClassLoader() : this.classLoader;
			ResourceFinder resourceFinder = new ResourceFinder(this.classLoader);
			Map<URL, List<URL>> serviceJars = resourceFinder.getNestedJars(ISOLATED_INTERFACE_PREFIX + this.service.getName());
			return new IsolatedServiceLoader<S>(this.service, this.classLoader, serviceJars, this.excludes, this.includes, serviceLoaderStrategy, loadFromLocal);
		}
	}

	private static interface ClassLoaderSource
	{
		ClassLoader getClassLoader();
	}

	private static class SimpleClassLoaderSource implements ClassLoaderSource
	{
		private final ClassLoader classLoader;

		private SimpleClassLoaderSource(final ClassLoader classLoader)
		{
			this.classLoader = classLoader;
		}

		@Override
		public ClassLoader getClassLoader()
		{
			return this.classLoader;
		}
	}

	private static class IsolatedClassLoaderSource implements ClassLoaderSource
	{
		private final ClassLoader parent;
		private final Map.Entry<URL, List<URL>> entry;
		private final Map<URL, URLClassLoader> classLoaderCache;
		private final String[] includes;
		private final String[] excludes;

		private IsolatedClassLoaderSource(final ClassLoader parent, final Map.Entry<URL, List<URL>> entry, final Map<URL, URLClassLoader> classLoaderCache,
		                                  final String[] includes, final String[] excludes)
		{
			this.parent = parent;
			this.entry = entry;
			this.classLoaderCache = classLoaderCache;
			this.includes = includes;
			this.excludes = excludes;
		}

		@Override
		public ClassLoader getClassLoader()
		{
			URLClassLoader ucl;
			if(classLoaderCache.containsKey(entry.getKey()))
			{
				ucl = classLoaderCache.get(entry.getKey());
			}else
			{
				List<URL> jarUrls = entry.getValue();
				ucl = new HermanClassLoader(jarUrls.toArray(new URL[jarUrls.size()]), new FilteredClassLoader(parent, includes, excludes), entry.getKey());
				classLoaderCache.put(entry.getKey(), ucl);
			}
			return ucl;
		}
	}
}
