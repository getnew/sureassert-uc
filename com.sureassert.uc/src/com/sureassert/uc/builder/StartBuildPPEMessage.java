/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.io.IOException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
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
public class StartBuildPPEMessage implements SAServerMessage<Integer> {

	private static final long serialVersionUID = 1L;

	/**
	 * The ppe to execute.
	 */
	private final SerializableProjectProcessEntity ppe;

	/**
	 * Create a new headless runner for Sureassert UC.
	 */
	public StartBuildPPEMessage(SerializableProjectProcessEntity ppe) {

		this.ppe = ppe;
	}

	/**
	 * Runs the builder using the given file, project or workspace.
	 * 
	 * @return The number of errors encountered, or -1 if the build could not be run.
	 * @throws IOException
	 * @throws JavaModelException
	 */
	public int run() {

		PersistentDataFactory.getInstance().resetNumStandaloneErrors();

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		SAUCBuilder builder = new SAUCBuilder(true);

		BuildExecutor executor = new BuildExecutor();
		SaUCPPEBuildJob buildJob = null;

		BasicUtils.debug("Build starting for PPE " + ppe);
		buildJob = executor.scheduleBuild(workspace, builder, new ProjectProcessEntity(ppe));
		;

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
