/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

public class BlockAwareSourceInsertion extends SourceInsertion {

	private static final long serialVersionUID = 1L;
	final int endBlockindex;

	public BlockAwareSourceInsertion(String insertString, int index, int endBlockIndex, int lineNum) {

		super(insertString, index, lineNum);
		this.endBlockindex = endBlockIndex;
	}
}
