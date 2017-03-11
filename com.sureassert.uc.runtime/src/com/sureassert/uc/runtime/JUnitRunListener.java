/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import com.sureassert.uc.runtime.exception.SARuntimeException;

public class JUnitRunListener extends RunListener {

	@Override
	public void testStarted(Description description) {

		try {
			PersistentDataFactory.getInstance().setCurrentJUnitMethodName(//
					description.getMethodName());
		} catch (NoSuchMethodError e) {
			throw new SARuntimeException("<unknown_pre_JUnit_4_6>");
		}
	}
}
