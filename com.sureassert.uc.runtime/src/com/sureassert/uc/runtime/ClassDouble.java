/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ClassDouble implements TestDouble {

	private static final long serialVersionUID = 1L;

	private final String doubledClassName;
	private final String testDoubleClassName;
	private final String testDoubleName;
	private transient Class<?> doubledClass_lazy;
	private final String javaPathStr;

	// private transient Class<?> testDoubleClass_lazy;

	// private transient Map<CtMethod, Method> methodToTDMethod_lazy;

	public ClassDouble(Class<?> doubledClass, String testDoubleClassName, String javaPathStr) {

		this.doubledClass_lazy = doubledClass;
		this.doubledClassName = doubledClass_lazy.getName();
		this.testDoubleName = "ClassDouble/" + BasicUtils.getSimpleClassName(doubledClassName);
		this.testDoubleClassName = testDoubleClassName;
		this.javaPathStr = javaPathStr;
	}

	// private Class<?> getTestDoubleClass() throws ClassNotFoundException {
	//
	// if (testDoubleClass_lazy == null) {
	// testDoubleClass_lazy = Class.forName(testDoubleClassName);
	// }
	// return testDoubleClass_lazy;
	// }

	public String getName() {

		return testDoubleName;
	}

	public String getTestDoubleClassName() {

		return testDoubleClassName;
	}

	public Class<?> getDoubledClass() throws TestDoubleException {

		if (doubledClass_lazy == null) {
			try {
				doubledClass_lazy = Class.forName(doubledClassName);
			} catch (ClassNotFoundException e) {
				throw new TestDoubleException(e);
			}
		}
		return doubledClass_lazy;
	}

	public String getDoubledClassName() {

		return doubledClassName;
	}

	public String getJavaPathStr() {

		return javaPathStr;
	}

	public Object intercept(Object obj, Method method, Object[] args) throws Throwable {

		// TODO Auto-generated method stub
		return null;
	}

	public Object intercept(Object obj, Constructor<?> constructor, Object[] args) throws Throwable {

		// TODO Auto-generated method stub
		return null;
	}

}
