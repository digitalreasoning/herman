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

import net.peachjean.commons.test.junit.TmpDir;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FilteredClassLoaderTest
{
	@Rule
	public TmpDir tmpdir = new TmpDir();
	private File jarFile;
	
	@Before
	public void setUp() throws IOException
	{
		File parentDir = tmpdir.getDir();
		parentDir.mkdirs();
		jarFile = new File(parentDir, "example.jar");
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		JarCreater.createJar(jarFile, Arrays.<JarCreater.Entry> asList(
               new JarCreater.Entry(DummyClass2.class.getPackage().getName().replace(".", "/"), "DummyClass2.class", classLoader.getResource(DummyClass2.class.getName().replace(".", "/") + ".class")), 
               new JarCreater.Entry(DummyClass1.class.getPackage().getName().replace(".", "/"), "DummyClass1.class", classLoader.getResource(DummyClass1.class.getName().replace(".", "/") + ".class"))));
	
	}

	@Test
	public void testNegativeFilter() throws IOException, ClassNotFoundException
	{
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		FilteredClassLoader filteredLoader = new FilteredClassLoader(parentClassLoader, new String[0], new String[] { DummyClass1.class.getName() });

		URLClassLoader ucl = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, filteredLoader);
		Class<?> dummyClass1 = ucl.loadClass(DummyClass1.class.getName());
		Class<?> dummyClass2 = ucl.loadClass(DummyClass2.class.getName());
		assertTrue(dummyClass1.getClassLoader() == ucl);
		assertTrue(dummyClass2.getClassLoader() == parentClassLoader);
	}

	@Test
	public void testPositiveFilter() throws IOException, ClassNotFoundException
	{
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		FilteredClassLoader filteredLoader = new FilteredClassLoader(parentClassLoader, new String[] { DummyClass1.class.getName(), "java.*" }, new String[0]);

		URLClassLoader ucl = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, filteredLoader);
		Class<?> dummyClass1 = ucl.loadClass(DummyClass1.class.getName());
		Class<?> dummyClass2 = ucl.loadClass(DummyClass2.class.getName());
		assertTrue(dummyClass1.getClassLoader() == parentClassLoader);
		assertTrue(dummyClass2.getClassLoader() == ucl);
	}
}
