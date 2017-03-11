/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;
import org.sureassert.uc.annotation.NamedClass;

import com.sureassert.uc.runtime.TypeConverterException;



public class NamedClassModel extends NameModel {

	public NamedClassModel(NamedClass namedClass) throws TypeConverterException {

		super(namedClass.name());
	}

	public NamedClassModel(IMemberValuePair[] params) throws TypeConverterException {

		super(params);
	}
}
