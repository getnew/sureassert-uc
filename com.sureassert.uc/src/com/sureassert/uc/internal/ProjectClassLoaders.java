/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import _sauc.SAInterceptor;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.InstrumentationSession;
import com.sureassert.uc.interceptor.ClassPath;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.Timer;

public class ProjectClassLoaders {

	public static final String TRANSFORMED_BIN_NAME = ".sabin";
	public static final String TRANSFORMED_SRC_NAME = ".sasrc";

	private static String lastSourceDirCache = "�^;init�^;";

	public final ClassLoader projectCL;
	public final ClassLoader transformedCL;
	private final Set<String> sourceDirs;

	private final Map<String, String> sourceDirToOutputBinaryDir;
	private final Map<String, String> binaryToTransformedPath;
	private final Map<String, String> sourceDirToTransformedBinaryDir;
	private final Map<String, String> sourceDirToTransformedSourceDir;

	public ProjectClassLoaders(ClassLoader projectCL, ClassLoader transformedCL, //
			Map<String, String> binaryToTransformedPath, Set<String> sourceDirs, //
			Map<String, String> sourceDirToOutputBinaryDir, Map<String, String> sourceDirToTransformedBinaryDir, //
			Map<String, String> sourceDirToTransformedSourceDir) {

		this.projectCL = projectCL;
		PersistentDataFactory.getInstance().setCurrentProjectClassLoader(transformedCL);
		this.transformedCL = transformedCL;
		this.binaryToTransformedPath = binaryToTransformedPath;
		this.sourceDirs = sourceDirs;
		this.sourceDirToOutputBinaryDir = sourceDirToOutputBinaryDir;
		this.sourceDirToTransformedBinaryDir = sourceDirToTransformedBinaryDir;
		this.sourceDirToTransformedSourceDir = sourceDirToTransformedSourceDir;

		initInterceptor(transformedCL);
	}

	public Set<String> getSourceDirs() {

		return Collections.unmodifiableSet(sourceDirs);
	}

	public String getBinaryDir(String sourceDir) {

		return sourceDirToOutputBinaryDir.get(sourceDir);
	}

	public String getTransformedBinaryDir(String sourceDir) {

		return sourceDirToTransformedBinaryDir.get(sourceDir);
	}

	public String getTransformedSourceDir(String sourceDir) {

		return sourceDirToTransformedSourceDir.get(sourceDir);
	}

	@SuppressWarnings("unused")
	private String findTransformedSourceDir(String sourceFilePath, StringBuffer relativePath) {

		if (sourceFilePath == null)
			return null;
		File sourceFile = new File(sourceFilePath);
		relativePath.insert(0, sourceFile.getName()).insert(0, File.separator);
		String parent = sourceFile.getParent();
		String transformedSourceDir = getTransformedSourceDir(parent);
		if (transformedSourceDir != null)
			return transformedSourceDir;
		else
			return findTransformedSourceDir(parent, relativePath);
	}

	public String findTransformedSourceFile(String sourceFile) {

		// try last matched source directory first
		synchronized (lastSourceDirCache) {
			if (lastSourceDirCache != null && sourceFile.startsWith(lastSourceDirCache)) {
				String transformedSrcDir = getTransformedSourceDir(lastSourceDirCache);
				if (transformedSrcDir != null)
					return getTransformedSourceDir(lastSourceDirCache) + sourceFile.substring(lastSourceDirCache.length());
			}
			for (String sourceDir : sourceDirs) {
				if (!sourceDir.equals(lastSourceDirCache) && sourceFile.startsWith(sourceDir)) {
					lastSourceDirCache = sourceDir;
					return getTransformedSourceDir(sourceDir) + sourceFile.substring(sourceDir.length());
				}
			}
			return null;
		}
	}

	public String findOriginalSourceFile(String transformedSourceFile) {

		// try last matched source directory first
		synchronized (lastSourceDirCache) {
			if (lastSourceDirCache != null) {
				String transformedSrcDir = getTransformedSourceDir(lastSourceDirCache);
				if (transformedSourceFile.startsWith(transformedSrcDir)) {
					// n src dirs -> 1 transformed src dir so need to check if file exists
					String path = lastSourceDirCache + transformedSourceFile.substring(transformedSrcDir.length());
					File file = new File(path);
					if (file.exists())
						return path;
				}
			}
			for (String sourceDir : sourceDirs) {
				String transformedSrcDir = getTransformedSourceDir(sourceDir);
				if (!sourceDir.equals(lastSourceDirCache) && transformedSourceFile.startsWith(transformedSrcDir)) {
					// n src dirs -> 1 transformed src dir so need to check if file exists
					String path = sourceDir + transformedSourceFile.substring(transformedSrcDir.length());
					File file = new File(path);
					if (file.exists()) {
						lastSourceDirCache = sourceDir;
						return path;
					}
				}
			}
			return null;
		}
	}

