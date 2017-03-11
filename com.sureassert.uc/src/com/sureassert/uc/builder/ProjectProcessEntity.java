/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.sureassert.uc.internal.ProcessEntity;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.ValueObject;

public class ProjectProcessEntity extends ValueObject {

	private static final long serialVersionUID = 1L;

	private final IJavaProject javaProject;

	private final Map<IPath, Set<Signature>> affectedFileSigs;

	public ProjectProcessEntity(SerializableProjectProcessEntity ppe) {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(ppe.getJavaProjectName());
		javaProject = JavaCore.create(project);

		if (ppe.getAffectedFileSigs() != null) {
			affectedFileSigs = new HashMap<IPath, Set<Signature>>();
			for (Entry<String, Set<Signature>> entry : ppe.getAffectedFileSigs().entrySet()) {
				affectedFileSigs.put(new Path(entry.getKey()), entry.getValue());
			}
		} else {
			affectedFileSigs = null;
		}
	}

	public ProjectProcessEntity(IJavaProject javaProject, Set<IPath> affectedFiles) {

		this.javaProject = javaProject;
		if (affectedFiles == null) {
			affectedFileSigs = null;
		} else {
			affectedFileSigs = new LinkedHashMap<IPath, Set<Signature>>();
			for (IPath affectedFile : affectedFiles) {
				affectedFileSigs.put(affectedFile, null);
			}
		}
	}

	private ProjectProcessEntity(IJavaProject javaProject, Map<IPath, Set<Signature>> affectedFileSigs) {

		this.javaProject = javaProject;
		this.affectedFileSigs = affectedFileSigs;
	}

	public static ProjectProcessEntity newFromProcessEntities(//
			IJavaProject javaProject, Set<ProcessEntity> processEntities) {

		Map<IPath, Set<Signature>> affectedFileSigs = new LinkedHashMap<IPath, Set<Signature>>();
		for (ProcessEntity processEntity : processEntities) {
			affectedFileSigs.put(processEntity.getJavaPath(), processEntity.getProcessSigs());
		}
		return new ProjectProcessEntity(javaProject, affectedFileSigs);
	}

	@Override
	protected Object[] getImmutableState() {

		return new Object[] { javaProject.getElementName(), affectedFileSigs };
	}

	public IJavaProject getJavaProject() {

		return javaProject;
	}

	public Map<IPath, Set<Signature>> getAffectedFileSigs() {

		return affectedFileSigs;
	}

	public void setCurrentClassLoaders() {

	}

}
