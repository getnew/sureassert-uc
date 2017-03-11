/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.sureassert.uc.builder.SAUCBuilder;
import com.sureassert.uc.builder.WorkspaceProperties;

/**
 * The activator class controls the plug-in life cycle
 */
public class SaUCBuilderActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.sureassert.uc";

	public static final int VERSION_MAJOR = 0;

	public static final int VERSION_MINOR = 9;

	@SuppressWarnings("deprecation")
	public static final Date RELEASE_DATE = new Date(2011 - 1900, 05 - 1, 04);

	// The shared instance
	private static SaUCBuilderActivator plugin;

	/**
	 * The constructor
	 */
	public SaUCBuilderActivator() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {

		super.start(context);
		plugin = this;
		init(context);
	}

	private void init(BundleContext context) {

		// Determine which bundles are registered as auto-startup
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		try {
			IConfigurationElement[] startupBundles = RegistryFactory.getRegistry().getConfigurationElementsFor("org.eclipse.ui.startup");
			List<String> startupBundleNames = new ArrayList<String>();
			for (IConfigurationElement configEl : startupBundles) {
				startupBundleNames.add(configEl.getContributor().getName());
			}
			WorkspaceProperties props = WorkspaceProperties.read(new File(workspace.getRoot().getRawLocationURI()));
			props.setAutoStartupPlugins(startupBundleNames.toArray(new String[startupBundleNames.size()]));
			props.write();

		} catch (Exception e) {
			// Do not load EclipseUtils in Activator (can cause OSGi load error depending on start
			// order)
			System.err.println(ExceptionUtils.getFullStackTrace(e));
		}

		try {
			// For standalone builds, in case runner killed before backup restore...
			// NOTE: don't want to load SaUCApplicationLaunchShortcut
			String isStandaloneBuildStr = System.getProperty("com.sureassert.uc.standaloneBuild");
			boolean isStandaloneBuild = isStandaloneBuildStr != null && Boolean.parseBoolean(isStandaloneBuildStr);
			if (isStandaloneBuild) {
				WorkspaceProperties.restorePreInitPropFileBackups(EclipseUtils.getWorkspaceLocation(workspace).//
						getAbsolutePath());
			}

		} catch (Exception e) {
			// Do not load EclipseUtils in Activator (can cause OSGi load error depending on start
			// order)
			System.err.println(ExceptionUtils.getFullStackTrace(e));
		}
	}

	private void stopBuildServer() {

		if (SAUCBuilder.buildServer != null)
			SAUCBuilder.buildServer.stopServer();
	}

	/**
	 * Initializes a preference store with default preference values
	 * for this plug-in.
	 */
	@Override
	protected void initializeDefaultPreferences(IPreferenceStore store) {

		store.addPropertyChangeListener(new SaUCPropertyChangeListener());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		stopBuildServer();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static SaUCBuilderActivator getDefault() {

		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {

		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
