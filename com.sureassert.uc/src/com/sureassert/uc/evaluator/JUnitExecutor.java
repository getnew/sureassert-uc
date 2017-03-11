/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.sureassert.uc.annotation.NoAutoRun;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.MarkerUtils;
import com.sureassert.uc.evaluator.model.HasJUnitModel;
import com.sureassert.uc.evaluator.model.StubsModel;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ExecutorResult;
import com.sureassert.uc.runtime.ExecutorResult.Type;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SaUCStub;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.runtime.TypeConverterException;

public class JUnitExecutor {

	private final HasJUnitModel hasJUnitModel;

	private final IType jUnitType;

	private final SourceFileFactory sfFactory;

	private final JavaPathData javaPathData;

	// private final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);

	public JUnitExecutor(HasJUnitModel hasJUnitModel, SourceFileFactory sfFactory, JavaPathData javaPathData) throws TypeConverterException, NamedInstanceNotFoundException {

		this.hasJUnitModel = hasJUnitModel;
		this.jUnitType = null;
		this.sfFactory = sfFactory;
		this.javaPathData = javaPathData;
	}

	public JUnitExecutor(IType jUnitType, SourceFileFactory sfFactory, JavaPathData javaPathData) throws TypeConverterException, NamedInstanceNotFoundException {

		this.hasJUnitModel = null;
		this.jUnitType = jUnitType;
		this.sfFactory = sfFactory;
		this.javaPathData = javaPathData;
	}

	public HasJUnitModel getModel() {

		return hasJUnitModel;
	}

	/**
	 * Executes the JUnit class specified by the hasJUnit model.
	 * Errors are returned against the given class.
	 * 
	 * @param classLoader
	 * @return A list of error descriptions; empty if no errors were encountered.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 */
	public List<ExecutorResult> execute(ClassLoader classLoader, int defaultLineNum, //
			Map<Signature, Integer> sigLineNums, boolean clearRuntimePersistentData, IType javaType, //
			IProgressMonitor monitor, int percentComplete, List<Pattern> excludeFilter) throws NamedInstanceNotFoundException, SecurityException, NoSuchMethodException, ClassNotFoundException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, JavaModelException, TypeConverterException {

		List<ExecutorResult> executorResults = new ArrayList<ExecutorResult>();
		for (String jUnitClassName : hasJUnitModel.getJUnitClassNames()) {

			executorResults.addAll(execute(classLoader, defaultLineNum, sigLineNums, jUnitClassName, //
					clearRuntimePersistentData, javaType, monitor, percentComplete, excludeFilter));
		}
		return executorResults;
	}

