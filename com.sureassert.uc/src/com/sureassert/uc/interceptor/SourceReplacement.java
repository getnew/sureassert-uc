/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

public class SourceReplacement extends SourceInsertion {

	private static final long serialVersionUID = 1L;
	final int endReplacementIndex;

	public SourceReplacement(String insertString, int index, int endReplacementIndex, int lineNum) {

		super(insertString, index, lineNum);
		this.endReplacementIndex = endReplacementIndex;
	}
}