/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc;

import org.eclipse.core.resources.IFile;

import com.sureassert.uc.runtime.exception.BaseSAException;

public class SAException extends BaseSAException {

	private static final long serialVersionUID = 1L;

	private IFile file;

	public SAException() {

		super();
	}

	public SAException(String message, Throwable cause) {

		super(message, cause);
	}

	public SAException(String message) {

		super(message);
	}

	public SAException(Throwable cause) {

		super("", cause);
	}

	public SAException(Throwable cause, IFile file) {

		super("", cause);
		this.file = file;
	}

	public SAException(String message, IFile file) {

		super(message);
		this.file = file;
	}

	public void setFile(IFile file) {

		this.file = file;
	}

	public IFile getFile() {

		return file;
	}

}
