package com.sureassert.uc.builder;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.Timer;

public class JaCoCoClient { // implements ICoverageVisitor {

	private ExecutionDataStore execDataStore;

	private CoverageBuilder coverageBuilder;

	private Analyzer analyzer;

	private final List<Object> analyzeClassBytes = new ArrayList<Object>();

	private final Map<String, Long> classNameToId = new HashMap<String, Long>();

	private final SAUCEditorCoverageListenner coverageListener;

	public JaCoCoClient(SAUCEditorCoverageListenner coverageListener) {

		this.coverageListener = coverageListener;
	}

	public void startSession() {

		reset();
		execDataStore = new ExecutionDataStore();
		coverageBuilder = new CoverageBuilder();
		analyzer = new Analyzer(execDataStore, coverageBuilder);
	}

	public void analyzeClass(String className, byte[] classBytes) {

		if (!classNameToId.containsKey(className)) {
			classNameToId.put(className, CRC64.checksum(classBytes));
			analyzeClassBytes.add(classBytes);
		}
	}

	public void printInfo(SourceFileFactory sfFactory, JavaPathData jpd, IProgressMonitor monitor, BuildManager buildManager) {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		int numClasses = coverageBuilder.getClasses().size();
		int classIdx = 0;
		long numLines = 0;
		Set<String> openFilePaths = new HashSet<String>();

		IProject[] projects = workspace.getRoot().getProjects();

		IWorkbench workbench = PlatformUI.getWorkbench();
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
									if (editorInput != null && editorInput instanceof IPathEditorInput) {
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

		try {
			monitor.beginTask("Marking test coverage", numClasses);

			for (final IClassCoverage cc : coverageBuilder.getClasses()) {

				if (buildManager.isBuildCancelled(monitor))
					break;

				String className = BasicUtils.getClassNameFromBinaryClassName(cc.getName());
				int percentComplete = (int) (((float) classIdx / (float) numClasses) * 100f);

				if (monitor != null) {
					monitor.setTaskName(String.format("Marking test coverage (%d%s; %d lines)", percentComplete, "%", numLines));
				}

				// if (!className.startsWith("org.aspectj.") &&
				// !cc.getName().startsWith("com.sureassert.uc.aspects.")) {
				// System.out.printf("Coverage of class %s%n", cc.getName());
				//
				// printCounter("instructions", cc.getInstructionCounter());
				// printCounter("branches", cc.getBranchCounter());
				// printCounter("lines", cc.getLineCounter());
				// printCounter("methods", cc.getMethodCounter());
				// printCounter("complexity", cc.getComplexityCounter());

				String pathStr = pdf.getJavaPathStr(className);
				if (pathStr != null) {
					IPath javaPath = new Path(pathStr);
					IFile file = jpd.getFile(javaPath, workspace);
					if (file != null && openFilePaths.contains(file.getLocation().toString())) {
						try {
							SourceFile sourceFile = sfFactory.getSourceFile(file);

							for (int lineNum = cc.getFirstLine(); lineNum <= cc.getLastLine(); lineNum++) {
								// if (cc.getLine(lineNum).getStatus() != ICounter.NOT_COVERED
								// &&
								// cc.getLine(lineNum).getStatus() != ICounter.EMPTY)
								// System.out.printf("Line %s: %s%n", Integer.valueOf(lineNum),
								// getColor(cc.getLine(lineNum).getStatus()));

								numLines++;
								ILine line = cc.getLine(lineNum);
								char coverage;
								if (line.getStatus() == ICounter.FULLY_COVERED) {
									coverage = 100;
								} else if (line.getStatus() == ICounter.PARTLY_COVERED) {
									coverage = (char) (line.getBranchCounter().getCoveredRatio() * 100d);
								} else if (line.getStatus() == ICounter.NOT_COVERED) {
									coverage = 0;
								} else {
									coverage = (char) CoveragePrinter.NO_COVERAGE_REQUIRED;
								}
								coverageListener.notifyCoverage(sourceFile, javaPath, //
										lineNum, className, coverage);
							}
						} catch (Exception e) {
							EclipseUtils.reportError(e, false);
						}
					}
				}
				classIdx++;
				monitor.worked(1);
			}
			// }
		} finally {
			monitor.done();
		}
	}

	private void printCounter(final String unit, final ICounter counter) {

		final Integer missed = Integer.valueOf(counter.getMissedCount());
		final Integer total = Integer.valueOf(counter.getTotalCount());
		System.out.printf("%s of %s %s missed%n", missed, total, unit);
	}

	private String getColor(final int status) {

		switch (status) {
		case ICounter.NOT_COVERED:
			return "red";
		case ICounter.PARTLY_COVERED:
			return "yellow";
		case ICounter.FULLY_COVERED:
			return "green";
		}
		return "";
	}

	public void dumpExecData() {

		Socket socket = null;
		final RemoteControlWriter writer;
		final RemoteControlReader reader;

		try {
			// Open a socket to the coverage agent:
			socket = new Socket((String) null, 6359);
			writer = new RemoteControlWriter(socket.getOutputStream());
			reader = new RemoteControlReader(socket.getInputStream());

			// Create visitors
			reader.setSessionInfoVisitor(new ISessionInfoVisitor() {

				public void visitSessionInfo(final SessionInfo info) {

					System.out.printf("Session \"%s\": %s - %s%n", info.getId(), new Date(info.getStartTimeStamp()), new Date(info.getDumpTimeStamp()));
				}
			});
			reader.setExecutionDataVisitor(new IExecutionDataVisitor() {

				public void visitClassExecution(final ExecutionData data) {

					// System.out.printf("%016x  %3d of %3d   %s%n", Long.valueOf(data.getId()),
					// Integer.valueOf(getHitCount(data.getData())),
					// Integer.valueOf(data.getData().length), data.getName());
					Long newID = classNameToId.get(data.getName());
					ExecutionData newExecData = data;
					if (newID != null)
						newExecData = new ExecutionData(newID, data.getName(), data.getData());

					try {
						execDataStore.visitClassExecution(newExecData);
					} catch (Exception e) {
						EclipseUtils.reportError(e, false);
					}
				}
			});

			// Send a dump command and read the response:
			Timer timer = new Timer("JaCoCo dump");
			writer.visitDumpCommand(true, true);
			reader.read();
			timer.printExpiredTime();

			timer = new Timer("JaCoCo Analyzer");
			for (Object classBytes : analyzeClassBytes) {
				if (classBytes != null)
					analyzer.analyzeClass((byte[]) classBytes);
			}
			analyzeClassBytes.clear();
			classNameToId.clear();

			timer.printExpiredTime();

		} catch (Exception e) {
			EclipseUtils.reportError(e);

		} finally {

			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					EclipseUtils.reportError(e);
				}
			}
		}
	}

	public void reset() {

		Socket socket = null;
		final RemoteControlWriter writer;
		final RemoteControlReader reader;

		try {
			// Open a socket to the coverage agent:
			socket = new Socket((String) null, 6359);
			writer = new RemoteControlWriter(socket.getOutputStream());
			reader = new RemoteControlReader(socket.getInputStream());

			// Send a reset command
			Timer timer = new Timer("JaCoCo reset");
			writer.visitDumpCommand(false, true);
			reader.read();
			timer.printExpiredTime();

		} catch (Exception e) {
			EclipseUtils.reportError(e);

		} finally {

			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					EclipseUtils.reportError(e);
				}
			}
		}
	}

	private static int getHitCount(final boolean[] data) {

		int count = 0;
		for (final boolean hit : data) {
			if (hit) {
				count++;
			}
		}
		return count;
	}
	//
	// public void visitCoverage(final IClassCoverage coverage) {
	//
	// System.out.printf("class name:   %s%n", coverage.getName());
	// System.out.printf("class id:     %016x%n", Long.valueOf(coverage.getId()));
	// System.out.printf("instructions: %s%n",
	// Integer.valueOf(coverage.getInstructionCounter().getTotalCount()));
	// System.out.printf("branches:     %s%n",
	// Integer.valueOf(coverage.getBranchCounter().getTotalCount()));
	// System.out.printf("lines:        %s%n",
	// Integer.valueOf(coverage.getLineCounter().getTotalCount()));
	// System.out.printf("methods:      %s%n",
	// Integer.valueOf(coverage.getMethodCounter().getTotalCount()));
	// System.out.printf("complexity:   %s%n%n",
	// Integer.valueOf(coverage.getComplexityCounter().getTotalCount()));
	// }
}
