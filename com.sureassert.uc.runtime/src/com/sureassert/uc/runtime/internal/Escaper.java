/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.regex.Matcher;

import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.BaseSAException;
import com.sureassert.uc.runtime.typeconverter.DoubleTC;
import com.sureassert.uc.runtime.typeconverter.StringTC;

/**
 * The Escaper is used to replace escaped or captured groups with place-holders
 * to facilitate parsing of a SIN Expression. Place-holders use a special control character
 * either side of a place-holder ID unique in the given expression.
 * <p>
 * Escaping should be invisible to TypeConverter implementations; it is handled in the evaluator and
 * TypeConverter base types.
 * <p>
 * <li>SIN Types that contain control characters must be between square brackets: [SINType] e.g.
 * [m:k1=v1,k2=v2]
 * <li>Any SIN Value that contains control characters must be quoted: e.g. s:' my, awkward [value] '
 * <li>value is a Type that has a TypeConverter
 * <li>TypeConverter used is StringTC unless prefix used
 * <p>
 * Must escape inner-most value first to avoid parse errors on parent values <br>
 * e.g. [a:[str1],[m:[k1]=[v1]]]
 * <p>
 * SINExpression parsing:
 * <p>
 * 1. Escape SIN Values in quotes (non-nesting escape fragments)
 * <li>[m:k1=v1,k2=' \'v2\'\\ ', k3=[l:v1,[a:[i:1],[i:2]],' v3 ']]
 * <p>
 * 2. Escape SIN Types in square brackets (nesting escape fragments - multiple passes)
 * <li>[m:k1=i:5,k2=$1$, k3=[l:v1,[a:[i:1],[i:2]],$2$]] -- $1= 'v2'\ ;$2 = v3
 * <li>[m:k1=i:5,k2=$1$, k3=[l:v1,[a:$3$,$4$],$2$]] -- $1= 'v2'\ ;$2 = v3 ;$3=i:1;$4=i:2;
 * <li>[m:k1=i:5,k2=$1$, k3=[l:v1,$5$,$2$]] -- $1= 'v2'\ ;$2 = v3 ;$3=i:1;$4=i:2;$5=a:$3$,$4$
 * <li>[m:k1=i:5,k2=$1$, k3=$6$] -- $1= 'v2'\ ;$2 = v3;$3=i:1;$4=i:2;$5=a:$3$,$4$;$6=l:v1,$5$,$2$
 * <li>$7$ -- $1= 'v2'\ ;$2 = v3;$3=i:1;$4=i:2;$5=a:$3$,$4$;$6=l:v1,$5$,$2$;$7=m:k1=i:5,k2=$1$,
 * k3=$6$
 */
public class Escaper {

	private static final Character PLACEHOLDER_PREFIX = '\uE206';
	private static final String PLACEHOLDER_PREFIX_STR = PLACEHOLDER_PREFIX.toString();
	private static final Character TEMP_PREFIX = '\uE207';

	private final Map<String, String> placeholderToLiteral;

	private final String rawSIN;

	private final String escapedSIN;

	private static WeakHashMap<String, WeakReference<Escaper>> rawToEscapedCache = new WeakHashMap<String, WeakReference<Escaper>>();

