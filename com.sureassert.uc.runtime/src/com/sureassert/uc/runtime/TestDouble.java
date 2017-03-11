/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A TestDouble is responsible for substituting an attempted invocation on an instance of a doubled
 * class with a "test double" response. Fakes and Stubs are both examples of a test double. *
 * <p>
 * The objective of a TestDouble is to remove run-time dependencies on a given class and its
 * dependency graph. The run-time dependency is serviced by a pre-prepared (static) response or a
 * fake implementation providing a dynamic response.
 * <p>
 * A SureAssert TestDouble does not need to implement an interface, does not need to extend the
 * doubled class and does not even need to implement all the methods of the doubled class. Only
 * those methods that are known to be invoked need have a test-double implementation.
 * <p>
 * Mocks are another example, but are not supported. This is because mocks introduce dependencies on
 * behaviour that is encapsulated behind method contracts, including all descendant nested
 * contracts. As such, when an implementation changes technically, even where there is no functional
 * and contractual change, mock definitions must change. I.e. one technical change can lead to the
 * invalidation of many mock definitions, leading to many test failures. UseCases should test
 * functional behaviour against defined contracts, not technical implementation. Introducing
 * dependencies between tests and technical implementation leads to technical inflexibility, for
 * example refactoring becomes difficult.
 * 
 * @author Nathan Dolan
 * 
 */
public interface TestDouble extends Serializable {

	/**
	 * Gets the unique name of this TestDouble instance.
	 * 
	 * @return name
	 */
	public String getName();

	/**
	 * Gets the name of the test double class.
	 * 
	 * @return
	 */
	public String getTestDoubleClassName();

	/**
	 * Gets the name of the class that this test double doubles.
	 * 
	 * @return doubled class
	 */
	public String getDoubledClassName();

	/**
	 * Gets the toString of the IPath of the source file declaring the TestDouble.
	 * 
	 * @return
	 */
	public String getJavaPathStr();

	/**
	 * Callback method that can optionally be invoked when the given method is executed on the given
	 * object with the given arguments, as dictated by the transformations performed by transform().
	 * 
	 * @param obj The object on which the invocation has been made
	 * @param method The method invoked
	 * @param args The method arguments
	 * @return A value to return from the method if applicable, else null.
	 */
	public Object intercept(Object obj, Method method, Object[] args) throws Throwable;

	/**
	 * Callback method that can optionally be invoked when the given contructor is executed on the
	 * given object with the given arguments, as dictated by the transformations performed by
	 * transform().
	 * 
	 * @param obj The object on which the invocation has been made
	 * @param method The method invoked
	 * @param args The method arguments
	 * @return A value to return from the method if applicable, else null.
	 */
	public Object intercept(Object obj, Constructor<?> constructor, Object[] args) throws Throwable;

}
