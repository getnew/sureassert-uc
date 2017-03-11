/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.TestState;
import org.sureassert.uc.annotation.UseCase;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.MarkerUtils;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.evaluator.UseCaseExecutor;
import com.sureassert.uc.evaluator.model.ModelFactory;
import com.sureassert.uc.evaluator.model.TestStateModel;
import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ClassDouble;
import com.sureassert.uc.runtime.ExecutorResult;
import com.sureassert.uc.runtime.ExecutorResult.Type;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.ISINExpression.DefaultToType;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.PersistentDataFactory.CurrentUseCaseMomento;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.SAUCBuildInterruptedError;
import com.sureassert.uc.runtime.exception.UseCaseException;
import com.sureassert.uc.runtime.model.UseCaseModel;

public class UseCaseExecutionDelegate {

	public static final UseCaseExecutionDelegate INSTANCE = new UseCaseExecutionDelegate();

	private Map<String, UseCaseExecutionCommand> ucExecCommandByUcName;
	private Map<String, UseCaseExecutionCommand> ucExecCommandByInstanceOut;

	private JavaPathData javaPathData;
	private SourceFileFactory sfFactory;
	private ClassLoader classLoader;

	private UseCaseExecutionDelegate() {

	}

	public void init(JavaPathData javaPathData, SourceFileFactory sfFactory, ClassLoader classLoader) {

		ucExecCommandByUcName = new HashMap<String, UseCaseExecutionCommand>();
		ucExecCommandByInstanceOut = new HashMap<String, UseCaseExecutionCommand>();
		this.javaPathData = javaPathData;
		this.sfFactory = sfFactory;
		this.classLoader = classLoader;
	}

	public void dispose() {

		if (ucExecCommandByUcName != null)
			ucExecCommandByUcName.clear();
		if (ucExecCommandByInstanceOut != null)
			ucExecCommandByInstanceOut.clear();
		javaPathData = null;
		sfFactory = null;
		classLoader = null;
	}

	/**
	 * Executes the UseCase with the given name. The use-case must have already been
	 * executed prior to invocation of this method.
	 * 
	 * @param ucName The name of a UseCase
	 * @param reexecName The name to which the returned instance should be assigned
	 * 
	 * @return true if a UseCase with the given name was found, otherwise false.
	 * @throws UseCaseException
	 * @throws IOException
	 * @throws CoreException
	 */
	public boolean reexecuteUseCase(String ucName, String reexecName) throws UseCaseException, IOException, CoreException {

		UseCaseExecutionCommand ucExecCommand = ucExecCommandByUcName.get(ucName);
		if (ucExecCommand == null) {
			return false;
		}
		try {
			ucExecCommand.ucModel.setName(reexecName);
			executeUseCase(ucExecCommand, new NullProgressMonitor(), -1, false, false);
		} finally {
			ucExecCommand.ucModel.setName(ucName);
		}
		return true;
	}

	/**
	 * Executes the UseCase with the given name. The use-case must have already been
	 * executed prior to invocation of this method.
	 * 
	 * @param ucName The name of a UseCase
	 * @param reexecName The name to which the returned instance should be assigned
	 * 
	 * @return true if a UseCase with the given name was found, otherwise false.
	 * @throws UseCaseException
	 * @throws IOException
	 * @throws CoreException
	 */
	public boolean reexecuteInstanceout(String instanceoutName, String reexecInstanceoutName) throws UseCaseException, IOException, CoreException {

		UseCaseExecutionCommand ucExecCommand = ucExecCommandByInstanceOut.get(instanceoutName);
		if (ucExecCommand == null) {
			return false;
		}
		try {
			ucExecCommand.ucModel.setInstanceout(reexecInstanceoutName);
			executeUseCase(ucExecCommand, new NullProgressMonitor(), -1, false, false);
		} finally {
			ucExecCommand.ucModel.setInstanceout(instanceoutName);
		}
		return true;
	}

