/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.transform;

import java.io.IOException;
import java.net.URL;

import org.aspectj.apache.bcel.util.ClassLoaderRepository;
import org.aspectj.weaver.bcel.ExtensibleURLClassLoader;

import _sauc.SAInterceptor;

import com.sureassert.uc.builder.InstrumentationSession;
import com.sureassert.uc.interceptor.SAUCClassLoader;
import com.sureassert.uc.runtime.PersistentDataFactory;

public class SAUCTransformingClassLoader extends ExtensibleURLClassLoader implements SAUCClassLoader {

	private final ClassLoader saucLibLoader;

	private final InstrumentationSession jaCoCoSession;

	public SAUCTransformingClassLoader(URL[] urls, ClassLoader parent, ClassLoader saucLibLoader, //
			InstrumentationSession jaCoCoSession) {

		super(urls, parent);
		ClassLoaderRepository.useSharedCache = false;
		if (saucLibLoader instanceof SAUCClassLoader)
			throw new IllegalArgumentException("Cannot nest SAUCClassLoaders");
		this.saucLibLoader = saucLibLoader;
		this.jaCoCoSession = jaCoCoSession;
	}

	public boolean isSaUCOnClasspath() {

		try {
			super.loadClass(SAInterceptor.class.getName());
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Attempts to load the given class, if cannot find, attempts to load from child CL.
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {

		// final byte[] bytes = definitions.get(name);
		// if (bytes != null) {
		// return defineClass(name, bytes, 0, bytes.length);
		// }

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

	/*
	 * public static byte[] asmTransformClass(byte[] in) {
	 * 
	 * ClassReader cr = new ClassReader(in);
	 * ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
	 * WriteCoverageCallClassVisitor writeCoverageCallAdapter = new
	 * WriteCoverageCallClassVisitor(cw);
	 * // ClassVisitor addFieldAdapter = new AddFieldAdapter(writeCoverageCallAdapter, //
	 * // Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT,
	 * // //
	 * // "$saucCoverageLines", "[Z");
	 * // ClassVisitor initProbeArrayVisitor = new
	 * // InitializeProbeArrayClassVisitor(addFieldAdapter);
	 * ClassVisitor cv = writeCoverageCallAdapter;
	 * cr.accept(cv, 0);
	 * return cw.toByteArray();
	 * }
	 */

	// private final Map<String, byte[]> definitions = new HashMap<String, byte[]>();

	public byte[] asmTransformClass(byte[] clazzBytes, String name) {

		if (clazzBytes == null)
			return clazzBytes;
		boolean instrumentCoverage = PersistentDataFactory.getInstance().isAnyProjectClass(name);
		clazzBytes = jaCoCoSession.instrument(name, clazzBytes, instrumentCoverage);
		return clazzBytes;
	}

	@Override
	protected byte[] getBytes(String name) throws IOException {

		byte[] bytes = super.getBytes(name);
		return asmTransformClass(bytes, name);
	}
}
