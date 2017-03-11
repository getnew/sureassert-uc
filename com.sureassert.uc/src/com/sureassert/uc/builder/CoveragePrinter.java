/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;

import com.sureassert.uc.CoverageDecorator;
import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.TestDouble;

public class CoveragePrinter {

	private final SAUCEditorCoverageListenner coverageListenner;

	public static final float NO_COVERAGE_REQUIRED = 101;

	public CoveragePrinter(SAUCEditorCoverageListenner coverageListenner) {

		this.coverageListenner = coverageListenner;
	}

	/**
	 * Marks coverage information on all open editor windows, or the given editor window
	 * (IWorkbenchPart) if specified.
	 * 
	 * @param coverage if null, mark all open windows; else mark only those for classes within the
	 *            given CoverageBuilder.
	 * @param sfFactory
	 * @param jpd
	 * @param monitor
	 * @param buildManager Can be null. If not null, uses the BuildManager to check if the build is
	 *            cancelled.
	 * @param part If null, mark all open windows; else mark only the the given window.
	 */
	public synchronized void printInfo(CoverageBuilder coverage, SourceFileFactory sfFactory, JavaPathData jpd, IProgressMonitor monitor, //
			BuildManager buildManager, IEditorInput filterEditorInput) {

		if (!SaUCPreferences.getIsCoverageDisplayEnabled())
			return;

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		int classIdx = 0;
		long numLines = 0;
		Set<String> openFilePaths = new HashSet<String>();

		// IProject[] projects = workspace.getRoot().getProjects();

		// Get open file editors
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null) {
			IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
			if (windows != null) {
				for (IWorkbenchWindow window : windows) {
					IWorkbenchPage page = window.getActivePage();
					if (page != null) {
						IEditorReference[] editors = page.getEditorReferences();
						if (editors != null) {
							for (IEditorReference editor : editors) {
								IEditorInput editorInput;
								try {
									editorInput = editor.getEditorInput();
									if (editorInput != null && editorInput instanceof IPathEditorInput && (filterEditorInput == null || filterEditorInput.equals(editorInput))) {
										IPath path = ((IPathEditorInput) editorInput).getPath();
										if (path != null) {
											openFilePaths.add(path.toString());
										}
									}
								} catch (Exception e) {
									EclipseUtils.reportError(e, false);
								}
							}
						}
					}
				}
			}
		}

		// Get classes covered by this test run
		Set<String> coveredClasses = new HashSet<String>();
		if (coverage != null) {
			Collection<IClassCoverage> iCCs = coverage.getClasses();
			for (IClassCoverage iCC : iCCs) {
				coveredClasses.add(BasicUtils.getClassNameFromBinaryClassName(iCC.getName()));
			}
		}

