package com.sureassert.uc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.sureassert.uc.internal.SaUCPreferences;

public class ToggleCoverageDisplayAction implements IWorkbenchWindowActionDelegate {

	public void run(IAction action) {

		if (!SaUCPreferences.getIsCoverageEnabled()) {
			action.setChecked(false);
		} else {
			boolean enabled = !SaUCPreferences.getIsCoverageDisplayEnabled();
			SaUCPreferences.setIsCoverageDisplayEnabled(enabled);
			action.setChecked(enabled);
			// NOTE: SaUCPropertyChangeListener detects change to property and runs job
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {

		action.setChecked(SaUCPreferences.getIsCoverageDisplayEnabled());
	}

	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

}
