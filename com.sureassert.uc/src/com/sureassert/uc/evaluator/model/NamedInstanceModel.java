/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;
import org.sureassert.uc.annotation.NamedInstance;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;

public class NamedInstanceModel extends NameModel {

	public NamedInstanceModel(NamedInstance namedInstance, Signature signature) throws TypeConverterException {

		super(BasicUtils.appendUCNamePrefix(signature, namedInstance.name()));
	}

	public NamedInstanceModel(IMemberValuePair[] params, final Signature signature) throws TypeConverterException {

		super(params, "name", new NameAdjuster() {

			public String adjustName(String name) {

				return BasicUtils.appendUCNamePrefix(signature, name);
			}
		});
	}
}
