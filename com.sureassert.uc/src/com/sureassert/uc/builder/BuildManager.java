/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import org.eclipse.core.runtime.IProgressMonitor;

public interface BuildManager {

	public boolean isBuildCancelled(IProgressMonitor monitor);
}
