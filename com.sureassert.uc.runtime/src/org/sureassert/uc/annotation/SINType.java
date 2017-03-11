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
 * Identifies a method that can be used as a SIN Type within SIN expressions.
 * This is particularly useful for methods that return objects for frequent use
 * in your tests. For example in a project that uses the Spring DI framework,
 * you might define a SINType method that returns the Spring bean with a
 * given name from Spring context used during unit testing. <br>
 * <br>
 * The annotated method must return something (i.e. not be void), and either
 * be static or be defined in a class with a no-args constructor (in which case
 * a new instance of the class will be created each time the SIN Type is used
 * within an expression).<br>
 * <br>
 * Use the prefix defined by this annotation within your SIN expressions with
 * the same number and type of parameters as expected by the SIN Type method.
 * For example if you annotate a method that expects a <code>String</code> and a
 * <code>boolean</code> and define a prefix of <code>abc</code>,
 * you might use this in the args of your UseCase like <code>args="abc:'StringParam',true"</code><br>
 * <br>
 * NOTE: The UC Engine will not re-execute UseCases and JUnits that depend
 * upon SIN Type methods if the SIN Type method implementation is altered.
 * Therefore if you alter the SINType-annotated method implementation the
 * dependant UseCases and JUnits will only be re-run when you rebuild or Sureassert
 * UC Refresh any projects that use it.
 * 
 * @since 1.0
 * @author Nathan Dolan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface SINType {

	/**
	 * The SIN Type prefix to assign to this method.
	 */
	String prefix();
}
