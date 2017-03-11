/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.transform.SaUCInstrumenter;

public class InstrumentationSession {

	private final ExecutionDataStore executionData = new ExecutionDataStore();

	private IRuntime runtime;

	private SaUCInstrumenter instr;

	private final List<byte[]> analyzedClasses = new ArrayList<byte[]>();

	private final CoveragePrinter coveragePrinter = new CoveragePrinter(new SAUCEditorCoverageListenner());

	private final CoverageBuilder coverageBuilder = new CoverageBuilder();

	private final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

	private final boolean instrumentCoverage;

	public InstrumentationSession(boolean instrumentCoverage) {

		this.instrumentCoverage = instrumentCoverage;
	}

	public void start() {

		if (instrumentCoverage) {
			try {
				runtime = new LoggerRuntime();
				runtime.startup();
				instr = new SaUCInstrumenter(runtime);
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		} else {
			instr = new SaUCInstrumenter(null);
		}
	}

	/**
	 * Instruments the given class.
	 * 
	 * @param clazz The class bytecode.
	 * @param instrumentThisClassCoverage True to include coverage probes for this class.
	 *            Note if this InstrumentationSession was created with instrumentCoverage==false,
	 *            then this has no effect (i.e. no coverage probes will be inserted for any
	 *            classes).
	 * @return The instrumented bytecode.
	 */
	public byte[] instrument(String className, byte[] clazz, boolean instrumentThisClassCoverage) {

		if (instrumentCoverage)
			analyzedClasses.add(clazz);
		
		return instr.instrument(className, clazz, instrumentCoverage && instrumentThisClassCoverage);
	}

	/**
	 * Ends the instrumentation session - analyses covered classes, persists
	 * coverage information and adds coverage markers.
	 * 
	 * @param sfFactory
	 * @param jpd
	 * @param monitor
	 * @param buildManager
	 */
	public void end(SourceFileFactory sfFactory, JavaPathData jpd, IProgressMonitor monitor, //
			BuildManager buildManager) {

		// Persist and report coverage information
		if (instrumentCoverage) {
			runtime.collect(executionData, null, false);
			runtime.shutdown();
			for (Object clazz : analyzedClasses) {
				if (clazz != null)
					analyzer.analyzeClass((byte[]) clazz);
			}

			// Move coverage info to PersistentDataFactory
			PersistentDataFactory pdf = PersistentDataFactory.getInstance();
			Collection<IClassCoverage> iCCs = coverageBuilder.getClasses();
			Set<String> coveredClasses = new HashSet<String>();
			ILine line;

			for (IClassCoverage iCC : iCCs) {
				String className = BasicUtils.getClassNameFromBinaryClassName(iCC.getName());
				
				if (pdf.isIgnoreCoverageClass(className))
					continue;
				
				coveredClasses.add(className);
				
				// Get existing coverage data for this class
				float[] coverage = pdf.getCoverage(className);

				// If no existing data or a size mismatch, start afresh
				if (coverage == null || coverage.length != iCC.getLastLine() + 1)
					coverage = new float[iCC.getLastLine() + 1];

				// NOTE: Always ignore 1st line (package declaration - JaCoCo wrong on this
				// sometimes)

				// Inner class coverage starts at the 1st line of the inner class in the java file
				for (int lineNum = 0; lineNum < Math.max(2, iCC.getFirstLine()); lineNum++) {
					// Mark as NO_COVERAGE_REQUIRED to avoid marking lines from parent class
					coverage[lineNum] = CoveragePrinter.NO_COVERAGE_REQUIRED;
				}

				// Register coverage for each line; merge with existing line coverage data
				List<Integer> ignoreCovStartEndLines = pdf.getIgnoreCoverageStartEndLines(className);
				for (int lineNum = Math.max(2, iCC.getFirstLine()); lineNum <= iCC.getLastLine(); lineNum++) {

					line = iCC.getLine(lineNum);
					float lineCoverage = coverage[lineNum]; // get existing coverage; defaults to 0%

					if (isIgnoreCoverageLine(lineNum, ignoreCovStartEndLines)) {
						// If line is covered by an IgnoreTestCoverage annotation, treat as NO_COVERAGE_REQUIRED
						coverage[lineNum] = CoveragePrinter.NO_COVERAGE_REQUIRED;
						
					} else if (lineCoverage < 100) {
						// If existing coverage is already 100%, leave as such
						if (line.getStatus() == ICounter.FULLY_COVERED) {
							lineCoverage = 100;
						} else if (line.getStatus() == ICounter.PARTLY_COVERED) {
							// Take max of previous and this session coverage
							float thisSessionLineCoverage = (float) (line.getInstructionCounter().getCoveredRatio() * 100f);
							if (thisSessionLineCoverage > lineCoverage)
								lineCoverage = thisSessionLineCoverage;
						} else if (line.getStatus() == ICounter.NOT_COVERED) {
							lineCoverage = 0;
						} else {
							lineCoverage = CoveragePrinter.NO_COVERAGE_REQUIRED;
						}
						coverage[lineNum] = lineCoverage;
					}
				}
				pdf.setCoverage(className, coverage);
			}

			// Print coverage
			coveragePrinter.printInfo(coverageBuilder, sfFactory, jpd, monitor, buildManager, null);
		}
	}
	
	private boolean isIgnoreCoverageLine(int queryLine, List<Integer> ignoreCovStartEndLines) {
		
		if (ignoreCovStartEndLines == null)
			return false;
		
		// ignoreCovStartEndLines is pairs of start/end lines
		for (Iterator<Integer> i = ignoreCovStartEndLines.iterator(); i.hasNext(); ) {
			if (queryLine >= i.next() && queryLine <= i.next()) {
				return true;
			}
		}
		return false;
	}

}
