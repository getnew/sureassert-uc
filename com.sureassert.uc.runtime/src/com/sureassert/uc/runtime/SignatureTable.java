/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SignatureTable {

	private final Map<Signature, Signature> signatureTable = new HashMap<Signature, Signature>();

	// private final Map<String, Set<Signature>> signaturesByClassName = new HashMap<String,
	// Set<Signature>>();

	public synchronized Signature getSignature(Signature sig) {

		Signature tableSig = signatureTable.get(sig);
		if (tableSig == null) {
			signatureTable.put(sig, sig);
			tableSig = sig;
		}
		/*
		 * Set<Signature> classSigs = signaturesByClassName.get(tableSig.getClassName());
		 * if (classSigs == null) {
		 * classSigs = new HashSet<Signature>();
		 * signaturesByClassName.put(tableSig.getClassName(), classSigs);
		 * }
		 * classSigs.add(sig);
		 */

		return tableSig;
	}

	/* package */Set<Signature> getAllSignatures(String className, ClassLoader classLoader) throws ClassNotFoundException {

		Class<?> clazz = classLoader.loadClass(className);
		Set<Signature> sigs = new HashSet<Signature>();
		sigs.add(getSignatureForClass(className));
		for (Method method : clazz.getDeclaredMethods()) {
			// if (!method.getName().equals("ajc$preClinit"))
			sigs.add(getSignature(method));
		}
		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			sigs.add(getSignature(constructor));
		}
		for (Field field : clazz.getDeclaredFields()) {
			// if (!field.getName().startsWith("ajc$tjp"))
			sigs.add(getSignature(field));
		}
		return sigs;
	}

	/* package */Signature getSignature(AccessibleObject ao) {

		return getSignature(new Signature(ao));
	}

	/* package */Signature getSignature(Class<?> clazz) {

		return getSignature(new Signature(clazz));
	}

	/* package */Signature getSignature(Method method) {

		return getSignature(new Signature(method));
	}

	/* package */Signature getSignatureForClass(String className) {

		return getSignature(new Signature(className));
	}

	/* package */Signature getSignature(String className, String memberName) {

		return getSignature(new Signature(className, memberName));
	}

	/* package */Signature getSignature(String className, String memberName, String paramClassNames) {

		return getSignature(new Signature(className, memberName, paramClassNames));
	}

	/* package */Signature getSignature(String className, String memberName, String[] paramClassNames) {

		return getSignature(new Signature(className, memberName, paramClassNames));
	}

	/* package */Signature getSignature(Class<?> clazz, String memberName, Class<?>[] paramTypes) {

		String className = clazz.getName();
		if (memberName == null)
			memberName = Signature.CONSTRUCTOR_METHOD_NAME;
		String[] paramClassNames = new String[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			paramClassNames[i] = paramTypes[i].getName();
		}
		return getSignature(className, memberName, paramClassNames);
	}

	/* package */void clear() {

		signatureTable.clear();
		// signaturesByClassName.clear();
	}

}
