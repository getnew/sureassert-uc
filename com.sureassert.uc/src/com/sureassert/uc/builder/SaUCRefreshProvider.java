/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.refresh.IRefreshMonitor;
import org.eclipse.core.resources.refresh.IRefreshResult;
import org.eclipse.core.resources.refresh.RefreshProvider;

public class SaUCRefreshProvider extends RefreshProvider {

	@Override
	public IRefreshMonitor installMonitor(IResource arg0, IRefreshResult arg1) {

		// System.out.println("hit installMonitor");
		return null;
	}

}
