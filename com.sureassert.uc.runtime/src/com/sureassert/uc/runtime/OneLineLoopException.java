/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.exception.BaseSAException;

public class OneLineLoopException extends BaseSAException {

	private static final long serialVersionUID = 1L;

	private final int lineNum;

	public OneLineLoopException(int lineNum) {

		super();
		this.lineNum = lineNum;
	}

	public OneLineLoopException(int lineNum, String message, Throwable cause) {

		super(message, cause);
		this.lineNum = lineNum;
	}

	public OneLineLoopException(int lineNum, String message) {

		super(message);
		this.lineNum = lineNum;
	}

	public int getLineNum() {

		return lineNum;
	}

}
