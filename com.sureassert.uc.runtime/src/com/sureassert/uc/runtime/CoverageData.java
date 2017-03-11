/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.Serializable;

public class CoverageData implements Serializable {

	private static final long serialVersionUID = 1;

	private int numCodeLines;
	private int linesCovered;

	public int getNumCodeLines() {

		return numCodeLines;
	}

	public int getLinesCovered() {

		return linesCovered;
	}

	public void incrementLinesCovered() {

		linesCovered++;
	}

	public void incremenetNumCodeLines() {

		numCodeLines++;
	}

}
