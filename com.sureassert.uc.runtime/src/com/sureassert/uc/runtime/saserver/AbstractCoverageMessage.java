package com.sureassert.uc.runtime.saserver;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.sureassert.uc.runtime.ICoverageAware;

public abstract class AbstractCoverageMessage implements SAServerMessage<VoidReturn> {

	private static final long serialVersionUID = 1L;

	protected static final List<ICoverageAware> coverageNotifiers = new ArrayList<ICoverageAware>();

	static {
		IConfigurationElement[] configEls = Platform.getExtensionRegistry().getConfigurationElementsFor(//
				ICoverageAware.COVERAGE_NOTIFIER_EXTENSION_ID);
		try {
			for (IConfigurationElement configEl : configEls) {
				Object execExtension = configEl.createExecutableExtension("class");
				if (execExtension instanceof ICoverageAware) {
					coverageNotifiers.add((ICoverageAware) execExtension);
				}
			}
		} catch (Throwable e) {
			System.err.println(ExceptionUtils.getFullStackTrace(e));
		}
	}

	public Class<VoidReturn> getReturnType() {

		return VoidReturn.class;
	}
}
