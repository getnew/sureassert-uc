/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;

public class SAUCBuilder extends IncrementalProjectBuilder {

	public static final String PLUGIN_ID = "com.sureassert.uc";

	public static final String BUILDER_ID = "com.sureassert.uc.saucBuilder";

	public static final String NATURE_ID = "com.sureassert.uc.saucNature";

	private static BuildInterruptListenner buildInterruptListenner;

	private static int buildInterruptListennerNum = 1;

	public static boolean isStandaloneBuild;

	private IProject standaloneProject;

	public static SABuildQueueManager buildServer;

	// private LicenseNagRunner licenseNagRunner;

	// private transient Map<String, UseCaseExecutionCommand> saved;

	public SAUCBuilder() {

		this(false);
	}

	public SAUCBuilder(boolean isStandaloneBuild) {

		SAUCBuilder.isStandaloneBuild = isStandaloneBuild;
		PersistentDataFactory.getInstance().setIsStandaloneBuild(isStandaloneBuild);

		// NOTE: standalone build must load persistent data itself

		/*
		 * startBuildQueueManager();
		 * 
		 * // Load PersistentData for all projects in workspace on first build
		 * synchronized (firstBuild) {
		 * if (firstBuild) {
		 * firstBuild = false;
		 * if (!isStandaloneBuild) {
		 * BasicUtils.deleteSaUCTempDir();
		 * }
		 * /*
		 * if (!SaUCPreferences.isLicenseKeyValid()) {
		 * // Display license nag message
		 * EclipseUtils.displayDialog(LicenseNagRunner.LICENSE_NAG_MESSAGE_TITLE, //
		 * LicenseNagRunner.LICENSE_NAG_MESSAGE, false, IStatus.INFO);
		 * licenseNagRunner = new LicenseNagRunner();
		 * licenseNagRunner.start();
		 * }
		 *//*
			 * IWorkspace workspace = ResourcesPlugin.getWorkspace();
			 * ProjectOrder projectOrder =
			 * workspace.computeProjectOrder(workspace.getRoot().getProjects());
			 * try {
			 * initChecks(workspace);
			 * 
			 * // NOTE: standalone build must load persistent data itself
			 * if (!isStandaloneBuild) {
			 * EclipseUtils.registerPartListener(new ReportCoverageEditorPartListener());
			 * for (IProject thisProject : projectOrder.projects) {
			 * try {
			 * PersistentDataFactory.getInstance().load(thisProject.getName(), //
			 * EclipseUtils.getRawPath(thisProject).toFile(),//
			 * thisProject.getWorkspace().getRoot().getLocation().toFile());
			 * } catch (PersistentDataLoadException e) {
			 * BasicUtils.debug("No Sureassert UC data, or could not load, for project \"" +
			 * thisProject.getName() + "\"");
			 * }
			 * }
			 * }
			 * } catch (OperationCanceledException oce) {
			 * // NO-OP
			 * } catch (Throwable e) {
			 * EclipseUtils.reportError(e);
			 * }
			 * }
			 * }
			 */
	}

	public void setStandaloneProject(IProject project) {

		standaloneProject = project;
	}

	public IProject safeGetProject() {

		if (isStandaloneBuild) {
			return standaloneProject;
		} else {
			return super.getProject();
		}
	}

	class EADeltaVisitor implements IResourceDeltaVisitor {

		private final IProgressMonitor progressMonitor;

