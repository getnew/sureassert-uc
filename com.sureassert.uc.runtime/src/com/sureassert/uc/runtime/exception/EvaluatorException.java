/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.exception;

public class EvaluatorException extends UseCaseException {

	private static final long serialVersionUID = 1L;

	public EvaluatorException(String message) {

		super(message);
	}

	public EvaluatorException(Throwable e) {

		super(e);
	}

}
