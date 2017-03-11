/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of, and calculates, session statistics.
 * 
 * @author Nathan Dolan
 */
public class SessionStats {

	/**
	 * The number of errors encountered while processing this project
	 */
	private final int numErrors = 0;

	/**
	 * The number of lines of code requiring code coverage for each java file.
	 * Key=java source path, value=num lines
	 */
	private final Map<String, Integer> numCoverageRequiredLinesByJavaPath = new HashMap<String, Integer>();

	/**
	 * The number of lines of code having code coverage for each java file.
	 * Key=java source path, value=num lines
	 */
	private final Map<String, Integer> numCoveredLinesByJavaPath = new HashMap<String, Integer>();

}
