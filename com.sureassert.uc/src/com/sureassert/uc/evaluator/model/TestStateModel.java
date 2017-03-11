/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;
import org.sureassert.uc.annotation.TestState;

import com.sureassert.uc.runtime.ErrorContainingModel;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;

public class TestStateModel extends NameModel implements ErrorContainingModel {

	private String error;

	private final Signature definedOnSig;

	public TestStateModel(TestState testState, Signature definedOnSig) throws TypeConverterException {

		super(testState.value());
		this.definedOnSig = definedOnSig;
	}

	public TestStateModel(IMemberValuePair[] params, Signature definedOnSig) throws TypeConverterException {

		super(params, "value");
		this.definedOnSig = definedOnSig;
	}

	public String getValue() {

		return getName();
	}

	public Object getTypeConvertedValue() throws TypeConverterException, NamedInstanceNotFoundException {

		if (getValue() == null)
			return null;
		SINType sinType = SINType.newFromRaw(getValue());
		return TypeConverterFactory.instance.typeConvert(sinType, null);
	}

	public void setError(String error) {

		this.error = error;
	}

	public String getError() {

		return error;
	}

	public Signature getSignature() {

		return definedOnSig;
	}
}
