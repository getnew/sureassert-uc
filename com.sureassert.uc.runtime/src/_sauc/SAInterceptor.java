/* Copyright 2011 Nathan Dolan. All rights reserved. */

package _sauc;

import org.aspectj.lang.reflect.CodeSignature;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.MethodStub;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.SourceStub;
import com.sureassert.uc.runtime.TypeConverter;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.exception.SAUCBuildInterruptedError;

public class SAInterceptor {

	// NOTE: runtime reflection dependency from ProjectClassLoaders before refactoring
	public static final SAInterceptor instance = new SAInterceptor();

	// private final List<ICoverageAware> coverageNotifiers;

	private PersistentDataFactory pdf;

	private SAInterceptor() {

		// coverageNotifiers = new ArrayList<ICoverageAware>();
	}

	// invoked via reflection from ProjectClassLoaders (for cl separation)
	public void init() {

		pdf = PersistentDataFactory.getInstance();
		// coverageNotifiers.clear();
		// IConfigurationElement[] configEls =
		// Platform.getExtensionRegistry().getConfigurationElementsFor(//
		// ICoverageAware.COVERAGE_NOTIFIER_EXTENSION_ID);
		// try {
		// for (IConfigurationElement configEl : configEls) {
		// Object execExtension = configEl.createExecutableExtension("class");
		// if (execExtension instanceof ICoverageAware) {
		// coverageNotifiers.add((ICoverageAware) execExtension);
		// }
		// }
		// } catch (Throwable e) {
		// System.err.println(ExceptionUtils.getFullStackTrace(e));
		// }
	}

	// invoked via reflection from ProjectClassLoaders (for cl separation)
	public void dispose() {

		// coverageNotifiers.clear();
	}

	public void registerMethodStart(Signature called, Object invokedObj, Object[] args) {

		Signature currentUCSig = pdf.getCurrentUseCaseSignature();
		if (currentUCSig != null) {
			// Register runtime dependency
			pdf.registerRuntimeDependency(called, pdf.getCurrentUseCaseSignature(), false);

			// Check for verify statement on this method
			pdf.verifyMethodCall(called, invokedObj, args);
		}
		interruptedCheck();
	}

	public void interruptedCheck(int lineNum, String className) {

		codeCovered(lineNum, className);
		interruptedCheck();
	}

	public void interruptedCheck() {

		if (!pdf.isStandaloneBuild() && pdf.isBuildInterrupted())
			throw new SAUCBuildInterruptedError();
	}

	public void codeCovered(final int lineNum, final String className) {

		// for (final ICoverageAware coverageNotifier : coverageNotifiers) {
		// try {
		// coverageNotifier.notifyCodeCovered(lineNum, className);
		// } catch (Throwable e) {
		// System.err.println(ExceptionUtils.getFullStackTrace(e));
		// }
		// }
	}

	public boolean codeCoveredTrue(final int lineNum, final String className) {

		codeCovered(lineNum, className);
		return true;
	}

	public void stubExecuted(final int lineNum, final int endLineNum, final String className) {

		// for (final ICoverageAware coverageNotifier : coverageNotifiers) {
		// try {
		// coverageNotifier.notifyStubExecuted(lineNum, endLineNum, className);
		// } catch (Throwable e) {
		// System.err.println(ExceptionUtils.getFullStackTrace(e));
		// }
		// }
	}

	public boolean isStubbed(Signature called, Object invokedObj, Object[] paramVals) {

		return pdf.getMethodStub(called, invokedObj, paramVals) != null;
	}

	public boolean isStubbed(Signature called) {

		return isStubbed(called, null, new Object[] {});
	}

	public boolean isStubbed(String sourceStubVarName) {

		return pdf.getSourceStub(sourceStubVarName) != null;
	}

	public Object execStub(Signature called) {

		return execStub(called, null, new Object[] {});
	}