	public void executeUseCase(UseCaseExecutionCommand ucExecCommand, IProgressMonitor monitor, //
			int percentComplete, boolean clearRuntimePersistentData, boolean doEvaluate) throws IOException, CoreException, UseCaseException {

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		CurrentUseCaseMomento currentUCMomento = pdf.getCurrentUCMomento();
		try {
			// Execute UseCase
			String ucName = ucExecCommand.ucModel.getName();
			String instanceOutName = ucExecCommand.ucModel.getInstanceout();
			if (ucName != null)
				ucExecCommandByUcName.put(ucName, ucExecCommand);
			if (instanceOutName != null)
				ucExecCommandByInstanceOut.put(instanceOutName, ucExecCommand);

			IMethod method = ucExecCommand.sourceModel.getMethod(ucExecCommand.methodSig);
			pdf.setCurrentUseCase(ucExecCommand.ucModel.getSignature(), ucExecCommand.methodSig, //
					ucExecCommand.ucModel.getName(), //
					ucExecCommand.ucModel.getDescription(), ucExecCommand.ucModel.getStubs(), //
					clearRuntimePersistentData, ucExecCommand.ucModel.getVerify(), //
					!ucExecCommand.ucModel.isDefinedAsUseCase());

			Object instance = getInstance(ucExecCommand.ucModel, ucExecCommand.sourceModel, ucExecCommand.sourceFile, //
					ucExecCommand.javaType, ucExecCommand.project.getProject(), classLoader, monitor, false);
			UseCaseExecutor ucExecutor = new UseCaseExecutor(ucExecCommand.ucModel);
			try {
				monitor.setTaskName("Sureassert UC: Executing " + //
						(PersistentDataFactory.getInstance().wasLastExecUseCase() ? "UseCase" : "Exemplar") + //
						" (" + percentComplete + "%): " + ucExecCommand.ucModel.getSignature().toString());
				executeUseCase(ucExecutor, ucExecCommand.sourceModel, instance, ucExecCommand.sourceFile, //
						method, ucExecCommand.project.getProject(), monitor, classLoader, doEvaluate);
			} catch (UseCaseExecutionFailure ucef) {
			}
		} catch (UseCaseException uce) {
			handleUCException(ucExecCommand.project.getProject(), uce, //
					ucExecCommand.ucModel.getDeclaredLineNum(), ucExecCommand.sourceFile, sfFactory, javaPathData);
		} finally {
			pdf.restoreCurrentUseCase(currentUCMomento);
		}
	}

