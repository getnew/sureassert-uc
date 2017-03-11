/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.sureassert.uc.builder.ReportCoverageEditorPartListener;
import com.sureassert.uc.builder.SABuildQueueManager;
import com.sureassert.uc.builder.SAUCBuilder;
import com.sureassert.uc.builder.SaUCApplicationLaunchShortcut;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.PersistentDataLoadException;

public class UCPluginStartup implements IStartup {

	public void earlyStartup() {

		// NOTE: only gets called if user (or SaUC!) hasn't disabled com.sureassert.uc as
		// an IStartup bundle
		Bundle bundle = Platform.getBundle(SAUCBuilder.PLUGIN_ID);
		try {
			bundle.start();
		} catch (BundleException e) {
			EclipseUtils.reportError(e);
		}

		String isStandaloneBuildStr = System.getProperty("com.sureassert.uc.standaloneBuild");
		boolean isStandaloneBuild = isStandaloneBuildStr != null && Boolean.parseBoolean(isStandaloneBuildStr);
		if (!isStandaloneBuild) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();

			SAUCBuilder.isStandaloneBuild = false;
			PersistentDataFactory.getInstance().setIsStandaloneBuild(false);

			startBuildQueueManager();

			// Load PersistentData for all projects in workspace on first build
			BasicUtils.deleteSaUCTempDir();
			/*
			 * if (!SaUCPreferences.isLicenseKeyValid()) {
			 * // Display license nag message
			 * EclipseUtils.displayDialog(LicenseNagRunner.LICENSE_NAG_MESSAGE_TITLE, //
			 * LicenseNagRunner.LICENSE_NAG_MESSAGE, false, IStatus.INFO);
			 * licenseNagRunner = new LicenseNagRunner();
			 * licenseNagRunner.start();
			 * }
			 */
			ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());
			try {

				EclipseUtils.registerPartListener(new ReportCoverageEditorPartListener());
				initChecks(workspace);

				for (IProject thisProject : projectOrder.projects) {
					try {
						PersistentDataFactory.getInstance().load(thisProject.getName(), //
								EclipseUtils.getRawPath(thisProject).toFile(),//
								thisProject.getWorkspace().getRoot().getLocation().toFile());
					} catch (PersistentDataLoadException e) {
						BasicUtils.debug("No Sureassert UC data, or could not load, for project \"" + thisProject.getName() + "\"");
					}
				}

			} catch (OperationCanceledException oce) {
				// NO-OP
			} catch (Throwable e) {
				EclipseUtils.reportError(e);
			}
		}
	}

	private void startBuildQueueManager() {

		if (SAUCBuilder.buildServer == null) {
			SAUCBuilder.buildServer = new SABuildQueueManager();
			new Thread(SAUCBuilder.buildServer, "BuildServer-1").start();
		}
	}

	private void initChecks(IWorkspace workspace) throws CoreException {

		// Create SaUC launchs
		if (workspace != null) {
			SaUCApplicationLaunchShortcut.createLaunch(workspace, true);
			SaUCApplicationLaunchShortcut.createLaunch(workspace, false);
		}
	}
}
