/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import org.eclipse.core.resources.IFile;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.runtime.ValueObject;

public class FileLine extends ValueObject {

	private static final long serialVersionUID = 1L;

	private final String path;

	private final Integer lineNum;

	public FileLine(IFile file, Integer lineNum) {

		this.path = EclipseUtils.getRawPath(file.getFullPath(), file.getProject()).//
				toFile().getAbsolutePath();
		this.lineNum = lineNum;
	}

	@Override
	protected Object[] getImmutableState() {

		return new Object[] { path, lineNum };
	}
}
