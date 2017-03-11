/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.exception;

/**
 * Error denoting that a build has been interrupted and should terminate
 * 
 * @author Nathan Dolan
 * 
 */
public class SAUCBuildInterruptedError extends Error {

	private static final long serialVersionUID = 1L;

	public SAUCBuildInterruptedError() {

		super();
	}

	public SAUCBuildInterruptedError(String message, Throwable cause) {

		super(message, cause);
	}

	public SAUCBuildInterruptedError(String message) {

		super(message);
	}

	public SAUCBuildInterruptedError(Throwable cause) {

		super(cause);
	}

}
