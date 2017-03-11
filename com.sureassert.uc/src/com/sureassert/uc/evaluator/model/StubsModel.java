/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.sureassert.uc.annotation.Stubs;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.runtime.ErrorContainingModel;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.SaUCStub;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TypeConverterException;

/**
 * @author Nathan
 */
public class StubsModel implements Cloneable, ErrorContainingModel {

	public static final String DEFAULT_NOARGS_CONSTRUCTOR = //
	NamedInstanceFactory.SPECIAL_CHAR + "noargs" + NamedInstanceFactory.SPECIAL_CHAR;
	public static final String NO_CONSTRUCTOR = //
	NamedInstanceFactory.SPECIAL_CHAR + "static" + NamedInstanceFactory.SPECIAL_CHAR;

	private static final String STUBS_ANNOTATION_CLASSNAME = Stubs.class.getSimpleName();

	private final int declaredLineNum;
	private final Signature signature;
	private String error;
	private final String[] stubs;
	private SaUCStub[] stubObjs;

	// TODO: mocks

	public StubsModel(Stubs stubsAnn, Signature signature) throws TypeConverterException {

		declaredLineNum = -1;
		this.signature = signature;
		stubs = new String[stubsAnn.stubs().length];
		for (int i = 0; i < stubsAnn.stubs().length; i++) {
			stubs[i] = stubsAnn.stubs()[i];
		}
	}

	protected StubsModel(int declaredLineNum, Signature signature, String error, String[] stubs) {

		this.declaredLineNum = declaredLineNum;
		this.signature = signature;
		this.error = error;
		this.stubs = stubs;
	}

	@Override
	public StubsModel clone() {

		return new StubsModel(declaredLineNum, signature, error, stubs);
	}

	public StubsModel(IMemberValuePair[] params, Signature signature, int declaredLineNum) {

		String[] _stubs = null;

		for (IMemberValuePair param : params) {

			if (param.getMemberName().equals("stubs")) {
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_stubs = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_stubs[i] = (String) value[i];
				}
			}
		}

		stubs = _stubs;
		this.declaredLineNum = declaredLineNum;
		this.signature = signature;
	}

	protected String[] getRawStubs() {

		return stubs;
	}

	public SaUCStub[] getStubs() throws TypeConverterException {

		if (stubs == null)
			return new SaUCStub[] {};
		stubObjs = new SaUCStub[stubs.length];
		for (int i = 0; i < stubs.length; i++) {

			stubObjs[i] = SaUCStub.newSaUCStub(stubs[i]);
		}
		return stubObjs;
	}

	public int getDeclaredLineNum() {

		return declaredLineNum;
	}

	public Signature getSignature() {

		return signature;
	}

	public void setError(String error) {

		this.error = error;
	}

	public String getError() {

		if (error == null && stubObjs != null) {
			for (SaUCStub stub : stubObjs) {
				if (stub.getError() != null)
					error = stub.getError();
			}
		}
		return error;
	}

	public boolean isValid() {

		return error == null;
	}

	@Override
	public String toString() {

		return stubs.toString();
	}

	/**
	 * Gets all the method stubs defined in the JUnit class.
	 * 
	 * @return Map of method name to MethodStub array.
	 * @throws JavaModelException
	 * @throws TypeConverterException
	 */
	public static Map<String, SaUCStub[]> getMethodStubs(IType jUnitClassType, ClassLoader classLoader) //
			throws JavaModelException, TypeConverterException {

		if (!EclipseUtils.hasMethodAnnotations(jUnitClassType, STUBS_ANNOTATION_CLASSNAME, classLoader, false)) {
			return Collections.emptyMap();
		}
		Map<String, SaUCStub[]> stubsByMethodName = new HashMap<String, SaUCStub[]>();
		for (IMethod method : jUnitClassType.getMethods()) {
			for (Entry<String, IAnnotation> annEntry : EclipseUtils.getAnnotations(method).entrySet()) {
				if (annEntry.getKey().equals(STUBS_ANNOTATION_CLASSNAME)) {
					IAnnotation stubsAnn = annEntry.getValue();
					Signature methodSig = SignatureTableFactory.instance.getSignature(//
							method.getDeclaringType().getFullyQualifiedName(), //
							method.getElementName(), //
							new String[] {}); // no params on test method
					StubsModel stubsModel = new StubsModel(stubsAnn.getMemberValuePairs(), methodSig, -1);
					stubsByMethodName.put(method.getElementName(), stubsModel.getStubs());
				}
			}
		}
		return stubsByMethodName;
	}

}
