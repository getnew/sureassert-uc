package com.sureassert.uc.runtime.saserver;

import com.sureassert.uc.runtime.ValueObject;

public class KillBuildServerMessage extends ValueObject implements SAServerMessage<VoidReturn> {

	private static final Object[] nullState = new Object[] {};

	public VoidReturn execute() {

		// do nothing; checked by BuildServer by type
		return null;
	}

	public Class<VoidReturn> getReturnType() {

		return VoidReturn.class;
	}

	@Override
	protected Object[] getImmutableState() {

		return nullState;
	}
}
