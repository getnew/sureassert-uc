package com.sureassert.uc.transform;

import org.jacoco.core.internal.flow.IMethodProbesVisitor;
import org.jacoco.core.internal.instr.ClassInstrumenter;
import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassVisitor;

import _sauc.SAInterceptor;
import _sauc.SAInterceptor.SaUCAdviceResponse;
import _sauc.SAUCStubbedMethodException;

import com.sureassert.uc.interceptor.SignatureUtils;
import com.sureassert.uc.runtime.IntHolder;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;

public class SaUCClassInstrumenter extends ClassInstrumenter {

	private final String className;

	private String currentMethodName;

	private String[] currentMethodParamTypes;

	public SaUCClassInstrumenter(String className, long id, IExecutionDataAccessorGenerator accessorGenerator, ClassVisitor cv) {

		super(id, accessorGenerator, cv);
		this.className = className;
	}

	@Override
	public IMethodProbesVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		Signature sig = SignatureUtils.getSignature(className, name, desc);
		currentMethodName = name;
		currentMethodParamTypes = sig.getParamClassNames();

		IMethodProbesVisitor jacocoMethodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
		// SaUCMethodInstrumenter saucMethodVisitor = new
		// SaUCMethodInstrumenter(jacocoMethodVisitor, sig);
		return jacocoMethodVisitor;
	}

	public String testMethod1(double arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5, float arg6, long arg7, double arg8, IntHolder[] arg9, short arg10) throws Exception {

		Signature sig = SignatureTableFactory.instance.getSignature(//
				"myClass", "myMethodName", "myParamsList");

		SaUCAdviceResponse response = SAInterceptor.instance.performAdvice(sig, this, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);

		if (!response.doProceed)
			return (String) response.stubRetval;

		System.out.println("Starting method");

		// exec method
		try {
			String myMethodExec = "rest of method";
			return myMethodExec;
		} catch (Throwable myMethodThrowable) {
			throw new RuntimeException(myMethodThrowable);
		}
	}

	public boolean testMethod3(double arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5) throws Exception {

		Signature sig = SignatureTableFactory.instance.getSignature(//
				"myClass", "myMethodName", "myParamsList");

		SaUCAdviceResponse response = SAInterceptor.instance.performAdvice(sig, this);

		if (!response.doProceed)
			return (Boolean) response.stubRetval;

		System.out.println("Starting method");

		// exec method
		try {
			boolean myMethodExec = true;
			return myMethodExec;
		} catch (Throwable myMethodThrowable) {
			throw new RuntimeException(myMethodThrowable);
		}
	}

	public char testMethod4(double arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5) throws Exception {

		Signature sig = SignatureTableFactory.instance.getSignature(//
				"myClass", "myMethodName", "myParamsList");

		SaUCAdviceResponse response = SAInterceptor.instance.performAdvice(sig, this);

		if (!response.doProceed)
			return (Character) response.stubRetval;

		System.out.println("Starting method");

		// exec method
		try {
			char myMethodExec = 'g';
			return myMethodExec;
		} catch (Throwable myMethodThrowable) {
			throw new RuntimeException(myMethodThrowable);
		}
	}

	public static short testMethod7(double arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5) throws Exception {

		Signature sig = SignatureTableFactory.instance.getSignature(//
				"myClass", "myMethodName", "myParamsList");

		SaUCAdviceResponse response = SAInterceptor.instance.performAdvice(sig, null);

		return (Short) response.stubRetval;
	}

	public short testMethod6(double arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5) throws Exception {

		Signature sig = SignatureTableFactory.instance.getSignature(//
				"myClass", "myMethodName", "myParamsList");

		SaUCAdviceResponse response = SAInterceptor.instance.performAdvice(sig, this);

		return (Short) response.stubRetval;
	}

	public byte testMethod5(double arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5) throws Exception {

		Signature sig = SignatureTableFactory.instance.getSignature(//
				"myClass", "myMethodName", "myParamsList");

		SaUCAdviceResponse response = SAInterceptor.instance.performAdvice(sig, this);

		return (Byte) response.stubRetval;
	}

	public String testMethod2(int arg1, String arg2, IntHolder[] arg3, boolean arg4, double arg5) throws Exception {

		try {
			SAInterceptor.instance.performAdvice(SignatureTableFactory.instance.getSignature(//
					"myClass", "myMethodName", "myParamsList"), this);

			// exec method
			try {
				String myMethodExec = "rest of method";
				return myMethodExec;
			} catch (Throwable myMethodThrowable) {
				throw new RuntimeException(myMethodThrowable);
			}
			// insert SAUC catch
		} catch (SAUCStubbedMethodException __$saucStubbed_) {
			return (String) __$saucStubbed_.stubRetval;
		}
	}
}
