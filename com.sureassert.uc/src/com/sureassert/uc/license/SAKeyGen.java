package com.sureassert.uc.license;

import java.util.Date;
import java.util.Random;

import org.sureassert.uc.annotation.MultiUseCase;
import org.sureassert.uc.annotation.UseCase;

class SAKeyGen {

	static final int LICENSE_TYPE_ID_FLOATING = 0;
	static final int LICENSE_TYPE_ID_OPEN_SOURCE = 1;
	static final int LICENSE_TYPE_ID_INDIVIDUAL = 2;

	static final String[][] LICENSE_TYPE_CHAR_CHOICES_BY_ID = new String[][] { //
	{ "4", "M", "Q", "J" }, { "W", "9", "2", "A" }, { "7", "C", "K", "B" } };

	private static final String[][] HEX_KEY_CHOICE_A = new String[][] { //
	{ "3", "R" }, { "7", "K" }, { "B", "Z" }, { "8", "S" }, //
			{ "5", "J" }, { "C", "P" }, { "E", "U" }, { "H", "Q" }, //
			{ "D", "W" }, { "2", "L" }, { "F", "N" }, { "4", "Y" }, //
			{ "6", "T" }, { "9", "V" }, { "A", "X" }, { "G", "M" } };

	// WARNING: Mapped values are not unique
	static final String[][] CHAR_TRANSLATE_MAP_T = new String[][] {//
	{ "0", "J" }, { "1", "V" }, { "2", "N" }, { "3", "P" }, { "4", "K" }, { "5", "4" }, { "6", "X" }, //
			{ "7", "R" }, { "8", "B" }, { "9", "3" }, { "0", "L" }, { "A", "Q" }, { "B", "A" }, { "C", "Z" }, //
			{ "E", "6" }, { "F", "E" }, { "G", "D" }, { "H", "2" }, { "I", "U" }, { "J", "Y" }, { "K", "W" }, //
			{ "L", "8" }, { "M", "C" }, { "N", "M" }, { "O", "7" }, { "P", "F" }, { "Q", "S" }, { "R", "G" }, //
			{ "S", "H" }, { "T", "9" }, { "U", "V" }, { "V", "5" }, { "W", "M" }, { "X", "B" }, { "Y", "T" }, //
			{ "Z", "6" } };

	static final String[] CODE_ALPHABET_A = { "7", "I", "A", "G", "T", "8", "0", "D", "2", "M", "X", "K", "V", "P", //
			"R", "U", "4", "Q", "9", "J", "1", "H", "5", "3", "O", "N", "Y", "6", "S", "Z", "E", "W", "B", "F", "L", "C" };
	static final String[] CODE_ALPHABET_B = { "M", "B", "7", "A", "G", "T", "8", "D", "2", "X", "K", "V", "P", //
			"R", "U", "4", "Q", "9", "J", "H", "5", "3", "N", "Y", "6", "S", "Z", "E", "W", "F", "L", "C" };

	private static final String DEFAULT_TRANSLATE_MAP_CHAR = "B";

	static final long startDateMillis = new Date(2011 - 1900, 0, 1).getTime();

	private final Random random = new Random();

	static void main(String[] args) {

		new SAKeyGen();
	}

	String generateKey(int licenseTypeID, String email, int majorVersion, int minorVersion) {

		// Get data; all must resolve to allowable characters ([0-9A-Z] excluding [01OI])
		String l = LICENSE_TYPE_CHAR_CHOICES_BY_ID[licenseTypeID][random.nextInt(4)];

		String[] emailChars = getEmailChars(email);
		String e1 = emailChars[0];
		String e2 = emailChars[1];

		String v1 = HEX_KEY_CHOICE_A[majorVersion][random.nextInt(2)];
		String v2 = HEX_KEY_CHOICE_A[minorVersion][random.nextInt(2)];

		String daysSince2011Hex = toHex(getDaysSince2011(), 4);
		String d1 = translateHex(daysSince2011Hex.substring(0, 1));
		String d2 = translateHex(daysSince2011Hex.substring(1, 2));
		String d3 = translateHex(daysSince2011Hex.substring(2, 3));
		String d4 = translateHex(daysSince2011Hex.substring(3, 4));

		String r1 = CHAR_TRANSLATE_MAP_T[random.nextInt(CHAR_TRANSLATE_MAP_T.length)][1];
		String r2 = CHAR_TRANSLATE_MAP_T[random.nextInt(CHAR_TRANSLATE_MAP_T.length)][1];
		String r3 = CHAR_TRANSLATE_MAP_T[random.nextInt(CHAR_TRANSLATE_MAP_T.length)][1];
		String r4 = CHAR_TRANSLATE_MAP_T[random.nextInt(CHAR_TRANSLATE_MAP_T.length)][1];
		String r5 = CHAR_TRANSLATE_MAP_T[random.nextInt(CHAR_TRANSLATE_MAP_T.length)][1];

		String key = r1 + c2(r1, v2, d2) + v2 + d2 + "-" + //
				c2(r2, l, e2) + r2 + l + e2 + "-" + //
				r3 + c2(r3, d1, e1) + d1 + e1 + "-" + //
				d3 + r4 + v1 + c2(d3, r4, v1) + "-" + //
				r5 + d4 + c2(r5, d4) + c(r1, v2, d2, r2, l, e2, r3, d1, e1, d3, r4, v1, r5, d4);

		return key;
	}

