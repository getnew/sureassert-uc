/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.function;

import com.sureassert.uc.runtime.exception.BaseSAException;

public class FunctionException extends BaseSAException {

	private static final long serialVersionUID = 1L;

	public FunctionException(String message) {

		super(message);
	}

	public FunctionException(Throwable e) {

		super("", e);
	}

}
