/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.commons.lang.ObjectUtils;

import com.sureassert.uc.runtime.exception.SARuntimeException;

public class BuiltInMethods {

	public static Object __SA_nop(Object obj) {

		return obj;
	}

	public static boolean __SA_eq(Object o1, Object o2) {

		return BasicUtils.equalsAsPrimitive(o1, o2);
	}

	public static boolean __SA_veq(Object o1, Object o2) {

		return ObjectUtils.equals(o1, o2);
	}

	public static boolean __SA_neq(Object o1, Object o2) {

		return !BasicUtils.equalsAsPrimitive(o1, o2);
	}

	public static boolean __SA_not(boolean b) {

		return !b;
	}

	public static boolean __SA_and(boolean b1, boolean b2) {

		return b1 && b2;
	}

	public static boolean __SA_or(boolean b1, boolean b2) {

		return b1 || b2;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean __SA_gt(Comparable o1, Comparable o2) {

		return o1.compareTo(o2) > 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean __SA_lt(Comparable o1, Comparable o2) {

		return o1.compareTo(o2) < 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean __SA_gtoe(Comparable o1, Comparable o2) {

		return o1.compareTo(o2) >= 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean __SA_ltoe(Comparable o1, Comparable o2) {

		return o1.compareTo(o2) <= 0;
	}

	public static Object __SA_element(Object array, int index) {

		try {
			return Array.get(array, index);
		} catch (Exception e) {
			throw new SARuntimeException(e);
		}
	}

	public static boolean __SA_arrayeq(Object[] a1, Object[] a2) {

		return Arrays.deepEquals(a1, a2);
	}

	public static boolean __SA_arrayeq(char[] a1, char[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(byte[] a1, byte[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(short[] a1, short[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(int[] a1, int[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(long[] a1, long[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(float[] a1, float[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(double[] a1, double[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static boolean __SA_arrayeq(boolean[] a1, boolean[] a2) {

		return Arrays.equals(a1, a2);
	}

	public static String __SA_add(Object o1, Object o2) {

		return o1.toString() + o2.toString();
	}

	public static boolean __SA_isa(Object o, Class<?> clazz) {

		return clazz.isAssignableFrom(BasicUtils.toNonPrimitiveType(o.getClass()));
	}

	public static boolean isBuiltInMethodSymbol(String symbol) {

		return getBuiltInMethodName(symbol) != null;
	}

	public static String getBuiltInMethodName(String symbol) {

		if (symbol.equals("")) {
			return "__SA_nop";
		} else if (symbol.equals("==")) {
			return "__SA_eq";
		} else if (symbol.equals("=")) {
			return "__SA_veq";
		} else if (symbol.equals("!=")) {
			return "__SA_neq";
		} else if (symbol.equals("!")) {
			return "__SA_not";
		} else if (symbol.equals(">")) {
			return "__SA_gt";
		} else if (symbol.equals("<")) {
			return "__SA_lt";
		} else if (symbol.equals(">=")) {
			return "__SA_gtoe";
		} else if (symbol.equals("<=")) {
			return "__SA_ltoe";
		} else if (symbol.equals("&") || symbol.equals("&&")) {
			return "__SA_and";
		} else if (symbol.equals("|") || symbol.equals("||")) {
			return "__SA_or";
		} else if (symbol.equals("#=")) {
			return "__SA_arrayeq";
		} else if (symbol.equals("#")) {
			return "__SA_element";
		} else if (symbol.equals("+")) {
			return "__SA_add";
		} else if (symbol.equals("isa")) {
			return "__SA_isa";
		} else {
			return null;
		}
	}

}
