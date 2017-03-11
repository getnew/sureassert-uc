/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.CoverageReporter;

public class ToggleNatureAction implements IObjectActionDelegate {

	private ISelection selection;

	private static final String ACTION_TEXT = "Sureassert UC";
	private static final String DISABLE = "Disable";
	private static final String ENABLE = "Enable";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {

		boolean enable = !DISABLE.equals(action.getText());
		if (selection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				}
				if (project != null) {
					try {
						boolean isActive = setNature(project, enable);
						if (!isActive) {
							deleteMarkers(project);
							action.setText(ACTION_TEXT + " (disabled)");
						} else {
							action.setText(ACTION_TEXT + " (enabled)");
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void deleteMarkers(IProject project) throws CoreException {

		if (project != null && project.exists()) {
			for (IPath javaPath : EclipseUtils.getAllFiles(project, "java")) {
				IFile file = project.getWorkspace().getRoot().getFile(javaPath);

				file.deleteMarkers(MarkerUtils.UC_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(MarkerUtils.UC_INFO_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(MarkerUtils.UC_WARNING_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(MarkerUtils.JUNIT_PROBLEM_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(MarkerUtils.JUNIT_INFO_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(SAUCEditorCoverageListenner.COVERAGE_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(SAUCEditorCoverageListenner.PARTIAL_COVERAGE_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(SAUCEditorCoverageListenner.CODE_STUBBED_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(SAUCEditorCoverageListenner.COVERAGE_REQUIRED_MARKER_TYPE, false, IResource.DEPTH_ONE);
				file.deleteMarkers(CoverageReporter.MARKER_TYPE_COVERAGE_REPORT, false, IResource.DEPTH_ZERO);
				file.deleteMarkers(CoverageReporter.MARKER_TYPE_COVERAGE_STAT, false, IResource.DEPTH_ZERO);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 * org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

		selectionChanged(action, targetPart.getSite().getPage().getSelection());

		if (selection instanceof IStructuredSelection) {
			for (@SuppressWarnings("rawtypes")
			Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				}
				if (project != null) {
					try {
						boolean isEnabled = isEnabled(project);
						if (isEnabled)
							action.setText(DISABLE);
						else
							action.setText(ENABLE);

					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	/**
	 * Toggles sample nature on a project
	 * 
	 * @param project
	 *            to have sample nature added or removed
	 * @throws CoreException
	 */
	private boolean setNature(IProject project, boolean enable) throws CoreException {

		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		if (!enable) {
			for (int i = 0; i < natures.length; ++i) {
				if (SAUCNature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
				}
			}
			return false;
		} else {
			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = SAUCNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
			return true;
		}
	}

	private boolean isEnabled(IProject project) throws CoreException {

		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			for (int i = 0; i < natures.length; ++i) {
				if (SAUCNature.NATURE_ID.equals(natures[i])) {
					// The nature exists
					return true;
				}
			}
			return false;
		} catch (Throwable e) {
			return false;
		}
	}

}
