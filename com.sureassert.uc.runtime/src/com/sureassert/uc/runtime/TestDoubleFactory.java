/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class returning mock instances loaded by the tool.
 * 
 * @author Nathan Dolan
 * 
 */
public class TestDoubleFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<String, TestDouble> testDoubleByDoubledClassName = new HashMap<String, TestDouble>();
	private final Map<String, TestDouble> testDoubleByTDClassName = new HashMap<String, TestDouble>();
	private final Map<String, String> doubledSourceFilePathByTDClassName = new HashMap<String, String>();
	private final Map<String, String> tdSourceFilePathByTDClassName = new HashMap<String, String>();

	/**
	 * The signature of the member that declares each test double.
	 * Key=doubled class name; value=Signature of declaring member.
	 */
	// private final Map<String, Signature> tdDeclaredSigs = new HashMap<String, Signature>();

	private final Object cacheLock = new String();

	public TestDoubleFactory() {

		// NO-OP
		System.out.println("new TestDoubleFactory");
	}

	public void addClassDouble(TestDouble classDouble, Signature declaringMemberSig) throws ClassNotFoundException {

		synchronized (cacheLock) {

			// tdDeclaredSigs.put(testDouble.getDoubledClass().getName(), declaringMemberSig);
			testDoubleByDoubledClassName.put(classDouble.getDoubledClassName(), classDouble);
			testDoubleByTDClassName.put(classDouble.getTestDoubleClassName(), classDouble);
		}
	}

	public void setDoubledClassSourcePath(String tdClass, String doubledClassSourcePath) {

		doubledSourceFilePathByTDClassName.put(tdClass, doubledClassSourcePath);
	}

	public void setTDClassSourcePath(String tdClass, String tdClassSourcePath) {

		tdSourceFilePathByTDClassName.put(tdClass, tdClassSourcePath);
	}

	public String getDoubledClassSourcePath(String tdClass) {

		return doubledSourceFilePathByTDClassName.get(tdClass);
	}

	public String getTDClassSourcePath(String tdClass) {

		return tdSourceFilePathByTDClassName.get(tdClass);
	}

	public TestDouble getClassDoubleForTDClassName(String tdClassName) {

		return testDoubleByTDClassName.get(tdClassName);
	}

	public TestDouble getClassDoubleForDoubledClassName(String doubledClassName) {

		return testDoubleByDoubledClassName.get(doubledClassName);
	}

	/*
	 * public List<TestDouble> getClassDoubles(String doubledClassName, ClassLoader projectCL) {
	 * 
	 * synchronized (cacheLock) {
	 * List<TestDouble> tdList = null;
	 * try {
	 * Class<?> doubledClass = projectCL.loadClass(doubledClassName);
	 * doubledClass = BasicUtils.toNonPrimitiveType(doubledClass);
	 * doubledClassName = doubledClass.getName();
	 * tdList = testDoublesByDoubledClassName.get(doubledClassName);
	 * if (tdList == null) {
	 * // Check for superclasses of the given class
	 * Class<?> thisClass;
	 * for (Entry<String, List<TestDouble>> entry : testDoublesByDoubledClassName.entrySet()) {
	 * try {
	 * thisClass = projectCL.loadClass(entry.getKey());
	 * if (thisClass.isAssignableFrom(doubledClass)) {
	 * // There is an entry in the map for a superclass of the given class:
	 * // add the given class to the map pointing at the same test-double
	 * // list.
	 * tdList = entry.getValue();
	 * testDoublesByDoubledClassName.put(doubledClassName, tdList);
	 * break;
	 * }
	 * } catch (ClassNotFoundException e) {
	 * // Ignore; in a different project
	 * }
	 * }
	 * }
	 * } catch (ClassNotFoundException e) {
	 * System.err.println("Could not load " + doubledClassName + ": " + BasicUtils.toDisplayStr(e));
	 * }
	 * if (tdList == null)
	 * tdList = new ArrayList<TestDouble>();
	 * return tdList;
	 * }
	 * }
	 */

	public void clear() {

		testDoubleByDoubledClassName.clear();
		testDoubleByTDClassName.clear();
		tdSourceFilePathByTDClassName.clear();
		doubledSourceFilePathByTDClassName.clear();
	}
}
