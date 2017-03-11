/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;

import com.sureassert.uc.runtime.TypeConverterException;

public abstract class NameModel {

	private final String name;

	protected NameModel(String name) {

		this.name = name;
	}

	protected NameModel(IMemberValuePair[] params) throws TypeConverterException {

		this(params, "name");
	}

	protected NameModel(IMemberValuePair[] params, String argName) throws TypeConverterException {

		this(params, argName, null);
	}

	protected NameModel(IMemberValuePair[] params, String argName, NameAdjuster nameAdjuster) throws TypeConverterException {

		String tmpName = null;
		for (IMemberValuePair param : params) {
			if (param.getMemberName().equals(argName)) {
				tmpName = (String) param.getValue();
			}
		}
		if (nameAdjuster != null)
			name = nameAdjuster.adjustName(tmpName);
		else
			name = tmpName;
	}

	public String getName() {

		return name;
	}

	public interface NameAdjuster {

		String adjustName(String name);
	}
}
