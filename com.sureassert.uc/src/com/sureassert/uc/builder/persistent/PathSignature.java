/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder.persistent;

import org.eclipse.core.runtime.IPath;

import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.ValueObject;

public class PathSignature extends ValueObject {

	private static final long serialVersionUID = 1L;

	private final IPath path;

	private final Signature signature;

	public PathSignature(IPath path, Signature signature) {

		super();
		this.path = path;
		this.signature = signature;
	}

	@Override
	protected Object[] getImmutableState() {

		return new Object[] { path, signature };
	}

	public IPath getPath() {

		return path;
	}

	public Signature getSignature() {

		return signature;
	}

}
