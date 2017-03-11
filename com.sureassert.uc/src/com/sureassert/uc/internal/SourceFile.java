/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.sureassert.uc.builder.MarkerUtils;
import com.sureassert.uc.builder.SAUCEditorCoverageListenner;
import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.exception.SARuntimeException;

public class SourceFile {

	private final List<Integer> linePositions;
	private final int fileSize;
	private final String source;
	private final IFile file;
	// private final String absolutePath;
	private Map<Integer, List<IMarker>> markersByLineNum;
	private Map<Integer, List<String>> markerMessagesByLineNum;

	private static final String[] MARKER_TYPES = new String[] { MarkerUtils.UC_INFO_MARKER_TYPE, MarkerUtils.UC_PROBLEM_MARKER_TYPE, MarkerUtils.UC_WARNING_MARKER_TYPE, //
			MarkerUtils.JUNIT_INFO_MARKER_TYPE, MarkerUtils.JUNIT_PROBLEM_MARKER_TYPE, SAUCEditorCoverageListenner.CODE_STUBBED_MARKER_TYPE, //
			SAUCEditorCoverageListenner.COVERAGE_MARKER_TYPE, SAUCEditorCoverageListenner.COVERAGE_REQUIRED_MARKER_TYPE, //
			SAUCEditorCoverageListenner.PARTIAL_COVERAGE_MARKER_TYPE };

	private SourceFile(IFile file) throws IOException, CoreException {

		this.file = file;
		// this.absolutePath = new File(file.getLocationURI()).getAbsolutePath();
		StringBuilder sourceBuilder = new StringBuilder();
		this.linePositions = new ArrayList<Integer>();
		InputStream sourceIn = file.getContents(true);
		try {
			// Get line positions
			if (!(sourceIn instanceof BufferedInputStream))
				sourceIn = new BufferedInputStream(sourceIn);
			int c;
			int pos = 0;
			while ((c = sourceIn.read()) > -1) {
				if ((char) c == '\n')
					linePositions.add(pos + 1);
				pos++;
				sourceBuilder.append((char) c);
			}
			this.fileSize = pos;
			source = sourceBuilder.toString();
		} finally {
			sourceIn.close();
		}
	}

	public SourceFile(String workingFile) throws CoreException, IOException {

		this.file = null;
		// this.absolutePath = new File(file.getLocationURI()).getAbsolutePath();
		this.markerMessagesByLineNum = new HashMap<Integer, List<String>>();
		this.markersByLineNum = new HashMap<Integer, List<IMarker>>();
		StringBuilder sourceBuilder = new StringBuilder();
		this.linePositions = new ArrayList<Integer>();
		InputStream sourceIn = new ByteArrayInputStream(workingFile.getBytes());
		try {
			// Get line positions
			if (!(sourceIn instanceof BufferedInputStream))
				sourceIn = new BufferedInputStream(sourceIn);
			int c;
			int pos = 0;
			while ((c = sourceIn.read()) > -1) {
				if ((char) c == '\n')
					linePositions.add(pos + 1);
				pos++;
				sourceBuilder.append((char) c);
			}
			this.fileSize = pos;
			source = workingFile;
		} finally {
			sourceIn.close();
		}
	}

	public void registerDeleteAllMarkers() {

		markersByLineNum.clear();
	}

	public int getNumLines() {

		return linePositions.size();
	}

	// private IMarker getMarkersAtLineNum(int lineNum) {
	//
	// List<String> markersByLineNum.get(lineNum);
	// }

	private Map<Integer, List<String>> _getMarkerMessagesByLineNum(IFile file, String... types) throws CoreException {

		Map<Integer, List<String>> markersByLineNum = new HashMap<Integer, List<String>>();
		List<IMarker> markers = new ArrayList<IMarker>();
		for (String type : types) {
			markers.addAll(Arrays.asList(file.findMarkers(type, false, IResource.DEPTH_ZERO)));
		}
		for (IMarker marker : markers) {
			int lineNum = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			BasicUtils.mapListAdd(markersByLineNum, lineNum, marker.getAttribute(IMarker.MESSAGE).toString());
		}
		return markersByLineNum;
	}

