/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;

import com.sureassert.uc.SaUCBuilderActivator;
import com.sureassert.uc.builder.SAUCBuilder;
import com.sureassert.uc.license.InvalidLicenseKeyException;
import com.sureassert.uc.license.SAKeyValidator;
import com.sureassert.uc.license.SAKeyValidator.LicenseKeyData;

public class SaUCPreferences {

	public static final String PLUGIN_ID = SAUCBuilder.PLUGIN_ID;

	public static final String PREFIX = PLUGIN_ID + ".";

	public static final String PREFERENCE_PAGE_CONTEXT = PREFIX + "preference_page_context";

	public static final String PREF_KEY_COVERAGE_WARN_THRESHOLD = PREFIX + "coverage.warnThreshold";
	public static final int PREF_DEFAULT_COVERAGE_WARN_THRESHOLD = 95;

	public static final String PREF_KEY_COVERAGE_ERROR_THRESHOLD = PREFIX + "coverage.errorThreshold";
	public static final int PREF_DEFAULT_COVERAGE_ERROR_THRESHOLD = 50;

	public static final String PREF_KEY_COVERAGE_ENABLED = PREFIX + "coverage.coverageEnabled";
	public static final boolean PREF_DEFAULT_COVERAGE_ENABLED = true;

	public static final String PREF_KEY_COVERAGE_PROBLEMS_ENABLED = PREFIX + "coverage.coverageProblemsEnabled";
	public static final boolean PREF_DEFAULT_COVERAGE_PROBLEMS_ENABLED = false;

	public static final String PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED = PREFIX + "coverage.coverageFileDecorationEnabled";
	public static final boolean PREF_DEFAULT_COVERAGE_FILE_DECORATION_ENABLED = true;

	public static final String PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED = PREFIX + "coverage.coverageFilePercentEnabled";
	public static final boolean PREF_DEFAULT_COVERAGE_FILE_PERCENT_ENABLED = true;

	public static final String PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED = PREFIX + "coverage.coverageProjectDecorationEnabled";
	public static final boolean PREF_DEFAULT_COVERAGE_PROJECT_DECORATION_ENABLED = false;

	public static final String PREF_KEY_COVERAGE_DISPLAY_ENABLED = PREFIX + "coverage.coverageDisplayEnabled";
	public static final boolean PREF_DEFAULT_COVERAGE_DISPLAY_ENABLED = true;

	public static final String PREF_KEY_COVERAGE_REQUIRED_THRESHOLD = PREFIX + "coverage.requiredThreshold";
	public static final int PREF_DEFAULT_COVERAGE_REQUIRED_THRESHOLD = 1;

	public static final String PREF_KEY_JUNIT_AUTOMATION = PREFIX + "junit.runAutomatically";
	public static final boolean PREF_DEFAULT_JUNIT_AUTOMATION = false;

	public static final String PREF_KEY_JUNIT_EXCLUDE_FILTER = PREFIX + "junit.excludeFilter";
	public static final String PREF_DEFAULT_JUNIT_EXCLUDE_FILTER = "";

	// public static final String PREF_KEY_AUTO_JUNIT_USE_SAUC_CONTEXT = PREFIX +
	// "junit.autoUseTransformed";
	// public static final boolean PREF_DEFAULT_JUNIT_USE_SAUC_CONTEXT = true;

	public static final String PREF_KEY_STUBS_ALLOW_SOURCE_STUBS = PREFIX + "stubs.allowSourceStubs";
	public static final boolean PREF_DEFAULT_STUBS_ALLOW_SOURCE_STUBS = true;

	public static final String PREF_KEY_LICENCE_KEY = PREFIX + "license.licenseKey";
	public static final String PREF_DEFAULT_LICENCE_KEY = "";

	public static final String PREF_KEY_LICENCE_EMAIL = PREFIX + "license.licenseEmail";
	public static final String PREF_DEFAULT_LICENCE_EMAIL = "";

	public static final String PREF_KEY_EXEC_CONCURRENT = PREFIX + "exec.concurrent";
	public static final boolean PREF_DEFAULT_EXEC_CONCURRENT = true;