	public String findSourceDir(String sourceFile) {

		// try last matched source directory first
		synchronized (lastSourceDirCache) {
			if (lastSourceDirCache != null && sourceFile.startsWith(lastSourceDirCache)) {
				return lastSourceDirCache;
			}
			for (String sourceDir : sourceDirs) {
				if (!sourceDir.equals(lastSourceDirCache) && sourceFile.startsWith(sourceDir)) {
					lastSourceDirCache = sourceDir;
					return sourceDir;
				}
			}
			return null;
		}
	}

	public String getTransformedPathForClass(File classFile) {

		String classFilePath = classFile.getAbsolutePath();
		for (Entry<String, String> sourceToTransEntry : binaryToTransformedPath.entrySet()) {
			if (classFilePath.startsWith(sourceToTransEntry.getKey())) {
				return sourceToTransEntry.getValue();
			}
		}
		return null;
	}

	public synchronized static ProjectClassLoaders getClassLoaders(IJavaProject project, //
			boolean isFullBuild, InstrumentationSession session) throws JavaModelException, URISyntaxException, IOException {

		// Timer timer = new Timer("ProjectClassLoaders.getClassLoaders");
		// String projectName = project.getElementName();
		// ProjectClassLoaders projectCLs = projectClassLoaders.get(projectName);
		// if (projectCLs == null) {
		// projectCLs = getClassPathURLs(project).getClassLoaders();
		// projectClassLoaders.put(projectName, projectCLs);
		// }
		// return projectCLs;
		// if (projectClassLoaders == null) {
		Set<IJavaProject> processedProjects = new HashSet<IJavaProject>();
		ClassPath thisClassPath = getClassPathURLs(project, processedProjects);
		// if (!isFullBuild) {
		// Add client project CPs to this CP
		for (IJavaProject clientProject : getClientProjects(project)) {
			thisClassPath.addBinaryURLs(getClassPathURLs(clientProject, processedProjects));
		}
		return thisClassPath.getClassLoaders(session);
	}

	/**
	 * Gets all java projects in the workspace that depend on the given project
	 * 
	 * @throws JavaModelException
	 */
	private static Set<IJavaProject> getClientProjects(IJavaProject project) throws JavaModelException {

		Set<IJavaProject> clientProjects = new HashSet<IJavaProject>();
		for (IProject thisProject : project.getProject().getWorkspace().getRoot().getProjects()) {

			if (thisProject.getName().equals(project.getProject().getName()))
				continue;
			IJavaProject thisJavaProject = EclipseUtils.getJavaProject(thisProject);
			if (thisJavaProject == null)
				continue;

			IClasspathEntry[] cpEntries = thisJavaProject.getResolvedClasspath(true);
			for (int i = 0; i < cpEntries.length; i++) {
				if (cpEntries[i].getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					String dependsProjectName = cpEntries[i].getPath().lastSegment();
					if (project.getElementName().equals(dependsProjectName)) {
						clientProjects.add(thisJavaProject);
					}
				}
			}
		}
		return clientProjects;
	}

	public static void cleanTransformedDirs(IJavaProject javaProject) throws JavaModelException {

		File transformedBinDir = getSADirLocation(javaProject, TRANSFORMED_BIN_NAME);
		if (transformedBinDir.exists()) {
			try {
				BasicUtils.deleteDirectory(transformedBinDir);
			} catch (IOException e) {
				EclipseUtils.reportError("Clean problem: could not delete directory " + //
						transformedBinDir.getAbsolutePath() + //
						". Another program may have locked the directory. Check any open text editors and navigate away from the directory in any open file explorer.", //
						true);

			}
		}
		BasicUtils.mkdirs(transformedBinDir);

		File transformedSrcDir = getSADirLocation(javaProject, TRANSFORMED_SRC_NAME);
		if (transformedSrcDir.exists()) {
			try {
				BasicUtils.deleteDirectory(transformedSrcDir);
			} catch (IOException e) {
				EclipseUtils.reportError("Clean problem: could not delete directory " + //
						transformedSrcDir.getAbsolutePath() + //
						". Another program may have locked the directory. Check any open text editors and navigate away from the directory in any open file explorer.", //
						true);
			}
		}
		BasicUtils.mkdirs(transformedSrcDir);
	}

