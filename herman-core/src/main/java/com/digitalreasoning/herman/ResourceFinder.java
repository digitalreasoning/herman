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


/*
 * This class returns nested jars under the URL protocol jarjar. JarJarUrlStreamHandler will have to be registered inorder to access these resources.
 */
class ResourceFinder
{
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
					throw new IOException("Herman only supports jars at 'file:' and 'jar:' urls.  Not from " + location);
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException("Failed to read jar entries from : " + location, e);
			}
		}

		return resources;
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
