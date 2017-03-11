/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ui.PlatformUI;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.MarkerUtils;
import com.sureassert.uc.builder.SAUCBuilder;
import com.sureassert.uc.builder.SAUCEditorCoverageListenner;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;

public class CoverageReporter {

	public static final String MARKER_TYPE_COVERAGE_REPORT = "com.sureassert.uc.suacCoverageReport";
	public static final String MARKER_TYPE_COVERAGE_STAT = "com.sureassert.uc.suacCoverageStat";

	private static NumberFormat nf = NumberFormat.getInstance();
	static {
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(0);
		nf.setGroupingUsed(false);
	}

	public void reportCoverage(IWorkspace workspace) {

		reportCoverage(null, new JavaPathData(), workspace, new SourceFileFactory());
	}

	public void reportCoverage(Set<IPath> checkPaths, JavaPathData javaPathData, //
			IWorkspace workspace, SourceFileFactory sfFactory) {

		if (checkPaths == null) {
			// Get all java files in all SAUC projects in workspace
			checkPaths = new HashSet<IPath>();
			ProjectOrder projectOrder = workspace.computeProjectOrder(workspace.getRoot().getProjects());
			for (IProject project : projectOrder.projects) {
				try {
					if (project.hasNature(SAUCBuilder.NATURE_ID)) {
						checkPaths.addAll(EclipseUtils.getAllFiles(project, "java"));
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		for (IPath path : checkPaths) {
			try {
				IFile file = javaPathData.getFileQuick(path, workspace);
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(path, workspace);
				if (javaUnit.exists()) {
					Map<Integer, List<IMarker>> coverageMarkers = //
					MarkerUtils.getMarkersByLineNum(file, SAUCEditorCoverageListenner.COVERAGE_MARKER_TYPE);
					coverageMarkers.putAll(//
							MarkerUtils.getMarkersByLineNum(file, SAUCEditorCoverageListenner.CODE_STUBBED_MARKER_TYPE));
					Map<Integer, List<IMarker>> coverageReqMarkers = //
					MarkerUtils.getMarkersByLineNum(file, SAUCEditorCoverageListenner.COVERAGE_REQUIRED_MARKER_TYPE);
					Set<IMarker> deleteMarkers = new HashSet<IMarker>();
					int covReqLines = coverageReqMarkers.size();
					int coveredLines = coverageMarkers.size();
					int removedCovReqLines = 0;

					// Remove covReq markers from lines containing a coverage marker
					// for (Entry<Integer, List<IMarker>> covReqEntry :
					// coverageReqMarkers.entrySet()) {
					// int lineNum = covReqEntry.getKey();
					// List<IMarker> covMarkers = coverageMarkers.get(lineNum);
					// if (covMarkers != null) {
					// removedCovReqLines++;
					// deleteMarkers.addAll(covReqEntry.getValue());
					// }
					// }

					// Only one coverage marker allowed per line
					// for (Entry<Integer, List<IMarker>> covEntry : coverageMarkers.entrySet()) {
					//
					// List<IMarker> covMarkers = coverageMarkers.get(covEntry.getKey());
					// for (int i = 1; i < covMarkers.size(); i++) {
					// deleteMarkers.add(covMarkers.get(i));
					// }
					// }

					// determine if any covered lines are found without
					// a covReqMarker, indicating this process has been
					// run on this file previously.
					// TODO: This isn't working - need to remember paths already processed in PDF
					// instead and clear at end of build
					boolean firstPass = removedCovReqLines == coveredLines;

					for (IMarker deleteMarker : deleteMarkers) {
						deleteMarker.delete();
					}

					// If any covered lines are found without
					// a covReqMarker, then CoverageReporter must have
					// already removed the cov req markers. In which case get covReqLines from the
					// info marker created below.
					if (!firstPass) {
						covReqLines = getCovReqLinesFromMarker(file);
						// if (covReqLines == 0)
						// covReqLines = coveredLines;
					}

					file.deleteMarkers(MARKER_TYPE_COVERAGE_REPORT, false, IResource.DEPTH_ZERO);
					file.deleteMarkers(MARKER_TYPE_COVERAGE_STAT, false, IResource.DEPTH_ZERO);

					// Write covReqLines to file for future reference (as covReq lines are deleted)
					String javaSrcName = javaUnit.getElementName();
					IMarker covInfoMarker = file.createMarker(MARKER_TYPE_COVERAGE_STAT);
					covInfoMarker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
					covInfoMarker.setAttribute(IMarker.MESSAGE, getNumCovReqLinesMessage(covReqLines, javaSrcName));

					// Write info/warning/error marker to file depending on threshold preference
					if (covReqLines > 0 || coveredLines > 0) {
						int percentCovered = (int) (((double) coveredLines / (double) covReqLines) * 100d);
						String message;
						int severity;
						if (percentCovered >= SaUCPreferences.getCoverageWarnThreshold() && //
								percentCovered >= SaUCPreferences.getCoverageErrorThreshold()) {
							severity = IMarker.SEVERITY_INFO;
						} else if (percentCovered >= SaUCPreferences.getCoverageErrorThreshold()) {
							severity = IMarker.SEVERITY_WARNING;
						} else {
							severity = IMarker.SEVERITY_ERROR;
						}
						message = "Test coverage of " + javaSrcName + " is " + percentCovered + "%";
						IMarker covRepMarker = file.createMarker(MARKER_TYPE_COVERAGE_REPORT);
						covRepMarker.setAttribute(IMarker.SEVERITY, severity);
						covRepMarker.setAttribute(IMarker.MESSAGE, message);
					}
				}

			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		}

		EclipseUtils.refreshWorkbench(PlatformUI.getWorkbench());
	}

	private static final String NUM_COV_REQ_LINES_MESSAGE = " lines require test coverage in ";

	private String getNumCovReqLinesMessage(int covReqLines, String javaSrcName) {

		return covReqLines + NUM_COV_REQ_LINES_MESSAGE + javaSrcName;
	}

	private int getCovReqLinesFromMarker(IFile file) throws CoreException {

		for (IMarker marker : file.findMarkers(MARKER_TYPE_COVERAGE_STAT, false, IResource.DEPTH_ZERO)) {
			String message = (String) marker.getAttribute(IMarker.MESSAGE);
			if (message != null && message.contains(NUM_COV_REQ_LINES_MESSAGE)) {
				StringBuilder covReqLinesStr = new StringBuilder();
				for (int i = 0; i < message.length() && Character.isDigit(message.charAt(i)); i++) {
					covReqLinesStr.append(message.charAt(i));
				}
				return Integer.parseInt(covReqLinesStr.toString());
			}
		}
		return 0;
	}
}
