/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.HashSet;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class SaUCPPEBuildJob extends WorkspaceJob {

	private final SAUCBuilder builder;
	private final ProjectProcessEntity ppe;
	private boolean finished = false;

	SaUCPPEBuildJob(SAUCBuilder builder, ProjectProcessEntity ppe) {

		super("Running Sureassert UC Builder");
		this.builder = builder;
		this.ppe = ppe;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {

		builder.setStandaloneProject(ppe.getJavaProject().getProject());
		builder.startStandaloneBuild(ppe, new HashSet<ProjectProcessEntity>(), true, monitor);
		System.setProperty(SaUCApplicationLaunchShortcut.IS_KILL_BUILD_ENV_VAR_NAME, "true");
		finished = true;
		return Status.OK_STATUS;
	}

	public boolean isFinished() {

		return finished;
	}
}
