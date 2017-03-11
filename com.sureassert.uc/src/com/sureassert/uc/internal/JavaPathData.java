/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

public class JavaPathData {

	private final Map<IPath, ICompilationUnit> javaUnitByPath = new HashMap<IPath, ICompilationUnit>();
	private final Map<IPath, IFile> fileByPath = new HashMap<IPath, IFile>();

	public ICompilationUnit getJavaUnit(IPath javaPath, IWorkspace workspace) {

		ICompilationUnit javaUnit = javaUnitByPath.get(javaPath);
		if (javaUnit == null) {
			try {
				IFile file = workspace.getRoot().getFile(javaPath);
				javaUnit = JavaCore.createCompilationUnitFrom(file);
				javaUnitByPath.put(javaPath, javaUnit);
				fileByPath.put(javaPath, file);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		return javaUnit;
	}

	public Set<IPath> getAllCachedFilePaths() {

		return new HashSet<IPath>(fileByPath.keySet());
	}

	public IFile getFile(IPath javaPath, IWorkspace workspace) {

		IFile file = fileByPath.get(javaPath);
		if (file == null) {
			if (workspace == null)
				workspace = ResourcesPlugin.getWorkspace();
			file = workspace.getRoot().getFile(javaPath);
			ICompilationUnit javaUnit = (ICompilationUnit) JavaCore.create(file);
			javaUnitByPath.put(javaPath, javaUnit);
			fileByPath.put(javaPath, file);

		}
		return file;
	}

	public IFile getFileForAbsolutePath(IPath absPath, IWorkspace workspace) {

		IFile file = fileByPath.get(absPath);
		if (file == null) {
			if (workspace == null)
				workspace = ResourcesPlugin.getWorkspace();
			file = workspace.getRoot().getFileForLocation(absPath);
			ICompilationUnit javaUnit = (ICompilationUnit) JavaCore.create(file);
			javaUnitByPath.put(absPath, javaUnit);
			fileByPath.put(absPath, file);

		}
		return file;
	}

	public IFile getFileQuick(IPath javaPath, IWorkspace workspace) {

		IFile file = fileByPath.get(javaPath);
		if (file == null) {
			if (workspace == null)
				workspace = ResourcesPlugin.getWorkspace();
			file = workspace.getRoot().getFile(javaPath);
			fileByPath.put(javaPath, file);
		}
		return file;
	}
}