	public List<ExecutorResult> execute(ClassLoader classLoader, //
			int defaultLineNum, Map<Signature, Integer> sigLineNums, String jUnitClassName, //
			boolean clearRuntimePersistentData, IType javaType, IProgressMonitor monitor, //
			int percentComplete, List<Pattern> excludeFilter) throws NamedInstanceNotFoundException, SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, InstantiationException, JavaModelException, TypeConverterException {

		List<ExecutorResult> executorResults = new ArrayList<ExecutorResult>();
		if (isExcludedFromExecution(jUnitClassName, excludeFilter)) {
			return executorResults;
		}

		Class<?> declaredOnClass = classLoader.loadClass(hasJUnitModel.getDefinedOnClassSig().getClassName());

		if (monitor != null) {
			monitor.setTaskName("Sureassert UC: Executing JUnits (" + percentComplete + "%): " + jUnitClassName);
		}

		Signature clazzSig = SignatureTableFactory.instance.getSignatureForClass(jUnitClassName);
		IType jUnitClassType = javaType.getJavaProject().findType(jUnitClassName);
		PersistentDataFactory.getInstance().setCurrentJUnit(jUnitClassName, clazzSig, //
				StubsModel.getMethodStubs(jUnitClassType, classLoader), clearRuntimePersistentData, null);

		PersistentDataFactory.getInstance().registerStaticDependency(hasJUnitModel.getDefinedOnClassSig(), clazzSig, false);

		Object testClassObj = NamedInstanceFactory.getInstance().getNamedInstance(//
				jUnitClassName, classLoader);
		try {
			if (testClassObj == null || !(testClassObj instanceof Class<?>)) {
				testClassObj = classLoader.loadClass(jUnitClassName);
			}
		} catch (Throwable e) {
			testClassObj = null;
		}
		if (testClassObj == null) {
			throw new NamedInstanceNotFoundException("Could not find JUnit test class named " + jUnitClassName);
		}
		Class<?> testClass = (Class<?>) testClassObj;
		try {
			IType testClassType = javaType.getJavaProject().findType(jUnitClassName);
			if (testClassType != null) {
				IFile file = javaPathData.getFileQuick(testClassType.getCompilationUnit().getPath(), //
						javaType.getJavaProject().getProject().getWorkspace());
				if (file != null) {
					SourceFile sourceFile = sfFactory.getSourceFile(file);
					if (sourceFile != null) {
						defaultLineNum = sourceFile.getLineNum(testClassType.getNameRange().getOffset());
						while (MarkerUtils.isEmptyLine(defaultLineNum, sourceFile) && defaultLineNum < sourceFile.getNumLines())
							defaultLineNum++;
					}
				}
			}
		} catch (Throwable e) {
			// Ignore
		}
		// defaultLineNum = 1;

		BasicUtils.debug("Executing JUnit class: " + testClass.getName());

		// Check for stub definition errors
		Set<SaUCStub> stubs = PersistentDataFactory.getInstance().getCurrentUCMethodStubs();
		if (stubs != null) {
			for (SaUCStub stub : stubs) {
				if (!stub.isValid()) {
					executorResults.add(new ExecutorResult(stub.getError(), Type.ERROR, null, defaultLineNum, testClass.getName()));
				}
			}
		}

		// Result result = JUnitCore.runClasses(testClass);

		// Class<?> utilClass = classLoader.loadClass(JUnitUtils.class.getName());
		// Method runMethod = utilClass.getMethod("runJUnit",
		// classLoader.loadClass(Class.class.getName()));
		// Result result = (Result) runMethod.invoke(utilClass.newInstance(), testClass);

		Object result = null;
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			Class<?>[] argsClass = new Class<?>[] { testClass };
			Class<?>[] args = new Class<?>[] { testClass };
			Class<?> junitCoreClass = classLoader.loadClass("org.junit.runner.JUnitCore");
			Class<?> junitRunListenerClass = classLoader.loadClass("org.junit.runner.notification.RunListener");
			Class<?> saucRunListenerClass = classLoader.loadClass("com.sureassert.uc.runtime.JUnitRunListener");
			Object junitCore = junitCoreClass.newInstance();
			// Add RunListener
			Method addListenerMethod = junitCoreClass.getMethod("addListener", junitRunListenerClass);
			Object runListener = saucRunListenerClass.newInstance();
			addListenerMethod.invoke(junitCore, runListener);
			// Execute
			Method runClassesMethod = junitCoreClass.getMethod("run", argsClass.getClass());
			Timer timer = new Timer("Run JUnit " + testClass.getName());
			result = runClassesMethod.invoke(junitCore, (Object) args);
			timer.printExpiredTime();
		} catch (Exception e) {
			EclipseUtils.reportError(e);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

		for (Object failure : JUnitUtils.getFailures(result)) {

			Throwable cause = JUnitUtils.getFailureException(failure);
			String failureMessage = JUnitUtils.getFailureMessage(failure);
			Object failureDescription = JUnitUtils.getFailureDescription(failure);
			Object failureDescriptionMethodName = JUnitUtils.getFailureDescriptionMethodName(failureDescription);
			String message;
			if (failureMessage == null || failureMessage.trim().equals("")) {
				if ((cause.getStackTrace() != null || cause.getMessage() != null)) {
					message = "JUnit \"" + failureDescriptionMethodName + //
							"\" failed: " + BasicUtils.toDisplayStr(cause);
				} else {
					message = "JUnit \"" + failureDescriptionMethodName + "\" encountered an error";
				}
			} else {
				message = "JUnit \"" + failureDescriptionMethodName + //
						"\" failed with message: " + failureMessage;
			}
			BasicUtils.debug(message);

			// If an exception occurred during execution, get the line number of the deepest
			// stack trace entry in the source class
			int lineNum = BasicUtils.getLineNum(cause, declaredOnClass);
			if (lineNum > -1) {
				executorResults.add(new ExecutorResult(message, Type.ERROR, null, lineNum));
			} else {
				lineNum = BasicUtils.getLineNum(cause, testClass);
				if (lineNum > -1) {
					executorResults.add(new ExecutorResult(message, Type.ERROR, null, lineNum, testClass.getName()));
				} else {
					executorResults.add(new ExecutorResult(message, Type.ERROR, null, defaultLineNum, testClass.getName()));
				}
			}
		}
		if (executorResults.isEmpty()) {
			// All JUnit tests executed successfully
			executorResults.add(new ExecutorResult("All tests passed in " + jUnitClassName, //
					Type.INFO, null, defaultLineNum, testClass.getName()));
		}
		return executorResults;
	}

