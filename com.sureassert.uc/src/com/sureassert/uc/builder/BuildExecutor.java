package com.sureassert.uc.builder;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.runtime.IPath;

import com.sureassert.uc.runtime.BasicUtils;

public class BuildExecutor {

	/**
	 * Creates and schedules a build job for the given files in the given projects, using the given
	 * project order.
	 * A SaUCBuildJob representing the asychronous job is returned. The client should interrogate
	 * the returned SaUCBuildJob to determine build progress.
	 * 
	 * @param workspace
	 * @param builder
	 * @param projects
	 * @param affectedFiles
	 * @param projectOrder
	 * @return
	 */
	public SaUCBuildJob scheduleBuild(IWorkspace workspace, SAUCBuilder builder, IProject[] projects, Set<IPath> affectedFiles, ProjectOrder projectOrder) {

		if (workspace.isAutoBuilding())
			BasicUtils.debug("Waiting for workspace update...");

		SaUCBuildJob buildJob = new SaUCBuildJob(builder, projects, affectedFiles, projectOrder);
		buildJob.setRule(workspace.getRoot());
		buildJob.schedule();
		return buildJob;
	}

	/**
	 * Creates and schedules a build job for the give ProjectProcessEntity, using the given
	 * project order.
	 * A SaUCBuildJob representing the asychronous job is returned. The client should interrogate
	 * the returned SaUCBuildJob to determine build progress.
	 * 
	 * @param workspace
	 * @param builder
	 * @param projects
	 * @param affectedFiles
	 * @param projectOrder
	 * @return
	 */
	public SaUCPPEBuildJob scheduleBuild(IWorkspace workspace, SAUCBuilder builder, ProjectProcessEntity ppe) {

		if (workspace.isAutoBuilding())
			BasicUtils.debug("Waiting for workspace update...");

		SaUCPPEBuildJob buildJob = new SaUCPPEBuildJob(builder, ppe);
		buildJob.setRule(workspace.getRoot());
		buildJob.schedule();
		return buildJob;
	}
}
