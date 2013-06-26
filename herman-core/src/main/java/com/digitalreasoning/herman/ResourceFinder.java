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
import java.net.JarURLConnection;
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
			}
			catch (Exception e)
			{
				throw new RuntimeException("Failed to read jar entries from : " + location, e);
			}
		}

		return resources;
	}

    private static List<URL> readJarEntries(URL location, String basePath) throws IOException {
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
            
            URL resource = new URL(JarJarUrlStreamHandler.PROTOCOL, location.getHost(), location.getPort(), JarJarUrlStreamHandler.PROTOCOL + ":" + location.getFile()
                                                                                                                                                                  .replace
		            (JarJarUrlStreamHandler
		                                                                                                                                                    .JAR_SEPARATOR,
                                                                                                                     JarJarUrlStreamHandler.JARJAR_SEPARATOR) + name + JarJarUrlStreamHandler.JARJAR_SEPARATOR);
            entryUrls.add(resource);
        }
        return entryUrls;
    }
}
