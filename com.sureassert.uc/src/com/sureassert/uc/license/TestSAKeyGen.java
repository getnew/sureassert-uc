package com.sureassert.uc.license;

import java.io.IOException;

import org.junit.Test;

public class TestSAKeyGen {

	@Test
	public void testSAKeyGen() {

		SAKeyGen keyGen = new SAKeyGen();
		SAKeyValidator validator = new SAKeyValidator();
		for (int x = 0; x < 1000; x++) {
			String email = "nd" + x + "@sa.com";
			String key = keyGen.generateKey(0, email, 0, 9);
			System.out.println(email + ": " + key);
			try {
				validator.validate(key, 0, 9, email);
			} catch (InvalidLicenseKeyException e) {
				System.out.println("INVALID: " + e.getErrorType());
			}
		}
	}

	@Test
	public void testInputKey() throws IOException {

		String email = "nd1@sa.com";
		SAKeyValidator validator = new SAKeyValidator();
		char c;
		do {
			System.out.print("Enter key: ");
			StringBuilder keyB = new StringBuilder();
			while ((c = (char) System.in.read()) != '\n' && c != '\r') {
				keyB.append(c);
			}
			System.in.read();
			String key = keyB.toString();
			try {
				validator.validate(key, 1, 0, email);
				System.out.println("VALID");
			} catch (InvalidLicenseKeyException e) {
				System.out.println("INVALID: " + e.getErrorType());
			}
		} while (true);
	}
}
