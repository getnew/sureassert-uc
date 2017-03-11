/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.ValueObject;

public class SABuildQueueManager implements Runnable {

	private final List<SAUCBuildJob> buildJobQueue = new ArrayList<SAUCBuildJob>();
	private final Set<SAUCBuildJob> buildJobs = new HashSet<SAUCBuildJob>();
	private BuildJobInterruptListenner buildInterruptListenner;
	private int buildInterruptListennerNum = 1;

	private boolean stopped = false;

	public void run() {

		while (!stopped) {

			while (buildJobQueue.isEmpty()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			try {
				// System.err.println("Running build job");
				SAUCBuildJob buildJob = buildJobQueue.remove(0);
				buildJobs.remove(buildJob);
				buildJob.run();
				// System.err.println("Finished build job.  " + buildJobQueue.size() +
				// " pending job(s)");
			} catch (OperationCanceledException oce) {
				// NO-OP
			} catch (Throwable e) {
				EclipseUtils.reportError(e);
			}
		}

	}

	public void scheduleBuildJob(SAUCBuildJob buildJob) {

		// Check that this build isn't the same as the last pending one (no point building the same
		// thing twice)
		// if (!buildJobQueue.isEmpty()) {
		// SAUCBuildJob oldBuildJob = buildJobQueue.get(buildJobQueue.size() - 1);
		// if (buildJob.delta == null && oldBuildJob.delta == null) {
		// if (buildJob.project.getName().equals(oldBuildJob.project.getName()))
		// return;
		// } else {
		// if (buildJob.delta != null && buildJob.delta.equals(oldBuildJob.delta)) {
		// return;
		// }
		// }
		// }
		if (!buildJobs.contains(buildJob)) {
			buildJobs.add(buildJob);
			buildJobQueue.add(buildJob);
		} else {
			BasicUtils.debug("Build job already on queue; ignoring");
		}
	}

	public void stopServer() {

		stopped = true;
	}

	public class SAUCBuildJob extends ValueObject {

		private static final long serialVersionUID = 1L;

		private final IProject project;

		private final boolean isStandaloneBuild;

		private final IResourceDelta delta;

		private final IResource resource;

		@Override
		protected Object[] getImmutableState() {

			return new Object[] { project == null ? null : project.getName(), resource == null ? null : resource.getName(), getDeltaState() };

		}

		private String[] getDeltaState() {

			if (delta == null)
				return null;
			Set<IPath> paths = EclipseUtils.getAffectedFiles(delta, null);
			String[] pathStrs = new String[paths.size() + 1];
			pathStrs[0] = Integer.toString(delta.getKind());
			int i = 0;
			for (IPath path : paths) {
				pathStrs[i++] = path == null ? "" : path.toString();
			}
			return pathStrs;
		}

		public SAUCBuildJob(IProject project, boolean isStandaloneBuild, IResource resource, IResourceDelta delta) {

			this.project = project;
			this.isStandaloneBuild = isStandaloneBuild;
			this.resource = resource;
			this.delta = delta;
		}

		public void run() {

			final SAUCBuildWorker worker = new SAUCBuildWorker(project, isStandaloneBuild, resource, delta, null);
			Job job = new Job("Sureassert UC") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {

					startBuildInterruptListenner(monitor);
					try {
						worker.setProgressMonitor(monitor);
						worker.run();
						return Status.OK_STATUS;
					} catch (OperationCanceledException oce) {
						return Status.OK_STATUS;
					} catch (Throwable e) {
						EclipseUtils.reportError(e);
						return Status.OK_STATUS;
					} finally {
						stopBuildInterruptListenner();
					}
				}
			};
			job.schedule();
			int sleep = 0;
			while (job.getResult() == null) {
				try {
					Thread.sleep(sleep);
					if (sleep < 100)
						sleep++;
				} catch (InterruptedException e) {
				}
			}
		}
	}

	class BuildJobInterruptListenner implements Runnable {

		private boolean running = true;
		private final IProgressMonitor progressMonitor;

		public BuildJobInterruptListenner(IProgressMonitor progressMonitor) {

			this.progressMonitor = progressMonitor;
		}

		public void run() {

			while (!progressMonitor.isCanceled() && running) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			if (progressMonitor.isCanceled()) {
				BasicUtils.debug("Build interrupted by user");
				PersistentDataFactory.getInstance().setBuildInterrupted();
			}
			running = false;
		}
	}

	private synchronized void startBuildInterruptListenner(IProgressMonitor progressMonitor) {

		if (!PersistentDataFactory.getInstance().isStandaloneBuild()) {
			stopBuildInterruptListenner();
			buildInterruptListenner = new BuildJobInterruptListenner(progressMonitor);
			new Thread(buildInterruptListenner, "BuildJobInterruptListenner-" + buildInterruptListennerNum++).start();
		}
	}

	private void stopBuildInterruptListenner() {

		if (!PersistentDataFactory.getInstance().isStandaloneBuild()) {
			if (buildInterruptListenner != null)
				buildInterruptListenner.running = false;
		}
	}

}
