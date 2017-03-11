/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.exception;

public class MethodArgumentException extends UseCaseException {

	private static final long serialVersionUID = 1L;

	public MethodArgumentException(String message) {

		super(message);
	}

	public MethodArgumentException(Throwable e) {

		super(e);
	}

}
