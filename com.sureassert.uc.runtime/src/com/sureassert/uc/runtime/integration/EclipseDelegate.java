package com.sureassert.uc.runtime.integration;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.sureassert.uc.runtime.IUCReexecutor;

public class EclipseDelegate {

	private static final IUCReexecutor tempReexecutor = new IUCReexecutor() {

		public boolean reExecuteUseCase(String useCaseName, String reexecName) {

			return false;
		}
	};

	private static boolean isStandalone = false;

	public static final void setIsStandalone() {

		isStandalone = true;
	}

	public static boolean isStandalone() {

		return isStandalone;
	}

	public static IUCReexecutor getUCReexecutor() {

		if (isStandalone)
			return tempReexecutor;

		IConfigurationElement[] configEls = Platform.getExtensionRegistry().getConfigurationElementsFor(//
				IUCReexecutor.UC_REEXECUTOR_EXTENSION_ID);

		try {
			Object execExtension = configEls[0].createExecutableExtension("class");
			if (execExtension instanceof IUCReexecutor) {
				return (IUCReexecutor) execExtension;
			} else {
				throw new RuntimeException(IUCReexecutor.UC_REEXECUTOR_EXTENSION_ID + " did not return an IUCReexecutor");
			}
		} catch (Throwable e) {
			throw new RuntimeException(ExceptionUtils.getFullStackTrace(e));
		}
	}
}
