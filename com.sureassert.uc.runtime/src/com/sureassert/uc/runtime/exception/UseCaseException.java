/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.exception;

public class UseCaseException extends BaseSAException {

	private static final long serialVersionUID = 1L;

	private int sourceLocation = -1;

	public UseCaseException() {

		super();
	}

	public UseCaseException(String message, int sourceLocation, Throwable cause) {

		super(message, cause);
		this.sourceLocation = sourceLocation;
	}

	public UseCaseException(String message, Throwable cause) {

		super(message, cause);
	}

	public UseCaseException(String message) {

		super(message);
	}

	public UseCaseException(Throwable cause) {

		super("", cause);
	}

	public void setSourceLocation(int sourceLocation) {

		this.sourceLocation = sourceLocation;
	}

	public int getSourceLocation() {

		return sourceLocation;
	}
}
