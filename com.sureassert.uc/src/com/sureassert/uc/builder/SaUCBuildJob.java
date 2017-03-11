/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class SaUCBuildJob extends WorkspaceJob {

	private final SAUCBuilder builder;
	private final IProject[] projects;
	private final Set<IPath> affectedFiles;
	private final ProjectOrder projectOrder;
	private boolean finished = false;

	SaUCBuildJob(SAUCBuilder builder, IProject[] projects, Set<IPath> affectedFiles, ProjectOrder projectOrder) {

		super("Running Sureassert UC Builder");
		this.builder = builder;
		this.projects = projects;
		this.affectedFiles = affectedFiles;
		this.projectOrder = projectOrder;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {

		// Execute builder on each project
		for (IProject project : projects) {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null && javaProject.exists()) {
				builder.setStandaloneProject(project);
				builder.startStandaloneBuild(new ProjectProcessEntity(javaProject, affectedFiles), //
						new HashSet<ProjectProcessEntity>(), true, monitor);
			} else {
				System.err.println("Project " + project.getName() + " not recognized or processable as a Java project.");
			}
		}
		System.setProperty(SaUCApplicationLaunchShortcut.IS_KILL_BUILD_ENV_VAR_NAME, "true");
		finished = true;
		return Status.OK_STATUS;
	}

	public boolean isFinished() {

		return finished;
	}
}
