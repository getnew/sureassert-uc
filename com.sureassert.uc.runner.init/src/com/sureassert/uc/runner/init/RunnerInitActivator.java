/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runner.init;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sureassert.uc.builder.SaUCApplicationLaunchShortcut;
import com.sureassert.uc.builder.WorkspaceProperties;
import com.sureassert.uc.runtime.BasicUtils;

public class RunnerInitActivator implements BundleActivator {

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

		RunnerInitActivator.context = bundleContext;

		// Disable workspace autorefresh - backup and re-write prefs file
		alterPrefs(WorkspaceProperties.UI_IDE_PREFS_PATH, WorkspaceProperties.UI_IDE_PREFS_BACKUP_PATH, new PrefsAlterer() {

			public String alter(String prefs) {

				if (prefs.contains("REFRESH_WORKSPACE_ON_STARTUP="))
					return prefs.replace("REFRESH_WORKSPACE_ON_STARTUP=true", "REFRESH_WORKSPACE_ON_STARTUP=false");
				else if (prefs.endsWith("\n"))
					return prefs + "REFRESH_WORKSPACE_ON_STARTUP=false\n";
				else
					return prefs + "\nREFRESH_WORKSPACE_ON_STARTUP=false\n";
			}
		});

		// Add all auto-start plugins to PLUGINS_NOT_ACTIVATED_ON_STARTUP - backup and re-write
		// prefs file
		alterPrefs(WorkspaceProperties.UI_WORKBENCH_PREFS_PATH, WorkspaceProperties.UI_WORKBENCH_PREFS_BACKUP_PATH, new PrefsAlterer() {

			public String alter(String prefs) throws Exception {

				String autoStartPluginsStr = "PLUGINS_NOT_ACTIVATED_ON_STARTUP=";
				WorkspaceProperties saProps = WorkspaceProperties.read(getWorkspaceLocation());
				String[] autoStartPlugins = saProps.getAutoStartPlugins();
				if (autoStartPlugins != null)
					autoStartPluginsStr += BasicUtils.unsplit(saProps.getAutoStartPlugins(), ";");

				if (prefs.contains("PLUGINS_NOT_ACTIVATED_ON_STARTUP=")) {
					return prefs.replaceAll("PLUGINS_NOT_ACTIVATED_ON_STARTUP\\=.*", autoStartPluginsStr);
				} else if (prefs.endsWith("\n")) {
					return prefs + autoStartPluginsStr;
				} else {
					return "\n" + prefs + autoStartPluginsStr;
				}
			}
		});

		BasicUtils.debug("Sureassert Runner initialized");
	}

	private void alterPrefs(String filePath, String backupFilePath, PrefsAlterer prefsAlterer) {

		try {
			File workspaceDir = getWorkspaceLocation();
			File prefsFile = new File(workspaceDir, filePath);
			File prefsBackup = new File(workspaceDir, backupFilePath);
			if (prefsBackup.exists())
				prefsBackup.delete();
			if (prefsFile.exists() && prefsFile.canRead()) {
				FileUtils.copyFile(prefsFile, prefsBackup);
				String prefs = FileUtils.readFileToString(prefsFile);
				prefs = prefsAlterer.alter(prefs);
				FileUtils.writeStringToFile(prefsFile, prefs);
				FileUtils.touch(prefsBackup);
			} else {
				System.err.println("Cannot read UI preferences file from " + prefsFile.getAbsolutePath());
			}
		} catch (Throwable e) {
			System.err.println(ExceptionUtils.getFullStackTrace(e));
		}
	}

	private File getWorkspaceLocation() {

		String workspacePath = System.getProperty(SaUCApplicationLaunchShortcut.WORKSPACE_PATH_ENV_VAR_NAME);
		if (workspacePath == null) {
			System.err.println("Must provide workspace location as system property via -D" + //
					SaUCApplicationLaunchShortcut.WORKSPACE_PATH_ENV_VAR_NAME);
			System.exit(-1);
		}
		return new File(workspacePath);
	}

	private interface PrefsAlterer {

		String alter(String prefs) throws Exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {

		RunnerInitActivator.context = null;
	}

}
