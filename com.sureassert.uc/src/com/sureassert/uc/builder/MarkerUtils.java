/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.CoverageReporter;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.saserver.SAServerMessage;
import com.sureassert.uc.runtime.saserver.VoidReturn;

public class MarkerUtils {

	public static final String UC_PROBLEM_MARKER_TYPE = "com.sureassert.uc.saucProblem";

	public static final String UC_INFO_MARKER_TYPE = "com.sureassert.uc.saucInfo";

	public static final String UC_WARNING_MARKER_TYPE = "com.sureassert.uc.saWarning";

	public static final String JUNIT_PROBLEM_MARKER_TYPE = "com.sureassert.uc.sajuProblem";

	public static final String JUNIT_INFO_MARKER_TYPE = "com.sureassert.uc.sajuInfo";

	// public static void addMarker(MarkerSpec markerSpec) {
	//
	// addMarker(markerSpec.sourceFile, markerSpec.message, markerSpec.lineNumber, //
	// markerSpec.severity, markerSpec.isJUnitMarker);
	// }

	public static void addMarker(SourceFileFactory sfFactory, JavaPathData jpd, SourceFile sourceFile, String message, int lineNumber, int severity, boolean isJUnitMarker) {

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		// if (pdf.isStandaloneBuild()) {
		// try {
		// BuildServer.getInstance().sendMessage(new AddMarkerMessage(//
		// sfFactory, jpd, sourceFile, message, lineNumber, severity, isJUnitMarker));
		// } catch (Exception e) {
		// throw new SARuntimeException(e);
		// }
		// return;
		// }

		if (!pdf.wasLastExecUseCase()) {
			message = message.replace("MultiUseCase", "Exemplars");
			message = message.replace("UseCase", "Exemplar");
		}

		if (severity == IMarker.SEVERITY_ERROR)
			BasicUtils.debug("ERROR: " + message);
		else if (severity == IMarker.SEVERITY_WARNING)
			BasicUtils.debug("WARNING: " + message);
		else
			BasicUtils.debug("INFO: " + message);
		if (pdf.isStandaloneBuild()) {
			if (severity == IMarker.SEVERITY_ERROR)
				pdf.registerStandaloneError();
			return;
		}
		try {
			IFile file = sourceFile.getFile();

			// Check if a marker with the given message already exists on this line
			// BasicUtils.debug("Added marker to " + file.getName() +
			// " at line " + lineNumber);
			Map<Integer, List<String>> markersByLineNum = sourceFile.getMarkerMessagesByLineNum();
			List<String> lineMarkers = markersByLineNum.get(lineNumber);
			if (lineMarkers != null) {
				for (String marker : lineMarkers) {
					if (marker.equals(message))
						return;
				}
			}
			// Add marker
			String markerType;
			if (severity == IMarker.SEVERITY_WARNING) {
				markerType = UC_WARNING_MARKER_TYPE;
			} else {
				if (isJUnitMarker)
					markerType = (severity == IMarker.SEVERITY_INFO) ? JUNIT_INFO_MARKER_TYPE : JUNIT_PROBLEM_MARKER_TYPE;
				else
					markerType = (severity == IMarker.SEVERITY_INFO) ? UC_INFO_MARKER_TYPE : UC_PROBLEM_MARKER_TYPE;
			}

			IMarker marker = file.createMarker(markerType);
			List<String> markers = markersByLineNum.get(lineNumber);
			if (markers == null) {
				markers = new ArrayList<String>();
				markersByLineNum.put(lineNumber, markers);
			}
			markers.add(message);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber > 0) {
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			}
			if (severity == IMarker.SEVERITY_ERROR)
				marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);

