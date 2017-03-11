/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder.persistent;

import java.io.Serializable;

public class Path extends org.eclipse.core.runtime.Path implements Serializable {

	private static final long serialVersionUID = 1L;

	public Path() {

		super("");
	}

	public Path(String fullPath) {

		super(fullPath);
	}

	public Path(String device, String path) {

		super(device, path);
	}
}
