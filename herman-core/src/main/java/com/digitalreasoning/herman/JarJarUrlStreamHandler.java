package com.digitalreasoning.herman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class JarJarUrlStreamHandler extends URLStreamHandler
{
	public static final String PROTOCOL = "jarjar";
	public static final String JARJAR_SEPARATOR = "^/";
	public static final String JAR_SEPARATOR = "!/";

	protected Map<String, File> extractedJars = new HashMap<String, File>();

	public static void register()
	{
		URL.setURLStreamHandlerFactory(new JarJarURLStreamHandlerFactory());
	}

	public static class JarJarURLStreamHandlerFactory implements URLStreamHandlerFactory
	{
		public URLStreamHandler createURLStreamHandler(String protocol)
		{
			if (protocol.equals(PROTOCOL))
			{
				return new JarJarUrlStreamHandler();
			}
			return null;
		}
	}

	private File getEmbeddedJar(String url) throws IOException
	{
		File tempJar = extractedJars.get(url);
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
		return tempJar;
	}

	public URLConnection openConnection(final URL url) throws IOException
	{
		String urlFile = url.getFile();
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
