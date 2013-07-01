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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class HermanUrlStreamHandler extends URLStreamHandler
{
	public static final String PROTOCOL = "herman";
	public static final String JARJAR_SEPARATOR = "^/";
	public static final String JAR_SEPARATOR = "!/";

	static {
		doRegister();
	}

	static void doRegister()
	{
		URL.setURLStreamHandlerFactory(new HermanUrlStreamHandler.HermanURLStreamHandlerFactory());
	}

	protected Map<String, File> extractedJars = new HashMap<String, File>();

	public static void register()
	{
		// do nothing! - this still registers due to static block if this is the first time that this is called
	}

	public static class HermanURLStreamHandlerFactory implements URLStreamHandlerFactory
	{
		public URLStreamHandler createURLStreamHandler(String protocol)
		{
			if (protocol.equals(PROTOCOL))
			{
				return new HermanUrlStreamHandler();
			}
			return null;
		}
	}

	private File getEmbeddedJar(String url) throws IOException
	{
		File tempJar = extractedJars.get(url);
		try
		{
			if (tempJar == null)
			{
				URLConnection embededJarCon = new URL(url).openConnection();
				InputStream input = embededJarCon.getInputStream();
				tempJar = File.createTempFile(PROTOCOL + "-", ".jar");
				tempJar.deleteOnExit();
				OutputStream output = new FileOutputStream(tempJar);

				try
				{
					byte[] buffer = new byte[4096];
					int n = 0;
					while (-1 != (n = input.read(buffer)))
					{
						output.write(buffer, 0, n);
					}
				}
				finally
				{
			        try {
			            if (input != null) {
				            input.close();
			            }
			        } catch (IOException ioe) {
			            // ignore
			        }
			        try {
			            if (output != null) {
				            output.close();
			            }
			        } catch (IOException ioe) {
			            // ignore
			        }
				}

				extractedJars.put(url, tempJar);
			}
		}
		catch (Exception e)
		{
			throw new IOException("Failed to load embedded jar file " + url, e);
		}
		return tempJar;
	}

	public URLConnection openConnection(final URL url) throws IOException
	{
		String urlFile = URLDecoder.decode(url.getFile(), "UTF-8");
		String embeddedJarURL = urlFile;
		String resourcePath = "";

		int pos = urlFile.lastIndexOf(JARJAR_SEPARATOR);
		if (pos >= 0)
		{
			embeddedJarURL = urlFile.substring(0, pos);
			if (urlFile.length() > pos + JARJAR_SEPARATOR.length())
			{
				resourcePath = urlFile.substring(pos + JARJAR_SEPARATOR.length());
			}
		}

		if(!embeddedJarURL.startsWith(PROTOCOL))
		{
			URL u = new URL("jar:" + embeddedJarURL + JAR_SEPARATOR + resourcePath);
			return u.openConnection();
		}
		
		File tempJar = getEmbeddedJar(embeddedJarURL);
		URL u = new URL(PROTOCOL + ":" + tempJar.getCanonicalFile().toURI().toURL().toExternalForm() + JARJAR_SEPARATOR + resourcePath);
		return u.openConnection();

	}
}
