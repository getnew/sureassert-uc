/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sureassert.uc.runtime.exception.InvalidSignatureException;

public class SignatureTableFactory {

	public static final SignatureTableFactory instance = new SignatureTableFactory();

	private final Map<String, SignatureTable> stByProjectName = new HashMap<String, SignatureTable>();

	private static final Object LOCK = new Object();

	private SignatureTableFactory() {

	}

	/**
	 * Gets the SignatureTable for the project currently being processed.
	 * 
	 * @return SignatureTable
	 */
	private SignatureTable get() {

		return get(PersistentDataFactory.getInstance().getCurrentProjectName());
	}

	/**
	 * Gets the SignatureTable for the project with the given name.
	 * 
	 * @param projectName
	 * @return SignatureTable
	 */
	private SignatureTable get(String projectName) {

		synchronized (LOCK) {
			SignatureTable pd = stByProjectName.get(projectName);
			if (pd == null) {
				pd = new SignatureTable();
				stByProjectName.put(projectName, pd);
			}
			return pd;
		}
	}

	public Set<Signature> getAllSignatures(String className, ClassLoader classLoader) throws ClassNotFoundException {

		// Check current project
		Set<Signature> sigs = get().getAllSignatures(className, classLoader);
		if (sigs.isEmpty()) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sigs = entry.getValue().getAllSignatures(className, classLoader);
				if (!sigs.isEmpty())
					return sigs;
			}
		}
		return sigs;
	}

	/**
	 * Gets all signatures for the given class and member (usually method) name.
	 * 
	 * @param className
	 * @param memberName
	 * @param classLoader
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Set<Signature> getAllSignatures(String className, String memberName, //
			ClassLoader classLoader) throws ClassNotFoundException {

		Set<Signature> memberSigs = new HashSet<Signature>();
		Set<Signature> classSigs = getAllSignatures(className, classLoader);
		for (Signature sig : classSigs) {
			if (sig.getMemberName() != null && sig.getMemberName().equals(memberName)) {
				memberSigs.add(sig);
			}
		}
		return memberSigs;
	}

	public Signature getSignature(AccessibleObject ao) {

		// Check current project
		Signature sig = get().getSignature(ao);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(ao);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignature(Class<?> clazz) {

		// Check current project
		Signature sig = get().getSignature(clazz);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(clazz);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignature(Method method) {

		// Check current project
		Signature sig = get().getSignature(method);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(method);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignatureForClass(String className) {

		// Check current project
		Signature sig = get().getSignatureForClass(className);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignatureForClass(className);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignature(String className, String memberName) {

		// Check current project
		Signature sig = get().getSignature(className, memberName);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(className, memberName);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignature(String className, String memberName, String paramClassNames) {

		// Check current project
		Signature sig = get().getSignature(className, memberName, paramClassNames);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(className, memberName, paramClassNames);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignature(String className, String memberName, String[] paramClassNames) {

		// Check current project
		Signature sig = get().getSignature(className, memberName, paramClassNames);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(className, memberName, paramClassNames);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public Signature getSignature(Class<?> clazz, String memberName, Class<?>[] paramTypes) {

		// Check current project
		Signature sig = get().getSignature(clazz, memberName, paramTypes);
		if (sig == null) {
			// Check all projects
			for (Entry<String, SignatureTable> entry : stByProjectName.entrySet()) {
				sig = entry.getValue().getSignature(clazz, memberName, paramTypes);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public static Signature parseSignature(String sigDescription) throws InvalidSignatureException {

		sigDescription = sigDescription.trim();
		int dotIndex = sigDescription.indexOf(".");
		if (dotIndex == -1)
			throw new InvalidSignatureException("Method signature must contain dot");
		if (sigDescription.indexOf(".", dotIndex + 1) > -1)
			throw new InvalidSignatureException(
					"Method signature must contain only one dot.  If using fully-qualified rather than simple class name, use / as the package separator rather than . (dot).");

		String className = sigDescription.substring(0, dotIndex);
		String methodDesc = sigDescription.substring(dotIndex + 1);

		// if (!className.contains("/"))
		// className = PersistentDataFactory.getInstance().getFullClassName(className);

		String methodName;
		String[] paramClassNames;
		int bracketIdx = sigDescription.indexOf("(");

		if (bracketIdx == -1) {
			methodName = methodDesc;
			paramClassNames = Signature.CLASS_PARAMS;
		} else {
			methodName = sigDescription.substring(dotIndex + 1, bracketIdx);
			String paramClassesStr = sigDescription.substring(bracketIdx + 1, sigDescription.length() - 1).trim();
			if (paramClassesStr.length() > 0) {
				paramClassNames = paramClassesStr.split(",");
				for (int i = 0; i < paramClassNames.length; i++) {
					if (!paramClassNames[i].contains("/")) {
						String fullClassName = PersistentDataFactory.getInstance().getFullClassName(paramClassNames[i]);
						if (fullClassName != null)
							paramClassNames[i] = fullClassName;
					} else {
						paramClassNames[i] = paramClassNames[i].replace('/', '.');
					}
				}
			} else {
				paramClassNames = new String[] {};
			}
		}

		return SignatureTableFactory.instance.getSignature(className, methodName, paramClassNames);
	}

	public void clear() {

		get().clear();
	}

}
