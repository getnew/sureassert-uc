package com.sureassert.uc.runtime.saserver;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.sureassert.uc.runtime.ICoverageAware;

public class CoverageMessage extends AbstractCoverageMessage {

	private static final long serialVersionUID = 1L;

	private final int lineNum;
	private final String className;

	public CoverageMessage(int lineNum, String className) {

		this.lineNum = lineNum;
		this.className = className;
	}

	public VoidReturn execute() {

		for (final ICoverageAware coverageNotifier : coverageNotifiers) {
			try {
				coverageNotifier.notifyCodeCovered(lineNum, className);
			} catch (Throwable e) {
				System.err.println(ExceptionUtils.getFullStackTrace(e));
			}
		}

		return VoidReturn.INSTANCE;
	}

}
