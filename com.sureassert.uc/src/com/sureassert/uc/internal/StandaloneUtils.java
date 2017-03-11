/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.sureassert.uc.builder.ProjectProcessEntity;
import com.sureassert.uc.builder.SaUCApplicationLaunchShortcut;
import com.sureassert.uc.runtime.BasicUtils;

/**
 * Utility methods for standalone builds.
 * 
 * @author Nathan Dolan
 */
public class StandaloneUtils {

	public static ProjectProcessEntity getStandaloneProjectProcessEntity() throws IOException, JavaModelException {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		String filePath = System.getProperty(SaUCApplicationLaunchShortcut.JAVA_PATH_ENV_VAR_NAME);
		String projectLocation = System.getProperty(SaUCApplicationLaunchShortcut.PROJECT_PATH_ENV_VAR_NAME);
		String projectName = System.getProperty(SaUCApplicationLaunchShortcut.PROJECT_NAME_ENV_VAR_NAME);

		if (filePath != null && !filePath.equals(projectLocation)) {
			// Build file
			BasicUtils.debug("Build starting for single file " + filePath);
			IProject project = workspace.getRoot().getProject(projectName);
			File srcFile = new File(filePath);
			IFile file = workspace.getRoot().findFilesForLocationURI(srcFile.toURI())[0];
			Set<IPath> affectedFiles = new HashSet<IPath>();
			affectedFiles.add(file.getFullPath());
			IJavaProject javaProject = getJavaProject(project);
			if (javaProject != null) {
				return new ProjectProcessEntity(javaProject, affectedFiles);
			}
		} else if (projectName != null) {
			// Build project
			BasicUtils.debug("Build starting for project " + projectName);
			IProject project = workspace.getRoot().getProject(projectName);
			IJavaProject javaProject = getJavaProject(project);
			if (javaProject != null) {
				return new ProjectProcessEntity(javaProject, null);
			}
		} else {
			// Build all projects in workspace
			BasicUtils.debug("Build starting for all projects in workspace");
			return new ProjectProcessEntity(null, null);
		}
		return null;
	}

	private static IJavaProject getJavaProject(IProject project) {

		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject != null && javaProject.exists()) {
			return javaProject;
		}
		return null;
	}
}
