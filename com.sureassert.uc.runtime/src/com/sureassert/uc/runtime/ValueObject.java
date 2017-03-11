/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Base class used by classes that require compare-by-value.
 * 
 * NOTE: Implementing classes MUST be immutable.
 * 
 * @author Nathan Dolan
 * 
 */
public abstract class ValueObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private Object[] state;
	private int hashCode = Integer.MIN_VALUE;

	private boolean gotState() {

		return hashCode != Integer.MIN_VALUE;
	}

	/**
	 * The state of this object used to compare it to other instances.
	 * All objects returned must be immutable.
	 * 
	 * @return the immutable state of this instance.
	 */
	protected abstract Object[] getImmutableState();

	private void init() {

		state = getImmutableState();

		final int prime = 31;
		hashCode = 1;
		hashCode = prime * hashCode + Arrays.deepHashCode(state);
		if (hashCode == Integer.MIN_VALUE)
			hashCode = Integer.MIN_VALUE + 1;
	}

	@Override
	public int hashCode() {

		if (!gotState())
			init();

		return hashCode;
	}

	private Object[] getImmutableStateInternal() {

		if (!gotState())
			init();

		return state;
	}

	@Override
	public boolean equals(Object obj) {

		if (!gotState())
			init();

		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueObject other = (ValueObject) obj;
		return Arrays.deepEquals(state, other.getImmutableStateInternal());
	}

	@Override
	public String toString() {

		return Arrays.toString(getImmutableStateInternal());
	}

}
