/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

public class IntHolder {

	private int value;
	private boolean locked;

	public IntHolder(int value) {

		this.value = value;
	}

	public int getValue() {

		return value;
	}

	public void setValue(int value) {

		if (!locked)
			this.value = value;
	}

	public void lock() {

		locked = true;
	}

	public void unlock() {

		locked = false;
	}

	public boolean isLocked() {

		return locked;
	}

	public void add(int addValue) {

		if (!locked)
			this.value += addValue;
	}
}
