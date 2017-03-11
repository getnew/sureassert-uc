/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

public class Timer {

	private static final boolean ENABLED = false;

	long startTime = System.nanoTime();
	String name;
	boolean nanos = false;

	public Timer(String name) {

		this.name = name;
	}

	public Timer(String name, boolean nanos) {

		this.name = name;
		this.nanos = nanos;
	}

	public void printExpiredTime() {

		if (ENABLED)
			com.sureassert.uc.runtime.BasicUtils.debug("Timer: " + name + " took " + ((System.nanoTime() - startTime) / //
					(nanos ? 1 : 1000000)) + (nanos ? "nanosecs" : "ms"));
	}
}
