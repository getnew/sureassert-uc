/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.sureassert.uc.builder.InstrumentationSession;
import com.sureassert.uc.internal.ProjectClassLoaders;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.SaUCRuntimeActivator;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.transform.SAUCTransformingClassLoader;

public class ClassPath {

	private final Set<String> sourceDirs = new LinkedHashSet<String>();
	private final Map<String, String> sourceDirToOutputBinaryDir = new HashMap<String, String>();
	private final Map<String, String> sourceDirToTransformedBinaryDir = new HashMap<String, String>();
	private final Map<String, String> sourceDirToTransformedSourceDir = new HashMap<String, String>();
	private final Set<URL> binaryURLs = new LinkedHashSet<URL>();
	private final Set<File> binaryFiles = new LinkedHashSet<File>();
	private final Set<URL> transformedBinaryURLs = new LinkedHashSet<URL>();
	private final Set<URL> libraryURLs = new LinkedHashSet<URL>();

	// private final Set<File> libraryFiles = new LinkedHashSet<File>();

	public ClassPath() throws URISyntaxException, IOException {

		// Add SaUC runtime plugin URL
		addLibraryURL(getPluginURL(SaUCRuntimeActivator.PLUGIN_ID, null));

		// Add saucAspectsJarFile
		URL saucAspectsJar = getPluginURL(SaUCRuntimeActivator.PLUGIN_ID, "/lib/com.sureassert.uc.aspects.jar");
		addLibraryURL(saucAspectsJar);

		// Add saucAspectsJarFile
		URL aspectJLibJar = getPluginURL(SaUCRuntimeActivator.PLUGIN_ID, "/lib/aspectjrt.jar");
		addLibraryURL(aspectJLibJar);
	}

	private URL getPluginURL(String pluginID, String filePath) throws URISyntaxException, IOException {

		URL fileURL = null;
		try {
			Bundle bundle = Platform.getBundle(pluginID);
			Path path = new Path("/");
			fileURL = FileLocator.find(bundle, path, null);
			fileURL = Platform.resolve(fileURL);
			fileURL = new URL(fileURL.toString().replace('\\', '/'));
			File file = null;
			if (fileURL.toString().startsWith("jar:file:")) {
				// Just want the JAR itself not a path to the root of its contents
				String jarLoc = fileURL.toString();
				int jarIndIdx = jarLoc.indexOf("!");
				if (jarIndIdx == -1)
					file = new File(jarLoc.substring(9));
				else
					file = new File(jarLoc.substring(9, jarIndIdx));
			} else {
				file = new File(fileURL.toURI());
			}
			URL url;
			if (file.isDirectory()) {
				// Plugin is a directory, should only be the case for dev mode
				// in which case append bin directory
				if (filePath != null) {
					// Add path to file within JAR
					String pluginURLStr = file.toURI().toURL().toString();
					pluginURLStr += filePath;
					url = new URL(pluginURLStr);
				} else {
					// Just path to plugin bin direct
					file = new File(file, "bin");
					url = file.toURI().toURL();
				}
			} else {
				// Plugin is a JAR
				if (filePath != null) {
					// Add path to file within JAR
					String pluginURLStr = file.toURI().toURL().toString();
					url = new URL("jar:" + pluginURLStr + "!" + filePath);
				} else {
					// Just path to plugin JAR
					url = file.toURI().toURL();
				}
			}

			// AspectJ cannot read from within JAR - extract to temp dir
			if (url.toString().startsWith("jar:")) {
				File extractFile = new File(BasicUtils.getSaUCTempDir(), filePath);
				if (!extractFile.exists())
					FileUtils.copyURLToFile(url, extractFile);
				url = extractFile.toURI().toURL();
			}

			return url;

		} catch (RuntimeException e) {
			if (fileURL == null)
				BasicUtils.debug("fileURL=null");
			else
				BasicUtils.debug("fileURL=" + fileURL.toString());
			throw e;
		}
	}

	public void addBinaryURLs(ClassPath cp) {

		// Do not add source paths as they are specific to a single project
		binaryURLs.addAll(cp.getBinaryURLs());
		binaryFiles.addAll(cp.getBinaryFiles());
		transformedBinaryURLs.addAll(cp.getTransformedBinaryURLs());
		libraryURLs.addAll(cp.getLibraryURLs());
		// libraryFiles.addAll(cp.getLibraryFiles());
	}

