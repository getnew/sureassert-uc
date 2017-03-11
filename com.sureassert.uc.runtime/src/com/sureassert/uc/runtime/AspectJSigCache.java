package com.sureassert.uc.runtime;

import java.util.Map;
import java.util.WeakHashMap;

import org.aspectj.lang.reflect.CodeSignature;

public class AspectJSigCache {

	private static final Map<String, Signature> aspectJSigToSASig = new WeakHashMap<String, Signature>();

	public static Signature getSASig(org.aspectj.lang.Signature aspectJSig) {

		String ajSigStr = aspectJSig.toLongString();
		Signature sig = aspectJSigToSASig.get(ajSigStr);
		if (sig == null) {

			synchronized (aspectJSigToSASig) {
				// String ajSigStr = aspectJSig.toLongString();
				if (!(aspectJSig instanceof CodeSignature)) {
					BasicUtils.debug("Unexpected call to getSASig with non-CodeSignature: " + aspectJSig.toString());
					return null;
				}
				CodeSignature ajCodeSig = (CodeSignature) aspectJSig;
				Class<?> clazz = ajCodeSig.getDeclaringType();
				String memberName = ajCodeSig.getName();
				Class<?>[] paramTypes = ajCodeSig.getParameterTypes();
				sig = SignatureTableFactory.instance.getSignature(clazz, memberName, paramTypes);

				aspectJSigToSASig.put(ajSigStr, sig);
			}
		}
		return sig;
	}

	public synchronized static void clear() {

		aspectJSigToSASig.clear();
	}
}
