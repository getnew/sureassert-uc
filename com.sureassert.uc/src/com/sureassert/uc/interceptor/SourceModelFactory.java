/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;

import com.sureassert.uc.internal.ProjectClassLoaders;
import com.sureassert.uc.internal.SourceFile;

public class SourceModelFactory {

	private final Set<String> tdClassNames = new HashSet<String>();
	private final ProjectClassLoaders classLoaders;

	private final Map<ICompilationUnit, SourceModel> sourceModelByCompUnit = new HashMap<ICompilationUnit, SourceModel>();

	public SourceModelFactory(ProjectClassLoaders classLoaders) {

		this.classLoaders = classLoaders;
	}

	/* package */void addTestDoubleClassName(String tdClassName) {

		tdClassNames.add(tdClassName);
	}

	public Set<String> getTestDoubleClassNames() {

		return tdClassNames;
	}

	public SourceModel getSourceModel(SourceFile file, ICompilationUnit javaUnit) //
			throws CoreException {

		SourceModel sm = sourceModelByCompUnit.get(javaUnit);
		if (sm == null) {
			sm = new SourceModel(file, javaUnit, javaUnit.getJavaProject(), classLoaders, this);
			sourceModelByCompUnit.put(javaUnit, sm);
		}
		return sm;
	}

	public Collection<SourceModel> getAllCachedSourceModels() {

		return Collections.unmodifiableCollection(sourceModelByCompUnit.values());
	}
}
