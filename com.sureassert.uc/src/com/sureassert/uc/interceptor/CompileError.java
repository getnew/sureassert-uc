/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import org.eclipse.core.resources.IFile;

public class CompileError {

	private final String msg;
	private final IFile file;
	private final int lineNum;

	public CompileError(String msg, IFile file, int lineNum) {

		this.msg = msg;
		this.file = file;
		this.lineNum = lineNum;
	}

	public String getMsg() {

		return msg;
	}

	public IFile getFile() {

		return file;
	}

	public int getLineNum() {

		return lineNum;
	}

}
