package com.digitalreasoning.herman;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

public class HermanClassLoaderTest {

    public static final String SERVICE = "com.digitalreasoning.harvest.io.IHarvestFactorySource";

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
            System.out.println("JAR:: " + nested.getKey());
            System.out.println("      " + resource);

        }
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
