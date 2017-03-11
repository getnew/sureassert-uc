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
 * Demarks a JUnit test class for a class.
 * 
 * @since 1.0
 * @author Nathan Dolan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface HasJUnit {

	/**
	 * The full class names of JUnit test classes that test this class.
	 * <p>
	 * The given JUnit test classes will be executed whenever this class changes, and whenever any
	 * other source dependencies of the JUnit tests change.
	 * </p>
	 * <p>
	 * Any test errors/failures will be marked as errors at the point they failed within this class
	 * if possible.
	 * </p>
	 * 
	 */
	public String[] jUnitClassNames();

	/**
	 * <p>
	 * Whether the JUnit tests should be executed using the master project source code or the
	 * Sureassert UC instrumented source code.
	 * </p>
	 * <p>
	 * If true (the default setting), classes will be replaced with their test doubles, stubs will
	 * be active, etc, within the context of the executed test class.
	 * </p>
	 * <p>
	 * If false, the test class will execute against the master project source and yield the same
	 * results as if executed outside the context of Sureassert UC.
	 * </p>
	 */
	public boolean useInstrumentedSource() default true;

}