	public Escaper(String rawSIN) throws TypeConverterException {

		this.rawSIN = rawSIN;

		if (rawSIN == null) {
			escapedSIN = null;
			placeholderToLiteral = new HashMap<String, String>();
		} else {

			if (rawSIN.indexOf(PLACEHOLDER_PREFIX) > -1)
				throw new TypeConverterException("The string \"" + rawSIN
						+ "\" contains characters \\uE206.  This special character is required as reserved a character by Sureassert and cannot be used in expressions.");
			if (rawSIN.indexOf(TEMP_PREFIX) > -1)
				throw new TypeConverterException("The string \"" + rawSIN
						+ "\" contains characters \\uE207.  This special character is required as reserved a character by Sureassert and cannot be used in expressions.");

			Escaper cachedEscaper = getCachedEscaper(rawSIN);
			if (cachedEscaper != null) {
				escapedSIN = cachedEscaper.escapedSIN;
				placeholderToLiteral = cachedEscaper.placeholderToLiteral;
			} else {

				try {
					int placeholderNum = 1;
					placeholderToLiteral = new HashMap<String, String>();
					// TODO: Escape \' and \\
					// 1. Escape special characters
					StringBuilder escapedSINp0 = new StringBuilder(rawSIN);
					int index = -1;
					while ((index = escapedSINp0.indexOf("//")) > -1) {
						String placeholder = String.format("%c%d%c", //
								PLACEHOLDER_PREFIX, placeholderNum++, PLACEHOLDER_PREFIX);
						escapedSINp0.replace(index, index + 2, placeholder);
						placeholderToLiteral.put(placeholder, "/");
					}
					while ((index = escapedSINp0.indexOf("/[")) > -1) {
						String placeholder = String.format("%c%d%c", //
								PLACEHOLDER_PREFIX, placeholderNum++, PLACEHOLDER_PREFIX);
						escapedSINp0.replace(index, index + 2, placeholder);
						placeholderToLiteral.put(placeholder, "[");
					}
					while ((index = escapedSINp0.indexOf("/]")) > -1) {
						String placeholder = String.format("%c%d%c", //
								PLACEHOLDER_PREFIX, placeholderNum++, PLACEHOLDER_PREFIX);
						escapedSINp0.replace(index, index + 2, placeholder);
						placeholderToLiteral.put(placeholder, "]");
					}
					while ((index = escapedSINp0.indexOf("[']")) > -1) {
						String placeholder = String.format("%c%d%c", //
								PLACEHOLDER_PREFIX, placeholderNum++, PLACEHOLDER_PREFIX);
						escapedSINp0.replace(index, index + 3, placeholder);
						placeholderToLiteral.put(placeholder, "'");
					}

					// 2. Capture SIN Values in quotes (non-nesting escape fragments)
					StringBuilder escapedSINp1 = new StringBuilder();
					boolean capturing = false;
					StringBuilder capturedStr = new StringBuilder();
					char c;
					int length = escapedSINp0.length();
					for (int i = 0; i < length; i++) {
						c = escapedSINp0.charAt(i);
						if (c == '\'') {
							capturing = !capturing;
							if (!capturing) {
								capturedStr.append(c);
								String placeholder = String.format("%c%d%c", //
										PLACEHOLDER_PREFIX, placeholderNum++, PLACEHOLDER_PREFIX);
								placeholderToLiteral.put(placeholder, capturedStr.toString());
								escapedSINp1.append(placeholder);
								capturedStr = new StringBuilder();
							}
						} else if (!capturing) {
							escapedSINp1.append(c);
						}
						if (capturing) {
							assert capturedStr != null;
							capturedStr.append(c);
						}
					}

					// 3. Auto-insert [] to arguments
					int closeIdx = -1;
					Stack<Integer> openIdxStack = new Stack<Integer>();
					char thisChar;
					for (int i = 0; i < escapedSINp1.length(); i++) {
						thisChar = escapedSINp1.charAt(i);
						if (thisChar == '(') {
							openIdxStack.push(i);
						} else if (thisChar == ')') {
							int openIdx = openIdxStack.pop();
							closeIdx = i;
							if (openIdx > -1 && openIdx < closeIdx - 1) {
								i += insertEscapes(escapedSINp1, openIdx, closeIdx);
							}
						}
					}

					// 4. Escape SIN Types in square brackets (nesting escape fragments - multiple
					// passes)
					String escapedSINp3 = escapedSINp1.toString();
					capturing = false;
					capturedStr = null;
					int closeCaptureIdx;
					while ((closeCaptureIdx = escapedSINp3.indexOf("]")) > -1) {
						int openCatureIdx = escapedSINp3.lastIndexOf("[", closeCaptureIdx);
						if (openCatureIdx == -1)
							throw new TypeConverterException("Close-square-bracket ] encountered without matching [");
						String captured = escapedSINp3.substring(openCatureIdx, closeCaptureIdx + 1);
						String placeholder = String.format("%c%d%c", //
								PLACEHOLDER_PREFIX, placeholderNum++, PLACEHOLDER_PREFIX);
						placeholderToLiteral.put(placeholder, captured);
						escapedSINp3 = escapedSINp3.substring(0, openCatureIdx) + placeholder + //
								escapedSINp3.substring(closeCaptureIdx + 1);
					}

					// 5. Replace decimal places with ,
					escapedSINp3 = escapedSINp3.replaceAll("\\.(\\d)", DoubleTC.DECIMAL_POINT_REPLACEMENT + "$1");

					escapedSIN = escapedSINp3.trim();

					rawToEscapedCache.put(rawSIN, new WeakReference<Escaper>(this));

				} catch (BaseSAException e) {
					throw new TypeConverterException("Syntax of expression \"" + rawSIN + "\" is invalid: " + //
							e.getMessage());
				} catch (Exception e) {
					throw new TypeConverterException("Syntax of expression \"" + rawSIN + "\" is invalid");
				}
			}
		}
	}

