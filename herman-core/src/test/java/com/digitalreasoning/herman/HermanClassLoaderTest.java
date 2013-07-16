package com.digitalreasoning.herman;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import net.peachjean.commons.test.junit.TmpDir;

public class HermanClassLoaderTest {

    public static final String SERVICE = "com.digitalreasoning.harvest.io.IHarvestFactorySource";

	@Rule
	public TmpDir tmpDir = new TmpDir();

    @Test @Ignore
    public void testArtifact() throws IOException {
        final URL[] locations = getUrls();
        final ClassLoader parentLoader = new URLClassLoader(locations);
        final ResourceFinder resourceFinder = new ResourceFinder(parentLoader);
        final Map<URL,List<URL>> nestedJars = resourceFinder.getNestedJars(IsolatedServiceLoader.ISOLATED_INTERFACE_PREFIX + SERVICE);

        for(Map.Entry<URL, List<URL>> nested: nestedJars.entrySet())
        {
            final List<URL> urls = nested.getValue();
            final HermanClassLoader hermanClassLoader = new HermanClassLoader(urls.toArray(new URL[urls.size()]), HermanClassLoaderTest.class.getClassLoader(), nested.getKey(), new String[0], new String[0]);
            final URL resource = hermanClassLoader.getResource("META-INF/services/" + SERVICE);
	        for(String path: Arrays.asList("/", "/frank", "/frank/", "/blah.txt", "/frank/blah.txt", "META-INF/services/" + SERVICE))
	        {
		        try
		        {
			        System.out.println(path + " :::: " + hermanClassLoader.getResource(path));
		        }
		        catch (IOError e)
		        {
			        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		        }

	        }

            System.out.println("JAR:: " + nested.getKey());
            System.out.println("      " + resource);
        }
    }

	@Test @Ignore
	public void testFileUrls() throws IOException
	{
		URL[] locations = getUrls();

		URL[] classpathDirs = extractJars(locations);

		final ClassLoader parentLoader = new URLClassLoader(classpathDirs);

		final ResourceFinder resourceFinder = new ResourceFinder(parentLoader);
		final Map<URL,List<URL>> nestedJars = resourceFinder.getNestedJars(IsolatedServiceLoader.ISOLATED_INTERFACE_PREFIX + SERVICE);

		for(Map.Entry<URL, List<URL>> nested: nestedJars.entrySet())
		{
			final List<URL> urls = nested.getValue();
			final HermanClassLoader hermanClassLoader = new HermanClassLoader(urls.toArray(new URL[urls.size()]), HermanClassLoaderTest.class.getClassLoader(), nested.getKey(), new String[0], new String[0]);
			final URL resource = hermanClassLoader.getResource("META-INF/services/" + SERVICE);
			System.out.println("JAR:: " + nested.getKey());
			System.out.println("      " + resource);
		}
	}

	private URL[] extractJars(final URL[] locations) throws IOException
	{
		List<URL> classpathUrls = new ArrayList<URL>();
		for(URL location: locations)
		{
			File file = new File(location.getPath());
			File extractDir = new File(tmpDir.getDir(), file.getName() + "/");
			extractDir.mkdirs();
			ZipFile zipFile = new ZipFile(file);
			Enumeration entries;

				entries = zipFile.entries();

				while(entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry)entries.nextElement();

					if(entry.isDirectory()) {
						continue;
					}

					File out = new File(extractDir, entry.getName());
					out.getParentFile().mkdirs();

					copyInputStream(zipFile.getInputStream(entry),
					                new BufferedOutputStream(new FileOutputStream(out)));
				}

				zipFile.close();

			classpathUrls.add(extractDir.toURI().toURL());

		}

		return classpathUrls.toArray(new URL[classpathUrls.size()]);
	}

	public static final void copyInputStream(InputStream in, OutputStream out)
			throws IOException
	{
		byte[] buffer = new byte[1024];
		int len;

		while((len = in.read(buffer)) >= 0)
			out.write(buffer, 0, len);

		in.close();
		out.close();
	}

	private URL[] getUrls() throws MalformedURLException {
        return new URL[] {
                getUrl("hbase-cdh4"),
                getUrl("hbase"),
                getUrl("accumulo"),
                getUrl("cloudbase"),
                getUrl("cassandra"),
                getUrl("simple")
        };
    }

    private URL getUrl(final String impl) throws MalformedURLException {
        return new File(System.getProperty("user.home") + "/.m2/repository/com/digitalreasoning/harvest/harvest-execution-" + impl + "-isolated/3.21.0-SNAPSHOT/harvest-execution-" + impl + "-isolated-3.21.0-SNAPSHOT.jar").toURI().toURL();
    }
}
