/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sureassert.uc.runtime.internal.SINExpression;
import com.sureassert.uc.runtime.typeconverter.StubExpressionListTC;

public abstract class SaUCStub {

	/**
	 * A list of expressions to be used as implementations of the stubbed method.
	 * The next item in list is iterated for each invocation of the doubled method under the
	 * UseCase.
	 */
	private final List<SINType> invokeExpressions;

	private final transient Iterator<SINType> invokeExpressionsIt;

	/**
	 * Describes a stub definition error if the stub definition is invalid.
	 * If valid, it is null.
	 */
	private String error;

	private static final String GENERIC_ERROR = "Invalid stub definition";

	public SaUCStub(List<SINType> invokeExpressions) {

		this.invokeExpressions = invokeExpressions;
		this.invokeExpressionsIt = invokeExpressions.iterator();
	}

	public static SaUCStub newSaUCStub(String stubDef) throws TypeConverterException {

		int splitIdx = stubDef.lastIndexOf("=");
		if (splitIdx == -1 || splitIdx == stubDef.length()) {
			// MethodStub stub = new MethodStub(SINExpression.NULL_EXPRESSION, Collections.<SINType>
			// emptyList(), false);
			// stub.setError(GENERIC_ERROR);
			// return stub;
			splitIdx = stubDef.length();
		}

		// Get stub name (either method signature or source stub variable name)
		boolean stubThrowsException = false;
		String stubName = stubDef.substring(0, splitIdx).trim();
		if (stubName.endsWith("^")) {
			stubThrowsException = true;
			stubName = stubName.substring(0, stubName.length() - 1).trim();
		}
		ISINExpression _methodMatchExpression = null;
		String _sourceStubVarName = null;
		if (stubName.contains(".")) {
			// stub name is method sig
			try {
				_methodMatchExpression = SINExpressionFactory.get(stubName);
			} catch (Exception e) {
				MethodStub stub = new MethodStub(SINExpression.NULL_EXPRESSION, Collections.<SINType> emptyList(), stubThrowsException);
				stub.setError(e.getMessage());
				return stub;
			}
		} else {
			// stub name is source stub variable name
			_sourceStubVarName = stubName;
		}

		// Get stub expression
		List<SINType> _invokeExpressions = new ArrayList<SINType>();
		if (splitIdx == stubDef.length()) {
			_invokeExpressions.add(SINType.newFromRaw(NamedInstanceFactory.NULL_INSTANCE_NAME));
		} else {
			String stubExprStr = stubDef.substring(splitIdx + 1);
			TypeConverter<?> tc;
			SINType stubExprST;
			try {
				stubExprST = SINType.newFromRaw(stubExprStr);
				tc = TypeConverterFactory.instance.getTypeConverterForValue(stubExprST);
			} catch (TypeConverterException e) {
				MethodStub stub = new MethodStub(SINExpression.NULL_EXPRESSION, Collections.<SINType> emptyList(), stubThrowsException);
				stub.setError(e.getMessage());
				return stub;
			}
			if (tc instanceof StubExpressionListTC) {
				List<String> rawSINs = ((StubExpressionListTC) tc).getRawSINElements(stubExprST);
				for (String rawSIN : rawSINs) {
					_invokeExpressions.add(SINType.newFromRaw(rawSIN));
				}
			} else {
				_invokeExpressions.add(stubExprST);
			}
		}

		// Return method stub or source stub
		if (_methodMatchExpression != null) {
			return new MethodStub(_methodMatchExpression, _invokeExpressions, stubThrowsException);
		} else {
			if (stubThrowsException) {
				MethodStub stub = new MethodStub(SINExpression.NULL_EXPRESSION, Collections.<SINType> emptyList(), stubThrowsException);
				stub.setError("sig^=expr notation can not be used with source stubs");
				return stub;
			}
			return new SourceStub(_sourceStubVarName, _invokeExpressions);
		}
	}

	/**
	 * Gets a key that identifies this stub definition.
	 * 
	 * @return String
	 */
	public abstract String getKey();

	public List<SINType> getInvokeExpressions() {

		return invokeExpressions;
	}

	public SINType getNextInvokeExpression() {

		if (invokeExpressionsIt.hasNext())
			return invokeExpressionsIt.next();
		return invokeExpressions.get(invokeExpressions.size() - 1);
	}

	public boolean isValid() {

		return error == null;
	}

	public String getError() {

		return error;
	}

	public void setError(String error) {

		this.error = error;
	}
}