	private Map<Integer, List<IMarker>> _getMarkersByLineNum(IFile file, String... types) throws CoreException {

		Map<Integer, List<IMarker>> markersByLineNum = new HashMap<Integer, List<IMarker>>();
		List<IMarker> markers = new ArrayList<IMarker>();
		for (String type : types) {
			markers.addAll(Arrays.asList(file.findMarkers(type, false, IResource.DEPTH_ZERO)));
		}
		for (IMarker marker : markers) {
			int lineNum = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			BasicUtils.mapListAdd(markersByLineNum, lineNum, marker);
		}
		return markersByLineNum;
	}

	public Map<Integer, List<String>> getMarkerMessagesByLineNum() {

		if (markerMessagesByLineNum == null && file != null) {
			try {
				markerMessagesByLineNum = _getMarkerMessagesByLineNum(file, MARKER_TYPES);
			} catch (CoreException e) {
				throw new SARuntimeException(e);
			}
		}
		return markerMessagesByLineNum;
	}

	public Map<Integer, List<IMarker>> getMarkersByLineNum() {

		if (markersByLineNum == null && file != null) {
			try {
				markersByLineNum = _getMarkersByLineNum(file, MARKER_TYPES);
			} catch (CoreException e) {
				throw new SARuntimeException(e);
			}
		}
		return markersByLineNum;
	}

	public List<IMarker> getMarkersAtLineNum(int lineNum) {

		return getMarkersByLineNum().get(lineNum);
	}

	// public String getAbsolutePath() {
	//
	// return absolutePath;
	// }

	public int getPosition(int lineNum) {

		if (lineNum <= 1)
			return 0;
		if (linePositions.size() <= lineNum - 1)
			return source.length() - 1;
		return linePositions.get(lineNum - 2);
	}

	public int getLineNum(int position) {

		if (position < 0)
			return -1;

		// Binary slice on linePositions; relative position as first try heuristic
		int numLines = linePositions.size();
		int tryLine = (int) Math.max(Math.min(numLines / ((double) fileSize / (double) position), numLines - 2), 1);
		int delta = Integer.MAX_VALUE;
		int lastDelta;
		while (true) {
			lastDelta = delta;
			if (tryLine >= numLines - 1)
				return numLines;
			else if (tryLine == 0)
				return 1;
			else if (linePositions.get(tryLine) < position && linePositions.get(tryLine + 1) > position)
				return tryLine + 2;
			else if (linePositions.get(tryLine) > position)
				delta = Math.min((delta == Integer.MAX_VALUE) ? -tryLine / 2 : -Math.abs(delta /= 2), -1);
			else
				delta = Math.max((delta == Integer.MAX_VALUE) ? (numLines - tryLine) / 2 : Math.abs(delta /= 2), 1);

			if (delta + lastDelta == 0)
				return tryLine;

			tryLine += delta;
		}
	}

	public Map<Signature, Integer> getSigLineNums(IType type, SourceModel sourceModel, ClassLoader cl) throws JavaModelException, SecurityException, ClassNotFoundException, NoSuchMethodException {

		HashMap<Signature, Integer> sigLineNums = new HashMap<Signature, Integer>();
		for (IMethod method : type.getMethods()) {
			sigLineNums.put(sourceModel.getSignature(method), getLineNum(method.getNameRange().getOffset()));
		}
		return sigLineNums;
	}

	public String getSource() {

		return source;
	}

	public IFile getFile() {

		return file;
	}

	public static class SourceFileFactory {

		private final Map<String, SourceFile> sourceFileByPath = new HashMap<String, SourceFile>();

		public SourceFile getSourceFile(IFile file) throws IOException, CoreException {

			SourceFile sourceFile = sourceFileByPath.get(file.getLocation().toString());
			if (sourceFile == null) {
				sourceFile = new SourceFile(file);
				sourceFileByPath.put(file.getLocation().toString(), sourceFile);
			}
			return sourceFile;
		}
	}
}