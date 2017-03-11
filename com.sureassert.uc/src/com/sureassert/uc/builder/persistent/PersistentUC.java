/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder.persistent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.ValueObject;

public class PersistentUC extends ValueObject {

	private static final long serialVersionUID = 1L;

	/** The full path to the file in which the UseCase is declared */
	private final String filePath;

	/** The signature of the UseCase within the file */
	private final Signature ucSignature;

	/** All dependencies of the UseCase included nested ones **/
	private final Set<DependentMember> ucDependencies = new HashSet<DependentMember>();

	/** All clients of the UseCase included nested ones **/
	private final Set<DependentMember> ucClients = new HashSet<DependentMember>();

	public PersistentUC(String filePath, Signature ucSignature) {

		this.filePath = filePath;
		this.ucSignature = ucSignature;
	}

	@Override
	protected Object[] getImmutableState() {

		return new Object[] { filePath, ucSignature };
	}

	public String getFilePath() {

		return filePath;
	}

	public Signature getUseCaseSignature() {

		return ucSignature;
	}

	public Set<DependentMember> getUseCaseDependencies() {

		return ucDependencies;
	}

	public Set<DependentMember> getUseCaseClients() {

		return ucClients;
	}

	/**
	 * Adds a dependency of this UseCase
	 * 
	 * @param filePath
	 * @param signature
	 */
	public void addDependency(String filePath, Signature signature) {

		ucDependencies.add(new DependentMember(filePath, signature));
	}

	/**
	 * Adds a client of this UseCase
	 * 
	 * @param filePath
	 * @param signature
	 */
	public void addClient(String filePath, Signature signature) {

		ucClients.add(new DependentMember(filePath, signature));
	}

	/**
	 * Gets the set of files on which this use-case is dependent, mapped against the
	 * signatures in the file on which the use-case is dependent.
	 * 
	 * @return
	 */
	public Map<String, List<Signature>> getDependentFiles() {

		Map<String, List<Signature>> files = new HashMap<String, List<Signature>>();
		for (DependentMember member : ucDependencies) {
			List<Signature> sigs = files.get(member.getFilePath());
			if (sigs == null)
				sigs = new ArrayList<Signature>();
			sigs.add(member.getSignature());
			files.put(member.getFilePath(), sigs);
		}
		return files;
	}

	public static class DependentMember {

		private final String filePath;

		private final Signature signature;

		public DependentMember(String filePath, Signature signature) {

			this.filePath = filePath;
			this.signature = signature;
		}

		public String getFilePath() {

			return filePath;
		}

		public Signature getSignature() {

			return signature;
		}
	}

}
