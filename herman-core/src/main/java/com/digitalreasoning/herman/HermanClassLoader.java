package com.digitalreasoning.herman;

import java.net.URL;
import java.net.URLClassLoader;

public class HermanClassLoader extends URLClassLoader
{
	private final URL isolatedJarLocation;
	public HermanClassLoader(final URL[] urls, final ClassLoader parent, final URL isolatedJarLocation)
	{
		super(urls, parent);
		this.isolatedJarLocation = isolatedJarLocation;
	}

	@Override
	public String toString()
	{
		return "Herman ClassLoader from [[ " + isolatedJarLocation.toString() + " ]]";
	}
}