	static String c(String... chars) {

		int checkVal = 0;
		for (String b : chars) {
			checkVal += getCodeAlphabetIdx(b);
		}
		checkVal = checkVal % CODE_ALPHABET_B.length;
		return CODE_ALPHABET_B[checkVal];
	}

	static String c(String str) {

		int checkVal = 0;
		for (int i = 0; i < str.length(); i++) {
			checkVal += getCodeAlphabetIdx(str.substring(i, i + 1));
		}
		checkVal = checkVal % CODE_ALPHABET_B.length;
		return CODE_ALPHABET_B[checkVal];
	}

	static String c2(String... chars) {

		int checkVal = 0;
		int idx = 0;
		for (String b : chars) {
			checkVal += (getCodeAlphabetIdx(b) + 1) * (idx + 1);
			idx++;
		}
		checkVal = checkVal % CODE_ALPHABET_B.length;
		return CODE_ALPHABET_B[checkVal];
	}

	static String c2(String chars) {

		int checkVal = 0;
		for (int idx = 0; idx < chars.length(); idx++) {
			checkVal += (getCodeAlphabetIdx(chars.substring(idx, idx + 1)) + 1) * (idx + 1);
		}
		checkVal = checkVal % CODE_ALPHABET_B.length;
		return CODE_ALPHABET_B[checkVal];
	}

	/**
	 * Gets an array containing the first and last characters of the local name part of
	 * the given email address (the part before the @).
	 * 
	 * If the string given is not a valid email address, gets the first and last characters
	 * of the given string.
	 * 
	 * @param email
	 * @return
	 */
	@MultiUseCase(uc = { @UseCase(a = "'joe.bloggs@test.com'", e = "#=(retval,[a:'j','s'])"), @UseCase(a = "'mycompany'", e = "#=(retval,[a:'m','y'])"),
			@UseCase(a = "'m'", e = "#=(retval,[a:'m','-'])"), @UseCase(a = "''", e = "#=(retval,[a:'-','-'])"), @UseCase(a = "null", e = "#=(retval,[a:'-','-'])") })
	static String[] getEmailChars(String email) {

		if (email == null || email.length() == 0)
			return new String[] { "-", "-" };
		return new String[] { c(email), c2(email) };
	}

	@MultiUseCase(uc = { @UseCase(a = "'e'", e = "'6'"), @UseCase(a = "'-'", e = "'B'"), @UseCase(a = "'n-'", e = "'MB'") })
	private String translateChars(String str) {

		StringBuilder translated = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			String c = str.substring(i, i + 1);
			String transC = getTranslatedChar(c.toUpperCase());
			if (transC == null)
				translated.append(DEFAULT_TRANSLATE_MAP_CHAR);
			else
				translated.append(transC);
		}
		return translated.toString();
	}

	private String getTranslatedChar(String c) {

		for (int i = 0; i < CHAR_TRANSLATE_MAP_T.length; i++) {
			if (c.equals(CHAR_TRANSLATE_MAP_T[i][0]))
				return CHAR_TRANSLATE_MAP_T[i][1];
		}
		return null;
	}

	static String getReverseTranslatedChar(String c) {

		for (int i = 0; i < CHAR_TRANSLATE_MAP_T.length; i++) {
			if (c.equals(CHAR_TRANSLATE_MAP_T[i][1]))
				return CHAR_TRANSLATE_MAP_T[i][0];
		}
		return null;
	}

	static int getHexChoiceAIdx(String c) {

		for (int i = 0; i < HEX_KEY_CHOICE_A.length; i++) {
			if (c.equals(HEX_KEY_CHOICE_A[i][0]) || c.equals(HEX_KEY_CHOICE_A[i][1]))
				return i;
		}
		return -1;
	}

	@UseCase
	private int getDaysSince2011() {

		return (int) ((new Date().getTime() - startDateMillis) / (1000 * 60 * 60 * 24));
	}

	@MultiUseCase(uc = { @UseCase(a = { "15", "4" }, e = "'000F'"), @UseCase(a = { "65530", "4" }, e = "'FFFA'") })
	private String toHex(int x, int numChars) {

		String hex = Integer.toHexString(x).toUpperCase();
		while (hex.length() < numChars) {
			hex = "0" + hex;
		}
		if (hex.length() > numChars)
			hex = hex.substring(0, numChars);
		return hex;
	}

	@MultiUseCase(uc = { @UseCase(a = "'3'", e = "|(retval.equals('8'),retval.equals('S'))"), @UseCase(a = "'3B'") })
	private String translateHex(String hex) {

		StringBuilder translated = new StringBuilder();
		for (int i = 0; i < hex.length(); i++) {
			String s = hex.substring(i, i + 1);
			int sVal = Integer.valueOf(s, 16);
			translated.append(HEX_KEY_CHOICE_A[sVal][random.nextInt(2)]);
		}
		return translated.toString();
	}

	private static int getCodeAlphabetIdx(String ch) {

		ch = ch.toUpperCase();
		for (int i = 0; i < CODE_ALPHABET_A.length; i++) {
			if (ch.equals(CODE_ALPHABET_A[i]))
				return i;
		}
		return 3;
	}
}