		try {
			monitor.beginTask("Marking test coverage", openFilePaths.size());

			// Iterate open editors; update coverage for any files containing classes that have
			// been covered by this run
			for (String absFilePath : openFilePaths) {

				if (buildManager != null && buildManager.isBuildCancelled(monitor))
					return;

				int percentComplete = (int) (((float) classIdx / (float) openFilePaths.size()) * 100f);

				if (monitor != null) {
					monitor.setTaskName(String.format("Marking test coverage in %d files (%d%s)", openFilePaths.size(), percentComplete, "%"));
				}

				IFile file = jpd.getFileForAbsolutePath(new Path(absFilePath), workspace);
				IPath javaPath = file.getFullPath();

				if (file != null && file.exists()) {
					try {
						SourceFile sourceFile = sfFactory.getSourceFile(file);
						Set<String> classNames = pdf.getClassNames(javaPath.toString());
						TestDouble classDouble = null;

						for (String className : classNames) {
							
							// If this is is the path to a doubled class, look up the Test Double class
							TestDouble thisClassDouble = pdf.getClassDoubleForDoubledClassName(className);
							if (thisClassDouble != null) {
								classDouble = thisClassDouble;
							}
						}

						if (classDouble == null) {
							for (String className : classNames) {
								
								if (coverage == null || coveredClasses.contains(className)) {
	
									float[] coveragePercentByLine = pdf.getCoverage(className);
	
									if (coveragePercentByLine != null) {
										for (int line = 0; line < coveragePercentByLine.length; line++) {
											float coveragePercent = coveragePercentByLine[line];
											coverageListenner.notifyCoverage(sourceFile, javaPath, //
													line, className, coveragePercent);
										}
									}
								}
							}
						}

					} catch (Exception e) {
						EclipseUtils.reportError(e, false);
					}
				}
				classIdx++;
				monitor.worked(1);
			}

			EclipseUtils.refreshWorkbench(workbench);
		} finally {
			monitor.done();
		}
	}
	

	/**
	 * Gets the % coverage for the given set of coverage lines.
	 * If there is no coverage data or no lines require coverage, returns null.
	 * 
	 * @param coverage
	 * @return coverage%
	 */
	public static Double getCombinedCoverage(float[] coverage) {

		if (coverage == null)
			return null;

		double combCov = 0;
		int numCovReqLines = 0;
		double thisCov;
		for (int covIdx = 0; covIdx < coverage.length; covIdx++) {
			thisCov = coverage[covIdx];
			if (thisCov != NO_COVERAGE_REQUIRED) {
				combCov += thisCov;
				numCovReqLines++;
			}
		}
		if (numCovReqLines == 0)
			return null;
		else
			return combCov / numCovReqLines;
	}

	/**
	 * Gets the % coverage for the given set of coverage lines.
	 * If there is no coverage data or no lines require coverage, returns null.
	 * 
	 * @param coverage
	 * @return i0=totalCov, i1=numCovReqLines
	 */
	public static Double[] getCombinedCoverageData(float[] coverage) {

		if (coverage == null)
			return null;

		double combCov = 0;
		int numCovReqLines = 0;
		double thisCov;
		for (int covIdx = 0; covIdx < coverage.length; covIdx++) {
			thisCov = coverage[covIdx];
			if (thisCov != NO_COVERAGE_REQUIRED) {
				combCov += thisCov;
				numCovReqLines++;
			}
		}
		if (numCovReqLines == 0)
			return null;
		else
			return new Double[] {combCov, (double)numCovReqLines};
	}

	
	public static double getProjectCoverage(String projectName) {
		
		if (projectName == null)
			return NO_COVERAGE_REQUIRED;
		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		Set<String> projectClasses = pdf.getAllProjectClasses(projectName);
		if (projectClasses == null)
			return NO_COVERAGE_REQUIRED;
		double totalCoverage = 0;
		int numCovReqLines = 0;
		for (String className : projectClasses) {
			Double[] classCoverage = getCombinedCoverageData(pdf.getCoverage(className));
			if (classCoverage != null) {
				totalCoverage += classCoverage[0];
				numCovReqLines += classCoverage[1];
			}
		}
		if (numCovReqLines == 0)
			return NO_COVERAGE_REQUIRED;
		else
			return totalCoverage / (double)numCovReqLines;
	}

	/**
	 * Combines the 2 coverage arrays into a single coverage array (additive)
	 * 
	 * @param cov1
	 * @param cov2
	 * @return the combined coverage array, the size of max(cov1,cov2)
	 */
	public static float[] combine(float[] cov1, float[] cov2) {

		if (cov1 == null)
			return cov2;
		if (cov2 == null)
			return cov1;
		int cov1Len = cov1.length;
		int cov2Len = cov2.length;
		float[] cov = new float[Math.max(cov1Len, cov2Len)];
		int i = 0;
		for (; i < Math.min(cov1Len, cov2Len); i++) {
			if (cov1[i] == NO_COVERAGE_REQUIRED)
				cov[i] = cov2[i];
			else if (cov2[i] == NO_COVERAGE_REQUIRED)
				cov[i] = cov1[i];
			else
				cov[i] = Math.max(100, cov1[i] + cov2[i]);
		}
		if (cov1Len > cov2Len) {
			for (; i < cov1Len; i++) {
				cov[i] = cov1[i];
			}
		} else if (cov1Len < cov2Len) {
			for (; i < cov2Len; i++) {
				cov[i] = cov2[i];
			}
		}
		return cov;
	}

	class UpdateDecoratorsJob extends WorkspaceJob {

		private final IWorkbench workbench;

		public UpdateDecoratorsJob(IWorkbench workbench) {

			super("Updating coverage decorations");
			setSystem(true);
			this.workbench = workbench;
		}

		@Override
		public IStatus runInWorkspace(final IProgressMonitor monitor) {

			try {
				workbench.getDecoratorManager().update(CoverageDecorator.COVERAGE_DECORATOR_ID);
				return new Status(Status.OK, SAUCBuilder.PLUGIN_ID, "Coverage update complete");
			} catch (Exception e) {
				return new Status(Status.ERROR, SAUCBuilder.PLUGIN_ID, ExceptionUtils.getFullStackTrace(e));
			}
		}
	}
}