	/**
	 * Gets the instance on which to execute the given UseCaseModel. Returns null
	 * if the UseCaseModel is defined on a constructor.
	 * 
	 * @param ucModel
	 * @param sourceModel
	 * @param sourceFile
	 * @param classLoader
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Object getInstance(UseCaseModel ucModel, SourceModel sourceModel, SourceFile sourceFile, IType javaType, IProject project, //
			ClassLoader classLoader, IProgressMonitor monitor, boolean doEvaluate) throws UseCaseException {

		Class<?> clazz = null;
		try {
			// Check for invalid model
			if (!ucModel.isValid()) {
				throw new UseCaseException(ucModel.getError());
			} else if (ucModel.getSignature().isConstructor()) {
				return null;
			} else if (ucModel.getInstance() != null) {
				ISINExpression expr = ucModel.getInstance();
				Object instance = expr.getInstance(classLoader);
				clazz = instance instanceof Class<?> ? (Class<?>) instance : instance.getClass();
				return expr.invoke(instance, classLoader, DefaultToType.NONE);

			} else {
				Signature constructorSig = null;
				if (ucModel.getConstructor().equals(UseCaseModel.NO_CONSTRUCTOR)) {
					return EclipseUtils.getClass(javaType, classLoader);
				} else if (ucModel.getConstructor().equals(UseCaseModel.DEFAULT_NOARGS_CONSTRUCTOR)) {
					clazz = EclipseUtils.getClass(javaType, classLoader);
					Object instance = BasicUtils.newInstance(clazz);
					setTestState(instance, sourceModel, javaType, classLoader);
					return instance;
				} else {
					constructorSig = PersistentDataFactory.getInstance().getDeclaringSignature(ucModel.getConstructor());
					if (constructorSig == null) {
						throw new NamedInstanceNotFoundException("No constructor UseCase found with name \"" + //
								ucModel.getConstructor() + "\"");
					}
				}
				clazz = EclipseUtils.getClass(javaType, classLoader);
				return newInstance(sourceModel, sourceFile, constructorSig, monitor, javaType, project, classLoader, doEvaluate);
			}
		} catch (UseCaseException uce) {
			throw uce;
		} catch (Exception e) {
			if (clazz != null) {
				throw new UseCaseException("Could not create instance of " + clazz.getName(), //
						sourceFile.getPosition(BasicUtils.getLineNum(e, clazz)), e);
			}
			throw new UseCaseException("Error occurred creating instance", e);
		}
	}

	/**
	 * Load an instance of the class represented by the given SourceFile,
	 * using the constructor annotated with a UseCase if present, or otherwise the
	 * default constructor.
	 * 
	 * NOTE: Non-static inner classes are not yet supported.
	 * 
	 * @param sourceModel
	 * @param sourceFile
	 * @param constructorSig The signature of the constructor UseCase to execute. If null,
	 *            the single specified constructor UC or no-arg constructor is executed.
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NamedInstanceNotFoundException
	 * @throws InvocationTargetException
	 * @throws UseCaseException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws CoreException
	 * @throws IOException
	 */
	private Object newInstance(SourceModel sourceModel, SourceFile sourceFile, Signature constructorSig, //
			IProgressMonitor monitor, IType javaType, IProject project, ClassLoader classLoader, boolean doEvaluate) throws SecurityException, NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException, NamedInstanceNotFoundException, InvocationTargetException, UseCaseException, ClassNotFoundException, NoSuchMethodException, IOException,
			CoreException {

		// Create instance
		Object instance = null;
		IMethod[] methods = javaType.getMethods();
		NamedUseCaseModelFactory nucmFactory = new NamedUseCaseModelFactory(//
				javaType, sourceModel, classLoader, sourceFile);
		for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {
			// Find constructor with @UseCase. If constructorSig is specified; only get this
			// constructor.
			IMethod iMethod = methods[methodIndex];
			if (iMethod.isConstructor()) {
				IAnnotation ucAnnotation = EclipseUtils.getAnnotations(iMethod).get(Exemplar.class.getSimpleName());
				if (ucAnnotation == null || !ucAnnotation.exists())
					ucAnnotation = EclipseUtils.getAnnotations(iMethod).get(UseCase.class.getSimpleName());
				if (ucAnnotation != null && ucAnnotation.exists()) {
					AccessibleObject method = sourceModel.getMethod(iMethod, classLoader);
					Signature thisUCSig = SignatureTableFactory.instance.getSignature(method);
					if (constructorSig == null || thisUCSig.equals(constructorSig)) {
						UseCaseModel ucModel = ModelFactory.newUseCaseModel(ucAnnotation.getMemberValuePairs(), //
								thisUCSig, sourceFile.getLineNum(ucAnnotation.getNameRange().getOffset()), //
								null, EclipseUtils.isUseCaseAn(ucAnnotation), nucmFactory);
						UseCaseExecutor ucExecutor = new UseCaseExecutor(ucModel);
						if (!(method instanceof Constructor<?>)) {
							throw new TypeConverterException("Method \"" + ((Method) method).getName() + //
									"\" matched in error to \"" + method.toString(), //
									iMethod.getNameRange().getOffset(), null);
						}
						try {
							instance = executeUseCase(ucExecutor, sourceModel, instance, sourceFile, iMethod, project, //
									monitor, classLoader, doEvaluate);
						} catch (UseCaseExecutionFailure ucef) {
							return null;
						}
						if (instance == null)
							return null; // Assume executeUseCase already set source error
					}
				}
			}
		}

		if (instance == null) {

			// Assume there is a default constructor
			instance = BasicUtils.newInstance(EclipseUtils.getClass(javaType, classLoader));
		}

		setTestState(instance, sourceModel, javaType, classLoader);

		return instance;
	}

	private void setTestState(Object instance, SourceModel sourceModel, IType javaType, ClassLoader classLoader) throws TypeConverterException, JavaModelException {

		// Set state
		for (IField iField : javaType.getFields()) {
			try {
				Map<String, IAnnotation> annotations = EclipseUtils.getAnnotations(iField);
				IAnnotation testStateAnnotation = annotations.get(TestState.class.getSimpleName());
				if (testStateAnnotation != null && testStateAnnotation.exists()) {
					Field field = BasicUtils.getField(EclipseUtils.getClass(javaType, classLoader), //
							iField.getElementName());
					TestStateModel testStateModel = new TestStateModel(//
							testStateAnnotation.getMemberValuePairs(), SignatureTableFactory.instance.getSignature(field));
					field.setAccessible(true);
					field.set(instance, testStateModel.getTypeConvertedValue());
				}
			} catch (Exception e) {
				throw new TypeConverterException("Error setting value of field \"" + //
						iField.getElementName() + "\"", iField.getNameRange().getOffset(), e);
			}
		}
	}