	public List<ExecutorResult> execute(ClassLoader classLoader, IJavaProject javaProject, //
			boolean clearRuntimePersistentData, IProgressMonitor monitor, int percentComplete, List<Pattern> excludeFilter) throws NamedInstanceNotFoundException, SecurityException,
			NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, JavaModelException,
			TypeConverterException {

		List<ExecutorResult> executorResults = new ArrayList<ExecutorResult>();
		String jUnitClassName = jUnitType.getFullyQualifiedName();
		if (isExcludedFromExecution(jUnitClassName, excludeFilter))
			return executorResults;
		if (EclipseUtils.hasClassAnnotation(jUnitType, NoAutoRun.class.getSimpleName()) || //
				!SaUCPreferences.getIsJUnitAutomationEnabled())
			return executorResults;

		if (monitor != null) {
			monitor.setTaskName("Sureassert UC: Executing JUnits (" + percentComplete + "%): " + jUnitClassName);
		}

		Signature clazzSig = SignatureTableFactory.instance.getSignatureForClass(jUnitClassName);
		Map<String, SaUCStub[]> methodStubs = null;
		methodStubs = StubsModel.getMethodStubs(jUnitType, classLoader);
		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		pdf.setCurrentJUnit(jUnitClassName, //
				clazzSig, methodStubs, clearRuntimePersistentData, null);
		Object testClassObj = classLoader.loadClass(jUnitClassName);
		Class<?> testClass = (Class<?>) testClassObj;
		if (testClass.isInterface() || Modifier.isAbstract(testClass.getModifiers())) {
			return Collections.emptyList();
		}
		int defaultLineNum = 1;
		try {
			IFile file = javaPathData.getFileQuick(jUnitType.getCompilationUnit().getPath(), //
					jUnitType.getJavaProject().getProject().getWorkspace());
			if (file != null) {
				SourceFile sourceFile = sfFactory.getSourceFile(file);
				if (sourceFile != null) {
					defaultLineNum = sourceFile.getLineNum(jUnitType.getNameRange().getOffset());
					while (MarkerUtils.isEmptyLine(defaultLineNum, sourceFile) && defaultLineNum < sourceFile.getNumLines())
						defaultLineNum++;
				}
			}
		} catch (Throwable e) {
			// Ignore
		}

		// Check for stub definition errors
		Set<SaUCStub> stubs = pdf.getCurrentUCMethodStubs();
		if (stubs != null) {
			for (SaUCStub stub : stubs) {
				if (!stub.isValid()) {
					executorResults.add(new ExecutorResult(stub.getError(), Type.ERROR, null, defaultLineNum, testClass.getName()));
				}
			}
		}

		BasicUtils.debug("Executing JUnit class: " + testClass.getName());

		// Result result = JUnitCore.runClasses(testClass);

		// Class<?> utilClass = classLoader.loadClass(JUnitUtils.class.getName());
		// Method runMethod = utilClass.getMethod("runJUnit",
		// classLoader.loadClass(Class.class.getName()));
		// Result result = (Result) runMethod.invoke(utilClass.newInstance(), testClass);

		Object result = null;
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			Class<?>[] argsClass = new Class<?>[] { testClass };
			Class<?>[] args = new Class<?>[] { testClass };
			Class<?> junitCoreClass = classLoader.loadClass("org.junit.runner.JUnitCore");
			Class<?> junitRunListenerClass = classLoader.loadClass("org.junit.runner.notification.RunListener");
			Class<?> saucRunListenerClass = classLoader.loadClass("com.sureassert.uc.runtime.JUnitRunListener");
			Object junitCore = junitCoreClass.newInstance();
			// Add RunListener
			Method addListenerMethod = junitCoreClass.getMethod("addListener", junitRunListenerClass);
			Object runListener = saucRunListenerClass.newInstance();
			addListenerMethod.invoke(junitCore, runListener);
			// Execute
			Method runClassesMethod = junitCoreClass.getMethod("run", argsClass.getClass());

			Timer timer = new Timer("Run JUnit " + testClass.getName());
			result = runClassesMethod.invoke(junitCore, (Object) args);
			timer.printExpiredTime();
		} catch (Exception e) {
			EclipseUtils.reportError(e);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

		for (Object failure : JUnitUtils.getFailures(result)) {

			Throwable cause = JUnitUtils.getFailureException(failure);
			String failureMessage = JUnitUtils.getFailureMessage(failure);
			Object failureDescription = JUnitUtils.getFailureDescription(failure);
			Object failureDescriptionMethodName = JUnitUtils.getFailureDescriptionMethodName(failureDescription);
			String message = null;

			if (failureDescriptionMethodName.equals("initializationError") && cause != null && cause.getStackTrace() != null && cause.getStackTrace().length > 0
					&& (cause.getStackTrace()[0].getClassName().startsWith("org.junit.") || //
					cause.getStackTrace()[0].getClassName().startsWith("junit."))) {
				// This isn't a valid JUnit test class; ignore
			} else {
				if (failureMessage == null || failureMessage.trim().equals("")) {
					if ((cause.getStackTrace() != null || cause.getMessage() != null)) {
						message = "JUnit \"" + failureDescriptionMethodName + //
								"\" failed: " + BasicUtils.toDisplayStr(cause);
					} else {
						message = "JUnit \"" + failureDescriptionMethodName + "\" encountered an error";
					}
				} else {
					message = "JUnit \"" + failureDescriptionMethodName + //
							"\" failed with message: " + failureMessage;
				}
			}
			if (message != null) {
				BasicUtils.debug(message);
				int lineNum = BasicUtils.getLineNum(cause, testClass);
				if (lineNum > -1) {
					executorResults.add(new ExecutorResult(message, Type.ERROR, null, lineNum, testClass.getName()));
				} else {
					executorResults.add(new ExecutorResult(message, Type.ERROR, null, defaultLineNum, testClass.getName()));
				}
			}
		}
		if (executorResults.isEmpty()) {
			// All JUnit tests executed successfully
			executorResults.add(new ExecutorResult("All tests passed in " + jUnitClassName, //
					Type.INFO, null, defaultLineNum, testClass.getName()));
		}
		return executorResults;
	}

