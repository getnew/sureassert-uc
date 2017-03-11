/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.sureassert.uc.runtime.internal.SINExpression;

public class SINExpressionFactory {

	/** SINExpression cache by rawExpression */
	private static WeakHashMap<String, WeakReference<SINExpression>> cachedSINExpByRaw = //
	new WeakHashMap<String, WeakReference<SINExpression>>();

	/**
	 * Creates a SINExpression by parsing the given SIN expression string.
	 * 
	 * @param rawSINExpression e.g. instanceName.methodName(i:1, s:'my second param')
	 * @throws TypeConverterException
	 */
	public static ISINExpression get(String rawSINExpression) {

		// Check cache
		ISINExpression sinExp = getCachedSINExp(rawSINExpression);
		if (sinExp != null)
			return sinExp;
		else
			return SINExpression.createNew(rawSINExpression);
	}

	private static ISINExpression getCachedSINExp(String rawSIN) {

		WeakReference<SINExpression> cachedSINExpRef = cachedSINExpByRaw.get(rawSIN);
		if (cachedSINExpRef != null) {
			ISINExpression cachedSINExp = cachedSINExpRef.get();
			if (cachedSINExp != null)
				return cachedSINExp;
		}
		return null;
	}

	public static void addCachedSINExp(String rawExp, WeakReference<SINExpression> expRef) {

		cachedSINExpByRaw.put(rawExp, expRef);
	}
}
