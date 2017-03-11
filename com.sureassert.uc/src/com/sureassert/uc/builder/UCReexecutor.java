/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.ArrayList;
import java.util.List;

import com.sureassert.uc.internal.UseCaseExecutionDelegate;
import com.sureassert.uc.runtime.IUCReexecutor;
import com.sureassert.uc.runtime.NamedInstanceException;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.exception.SARuntimeException;

/**
 * Class to re-execute named UseCases that are present in the NamedInstanceFactory.
 * 
 * @author Nathan Dolan
 */
public class UCReexecutor implements IUCReexecutor {

	public boolean reExecuteUseCase(String instanceName, String reexecName) {

		// Get existing contextual named instances
		NamedInstanceFactory niFactory = NamedInstanceFactory.getInstance();
		Object retvalNI = null;
		Object thisNI = null;
		List<Object> argNIs = new ArrayList<Object>();
		try {
			retvalNI = niFactory.getNamedInstance(NamedInstanceFactory.RETVAL_INSTANCE_NAME, null);
			thisNI = niFactory.getNamedInstance(NamedInstanceFactory.THIS_INSTANCE_NAME, null);
			int argNum = 1;
			while (niFactory.namedInstanceExists(NamedInstanceFactory.ARG_PREFIX + argNum)) {
				argNIs.add(niFactory.getNamedInstance(NamedInstanceFactory.ARG_PREFIX + argNum, null));
				argNum++;
			}
		} catch (NamedInstanceNotFoundException e) {
		}

		try {
			if (!instanceName.contains("/")) {
				// Add class name prefix
				String currentClassName = PersistentDataFactory.getInstance().getCurrentUseCaseSimpleClassName();
				if (currentClassName != null)
					instanceName = currentClassName + "/" + instanceName;
			}

			boolean success = UseCaseExecutionDelegate.INSTANCE.reexecuteUseCase(instanceName, reexecName);
			if (!success)
				success = UseCaseExecutionDelegate.INSTANCE.reexecuteInstanceout(instanceName, reexecName);
			return success;
		} catch (Exception e) {
			throw new SARuntimeException(e);
		} catch (StackOverflowError se) {
			throw new SARuntimeException("Cyclic dependencies detected while re-executing \"" + instanceName + "\"");

		} finally {

			// Restore contextual named instances
			try {
				niFactory.addNamedInstance(NamedInstanceFactory.RETVAL_INSTANCE_NAME, retvalNI, null);
				niFactory.addNamedInstance(NamedInstanceFactory.THIS_INSTANCE_NAME, thisNI, null);
				for (int argNum = 1; argNum < argNIs.size() + 1; argNum++) {
					niFactory.addNamedInstance(NamedInstanceFactory.ARG_PREFIX + argNum, argNIs.get(argNum - 1), null);
				}
			} catch (NamedInstanceException e) {
				throw new SARuntimeException(e);
			}
		}
	}

}
