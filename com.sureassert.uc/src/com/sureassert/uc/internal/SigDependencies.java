/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.ValueObject;
import com.sureassert.uc.runtime.exception.SARuntimeException;

public class SigDependencies extends ValueObject {

	private static final long serialVersionUID = 1L;

	/* package */final Signature sig;

	/* package */final IPath path;

	/* package */final IFile file;

	/* package */final IJavaProject project;

	/* package */final Set<SigDependencies> clientSigDepends;

	/* package */final Set<SigDependencies> dependSigDepends;

	/* package */final ICompilationUnit javaUnit;

	SigDependencies(Signature sig, IPath path, IWorkspace workspace, //
			Map<String, Set<Signature>> processedSigsByProjectName, //
			boolean addClients, Set<Signature> excludeClients, JavaPathData javaPathData) {

		this.sig = sig;
		this.path = path;
		this.clientSigDepends = new HashSet<SigDependencies>();
		this.dependSigDepends = new HashSet<SigDependencies>();
		this.javaUnit = javaPathData.getJavaUnit(path, workspace);
		this.file = javaPathData.getFile(path, workspace);
		this.project = javaUnit.getJavaProject();
		// Add depenencies and clients. Note that if PersistentData has not yet been calculated,
		// dependencies will not be added.
		addDependsAndClients(workspace, processedSigsByProjectName, addClients, excludeClients, javaPathData, false);
	}

	@Override
	protected Object[] getImmutableState() {

		return new Object[] { sig };
	}

	/**
	 * Adds the dependencies, clients, and dependencies of clients to this SigDependencies instance.
	 * Prior to invoking this, PersistentData for the process entities must be fully loaded.
	 * 
	 * This can be called multiple times to pull in newly calculated dependencies.
	 * 
	 * @param workspace
	 * @param processedSigsByProjectName
	 * @param addClients
	 * @param excludeClients
	 * @param javaPathData
	 */
	public void addDependsAndClients(IWorkspace workspace, //
			Map<String, Set<Signature>> processedSigsByProjectName, //
			boolean addClients, Set<Signature> excludeClients, JavaPathData javaPathData, boolean printDebug) {

		// Get dependencies and clients
		Set<Signature> processedSigs = BasicUtils.mapSetGet(processedSigsByProjectName, project.getElementName());
		if (processedSigs.contains(sig))
			return; // don't process recursive dependencies
		processedSigs.add(sig);

		if (addClients && !PersistentDataFactory.getInstance().isStandaloneBuild()) {
			// Add files containing client UCs of all changed files.
			// This should only be done if this signature has changed since the last build
			// as client UCs should only be executed if this signature has changed.
			Set<Signature> clientSigs = PersistentDataFactory.getInstance().getClientUseCases(sig);
			for (Signature clientSig : clientSigs) {
				if (!excludeClients.contains(clientSig)) {
					String clientPathStr = PersistentDataFactory.getInstance().getJavaPathStr(clientSig.getClassName());
					if (clientPathStr == null) {
						throw new SARuntimeException("Internal error: could not find path for class " + clientSig.getClassName() + //
								".  Please clean project (Project->Clean...)");
					}
					IPath clientPath = new Path(clientPathStr);

					// Add this signature to the set of signatures affected by the change
					if (printDebug)
						com.sureassert.uc.runtime.BasicUtils.debug("Added client of " + sig.toString() + " -> " + clientSig.toString());
					SigDependencies newDepends = new SigDependencies(clientSig, clientPath, workspace, //
							processedSigsByProjectName, false, excludeClients, javaPathData);
					if (clientSigDepends.contains(newDepends)) // matches on sig
						clientSigDepends.remove(newDepends);
					clientSigDepends.add(newDepends);
				}
			}
		}
		// Add files containing UseCases (e.g. declaring named instance) required by the given sig
		Set<Signature> dependUCs = PersistentDataFactory.getInstance().getDependencyUseCases(sig);
		for (Signature dependUC : dependUCs) {
			// Get path of this client or dependent signature
			String dependUCPathStr = PersistentDataFactory.getInstance().getJavaPathStr(dependUC.getClassName());
			if (dependUCPathStr == null) {
				throw new SARuntimeException("Internal error: could not find path for class " + dependUC.getClassName() + //
						".  Please clean project (Project->Clean...)");
			}
			IPath dependUCPath = new Path(dependUCPathStr);

			// Add this signature to the set of signatures affected by the change
			BasicUtils.debug("Added dependent UC of " + sig.toString() + " -> " + dependUC.toString());
			SigDependencies newDepends = new SigDependencies(dependUC, dependUCPath, workspace, //
					processedSigsByProjectName, false, null, javaPathData);
			if (dependSigDepends.contains(newDepends)) // matches on sig
				dependSigDepends.remove(newDepends);
			dependSigDepends.add(newDepends);

			// Add dependencies of this signature
			// addDependencyUCPaths(dependUC, dependUCPaths, pathSigs, clientPaths, //
			// false, dependsAsClients, project, processedSigs);
		}
	}

	@Override
	public String toString() {

		return sig.toString();
	}

	public static Set<Signature> getSigs(Set<SigDependencies> sigDependsSet) {

		Set<Signature> sigs = new HashSet<Signature>();
		for (SigDependencies sigDepends : sigDependsSet) {
			sigs.add(sigDepends.sig);
		}
		return sigs;
	}
}
