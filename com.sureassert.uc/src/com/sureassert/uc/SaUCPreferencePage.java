/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.sureassert.uc.internal.SaUCPreferences;

/**
 * PreferencePage implementation for Sureassert UC
 */
public class SaUCPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, SelectionListener, ModifyListener {

	private Button runJunitAutomaticallyCheckbox;
	private Text junitExcludeFilterText;
	private Button calculateCoverageCheckbox;
	private Button coverageProblemsCheckbox;
	private Button coverageProjectDecorationCheckbox;
	private Button coverageFileDecorationCheckbox;
	private Button coverageFilePercentCheckbox;
	private Button coverageDisplayEnabledCheckbox;
	private Spinner coverageWarnThreshold;
	private Spinner coverageErrorThreshold;
	// private Spinner coverageRequiredThreshold;
	private Button sourceStubsEnabledCheckbox;

	// private Button execConcurrentCheckbox;

	// private Text licenceKeyText;
	// private Text licenceEmailText;
	// private Button licenseApplyButton;
	// private Button licenseStatusButton;
	// private LicenseWaitRunner licenseWaitRunner;

	/**
	 * Creates an new checkbox instance and sets the default
	 * layout data.
	 * 
	 * @param group the composite in which to create the checkbox
	 * @param label the string to set into the checkbox
	 * @return the new checkbox
	 */
	private Button createCheckBox(Composite group, String label, int horizSpan) {

		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		button.addSelectionListener(this);
		GridData data = new GridData();
		data.horizontalSpan = horizSpan;
		button.setLayoutData(data);
		return button;
	}