	private static File getSADirLocation(IJavaProject project, String saDirName) throws JavaModelException {

		return new File(EclipseUtils.getRawPath(project.getProject()).toFile(), saDirName);
	}

	private static ClassPath getClassPathURLs(IJavaProject project, Set<IJavaProject> processedProjects) throws JavaModelException, URISyntaxException, IOException {

		if (processedProjects.contains(project))
			return new ClassPath();

		processedProjects.add(project);

		Timer timer = new Timer("ProjectClassLoaders.getClassPathURLs for " + project.getElementName());

		IClasspathEntry[] cpEntries = project.getResolvedClasspath(true);
		ClassPath classPath = new ClassPath();
		File transformedBinFile = getSADirLocation(project, TRANSFORMED_BIN_NAME);
		BasicUtils.mkdirs(transformedBinFile);
		File transformedSrcFile = getSADirLocation(project, TRANSFORMED_SRC_NAME);
		BasicUtils.mkdirs(transformedSrcFile);
		for (int i = 0; i < cpEntries.length; i++) {

			IPath binaryPath = null;
			if (cpEntries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {

				File binaryFile = null;
				if (project.getProject().exists(cpEntries[i].getPath())) {
					binaryPath = EclipseUtils.getRawPath(cpEntries[i].getPath(), project.getProject());
					if (binaryPath != null)
						binaryFile = binaryPath.toFile();
				} else if ((binaryFile = cpEntries[i].getPath().toFile()).exists()) {
					binaryPath = cpEntries[i].getPath();
				} else {
					binaryPath = EclipseUtils.getProjectRootedPath(cpEntries[i].getPath(), project.getProject().getWorkspace());
					if (binaryPath != null)
						binaryFile = binaryPath.toFile();
				}
				if (binaryFile != null && binaryFile.exists())
					classPath.addLibraryURL(binaryFile.toURI().toURL());

			} else if (cpEntries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {

				// check source path segments for "test"; do not add test source to SU source path.
				// IPath sourcePath = cpEntries[i].getPath();
				boolean isTestSource = false;
				// for (int segmentIdx = 0; segmentIdx < sourcePath.segmentCount(); segmentIdx++) {
				// if (sourcePath.segment(segmentIdx).equals("test"))
				// isTestSource = true;
				// }
				if (!isTestSource) {
					IPath sourcePath = EclipseUtils.getRawPath(cpEntries[i].getPath(), project.getProject());
					binaryPath = EclipseUtils.getRawPath(cpEntries[i].getOutputLocation() == null ? //
					project.getOutputLocation() : cpEntries[i].getOutputLocation(), project.getProject());
					File binaryFile = binaryPath.toFile();
					classPath.addSourceURL(sourcePath.toFile(), binaryFile.toURI().toURL(), //
							binaryFile, transformedBinFile, transformedSrcFile);
				}

			} else if (cpEntries[i].getEntryKind() == IClasspathEntry.CPE_PROJECT) {

				String dependsProjectName = cpEntries[i].getPath().lastSegment();
				IProject dependsProject = project.getProject().getWorkspace().getRoot().//
						getProject(dependsProjectName);
				IJavaProject dependsJavaProject = EclipseUtils.getJavaProject(dependsProject);
				if (dependsJavaProject != null && !processedProjects.contains(dependsJavaProject))
					classPath.addBinaryURLs(getClassPathURLs(dependsJavaProject, processedProjects));
			}
		}
		timer.printExpiredTime();
		return classPath;
	}

	public void clear() {

		disposeInterceptor(transformedCL);
	}

	private static void initInterceptor(ClassLoader classLoader) {

		try {
			Class<?> clazz = classLoader.loadClass(SAInterceptor.class.getName());
			Field instanceField = clazz.getField("instance");
			Object saInterceptor = instanceField.get(null);
			Method initMethod = clazz.getMethod("init");
			initMethod.invoke(saInterceptor);
		} catch (Exception e) {
			EclipseUtils.reportError(e);
		}
	}

	private static void disposeInterceptor(ClassLoader classLoader) {

		try {
			Class<?> clazz = classLoader.loadClass(SAInterceptor.class.getName());
			Field instanceField = clazz.getField("instance");
			Object saInterceptor = instanceField.get(null);
			Method disposeMethod = clazz.getMethod("dispose");
			disposeMethod.invoke(saInterceptor);
		} catch (Exception e) {
			EclipseUtils.reportError(e);
		}
	}
}