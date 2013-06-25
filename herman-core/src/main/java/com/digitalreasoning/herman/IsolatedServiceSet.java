package com.digitalreasoning.herman;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class IsolatedServiceSet<S> implements Iterable<S>
{
	private static final String ISOLATED_INTERFACE_PREFIX = "META-INF/isolated/";
	private final Class<S> service;
	private final ClassLoader classLoader;
	private final Map<URL, List<URL>> serviceJars;
	private final Map<URL, URLClassLoader> serviceClassLoaders;
	private final String[] negativeFilters;
	private final String[] positiveFilters;

	private IsolatedServiceSet(Class<S> service, ClassLoader classLoader, Map<URL, List<URL>> serviceJars, String[] negativeFilters, final String[] positiveFilters)
	{
		this.service = service;
		this.classLoader = classLoader;
		this.serviceJars = serviceJars;
		this.negativeFilters = negativeFilters;
		this.positiveFilters = positiveFilters;
		this.serviceClassLoaders = new HashMap<URL, URLClassLoader>();
	}

	@Override
	public Iterator<S> iterator()
	{
		return new Iterator<S>() {
			Iterator<Map.Entry<URL, List<URL>>> iterator = serviceJars.entrySet().iterator();

			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public S next()
			{
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
					ucl = new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]), new FilteredClassLoader(classLoader, positiveFilters, negativeFilters));
					serviceClassLoaders.put(entry.getKey(), ucl);
				}
				
				ServiceLoader<S> serviceLoader = ServiceLoader.load(service, ucl);
				if (!serviceLoader.iterator().hasNext())
				{
					throw new ServiceConfigurationError(service.getName());
				}
				return serviceLoader.iterator().next();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();

			}
		};
	}

	public static <S> Loader<S> loader(Class<S> service)
	{
		return new Loader<S>(service);
	}

	public static class Loader<S>
	{
		private Class<S> service;
		private String[] negativeFilters;
		private String[] positiveFilters;
		private ClassLoader classLoader;

		private Loader(Class<S> service)
		{
			this.service = service;
		}

		public Loader<S> negativeFilters(String[] filters)
		{
			this.negativeFilters = filters;
			return this;
		}

		public Loader<S> positiveFilters(String[] filters)
		{
			this.positiveFilters = filters;
			return this;
		}

		public Loader<S> classLoader(ClassLoader classLoader)
		{
			this.classLoader = classLoader;
			return this;
		}

		public IsolatedServiceSet<S> load() throws IOException
		{
			this.negativeFilters = this.negativeFilters == null ? new String[0] : this.negativeFilters;
			this.positiveFilters = this.positiveFilters == null ? new String[0] : this.positiveFilters;
			this.classLoader = this.classLoader == null ? Thread.currentThread().getContextClassLoader() : this.classLoader;
			ResourceFinder resourceFinder = new ResourceFinder(this.classLoader);
			Map<URL, List<URL>> serviceJars = resourceFinder.getNestedJars(ISOLATED_INTERFACE_PREFIX + this.service.getName());
			return new IsolatedServiceSet<S>(this.service, this.classLoader, serviceJars, this.negativeFilters, this.positiveFilters);
		}
	}
}
