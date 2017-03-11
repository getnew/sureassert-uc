/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.sureassert.uc.EclipseUtils;

public class RefreshActionDelegate implements IObjectActionDelegate {

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
				IResource file = null;
				if (element instanceof IResource) {
					file = (IResource) element;
				} else if (element instanceof IAdaptable) {
					file = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
				}
				if (file != null) {
					// Refresh
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IWorkspaceDescription description = workspace.getDescription();
					if (description.isAutoBuilding()) {
						try {
							file.touch(null);
						} catch (CoreException e) {
							EclipseUtils.reportError(e);
						}
					} else {
						try {
							description.setAutoBuilding(true);
							workspace.setDescription(description);
							file.touch(null);
							Thread.sleep(200);
						} catch (Exception e) {
							EclipseUtils.reportError(e);
						} finally {
							description.setAutoBuilding(false);
							try {
								workspace.setDescription(description);
							} catch (CoreException e) {
								EclipseUtils.reportError(e);
							}

						}
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