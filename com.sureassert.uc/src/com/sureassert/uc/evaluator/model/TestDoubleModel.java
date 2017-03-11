/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.sureassert.uc.annotation.TestDouble;

import com.sureassert.uc.runtime.TypeConverterException;

public class TestDoubleModel {

	private Class<?> doubledClass;
	private final String tdClassName;
	private final String javaPathStr;

	public TestDoubleModel(TestDouble testDouble, String tdClassName, String javaPathStr) throws TypeConverterException {

		this.tdClassName = tdClassName;
		this.javaPathStr = javaPathStr;
	}

	public TestDoubleModel(IMemberValuePair[] params, String tdClassName, IType javaType, //
			ClassLoader cl) throws TypeConverterException, ClassNotFoundException, JavaModelException {

		this.tdClassName = tdClassName;

		Class<?> _doubledClass = null;
		for (IMemberValuePair param : params) {
			if (param.getMemberName().equals("replaces")) {
				String className = (String) param.getValue();
				String[][] resolvedName = javaType.resolveType(className);
				_doubledClass = cl.loadClass(resolvedName[0][0] + "." + resolvedName[0][1]);
			}
		}
		doubledClass = _doubledClass;
		javaPathStr = javaType.getPath().toString();
	}

	public com.sureassert.uc.runtime.ClassDouble newTestDouble() {

		return new com.sureassert.uc.runtime.ClassDouble(doubledClass, tdClassName, javaPathStr);
	}
}
