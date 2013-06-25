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

import java.net.URL;

/**
 * FireWallClassLoader in apache cxf common
 */
class FilteredClassLoader extends ClassLoader
{
	private final String[] filters;
	private final String[] fnFilters;
	private final String[] negativeFilters;
	private final String[] negativeFNFilters;

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

		boolean javaCovered = false;
		if (this.filters == null)
		{
			javaCovered = true;
		}
		else
		{
			for (int i = 0; i < this.filters.length; i++)
			{
				if (this.filters[i].equals("java."))
				{
					javaCovered = true;
				}
			}
		}

		if (this.negativeFilters != null)
		{
			String java = "java.";
			// try all that would match java: j, ja, jav, java and java.
			for (int i = java.length(); i >= 0; i--)
			{
				for (int j = 0; j < this.negativeFilters.length; j++)
				{
					if (negativeFilters[j].equals(java.substring(0, i)))
					{
						javaCovered = false;
					}
				}
			}
		}

		if (!javaCovered)
		{
			throw new SecurityException("It's unsafe to construct a " + "FilteredClassLoader that does not let the java. " + "package through.");
		}
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

	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		if (negativeFilters != null)
		{
			for (int i = 0; i < negativeFilters.length; i++)
			{
				if (name.startsWith(negativeFilters[i]))
				{
					throw new ClassNotFoundException(name);
				}
			}
		}

		if (filters != null)
		{
			for (int i = 0; i < filters.length; i++)
			{
				if (name.startsWith(filters[i]))
				{
					return super.loadClass(name, resolve);
				}
			}
		}
		else
		{
			return super.loadClass(name, resolve);
		}
		throw new ClassNotFoundException(name);
	}

	public URL getResource(String name)
	{
		if (negativeFNFilters != null)
		{
			for (int i = 0; i < negativeFNFilters.length; i++)
			{
				if (name.startsWith(negativeFNFilters[i]))
				{
					return null;
				}
			}
		}

		if (fnFilters != null)
		{
			for (int i = 0; i < fnFilters.length; i++)
			{
				if (name.startsWith(fnFilters[i]))
				{
					return super.getResource(name);
				}
			}
		}
		else
		{
			return super.getResource(name);
		}
		return null;
	}
}
