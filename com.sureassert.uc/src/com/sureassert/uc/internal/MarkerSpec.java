/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

public class MarkerSpec {

	SourceFile sourceFile;
	String message;
	int lineNumber;
	int severity;
	boolean isJUnitMarker;

	public MarkerSpec(SourceFile sourceFile, String message, int lineNumber, int severity, boolean isJUnitMarker) {

		this.sourceFile = sourceFile;
		this.message = message;
		this.lineNumber = lineNumber;
		this.severity = severity;
		this.isJUnitMarker = isJUnitMarker;
	}

}