	private void validateUseCase(UseCaseModel ucModel, SourceFile sourceFile, IMethod method, AccessibleObject clazzMethod) {

		// if (isTestClass(sourceFile.getFile().getFullPath()))
		// ucModel.setError("Cannot execute UseCase on test class (class in src/test location).  " +
		// //
		// "UseCases can only be defined on application classes, not test classes.");

		// Check for UseCases defined on doubled classes
		ClassDouble classDouble = (ClassDouble) PersistentDataFactory.getInstance().//
				getClassDoubleForDoubledClassName(method.getDeclaringType().getFullyQualifiedName());
		if (classDouble != null) {
			ucModel.setError("Cannot define UseCases on classes that are replaced by a TestDouble (this class is doubled by " + //
					classDouble.getTestDoubleClassName() + ")");
		}
	}

	public boolean isBuildCancelled(IProgressMonitor monitor) {

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		return (!pdf.isStandaloneBuild() && pdf.isBuildInterrupted()) || monitor.isCanceled();
	}

	private Object executeUseCase(UseCaseExecutor ucExecutor, SourceModel sourceModel, Object instance, //
			SourceFile sourceFile, IMethod method, IProject project, IProgressMonitor monitor, ClassLoader classLoader, //
			boolean doEvaluate) //
			throws NamedInstanceNotFoundException, UseCaseExecutionFailure, IOException, CoreException {

		if (isBuildCancelled(monitor))
			throw new SAUCBuildInterruptedError();
		List<ExecutorResult> results;
		Object returnedValue = null;
		boolean executionErrorOccurred = false;
		try {
			AccessibleObject clazzMethod = sourceModel.getMethod(method, classLoader);
			validateUseCase(ucExecutor.getModel(), sourceFile, method, clazzMethod);
			results = ucExecutor.execute(clazzMethod, instance, classLoader, doEvaluate);
		} catch (Exception e) {
			results = new ArrayList<ExecutorResult>();
			results.add(new ExecutorResult(BasicUtils.toDisplayStr(e), Type.ERROR, null));
		}

		UseCaseModel ucModel = ucExecutor.getModel();
		SourceFile declaredOnFile = sourceFile;

		Signature inheritedFromSig = ucModel.getInheritedFromSignature();
		String declaredOnClass = null;
		if (inheritedFromSig != null) {
			declaredOnClass = inheritedFromSig.getClassName();
			String javaPathStr = PersistentDataFactory.getInstance().getJavaPathStr(declaredOnClass);
			Path javaPath = new Path(javaPathStr);
			IFile inheritedFromFile = javaPathData.getFile(javaPath, project.getWorkspace());
			declaredOnFile = sfFactory.getSourceFile(inheritedFromFile);
		}

		for (ExecutorResult result : results) {
			if (!ignoreResult(result)) {
				if (result.getType() == Type.ERROR) {
					executionErrorOccurred = true;
				}
				SourceFile markerFile = declaredOnFile;

				int lineNum;
				if (result.getErrorLineNum() == -1) {
					lineNum = ucExecutor.getModel().getDeclaredLineNum();
					if (declaredOnClass != null)
						result.setClassName(declaredOnClass);
				} else {
					lineNum = result.getErrorLineNum();
				}

				if (result.getClassName() != null) {
					String javaPathStr = PersistentDataFactory.getInstance().getJavaPathStr(result.getClassName());
					if (javaPathStr == null) {
						EclipseUtils.reportError("Could not determine source file for class " + result.getClassName(), false);
					} else {
						IFile file = javaPathData.getFile(new Path(javaPathStr), project.getWorkspace());
						markerFile = sfFactory.getSourceFile(file);
					}
				}
				MarkerUtils.addMarker(sfFactory, javaPathData, markerFile, result.getDescription(), lineNum, //
						EclipseUtils.getSeverity(result), false);
				returnedValue = result.getReturnedValue();
			}
		}
		if (executionErrorOccurred)
			throw new UseCaseExecutionFailure();
		return returnedValue;
	}

	public static boolean ignoreResult(ExecutorResult res) {

		return res.getType() == ExecutorResult.Type.ERROR && //
				res.getDescription().startsWith(BasicUtils.JAVA_COMPILE_ERROR);
	}

	public static <E extends UseCaseException> void handleUCException(IProject project, E e, int lineNum, SourceFile sourceFile, SourceFileFactory sfFactory, JavaPathData javaPathData) throws E {

		if (e.getSourceLocation() != -1)
			lineNum = sourceFile.getLineNum(e.getSourceLocation());
		if (lineNum != -1) {
			MarkerUtils.addMarker(sfFactory, javaPathData, sourceFile, BasicUtils.toDisplayStr(e), lineNum, IMarker.SEVERITY_ERROR, false);
		} else {
			throw e;
		}
	}

}
