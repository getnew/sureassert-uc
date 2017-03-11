/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;

import com.sureassert.uc.builder.CoveragePrinter;
import com.sureassert.uc.builder.MarkerUtils;
import com.sureassert.uc.builder.SAUCBuilder;
import com.sureassert.uc.builder.SAUCEditorCoverageListenner;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;

public class SaUCPropertyChangeListener implements IPropertyChangeListener {

	private static final Object JOB_LOCK = new Object();

	public SaUCPropertyChangeListener() {

	}

	public void propertyChange(PropertyChangeEvent event) {

		if (event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_ERROR_THRESHOLD) || //
				event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_WARN_THRESHOLD)) {
			EclipseUtils.runJob(new RefreshJob(), false);

		} else if (event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_PROBLEMS_ENABLED)) {
			if ((Boolean) event.getNewValue() == false)
				EclipseUtils.runJob(new RemoveCoverageDecorationsJob(), false);
			EclipseUtils.runJob(new RefreshJob(), false);

		} else if (event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED) || //
				event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED) || //
				event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED)) {
			EclipseUtils.runJob(new RefreshJob(), false);

		} else if (event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_ENABLED) && //
				(Boolean) event.getNewValue() == false) {
			EclipseUtils.runJob(new RefreshJob(), false);
			EclipseUtils.runJob(new RemoveCoverageMarkersJob(), false);

		} else if (event.getProperty().equals(SaUCPreferences.PREF_KEY_COVERAGE_DISPLAY_ENABLED)) {
			if ((Boolean) event.getNewValue() == false)
				EclipseUtils.runJob(new RemoveCoverageMarkersJob(), false);
			else
				EclipseUtils.runJob(new RedrawCoverageMarkersJob(), false);

		} else if (event.getProperty().equals(SaUCPreferences.PREF_KEY_JUNIT_AUTOMATION)) {

			if ((Boolean) event.getNewValue() == true) {
				// EclipseUtils.runJob(new BuildJob(), true);
				EclipseUtils.displayDialog("Sureassert property changes", //
						"Perform a clean build (Project->Clean...) for changes to take effect.", //
						false, IStatus.INFO);
			} else {
				EclipseUtils.runJob(new RemoveJUnitMarkersJob(), false);
			}

		} else if (event.getProperty().equals(SaUCPreferences.PREF_KEY_LICENCE_EMAIL) || //
				event.getProperty().equals(SaUCPreferences.PREF_KEY_LICENCE_KEY) || //
				event.getProperty().equals(SaUCPreferences.PREF_KEY_EXEC_CONCURRENT)) {

			// No action required.

		} else {
			// Rebuild for
			// SaUCPreferences.PREF_KEY_COVERAGE_REQUIRED_THRESHOLD
			// SaUCPreferences.PREF_KEY_JUNIT_AUTOMATION
			//

			// EclipseUtils.runJob(new BuildJob(), true);
			EclipseUtils.displayDialog("Sureassert property changes", //
					"Perform a clean build (Project->Clean...) for changes to take effect.", //
					false, IStatus.INFO);
		}
	}

	public static class RemoveCoverageMarkersJob extends Job {

		public RemoveCoverageMarkersJob() {

			super("Removing coverage markers");
			setSystem(true);
		}

		@Override
		public IStatus run(final IProgressMonitor monitor) {

			synchronized (JOB_LOCK) {

				// Get all java files in all SAUC projects in workspace
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				JavaPathData javaPathData = new JavaPathData();
				ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());
				for (IProject project : projectOrder.projects) {
					try {
						if (project.hasNature(SAUCBuilder.NATURE_ID)) {
							for (IPath javaPath : EclipseUtils.getAllFiles(project, "java")) {
								IFile file = javaPathData.getFileQuick(javaPath, workspace);
								if (file.exists()) {
									file.deleteMarkers(SAUCEditorCoverageListenner.CODE_STUBBED_MARKER_TYPE, false, IResource.DEPTH_ZERO);
									file.deleteMarkers(SAUCEditorCoverageListenner.COVERAGE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
									file.deleteMarkers(SAUCEditorCoverageListenner.COVERAGE_REQUIRED_MARKER_TYPE, false, IResource.DEPTH_ZERO);
									file.deleteMarkers(SAUCEditorCoverageListenner.PARTIAL_COVERAGE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
								}
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Done");
			}
		}
	}

	public static class RedrawCoverageMarkersJob extends Job {

		public RedrawCoverageMarkersJob() {

			super("Marking test coverage");
			setSystem(true);
		}

		@Override
		public IStatus run(final IProgressMonitor monitor) {

			synchronized (JOB_LOCK) {
				CoveragePrinter covPrinter = new CoveragePrinter(new SAUCEditorCoverageListenner());
				SourceFileFactory sfFactory = new SourceFileFactory();
				JavaPathData jpd = new JavaPathData();
				covPrinter.printInfo(null, sfFactory, jpd, monitor, null, null);
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Done");
			}
		}
	}

	public static class RemoveJUnitMarkersJob extends Job {

		public RemoveJUnitMarkersJob() {

			super("Removing JUnit markers");
			setSystem(true);
		}

		@Override
		public IStatus run(final IProgressMonitor monitor) {

			synchronized (JOB_LOCK) {
				Set<IFile> files = new HashSet<IFile>();
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				JavaPathData javaPathData = new JavaPathData();
				ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());
				for (IProject project : projectOrder.projects) {
					try {
						if (project.hasNature(SAUCBuilder.NATURE_ID)) {
							for (IPath path : EclipseUtils.getAllFiles(project, "java")) {
								IFile file = javaPathData.getFile(path, workspace);
								files.add(file);
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				monitor.beginTask("Removing JUnit markers", 1);
				for (IFile file : files) {
					try {
						file.deleteMarkers(MarkerUtils.JUNIT_INFO_MARKER_TYPE, false, IResource.DEPTH_ZERO);
						file.deleteMarkers(MarkerUtils.JUNIT_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ZERO);
					} catch (CoreException e) {
						e.printStackTrace();
					}
					monitor.worked(1);
				}
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Done");
			}
		}
	}

	public static class RemoveCoverageDecorationsJob extends Job {

		public RemoveCoverageDecorationsJob() {

			super("Removing coverage decorations");
			setSystem(true);
		}

		@Override
		public IStatus run(final IProgressMonitor monitor) {

			synchronized (JOB_LOCK) {
				Set<IFile> files = new HashSet<IFile>();
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				JavaPathData javaPathData = new JavaPathData();
				ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());
				for (IProject project : projectOrder.projects) {
					try {
						if (project.hasNature(SAUCBuilder.NATURE_ID)) {
							for (IPath path : EclipseUtils.getAllFiles(project, "java")) {
								IFile file = javaPathData.getFile(path, workspace);
								files.add(file);
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				monitor.beginTask("Removing coverage decorations", 1);
				for (IFile file : files) {
					try {
						file.deleteMarkers(CoverageDecorator.MARKER_TYPE_COVERAGE_REPORT, false, IResource.DEPTH_ZERO);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				monitor.worked(1);
				EclipseUtils.refreshWorkbench(PlatformUI.getWorkbench());
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Done");
			}
		}
	}

	public static class RefreshJob extends WorkspaceJob {

		public RefreshJob() {

			super("Refreshing workspace");
			setUser(true);

		}

		@Override
		public IStatus runInWorkspace(final IProgressMonitor monitor) {

			try {

				IWorkspace workspace = ResourcesPlugin.getWorkspace();

				JavaPathData javaPathData = new JavaPathData();
				Set<IFile> files = new HashSet<IFile>();
				ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());
				for (IProject project : projectOrder.projects) {
					try {
						if (project.hasNature(SAUCBuilder.NATURE_ID)) {
							for (IPath path : EclipseUtils.getAllFiles(project, "java")) {
								IFile file = javaPathData.getFile(path, workspace);
								files.add(file);
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}

				for (IFile file : files) {
					try {
						file.deleteMarkers(CoverageDecorator.MARKER_TYPE_COVERAGE_REPORT, false, IResource.DEPTH_ZERO);

					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				EclipseUtils.refreshWorkbench(PlatformUI.getWorkbench());

				// for (IProject project : workspace.getRoot().getProjects()) {
				// project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				// }
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Build finished");
			} catch (Exception e) {
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Error");
			}
		}
	}

	public static class BuildJob extends WorkspaceJob {

		public BuildJob() {

			super("Building workspace");
			setUser(true);

		}

		@Override
		public IStatus runInWorkspace(final IProgressMonitor monitor) {

			try {
				// monitor.beginTask("Building workspace", 1);
				ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				// monitor.worked(1);
				//
				// Display.getDefault().asyncExec(new Runnable() {
				//
				// public void runInWorkspace() {
				//
				// try {
				// ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD,
				// monitor);
				// } catch (CoreException e) {
				// EclipseUtils.reportError(e);
				// }
				// }
				// });
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Build finished");
			} catch (Exception e) {
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Error");
			}
		}
	}
}
