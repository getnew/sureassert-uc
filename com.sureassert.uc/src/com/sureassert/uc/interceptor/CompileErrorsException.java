/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.exception.BaseSAException;

public class CompileErrorsException extends BaseSAException {

	private static final long serialVersionUID = 1L;

	private final List<CompileError> compileErrors;

	public CompileErrorsException(List<CompileError> compileErrors) {

		this.compileErrors = compileErrors;
	}

	public List<CompileError> getCompileErrors() {

		return Collections.unmodifiableList(compileErrors);
	}
}
