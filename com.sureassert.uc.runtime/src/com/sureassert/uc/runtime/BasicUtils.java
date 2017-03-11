/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import _sauc.StubThrowingException;

import com.sureassert.uc.runtime.exception.BaseSAException;
import com.sureassert.uc.runtime.exception.ExtendedMessageException;
import com.sureassert.uc.runtime.exception.MethodArgumentException;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.exception.SAUCBuildInterruptedError;
import com.sureassert.uc.runtime.exception.UseCaseException;

/**
 * Class for basic utilities. No dependencies on non-JDK classes permitted.
 * 
 * @author Nathan Dolan
 */
public class BasicUtils {

	public static final String JAVA_COMPILE_ERROR = "Java compile error: ";
	public static final boolean DEBUG = true;
	private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(//
			DateFormat.SHORT, DateFormat.MEDIUM);

	public static String getSimpleClassName(String className) {

		if (className == null)
			return null;
		className = className.replace("$", ".");
		int lastDotIndex = className.lastIndexOf(".");
		return lastDotIndex > -1 ? className.substring(lastDotIndex + 1) : className;
	}

	public static void debug(String message) {

		if (DEBUG) {
			// System.out.println(dateFormat.format(new Date()) + ": [" +
			// Thread.currentThread().getName() + "] - " + message);
			System.out.println(dateFormat.format(new Date()) + ": " + message);
		}
	}

	/**
	 * Gets a String to describe the UseCase with the given properties.
	 * 
	 * @param definedOnSig
	 * @param ucName
	 * @param ucDescription
	 * @return String
	 */
	public static String getUseCaseDisplayName(Signature definedOnSig, String ucName, String ucDescription) {

		String ucEx = PersistentDataFactory.getInstance().wasLastExecUseCase() ? "UseCase" : "Exemplar";
		if (ucName != null && !ucName.startsWith(NamedInstanceFactory.SPECIAL_CHAR))
			return ucEx + " \"" + ucName + "\"";
		else if (ucDescription != null)
			return ucEx + " \"" + ucDescription + "\"";
		else if (definedOnSig != null)
			return ucEx + " defined on " + definedOnSig.toSimpleString();
		else
			return ucEx;
	}

	/**
	 * Gets a String to describe the currently executing UseCase.
	 * 
	 * @return String
	 */
	public static String getCurrentUseCaseDisplayName() {

		return getCurrentUseCaseDisplayName(true);
	}

	/**
	 * Gets a String to describe the currently executing UseCase.
	 * 
	 * @param useSigName Use false to prevent using the UseCase declaring signature name.
	 * @return String
	 */
	public static String getCurrentUseCaseDisplayName(boolean useSigName) {

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		return getUseCaseDisplayName(useSigName ? pdf.getCurrentUseCaseSignature() : null, //
				pdf.getCurrentUseCaseName(), pdf.getCurrentUseCaseDescription());
	}

	/**
	 * Gets the name of the given class as it would be declared in Java source code.
	 * 
	 * @param clazz
	 * @return
	 */
	public static String getClassNameAsSrc(Class<?> clazz) {

		return getClassNameAsSrc(clazz, 0);
	}

	private static String getClassNameAsSrc(Class<?> clazz, int numArrayDimensions) {

		if (clazz.isArray()) {
			return getClassNameAsSrc(clazz.getComponentType(), numArrayDimensions + 1);
		} else {
			if (numArrayDimensions == 0) {
				return clazz.getName().replace('$', '.');
			} else {
				StringBuilder classJavaSrcName = new StringBuilder(clazz.getName().replace('$', '.'));
				for (int n = 0; n < numArrayDimensions; n++) {
					classJavaSrcName.append("[]");
				}
				return classJavaSrcName.toString();
			}
		}
	}

	/**
	 * Gets the class with the given binary name using the given classLoader.
	 * If a user-defined class isn't found with the given name, checks for primitive types.
	 * 
	 * @param className
	 * @param classLoader
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getClass(String className, ClassLoader classLoader) throws ClassNotFoundException {

		try {
			return Class.forName(className, false, classLoader);
		} catch (ClassNotFoundException cnfe) {
			if (className.equals("Z") || className.equals("boolean"))
				return Boolean.TYPE;
			else if (className.equals("B") || className.equals("byte"))
				return Byte.TYPE;
			else if (className.equals("C") || className.equals("char"))
				return Character.TYPE;
			else if (className.equals("D") || className.equals("double"))
				return Double.TYPE;
			else if (className.equals("F") || className.equals("float"))
				return Float.TYPE;
			else if (className.equals("I") || className.equals("int"))
				return Integer.TYPE;
			else if (className.equals("J") || className.equals("long"))
				return Long.TYPE;
			else if (className.equals("S") || className.equals("short"))
				return Short.TYPE;
			else
				throw cnfe;
		}
	}

	public static boolean typeEquals(Class<?> c1, Class<?> c2, boolean checkParamSupertypes) {

		c1 = toNonPrimitiveType(c1);
		c2 = toNonPrimitiveType(c2);
		return c1 == null || c2 == null || c1.equals(c2) || (checkParamSupertypes && c1.isAssignableFrom(c2));
	}

	public static Class<?> toNonPrimitiveType(Class<?> c) {

		if (c == null || !c.isPrimitive())
			return c;

		// In descending order of likelihood...
		if (c.equals(Void.TYPE))
			return c;
		else if (c.equals(Boolean.TYPE))
			return Boolean.class;
		else if (c.equals(Integer.TYPE))
			return Integer.class;
		else if (c.equals(Double.TYPE))
			return Double.class;
		else if (c.equals(Long.TYPE))
			return Long.class;
		else if (c.equals(Character.TYPE))
			return Character.class;
		else if (c.equals(Byte.TYPE))
			return Byte.class;
		else if (c.equals(Short.TYPE))
			return Short.class;
		else if (c.equals(Float.TYPE))
			return Float.class;
		else {
			assert false : "It seems a new-fangled primitive type has been invented called " + c.getName();
			return c;
		}
	}

	/**
	 * Returns whether any of the strings in the given set start with the given prefix
	 * 
	 * @param <E>
	 * @param set
	 * @param entry
	 * @return
	 */
	public static boolean setContainsStartsWith(Set<String> set, String prefix) {

		for (String e : set) {
			if (e.startsWith(prefix))
				return true;
		}
		return true;
	}