	public Object execStub(Signature called, Object invokedObj, Object[] paramVals) {

		MethodStub methodStub;
		Object retval;
		NamedInstanceFactory niFactory = NamedInstanceFactory.getInstance();
		try {
			ClassLoader classLoader = pdf.getCurrentProjectClassLoader();
			methodStub = pdf.getMethodStub(called, invokedObj, paramVals, classLoader);
			SINType invokeExpr = methodStub.getNextInvokeExpression();
			TypeConverter<?> tc = TypeConverterFactory.instance.getTypeConverterForValue(invokeExpr);

			// Set named instances
			niFactory.addNamedInstance(NamedInstanceFactory.VERIFY_OBJ_INSTANCE_NAME, invokedObj, null);
			for (int i = 1; i <= paramVals.length; i++) {
				niFactory.addNamedInstance(NamedInstanceFactory.VERIFY_ARG_INSTANCE_NAME + i, paramVals[i - 1], null);
			}

			retval = tc.toInstance(invokeExpr, classLoader);

		} catch (SARuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SARuntimeException(e);
		} finally {
			// Clear named instances
			niFactory.removeNamedInstance(NamedInstanceFactory.VERIFY_OBJ_INSTANCE_NAME);
			for (int i = 1; i <= paramVals.length; i++) {
				niFactory.removeNamedInstance(NamedInstanceFactory.VERIFY_ARG_INSTANCE_NAME + i);
			}
		}
		if (methodStub != null && methodStub.getStubThrowsException()) {
			if (retval == null) {
				throw new SARuntimeException("Method stub uses ^= notation but doesn't return a Throwable; instead returns null.");
			} else if (retval instanceof RuntimeException) {
				throw (RuntimeException) retval;
			} else if (retval instanceof Exception) {
				throw new SAUCWrapperException((Throwable) retval);
			} else {
				throw new SARuntimeException("Method stub uses ^= notation but doesn't return a Throwable; " + //
						"instead returned Object of type " + retval.getClass().getName());
			}
		}

		return retval;
	}

	public Object execStub(String sourceStubVarName) {

		try {
			ClassLoader classLoader = pdf.getCurrentProjectClassLoader();
			SourceStub sourceStub = pdf.getSourceStub(sourceStubVarName, classLoader);
			if (sourceStub == null) {
				if (pdf.getCurrentJUnitClass() != null) {
					throw new SARuntimeException("No source stub defined named \"" + sourceStubVarName + //
							"\" by JUnit " + pdf.getCurrentJUnitClass() + "." + pdf.getCurrentJUnitMethod());
				} else {
					throw new SARuntimeException("No source stub defined named \"" + sourceStubVarName + //
							"\" by " + BasicUtils.getCurrentUseCaseDisplayName());
				}
			}
			SINType invokeExpr = sourceStub.getNextInvokeExpression();
			TypeConverter<?> tc = TypeConverterFactory.instance.getTypeConverterForValue(invokeExpr);
			return tc.toInstance(invokeExpr, classLoader);
		} catch (SARuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SARuntimeException(e);
		}
	}

	/**
	 * 
	 * @param joinPoint
	 * @param instance
	 * @param stubRetval
	 * @return True if proceed() should be called
	 * @throws Exception
	 */
	public SaUCAdviceResponse performAdvice(Signature sig, Object instance, Object... args) throws Exception {

		// Check if the thread has been interrupted
		interruptedCheck();

		// Ensure join point is known
		if (sig == null)
			return SaUCAdviceResponse.PROCEED_RESPONSE;

		// Register the invocation of the method
		registerMethodStart(sig, instance, args);

		// Check for method stubs
		if (isStubbed(sig, instance, args)) {

			try {
				return new SaUCAdviceResponse(false, execStub(sig, instance, args));
			} catch (SAUCWrapperException e) {
				// Stub threw exception
				if (e.getCause() instanceof Exception)
					throw (Exception) e.getCause();
				else
					throw (Error) e.getCause();
			}
		} else {
			return SaUCAdviceResponse.PROCEED_RESPONSE;
		}
	}

	public static class SaUCAdviceResponse {

		public static SaUCAdviceResponse PROCEED_RESPONSE = new SaUCAdviceResponse(true);

		/** Whether to proceed with executing the advised method */
		public final boolean doProceed;

		/** The value to return from the method where !doProceed */
		public final Object stubRetval;

		public SaUCAdviceResponse(boolean doProceed) {

			this.doProceed = doProceed;
			this.stubRetval = null;
		}

		public SaUCAdviceResponse(boolean doProceed, Object stubRetval) {

			this.doProceed = doProceed;
			this.stubRetval = stubRetval;
		}
	}

	private Signature getSASig(org.aspectj.lang.Signature ajSig) {

		CodeSignature ajCodeSig = ((CodeSignature) ajSig);
		Class<?> clazz = ajCodeSig.getDeclaringType();
		String memberName = ajCodeSig.getName();
		Class<?>[] paramTypes = ajCodeSig.getParameterTypes();
		return SignatureTableFactory.instance.getSignature(clazz, memberName, paramTypes);
	}
}
