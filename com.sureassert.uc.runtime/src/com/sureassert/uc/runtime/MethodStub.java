/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.util.Collections;
import java.util.List;

public class MethodStub extends SaUCStub {

	public static final MethodStub NULL_METHOD_STUB = new MethodStub(null, Collections.<SINType> emptyList(), false);

	/**
	 * True if the stub expression is to be thrown as an exception
	 */
	private final boolean stubThrowsException;

	/**
	 * The method whose implementation will be stubbed.
	 * <p>
	 * A stubbed method cannot be a constructor.
	 */
	// private final Signature doubledMethodSig;

	private final ISINExpression methodMatchExpression;

	private String key = null;

	MethodStub(ISINExpression methodMatchExpression, List<SINType> invokeExpressions, boolean stubThrowsException) {

		super(invokeExpressions);
		this.methodMatchExpression = methodMatchExpression;
		this.stubThrowsException = stubThrowsException;
	}

	public ISINExpression getMethodMatchExpression() {

		return methodMatchExpression;
	}

	public boolean getStubThrowsException() {

		return stubThrowsException;
	}

	@Override
	public String getKey() {

		synchronized (this) {
			if (key == null) {
				try {
					ClassLoader cl = PersistentDataFactory.getInstance().getCurrentProjectClassLoader();
					Object instance = methodMatchExpression.getInstance(cl);
					Class<?> clazz = (instance instanceof Class<?>) ? (Class<?>) instance : instance.getClass();
					String className = clazz.getName();
					if (className.startsWith(PersistentDataFactory.DEFAULT_GEN_PACKAGE_NAME)) {
						// key for defaultgen class is superclass/interface
						// that defaultgen class was generated for
						className = PersistentDataFactory.getInstance().getClassNameOfDefaultSuperclass(className);
					}
					key = className + "." + methodMatchExpression.getMethodName();
				} catch (Exception e) {
					key = "Error determining stubbed method";
					setError(e.getMessage());
				}
			}
		}
		return key;
	}
}
