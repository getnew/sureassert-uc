/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.exception.SARuntimeException;

public class CircularDependencyException extends SARuntimeException {

	private static final long serialVersionUID = 1L;

	public CircularDependencyException(String message) {

		super(message);
	}

	@Override
	public boolean displayExceptionName() {

		return false;
	}
}