	/**
	 * Gets whether the given JUnit class name matches the exclusion filters
	 * defined by the user.
	 * 
	 * @param jUnitClassName
	 * @return true if excluded
	 */
	public static boolean isExcludedFromExecution(String jUnitClassName, List<Pattern> excludeFilter) {

		for (Pattern excPattern : excludeFilter) {
			if (excPattern.matcher(jUnitClassName).matches())
				return true;
		}
		return false;
	}

	/*
	 * private void old() {
	 * 
	 * Object result = null;
	 * try {
	 * Class<?>[] argsClass = new Class<?>[] { testClass };
	 * Class<?>[] args = new Class<?>[] { testClass };
	 * Class<?> junitCoreClass = classLoader.loadClass("org.junit.runner.JUnitCore");
	 * Method runClassesMethod = junitCoreClass.getMethod("runClasses", argsClass.getClass());
	 * result = runClassesMethod.invoke(null, (Object) args);
	 * } catch (Exception e) {
	 * EclipseUtils.reportError(e);
	 * }
	 * 
	 * for (Object failure : JUnitUtils.getFailures(result)) {
	 * 
	 * Throwable cause = JUnitUtils.getFailureException(failure);
	 * String failureMessage = JUnitUtils.getFailureMessage(failure);
	 * Object failureDescription = JUnitUtils.getFailureDescription(failure);
	 * Object failureDescriptionMethodName =
	 * JUnitUtils.getFailureDescriptionMethodName(failureDescription);
	 * String message;
	 * if (failureMessage == null || failureMessage.trim().equals("")) {
	 * if ((cause.getStackTrace() != null || cause.getMessage() != null)) {
	 * message = "JUnit \"" + failureDescriptionMethodName + //
	 * "\" failed: " + BasicUtils.toDisplayStr(cause);
	 * } else {
	 * message = "JUnit \"" + failureDescriptionMethodName + "\" encountered an error";
	 * }
	 * } else {
	 * message = "JUnit \"" + failureDescriptionMethodName + //
	 * "\" failed with message: " + failureMessage;
	 * }
	 * BasicUtils.debug(message);
	 * 
	 * // If an exception occurred during execution, get the line number of the deepest
	 * // stack trace entry in the source class
	 * int lineNum = BasicUtils.getLineNum(cause, declaredOnClass);
	 * if (lineNum > -1) {
	 * executorResults.add(new ExecutorResult(message, Type.ERROR, null, lineNum));
	 * } else {
	 * lineNum = BasicUtils.getLineNum(cause, testClass);
	 * if (lineNum > -1) {
	 * executorResults.add(new ExecutorResult(message, Type.ERROR, null, lineNum,
	 * testClass.getName()));
	 * } else {
	 * executorResults.add(new ExecutorResult(message, Type.ERROR, null, defaultLineNum));
	 * }
	 * }
	 * }
	 * if (executorResults.isEmpty()) {
	 * // All JUnit tests executed successfully
	 * executorResults.add(new ExecutorResult("All tests passed in " + jUnitClassName, //
	 * Type.INFO, null, defaultLineNum));
	 * }
	 * return executorResults;
	 * }
	 */
}
