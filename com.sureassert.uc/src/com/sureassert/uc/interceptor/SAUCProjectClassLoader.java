/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.net.URL;
import java.net.URLClassLoader;

import com.sureassert.uc.runtime.PersistentDataFactory;

public class SAUCProjectClassLoader extends URLClassLoader implements SAUCClassLoader {

	private final ClassLoader saucLibLoader;

	public SAUCProjectClassLoader(URL[] urls, ClassLoader parent, ClassLoader saucLibLoader) {

		super(urls, parent);
		if (saucLibLoader instanceof SAUCClassLoader)
			throw new IllegalArgumentException("Cannot nest SAUCClassLoaders");
		this.saucLibLoader = saucLibLoader;
	}

	/**
	 * Attempts to load the given class, if cannot find, attempts to load from child CL.
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {

		if (name.startsWith("_sauc.") || name.startsWith("org.sureassert.uc.") || //
				(name.startsWith("com.sureassert.uc.") && //
						!name.equals("com.sureassert.uc.runtime.JUnitRunListener") && //
						!name.contains(".tutorial.") && !name.contains(".aspects."))) {
			return saucLibLoader.loadClass(name);
		}

		try {
			Class<?> clazz = super.loadClass(name);
			return clazz;

		} catch (ClassNotFoundException cnfe) {

			// Attempt to load default superclass for default (generated) classes
			// Note default classes won't be on the project classpath but their superclass should be
			String defaultSuperclassName = PersistentDataFactory.getInstance().getClassNameOfDefaultSuperclass(name);
			if (defaultSuperclassName != null && defaultSuperclassName != name) {
				return loadClass(defaultSuperclassName);
			}
			try {
				return saucLibLoader.loadClass(name);
			} catch (ClassNotFoundException cnfe2) {
				throw cnfe;
			}
		}
	}
}
