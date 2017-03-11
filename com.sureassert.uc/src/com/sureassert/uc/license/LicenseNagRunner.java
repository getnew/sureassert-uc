package com.sureassert.uc.license;

public class LicenseNagRunner extends Thread implements Runnable {

	private final int NAG_INTERVAL_SECS = 60 * 20;

	public static final String LICENSE_NAG_MESSAGE_TITLE = "Sureassert UC";

	// public static final String LICENSE_NAG_MESSAGE = "Thank-you for trialing Sureassert UC.\n\n"
	// + //
	// "This tool is licensed for personal/educational use and for use on open-source projects.\n" +
	// //
	// "Low-cost individual and floating user commercial-use licenses can be purchased at http://www.sureassert.com.\n\n"
	// + //
	// "If you have a license please enter it at Window->Preferences->Sureassert UC to prevent this message re-appearing.";

	public static final String LICENSE_NAG_MESSAGE = "Thank-you for trialing Sureassert UC.\n\n" + //
			"This is a pre-release trial version licensed for personal/educational use, " + //
			"use on open-source projects and for commercial trial use.\n\n" + //
			"Low-cost commercial-use licenses will be available for the upcoming v1.0 release.";

	@Override
	public void run() {

		try {
			Thread.sleep(1000 * NAG_INTERVAL_SECS);
		} catch (InterruptedException e) {
		}
	}
}
