/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import com.sureassert.uc.runtime.ValueObject;

public class SourceInsertion extends ValueObject implements Comparable<SourceInsertion> {

	private static final long serialVersionUID = 1L;

	final String insertString;
	final int index;
	final long id;
	final int lineNum;
	private static long sourceInsertionIDSequence = 0;

	public SourceInsertion(String insertString, int index, int lineNum) {

		this.insertString = insertString;
		this.index = index;
		this.lineNum = lineNum;
		this.id = sourceInsertionIDSequence++;
	}

	@Override
	protected Object[] getImmutableState() {

		return new Object[] { id };
	}

	public int compareTo(SourceInsertion o) {

		if (index < o.index)
			return 1;
		else if (index > o.index)
			return -1;
		else if (id < o.id)
			return 1;
		else if (id > o.id)
			return -1;
		else
			return 0;
	}

	@Override
	public String toString() {

		return "[" + id + ";" + index + ";" + insertString + "]";
	}
}