		public EADeltaVisitor(IProgressMonitor progressMonitor) {

			this.progressMonitor = progressMonitor;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
		 * .core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {

			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				startBuild(resource, delta, progressMonitor);
				// runJob(new BuildJob(resource, delta));
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				Set<IPath> removedJavaPaths = EclipseUtils.getAffectedFiles(delta, "java");
				for (IPath removeJavaPath : removedJavaPaths) {
					PersistentDataFactory.getInstance().setCurrentProject(resource.getName(), //
							EclipseUtils.getRawPath(safeGetProject()).toString());
					Set<String> classNames = PersistentDataFactory.getInstance().getClassNames(//
							removeJavaPath.toString());
					for (String className : classNames) {
						PersistentDataFactory.getInstance().unregisterAllClassData(className, null);
					}
				}
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				startBuild(resource, delta, progressMonitor);
				// runJob(new BuildJob(resource, delta));
				break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class SAUCResourceVisitor implements IResourceVisitor {

		private final IProgressMonitor progressMonitor;

		public SAUCResourceVisitor(IProgressMonitor progressMonitor) {

			this.progressMonitor = progressMonitor;
		}

		public boolean visit(IResource resource) {

			startBuild(resource, null, progressMonitor);
			// runJob(new BuildJob(resource, null));

			// return true to continue visiting children.
			return true;
		}
	}

	private void startBuild(IResource resource, IResourceDelta delta, IProgressMonitor progressMonitor) {

		// Filter out auto-refresh builds
		boolean isAutoRefresh = false;
		if (delta != null && delta.getKind() == IResourceDelta.CHANGED && delta.getAffectedChildren() != null && delta.getAffectedChildren().length == 1) {
			IResourceDelta deltaChild = delta.getAffectedChildren()[0];
			isAutoRefresh = deltaChild != null && deltaChild.getFullPath() != null && deltaChild.getFullPath().toString().endsWith(".sauc.obj");
		}
		if (isAutoRefresh) {
			BasicUtils.debug("Ignored auto-refresh build request");
			return;
		}

		if (SaUCPreferences.getIsExecConcurrent()) {
			// Schedule background build job with build server
			buildServer.scheduleBuildJob(buildServer.new SAUCBuildJob(safeGetProject(), isStandaloneBuild, resource, delta));
		} else {
			// Run in builder
			startBuildInterruptListenner(progressMonitor);
			try {
				SAUCBuildWorker worker = new SAUCBuildWorker(safeGetProject(), isStandaloneBuild, resource, delta, progressMonitor);
				worker.run();
			} catch (OperationCanceledException oce) {
				// NO-OP
			} catch (Throwable e) {
				EclipseUtils.reportError(e);
			} finally {
				stopBuildInterruptListenner();
			}
		}
	}

	public void startStandaloneBuild(ProjectProcessEntity projectProcessEntity, //
			Set<ProjectProcessEntity> alreadyProcessed, boolean hasProjectChanged, IProgressMonitor progressMonitor) {

		SAUCBuildWorker worker = new SAUCBuildWorker(safeGetProject(), isStandaloneBuild);
		worker.processJavaProject(projectProcessEntity, alreadyProcessed, hasProjectChanged, progressMonitor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {

		if (isStandaloneBuild) {
			return null;
		}

		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(safeGetProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	class BuildInterruptListenner implements Runnable {

		private boolean running = true;
		private final IProgressMonitor progressMonitor;

		public BuildInterruptListenner(IProgressMonitor progressMonitor) {

			this.progressMonitor = progressMonitor;
		}

		public void run() {

			while (!isInterrupted() && !progressMonitor.isCanceled() && running) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			if (isInterrupted() || progressMonitor.isCanceled()) {
				BasicUtils.debug("Build interrupted by user");
				PersistentDataFactory.getInstance().setBuildInterrupted();
			}
			running = false;
		}
	}

	private synchronized void startBuildInterruptListenner(IProgressMonitor progressMonitor) {

		if (!isStandaloneBuild) {
			stopBuildInterruptListenner();
			buildInterruptListenner = new BuildInterruptListenner(progressMonitor);
			new Thread(buildInterruptListenner, "BuildInterruptListenner-" + buildInterruptListennerNum++).start();
		}
	}

	private void stopBuildInterruptListenner() {

		if (!isStandaloneBuild) {
			if (buildInterruptListenner != null)
				buildInterruptListenner.running = false;
		}
	}

	@Override
	public void finalize() {

		stopBuildInterruptListenner();
	}

	protected void fullBuild(final IProgressMonitor progressMonitor) throws CoreException {

		getProject().accept(new SAUCResourceVisitor(progressMonitor));
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor progressMonitor) throws CoreException {

		delta.accept(new EADeltaVisitor(progressMonitor));
	}

	public class BuildJob extends WorkspaceJob {

		IResource resource;
		IResourceDelta delta;

		public BuildJob(IResource resource, IResourceDelta delta) {

			super("Running Sureassert UC");
			setUser(true);
			this.resource = resource;
			this.delta = delta;
		}

		@Override
		public IStatus runInWorkspace(final IProgressMonitor monitor) {

			try {
				startBuild(resource, delta, monitor);
				monitor.done();
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Build finished");
			} catch (Exception e) {
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Error");
			}
		}
	}
}
