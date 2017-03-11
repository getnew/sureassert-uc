/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;

public class SAUCEditorCoverageListenner {

	public static final String COVERAGE_MARKER_TYPE = "com.sureassert.uc.codeCoverage";

	public static final String PARTIAL_COVERAGE_MARKER_TYPE = "com.sureassert.uc.codePartialCoverage";

	public static final String COVERAGE_REQUIRED_MARKER_TYPE = "com.sureassert.uc.coverageRequired";

	public static final String CODE_STUBBED_MARKER_TYPE = "com.sureassert.uc.codeStubbed";

	private final SourceFileFactory sfFactory = new SourceFileFactory();
	// private JavaPathData javaPathData = new JavaPathData();

	/**
	 * Line numbers covered mapped against class name
	 */
	// private final Map<String, Set<Integer>> coveredLines = new HashMap<String, Set<Integer>>();

	private final boolean isCoverageDisplayEnabled = SaUCPreferences.getIsCoverageDisplayEnabled();

	private final boolean disabled = !isCoverageDisplayEnabled;

	private final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);

	public SAUCEditorCoverageListenner() {

		/*
		 * try {
		 * IWorkspace workspace = ResourcesPlugin.getWorkspace();
		 * workspace.addSaveParticipant(getClass().getName(), this);
		 * } catch (Throwable e) {
		 * EclipseUtils.reportError(e);
		 * }
		 */
	}

	public void notifyCoverage(SourceFile sourceFile, IPath javaPath, int lineNum, //
			String className, float coverage) {

		if (disabled)
			return;

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();

		try {
			List<IMarker> markers = sourceFile.getMarkersAtLineNum(lineNum);
			if (markers != null) {
				for (IMarker marker : markers) {
					try {
						if (marker.getType().equals(PARTIAL_COVERAGE_MARKER_TYPE) || //
								marker.getType().equals(COVERAGE_MARKER_TYPE) || //
								marker.getType().equals(COVERAGE_REQUIRED_MARKER_TYPE) || //
								marker.getType().equals(CODE_STUBBED_MARKER_TYPE))
							marker.delete();
					} catch (CoreException re) {
						// Sometimes Eclipse loses the marker - Ignore
					}
				}
			}
			IMarker coverageMarker;
			if (coverage == CoveragePrinter.NO_COVERAGE_REQUIRED)
				return;
			else if (coverage == 100) {
				coverageMarker = sourceFile.getFile().createMarker(COVERAGE_MARKER_TYPE);
				String message = String.format("100%s test coverage at %s", "%", df.format(new Date()));
				coverageMarker.setAttribute(IMarker.MESSAGE, message);
			} else if (coverage == 0) {
				coverageMarker = sourceFile.getFile().createMarker(COVERAGE_REQUIRED_MARKER_TYPE);
				coverageMarker.setAttribute(IMarker.MESSAGE, "No test coverage");
			} else {
				coverageMarker = sourceFile.getFile().createMarker(PARTIAL_COVERAGE_MARKER_TYPE);
				String message = String.format("%d%s test coverage at %s", (int) coverage, "%", df.format(new Date()));
				coverageMarker.setAttribute(IMarker.MESSAGE, message);
			}
			coverageMarker.setAttribute(IMarker.CHAR_START, sourceFile.getPosition(lineNum));
			coverageMarker.setAttribute(IMarker.CHAR_END, sourceFile.getPosition(lineNum + 1));
			coverageMarker.setAttribute(IMarker.LINE_NUMBER, lineNum);
			pdf.addMarkedFilePath(javaPath.toString());

		} catch (Throwable e) {
			EclipseUtils.reportError(ExceptionUtils.getFullStackTrace(e), false);
		}
	}

	public void notifyStubExecuted(int lineNum, int endLineNum, String className) {

		for (; lineNum <= endLineNum; lineNum++) {
			notifyStubExecuted(lineNum, className);
		}
	}

	public void notifyStubExecuted(int lineNum, String className) {

		if (disabled)
			return;

		try {
			PersistentDataFactory pdf = PersistentDataFactory.getInstance();
			// Set<Integer> classCoveredLines = BasicUtils.mapSetGet(coveredLines, className);
			// if (!classCoveredLines.contains(lineNum)) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			String javaPathStr = pdf.getJavaPathStr(className);
			if (javaPathStr == null) {
				EclipseUtils.reportError("Could not find java path for class " + className + " - cannot notify code coverage", false);
			} else {
				// IFile file = javaPathData.getFileQuick(new Path(javaPathStr), workspace);
				IFile file = workspace.getRoot().getFile(new Path(javaPathStr));
				SourceFile sourceFile = sfFactory.getSourceFile(file);
				int startPos = sourceFile.getPosition(lineNum);
				int endPos = sourceFile.getPosition(lineNum + 1);
				IMarker coverageMarker = file.createMarker(CODE_STUBBED_MARKER_TYPE);
				coverageMarker.setAttribute(IMarker.CHAR_START, startPos);
				coverageMarker.setAttribute(IMarker.CHAR_END, endPos);
				coverageMarker.setAttribute(IMarker.LINE_NUMBER, lineNum);
				String junitClass = pdf.getCurrentJUnitClass();

				String message;
				if (junitClass == null) {
					message = "Code replaced by stub executed by " + BasicUtils.getCurrentUseCaseDisplayName();
				} else {
					message = "Code replaced by stub executed by JUnit " + junitClass + "." + //
							pdf.getCurrentJUnitMethod();
				}
				coverageMarker.setAttribute(IMarker.MESSAGE, message);
				// BasicUtils.mapSetAdd(coveredLines, className, lineNum);

				// Ensure there is a coverage required marker on this line else the
				// coverage data calculated by CoverageReporter will be incorrect
				Map<Integer, List<IMarker>> coverageReqMarkers = //
				MarkerUtils.getMarkersByLineNum(file, SAUCEditorCoverageListenner.COVERAGE_REQUIRED_MARKER_TYPE);
				if (!coverageReqMarkers.containsKey(lineNum)) {
					IMarker coverageReqMarker = file.createMarker(COVERAGE_REQUIRED_MARKER_TYPE);
					coverageReqMarker.setAttribute(IMarker.CHAR_START, startPos);
					coverageReqMarker.setAttribute(IMarker.CHAR_END, endPos);
					coverageReqMarker.setAttribute(IMarker.LINE_NUMBER, lineNum);
				}

				// pdf.addMarkedFilePath(javaPathStr);
			}
			// }
		} catch (Throwable e) {
			EclipseUtils.reportError(ExceptionUtils.getFullStackTrace(e), false);
		}
	}

	/*
	 * private void deleteMarker(IFile file, String type, int lineNum) throws CoreException {
	 * 
	 * List<IMarker> markers = new ArrayList<IMarker>(Arrays.asList(//
	 * file.findMarkers(type, false, IResource.DEPTH_ZERO)));
	 * for (IMarker marker : markers) {
	 * int thisLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
	 * if (thisLine == lineNum) {
	 * marker.delete();
	 * }
	 * }
	 * }
	 */

	/*
	 * public void doneSaving(ISaveContext arg0) {
	 * 
	 * dispose();
	 * }
	 * 
	 * public void prepareToSave(ISaveContext arg0) throws CoreException {
	 * 
	 * }
	 * 
	 * public void rollback(ISaveContext arg0) {
	 * 
	 * }
	 * 
	 * public void saving(ISaveContext arg0) throws CoreException {
	 * 
	 * }
	 */
}