	/**
	 * 
	 * Adds the given value to the ArrayList stored against the given key in the given map.
	 * If the map does not contain the given key, a new HashMap is created and the value
	 * is added to that.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map value type.
	 * @param map The map.
	 * @param key The key.
	 * @param value The value to add to the ArrayList mapped against the given key in the given map.
	 * @return The ArrayList mapped against the given key in the given map, which will contain the
	 *         given value.
	 */
	public static <K, V> List<V> mapListAdd(Map<K, List<V>> map, K key, V value) {

		List<V> list = map.get(key);
		if (list == null) {
			list = new ArrayList<V>();
			map.put(key, list);
		}
		list.add(value);
		return list;
	}

	/**
	 * 
	 * Adds the given value to the HashSet stored against the given key in the given map.
	 * If the map does not contain the given key, a new HashMap is created and the value
	 * is added to that.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map value type.
	 * @param map The map.
	 * @param key The key.
	 * @param value The value to add to the HashSet mapped against the given key in the given map.
	 * @return The HashSet mapped against the given key in the given map, which will contain the
	 *         given value.
	 */
	public static <K, V> Set<V> mapSetAdd(Map<K, Set<V>> map, K key, V value) {

		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>();
			map.put(key, set);
		}
		set.add(value);
		return set;
	}

	public static <K, V> Set<V> mapSetAddAll(Map<K, Set<V>> map, K key, Collection<V> valueCol) {

		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>();
			map.put(key, set);
		}
		set.addAll(valueCol);
		return set;
	}

	/**
	 * 
	 * Gets the HashSet stored against the given key in the given map.
	 * If the map does not contain the given key, a new HashMap is created.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map value type.
	 * @param map The map.
	 * @param key The key.
	 * @return The HashSet mapped against the given key in the given map.
	 */
	public static <K, V> Set<V> mapSetGet(Map<K, Set<V>> map, K key) {

		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>();
			map.put(key, set);
		}
		return set;
	}

	/**
	 * Adds the given value to the LinkedHashSet stored against the given key in the given map.
	 * If the map does not contain the given key, a new HashMap is created and the value
	 * is added to that.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map set value type.
	 * @param map The map.
	 * @param key The key.
	 * @param value The value to add to the LinkedHashSet mapped against the given key in the given
	 *            map.
	 * @return The LinkedHashSet mapped against the given key in the given map, which will contain
	 *         the given value.
	 */
	public static <K, V> Set<V> mapLinkedSetAdd(Map<K, Set<V>> map, K key, V value) {

		Set<V> set = map.get(key);
		if (set == null) {
			set = new LinkedHashSet<V>();
			map.put(key, set);
		}
		set.add(value);
		return set;
	}

	/**
	 * Remove all instances of value from the collections in the given map.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map collection value type.
	 * @param map The map.
	 * @param value The value to remove from the collection.
	 * @param removeObj
	 */
	public static <K, V> void mapCollectionRemove(Map<K, ? extends Collection<V>> map, V value) {

		for (Entry<K, ? extends Collection<V>> entry : map.entrySet()) {
			while (entry.getValue().remove(value))
				;
		}
	}

	/**
	 * Remove all signatures from the collections in the given map of collections of signatures
	 * where the signature class name matches the one given.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map collection value type.
	 * @param map The map.
	 * @param className The class
	 */
	public static <K> void mapToSigsRemoveByClass(Map<K, ? extends Collection<Signature>> map, String className) {

		for (Entry<K, ? extends Collection<Signature>> entry : map.entrySet()) {
			for (Iterator<Signature> i = entry.getValue().iterator(); i.hasNext();) {
				if (i.next().getClassName().equals(className)) {
					i.remove();
				}
			}
		}
	}

	/**
	 * Remove all signatures from the collections in the given map of collections of signatures
	 * where the signature class name matches the one given.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map collection value type.
	 * @param map The map.
	 * @param className The class
	 */
	public static <K, V> void sigMapRemoveByClass(Map<Signature, V> map, String className) {

		for (Iterator<Signature> i = map.keySet().iterator(); i.hasNext();) {
			if (i.next().getClassName().equals(className)) {
				i.remove();
			}
		}
	}

	/**
	 * Removes all the given keys from the given map.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map value type.
	 * @param map The map.
	 * @param removeKeys The keys to remove.
	 */
	public static <K, V> void mapRemoveAll(Map<K, V> map, Collection<K> removeKeys) {

		for (K removeKey : removeKeys) {
			map.remove(removeKey);
		}
	}

	/**
	 * Removes all keys from the map that map to one of the given collection of values.
	 * 
	 * @param <K> The map key type.
	 * @param <V> The map collection value type.
	 * @param map The map.
	 * @param value The values to match when removing keys.
	 */
	public static <K, V> void mapValueRemoveAll(Map<K, V> map, Collection<V> removeValues) {

		for (Iterator<Entry<K, V>> i = map.entrySet().iterator(); i.hasNext();) {
			if (removeValues.contains(i.next().getValue()))
				i.remove();
		}
	}

	public static <K, V> Set<V> mapGetSetNoNull(Map<K, Set<V>> map, K key) {

		Set<V> set = map.get(key);
		return set == null ? new HashSet<V>() : set;
	}

	/**
	 * Gets the subset of the given collection of Signature whose sig class matches the given class
	 * name
	 * 
	 * @param className
	 * @param sigs
	 * @return
	 */
	public static Set<Signature> getClassSigs(String className, Collection<Signature> sigs) {

		Set<Signature> classSigs = new HashSet<Signature>();
		for (Signature sig : sigs) {
			if (sig.getClassName().equals(className))
				classSigs.add(sig);
		}
		return classSigs;
	}

	/**
	 * Gets the subset of the given map of Key->Signature whose sig class matches the given class
	 * name
	 * 
	 * @param className
	 * @param sigs
	 * @return
	 */
	public static <K> Set<K> getClassKeys(K className, Map<K, Signature> sigs) {

		Set<K> classEntries = new HashSet<K>();
		for (Entry<K, Signature> entry : sigs.entrySet()) {
			if (entry.getValue().getClassName().equals(className))
				classEntries.add(entry.getKey());
		}
		return classEntries;
	}

	/**
	 * Returns true if the given array contains the an element equal to the given value.
	 * 
	 * @param array
	 * @param val
	 * @return boolean
	 */
	public static boolean arrayContains(Object array, Object... vals) {

		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			for (Object val : vals) {
				if (val.equals(Array.get(array, i)))
					return true;
			}
		}
		return false;
	}

	/**
	 * Returns a new list containing the given items.
	 * 
	 * @param <V>
	 * @param items
	 * @return
	 */
	public static <V> List<V> newList(V... items) {

		List<V> list = new ArrayList<V>();
		for (V item : items) {
			list.add(item);
		}
		return list;
	}

	public static Class<?> toPrimitiveType(Class<?> c) {

		if (c == null || c.isPrimitive())
			return c;

		// In descending order of likelihood...
		if (c.equals(Void.class))
			return c;
		else if (c.equals(Boolean.class))
			return Boolean.TYPE;
		else if (c.equals(Integer.class))
			return Integer.TYPE;
		else if (c.equals(Double.class))
			return Double.TYPE;
		else if (c.equals(Long.class))
			return Long.TYPE;
		else if (c.equals(Character.class))
			return Character.TYPE;
		else if (c.equals(Byte.class))
			return Byte.TYPE;
		else if (c.equals(Short.class))
			return Short.TYPE;
		else if (c.equals(Float.class))
			return Float.TYPE;
		else {
			assert false : "It seems a new-fangled primitive type has been invented called " + c.getName();
			return c;
		}
	}

	public static boolean equalsAsPrimitive(Object o1, Object o2) {

		if (o1 == null && o2 == null)
			return true;
		if (o1 == null || o2 == null)
			return false;
		o1 = toPrimitive(o1); // as o1 may be primitive and o2 not so
		o2 = toPrimitive(o2); // as o1 may be primitive and o2 not so
		if (o1.getClass() != o2.getClass())
			return false;
		Class<?> c = o1.getClass();

		if (c.equals(Void.class))
			return false;
		else if (c.equals(Boolean.class))
			return ((Boolean) o1).booleanValue() == ((Boolean) o2).booleanValue();
		else if (c.equals(Integer.class))
			return ((Integer) o1).intValue() == ((Integer) o2).intValue();
		else if (c.equals(Double.class))
			return ((Double) o1).doubleValue() == ((Double) o2).doubleValue();
		else if (c.equals(Long.class))
			return ((Long) o1).longValue() == ((Long) o2).longValue();
		else if (c.equals(Character.class))
			return ((Character) o1).charValue() == ((Character) o2).charValue();
		else if (c.equals(Byte.class))
			return ((Byte) o1).byteValue() == ((Byte) o2).byteValue();
		else if (c.equals(Short.class))
			return ((Short) o1).shortValue() == ((Short) o2).shortValue();
		else if (c.equals(Float.class))
			return ((Float) o1).floatValue() == ((Float) o2).floatValue();
		else
			return false;
	}

	public static Object toPrimitive(Object o) {

		if (o == null)
			return null;

		Class<?> c = o.getClass();
		if (c.isPrimitive())
			return o;

		// In descending order of likelihood...
		if (c.equals(Void.class))
			return o;
		else if (c.equals(Boolean.class))
			return (boolean) (Boolean) o;
		else if (c.equals(Integer.class))
			return (int) (Integer) o;
		else if (c.equals(Double.class))
			return (double) (Double) o;
		else if (c.equals(Long.class))
			return (long) (Long) o;
		else if (c.equals(Character.class))
			return (char) (Character) o;
		else if (c.equals(Byte.class))
			return (byte) (Byte) o;
		else if (c.equals(Short.class))
			return (short) (Short) o;
		else if (c.equals(Float.class))
			return (float) (Float) o;

		return o;
	}

	public static String getUUID() {

		return UUID.randomUUID().toString();
	}

	public static void serialize(Serializable obj, File file) {

		ObjectOutputStream fos = null;
		try {
			file.delete();
			file.createNewFile();
			fos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			fos.writeObject(obj);
		} catch (Throwable e) {
			throw new SARuntimeException(e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Throwable e2) {
					throw new SARuntimeException(e2);
				}
			}
		}
	}

	public static void serialize(Serializable obj, OutputStream stream) {

		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(stream);
			oos.writeObject(obj);
		} catch (Throwable e) {
			throw new SARuntimeException(e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (Throwable e2) {
					throw new SARuntimeException(e2);
				}
			}
		}
	}

	public static Object deserialize(File file) throws IOException, ClassNotFoundException {

		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			return in.readObject();
		} finally {
			if (in != null)
				in.close();
		}
	}

	public static Object deserialize(InputStream stream) throws IOException, ClassNotFoundException {

		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(stream);
			return in.readObject();
		} finally {
			if (in != null)
				in.close();
		}
	}

	public static void mkdirs(File file) {

		if (file.isDirectory() && file.exists())
			return;
		if (!file.mkdirs()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			System.gc();
			if (!file.mkdirs()) {
				throw new SARuntimeException("Internal error: could not create directory " + //
						file.getAbsolutePath() + ".  Please ensure the parent directory is not locked by Explorer or any other process (e.g. text editors).");
			}
		}
	}

	public static void deleteDirectory(File file) throws IOException {

		deleteDirectory(file, 0);
	}

	private static void deleteDirectory(File file, int attempt) throws IOException {

		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			if (attempt < 3) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
				System.gc();
				deleteDirectory(file, attempt + 1);
			} else {
				throw e;
			}
		}
	}

	public static Object newInstance(Class<?> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, UseCaseException {

		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			if (constructor.getParameterTypes().length == 0) {
				constructor.setAccessible(true);
				return constructor.newInstance();
			}
		}
		throw new UseCaseException("Class " + clazz.getName() + " does not have a default constructor.");

		// Enhancer e = new Enhancer();
		// e.setClassLoader(classLoader);
		// e.setSuperclass(clazz);
		// e.setCallback(EAInterceptor.instance);
		// try {
		// return e.create();
		// } catch (NoClassDefFoundError ncdfe) {
		// // have another go...
		// return e.create();
		// }
	}

	public static <T> Set<T> newSet(T... entries) {

		HashSet<T> set = new HashSet<T>();
		for (T entry : entries) {
			set.add(entry);
		}
		return set;
	}

	/**
	 * Returns true if the given directory or any descendent subdirectories contain a file with the
	 * given extension.
	 * 
	 * @param directory
	 * @param extension
	 * @return
	 */
	public static boolean exists(File directory, final String extension, Set<File> processed) {

		if (processed.contains(directory))
			return false;
		processed.add(directory);
		final HardReference<Boolean> foundMatch = new HardReference<Boolean>(false);
		File[] subdirs = directory.listFiles(new FileFilter() {

			public boolean accept(File file) {

				if (foundMatch.value)
					return false;
				if (file.getName().endsWith(extension))
					foundMatch.value = true;
				return file.isDirectory();
			}
		});
		if (foundMatch.value)
			return true;
		if (subdirs != null) {
			for (File subdir : subdirs) {
				if (exists(subdir, extension, processed))
					return true;
			}
		}
		return false;
	}

	public static String toDisplayStr(AssertionError e) {

		if (e.getMessage() == null || e.getMessage().trim().equals("")) {
			return "Assertion Error";
		} else {
			return "Assertion Error: " + e.getMessage();
		}
	}

	/**
	 * Get the field in the given class.
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class<?> clazz, String fieldName) {

		return BasicUtils.getField(clazz, fieldName, true);
	}

	public static Field getField(Class<?> clazz, String fieldName, boolean checkParamSupertypes) {

		// Check declared fields in this class
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			// Check superclass and interfaces
			Set<Class<?>> superClasses = BasicUtils.getSuperClassAndInterfaces(clazz);
			for (Class<?> superClass : superClasses) {
				Field field = getField(superClass, fieldName, checkParamSupertypes);
				if (field != null)
					return field;
			}
		}
		return null;
	}

	/**
	 * Gets set containing the superclass (if there is one) and any super-interfaces of the given
	 * class. Note this does not include super-super classes or interfaces.
	 */
	public static Set<Class<?>> getSuperClassAndInterfaces(Class<?> clazz) {

		return getSuperClassAndInterfaces(clazz, false, false);
	}

	private static final Object LOCK = new Object();

	/**
	 * Gets set containing the superclass (if there is one) and any super-interfaces of the given
	 * class.
	 */
	public static Set<Class<?>> getSuperClassAndInterfaces(Class<?> clazz, boolean isRecursive, boolean includeJavaLangObject) {

		Set<Class<?>> superClasses = new HashSet<Class<?>>();
		{
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null && (includeJavaLangObject || !superClass.getName().equals("java.lang.Object")))
				superClasses.add(superClass);
			superClasses.addAll(Arrays.asList(clazz.getInterfaces()));
		}
		if (isRecursive) {
			try {
				Set<Class<?>> recursiveSuperClasses = new HashSet<Class<?>>(superClasses);
				for (Class<?> superClass : superClasses) {
					recursiveSuperClasses.addAll(getSuperClassAndInterfaces(superClass, isRecursive, includeJavaLangObject));
				}
				superClasses = recursiveSuperClasses;
			} catch (ConcurrentModificationException e) {
				e.printStackTrace();
			}
		}
		return superClasses;
	}

	/**
	 * Returns true if obj1 and obj2 are both null, or if obj1.equals(obj2). Else returns false.
	 * 
	 * @param obj1
	 * @param obj2
	 * @return boolean
	 */
	public static boolean equals(Object obj1, Object obj2) {

		if (obj1 == obj2)
			return true;
		if (obj1 == null || obj2 == null)
			return false;
		return obj1.equals(obj2);
	}

	public static byte[] toByteArray(Object obj) throws IOException {

		byte[] bytes = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(obj);
		oos.flush();
		oos.close();
		bos.close();
		bytes = bos.toByteArray();
		return bytes;
	}

	private static void appendIfNotAspectTrace(StringBuilder str, String appendStr) {

		if (!appendStr.contains("com.sureassert.uc.aspects."))
			str.append(appendStr);
	}

	public static String toDisplayStr(Throwable e) {

		StringBuilder msg = new StringBuilder();
		boolean excludeSAUC = false;
		String appendMsg = null;
		String prefixMsg = null;
		if (e instanceof SARuntimeException && e.getCause() != null) {
			appendMsg = ((SARuntimeException) e).getAppendMessage();
			prefixMsg = ((SARuntimeException) e).getPrefixMessage();
			// Unwrap SA Exception
			e = e.getCause();
		}
		if (e instanceof SARuntimeException) {
			msg.append((((SARuntimeException) e).displayExceptionName() ? e.getClass().getSimpleName() + ": " : "") + //
					e.getMessage());
		} else if (e instanceof StubThrowingException) {
			msg.append("Stub throws an Exception of a type not throwable from the stubbed method.");
		} else if (e instanceof AssertionError && e.getStackTrace().length > 0 && //
				e.getStackTrace()[0].getClassName().startsWith("org.junit.")) {

			int i = 0;
			for (; i < e.getStackTrace().length && //
					e.getStackTrace()[i].getClassName().startsWith("org.junit."); i++)
				;
			msg.append(e.getMessage() == null || e.getMessage().trim().equals("") ? //
			"Assertion error" : "Assertion error: \"" + e.getMessage() + "\"");
			if (e.getStackTrace().length > 0)
				msg.append(" at " + e.getStackTrace()[i]);
		} else if (e instanceof AssertionError) {
			excludeSAUC = true;
			msg.append(e.getMessage() == null || e.getMessage().trim().equals("") ? //
			"Assertion error" : "Assertion error: \"" + e.getMessage() + "\"");
			if (e.getStackTrace().length > 0)
				msg.append(" at " + e.getStackTrace()[0]);
		} else if (e != null && (e instanceof OneLineLoopNotSupportedError) || (e instanceof MethodArgumentException)) {
			excludeSAUC = true;
			msg.append(e.getMessage());
		} else if (e instanceof BaseSAException) {
			msg.append(e.getMessage());
		} else if (e != null && e instanceof SAUCBuildInterruptedError) {
			excludeSAUC = true;
			msg.append("The build was cancelled whilst processing this line or method");
		} else if (e != null && e instanceof Error && e.getMessage() != null && //
				e.getMessage().startsWith("Unresolved compilation problem")) {
			excludeSAUC = true;
			msg.append(BasicUtils.JAVA_COMPILE_ERROR + e.getMessage());
		} else if (e != null) {
			excludeSAUC = true;
			msg.append(e.getClass().getName());
			if (e.getMessage() != null)
				msg.append(": ").append(e.getMessage());
			int stLength = getStackTraceLength(e, excludeSAUC);
			if (stLength > 0)
				msg.append(" - trace: ");
			if (stLength > 0)
				appendIfNotAspectTrace(msg, e.getStackTrace()[0].toString());
			if (stLength > 1)
				appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[1]);
			if (stLength > 2)
				appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[2]);
			if (stLength > 3)
				appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[3]);
			if (stLength > 4)
				appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[4]);
			if (true) {
				if (stLength > 5)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[5]);
				if (stLength > 6)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[6]);
				if (stLength > 7)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[7]);
				if (stLength > 8)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[8]);
				if (stLength > 9)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[9]);
				if (stLength > 10)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[10]);
				if (stLength > 11)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[11]);
				if (stLength > 12)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[12]);
				if (stLength > 13)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[13]);
				if (stLength > 14)
					appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[14]);
				if (stLength > 15)
					appendIfNotAspectTrace(msg, "...");
			}
		}
		if (e.getCause() != null) {
			appendCausedBy(msg, e.getCause(), excludeSAUC);
		}

		if (prefixMsg == null) {
			if (e instanceof ExtendedMessageException && ((ExtendedMessageException) e).getPrefixMessage() != null) {
				prefixMsg = ((ExtendedMessageException) e).getPrefixMessage();
			} else {
				Throwable cause = e.getCause();
				if (cause instanceof ExtendedMessageException && ((ExtendedMessageException) cause).getPrefixMessage() != null) {
					prefixMsg = ((ExtendedMessageException) cause).getPrefixMessage();
				}
			}
		}
		if (prefixMsg != null) {
			msg.insert(0, prefixMsg);
		}
		if (appendMsg == null) {
			if (e instanceof ExtendedMessageException && ((ExtendedMessageException) e).getAppendMessage() != null) {
				appendMsg = ((ExtendedMessageException) e).getAppendMessage();
			} else {
				Throwable cause = e.getCause();
				if (cause instanceof ExtendedMessageException && ((ExtendedMessageException) cause).getAppendMessage() != null) {
					appendMsg = ((ExtendedMessageException) cause).getAppendMessage();
				}
			}
		}
		if (appendMsg != null) {
			msg.append(appendMsg);
		}
		return msg.toString();
	}

	private static void appendCausedBy(StringBuilder msg, Throwable e, boolean excludeSAUC) {

		msg.append("  |  Caused by " + e.getClass().getSimpleName() + ": " + e.getMessage());
		int stLength = getStackTraceLength(e, excludeSAUC);
		if (stLength > 0)
			msg.append(" - trace: ");
		if (stLength > 0)
			appendIfNotAspectTrace(msg, e.getStackTrace()[0].toString());
		if (stLength > 1)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[1]);
		if (stLength > 2)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[2]);
		if (stLength > 3)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[3]);
		if (stLength > 4)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[4]);
		if (stLength > 5)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[5]);
		if (stLength > 6)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[6]);
		if (stLength > 7)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[7]);
		if (stLength > 8)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[8]);
		if (stLength > 9)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[9]);
		if (stLength > 10)
			appendIfNotAspectTrace(msg, "; " + e.getStackTrace()[10]);
		if (e.getCause() != null)
			appendCausedBy(msg, e.getCause(), excludeSAUC);
	}

	/**
	 * Gets the number of stack trace elements after removing Sureassert UC from the trace.
	 * 
	 * @param e
	 * @return
	 */
	public static int getStackTraceLength(Throwable e, boolean excludeSAUC) {

		if (!excludeSAUC)
			return e.getStackTrace().length;
		int saIdx = -1;
		StackTraceElement[] st = e.getStackTrace();
		for (int i = st.length - 1; i >= 0; i--) {
			if (st[i].getClassName().startsWith("com.sureassert.uc"))
				saIdx = i;
		}
		if (saIdx == -1)
			return st.length;
		else
			return saIdx;
	}

	private static boolean isReflectionClass(StackTraceElement el) {

		return el.getClassName().startsWith("java.lang.reflect.") || el.getClassName().startsWith("sun.reflect.");
	}

	public static int getLineNum(Throwable e, String methodName, Class<?> clazz) {

		for (StackTraceElement ste : e.getStackTrace()) {
			if (ste.getClassName().equals(clazz.getName()) && ste.getMethodName().equals(methodName))
				return ste.getLineNumber();
		}
		if (e.getCause() != null)
			return getLineNum(e.getCause(), methodName, clazz);
		else
			return -1;
	}

	public static Set<Integer> getLineNums(Throwable e, Class<?> clazz) {

		Set<Integer> lineNums = new HashSet<Integer>();
		for (StackTraceElement ste : e.getStackTrace()) {
			if (ste.getClassName().equals(clazz.getName()))
				lineNums.add(ste.getLineNumber());
		}
		if (e.getCause() != null)
			lineNums.addAll(getLineNums(e.getCause(), clazz));
		return lineNums;
	}

	public static int getLineNum(Throwable e, Class<?> clazz) {

		for (StackTraceElement ste : e.getStackTrace()) {
			if (ste.getClassName().equals(clazz.getName()))
				return ste.getLineNumber();
		}
		if (e.getCause() != null)
			return getLineNum(e.getCause(), clazz);
		else
			return -1;
	}

	/**
	 * Gets whether the given class is a non-static inner class
	 * 
	 * @param clazz
	 * @return
	 */
	public static boolean isNonStaticInnerClass(Class<?> clazz) {

		return clazz.getEnclosingClass() != null && !java.lang.reflect.Modifier.isStatic(clazz.getModifiers());
	}

	/**
	 * Get the method in the given class that is most closely compatible with the given parameters.
	 * 
	 * @param clazz
	 * @param methodName
	 * @param params
	 * @return
	 */
	public static Method getMethod(Class<?> clazz, String methodName, List<Object> params) {

		return BasicUtils.getMethod(clazz, methodName, params, true);
	}

	public static Method getMethod(Class<?> clazz, String methodName, List<Object> params, boolean checkParamSupertypes) {

		List<Class<?>> paramTypes = new ArrayList<Class<?>>();
		if (params != null) {
			for (Object param : params) {
				paramTypes.add(param == null ? null : param.getClass());
			}
		}
		return getMethodForType(clazz, methodName, paramTypes, checkParamSupertypes);
	}

	public static Method getMethodForType(Class<?> clazz, String methodName, List<Class<?>> paramTypes, boolean checkParamSupertypes) {

		// Check declared methods in this class
		for (Method method : clazz.getDeclaredMethods()) {

			if (method.getName().equals(methodName) && paramTypes.size() == method.getParameterTypes().length) {
				Class<?>[] thisParamTypes = method.getParameterTypes();
				int i = 0;
				for (; i < paramTypes.size(); i++) {
					Class<?> paramClass = paramTypes.get(i) == null ? null : paramTypes.get(i);
					if (!typeEquals(thisParamTypes[i], paramClass, checkParamSupertypes))
						break;
				}
				if (i == paramTypes.size())
					return method;
			}
		}
		// Check super-classes/interfaces
		Set<Class<?>> superClasses = BasicUtils.getSuperClassAndInterfaces(clazz);
		for (Class<?> superClass : superClasses) {
			Method method = getMethodForType(superClass, methodName, paramTypes, checkParamSupertypes);
			if (method != null)
				return method;
		}

		return null;
	}

	public static Constructor<?> getConstructor(Class<?> clazz, List<Object> params) {

		return BasicUtils.getConstructor(clazz, params, true);
	}

	public static Constructor<?> getConstructor(Class<?> clazz, List<Object> params, boolean checkParamSupertypes) {

		List<Class<?>> paramTypes = new ArrayList<Class<?>>();
		for (Object param : params) {
			paramTypes.add(param == null ? null : param.getClass());
		}
		return getConstructorForType(clazz, paramTypes, checkParamSupertypes);
	}

	public static Constructor<?> getConstructorForType(Class<?> clazz, List<Class<?>> paramTypes, boolean checkParamSupertypes) {

		// Check declared constructors in this class
		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {

			if (paramTypes.size() == constructor.getParameterTypes().length) {
				Class<?>[] thisParamTypes = constructor.getParameterTypes();
				int i = 0;
				for (; i < paramTypes.size(); i++) {
					Class<?> paramClass = paramTypes.get(i) == null ? null : paramTypes.get(i);
					if (!typeEquals(thisParamTypes[i], paramClass, checkParamSupertypes))
						break;
				}
				if (i == paramTypes.size()) {
					if (clazz.getName().equals(Object.class.getName()) && paramTypes.size() == 0) {
						// Java spec doesn't allow inheritance of no-arg Object constructor
						return null;
					}
					return constructor;
				}
			}
		}
		// Check super-classes/interfaces
		Set<Class<?>> superClasses = BasicUtils.getSuperClassAndInterfaces(clazz);
		for (Class<?> superClass : superClasses) {
			Constructor<?> constructor = getConstructorForType(superClass, paramTypes, checkParamSupertypes);
			if (constructor != null)
				return constructor;
		}
		return null;
	}

	public static String[] getClassNames(Class<?>[] classes) {

		String[] names = new String[classes.length];
		for (int i = 0; i < classes.length; i++) {
			names[i] = classes[i].getName();
		}
		return names;
	}

	public static Set<String> getDeclaredNonPrivateFinalFieldNames(Class<?> clazz) {

		Set<String> fieldNames = new HashSet<String>();
		boolean excludePackagePrivate = false;
		// Check declared fields in this class
		for (Field field : clazz.getDeclaredFields()) {
			int mod = field.getModifiers();
			if (!field.isSynthetic() && !Modifier.isPrivate(mod) && Modifier.isFinal(mod)) {
				if (!excludePackagePrivate || Modifier.isProtected(mod) || Modifier.isPublic(mod))
					fieldNames.add(field.getName());
			}
		}

		return fieldNames;
	}

	public static Object toObject(Class<?> clazz, List<Object> constructArgs, Map<String, Object> fields) throws IllegalArgumentException, InstantiationException, IllegalAccessException,
			InvocationTargetException {

		Constructor<?> constructor = getConstructor(clazz, constructArgs);
		if (constructor == null)
			return null;

		Object instance = constructor.newInstance(constructArgs);

		for (Entry<String, Object> fieldEntry : fields.entrySet()) {
			Field field = getField(clazz, fieldEntry.getKey());
			field.setAccessible(true);
			field.set(instance, fieldEntry.getValue());
		}
		return instance;
	}

	public static String unsplit(String[] array, String seperator) {

		StringBuilder str = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			str.append(array[i]);
			if (i < array.length - 1)
				str.append(seperator);
		}
		return str.toString();
	}

	public static String[] splitTrim(String str, String sep) {

		String[] split = str.split(sep);
		return trim(split);
	}

	public static String[] trim(String[] array) {

		if (array == null)
			return array;
		for (int i = 0; i < array.length; i++) {
			array[i] = array[i].trim();
		}
		return array;
	}

	public static String toString(Iterable<String> strs) {

		StringBuilder ret = new StringBuilder();
		for (Iterator<String> i = strs.iterator(); i.hasNext();) {
			ret.append(i.next());
			if (i.hasNext())
				ret.append(",");
		}
		return ret.toString();
	}

	/**
	 * Gets whether the given source code contains a JUnit test class.
	 * This is determined by checking for JUnit library import statements.
	 * 
	 * @param src
	 * @return
	 */
	public static boolean isJUnitTestClass(String src) {

		return src.contains("\nimport junit.") || src.contains("\nimport org.junit.");
	}

	public static String getTypeNameForSrc(String typeName, Map<String, String> resolvedTypeByTypeParam, //
			Set<String> methodGenericTypes) {

		StringBuilder postfix = new StringBuilder();
		String srcName = null;
		int i = 0;
		while (typeName.charAt(i) == '[') {
			postfix.append("[]");
			i++;
		}
		char type = typeName.charAt(i);
		if (type == 'V')
			srcName = "void";
		if (type == 'Z')
			srcName = "boolean";
		else if (type == 'B')
			srcName = "byte";
		else if (type == 'C')
			srcName = "char";
		else if (type == 'L')
			srcName = getSrcName(typeName.substring(i + 1, typeName.length() - 1), resolvedTypeByTypeParam, methodGenericTypes);
		else if (type == 'Q')
			srcName = resolvedTypeByTypeParam.get(typeName.substring(i + 1, typeName.length() - 1));
		else if (type == 'T') {
			srcName = getSrcName(typeName.substring(i + 1, typeName.length() - 1), resolvedTypeByTypeParam, methodGenericTypes);
			methodGenericTypes.add(srcName);
		} else if (type == '!')
			srcName = getSrcName(typeName.substring(i + 1, typeName.length() - 1), resolvedTypeByTypeParam, methodGenericTypes);
		else if (type == 'D')
			srcName = "double";
		else if (type == 'F')
			srcName = "float";
		else if (type == 'I')
			srcName = "int";
		else if (type == 'J')
			srcName = "long";
		else if (type == 'S')
			srcName = "short";
		return srcName + postfix.toString();
	}

	private static String getSrcName(String typeName, Map<String, String> resolvedTypeByTypeParam, //
			Set<String> methodGenericTypes) {

		final String START_TOKEN = "\uE206";
		final String END_TOKEN = "\uE207";

		StringBuilder src = new StringBuilder(typeName);
		int endGenTypIdx;
		while ((endGenTypIdx = src.indexOf(">")) > -1) {
			int startGenTypIdx = src.lastIndexOf("<", endGenTypIdx - 1);
			StringBuilder genTypesRaw = new StringBuilder(src.substring(startGenTypIdx + 1, endGenTypIdx));

			int typeEndIdx = -1;
			int typeStartIdx = 0;
			while ((typeEndIdx = genTypesRaw.indexOf(";", typeStartIdx)) > -1) {
				String genType = genTypesRaw.substring(typeStartIdx, typeEndIdx + 1);
				String srcType = typeStartIdx == 0 ? "" : ",";
				srcType += getTypeNameForSrc(genType, resolvedTypeByTypeParam, methodGenericTypes);
				genTypesRaw.replace(typeStartIdx, typeEndIdx + 1, srcType);
				typeStartIdx = typeStartIdx + srcType.length();
			}
			src.replace(startGenTypIdx, startGenTypIdx + 1, START_TOKEN);
			src.replace(endGenTypIdx, endGenTypIdx + 1, END_TOKEN);
			src.replace(startGenTypIdx + 1, endGenTypIdx, genTypesRaw.toString());
		}
		String returnSrc = src.toString();
		returnSrc = returnSrc.replace(START_TOKEN, "<");
		returnSrc = returnSrc.replace(END_TOKEN, ">");
		returnSrc = returnSrc.replace("*", "?");
		return returnSrc;
	}

	public static String getDefaultValueStrForClass(String typeName) {

		if (typeName == null)
			return "null";
		if (typeName.equals("boolean"))
			return "false";
		else if (typeName.equals("byte"))
			return "0";
		else if (typeName.equals("char"))
			return "0";
		else if (typeName.equals("double"))
			return "0";
		else if (typeName.equals("float"))
			return "0";
		else if (typeName.equals("int"))
			return "0";
		else if (typeName.equals("long"))
			return "0";
		else if (typeName.equals("short"))
			return "0";
		else
			return "null";
	}

	public static String getDefaultSINValueStrForClass(String typeName) {

		if (typeName == null)
			return "null";
		if (typeName.equals("boolean"))
			return "false";
		else if (typeName.equals("byte"))
			return "0b";
		else if (typeName.equals("char"))
			return "0c";
		else if (typeName.equals("double"))
			return "0.0";
		else if (typeName.equals("float"))
			return "0f";
		else if (typeName.equals("int"))
			return "0";
		else if (typeName.equals("long"))
			return "0l";
		else if (typeName.equals("short"))
			return "0s";
		else
			return "null";
	}

	public static String getNonPrimitiveClassName(String typeName) {

		if (typeName == null)
			return "null";
		if (typeName.equals("boolean"))
			return Boolean.class.getName();
		else if (typeName.equals("byte"))
			return Byte.class.getName();
		else if (typeName.equals("char"))
			return Character.class.getName();
		else if (typeName.equals("double"))
			return Double.class.getName();
		else if (typeName.equals("float"))
			return Float.class.getName();
		else if (typeName.equals("int"))
			return Integer.class.getName();
		else if (typeName.equals("long"))
			return Long.class.getName();
		else if (typeName.equals("short"))
			return Short.class.getName();
		else
			return typeName;
	}

	public static String getDefaultValueStrForType(String typeName) {

		char type = typeName.charAt(0);
		if (type == '[')
			return "null";
		else if (type == 'Z')
			return "false";
		else if (type == 'B')
			return "0";
		else if (type == 'C')
			return "0";
		else if (type == 'L')
			return "null";
		else if (type == 'D')
			return "0";
		else if (type == 'F')
			return "0";
		else if (type == 'I')
			return "0";
		else if (type == 'J')
			return "0";
		else if (type == 'S')
			return "0";
		else
			return "null";
	}

	public static String toSrcString(String[] array) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(array[i]);
		}
		return sb.toString();
	}

	public static Method findMethod(Class<?> clazz, ClassLoader cl, Signature methodSig) throws ClassNotFoundException {

		String[] paramClassNames = methodSig.getParamClassNames();
		Class<?>[] paramTypes = new Class<?>[paramClassNames.length];
		for (int i = 0; i < paramClassNames.length; i++) {
			paramTypes[i] = BasicUtils.getClass(paramClassNames[i], cl);
		}
		return BasicUtils.getMethodForType(clazz, methodSig.getMemberName(), Arrays.asList(paramTypes), true);
	}

	public static Constructor<?> findConstructor(Class<?> clazz, ClassLoader cl, Signature methodSig) throws ClassNotFoundException {

		String[] paramClassNames = methodSig.getParamClassNames();
		Class<?>[] paramTypes = new Class<?>[paramClassNames.length];
		for (int i = 0; i < paramClassNames.length; i++) {
			paramTypes[i] = BasicUtils.getClass(paramClassNames[i], cl);
		}
		return BasicUtils.getConstructorForType(clazz, Arrays.asList(paramTypes), true);
	}

	public static String appendUCNamePrefix(String ucNamePrefix, String ucName) {

		if (ucName != null && !ucName.contains("/"))
			return ucNamePrefix + ucName;
		else
			return ucName;
	}

	public static String appendUCNamePrefix(Signature signature, String ucName) {

		if (ucName != null && !ucName.contains("/")) {
			String ucNamePrefix = signature != null && signature.getClassName() != null ? //
			(BasicUtils.getSimpleClassName(signature.getClassName()) + "/") : "";
			return ucNamePrefix + ucName;
		} else
			return ucName;
	}

	public static void close(Closeable in) {

		try {
			if (in != null) {
				in.close();
			}
		} catch (IOException e) {
		}
	}

	public static Object invokePrivate(Class<?> clazz, Object instance, String methodName, Class<?>[] argTypes, Object[] args) {

		try {
			Method method = clazz.getDeclaredMethod(methodName, argTypes);
			method.setAccessible(true);
			return method.invoke(instance, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object getFieldValue(Class<?> clazz, Object instance, String fieldName) {

		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(instance);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static File getSaUCTempDir() {

		File tmpDir = new File(FileUtils.getTempDirectory(), "sauc-temp");
		tmpDir.mkdirs();
		return tmpDir;
	}

	public static void deleteSaUCTempDir() {

		File tmpDir = new File(FileUtils.getTempDirectory(), "sauc-temp");
		try {
			FileUtils.deleteDirectory(tmpDir);
		} catch (IOException e) {
			debug(ExceptionUtils.getFullStackTrace(e));
		}
	}

	public static String getClassNameFromBinaryClassName(String internalClassName) {

		return internalClassName.replace('/', '.');
	}

}
