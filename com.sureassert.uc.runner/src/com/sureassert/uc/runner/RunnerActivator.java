/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runner;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.SaUCApplicationLaunchShortcut;
import com.sureassert.uc.builder.StartBuildMessage;
import com.sureassert.uc.builder.WorkspaceProperties;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.PersistentDataLoadException;

public class RunnerActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {

		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {

		boolean isStandaloneBuild = Boolean.parseBoolean(//
				System.getProperty(SaUCApplicationLaunchShortcut.IS_STANDALONE_BUILD_ENV_VAR_NAME));
		if (!isStandaloneBuild)
			return;

		RunnerActivator.context = bundleContext;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		String workspacePath = workspace.getRoot().getLocationURI().toString();
		BasicUtils.debug("Executing Sureassert UC builder in workspace: " + workspacePath);

		WorkspaceProperties.restorePreInitPropFileBackups(workspacePath);

		ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());

		// Load persistent data
		for (IProject thisProject : projectOrder.projects) {
			try {
				PersistentDataFactory.getInstance().load(thisProject.getName(), //
						EclipseUtils.getRawPath(thisProject).toFile(),//
						thisProject.getWorkspace().getRoot().getLocation().toFile());
			} catch (PersistentDataLoadException e) {
				BasicUtils.debug("No Sureassert UC data, or could not load, for project \"" + thisProject.getName() + "\"");
			}
		}

		// One-hit build execution mode
		// if (!isBuildServer) {
		int exitCode = -1;
		try {
			exitCode = new StartBuildMessage().run();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			exit(exitCode);
		}
		// }

		// Start build server mode
		// BuildServer server = BuildServer.getInstance();
		// server.waitFor(new KillBuildServerMessage());
		// exit(0);
	}

	private void exit(int exitCode) {

		System.exit(exitCode); // TODO: cannot shut down platform without errors from
		// org.eclipse.debug.ui and others
		try {
			System.out.println("Shutting down");
			try {
				Platform.getBundle("org.eclipse.osgi").stop(0);
				// EclipseStarter.getSystemBundleContext().getBundle().stop(0);
			} catch (Throwable e) {
			}
			EclipseStarter.shutdown();
			System.out.println("Shut down");
		} catch (Exception e) {
			System.err.println(ExceptionUtils.getFullStackTrace(e));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {

		RunnerActivator.context = null;
	}

}
