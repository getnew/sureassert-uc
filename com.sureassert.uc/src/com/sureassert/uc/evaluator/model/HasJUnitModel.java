/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;
import org.sureassert.uc.annotation.HasJUnit;

import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;

public class HasJUnitModel {

	private final String[] jUnitClassNames;
	private final boolean useInstrumentedSource;
	private final Signature definedOnClassSig;

	public HasJUnitModel(HasJUnit hasJUnit, Signature definedOnClassSig) throws TypeConverterException {

		this.jUnitClassNames = hasJUnit.jUnitClassNames();
		this.definedOnClassSig = definedOnClassSig;
		this.useInstrumentedSource = hasJUnit.useInstrumentedSource();
	}

	public HasJUnitModel(IMemberValuePair[] params, Signature definedOnClassSig) throws TypeConverterException {

		this.definedOnClassSig = definedOnClassSig;
		boolean _useInstrumentedSource = true;
		String[] _jUnitClassNames = null;

		for (IMemberValuePair param : params) {

			if (param.getMemberName().equals("useInstrumentedSource") && param.getValue() != null) {
				_useInstrumentedSource = Boolean.parseBoolean(param.getValue().toString());
			} else if (param.getMemberName().equals("jUnitClassNames")) {
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_jUnitClassNames = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_jUnitClassNames[i] = (String) value[i];
				}
				if (_jUnitClassNames.length == 1 && _jUnitClassNames[0].length() == 0)
					_jUnitClassNames = null;
			}
		}
		useInstrumentedSource = _useInstrumentedSource;
		jUnitClassNames = _jUnitClassNames;
	}

	public Signature getDefinedOnClassSig() {

		return definedOnClassSig;
	}

	public boolean useInstrumentedSource() {

		return useInstrumentedSource;
	}

	public String[] getJUnitClassNames() {

		return jUnitClassNames;
	}

}
