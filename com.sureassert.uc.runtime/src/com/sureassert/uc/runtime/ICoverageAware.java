/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

public interface ICoverageAware {

	public static final String COVERAGE_NOTIFIER_EXTENSION_ID = "com.sureassert.uc.runtime.coverage";

	/**
	 * Invoked to notify that some code on the given line number within the source file
	 * for the given class has been executed.
	 * 
	 * @param lineNum
	 * @param className
	 */
	public void notifyCodeCovered(int lineNum, String className);

	/**
	 * Invoked to notify that the stub code within the given line number range within the source
	 * file
	 * for the given class has been executed.
	 * 
	 * @param lineNum
	 * @param endLineNum
	 * @param className
	 */
	public void notifyStubExecuted(int lineNum, int endLineNum, String className);

	/**
	 * Invoked to indicate that the code on the given line within the source file
	 * for the given class requires test coverage.
	 * 
	 * @param lineNum
	 * @param javaPathStr
	 * @param className
	 */
	public void notifyCoverageRequired(int lineNum, String javaPathStr, String className);
}
