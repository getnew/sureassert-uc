/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.runtime.BasicUtils;

/**
 * Encapsulates the .saprops file containing workspace-wide Sureassert UC data
 * not captured using the preference service.
 * 
 * @author Nathan Dolan
 */
public class WorkspaceProperties {

	public static final String UI_IDE_PREFS_PATH = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.ide.prefs";
	public static final String UI_IDE_PREFS_BACKUP_PATH = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.ide.saucbak";
	public static final String UI_WORKBENCH_PREFS_PATH = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.workbench.prefs";
	public static final String UI_WORKBENCH_PREFS_BACKUP_PATH = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.workbench.saucbak";
	public static final String SAUC_PREFS_PATH = ".metadata/.plugins/org.eclipse.core.runtime/.settings/com.sureassert.uc.prefs";

	private static String PROP_NAME_AUTO_START_PLUGINS = "autoStartPlugins";

	public static final String SUREASSERT_UC_BUILDER_PLUGIN_ID = "com.sureassert.uc";

	private final String propsFilePath;
	private String[] autoStartPlugins;

	private WorkspaceProperties(Properties props, String propsFilePath) {

		this.propsFilePath = propsFilePath;

		String autoActivatingPluginsStr = props.getProperty(PROP_NAME_AUTO_START_PLUGINS);
		if (autoActivatingPluginsStr == null)
			autoStartPlugins = null;
		else
			autoStartPlugins = autoActivatingPluginsStr.split(",");
	}

	public static WorkspaceProperties read(File workspaceLocation) throws IOException {

		File propsDir = new File(workspaceLocation, ".metadata/.plugins/" + //
				SUREASSERT_UC_BUILDER_PLUGIN_ID + "/");
		File propsFile = new File(propsDir, "sauc.props");
		propsDir.mkdirs();
		propsFile.createNewFile();
		Properties props = new Properties();
		InputStream in = new BufferedInputStream(new FileInputStream(propsFile));
		try {
			props.load(in);
		} finally {
			in.close();
		}
		return new WorkspaceProperties(props, propsFile.getAbsolutePath());
	}

	public void write() throws IOException {

		File propsFile = new File(propsFilePath);
		propsFile.getParentFile().mkdirs();
		propsFile.createNewFile();
		Properties props = new Properties();
		props.put(PROP_NAME_AUTO_START_PLUGINS, BasicUtils.unsplit(autoStartPlugins, ","));

		OutputStream out = new BufferedOutputStream(new FileOutputStream(propsFile));
		try {
			props.store(out, "Sureassert UC internal properties");
		} finally {
			out.close();
		}
	}

	public String[] getAutoStartPlugins() {

		return autoStartPlugins;
	}

	public void setAutoStartupPlugins(String[] autoStartPlugins) {

		this.autoStartPlugins = autoStartPlugins;
	}

	public static void restorePreInitPropFileBackups(String workspacePath) {

		File uiIdePrefsBackupFile = new File(workspacePath, UI_IDE_PREFS_BACKUP_PATH);
		File uiIdePrefsFile = new File(workspacePath, UI_IDE_PREFS_PATH);
		File uiWorkbenchPrefsBackupFile = new File(workspacePath, UI_WORKBENCH_PREFS_BACKUP_PATH);
		File uiWorkbenchPrefsFile = new File(workspacePath, UI_WORKBENCH_PREFS_PATH);

		try {
			restorePreInitPropFileBackups(uiIdePrefsFile, uiIdePrefsBackupFile);
			restorePreInitPropFileBackups(uiWorkbenchPrefsFile, uiWorkbenchPrefsBackupFile);
		} catch (IOException ioe) {
			EclipseUtils.reportError(ioe);
		}
	}

	private static void restorePreInitPropFileBackups(File prefsFile, File prefsBackupFile) throws IOException {

		if (prefsBackupFile.exists()) {
			if (!prefsFile.exists()) {
				FileUtils.copyFile(prefsBackupFile, prefsFile);
			} else if (prefsBackupFile.lastModified() >= prefsFile.lastModified()) {
				if (prefsFile.delete())
					FileUtils.copyFile(prefsBackupFile, prefsFile);
			} else {
				System.err.println("Could not restore workspace preferences backup as user modified in interim.  " + //
						"Please check your workspace startup preferences are correct.");
			}
			prefsBackupFile.delete();
		}
	}

}