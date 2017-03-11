package com.sureassert.uc.runtime.saserver;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.sureassert.uc.runtime.ICoverageAware;

public class CoverageRequiredMessage extends AbstractCoverageMessage {

	private static final long serialVersionUID = 1L;

	private final int lineNum;
	private final String javaPathStr;
	private final String className;

	public CoverageRequiredMessage(int lineNum, String javaPathStr, String className) {

		this.lineNum = lineNum;
		this.javaPathStr = javaPathStr;
		this.className = className;
	}

	public VoidReturn execute() {

		for (final ICoverageAware coverageNotifier : coverageNotifiers) {
			try {
				coverageNotifier.notifyCoverageRequired(lineNum, javaPathStr, className);
			} catch (Throwable e) {
				System.err.println(ExceptionUtils.getFullStackTrace(e));
			}
		}
		return VoidReturn.INSTANCE;
	}
}
