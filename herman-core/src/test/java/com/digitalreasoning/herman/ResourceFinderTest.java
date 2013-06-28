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
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.peachjean.commons.test.junit.TmpDir;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ResourceFinderTest
{
	@Rule
	public TmpDir tmpdir = new TmpDir();

	private ResourceFinder underTest;
	private static final String TEST_PACKAGE = "META-INF/isolated/test.package.Service";

	@Before
	public void setUp() throws Exception
	{
		HermanUrlStreamHandler.doRegister();
		File parentDir = tmpdir.getDir();
		parentDir.mkdirs();
		File jarFile1 = new File(parentDir, "example1.jar");
		File combJar = new File(parentDir, "combinedJar.jar");
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		List<JarCreater.Entry> entries = Arrays.<JarCreater.Entry> asList(
		                                                                  new JarCreater.Entry(DummyClass2.class.getPackage().getName().replace(".", "/"), "DummyClass2.class", classLoader.getResource(DummyClass2.class.getName().replace(".", "/") + ".class")), 
		                                                                  new JarCreater.Entry(DummyClass1.class.getPackage().getName().replace(".", "/"), "DummyClass1.class", classLoader.getResource(DummyClass1.class.getName().replace(".", "/") + ".class")));
		
		JarCreater.createJar(jarFile1, entries);
		
		JarCreater.createJar(combJar, Arrays.<JarCreater.Entry> asList(	new JarCreater.Entry(TEST_PACKAGE, "example1.jar", jarFile1.toURI().toURL())));
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {combJar.toURI().toURL()});
		underTest = new ResourceFinder(urlClassLoader);
	}

	@Test
	public void testGetGroupedFiles() throws IOException, ClassNotFoundException
	{
		Map<URL, List<URL>> files = underTest.getNestedJars(TEST_PACKAGE);
		assertTrue("Expected nested jars in " + TEST_PACKAGE, !files.isEmpty());
		List<URL> urls = files.entrySet().iterator().next().getValue();
		URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader().getParent());
		Class<?> clazz = classLoader.loadClass(DummyClass1.class.getName());
		assertTrue(clazz.getClassLoader() == classLoader);
	}
}
