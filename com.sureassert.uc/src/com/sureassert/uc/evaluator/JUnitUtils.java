/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.sureassert.uc.EclipseUtils;

public class JUnitUtils {

	public static Object runJUnit(ClassLoader classLoader, Class<?> testClass) {

		try {
			Class<?>[] argsClass = new Class<?>[] { testClass };
			Class<?>[] args = new Class<?>[] { testClass };
			Class<?> junitCoreClass = classLoader.loadClass("org.junit.runner.JUnitCore");
			Method runClassesMethod = junitCoreClass.getMethod("runClasses", argsClass.getClass());
			return runClassesMethod.invoke(null, (Object) args);
		} catch (Exception e) {
			EclipseUtils.reportError(e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Object> getFailures(Object result) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Method getFailuresMethod = result.getClass().getMethod("getFailures");
		return (List<Object>) getFailuresMethod.invoke(result, new Object[] {});
	}

	public static Throwable getFailureException(Object failure) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Method getExceptionMethod = failure.getClass().getMethod("getException");
		return (Throwable) getExceptionMethod.invoke(failure, new Object[] {});
	}

	public static String getFailureMessage(Object failure) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Method getMessageMethod = failure.getClass().getMethod("getMessage");
		return (String) getMessageMethod.invoke(failure, new Object[] {});
	}

	public static Object getFailureDescription(Object failure) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Method getDescriptionMethod = failure.getClass().getMethod("getDescription");
		return getDescriptionMethod.invoke(failure, new Object[] {});
	}

	public static String getFailureDescriptionMethodName(Object failureDescription) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {

		try {
			Method getMethodNameMethod = failureDescription.getClass().getMethod("getMethodName");
			return (String) getMethodNameMethod.invoke(failureDescription, new Object[] {});
		} catch (Throwable e) {
			return "<unknown_pre_JUnit_4_6>";
		}
	}

}
