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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * This class returns nested jars under the URL protocol jarjar. JarJarUrlStreamHandler will have to be registered inorder to access these resources.
 */
class ResourceFinder
{
	private static final Logger logger = LoggerFactory.getLogger(ResourceFinder.class);

	static
	{
		HermanUrlStreamHandler.register();
	}

	private final ClassLoader classLoader;

	public ResourceFinder()
	{
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	public ResourceFinder(ClassLoader classLoader)
	{
		this.classLoader = classLoader;
	}

	public Map<URL, List<URL>> getNestedJars(String uri) throws IOException
	{
		Map<URL, List<URL>> resources = new HashMap<URL, List<URL>>();
		if (!uri.endsWith("/"))
		{
			uri += "/";
		}
		Enumeration<URL> urls = classLoader.getResources(uri);
		Pattern uriEnding = Pattern.compile("/?" + Pattern.quote(uri) + "$");

		while (urls.hasMoreElements())
		{
			URL location = urls.nextElement();

			try
			{
				if (location.getProtocol().equals("jar"))
				{
					List<URL> jarUrls = readJarEntries(location, uri);
					if(!jarUrls.isEmpty())
					{
						resources.put(location, jarUrls);
					}
				}
				else if (location.getProtocol().equals("file"))
				{
					File root;
					try {
						root = new File(location.toURI());
					} catch(URISyntaxException e) {
						root = new File(location.getPath());
					}
					final File[] jarFiles = root.listFiles(new FilenameFilter()
					{
						@Override
						public boolean accept(final File dir, final String name)
						{
							return name.endsWith(".jar");
						}
					});
					if(jarFiles != null)
					{
						List<URL> jarUrls = new ArrayList<URL>();
						for(File file: jarFiles)
						{
							jarUrls.add(file.toURI().toURL());
						}
						resources.put(location, jarUrls);
					}
				}
				else
				{
					final String spec = uriEnding.matcher(location.toString()).replaceAll("");
					URL jarUrl = new URL(spec);
					List<URL> jarUrls = null;
					try
					{
						jarUrls = readJarEntriesForArbitraryUrl(jarUrl, uri);
					}
					catch (RuntimeException e)
					{
						logger.warn("Got exception trying to read from " + jarUrl, e);
						throw e;
					}
					catch (Error e)
					{
						logger.warn("Got error trying to read from " + jarUrl, e);
						throw e;
					}
					if(!jarUrls.isEmpty())
					{
						resources.put(location, jarUrls);
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException("Failed to read jar entries from : " + location, e);
			}
		}

		return resources;
	}

	private List<URL> readJarEntriesForArbitraryUrl(final URL jarUrl, final String basePath) throws IOException
	{
		logger.debug("Unrecognized procotol, so we are going to try searching the jar at " + jarUrl);
		final InputStream in = jarUrl.openStream();
		final JarInputStream jarInputStream;
		if(in instanceof JarInputStream)
		{
			logger.debug("Url provided a JarInputStream, so we're going to use it.");
			jarInputStream = (JarInputStream) in;
		}
		else
		{
			logger.debug("Url provided something other than a JarInputStream, we will wrap it with a JarInputStream.");
			jarInputStream = new JarInputStream(in);
		}

		List<URL> entryUrls = new ArrayList<URL>();
		for(JarEntry entry = jarInputStream.getNextJarEntry(); entry != null; entry = jarInputStream.getNextJarEntry())
		{
			String entryName = entry.getName();
			logger.debug("Trying entry " + entryName);
			if (entry.isDirectory() || !entryName.startsWith(basePath) || entryName.length() == basePath.length())
			{
				continue;
			}

			String jarName = entryName.substring(basePath.length());

			if (jarName.contains("/"))
			{
				continue;
			}

			final URL resource;
			if(!jarUrl.toString().contains(HermanUrlStreamHandler.JAR_SEPARATOR))
			{
				resource = new URL(jarUrl.toString() + "/" + entryName);
			}
			else
			{
				resource = new URL(null, "herman:" + jarUrl.toString() + "/" + entryName + HermanUrlStreamHandler.HERMAN_SEPARATOR, HermanUrlStreamHandler.INSTANCE);
			}
			logger.debug("Found embedded jar " + resource + " inside jar " + jarUrl);
			entryUrls.add(resource);
		}
		logger.info("Found " + entryUrls.size() + " embedded jars in " + jarUrl);
		return entryUrls;
	}

	static List<URL> readJarEntries(URL location, String basePath) throws IOException {
        JarURLConnection conn = (JarURLConnection) location.openConnection();
        JarFile jarfile = null;
        jarfile = conn.getJarFile();

        Enumeration<JarEntry> entries = jarfile.entries();
        List<URL> entryUrls = new ArrayList<URL>();
        while (entries != null && entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (entry.isDirectory() || !name.startsWith(basePath) || name.length() == basePath.length()) {
                continue;
            }

            name = name.substring(basePath.length());

            if (name.contains("/")) {
                continue;
            }

	        URL resource = new URL(HermanUrlStreamHandler.PROTOCOL + ":" + location.toString() + name + HermanUrlStreamHandler.HERMAN_SEPARATOR);
            entryUrls.add(resource);
        }
        return entryUrls;
    }
}
