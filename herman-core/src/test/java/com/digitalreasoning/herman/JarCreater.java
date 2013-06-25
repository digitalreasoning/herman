package com.digitalreasoning.herman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;

public class JarCreater
{
	public static class Entry
	{
		private final String parentFolderName;
		private final String fileName;
		private final URL resource;

		public Entry(String parentFolderName, String fileName, URL resource)
		{
			this.parentFolderName = parentFolderName.endsWith("/") ? parentFolderName : parentFolderName + "/";
			this.fileName = fileName;
			this.resource = resource;
		}

	}

	public static void createJar(File outputJarFile, List<Entry> entries) throws IOException
	{
		if (!outputJarFile.getParentFile().exists())
		{
			outputJarFile.getParentFile().mkdirs();
		}
		if (!outputJarFile.exists())
		{
			outputJarFile.createNewFile();
		}
		
		FileOutputStream fOut = new FileOutputStream(outputJarFile);
		JarOutputStream jarOut = new JarOutputStream(fOut);
		Set<String> packageSet = new HashSet<String>();
		try
		{
			for (Entry folderFile : entries)
			{
				InputStream inputStream = folderFile.resource.openStream();

				try
				{
					if (!packageSet.contains(folderFile.parentFolderName))
					{
						jarOut.putNextEntry(new ZipEntry(folderFile.parentFolderName));
						jarOut.closeEntry();
						packageSet.add(folderFile.parentFolderName);
					}

					jarOut.putNextEntry(new ZipEntry(folderFile.parentFolderName + (folderFile.parentFolderName.endsWith("/") ? "" : "/") + folderFile.fileName));
					IOUtils.copy(inputStream, jarOut);
					jarOut.closeEntry();
				}
				finally
				{
					inputStream.close();
				}

			}
			
		}finally
		{
			jarOut.close();
			fOut.close();
		}
	}
}
