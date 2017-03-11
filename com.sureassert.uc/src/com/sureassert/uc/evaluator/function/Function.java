/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.function;

/**
 * A function that operates on a value or values of a specified
 * type and returns a value of a specified type.
 * 
 * @author Nathan Dolan
 * 
 * @param <P> The parameter type of this function
 * @param <R> The return type of this function
 */
public interface Function<P, R> {

	/**
	 * Gets the short unique ID used to identify this function.
	 * 
	 * @return The function prefix ID
	 */
	public String getPrefixID();

	/**
	 * Execute this function on the given argument.
	 * 
	 * @param param The function argument
	 * @return The function result
	 */
	public R execute(Object instance, P... params) throws FunctionException;

	/**
	 * Returns whether this function supports the given argument list.
	 * <p>
	 * For example if the function expects 2 arguments and the given list contains 1, this method
	 * would return false.
	 * 
	 * @param param
	 * @return true if this function supports the given argument list, else
	 *         false.
	 */
	public boolean supportsParams(P... params);
}
