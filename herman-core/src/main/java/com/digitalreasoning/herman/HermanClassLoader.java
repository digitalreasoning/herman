package com.digitalreasoning.herman;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

public class HermanClassLoader extends URLClassLoader
{
	private final URL isolatedJarLocation;

	public HermanClassLoader(final URL[] urls, final ClassLoader parent, final URL isolatedJarLocation, final String[] includes, final String[] excludes)
	{
		super(urls, new FilteredClassLoader(parent, includes, excludes));
		this.isolatedJarLocation = isolatedJarLocation;
	}

	@Override
	public String toString()
	{
		return "Herman ClassLoader from [[ " + isolatedJarLocation.toString() + " ]]";
	}
}
