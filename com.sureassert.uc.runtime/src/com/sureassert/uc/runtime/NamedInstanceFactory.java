/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.sureassert.uc.runtime.typeconverter.NamedInstanceTC;

/**
 * Factory class returning named instances loaded by the tool.
 * 
 * @author Nathan Dolan
 * 
 */
public class NamedInstanceFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String NULL_INSTANCE_NAME = "null";

	public static final String RETVAL_INSTANCE_NAME = "retval";

	public static final String SPECIAL_CHAR = "\uE306";

	public static final String CHAINED_INSTANCE_NAME = SPECIAL_CHAR + "chained" + SPECIAL_CHAR;

	public static final String VERIFY_ARG_INSTANCE_NAME = "arg";

	public static final String VERIFY_OBJ_INSTANCE_NAME = "obj";

	public static final String THIS_INSTANCE_NAME = "this";

	public static final String DEFAULT_INSTANCE_NAME = "*";

	public static final String DEFAULT_CLASS_INTERNAL_SUPERNAME_PREFIX = SPECIAL_CHAR + "*";

	public static final String TEST_DOUBLE_INSTANCE_NAME_PREFIX = SPECIAL_CHAR + "TD" + SPECIAL_CHAR;

	public static final String DEFAULT_CONSTRUCTOR_NAME_PREFIX = SPECIAL_CHAR + "init" + SPECIAL_CHAR;

	public static final String ARG_PREFIX = "$arg";

	private final Map<String, Object> namedInstances = new HashMap<String, Object>();

	private final Map<Object, String> instanceNames = new HashMap<Object, String>();

	private static class SingletonHolder {

		private static NamedInstanceFactory instance = new NamedInstanceFactory();
	}

	public static final String BUILT_IN_METHODS_NAME = BuiltInMethods.class.getName().replace(".", "/");

	private NamedInstanceFactory() {

		// NO-OP
	}

	public static NamedInstanceFactory getInstance() {

		return SingletonHolder.instance;
	}

	public static void setInstance(NamedInstanceFactory instance) {

		SingletonHolder.instance = instance;
	}

	public void addNamedInstance(String name, Object namedInstance, Signature declaringMemberSig) throws NamedInstanceException {

		if (NamedInstanceTC.isValidInstanceName(name)) {
			if (instanceNames.containsKey(name)) {
				throw new NamedInstanceException("An instance with name \"" + name + //
						"\" is already declared in " + PersistentDataFactory.getInstance().getDeclaringSignature(name).toString());
			}
			instanceNames.put(namedInstance, name);
		}
		validateName(name);
		namedInstances.put(name, namedInstance);
		if (declaringMemberSig != null)
			PersistentDataFactory.getInstance().registerDeclaringSignature(declaringMemberSig, name);
	}

	private void validateName(String name) throws NamedInstanceException {

		if (name != null && !name.contains(SPECIAL_CHAR)) {
			if (name.matches(".*\\p{Space}.*") || name.contains(".") || name.contains(",") || name.contains("(") || name.contains(")") || //
					name.contains(":") || name.contains("[") || name.contains("]")) {
				throw new NamedInstanceException("Names cannot contain a space or any of the following characters: " + //
						". , ( ) : [ ]");
			}
		}
	}

	public void removeNamedInstance(String name) {

		namedInstances.remove(name);
		instanceNames.remove(name);
	}

	/**
	 * Gets the instance with the given name.
	 * 
	 * @param name The instance name
	 * @return The instance, or null.
	 * @throws NamedInstanceNotFoundException If an instance with the given name couldn't be found.
	 */
	public Object getNamedInstance(String name, ClassLoader classLoader) throws NamedInstanceNotFoundException {

		// try named instance
		if (namedInstances.containsKey(name)) {
			registerDependency(name);
			return namedInstances.get(name);
		}
		// try class-prefixed named-instance
		String currentUCSimpleClassName = PersistentDataFactory.getInstance().getCurrentUseCaseSimpleClassName();
		String prefixedName = currentUCSimpleClassName == null ? null : currentUCSimpleClassName + "/" + name;
		if (currentUCSimpleClassName != null && namedInstances.containsKey(prefixedName)) {
			registerDependency(prefixedName);
			return namedInstances.get(prefixedName);
		} else {
			// try named class
			return getNamedClass(name, classLoader);
		}
	}

	private void registerDependency(String name) {

		if (NamedInstanceTC.isValidInstanceName(name)) {
			Signature declaringSig = PersistentDataFactory.getInstance().getDeclaringSignature(name);
			if (declaringSig != null) {
				PersistentDataFactory.getInstance().registerDependency(declaringSig, //
						PersistentDataFactory.getInstance().getCurrentUseCaseSignature(), true);
				PersistentDataFactory.getInstance().registerDependency(declaringSig, //
						PersistentDataFactory.getInstance().getCurrentExecMethodSignature(), true);
			}
		}
	}

	public Class<?> getNamedClass(String name, ClassLoader classLoader) throws NamedInstanceNotFoundException {

		Class<?> clazz;
		if (PersistentDataFactory.getInstance().getFullClassName(name) != null) {
			try {
				// load named class
				clazz = classLoader.loadClass(PersistentDataFactory.getInstance().getFullClassName(name));
				registerDependency(name);
				return clazz;
			} catch (ClassNotFoundException cnfe) {
				throw new NamedInstanceNotFoundException("Could not load class \"" + //
						PersistentDataFactory.getInstance().getFullClassName(name) + "\"");
			}
		} else {
			try {
				// attempt to load class with given name
				clazz = classLoader.loadClass(BasicUtils.getNonPrimitiveClassName(name.replace('/', '.')));
				registerDependency(name);
				return clazz;
			} catch (Throwable e) {
				try {
					// attempt to load java.lang class
					clazz = classLoader.loadClass("java.lang." + name);
					registerDependency(name);
					return clazz;
				} catch (Throwable e2) {
					// no instance registered against given name
					throw new NamedInstanceNotFoundException("No instance registered with name \"" + name + "\"");
				}
			}
		}
	}

	public String getInstanceName(Object instance) {

		return instanceNames.get(instance);
	}

	public boolean namedInstanceExists(String name) {

		return namedInstances.containsKey(name);
	}

	public void clear() {

		// niDeclaredSigs.clear();
		namedInstances.clear();
		instanceNames.clear();
	}

	// public void addNamedInstance(String name, Class<?> instanceClass, Object...
	// constructorParams) //
	// throws InstantiationException, IllegalAccessException, IllegalArgumentException,
	// InvocationTargetException, NamedInstanceException {
	//
	// addNamedInstance(name, getConstructor(instanceClass, constructorParams).//
	// newInstance(constructorParams));
	// }
	//
	// private Constructor<?> getConstructor(Class<?> instanceClass, Object... constructorParams) //
	// throws NamedInstanceException {
	//
	// // TODO: This doesn't determine precedence for overloaded constructors where all params
	// // reside in the same type hierarchy.
	// for (Constructor<?> constructor : instanceClass.getConstructors()) {
	// Class<?>[] paramTypes = constructor.getParameterTypes();
	// if (paramTypes.length == constructorParams.length) {
	// int i = 0;
	// for (; i < constructorParams.length; i++) {
	// if (!paramTypes[i].isAssignableFrom(constructorParams[0].getClass()))
	// break;
	// }
	// if (i == constructorParams.length)
	// return constructor;
	// }
	// }
	//
	// throw new NamedInstanceException("Constructor not found in class " + instanceClass.getName()
	// + //
	// " for parameters " + constructorParams.toString());
	// }

}
