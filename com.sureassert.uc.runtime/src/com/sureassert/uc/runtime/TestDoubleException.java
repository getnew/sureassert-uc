/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.exception.SARuntimeException;

public class TestDoubleException extends SARuntimeException {

	private static final long serialVersionUID = 1L;

	public TestDoubleException(String message) {

		super(message);
	}

	public TestDoubleException(String message, Throwable e) {

		super(message, e);
	}

	public TestDoubleException(Throwable e) {

		super(e);
	}

}
