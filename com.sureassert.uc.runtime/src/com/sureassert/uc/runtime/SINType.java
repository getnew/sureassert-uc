/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.internal.Escaper;
import com.sureassert.uc.runtime.typeconverter.StringTC;

/**
 * A SIN Type string, encapsulating the raw and escaped versions of a given SIN type string.
 * <p>
 * The raw SIN includes escape characters such as '' and []. The escaped SIN replaces literals
 * wrapped by the escape characters with placeholders.
 * 
 * @author Nathan Dolan
 */
public class SINType {

	private final String rawSINType;
	private final String escapedSINType;
	private final Escaper escaper;

	public static SINType newFromRaw(String rawSINType) throws TypeConverterException {

		Escaper escaper = new Escaper(rawSINType == null ? null : rawSINType.trim());
		return new SINType(rawSINType, escaper.getEscapedSIN(), escaper);
	}

	public static SINType newFromRaw(String prefix, String value) throws TypeConverterException {

		if (prefix == null)
			prefix = "";
		return newFromRaw(value == null ? null : prefix + ":" + value);
	}

	public static SINType newFromEscaped(String escapedSINType, Escaper escaper) throws TypeConverterException {

		return new SINType(null, escapedSINType, escaper);
	}

	private SINType(String rawSINType, String escapedSINType, Escaper escaper) {

		this.rawSINType = rawSINType;
		// Unescape fully escapedSINType (i.e. where whole string is escaped, for 'x' or [x]
		this.escapedSINType = escaper.unescape(escapedSINType);
		this.escaper = escaper;
	}

	public String getRawSINType() {

		return rawSINType;
	}

	public String toRawString() {

		return escaper.toRaw(escapedSINType);
	}

	public Escaper getEscaper() {

		return escaper;
	}

	public String getTypePrefix() {

		if (escaper.isQuotedStringType(escapedSINType))
			return StringTC.PREFIX;
		int prefixEndIndex = escapedSINType.indexOf(":");
		String prefix = prefixEndIndex > -1 ? escapedSINType.substring(0, prefixEndIndex).trim() : null;
		if (prefix == null || prefix.contains(" ") || prefix.contains("(") || prefix.contains("{"))
			return null;
		return prefix;
	}

	public String getSINValue() {

		String escapedType = escapedSINType;
		if (escaper.isQuotedStringType(escapedSINType)) {
			int prefixEndIndex = escapedType.indexOf(":");
			escapedType = escaper.unescape(prefixEndIndex > -1 ? //
			escapedType.substring(prefixEndIndex + 1) : escapedType);
		}
		int prefixEndIndex = escapedType.indexOf(":");
		return escaper.unescape(prefixEndIndex > -1 ? //
		escapedType.substring(prefixEndIndex + 1) : escapedType);
	}

	@Override
	public String toString() {

		return escapedSINType;
	}
}