	public void addSourceURL(File sourceFile, URL binaryURL, File binaryFile, File transformedBinaryFile, File transformedSourceFile) throws URISyntaxException, MalformedURLException {

		sourceDirs.add(sourceFile.getAbsolutePath());
		sourceDirToTransformedBinaryDir.put(sourceFile.getAbsolutePath(), transformedBinaryFile.getAbsolutePath());
		sourceDirToTransformedSourceDir.put(sourceFile.getAbsolutePath(), transformedSourceFile.getAbsolutePath());
		binaryURLs.add(binaryURL);
		binaryFiles.add(binaryFile);
		sourceDirToOutputBinaryDir.put(sourceFile.getAbsolutePath(), new File(binaryURL.toURI()).getAbsolutePath());
		transformedBinaryURLs.add(transformedBinaryFile.toURI().toURL());
	}

	public void addLibraryURL(URL url) {

		libraryURLs.add(url);
		// libraryFiles.add(file);
	}

	public Set<String> getSourceDirs() {

		return Collections.unmodifiableSet(sourceDirs);
	}

	public Set<URL> getBinaryURLs() {

		return Collections.unmodifiableSet(binaryURLs);
	}

	public Set<File> getBinaryFiles() {

		return Collections.unmodifiableSet(binaryFiles);
	}

	public Set<URL> getTransformedBinaryURLs() {

		return Collections.unmodifiableSet(transformedBinaryURLs);
	}

	public Set<URL> getLibraryURLs() {

		return Collections.unmodifiableSet(libraryURLs);
	}

	// public Set<File> getLibraryFiles() {
	//
	// return Collections.unmodifiableSet(libraryFiles);
	// }

	public ProjectClassLoaders getClassLoaders(InstrumentationSession session) {

		// TODO: Clean project .sinbin directory

		Timer timer = new Timer("ClassPath.getClassLoaders");

		URL[] sourceURLArray = binaryURLs.toArray(new URL[binaryURLs.size()]);
		URL[] transformedSourceURLArray = transformedBinaryURLs.toArray(new URL[transformedBinaryURLs.size()]);
		URL[] libraryURLArray = libraryURLs.toArray(new URL[libraryURLs.size()]);
		Map<String, String> binaryToTransformedPath = new HashMap<String, String>();

		// ProjectCL = LibraryCL + SourceCL
		// TransformingCL = LibraryCL + TransformingCL(SourceCL)
		// for (URL sourceURL : sourceURLs) {
		// if (libraryURLs.contains(sourceURL))
		// throw new RuntimeException("SHIT!");
		// }

		for (File file : binaryFiles) {
			// if (file.exists() && file.canRead())
			// classPool.appendClassPath(file.getAbsolutePath());
			File transFile = new File(file.getParent(), ProjectClassLoaders.TRANSFORMED_BIN_NAME);
			binaryToTransformedPath.put(file.getAbsolutePath(), transFile.getAbsolutePath());
		}
		// for (File file : libraryFiles) {
		// if (file.exists() && file.canRead())
		// classPool.appendClassPath(file.getAbsolutePath());
		// }

		// ClassLoader sourceCL = new URLClassLoader(sourceURLArray, getClass().getClassLoader());
		// ClassLoader transformedSourceCL = new URLClassLoader(transformedSourceURLArray,
		// getClass().getClassLoader());
		// ClassLoader libraryCL = new URLClassLoader(libraryURLArray, getClass().getClassLoader());

		ClassLoader pluginCL = getClass().getClassLoader();
		// ClassLoader projectCL = new SAUCProjectClassLoader(//
		// (URL[]) ArrayUtils.addAll(sourceURLArray, libraryURLArray), null, pluginCL);
		// ClassLoader transformedCL = new SAUCProjectClassLoader(//
		// (URL[]) ArrayUtils.addAll(transformedSourceURLArray, libraryURLArray), null, pluginCL);

		// NOTE: Must add projectURLs *after* transformed URLs in transformedCL
		// to ensure classes from referenced non-SAUC-enabled projects (without .sabin) can be
		// loaded.
		URL[] projectURLs = (URL[]) ArrayUtils.addAll(sourceURLArray, libraryURLArray);
		URL[] transformedURLs = (URL[]) ArrayUtils.addAll(transformedSourceURLArray, projectURLs);
		ClassLoader transformedCL = new SAUCTransformingClassLoader(transformedURLs, null, pluginCL, session);
		ClassLoader projectCL = new SAUCProjectClassLoader(projectURLs, null, pluginCL);
		// ClassLoader projectCL = transformedCL;
		projectCL.setDefaultAssertionStatus(true);
		transformedCL.setDefaultAssertionStatus(true);

		timer.printExpiredTime();

		return new ProjectClassLoaders(projectCL, transformedCL, binaryToTransformedPath, //
				sourceDirs, sourceDirToOutputBinaryDir, sourceDirToTransformedBinaryDir, //
				sourceDirToTransformedSourceDir);
	}
}
