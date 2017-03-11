/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.sureassert.uc.EclipseUtils;

public class RefreshFileActionDelegate implements IObjectActionDelegate {

	private ISelection selection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {

		if (selection instanceof IStructuredSelection) {
			for (Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IFile file = null;
				if (element instanceof IFile) {
					file = (IFile) element;
				} else if (element instanceof IAdaptable) {
					file = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
				}
				if (file != null) {
					// Refresh
					try {
						file.touch(null);
					} catch (CoreException e) {
						EclipseUtils.reportError(e);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 * org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {

		this.selection = selection;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

		// selectionChanged(action, targetPart.getSite().getPage().getSelection());
		//
		// if (selection instanceof IStructuredSelection) {
		// for (@SuppressWarnings("rawtypes")
		// Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
		// Object element = it.next();
		// IProject project = null;
		// if (element instanceof IProject) {
		// project = (IProject) element;
		// } else if (element instanceof IAdaptable) {
		// project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
		// }
		// if (project != null) {
		// }
		// }
		// }

	}

}