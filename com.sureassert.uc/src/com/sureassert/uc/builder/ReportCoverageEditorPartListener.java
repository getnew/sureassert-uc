/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;

public class ReportCoverageEditorPartListener implements IPartListener2 {

	public void partActivated(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

	public void partBroughtToTop(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

	public void partClosed(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

	public void partDeactivated(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

	public void partOpened(IWorkbenchPartReference partRef) {

		System.out.println("partOpened - " + partRef.getPartName());

		IWorkbenchPart part = partRef.getPart(true);
		if (part instanceof IEditorPart) {
			IEditorInput input = ((IEditorPart) part).getEditorInput();
			SourceFileFactory sfFactory = new SourceFileFactory();
			JavaPathData jpd = new JavaPathData();
			IProgressMonitor monitor = new NullProgressMonitor();
			new CoveragePrinter(new SAUCEditorCoverageListenner()).printInfo(//
					null, sfFactory, jpd, monitor, null, input);
		}
	}

	public void partHidden(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

	public void partVisible(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

	public void partInputChanged(IWorkbenchPartReference paramIWorkbenchPartReference) {

	}

}
