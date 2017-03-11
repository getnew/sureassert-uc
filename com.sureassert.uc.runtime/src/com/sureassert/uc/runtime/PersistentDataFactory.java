/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.sureassert.uc.runtime.ExecutorResult.Type;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.exception.UseCaseException;
import com.sureassert.uc.runtime.internal.PersistentData;
import com.sureassert.uc.runtime.internal.SINExpression;

public class PersistentDataFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	private static class SingletonHolder {

		private static final PersistentDataFactory instance = new PersistentDataFactory();
	}

	private final HashMap<String, PersistentData> pdByProjectName = //
	new HashMap<String, PersistentData>();

	private transient boolean wasLastExecUseCase;

	private transient Signature currentUCSig;

	private transient String currentUCSimpleClassName;

	private transient String currentUCName;

	private transient String currentUCDescription;

	private transient ClassLoader projectClassLoader;

	private transient String currentProjectFilePath;

	private transient String currentJUnitClassName;

	private transient String currentJUnitMethodName;

	private transient TestType currentTestType;

	private transient Set<String> currentMarkedFilePaths = new HashSet<String>();

	private transient boolean isStandaloneBuild;

	private transient int numStandaloneErrors = 0;

	private transient int numSystemErrors = 0;

	private transient boolean licensedBreached = false;

	public enum TestType {
		USE_CASE, JUNIT
	};

	/**
	 * The method currently being executed by the current UseCase; will be different to currentUCSig
	 * for inherited UCs
	 */
	private transient Signature currentExecMethodSig;

	private transient String currentProjectName;

	/** Method stubs: key=className.methodName, value=method stubs */
	private transient Map<String, UCRuntimeStore> ucRuntimeStoreByMethodName;

	private static final String CURRENT_UC = "$current";

	private transient SINExpressionMatcher sinExprMatcher = new SINExpressionMatcher();

	/** Caches the method stub applicable to the given sig under the current UC */
	private transient Map<String, MethodStub> methodStubCache = new HashMap<String, MethodStub>();

	private transient boolean isBuildInterrupted;

	/** The set of paths of default classes generated for this build run for the current project. */
	private transient Set<String> addedDefaultClassSrcPaths;

	public static final String DEFAULT_GEN_PACKAGE_NAME = "sadefaultgen";

	private static final Map<Long, String> classIDToClassName = new HashMap<Long, String>();

	private static final Object LOCK = new Object();

	public class UCRuntimeStore {

		private final Map<String, List<SaUCStub>> ucMethodStubs;
		private final ISINExpression[] currentVerifies;
		// private final int currentVerifyIdx;
		private final ExecutorResult[] verifyResults;

		private UCRuntimeStore(ISINExpression[] ucVerifies) {

			ucMethodStubs = new HashMap<String, List<SaUCStub>>();
			currentVerifies = ucVerifies;
			verifyResults = (currentVerifies != null) ? new ExecutorResult[ucVerifies.length] : null;
			// currentVerifyIdx = 0;
		}

		private List<SaUCStub> getStubs(String stubKey) {

			return ucMethodStubs.get(stubKey);
		}

		private HashSet<SaUCStub> getAllStubs() {

			HashSet<SaUCStub> stubs = new HashSet<SaUCStub>();
			for (List<SaUCStub> entry : ucMethodStubs.values()) {
				stubs.addAll(entry);
			}
			return stubs;
		}

		public ISINExpression[] getCurrentVerifies() {

			return currentVerifies;
		}

		public ExecutorResult[] getVerifyResults() {

			return verifyResults;
		}

		private void addVerifyResult(int verifyIdx, ExecutorResult result) {

			verifyResults[verifyIdx] = result;
		}

		private void add(String stubKey, SaUCStub stub) {

			BasicUtils.mapListAdd(ucMethodStubs, stubKey, stub);
		}
	}

	public static PersistentDataFactory getInstance() {

		return SingletonHolder.instance;
	}

	private PersistentDataFactory() {

		// com.sureassert.uc.runtime.BasicUtils.debug("PersistentDataFactory created by CL " +
		// getClass().getClassLoader().hashCode());
	}

	public void setIsStandaloneBuild(boolean isStandaloneBuild) {

		this.isStandaloneBuild = isStandaloneBuild;
	}

	public boolean isStandaloneBuild() {

		return isStandaloneBuild;
	}

	public void registerStandaloneError() {

		numStandaloneErrors++;
	}

	public int getNumStandaloneErrors() {

		return numStandaloneErrors;
	}

	public void resetNumStandaloneErrors() {

		numStandaloneErrors = 0;
	}

	public static class CurrentUseCaseMomento {

		Signature currentUCSig;
		String currentUCSimpleClassName;
		Signature currentExecMethodSig;
		String currentUCName;
		String currentUCDescription;
		Map<String, UCRuntimeStore> ucStubsByUCName;

		private CurrentUseCaseMomento(Signature currentUCSig, String currentUCSimpleClassName, Signature currentExecMethodSig, //
				String currentUCName, String currentUCDescription, //
				Map<String, UCRuntimeStore> ucStubsByUCName) {

			this.currentUCSig = currentUCSig;
			this.currentUCSimpleClassName = currentUCSimpleClassName;
			this.currentExecMethodSig = currentExecMethodSig;
			this.ucStubsByUCName = ucStubsByUCName;
			this.currentUCName = currentUCName;
			this.currentUCDescription = currentUCDescription;
		}
	}

	public CurrentUseCaseMomento getCurrentUCMomento() {

		return new CurrentUseCaseMomento(currentUCSig, currentUCSimpleClassName, currentExecMethodSig, currentUCName, //
				currentUCDescription, ucRuntimeStoreByMethodName);
	}

	public void restoreCurrentUseCase(CurrentUseCaseMomento currentUcMomento) {

		this.currentUCSig = currentUcMomento.currentUCSig;
		this.currentUCSimpleClassName = currentUcMomento.currentUCSimpleClassName;
		this.currentExecMethodSig = currentUcMomento.currentExecMethodSig;
		this.currentUCName = currentUcMomento.currentUCName;
		this.currentUCDescription = currentUcMomento.currentUCDescription;
		this.ucRuntimeStoreByMethodName = currentUcMomento.ucStubsByUCName;
	}

	public void setCurrentUseCase(Signature ucSig, Signature execMethodSig, String ucName, String ucDescription, SaUCStub[] stubs, //
			boolean clearRuntimePersistentData, ISINExpression[] ucVerifies, boolean definedAsExemplar) throws UseCaseException {

		isBuildInterrupted = false;
		if (clearRuntimePersistentData)
			unregisterRuntimeUseCaseData(ucSig);
		currentUCSig = ucSig;
		currentUCSimpleClassName = ucSig == null ? null : BasicUtils.getSimpleClassName(ucSig.getClassName());
		currentExecMethodSig = execMethodSig;
		currentUCName = ucName;
		currentUCDescription = ucDescription;
		currentJUnitClassName = null;
		currentTestType = TestType.USE_CASE;
		wasLastExecUseCase = !definedAsExemplar;
		ucRuntimeStoreByMethodName = new HashMap<String, UCRuntimeStore>();
		UCRuntimeStore methodStubStore = new UCRuntimeStore(ucVerifies);
		ucRuntimeStoreByMethodName.put(CURRENT_UC, methodStubStore);
		if (stubs != null) {
			for (SaUCStub stub : stubs) {
				String classMethod = stub.getKey();
				methodStubStore.add(classMethod, stub);
				if (!stub.isValid())
					throw new UseCaseException(stub.getError());
			}
		}
		methodStubCache.clear();
		currentJUnitMethodName = null;
	}

	public void setCurrentJUnit(String jUnitClassName, Signature classSig, Map<String, SaUCStub[]> methodStubsByJUnitMethodName, //
			boolean clearRuntimePersistentData, ISINExpression[] ucVerifies) {

		isBuildInterrupted = false;
		if (clearRuntimePersistentData)
			unregisterRuntimeUseCaseData(classSig);
		currentUCSig = classSig;
		currentUCSimpleClassName = classSig == null ? null : BasicUtils.getSimpleClassName(classSig.getClassName());
		currentExecMethodSig = classSig;
		currentUCName = null;
		currentUCDescription = null;
		currentJUnitClassName = jUnitClassName;
		currentTestType = TestType.JUNIT;
		ucRuntimeStoreByMethodName = new HashMap<String, UCRuntimeStore>();
		if (methodStubsByJUnitMethodName != null) {

			for (Entry<String, SaUCStub[]> junitMethodToMethodStubs : methodStubsByJUnitMethodName.entrySet()) {

				String junitMethod = junitMethodToMethodStubs.getKey();
				SaUCStub[] methodStubs = junitMethodToMethodStubs.getValue();
				UCRuntimeStore methodStubStore = new UCRuntimeStore(ucVerifies);
				ucRuntimeStoreByMethodName.put(junitMethod, methodStubStore);

				for (SaUCStub methodStub : methodStubs) {
					String stubKey = methodStub.getKey();
					methodStubStore.add(stubKey, methodStub);
				}
			}
		}
		methodStubCache.clear();
		currentJUnitMethodName = null;
	}

	public void setCurrentJUnitMethodName(String jUnitMethodName) {

		currentJUnitMethodName = jUnitMethodName;
		methodStubCache.clear();
	}

	public void setBuildInterrupted() {

		isBuildInterrupted = true;
	}

	public boolean isBuildInterrupted() {

		return isBuildInterrupted;
	}

	public String getClassName(long classID) {

		return classIDToClassName.get(classID);
	}

	public long registerNewClass(String className) {

		synchronized (classIDToClassName) {

			long id = classIDToClassName.size();
			classIDToClassName.put(id, className);
			return id;
		}
	}

	/**
	 * Gets whether the last UseCase that was executed was defined as a UseCase
	 * or an Exemplar. Returns true if it was a UseCase.
	 * 
	 * @return
	 */
	public boolean wasLastExecUseCase() {

		return wasLastExecUseCase;
	}

	public void setWasLastExecUseCase(boolean wasLastExecUseCase) {

		this.wasLastExecUseCase = wasLastExecUseCase;
	}

	/*
	 * public void setCurrentExecMethod(Signature execMethodSig) {
	 * 
	 * currentExecMethodSig = execMethodSig;
	 * }
	 */

	public void setCurrentProject(String projectName, String currentProjectFilePath) {

		currentProjectName = projectName;
		this.currentProjectFilePath = currentProjectFilePath;
		addedDefaultClassSrcPaths = new HashSet<String>();
		numSystemErrors = 0;
		licensedBreached = false;
	}

	public void registerSystemError() {

		numSystemErrors++;
	}

	public void registerLicenseBreached() {

		licensedBreached = true;
	}

	public int getNumSystemErrors() {

		return numSystemErrors;
	}

	public boolean isLicensedBreached() {

		return licensedBreached;
	}

	public String getCurrentProjectFilePath() {

		return currentProjectFilePath;
	}

	public void addDefaultClass(String srcPath) {

		addedDefaultClassSrcPaths.add(srcPath);
	}

	public Set<String> getAddedDefaultClassSrcPaths() {

		return addedDefaultClassSrcPaths;
	}

	/**
	 * If currently executing a JUnit test, returns the name of the JUnit class. Otherwise returns
	 * null.
	 * 
	 * @return
	 */
	public String getCurrentJUnitClass() {

		return currentJUnitClassName;
	}

	/**
	 * If currently executing a JUnit test, returns the name of the JUnit class. Otherwise returns
	 * null.
	 * 
	 * @return
	 */
	public String getCurrentJUnitMethod() {

		return currentJUnitMethodName;
	}

	public void setCurrentProjectClassLoader(ClassLoader projectCL) {

		projectClassLoader = projectCL;
	}

	public ClassLoader getCurrentProjectClassLoader() {

		return projectClassLoader;
	}

	public void addMarkedFilePath(String markedFilePath) {

		currentMarkedFilePaths.add(markedFilePath);
	}

	public Set<String> getMarkedFilePaths() {

		return currentMarkedFilePaths;
	}

	public void clearMarkedFilePaths() {

		currentMarkedFilePaths.clear();
	}

	public Signature getCurrentUseCaseSignature() {

		return currentUCSig;
	}

	public String getCurrentUseCaseSimpleClassName() {

		return currentUCSimpleClassName;
	}

	public String getCurrentUseCaseName() {

		return currentUCName;
	}

	public String getCurrentUseCaseDescription() {

		return currentUCDescription;
	}

	public Signature getCurrentExecMethodSignature() {

		return currentExecMethodSig;
	}

	public String getCurrentProjectName() {

		return currentProjectName;
	}

	public Set<SaUCStub> getCurrentUCMethodStubs() {

		Set<SaUCStub> stubs = null;
		if (currentTestType == TestType.USE_CASE) {
			stubs = ucRuntimeStoreByMethodName.get(CURRENT_UC).getAllStubs();
		} else {
			stubs = new LinkedHashSet<SaUCStub>();
			for (UCRuntimeStore store : ucRuntimeStoreByMethodName.values()) {
				stubs.addAll(store.getAllStubs());
			}
		}
		return stubs;
	}

	private List<SaUCStub> getCurrentUCMethodStubs(Signature method) {

		List<SaUCStub> stubs = null;
		if (currentTestType == TestType.USE_CASE) {
			// Check for method stub defined using full class name
			stubs = ucRuntimeStoreByMethodName.get(CURRENT_UC).getStubs(method.getClassMethodNameKey());
			if (stubs == null) {
				// Check for method stub defined for superclasses
				try {
					ClassLoader cl = PersistentDataFactory.getInstance().getCurrentProjectClassLoader();
					Class<?> clazz = cl.loadClass(method.getClassName());

					Set<Class<?>> superClasses = BasicUtils.getSuperClassAndInterfaces(clazz, true, false);
					for (Class<?> superClass : superClasses) {

						if (stubs == null) {
							stubs = ucRuntimeStoreByMethodName.get(CURRENT_UC).getStubs(//
									Signature.getClassMethodNameKey(superClass, method.getMemberName()));
						}
					}
				} catch (Exception e) {
					BasicUtils.debug(ExceptionUtils.getFullStackTrace(e));
				}
			}
		} else {
			if (ucRuntimeStoreByMethodName.isEmpty()) {
				return null;
			}
			// Determine which test method is being executed
			UCRuntimeStore methodStubStore = ucRuntimeStoreByMethodName.get(currentJUnitMethodName);
			if (methodStubStore != null && method != null) {
				stubs = methodStubStore.getStubs(method.getClassMethodNameKey());
				if (stubs == null) {
					stubs = methodStubStore.getStubs(method.getSimpleClassMethodNameKey());
				}
			}
		}
		return stubs;
	}

	private List<SaUCStub> getCurrentUCSourceStubs(String sourceStubVarName) {

		return ucRuntimeStoreByMethodName.get(CURRENT_UC).getStubs(sourceStubVarName);
	}

	public UCRuntimeStore getCurrentUCRuntimeStore() {

		return ucRuntimeStoreByMethodName.get(CURRENT_UC);
	}

	public SourceStub getSourceStub(String sourceStubVarName) {

		return getSourceStub(sourceStubVarName, PersistentDataFactory.getInstance().getCurrentProjectClassLoader());
	}

	/**
	 * Gets the active stub variable definition for the given source stub variable name
	 * 
	 * @param sourceStubVarName
	 * @return
	 */
	public SourceStub getSourceStub(String sourceStubVarName, ClassLoader classLoader) {

		if (sourceStubVarName.contains("."))
			throw new IllegalArgumentException("Source stub variable names cannot contain a dot (.)");
		List<SaUCStub> stubs = getCurrentUCSourceStubs(sourceStubVarName);
		if (stubs == null || stubs.isEmpty())
			return null;
		else
			return (SourceStub) stubs.get(0);
	}

	public MethodStub getMethodStub(Signature method, Object invokedObj, Object[] paramVals) {

		return getMethodStub(method, invokedObj, paramVals, projectClassLoader);
	}

	/**
	 * Gets the active method stub for the given method
	 * 
	 * @param method
	 * @return
	 */
	public MethodStub getMethodStub(Signature method, Object invokedObj, Object[] paramVals, ClassLoader classLoader) {

		MethodStub stub = methodStubCache.get(method.toString());
		if (stub == MethodStub.NULL_METHOD_STUB)
			return null;
		else if (stub != null)
			return stub;
		List<SaUCStub> stubs = getCurrentUCMethodStubs(method);
		if (stubs == null) {
			methodStubCache.put(method.getClassMethodNameKey(), MethodStub.NULL_METHOD_STUB);
			return null;
		}
		stubs = new ArrayList<SaUCStub>(stubs);
		// String[] stubParamClassNames;
		// String[] methodParamClassNames = method.getParamClassNames();
		boolean paramsMatch;
		ISINExpression matchExpr = null;
		for (Iterator<SaUCStub> i = stubs.iterator(); i.hasNext();) {
			stub = (MethodStub) i.next();
			/*
			 * paramsMatch = true;
			 * stubParamClassNames = stub.getDoubledMethodSig().getParamClassNames();
			 * if (stubParamClassNames != Signature.CLASS_PARAMS) {
			 * if (stubParamClassNames.length != methodParamClassNames.length) {
			 * paramsMatch = false;
			 * } else {
			 * for (int paramIndex = 0; paramIndex < stubParamClassNames.length && paramsMatch;
			 * paramIndex++) {
			 * try {
			 * if (BasicUtils.equals(stubParamClassNames[paramIndex],
			 * methodParamClassNames[paramIndex])) {
			 * // match
			 * } else if (!stubParamClassNames[paramIndex].contains(".") && //
			 * BasicUtils.equals(stubParamClassNames[paramIndex],//
			 * BasicUtils.getSimpleClassName(methodParamClassNames[paramIndex]))) {
			 * // match
			 * } else {
			 * Class<?> stubParamClass = classLoader.loadClass(stubParamClassNames[paramIndex]);
			 * Class<?> methodParamClass = classLoader.loadClass(methodParamClassNames[paramIndex]);
			 * if (!stubParamClass.isAssignableFrom(methodParamClass))
			 * paramsMatch = false;
			 * }
			 * } catch (ClassNotFoundException cnfe) {
			 * paramsMatch = false;
			 * }
			 * }
			 * }
			 * }
			 */

			matchExpr = stub.getMethodMatchExpression();
			ExecutorResult result = sinExprMatcher.match(matchExpr, method, invokedObj, paramVals, classLoader);
			// if (result != null)
			// ucRuntimeStore.addVerifyResult(verifyIdx, result);
			paramsMatch = !(result == null || result.getType() != Type.INFO);

			if (!paramsMatch) {
				i.remove();
			}
		}
		if (stubs.size() > 1)
			throw new SARuntimeException("More than one method stub definition matches this method");
		stub = stubs.isEmpty() ? null : (MethodStub) stubs.get(0);
		if (matchExpr != null) {
			// Cache the stub for this method - but only if no param matchers specified
			try {
				Signature matchExprSig = matchExpr.getSignature(classLoader);
				methodStubCache.put(matchExprSig.toString(), stub);
			} catch (Exception e) {
				// expression isn't simple signature (has match expressions); fine but can't cache
			}
		}
		return stub;
	}

	/**
	 * Assesses the given method call against the current list of verify expressions.
	 * Updates the current verify results.
	 * 
	 * @param method
	 * @return
	 */
	public void verifyMethodCall(Signature method, Object invokedObj, Object... paramVals) {

		UCRuntimeStore ucRuntimeStore = getCurrentUCRuntimeStore();
		if (ucRuntimeStore == null)
			return;
		ISINExpression[] verifies = ucRuntimeStore.getCurrentVerifies();
		if (verifies == null)
			return;
		try {
			ClassLoader cl = getCurrentProjectClassLoader();
			for (int verifyIdx = 0; verifyIdx < verifies.length; verifyIdx++) {

				SINExpression verify = (SINExpression) verifies[verifyIdx];
				ExecutorResult result = sinExprMatcher.match(verify, method, invokedObj, paramVals, cl);
				if (result != null)
					ucRuntimeStore.addVerifyResult(verifyIdx, result);
			}
		} catch (Exception e) {
			if (!(e instanceof RuntimeException)) {
				throw new SARuntimeException(e);
			} else {
				throw (RuntimeException) e;
			}
		}
	}

	/**
	 * Gets the PersistentData for the project currently being processed.
	 * 
	 * @return PersistentData
	 */
	private PersistentData get() {

		return get(getCurrentProjectName());
	}

	/**
	 * Gets the Persistent data for the project with the given name.
	 * 
	 * @param projectName
	 * @return PersistentData
	 */
	private PersistentData get(String projectName) {

		synchronized (LOCK) {
			PersistentData pd = pdByProjectName.get(projectName);
			if (pd == null) {
				pd = new PersistentData(projectName);
				pdByProjectName.put(projectName, pd);
			}
			return pd;
		}
	}

	public void registerProjectClass(String javaPathStr, String className, String simpleClassName) throws ClassNotFoundException {

		get().registerProjectClass(javaPathStr, className);
		get().addNamedClass(simpleClassName, className);
		get().registerDeclaringSignature(//
				SignatureTableFactory.instance.getSignatureForClass(className), simpleClassName);
	}

	public void registerProjectUC(Signature ucSig) {

		get().registerProjectUC(ucSig);
	}

	public void setCoverage(String className, float[] coverage) {
		
		// Check for test double
		TestDouble classDouble = getClassDoubleForDoubledClassName(className);
		if (classDouble != null && !classDouble.getTestDoubleClassName().equals(className)) {
			setCoverage(classDouble.getTestDoubleClassName(), coverage);
		}

		// Check current project
		PersistentData pd = get();
		boolean isProjectUC = pd.isProjectClass(className);
		if (isProjectUC) {
			pd.setCoverage(className, coverage);
		} else {
			// Check all projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				pd = entry.getValue();
				isProjectUC = pd.isProjectClass(className);
				if (isProjectUC) {
					pd.setCoverage(className, coverage);
				}
			}
		}
	}

	public float[] getCoverage(String className) {

		// Check for test double
		TestDouble classDouble = getClassDoubleForDoubledClassName(className);
		if (classDouble != null && !classDouble.getTestDoubleClassName().equals(className)) {
			return getCoverage(classDouble.getTestDoubleClassName());
		}

		// Check current project
		float[] coverage = get().getCoverage(className);
		if (coverage == null) {
			// Check all projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				coverage = entry.getValue().getCoverage(className);
				if (coverage != null)
					return coverage;
			}
		}
		return coverage;
	}
	
	public Set<String> getAllProjectClasses(String projectName) {
		
		PersistentData pd = get(projectName);
		if (pd == null)
			return null;
		return pd.getAllProjectClasses();
	}

	public boolean isAnyProjectClass(String className) {

		// Check current project
		boolean isProjectUC = get().isProjectClass(className);
		if (!isProjectUC) {
			// Check all projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				isProjectUC = entry.getValue().isProjectClass(className);
				if (isProjectUC)
					return true;
			}
		}
		return isProjectUC;
	}

	public boolean isIgnoreCoverageClass(String className) {

		// Check current project
		PersistentData pd = get();
		if (pd.isProjectClass(className))
			return pd.isIgnoreCoverageClass(className);
		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			if (entry.getValue().isProjectClass(className))
				return entry.getValue().isIgnoreCoverageClass(className);
		}
		return false;
	}

	public void registerIgnoreCoverageClass(String className) {

		get().registerIgnoreCoverageClass(className);
	}

	public void registerIgnoreCoverageMethods(String className, List<Integer> methodStartEndLinePairs) {

		get().registerIgnoreCoverageMethods(className, methodStartEndLinePairs);
	}

	public boolean hasIgnoreCoverageMethods(String className) {

		// Check current project
		PersistentData pd = get();
		if (pd.isProjectClass(className))
			return pd.hasIgnoreCoverageMethods(className);
		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			if (entry.getValue().isProjectClass(className))
				return entry.getValue().hasIgnoreCoverageMethods(className);
		}
		return false;
	}

	public List<Integer> getIgnoreCoverageStartEndLines(String className) {

		// Check current project
		PersistentData pd = get();
		if (pd.isProjectClass(className))
			return pd.getIgnoreCoverageStartEndLines(className);
		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			if (entry.getValue().isProjectClass(className))
				return entry.getValue().getIgnoreCoverageStartEndLines(className);
		}
		return null;
	}

	public Set<String> getClassNames(String pathStr) {

		// Check current project
		Set<String> classNames = get().getClassNames(pathStr);
		if (classNames.isEmpty()) {
			// Check all projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				classNames = entry.getValue().getClassNames(pathStr);
				if (!classNames.isEmpty())
					return classNames;
			}
		}
		return classNames;
	}

	public String getJavaPathStr(String className) {

		// Check current project
		String pathStr = get().getJavaPathStr(className);
		if (pathStr == null) {
			// Check all projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				pathStr = entry.getValue().getJavaPathStr(className);
				if (pathStr != null)
					return pathStr;
			}
		}
		return pathStr;
	}

	/**
	 * Registers that a UseCase will invoke (directly or directly) the given member signature.
	 * 
	 * @param called The signature of a member (method/constructor/field) that has been invoked.
	 * @param useCase The UseCase being executed.
	 * @param isUseCaseDepends True if the called member is a UseCase, e.g. defining a named
	 *            instance, and the dependency is on the UseCase itself rather than the member.
	 */
	public void registerDependency(Signature called, Signature useCase, boolean isUCDepends) {

		registerDependency(called, useCase, isUCDepends, false);
	}

	/**
	 * Registers that a use-case has invoked (directly or directly) the given member signature
	 * during execution of a use-case.
	 * 
	 * @param called The signature of a member (method/constructor/field) that has been invoked.
	 * @param useCase The use-case being executed.
	 * @param isUseCaseDepends True if the called member is a UseCase, e.g. defining a named
	 *            instance, and the dependency is on the UseCase itself rather than the member.
	 */
	public void registerRuntimeDependency(Signature called, Signature useCase, boolean isUseCaseDepends) {

		get().registerRuntimeDependency(called, useCase, isUseCaseDepends);
	}

	/**
	 * Registers that a use-case has invoked (directly or directly) the given member signature.
	 * 
	 * @param called The signature of a member (method/constructor/field) that has been invoked.
	 * @param useCase The use-case being executed.
	 * @param isUseCaseDepends True if the called member is a UseCase, e.g. defining a named
	 *            instance, and the dependency is on the UseCase itself rather than the member.
	 * @param isOffspring True where registering a dependency from a parent UC to a child UC,
	 *            e.g. superclass method to implementation method.
	 */
	public void registerDependency(Signature called, Signature useCase, boolean isUseCaseDepends, //
			boolean isOffspring) {

		// Check for circular dependency
		if (isUseCaseDepends && !isOffspring && getDependencyUseCases(called).contains(useCase)) {
			if (!useCase.getClassName().equals(called.getClassName())) {
				throw new CircularDependencyException("Circular dependencies are not permitted.  " + //
						"UseCases defined on following two signatures depend on each-other: " + //
						called.toString() + " and " + useCase.toString());
			}
		}
		get().registerDependency(called, useCase, isUseCaseDepends);
	}

	/**
	 * Registers that a member (method/constructor/field) has a potential dependency on the given
	 * member.
	 * This is used to dictate that any dependency of a use-case on <code>from</code> also
	 * has a dependency on <code>to</code>
	 * 
	 * @param from
	 * @param to
	 * @param toIsSuperSig True if to is a superclass/interface UseCase sig and from is the
	 *            implementation sig.
	 */
	public void registerStaticDependency(Signature from, Signature to, boolean toIsSuperSig) {

		if (from != null && to != null && from.getClassName() != null && to.getClassName() != null && //
				!from.getClassName().startsWith("sureassert.") && !to.getClassName().startsWith("sureassert.") && //
				!from.getClassName().startsWith("java.") && !to.getClassName().startsWith("java.")) {
			get().registerStaticDependency(from, to, toIsSuperSig);
		}
	}

	/**
	 * Deletes all statically-determined references keyed by the given class from all PersistentData
	 * instances.
	 * 
	 * @param className The name of a deleted class.
	 * @param classLoader Optional. If given will significantly speed up retrieval of class sigs.
	 *            Otherwise all UseCase sigs across all projects are searched.
	 * @throws ClassNotFoundException
	 */
	public void unregisterStaticClassData(String className, ClassLoader classLoader) {

		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			entry.getValue().unregisterStaticClassData(className, classLoader);
		}
	}

	/**
	 * Deletes all statically and runtime-determined references keyed by the given class from all
	 * PersistentData
	 * instances.
	 * 
	 * @param className The name of a deleted class.
	 * @param classLoader Optional. If given will significantly speed up retrieval of class sigs.
	 *            Otherwise all UseCase sigs across all projects are searched.
	 * @throws ClassNotFoundException
	 */
	public void unregisterAllClassData(String className, ClassLoader classLoader) {

		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			entry.getValue().unregisterAllClassData(className, classLoader);
		}
	}

	/**
	 * Deletes all runtime-determined references keyed by the given UseCase signature from all
	 * PersistentData instances.
	 * 
	 * @param className The name of a deleted class.
	 * @throws ClassNotFoundException
	 */
	public void unregisterRuntimeUseCaseData(Signature ucSig) {

		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			entry.getValue().unregisterRuntimeUseCaseData(ucSig);
		}
	}

	/**
	 * Gets the set of signatures of UseCases which invoke (directly or indirectly)
	 * the member with the given signature.
	 * 
	 * @param sig
	 * @return
	 */
	public Set<Signature> getClientUseCases(Signature sig) {

		Set<Signature> sigs = new HashSet<Signature>();
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			sigs.addAll(entry.getValue().getClientUseCases(sig));
		}
		return sigs;
	}

	/**
	 * Gets the set of signatures of UseCases which the UseCase with the given
	 * signature requires for successful execution.
	 * 
	 * @param ucSig
	 * @return
	 */
	public Set<Signature> getDependencyUseCases(Signature ucSig) {

		Set<Signature> sigs = new HashSet<Signature>();
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			sigs.addAll(entry.getValue().getDependencyUseCases(ucSig, new HashSet<Signature>()));
		}
		return sigs;
	}

	/**
	 * Gets whether the given signature is a UseCase signature
	 * 
	 * @param sig
	 * @return
	 */
	public boolean isUseCaseSignature(Signature sig) {

		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			if (entry.getValue().isProjectUC(sig))
				return true;
		}
		return false;
	}

	public void registerDeclaringSignature(Signature sig, String instanceName) {

		if (sig == null || instanceName == null)
			return;
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			if (entry.getValue().isProjectClass(sig.getClassName())) {
				entry.getValue().registerDeclaringSignature(sig, instanceName);
				return;
			}
		}
		get().registerDeclaringSignature(sig, instanceName);
	}

	public Signature getDeclaringSignature(String instanceName) {

		// Check current project
		Signature sig = get().getDeclaringSignature(instanceName);
		if (sig == null) {
			// Check all projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				sig = entry.getValue().getDeclaringSignature(instanceName);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	public void addNamedClass(String name, String className) {

		get().addNamedClass(name, className);
	}

	public String getFullClassName(String simpleClassName) {

		// Try this project
		String fullClassName = get().getFullClassName(simpleClassName);
		if (fullClassName != null)
			return fullClassName;
		// Check all projects
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			fullClassName = entry.getValue().getFullClassName(simpleClassName);
			if (fullClassName != null)
				return fullClassName;
		}
		return null;
	}

	public void registerDefaultClass(String simpleGenClassName, String genClassName, String genSuperClassName) {

		addNamedClass("*" + simpleGenClassName, genClassName);
		addNamedClass(NamedInstanceFactory.DEFAULT_CLASS_INTERNAL_SUPERNAME_PREFIX + genClassName, genSuperClassName);
	}

	/**
	 * Gets the name of the class from which the given default class was generated.
	 */
	public String getClassNameOfDefaultSuperclass(String defaultClassName) {

		return getFullClassName(//
		NamedInstanceFactory.DEFAULT_CLASS_INTERNAL_SUPERNAME_PREFIX + defaultClassName);
	}

	public void addClassDouble(TestDouble classDouble, Signature declaringMemberSig) throws ClassNotFoundException {

		String className = classDouble.getTestDoubleClassName();
		// Try this project
		if (get().isProjectClass(className)) {
			get().addClassDouble(classDouble, declaringMemberSig);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(className)) {
					entry.getValue().addClassDouble(classDouble, declaringMemberSig);
				}
			}
		}
	}

	public String getProjectOfClass(String className) {

		// Try this project
		if (get().isProjectClass(className)) {
			return currentProjectName;
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(className)) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	public void setDoubledClassSourcePath(String tdClass, String doubledClassSourcePath) {

		// Try this project
		if (get().isProjectClass(tdClass)) {
			get().setDoubledClassSourcePath(tdClass, doubledClassSourcePath);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(tdClass)) {
					entry.getValue().setDoubledClassSourcePath(tdClass, doubledClassSourcePath);
				}
			}
		}
	}

	public void setTDClassSourcePath(String tdClass, String tdClassSourcePath) {

		// Try this project
		if (get().isProjectClass(tdClass)) {
			get().setTDClassSourcePath(tdClass, tdClassSourcePath);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(tdClass)) {
					entry.getValue().setTDClassSourcePath(tdClass, tdClassSourcePath);
				}
			}
		}
	}

	public String getDoubledClassSourcePath(String tdClass) {

		// Try this project
		if (get().isProjectClass(tdClass)) {
			return get().getDoubledClassSourcePath(tdClass);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(tdClass)) {
					return entry.getValue().getDoubledClassSourcePath(tdClass);
				}
			}
		}
		return null;
	}

	public String getTDClassSourcePath(String tdClass) {

		// Try this project
		if (get().isProjectClass(tdClass)) {
			return get().getTDClassSourcePath(tdClass);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(tdClass)) {
					return entry.getValue().getTDClassSourcePath(tdClass);
				}
			}
		}
		return null;
	}

	public TestDouble getClassDoubleForTDClassName(String tdClassName) {

		// Try this project
		if (get().isProjectClass(tdClassName)) {
			return get().getClassDoubleForTDClassName(tdClassName);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(tdClassName)) {
					return entry.getValue().getClassDoubleForTDClassName(tdClassName);
				}
			}
		}
		return null;
	}

	public TestDouble getClassDoubleForDoubledClassName(String doubledClassName) {

		// Try this project
		if (get().isProjectClass(doubledClassName)) {
			return get().getClassDoubleForDoubledClassName(doubledClassName);
		} else {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				if (entry.getValue().isProjectClass(doubledClassName)) {
					return entry.getValue().getClassDoubleForDoubledClassName(doubledClassName);
				}
			}
		}
		return null;
	}

	public TestDouble getClassDoubleForJavaPath(String javaPathStr) {

		// Try this project
		TestDouble td = get().getClassDoubleForJavaPath(javaPathStr);
		if (td == null) {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				td = entry.getValue().getClassDoubleForJavaPath(javaPathStr);
				if (td != null)
					return td;
			}
		}
		return td;
	}

	public void registerSINType(String prefix, Signature sig) {

		get().registerSINType(prefix, sig);
	}

	public String getSinTypePrefixByMethod(Signature sig) {

		// Try this project
		String prefix = get().getSinTypePrefixByMethod(sig);
		if (prefix == null) {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				prefix = entry.getValue().getSinTypePrefixByMethod(sig);
				if (prefix != null)
					return prefix;
			}
		}
		return prefix;
	}

	public Signature getSinTypeMethodByPrefix(String prefix) {

		// Try this project
		Signature sig = get().getSinTypeMethodByPrefix(prefix);
		if (sig == null) {
			// Try other projects
			for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
				sig = entry.getValue().getSinTypeMethodByPrefix(prefix);
				if (sig != null)
					return sig;
			}
		}
		return sig;
	}

	/**
	 * Gets the number of signatures that have UseCases on them.
	 * 
	 * @return
	 */
	public int getNumUseCaseSigs() {

		int numUseCaseSigs = 0;
		for (Entry<String, PersistentData> entry : pdByProjectName.entrySet()) {
			numUseCaseSigs += entry.getValue().getNumUseCaseSigs();
		}
		return numUseCaseSigs;
	}

	public void serialize(String projectName, File projectLocation) {

		deletePersistentStore(projectName, projectLocation);

		Timer timer = new Timer("serialize for " + projectName);
		get(projectName).serialize(projectName, projectLocation);
		timer.printExpiredTime();
	}

	public void writePersistentData(ObjectOutputStream out) throws IOException {

		BasicUtils.debug("Writing number of PersistentData");
		out.writeObject(new Integer(pdByProjectName.size()));
		int i = 0;
		for (PersistentData pd : pdByProjectName.values()) {
			BasicUtils.debug("Writing PersistentData[" + (i++) + "]");
			out.writeObject(pd);
		}
	}

	public void serialize(String projectName, ObjectOutputStream out) throws IOException {

		Timer timer = new Timer("serialize to ObjectOutputStream for " + projectName);
		out.writeObject(get(projectName));
		timer.printExpiredTime();
	}

	public void deletePersistentStore(String projectName, File projectLocation) {

		get().deletePersistentStore(projectName, projectLocation);
	}

	public void setNewPersistentStore(String projectName) {

		pdByProjectName.put(projectName, new PersistentData(projectName));
	}

	public void load(String projectName, File projectLocation, File workspaceRoot) throws PersistentDataLoadException {

		Timer timer = new Timer("load for " + projectName);
		try {
			pdByProjectName.put(projectName, PersistentData.load(projectName, projectLocation));
		} catch (Throwable e) {
			throw new PersistentDataLoadException(e);
		} finally {
			timer.printExpiredTime();
		}
	}

	public void load(ObjectInputStream in) throws PersistentDataLoadException {

		Timer timer = new Timer("load project from InputStream");
		try {
			PersistentData pd = (PersistentData) in.readObject();
			pdByProjectName.put(pd.getProjectName(), pd);
		} catch (Throwable e) {
			throw new PersistentDataLoadException(e);
		} finally {
			timer.printExpiredTime();
		}
	}

	public void clear() {

		get().clear();
	}
}
