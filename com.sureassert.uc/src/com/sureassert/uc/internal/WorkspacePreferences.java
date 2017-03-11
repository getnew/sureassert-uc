/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

public class WorkspacePreferences {

	public static final WorkspacePreferences instance = new WorkspacePreferences();

	public boolean isVisitTestClassesEnabled() {

		return false;
	}

	public String getVisitTestClassesOptionName() {

		return "Allow UseCases on JUnit test classes";
	}
}
