package com.sureassert.uc.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.sureassert.uc.runtime.exception.EvaluatorException;

public interface ISINExpression {

	public void parse() throws TypeConverterException, NamedInstanceNotFoundException;

	public Object getInstance(ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException;

	public SINType getInstanceType() throws TypeConverterException, NamedInstanceNotFoundException;

	public String getMethodName() throws TypeConverterException, NamedInstanceNotFoundException;

	public List<SINType> getParams() throws TypeConverterException, NamedInstanceNotFoundException;

	public String getRawSINExpression() throws TypeConverterException, NamedInstanceNotFoundException;

	public String getAssociatedMessage();

	public boolean isEmpty();

	public enum DefaultToType {
		NONE, RETVAL, ARG
	};

	public Signature getSignature(ClassLoader cl) throws TypeConverterException, NamedInstanceNotFoundException, EvaluatorException;

	/**
	 * Invokes this SINExpression.
	 * 
	 * @param classLoader The classloader to use when executing the expression.
	 * @param defaultToBoolean If true, where the expression evaluates to a non-boolean type or is
	 *            simply "true" or "false", assumes the expression is X and returns the evaluation
	 *            of the expression <code>=(X,retval)</code>
	 * @return the result of the invocation, i.e. the value of the expression.
	 * @throws NamedInstanceException
	 */
	public Object invoke(Object instanceObj, ClassLoader classLoader, DefaultToType defaultTo) throws SecurityException, NoSuchMethodException, EvaluatorException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, TypeConverterException, NamedInstanceNotFoundException, InstantiationException, NamedInstanceException;

}