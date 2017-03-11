/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.pde.launching.OSGiLaunchConfigurationInitializer;
import org.eclipse.pde.ui.launcher.OSGiLaunchShortcut;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.sureassert.uc.EclipseUtils;

/**
 * Performs single click launching for Sureassert UC OSGi runner.
 */
public class SaUCApplicationLaunchShortcut extends OSGiLaunchShortcut {

	public static final String JAVA_PATH_ENV_VAR_NAME = "com.sureassert.uc.standalone.javaPath";
	public static final String PROJECT_PATH_ENV_VAR_NAME = "com.sureassert.uc.standalone.projectPath";
	public static final String PROJECT_NAME_ENV_VAR_NAME = "com.sureassert.uc.standalone.projectName";
	public static final String WORKSPACE_PATH_ENV_VAR_NAME = "com.sureassert.uc.standalone.workspacePath";
	public static final String IS_STANDALONE_BUILD_ENV_VAR_NAME = "com.sureassert.uc.standaloneBuild";
	public static final String IS_KILL_BUILD_ENV_VAR_NAME = "com.sureassert.uc.killBuild";

	@Override
	public void launch(ISelection selection, String mode) {

		if (selection != null && selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;
			Object selected = treeSelection.getFirstElement();
			if (selected != null && selected instanceof IJavaElement) {
				if (selected instanceof IJavaProject) {
					try {
						launch(((IJavaProject) selected).getProject().getWorkspace(), mode);
					} catch (CoreException e) {
						EclipseUtils.reportError(e);
					}
				}
				ICompilationUnit javaUnit = getCompUnit((IJavaElement) selected);
				if (javaUnit != null) {
					try {
						launch(javaUnit.getJavaProject().getProject().getWorkspace(), mode);
					} catch (CoreException e) {
						EclipseUtils.reportError(e);
					}
				}
			}
		}
	}

