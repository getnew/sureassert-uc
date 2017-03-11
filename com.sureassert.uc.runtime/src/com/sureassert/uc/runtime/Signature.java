/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.lang.ArrayUtils;

public class Signature extends ValueObject {

	private static final long serialVersionUID = 1L;

	public static final String[] CLASS_PARAMS = null;

	public static final String ANY_MEMBER_NAME = null;

	/* package */static final Signature NULL_SIGNATURE = new Signature(null, null, new String[] {});

	public static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	private final String className;

	private final String memberName;

	private final String[] paramClassNames;

	private String toString;

	/* package */Signature(AccessibleObject ao) {

		this(getDeclaringClassName(ao), getName(ao), getParamTypeNames(ao));
	}

	/* package */Signature(Class<?> clazz) {

		this(clazz.getName(), ANY_MEMBER_NAME, CLASS_PARAMS);
	}

	/* package */Signature(Method method) {

		this(method.getDeclaringClass().getName(), method.getName(), getClassNames(method.getParameterTypes()));
	}

	/* package */Signature(Constructor<?> constructor) {

		this(constructor.getDeclaringClass().getName(), CONSTRUCTOR_METHOD_NAME, //
				BasicUtils.isNonStaticInnerClass(constructor.getDeclaringClass()) ? //
				getClassNames((Class<?>[]) ArrayUtils.subarray(constructor.getParameterTypes(), 1, Integer.MAX_VALUE)) : //
				getClassNames(constructor.getParameterTypes()));
	}

	/* package */Signature(String className) {

		this.className = className;
		this.memberName = ANY_MEMBER_NAME;
		this.paramClassNames = CLASS_PARAMS;
	}

	/* package */Signature(String className, String memberName) {

		this(className, memberName, new String[] {});
	}

	/* package */Signature(String className, String memberName, String paramClassNames) {

		this(className, memberName, //
				paramClassNames == null || paramClassNames.trim().equals("") ? //
				new String[] {} : paramClassNames.split(","));
	}

	/* package */Signature(String className, String memberName, String[] paramClassNames) {

		// Treat AspectJ around advice methods the same as the methods they wrap
		// methodName_aroundBodyN - where N is an integer
		// if (memberName != null) {
		// int ajMethodIdx = memberName.indexOf("_aroundBody");
		// if (ajMethodIdx > -1 && Character.isDigit(memberName.charAt(memberName.length() - 1)))
		// memberName = memberName.substring(0, ajMethodIdx);
		// }

		this.className = className;
		this.memberName = memberName;
		this.paramClassNames = BasicUtils.trim(paramClassNames);
	}

	public String getClassName() {

		return className;
	}

	public String getMemberName() {

		return memberName;
	}

	public String[] getParamClassNames() {

		return paramClassNames;
	}

	public String getParamClassNamesAppended() {

		StringBuilder pcnAp = new StringBuilder();
		for (int i = 0; i < paramClassNames.length; i++) {
			pcnAp.append(paramClassNames[i]);
			if (i < paramClassNames.length - 1)
				pcnAp.append(',');
		}
		return pcnAp.toString();
	}

	public boolean isConstructor() {

		return BasicUtils.equals(memberName, CONSTRUCTOR_METHOD_NAME);
	}

	public boolean isClass() {

		return BasicUtils.equals(memberName, ANY_MEMBER_NAME) && paramClassNames.equals(CLASS_PARAMS);
	}

	@Override
	protected Object[] getImmutableState() {

		// first object is checked first so make it the most likely to differ for performance
		return new Object[] { memberName, className, paramClassNames };
	}

	private static String[] getClassNames(Class<?>[] classes) {

		String[] classNames = new String[classes.length];
		for (int i = 0; i < classes.length; i++) {
			classNames[i] = classes[i].getName();
		}
		return classNames;
	}

	private static String[] getParamTypeNames(AccessibleObject ao) {

		if (ao instanceof Method)
			return getClassNames(((Method) ao).getParameterTypes());
		else if (ao instanceof Constructor<?>) {
			if (BasicUtils.isNonStaticInnerClass(((Constructor<?>) ao).getDeclaringClass()))
				return getClassNames((Class<?>[]) ArrayUtils.subarray(((Constructor<?>) ao).getParameterTypes(), 1, Integer.MAX_VALUE));
			else
				return getClassNames(((Constructor<?>) ao).getParameterTypes());
		} else
			return new String[] {};
	}

	private static String getName(AccessibleObject ao) {

		if (ao instanceof Method)
			return ((Method) ao).getName();
		else if (ao instanceof Constructor<?>)
			return CONSTRUCTOR_METHOD_NAME;
		else if (ao instanceof Field)
			return ((Field) ao).getName();
		else
			return null;
	}

	private static String getDeclaringClassName(AccessibleObject ao) {

		if (ao instanceof Method)
			return ((Method) ao).getDeclaringClass().getName();
		else if (ao instanceof Constructor<?>)
			return ((Constructor<?>) ao).getDeclaringClass().getName();
		else if (ao instanceof Field)
			return ((Field) ao).getDeclaringClass().getName();
		else
			return null;
	}

	private String _toString() {

		if (memberName == null)
			return className;
		StringBuilder paramClassNamesStr = new StringBuilder();
		for (int i = 0; i < paramClassNames.length; i++) {
			paramClassNamesStr.append(paramClassNames[i]);
			if (i < paramClassNames.length - 1)
				paramClassNamesStr.append(", ");
		}
		return paramClassNames.length == 0 ? String.format("%s.%s", className, memberName) : //
		String.format("%s.%s(%s)", className, memberName, paramClassNamesStr);
	}

	@Override
	public String toString() {

		synchronized (this) {
			if (toString == null) {
				toString = _toString();
			}
		}
		return toString;
	}

	public String toSimpleString() {

		if (memberName == null)
			return BasicUtils.getSimpleClassName(className);
		StringBuilder paramClassNamesStr = new StringBuilder();
		for (int i = 0; i < paramClassNames.length; i++) {
			paramClassNamesStr.append(BasicUtils.getSimpleClassName(paramClassNames[i]));
			if (i < paramClassNames.length - 1)
				paramClassNamesStr.append(", ");
		}
		return paramClassNames.length == 0 ? String.format("%s.%s", //
				BasicUtils.getSimpleClassName(className), memberName) : //
		String.format("%s.%s(%s)", BasicUtils.getSimpleClassName(className), //
				memberName, paramClassNamesStr);
	}

	public String getClassMethodNameKey() {

		if (memberName == null)
			return className;
		else
			return className + "." + memberName;
	}

	public static String getClassMethodNameKey(Class<?> clazz, String memberName) {

		if (memberName == null)
			return clazz.getName();
		else
			return clazz.getName() + "." + memberName;
	}

	public String getSimpleClassMethodNameKey() {

		if (memberName == null)
			return BasicUtils.getSimpleClassName(className);
		else
			return BasicUtils.getSimpleClassName(className) + "." + memberName;
	}
}
