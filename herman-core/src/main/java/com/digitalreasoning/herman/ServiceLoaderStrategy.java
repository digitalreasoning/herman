package com.digitalreasoning.herman;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ServiceLoaderStrategy<S>
{
	Iterable<S> runLoader(Class<S> serviceType, ClassLoader classLoader);

	public static class Default<S> implements ServiceLoaderStrategy<S>
	{
		private static final Logger logger = LoggerFactory.getLogger(Default.class);

		@Override
		public Iterable<S> runLoader(final Class<S> serviceType, final ClassLoader classLoader)
		{
			logger.debug("Looking for services in classloader: " + classLoader);
			try
			{
				final Enumeration<URL> resources = classLoader.getResources("META-INF/services/" + serviceType.getName());
				while(resources.hasMoreElements())
				{
					logger.debug("Looking for services in resource " + resources.nextElement());
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException("Failed to load services: " + serviceType, e);
			}
			return ServiceLoader.load(serviceType, classLoader);
		}
	}
}