	public static int getCoverageWarnThreshold() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getInt(PREF_KEY_COVERAGE_WARN_THRESHOLD);
	}

	public static int getCoverageErrorThreshold() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getInt(PREF_KEY_COVERAGE_ERROR_THRESHOLD);
	}

	public static int getCoverageRequiredThreshold() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getInt(PREF_KEY_COVERAGE_REQUIRED_THRESHOLD);
	}

	public static boolean getIsCoverageEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_COVERAGE_ENABLED);
	}

	public static boolean getIsCoverageProblemsEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return getIsCoverageEnabled() && SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_COVERAGE_PROBLEMS_ENABLED);
	}

	public static boolean getIsCoverageFileDecorationEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return getIsCoverageEnabled() && SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED);
	}

	public static boolean getIsCoverageProjectDecorationEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return getIsCoverageEnabled() && SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED);
	}

	public static boolean getIsCoverageFilePercentEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return getIsCoverageEnabled() && SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED);
	}

	public static boolean getIsCoverageDisplayEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return getIsCoverageEnabled() && SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_COVERAGE_DISPLAY_ENABLED);
	}

	public static void setIsCoverageDisplayEnabled(boolean enabled) {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		SaUCBuilderActivator.getDefault().getPreferenceStore().setValue(PREF_KEY_COVERAGE_DISPLAY_ENABLED, enabled);
	}

	public static boolean getIsJUnitAutomationEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_JUNIT_AUTOMATION);
	}

	public static List<Pattern> getJUnitExcludeFilter() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		String exFilter = SaUCBuilderActivator.getDefault().getPreferenceStore().getString(PREF_KEY_JUNIT_EXCLUDE_FILTER);

		List<Pattern> patterns = new ArrayList<Pattern>();
		String[] regexs = exFilter.split("\n");
		for (String regex : regexs) {
			regex = regex.trim();
			if (regex.length() > 0) {
				patterns.add(Pattern.compile(regex));
			}
		}
		return patterns;
	}

	public static boolean getIsSourceStubsEnabled() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_STUBS_ALLOW_SOURCE_STUBS);
	}

	public static String getLicenseKey() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getString(PREF_KEY_LICENCE_KEY);
	}

	public static String getLicenseEmail() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getString(PREF_KEY_LICENCE_EMAIL);
	}

	public static boolean isLicenseKeyValid() {

		return true;
		// String key = getLicenseKey();
		// SAKeyValidator validator = new SAKeyValidator();
		// try {
		// validator.validate(key, SaUCBuilderActivator.VERSION_MAJOR, //
		// SaUCBuilderActivator.VERSION_MINOR, getLicenseEmail());
		// } catch (InvalidLicenseKeyException e) {
		// return false;
		// }
		// return true;
	}

	public static LicenseKeyData getLicenseKeyData(String newLicenseKey, String newEmail) throws InvalidLicenseKeyException {

		SAKeyValidator validator = new SAKeyValidator();
		return validator.validate(newLicenseKey, SaUCBuilderActivator.VERSION_MAJOR, //
				SaUCBuilderActivator.VERSION_MINOR, newEmail);
	}

	public static boolean getIsExecConcurrent() {

		IPreferenceStore store = SaUCBuilderActivator.getDefault().getPreferenceStore();
		setDefaults(store);
		return SaUCBuilderActivator.getDefault().getPreferenceStore().getBoolean(PREF_KEY_EXEC_CONCURRENT);
	}

	public static void setDefaults(IPreferenceStore store) {

		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_WARN_THRESHOLD, SaUCPreferences.PREF_DEFAULT_COVERAGE_WARN_THRESHOLD);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_ERROR_THRESHOLD, SaUCPreferences.PREF_DEFAULT_COVERAGE_ERROR_THRESHOLD);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_REQUIRED_THRESHOLD, SaUCPreferences.PREF_DEFAULT_COVERAGE_REQUIRED_THRESHOLD);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_ENABLED, SaUCPreferences.PREF_DEFAULT_COVERAGE_ENABLED);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_PROBLEMS_ENABLED, SaUCPreferences.PREF_DEFAULT_COVERAGE_PROBLEMS_ENABLED);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED, SaUCPreferences.PREF_DEFAULT_COVERAGE_FILE_DECORATION_ENABLED);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED, SaUCPreferences.PREF_DEFAULT_COVERAGE_PROJECT_DECORATION_ENABLED);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED, SaUCPreferences.PREF_DEFAULT_COVERAGE_FILE_PERCENT_ENABLED);
		store.setDefault(SaUCPreferences.PREF_KEY_COVERAGE_DISPLAY_ENABLED, SaUCPreferences.PREF_DEFAULT_COVERAGE_DISPLAY_ENABLED);
		store.setDefault(SaUCPreferences.PREF_KEY_JUNIT_AUTOMATION, SaUCPreferences.PREF_DEFAULT_JUNIT_AUTOMATION);
		store.setDefault(SaUCPreferences.PREF_KEY_JUNIT_EXCLUDE_FILTER, SaUCPreferences.PREF_DEFAULT_JUNIT_EXCLUDE_FILTER);
		// store.setDefault(SaUCPreferences.PREF_KEY_AUTO_JUNIT_USE_SAUC_CONTEXT,
		// SaUCPreferences.PREF_DEFAULT_JUNIT_USE_SAUC_CONTEXT);
		store.setDefault(SaUCPreferences.PREF_KEY_STUBS_ALLOW_SOURCE_STUBS, SaUCPreferences.PREF_DEFAULT_STUBS_ALLOW_SOURCE_STUBS);
		store.setDefault(SaUCPreferences.PREF_KEY_LICENCE_KEY, SaUCPreferences.PREF_DEFAULT_LICENCE_KEY);
		store.setDefault(SaUCPreferences.PREF_KEY_LICENCE_EMAIL, SaUCPreferences.PREF_DEFAULT_LICENCE_EMAIL);
		store.setDefault(SaUCPreferences.PREF_KEY_EXEC_CONCURRENT, SaUCPreferences.PREF_DEFAULT_EXEC_CONCURRENT);
	}

}
