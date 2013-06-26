package com.digitalreasoning.herman;

import java.util.ServiceLoader;

public interface ServiceLoaderStrategy<S>
{
	Iterable<S> runLoader(Class<S> serviceType, ClassLoader classLoader);

	public static class Default<S> implements ServiceLoaderStrategy<S>
	{
		@Override
		public Iterable<S> runLoader(final Class<S> serviceType, final ClassLoader classLoader)
		{
			return ServiceLoader.load(serviceType, classLoader);
		}
	}
}
