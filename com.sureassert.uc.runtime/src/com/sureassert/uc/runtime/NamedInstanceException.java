/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.exception.UseCaseException;

public class NamedInstanceException extends UseCaseException {

	private static final long serialVersionUID = 1L;

	public NamedInstanceException(String message) {

		super(message);
	}

}
