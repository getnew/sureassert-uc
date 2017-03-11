/*
 * Copyright 2011 Nathan Dolan. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this 
 *    list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER ``AS IS'' AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO 
 * EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 * 
 */
package org.sureassert.uc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows multiple use-cases to be defined.
 * 
 * @since 1.0
 * @author Nathan Dolan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface MultiUseCase {

	/**
	 * The set of UseCases to execute
	 */
	UseCase[] usecases() default {};

	/**
	 * Shorthand for <code>usecases</code>
	 */
	UseCase[] uc() default {};

	/**
	 * A list of method or source stub definitions.
	 * Stubs are defined as either:
	 * <p>
	 * <li><b>Source stubs:</b> <code>sourceStubVar=expression</code>, where
	 * <code>sourceStubVar</code> is the name of a source stub variable defined in-line within code
	 * comments of any class executed under the UseCase. <code>expression</code> is a SIN Expression
	 * that replaces the implementation of the stubbed method.
	 * <li><b>Method stubs:</b> <code>signature=expression</code>, where <code>signature</code> is
	 * the signature of the method to be stubbed and expression is a SIN Expression that replaces
	 * the implementation of the stubbed method.
	 * </p>
	 * <br>
	 * Method stubs can also be defined as <code>signature=[sl:expression]</code>, in order for a
	 * method to be assigned a &quot;stub list&quot; of expressions to be used as return values of
	 * the stubbed method. The items in the list are iterated for each invocation of the stubbed
	 * method by the UseCase. Where the number of invocations exceeds the number of stub
	 * expressions in the list, the last defined stub expression is used. <br>
	 * <br>
	 * Use <code>signature^=exceptionExpression or [sl:exceptionExpression]</code> to throw an
	 * exception from the stubbed method, where exceptionExpression is an expression which
	 * evaluates to an exception throwable from the stubbed method. <br>
	 * <br>
	 * Method <code>signature</code>s are defined as <code>class.method</code>,
	 * <code>class.method()</code> or <code>class.method(arg1class, ..., argNclass)</code> where:
	 * <li><code>class</code> is the simple or fully-qualified class name of the stubbed method.
	 * Fully-qualified names must use <code>/</code> as the package separator rather than
	 * <code>.</code> <li><code>method</code> is the name of the method to stub and <li>optionally
	 * <code>argXclass</code> is the simple or fully-qualified class name of each argument of the
	 * stubbed method. Note that methods are matched based on whether the classes of arguments
	 * passed at runtime are assignable to the specified argument classes.<br>
	 * <br>
	 * If the <code>class.method</code> notation is used, the stub expression will be applied to all
	 * methods with the given name.
	 */
	String[] stubs() default {};

}
