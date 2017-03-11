/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.transform;


public class TransformedRuntimeUtils {

	/**
	 * Gets the initialised value of the given field in the instance of the given test double class.
	 * NOTE: Invoked at runtime via cglib.
	 * For internal use only.
	 * 
	 * @param tdObj
	 * @param fieldName
	 * @return
	 */
	/*
	 * public static Object getFieldInit(String tdClassName, String fieldName) {
	 * 
	 * Class<?> tdClass;
	 * try {
	 * // Uses the TestDoubleFactory loaded by the calling class
	 * tdClass = ProjectClassLoaders.getCurrentClassLoaders().transformedCL.loadClass(tdClassName);
	 * } catch (ClassNotFoundException cnfe) {
	 * throw new TestDoubleException("Class " + tdClassName + " not found", cnfe);
	 * }
	 * String tdInstanceName = NamedInstanceFactory.TEST_DOUBLE_INSTANCE_NAME_PREFIX + tdClassName;
	 * Object tdInstance;
	 * try {
	 * tdInstance = NamedInstanceFactory.getInstance().getNamedInstance(tdInstanceName, //
	 * TransformedRuntimeUtils.class.getClassLoader());
	 * } catch (NamedInstanceNotFoundException e1) {
	 * try {
	 * tdInstance = tdClass.newInstance();
	 * NamedInstanceFactory.getInstance().addNamedInstance(tdInstanceName, tdInstance, null);
	 * } catch (Exception e) {
	 * throw new TestDoubleException("TestDouble class " + tdClass.getName() + //
	 * " must be given a no-args constructor.");
	 * }
	 * }
	 * try {
	 * Field field = BasicUtils.getField(tdClass, fieldName);
	 * if (field == null) {
	 * throw new TestDoubleException("Error getting initialized field value from " + //
	 * tdClassName + "." + fieldName);
	 * }
	 * field.setAccessible(true);
	 * return field.get(tdInstance);
	 * } catch (Exception e) {
	 * throw new TestDoubleException("Error getting initialized field value from " + //
	 * tdClassName + "." + fieldName);
	 * }
	 * }
	 */
}
