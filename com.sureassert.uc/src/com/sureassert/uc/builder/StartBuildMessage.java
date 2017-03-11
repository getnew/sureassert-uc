/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaModelException;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.saserver.SAServerMessage;

/**
 * Entry class for running headless builds.
 * 
 * @author Nathan Dolan
 * 
 */
public class StartBuildMessage implements SAServerMessage<Integer> {

	private static final long serialVersionUID = 1L;

	/**
	 * The source file to execute SaUC on. Null to execute the whole project.
	 */
	private final String filePath;

	/**
	 * Root path to the project directory; mandatory.
	 */
	private final String projectLocation;

	/**
	 * Name of the project; mandatory.
	 */
	private final String projectName;

	/**
	 * Creates a new runner with the
	 * 
	 * @param isOneHitBuild
	 * @param filePath
	 * @param projectLocation
	 * @param projectName
	 */
	public StartBuildMessage(String filePath, String projectLocation, String projectName) {

		this.filePath = filePath;
		this.projectLocation = projectLocation;
		this.projectName = projectName;
	}

	/**
	 * Create a new headless runner for Sureassert UC. Reads system properties to
	 * determine execution data; see field javadoc and SaUCApplicationLaunchShortcut constants for
	 * details.
	 */
	public StartBuildMessage() {

		filePath = System.getProperty(SaUCApplicationLaunchShortcut.JAVA_PATH_ENV_VAR_NAME);
		projectLocation = System.getProperty(SaUCApplicationLaunchShortcut.PROJECT_PATH_ENV_VAR_NAME);
		projectName = System.getProperty(SaUCApplicationLaunchShortcut.PROJECT_NAME_ENV_VAR_NAME);
	}

	/**
	 * Runs the builder using the given file, project or workspace.
	 * 
	 * @return The number of errors encountered, or -1 if the build could not be run.
	 * @throws IOException
	 * @throws JavaModelException
	 */
	public int run() {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		SAUCBuilder builder = new SAUCBuilder(true);

		ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());

		BuildExecutor executor = new BuildExecutor();
		SaUCBuildJob buildJob = null;

		if (filePath != null && !filePath.equals(projectLocation)) {
			// Build file
			BasicUtils.debug("Build starting for single file " + filePath);
			IProject project = workspace.getRoot().getProject(projectName);
			File srcFile = new File(filePath);
			IFile file = workspace.getRoot().findFilesForLocationURI(srcFile.toURI())[0];
			Set<IPath> affectedFiles = new HashSet<IPath>();
			affectedFiles.add(file.getFullPath());
			buildJob = executor.scheduleBuild(workspace, builder, new IProject[] { project }, affectedFiles, projectOrder);
		} else if (projectName != null) {
			// Build project
			BasicUtils.debug("Build starting for project " + projectName);
			IProject project = workspace.getRoot().getProject(projectName);
			buildJob = executor.scheduleBuild(workspace, builder, new IProject[] { project }, null, projectOrder);
		} else {
			// Build all projects in workspace
			BasicUtils.debug("Build starting for all projects in workspace");
			List<IProject> projects = new ArrayList<IProject>();
			for (IProject project : projectOrder.projects) {
				try {
					if (project.hasNature(SAUCBuilder.NATURE_ID)) {
						projects.add(project);
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			buildJob = executor.scheduleBuild(workspace, builder, projects.toArray(new IProject[] {}), null, projectOrder);
		}

		while (!buildJob.isFinished()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}

		int numErrors = PersistentDataFactory.getInstance().getNumStandaloneErrors();
		BasicUtils.debug("Sureassert UC builder finished");
		BasicUtils.debug("Total number of errors: " + numErrors);

		return numErrors;
	}

	public Integer execute() {

		return run();
	}

	public Class<Integer> getReturnType() {

		return Integer.class;
	}

}