	private Composite createComposite(Composite parent, int numColumns) {

		Composite composite = new Composite(parent, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	private Composite createGroup(Composite parent, int numColumns, String title) {

		Group composite = new Group(parent, SWT.NULL);
		composite.setText(title);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	/**
	 * (non-Javadoc)
	 * Method declared on PreferencePage
	 */
	@Override
	protected Control createContents(final Composite parent) {

		try {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, SaUCPreferences.PREFERENCE_PAGE_CONTEXT);

			// createLabel(null, parent, "Sureassert UC Preferences");
			createComposite(parent, 3);

			// -----------------------------------------------------------------------------------
			Composite groupCoverage = createGroup(parent, 1, "Code Coverage Reporting");
			Composite compositeCodeCoverage = createComposite(groupCoverage, 3);
			calculateCoverageCheckbox = createCheckBox(compositeCodeCoverage, " Calculate code coverage", 3);
			coverageDisplayEnabledCheckbox = createCheckBox(compositeCodeCoverage, " Display coverage graphically in Java editor", 3);
			coverageProjectDecorationCheckbox = createCheckBox(compositeCodeCoverage, " Report project-level coverage (delay for large projects)", 3);
			coverageFileDecorationCheckbox = createCheckBox(compositeCodeCoverage, " Report file-level coverage via icon decorations", 3);
			coverageFilePercentCheckbox = createCheckBox(compositeCodeCoverage, " Report file-level coverage % alongside Java files", 3);
			coverageProblemsCheckbox = createCheckBox(compositeCodeCoverage, " Report file-level coverage as problems", 3);

			// createLabel(null, compositeCodeCoverage,
			// "Only require coverage for methods containing over ");
			// coverageRequiredThreshold = new Spinner(compositeCodeCoverage, SWT.BORDER);
			// coverageRequiredThreshold.setMinimum(0);
			// coverageRequiredThreshold.setMaximum(100);
			// coverageRequiredThreshold.setIncrement(1);
			// coverageRequiredThreshold.setPageIncrement(10);
			// createLabel(null, compositeCodeCoverage, "statement(s)");

			createLabel(null, compositeCodeCoverage, "Mark as warning/amber for coverage under ");
			coverageWarnThreshold = new Spinner(compositeCodeCoverage, SWT.BORDER);
			coverageWarnThreshold.setMinimum(0);
			coverageWarnThreshold.setMaximum(100);
			coverageWarnThreshold.setIncrement(1);
			coverageWarnThreshold.setPageIncrement(10);
			createLabel(null, compositeCodeCoverage, "%");

			createLabel(null, compositeCodeCoverage, "Mark as error/red for coverage under ");
			coverageErrorThreshold = new Spinner(compositeCodeCoverage, SWT.BORDER);
			coverageErrorThreshold.setMinimum(0);
			coverageErrorThreshold.setMaximum(100);
			coverageErrorThreshold.setIncrement(1);
			coverageErrorThreshold.setPageIncrement(10);
			createLabel(null, compositeCodeCoverage, "%");

			// -----------------------------------------------------------------------------------

			createComposite(parent, 3);
			Composite groupJUnit = createGroup(parent, 2, "JUnit Integration");
			// tabForward(groupJUnit);
			Composite compositeJUnit = createComposite(groupJUnit, 2);
			tabForward(groupJUnit);
			runJunitAutomaticallyCheckbox = createCheckBox(compositeJUnit, " Run JUnit tests automatically", 2);

			createLabel(null, groupJUnit, " Exclude JUnit classes with name matching regex(s):");
			tabForward(groupJUnit);
			junitExcludeFilterText = new Text(groupJUnit, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
			gridData.widthHint = 300;
			gridData.heightHint = 50;
			gridData.horizontalSpan = 2;
			junitExcludeFilterText.setLayoutData(gridData);

			// -----------------------------------------------------------------------------------

			createComposite(parent, 3);
			Composite groupStubs = createGroup(parent, 2, "Stubbing");
			tabForward(groupStubs);
			Composite compositeStubs = createComposite(groupStubs, 1);
			sourceStubsEnabledCheckbox = createCheckBox(compositeStubs, " Allow source stubs", 2);

			// -----------------------------------------------------------------------------------

			// createComposite(parent, 3);
			// Composite groupExec = createGroup(parent, 2, "Execution");
			// tabForward(groupExec);
			// Composite compositeExec = createComposite(groupExec, 1);
			// execConcurrentCheckbox = createCheckBox(compositeExec,
			// " Enable background UC Engine server", 2);

			// -----------------------------------------------------------------------------------
			/*
			 * createComposite(parent, 3);
			 * Composite groupLicense = createGroup(parent, 1, "License");
			 * Composite compositeLicense = createComposite(groupLicense, 3);
			 * createLabel(null, compositeLicense, "License Key ");
			 * licenceKeyText = new Text(compositeLicense, SWT.BORDER);
			 * GridData gridData = new GridData();
			 * gridData.widthHint = 200;
			 * licenceKeyText.setLayoutData(gridData);
			 * licenseApplyButton = createButton(null, compositeLicense, " Apply ", SWT.CENTER);
			 * licenceKeyText.addKeyListener(new LicenseFieldKeyListener(licenseApplyButton));
			 * licenseApplyButton.setEnabled(false);
			 * licenseApplyButton.addSelectionListener(new SelectionListener() {
			 * 
			 * public void widgetSelected(SelectionEvent event) {
			 * 
			 * try {
			 * licenseApplyButton.setEnabled(false);
			 * IPreferenceStore store = getPreferenceStore();
			 * store.setValue(SaUCPreferences.PREF_KEY_LICENCE_KEY, licenceKeyText.getText());
			 * store.setValue(SaUCPreferences.PREF_KEY_LICENCE_EMAIL, licenceEmailText.getText());
			 * displayLicenseDetails(parent.getDisplay());
			 * } catch (IOException e) {
			 * throw new RuntimeException(e);
			 * }
			 * }
			 * 
			 * public void widgetDefaultSelected(SelectionEvent event) {
			 * 
			 * }
			 * });
			 * 
			 * createLabel(null, compositeLicense, "Licensed Email Address ");
			 * licenceEmailText = new Text(compositeLicense, SWT.BORDER);
			 * licenceEmailText.setLayoutData(gridData);
			 * licenceEmailText.addKeyListener(new LicenseFieldKeyListener(licenseApplyButton));
			 * licenseStatusButton = createButton(null, compositeLicense, "", SWT.CENTER);
			 * licenseStatusButton.setImage(getLicenseValidityImage(parent.getDisplay()));
			 * licenseStatusButton.addSelectionListener(new SelectionListener() {
			 * 
			 * public void widgetSelected(SelectionEvent event) {
			 * 
			 * try {
			 * displayLicenseDetails(parent.getDisplay());
			 * } catch (IOException e) {
			 * throw new RuntimeException(e);
			 * }
			 * }
			 * 
			 * public void widgetDefaultSelected(SelectionEvent event) {
			 * 
			 * }
			 * });
			 */
			initializeValues();

			return createComposite(parent, 3);
		} catch (Throwable e) {
			EclipseUtils.reportError(e);
			throw new RuntimeException(e);
		}
	}

	/*
	 * private class LicenseFieldKeyListener implements KeyListener {
	 * 
	 * private final Button applyLicButton;
	 * 
	 * private LicenseFieldKeyListener(Button applyLicButton) {
	 * 
	 * this.applyLicButton = applyLicButton;
	 * }
	 * 
	 * public void keyReleased(KeyEvent event) {
	 * 
	 * IPreferenceStore store = getPreferenceStore();
	 * if (licenceKeyText.getText().equals(store.getString(SaUCPreferences.PREF_KEY_LICENCE_KEY)) &&
	 * //
	 * licenceEmailText.getText().equals(store.getString(SaUCPreferences.PREF_KEY_LICENCE_EMAIL))) {
	 * applyLicButton.setEnabled(false);
	 * } else {
	 * if (licenseWaitRunner == null || !licenseWaitRunner.isAlive())
	 * applyLicButton.setEnabled(true);
	 * }
	 * }
	 * 
	 * public void keyPressed(KeyEvent event) {
	 * 
	 * }
	 * }
	 * 
	 * private void displayLicenseDetails(Display display) throws IOException {
	 * 
	 * try {
	 * DateFormat df = DateFormat.getDateInstance();
	 * LicenseKeyData lData = SaUCPreferences.getLicenseKeyData(licenceKeyText.getText(), //
	 * licenceEmailText.getText());
	 * licenseStatusButton.setImage(getLicenseValidityImage(display, true));
	 * EclipseUtils.displayDialog("Sureassert UC License information", //
	 * "License type: " + lData.getLicenseTypeName() + "\n" + //
	 * "Licensed version: " + lData.getLicensedVersionStr() + "\n" + //
	 * "Purchase date: " + df.format(lData.createdDate) + "\n", //
	 * false, IStatus.INFO);
	 * 
	 * } catch (InvalidLicenseKeyException e) {
	 * 
	 * licenseStatusButton.setImage(getLicenseValidityImage(display, false));
	 * EclipseUtils.displayDialog("Invalid license key", //
	 * "Validation of the license key entered failed with the message: \n\n" + //
	 * e.getMessage(), false, IStatus.ERROR);
	 * licenseApplyButton.setEnabled(false);
	 * licenseStatusButton.setEnabled(false);
	 * licenseWaitRunner = new LicenseWaitRunner(display, licenseApplyButton, licenseStatusButton);
	 * licenseWaitRunner.start();
	 * }
	 * 
	 * }
	 * 
	 * private Image getLicenseValidityImage(Display display) throws IOException {
	 * 
	 * return SaUCPreferences.isLicenseKeyValid() ? //
	 * getImage(display, "icons/info.gif") : getImage(display, "icons/error.gif");
	 * }
	 * 
	 * private Image getLicenseValidityImage(Display display, boolean isValid) throws IOException {
	 * 
	 * return isValid ? //
	 * getImage(display, "icons/info.gif") : getImage(display, "icons/error.gif");
	 * }
	 * 
	 * private Image getImage(Display display, String filePath) throws IOException {
	 * 
	 * Bundle bundle = Platform.getBundle(SaUCBuilderActivator.PLUGIN_ID);
	 * Path path = new Path(filePath);
	 * URL imageURL = FileLocator.find(bundle, path, null);
	 * imageURL = Platform.resolve(imageURL);
	 * return new Image(display, imageURL.openStream());
	 * }
	 */
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 * 
	 * @param parent the parent for the new label
	 * @param text the text for the new label
	 * @return the new label
	 */
	private Label createLabel(GridData gridData, Composite parent, String text, int style) {

		Label label = new Label(parent, style);
		label.setText(text);
		if (gridData == null) {
			gridData = new GridData();
			gridData.horizontalSpan = 1;
			gridData.horizontalAlignment = GridData.FILL;
		}
		label.setLayoutData(gridData);
		return label;
	}

	private Label createLabel(GridData gridData, Composite parent, String text) {

		return createLabel(gridData, parent, text, SWT.LEFT);
	}

	private Button createButton(GridData gridData, Composite parent, String text, int style) {

		Button button = new Button(parent, style);
		button.setText(text);
		if (gridData == null) {
			gridData = new GridData();
			gridData.horizontalSpan = 1;
			gridData.horizontalAlignment = GridData.FILL;
		}
		button.setLayoutData(gridData);
		return button;
	}

	/**
	 * The <code>ReadmePreferencePage implementation of this
	 * <code>PreferencePage method 
	 * returns preference store that belongs to the our plugin.
	 * This is important because we want to store
	 * our preferences separately from the workbench.
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {

		return SaUCBuilderActivator.getDefault().getPreferenceStore();
	}

	/*
	 * (non-Javadoc)
	 * Method declared on IWorkbenchPreferencePage
	 */
	public void init(IWorkbench workbench) {

		// do nothing
	}

	/**
	 * Initializes states of the controls using default values
	 * in the preference store.
	 */
	private void initializeDefaults() {

		IPreferenceStore store = getPreferenceStore();
		SaUCPreferences.setDefaults(store);
		runJunitAutomaticallyCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_JUNIT_AUTOMATION));
		junitExcludeFilterText.setText(store.getDefaultString(SaUCPreferences.PREF_KEY_JUNIT_EXCLUDE_FILTER));
		calculateCoverageCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_COVERAGE_ENABLED));
		coverageProblemsCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_COVERAGE_PROBLEMS_ENABLED));
		coverageProjectDecorationCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED));
		coverageFileDecorationCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED));
		coverageFilePercentCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED));
		coverageDisplayEnabledCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_COVERAGE_DISPLAY_ENABLED));
		coverageWarnThreshold.setSelection(store.getDefaultInt(SaUCPreferences.PREF_KEY_COVERAGE_WARN_THRESHOLD));
		coverageErrorThreshold.setSelection(store.getDefaultInt(SaUCPreferences.PREF_KEY_COVERAGE_ERROR_THRESHOLD));
		// coverageRequiredThreshold.setSelection(store.getDefaultInt(SaUCPreferences.PREF_KEY_COVERAGE_REQUIRED_THRESHOLD));
		sourceStubsEnabledCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_STUBS_ALLOW_SOURCE_STUBS));
		// licenceKeyText.setText(store.getDefaultString(SaUCPreferences.PREF_KEY_LICENCE_KEY));
		// licenceEmailText.setText(store.getDefaultString(SaUCPreferences.PREF_KEY_LICENCE_EMAIL));
		// execConcurrentCheckbox.setSelection(store.getDefaultBoolean(SaUCPreferences.PREF_KEY_EXEC_CONCURRENT));
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {

		IPreferenceStore store = getPreferenceStore();
		SaUCPreferences.setDefaults(store);
		runJunitAutomaticallyCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_JUNIT_AUTOMATION));
		junitExcludeFilterText.setText(store.getString(SaUCPreferences.PREF_KEY_JUNIT_EXCLUDE_FILTER));
		calculateCoverageCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_COVERAGE_ENABLED));
		coverageProblemsCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_COVERAGE_PROBLEMS_ENABLED));
		coverageProjectDecorationCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED));
		coverageFileDecorationCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED));
		coverageFilePercentCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED));
		coverageDisplayEnabledCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_COVERAGE_DISPLAY_ENABLED));
		coverageWarnThreshold.setSelection(store.getInt(SaUCPreferences.PREF_KEY_COVERAGE_WARN_THRESHOLD));
		coverageErrorThreshold.setSelection(store.getInt(SaUCPreferences.PREF_KEY_COVERAGE_ERROR_THRESHOLD));
		// coverageRequiredThreshold.setSelection(store.getInt(SaUCPreferences.PREF_KEY_COVERAGE_REQUIRED_THRESHOLD));
		sourceStubsEnabledCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_STUBS_ALLOW_SOURCE_STUBS));
		// licenceKeyText.setText(store.getString(SaUCPreferences.PREF_KEY_LICENCE_KEY));
		// licenceEmailText.setText(store.getString(SaUCPreferences.PREF_KEY_LICENCE_EMAIL));
		// execConcurrentCheckbox.setSelection(store.getBoolean(SaUCPreferences.PREF_KEY_EXEC_CONCURRENT));

		rationalizeControls();
	}

	/**
	 * (non-Javadoc)
	 * Method declared on ModifyListener
	 */
	public void modifyText(ModifyEvent event) {

		event.toString();
	}

	/*
	 * (non-Javadoc)
	 * Method declared on PreferencePage
	 */
	@Override
	protected void performDefaults() {

		super.performDefaults();
		initializeDefaults();
	}

	/*
	 * (non-Javadoc)
	 * Method declared on PreferencePage
	 */
	@SuppressWarnings("deprecation")
	@Override
	public boolean performOk() {

		storeValues();
		SaUCBuilderActivator.getDefault().savePluginPreferences();
		return true;
	}

	/**
	 * Stores the values of the controls back to the preference store.
	 */
	private void storeValues() {

		IPreferenceStore store = getPreferenceStore();
		store.setValue(SaUCPreferences.PREF_KEY_JUNIT_AUTOMATION, runJunitAutomaticallyCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_JUNIT_EXCLUDE_FILTER, junitExcludeFilterText.getText());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_ENABLED, calculateCoverageCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_PROBLEMS_ENABLED, coverageProblemsCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_PROJECT_DECORATION_ENABLED, coverageProjectDecorationCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_FILE_DECORATION_ENABLED, coverageFileDecorationCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_FILE_PERCENT_ENABLED, coverageFilePercentCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_DISPLAY_ENABLED, coverageDisplayEnabledCheckbox.getSelection());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_WARN_THRESHOLD, coverageWarnThreshold.getText());
		store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_ERROR_THRESHOLD, coverageErrorThreshold.getText());
		// store.setValue(SaUCPreferences.PREF_KEY_COVERAGE_REQUIRED_THRESHOLD,
		// coverageRequiredThreshold.getText());
		store.setValue(SaUCPreferences.PREF_KEY_STUBS_ALLOW_SOURCE_STUBS, sourceStubsEnabledCheckbox.getSelection());
		// store.setValue(SaUCPreferences.PREF_KEY_LICENCE_KEY, licenceKeyText.getText());
		// store.setValue(SaUCPreferences.PREF_KEY_LICENCE_EMAIL, licenceEmailText.getText());
		// store.setValue(SaUCPreferences.PREF_KEY_EXEC_CONCURRENT,
		// execConcurrentCheckbox.getSelection());
	}

	/**
	 * Creates a tab of one horizontal spans.
	 * 
	 * @param parent the parent in which the tab should be created
	 */
	private void tabForward(Composite parent) {

		Label vfiller = new Label(parent, SWT.LEFT);
		GridData gridData = new GridData();
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.BEGINNING;
		gridData.grabExcessHorizontalSpace = false;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.grabExcessVerticalSpace = false;
		vfiller.setLayoutData(gridData);
	}

	/**
	 * (non-Javadoc)
	 * Method declared on SelectionListener
	 */
	public void widgetDefaultSelected(SelectionEvent event) {

		// Handle a default selection. Do nothing in this example
	}

	/**
	 * (non-Javadoc)
	 * Method declared on SelectionListener
	 */
	public void widgetSelected(SelectionEvent event) {

		rationalizeControls();
	}

	private void rationalizeControls() {

		if (calculateCoverageCheckbox.getSelection()) {
			coverageDisplayEnabledCheckbox.setEnabled(true);
			coverageProblemsCheckbox.setEnabled(true);
			coverageProjectDecorationCheckbox.setEnabled(true);
			coverageFileDecorationCheckbox.setEnabled(true);
			coverageFilePercentCheckbox.setEnabled(true);
			coverageErrorThreshold.setEnabled(true);
			coverageWarnThreshold.setEnabled(true);
			// coverageRequiredThreshold.setEnabled(true);
		} else {
			coverageDisplayEnabledCheckbox.setEnabled(false);
			coverageProblemsCheckbox.setEnabled(false);
			coverageProjectDecorationCheckbox.setEnabled(false);
			coverageFileDecorationCheckbox.setEnabled(false);
			coverageFilePercentCheckbox.setEnabled(false);
			coverageErrorThreshold.setEnabled(false);
			coverageWarnThreshold.setEnabled(false);
			// coverageRequiredThreshold.setEnabled(false);
		}
		if (!coverageProblemsCheckbox.getSelection() && !coverageFileDecorationCheckbox.getSelection() && //
				!coverageProjectDecorationCheckbox.getSelection()) {
			coverageErrorThreshold.setEnabled(false);
			coverageWarnThreshold.setEnabled(false);
			// coverageRequiredThreshold.setEnabled(false);
		}
		junitExcludeFilterText.setEnabled(runJunitAutomaticallyCheckbox.getSelection());
	}
}
