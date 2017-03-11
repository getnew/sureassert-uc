/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;
import org.sureassert.uc.annotation.SINType;

import com.sureassert.uc.runtime.ErrorContainingModel;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;

public class SINTypeModel extends NameModel implements ErrorContainingModel {

	private String error;

	private final Signature definedOnSig;

	public SINTypeModel(SINType sinType, Signature definedOnSig) throws TypeConverterException {

		super(sinType.prefix());
		this.definedOnSig = definedOnSig;
	}

	public SINTypeModel(IMemberValuePair[] params, Signature definedOnSig) throws TypeConverterException {

		super(params, "prefix");
		this.definedOnSig = definedOnSig;
	}

	public String getPrefix() {

		return getName();
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
