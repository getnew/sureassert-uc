/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

public interface IUCReexecutor {

	public static final String UC_REEXECUTOR_EXTENSION_ID = "com.sureassert.uc.runtime.ucReexecutor";

	/**
	 * Executes the UseCase with the given name. The use-case must have already been
	 * executed prior to invocation of this method.
	 * 
	 * @param useCaseName The name of a UseCase
	 * @param reexecName The name to which the returned instance should be assigned
	 * @return true if a UseCase with the given name was found, otherwise false.
	 */
	public boolean reExecuteUseCase(String useCaseName, String reexecName);

}
