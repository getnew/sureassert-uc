/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.exception.UseCaseException;

public class TypeConverterException extends UseCaseException {

	private static final long serialVersionUID = 1L;

	public TypeConverterException(String message) {

		super(message);
	}

	public TypeConverterException(String message, int sourceLocation, Throwable e) {

		super(message, sourceLocation, e);
	}

}
