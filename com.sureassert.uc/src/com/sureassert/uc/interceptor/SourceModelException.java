/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import org.eclipse.core.resources.IFile;

import com.sureassert.uc.SAException;

/**
 * Source model exceptions found at a specified line number in the source file
 * 
 * @author Nathan Dolan
 */
public class SourceModelException extends SAException {

	private static final long serialVersionUID = 1L;

	private final int lineNum;

	public SourceModelException(String message, IFile file, int lineNum) {

		super(message, file);
		this.lineNum = lineNum;
	}

	public int getLineNum() {

		return lineNum;
	}
}
