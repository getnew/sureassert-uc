/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.util.HashSet;
import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;

public class SignatureUtils {

	public static Signature getSignature(IMethodBinding methodBinding, Map<String, String> resolvedTypeByTypeParam) throws JavaModelException {

		String className = methodBinding.getDeclaringClass().getBinaryName();
		if (className == null) {
			// Should never happen, but does for anonymous classes defined under inner classes
			className = "<unknown>";
		}
		String methodName = methodBinding.getName();
		if (methodBinding.isConstructor())
			methodName = Signature.CONSTRUCTOR_METHOD_NAME;
		ITypeBinding[] paramTypes = methodBinding.getParameterTypes();
		String[] paramClassNames = new String[paramTypes.length];
		int paramClassNameIdxOffset = 0;

		// NOTE: Enum constructors include 2 implicit params (String, int)
		// if (methodBinding.getJavaElement() != null) {
		// if (((IMethod) methodBinding.getJavaElement()).getDeclaringType().isEnum()) {
		// paramClassNames = new String[paramTypes.length + 2];
		// paramClassNames[0] = String.class.getName();
		// paramClassNames[1] = "int";
		// paramClassNameIdxOffset = 2;
		// }
		// }

		for (int paramIdx = 0; paramIdx < paramTypes.length; paramIdx++) {
			ITypeBinding erasureType = paramTypes[paramIdx].getErasure();
			if (erasureType.isPrimitive()) {
				paramClassNames[paramIdx + paramClassNameIdxOffset] = erasureType.getQualifiedName();
			} else {
				paramClassNames[paramIdx + paramClassNameIdxOffset] = erasureType.getBinaryName();
			}
			if (paramTypes[paramIdx].getName().indexOf("<") == -1 && paramTypes[paramIdx] != erasureType)
				resolvedTypeByTypeParam.put(paramTypes[paramIdx].getName(), paramClassNames[paramIdx + paramClassNameIdxOffset]);
		}
		ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
		for (int i = 0; i < exceptionTypes.length; i++) {
			ITypeBinding erasureType = exceptionTypes[i].getErasure();
			String excClassName;
			if (erasureType.isPrimitive()) {
				excClassName = erasureType.getQualifiedName();
			} else {
				excClassName = erasureType.getBinaryName();
			}
			if (exceptionTypes[i].getName().indexOf("<") == -1 && exceptionTypes[i] != erasureType)
				resolvedTypeByTypeParam.put(exceptionTypes[i].getName(), excClassName);
		}
		ITypeBinding returnType = methodBinding.getReturnType();
		if (returnType.getName().indexOf("<") == -1) {
			ITypeBinding erasureType = returnType.getErasure();
			String returnTypeSrc;
			if (erasureType.isPrimitive()) {
				returnTypeSrc = erasureType.getQualifiedName();
			} else {
				returnTypeSrc = erasureType.getBinaryName();
			}
			if (returnType != erasureType)
				resolvedTypeByTypeParam.put(returnType.getName(), returnTypeSrc);
		}
		return SignatureTableFactory.instance.getSignature(className, methodName, paramClassNames);
	}

	public static Signature getSignature(IMethod method, Map<String, String> resolvedTypeByTypeParam) throws JavaModelException {

		String className = method.getDeclaringType().getFullyQualifiedName();
		String methodName = method.getElementName();
		if (method.isConstructor())
			methodName = Signature.CONSTRUCTOR_METHOD_NAME;
		String[] paramTypes = method.getParameterTypes();
		String[] paramClassNames = new String[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			paramClassNames[i] = BasicUtils.getTypeNameForSrc(paramTypes[i], resolvedTypeByTypeParam, new HashSet<String>());
		}
		return SignatureTableFactory.instance.getSignature(className, methodName, paramClassNames);
	}

	public static Signature getSignature(IMethod method, Map<String, String> resolvedTypeByTypeParam, //
			String overrideClassName) throws JavaModelException {

		Signature sig = getSignature(method, resolvedTypeByTypeParam);
		return SignatureTableFactory.instance.getSignature(overrideClassName, sig.getMemberName(), sig.getParamClassNames());
	}

	public static Signature getSignature(String className, String methodName, String asmSig) {

		String[] paramTypes = org.eclipse.jdt.core.Signature.getParameterTypes(asmSig);
		String[] paramClassNames = new String[paramTypes.length];
		int paramIdx = 0;
		for (String paramType : paramTypes) {
			String paramClassName = org.eclipse.jdt.core.Signature.getSignatureSimpleName(paramType);
			paramClassNames[paramIdx] = BasicUtils.getClassNameFromBinaryClassName(paramClassName);
			paramIdx++;
		}
		return SignatureTableFactory.instance.getSignature(className, methodName, paramClassNames);
	}
}