			// IAnnotationModel#addAnnotation(new Annotation("custom.occurrences", false, message),
			// new Position(offset,length));

		} catch (CoreException e) {
		}

	}

	public static class AddMarkerMessage implements SAServerMessage<VoidReturn> {

		private static final long serialVersionUID = 1L;

		private final String sourceFilePath;
		private final String message;
		private final int lineNumber;
		private final int severity;
		private final boolean isJUnitMarker;

		private transient SourceFileFactory sfFactory;
		private transient JavaPathData jpd;

		public AddMarkerMessage(SourceFileFactory sfFactory, JavaPathData jpd, SourceFile sourceFile, String message, int lineNumber, int severity, boolean isJUnitMarker) {

			this.sfFactory = sfFactory;
			this.jpd = jpd;
			this.sourceFilePath = sourceFile.getFile().getFullPath().toString();
			this.message = message;
			this.lineNumber = lineNumber;
			this.severity = severity;
			this.isJUnitMarker = isJUnitMarker;
		}

		public VoidReturn execute() {

			try {
				IFile file = jpd.getFileQuick(new Path(sourceFilePath), null);
				SourceFile sourceFile;
				sourceFile = sfFactory.getSourceFile(file);
				MarkerUtils.addMarker(sfFactory, jpd, sourceFile, message, lineNumber, severity, isJUnitMarker);
				return VoidReturn.INSTANCE;
			} catch (Exception e) {
				throw new SARuntimeException(e);
			}
		}

		public Class<VoidReturn> getReturnType() {

			return VoidReturn.class;
		}

	}

	public static void deleteAllMarkers(Set<IPath> paths, IProject project, SourceFileFactory sfFactory, //
			JavaPathData javaPathData) throws CoreException, IOException {

		if (PersistentDataFactory.getInstance().isStandaloneBuild())
			return;
		if (paths == null)
			paths = EclipseUtils.getAllFiles(project, "java");

		for (IPath path : paths) {

			IFile file = javaPathData.getFile(path, project.getWorkspace());
			if (file.exists() && file.isAccessible()) {
				file.deleteMarkers(UC_INFO_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(UC_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(UC_WARNING_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(JUNIT_INFO_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(JUNIT_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(SAUCEditorCoverageListenner.COVERAGE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(SAUCEditorCoverageListenner.PARTIAL_COVERAGE_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(SAUCEditorCoverageListenner.CODE_STUBBED_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(SAUCEditorCoverageListenner.COVERAGE_REQUIRED_MARKER_TYPE, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(CoverageReporter.MARKER_TYPE_COVERAGE_REPORT, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(CoverageReporter.MARKER_TYPE_COVERAGE_STAT, false, IResource.DEPTH_ZERO);
				try {
					sfFactory.getSourceFile(file).registerDeleteAllMarkers();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Deletes all Sureassert markers at the given line number of the given file.
	 * 
	 * @param file
	 * @param lineNumer
	 * @throws CoreException
	 */
	public static void deleteUCMarkers(SourceFile file, int startLine, int endLine, List<Range> excludeRanges, String onlyWithPostfix) throws CoreException {

		_deleteMarkers(file, startLine, endLine, UC_INFO_MARKER_TYPE, UC_PROBLEM_MARKER_TYPE, //
				UC_WARNING_MARKER_TYPE, excludeRanges, onlyWithPostfix);
	}

	/**
	 * Deletes all JUnit markers at the given line number of the given file.
	 * 
	 * @param file
	 * @param lineNumer
	 * @throws CoreException
	 */
	public static void deleteJUnitMarkers(SourceFile file, int startLine, int endLine, List<Range> excludeRanges, String onlyWithPostfix) throws CoreException {

		_deleteMarkers(file, startLine, endLine, JUNIT_INFO_MARKER_TYPE, JUNIT_PROBLEM_MARKER_TYPE, //
				JUNIT_PROBLEM_MARKER_TYPE, excludeRanges, onlyWithPostfix);
	}

	private static void _deleteMarkers(SourceFile sourceFile, int startLine, int endLine, //
			String infoMarkerType, String problemMarkerType, String warningMarkerType, List<Range> excludeRanges, //
			String onlyWithPostfix) throws CoreException {

		// System.out.println("deleting " + problemMarkerType + " markers from " +
		// sourceFile.getFile().getFullPath().toString() + " lines " + startLine + "-" + endLine +
		// "; onlyWithPrefix="
		// + onlyWithPostfix);
		if (PersistentDataFactory.getInstance().isStandaloneBuild())
			return;
		// BasicUtils.debug("Deleted markers to " + file.getName() +
		// " between lines " + startLine
		// + " and " + endLine);
		IFile file = sourceFile.getFile();
		Map<Integer, List<String>> markersByLineNum = sourceFile.getMarkerMessagesByLineNum();
		List<IMarker> markers = new ArrayList<IMarker>(Arrays.asList(//
				file.findMarkers(infoMarkerType, false, IResource.DEPTH_ZERO)));
		markers.addAll(Arrays.asList(//
				file.findMarkers(problemMarkerType, false, IResource.DEPTH_ZERO)));
		markers.addAll(Arrays.asList(//
				file.findMarkers(warningMarkerType, false, IResource.DEPTH_ZERO)));
		for (IMarker marker : markers) {
			int lineNum = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			if (lineNum >= startLine && lineNum <= endLine) {
				boolean exclude = false;
				if (excludeRanges != null) {
					for (Range excludeRange : excludeRanges) {
						if (lineNum >= excludeRange.from && lineNum <= excludeRange.to)
							exclude = true;
					}
				}
				if (onlyWithPostfix != null) {
					String message = (String) marker.getAttribute(IMarker.MESSAGE);
					exclude = !(message != null && message.endsWith(onlyWithPostfix));
				}
				if (!exclude) {
					marker.delete();
					markersByLineNum.remove(lineNum);
				}
			}
		}
	}

	public static Map<Integer, List<IMarker>> getMarkersByLineNum(IFile file, String type) throws CoreException {

		Map<Integer, List<IMarker>> markersByLineNum = new HashMap<Integer, List<IMarker>>();
		List<IMarker> markers = new ArrayList<IMarker>();
		markers.addAll(Arrays.asList(file.findMarkers(type, false, IResource.DEPTH_ZERO)));
		for (IMarker marker : markers) {
			int lineNum = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			BasicUtils.mapListAdd(markersByLineNum, lineNum, marker);
		}
		return markersByLineNum;
	}

	public static boolean isEmptyLine(int lineNum, SourceFile sourceFile) {

		String lineSrc = sourceFile.getSource().substring(sourceFile.getPosition(lineNum), //
				sourceFile.getPosition(lineNum + 1));
		return lineSrc.trim().length() == 0;
	}
}