	@Override
	public void launch(IEditorPart editorPart, String mode) {

		IEditorInput input = editorPart.getEditorInput();
		IJavaElement javaElement = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (javaElement != null && javaElement instanceof ICompilationUnit) {
			ICompilationUnit javaUnit = (ICompilationUnit) javaElement;
			try {
				launch(javaUnit.getJavaProject().getProject().getWorkspace(), mode);
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		} else if (javaElement != null && javaElement instanceof IProject) {
			IProject project = (IProject) javaElement;
			try {
				launch(project.getWorkspace(), mode);
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		} else if (javaElement != null && javaElement instanceof IJavaProject) {
			IJavaProject javaProject = (IJavaProject) javaElement;
			try {
				launch(javaProject.getProject().getWorkspace(), mode);
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		}
		// ITextEditor textEditor = (ITextEditor) editorPart;
		// ISelection selectedRegion = textEditor.getSelectionProvider().getSelection();
	}

	private ICompilationUnit getCompUnit(IJavaElement el) {

		if (el == null)
			return null;
		else if (el instanceof ICompilationUnit)
			return (ICompilationUnit) el;
		else
			return getCompUnit(el.getParent());
	}

	/*
	 * protected void searchAndLaunch(IType javaType, String mode) {
	 * 
	 * if (search != null) {
	 * try {
	 * SourceFile sourceFile = new SourceFile(null);
	 * SourceModel sourceModel = new SourceModel(file, javaUnit, javaProject, classLoaders,
	 * smFactory);
	 * 
	 * types = AppletLaunchConfigurationUtils.findApplets(new ProgressMonitorDialog(getShell()),
	 * search);
	 * } catch (Exception e) {
	 * }
	 * IType type = null;
	 * if (types.length == 0) {
	 * MessageDialog.openInformation(getShell(), "Applet Launch", "No applets found.");
	 * } else if (types.length > 1) {
	 * type = chooseType(types, mode);
	 * } else {
	 * type = types[0];
	 * }
	 * if (type != null) {
	 * launch(type, mode);
	 * }
	 * }
	 * }
	 */
	public static ILaunchConfiguration createLaunch(IWorkspace workspace, boolean projectSpecificLaunch) throws CoreException {

		// Get launch
		String launchName = projectSpecificLaunch ? "Sureassert UC Selection Runner" : //
		"Sureassert UC Workspace Runner";
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType launchType = manager.getLaunchConfigurationType("org.eclipse.pde.ui.EquinoxLauncher");
		ILaunchConfiguration[] configurations = manager.getLaunchConfigurations();
		for (int i = 0; i < configurations.length; i++) {
			ILaunchConfiguration configuration = configurations[i];
			if (configuration.getName().equals(launchName)) {
				ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
				setSourcePath(workingCopy, workspace);
				return workingCopy.doSave();
			}
		}
		ILaunchConfigurationWorkingCopy workingCopy = launchType.newInstance(null, launchName);

		setSourcePath(workingCopy, workspace);

		OSGiLaunchConfigurationInitializer initializer = new OSGiLaunchConfigurationInitializer();
		initializer.initialize(workingCopy);
		workingCopy.setAttribute("default_auto_start", false);
		String targetBundles = workingCopy.getAttribute("target_bundles", "");
		targetBundles = targetBundles.replace("com.sureassert.uc.runner@default:default", "com.sureassert.uc.runner@5:true");
		targetBundles = targetBundles.replace("com.sureassert.uc.runner.init@default:default", "com.sureassert.uc.runner.init@2:true");
		workingCopy.setAttribute("target_bundles", targetBundles);

		// Specify a Main Type and Program Arguments
		File workspaceDir = workspace.getRoot().getLocation().toFile();
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, //
				"-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -data " + //
						workspaceDir.getAbsolutePath());

		// Specify JVM properties
		StringBuilder jvmProps = new StringBuilder("-D").append(IS_STANDALONE_BUILD_ENV_VAR_NAME).append("=true").append(//
				" -D").append(WORKSPACE_PATH_ENV_VAR_NAME).append("=\"${workspace_loc}\"");
		;
		if (projectSpecificLaunch) {
			jvmProps.append(" -D").append(JAVA_PATH_ENV_VAR_NAME).append("=\"${resource_loc}\"").append(//
					" -D").append(PROJECT_NAME_ENV_VAR_NAME).append("=\"${project_name}\"").append(//
					" -D").append(PROJECT_PATH_ENV_VAR_NAME).append("=\"${project_loc}\"");
		}
		jvmProps.append(" -Xms40m -Xmx128m");
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, jvmProps.toString());

		// Run launch
		return workingCopy.doSave();
	}

	private static void setSourcePath(ILaunchConfigurationWorkingCopy workingCopy, IWorkspace workspace) throws CoreException {

		// Specify sourcepath
		List<String> sourcepath = new ArrayList<String>();
		List<IRuntimeClasspathEntry> sourcepathEntries = new ArrayList<IRuntimeClasspathEntry>();
		List<ISourceContainer> sourceContainers = new ArrayList<ISourceContainer>();
		for (IProject thisProject : workspace.getRoot().getProjects()) {
			if (thisProject.isOpen()) {
				try {
					IJavaProject javaProject = JavaCore.create(thisProject);
					if (javaProject != null) {
						sourcepathEntries.add(JavaRuntime.newProjectRuntimeClasspathEntry(javaProject));
						sourceContainers.add(new JavaProjectSourceContainer(javaProject));
					}
				} catch (Exception e) {
					EclipseUtils.reportError(e);
				}
			}
		}
		sourceContainers.add(new DefaultSourceContainer());

		for (IRuntimeClasspathEntry sorucepathEntry : sourcepathEntries) {
			sourcepath.add(sorucepathEntry.getMemento());
		}

		SASourceLookupDirector sourceLookupDirector = new SASourceLookupDirector();
		sourceLookupDirector.setSourceContainers(sourceContainers.toArray(new ISourceContainer[sourceContainers.size()]));
		workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, "org.eclipse.pde.ui.launcher.PDESourceLookupDirector");
		workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, sourceLookupDirector.getMemento());
	}

	// protected void launch(IProject project, String mode) throws CoreException {
	//
	// DebugUITools.launch(createLaunch(project.getWorkspace(), project), mode);
	// }

	protected void launch(IWorkspace workspace, String mode) throws CoreException {

		DebugUITools.launch(createLaunch(workspace, true), mode);
	}

	static class SASourceLookupDirector extends AbstractSourceLookupDirector {

		public void initializeParticipants() {

		}
	}

}
