/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.digitalreasoning.herman;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Copied from FireWallClassLoader in apache cxf common.
 *
 * The goal here is to wrap an existing classloader and restrict what classes can be loaded from it.  It is important to note that it will NEVER filter classes or resources from
 * the extension or bootstrap classloader.
 */
class FilteredClassLoader extends ClassLoader
{
	private final String[] filters;
	private final String[] fnFilters;
	private final String[] negativeFilters;
	private final String[] negativeFNFilters;
	private final ClassLoader extensionClassLoader;

	public FilteredClassLoader(ClassLoader parent, String[] fs)
	{
		this(parent, fs, new String[0]);
	}

	public FilteredClassLoader(ClassLoader parent, String[] fs, String[] negativeFs)
	{
		super(parent);

		this.filters = processFilters(fs);
		this.negativeFilters = processFilters(negativeFs);

		this.fnFilters = filters2FNFilters(this.filters);
		this.negativeFNFilters = filters2FNFilters(this.negativeFilters);

		extensionClassLoader = getSystemClassLoader().getParent();
	}

	private static String[] processFilters(String[] fs)
	{
		if (fs == null || fs.length == 0)
		{
			return null;
		}

		String[] f = new String[fs.length];
		for (int i = 0; i < fs.length; i++)
		{
			String filter = fs[i];
			if (filter.endsWith("*"))
			{
				filter = filter.substring(0, filter.length() - 1);
			}
			f[i] = filter;
		}
		return f;
	}

	private static String[] filters2FNFilters(String[] fs)
	{
		if (fs == null || fs.length == 0)
		{
			return null;
		}

		String[] f = new String[fs.length];
		for (int i = 0; i < fs.length; i++)
		{
			f[i] = fs[i].replace('.', '/');
		}
		return f;
	}

	private boolean isExcluded(String name)
	{
		if(negativeFilters != null)
		{
			for(String negativeFilter: negativeFilters)
			{
				if (name.startsWith(negativeFilter))
				{
					return true;
				}
			}
		}
		return false;
	}

	private boolean isIncluded(String name)
	{
		if(isExcluded(name))
		{
			return false;
		}
		if (filters != null)
		{
			for(String filter: filters)
			{
				if (name.startsWith(filter))
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			// if no filters are defined, then we assume everything not excluded is included
			return true;
		}
	}

	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		try
		{
			extensionClassLoader.loadClass(name);
		}
		catch (ClassNotFoundException e)
		{
			// purposefully do nothing - since it's not in the system class loader, now we need to apply the filters.
		}
		if(isIncluded(name))
		{
			return super.loadClass(name, resolve);
		}
		else
		{
			throw new ClassNotFoundException(name);
		}
	}

	public URL getResource(String name)
	{
		URL resource = extensionClassLoader.getResource(name);
		if(resource == null && isIncluded(name))
		{
			resource = super.getResource(name);
		}
		return resource;
	}

	@Override
	public Enumeration<URL> getResources(final String name) throws IOException
	{
		if(isIncluded(name))
		{
			return super.getResources(name);
		}
		else
		{
			return extensionClassLoader.getResources(name);
		}
	}
}
