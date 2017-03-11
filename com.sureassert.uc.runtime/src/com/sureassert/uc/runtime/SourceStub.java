/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.util.Collections;
import java.util.List;

public class SourceStub extends SaUCStub {

	public static final SourceStub NULL_SOURCE_STUB = new SourceStub(null, Collections.<SINType> emptyList());

	/**
	 * The name of this source stub variable; must be [a-zA-Z0-9_]
	 */
	private final String sourceStubVarName;

	SourceStub(String sourceStubVarName, List<SINType> invokeExpressions) {

		super(invokeExpressions);
		this.sourceStubVarName = sourceStubVarName;
	}

	public String getSourceStubVarName() {

		return sourceStubVarName;
	}

	@Override
	public String getKey() {

		return sourceStubVarName;
	}
}
