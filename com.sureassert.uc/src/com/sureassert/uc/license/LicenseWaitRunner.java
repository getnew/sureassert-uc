package com.sureassert.uc.license;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;

public class LicenseWaitRunner extends Thread implements Runnable {

	private final Display display;
	private final Button applyButton;
	private final Button infoButton;

	public LicenseWaitRunner(Display display, Button applyButton, Button infoButton) {

		this.display = display;
		this.applyButton = applyButton;
		this.infoButton = infoButton;
	}

	@Override
	public void run() {

		try {
			final StringBuilder text = new StringBuilder();
			display.syncExec(new Runnable() {

				public void run() {

					text.append(applyButton.getText());
				}
			});

			for (int i = 10; i > 0; i--) {

				final int wait = i;
				display.syncExec(new Runnable() {

					public void run() {

						applyButton.setText("Wait " + wait);
					}
				});

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

			display.syncExec(new Runnable() {

				public void run() {

					applyButton.setText(text.toString());
					applyButton.setEnabled(true);
					infoButton.setEnabled(true);
				}
			});
		} catch (Throwable e) {
			System.err.println(ExceptionUtils.getFullStackTrace(e));
		}
	}
}