	private static Escaper getCachedEscaper(String rawSIN) {

		WeakReference<Escaper> cachedEscaperRef = rawToEscapedCache.get(rawSIN);
		if (cachedEscaperRef != null) {
			Escaper cachedEscaper = cachedEscaperRef.get();
			if (cachedEscaper != null)
				return cachedEscaper;
		}
		return null;
	}

	private int insertEscapes(StringBuilder str, int openIdx, int closeIdx) {

		int numCharsInserted = 0;
		StringBuilder bracketedStr = new StringBuilder(str.substring(openIdx + 1, closeIdx).trim());

		if (bracketedStr.length() != 0) {

			// Remove any inner bracketed text
			int innerOpenIdx;
			List<String> replacedStrs = new ArrayList<String>();
			int numTempReplaced = 0;
			while ((innerOpenIdx = bracketedStr.lastIndexOf("(")) > -1) {
				int innerCloseIdx = bracketedStr.indexOf(")", innerOpenIdx);
				String placeholder = String.format("%c%d%c", //
						TEMP_PREFIX, numTempReplaced++, TEMP_PREFIX);
				replacedStrs.add(bracketedStr.substring(innerOpenIdx, innerCloseIdx + 1));
				bracketedStr = bracketedStr.replace(innerOpenIdx, innerCloseIdx + 1, placeholder);
			}

			StringBuffer newStr = new StringBuffer();
			String[] args = bracketedStr.toString().split(",");
			for (int i = 0; i < args.length; i++) {
				args[i] = args[i].trim();
				// Replace inner bracketed text
				for (int tempReplaceId = numTempReplaced - 1; tempReplaceId >= 0; tempReplaceId--) {
					String placeholder = String.format("%c%d%c", //
							TEMP_PREFIX, tempReplaceId, TEMP_PREFIX);
					args[i] = args[i].replaceAll(placeholder, Matcher.quoteReplacement(replacedStrs.get(tempReplaceId)));
				}
				if (args[i].length() > 0) {
					if (!Character.isDigit(args[i].charAt(0)) && args[i].charAt(0) != '[' && //
							args[i].charAt(0) != PLACEHOLDER_PREFIX) {
						args[i] = "[" + args[i] + "]";
					}
					newStr.append(args[i]);
					if (i < args.length - 1)
						newStr.append(",");
				}
			}
			str.replace(openIdx + 1, closeIdx, newStr.toString());
			return newStr.length() - (closeIdx - (openIdx + 1));
		}
		return numCharsInserted;
	}

	/**
	 * Unescapes the given string if the string is an escaper placeholder.
	 * <p>
	 * Note: this does NOT unescape every placeholder in the given string; for this see
	 * toRaw(String)
	 * 
	 * @param string
	 * @return If the string given is an escaper placeholder, returns the literal
	 *         that the placeholder represents. Otherwise, returs the given string.
	 */
	public String unescape(String string) {

		String unescaped = placeholderToLiteral.get(string);
		if (isQuotedStringType(string))
			return StringTC.PREFIX_WITH_SEPARATOR + unescaped.substring(1, unescaped.length() - 1);
		return unescaped == null ? string : unescaped.substring(1, unescaped.length() - 1);
	}

	public boolean isQuotedStringType(String string) {

		String unescaped = placeholderToLiteral.get(string);
		return unescaped != null && unescaped.startsWith("'") && unescaped.endsWith("'");
	}

	/**
	 * Replaces all place-holders in the given string with their mapped literals,
	 * including place-holders nested within mapped literals.
	 * 
	 * @param string
	 * @return The given string with all place-holders replaced with literals
	 */
	public String toRaw(String string) {

		int placeholderIndex;
		while ((placeholderIndex = string.indexOf(PLACEHOLDER_PREFIX)) > -1) {
			int placeholderEndIndex = string.indexOf(PLACEHOLDER_PREFIX, placeholderIndex + 1);
			String placeholder = string.substring(placeholderIndex, placeholderEndIndex + 1);
			String literal = placeholderToLiteral.get(placeholder);
			string = string.substring(0, placeholderIndex) + literal + string.substring(placeholderEndIndex + 1);
		}
		return string;
	}

	/**
	 * Returns whether the given SIN Value string has a type prefix
	 * 
	 * @param string
	 * @return boolean
	 */
	public static boolean hasPrefix(String sinVal) {

		return sinVal.contains(":");
	}

	public String getRawSIN() {

		return rawSIN;
	}

	public String getEscapedSIN() {

		return escapedSIN;
	}

	public static String escapeControlChars(String str) {

		str = str.replace("'", Escaper.TEMP_PREFIX + "'");
		str = str.replace("[", "/[");
		str = str.replace("]", "/]");
		str = str.replace(Escaper.TEMP_PREFIX + "'", "[']");
		return str;
	}
}
