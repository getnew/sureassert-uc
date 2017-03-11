/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

public class PersistentDataLoadException extends Exception {

	private static final long serialVersionUID = 1L;

	public PersistentDataLoadException(Throwable e) {

		super(e);
	}

	public PersistentDataLoadException() {

		super();
	}

	public PersistentDataLoadException(String msg) {

		super(msg);
	}
}
