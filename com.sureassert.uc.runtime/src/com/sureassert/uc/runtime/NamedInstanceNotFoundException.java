/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.exception.UseCaseException;

public class NamedInstanceNotFoundException extends UseCaseException {

	// private int errorLineNum = -1;

	private static final long serialVersionUID = 1L;

	public NamedInstanceNotFoundException(String message) {

		super(message);
	}

	// public void setErrorLineNum(int errorLineNum) {
	//
	// this.errorLineNum = errorLineNum;
	// }
	//
	// public int getErrorLineNum() {
	//
	// return errorLineNum;
	// }

}
