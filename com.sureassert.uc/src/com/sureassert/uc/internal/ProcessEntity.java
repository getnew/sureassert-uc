/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.HasJUnit;
import org.sureassert.uc.annotation.MultiUseCase;
import org.sureassert.uc.annotation.NamedClass;
import org.sureassert.uc.annotation.NamedInstance;
import org.sureassert.uc.annotation.TestState;
import org.sureassert.uc.annotation.UseCase;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.builder.ClassNotYetCompiledException;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.evaluator.model.ModelFactory;
import com.sureassert.uc.evaluator.model.MultiUseCaseModel;
import com.sureassert.uc.evaluator.model.NamedClassModel;
import com.sureassert.uc.evaluator.model.NamedInstanceModel;
import com.sureassert.uc.evaluator.model.TestStateModel;
import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.interceptor.SourceModelFactory;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.IntHolder;
import com.sureassert.uc.runtime.MethodStub;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINExpressionFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.SaUCStub;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.runtime.TypeConverter;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.exception.BaseSAException;
import com.sureassert.uc.runtime.exception.UseCaseException;
import com.sureassert.uc.runtime.model.UseCaseModel;
import com.sureassert.uc.runtime.typeconverter.SINExpressionTC;

public class ProcessEntity {

	private static final long serialVersionUID = 1L;

	private final IFile file;
	private final IPath javaPath;
	private final IType javaType;
	private Set<Signature> processSigs;
	private final boolean isChanged;
	private Map<Signature, List<TestStateModel>> testStateModels;
	private Map<Signature, List<UseCaseModel>> useCaseModels;
	private Map<Signature, UseCaseMetadata> ucMetadata;

	private Set<Signature> dependsSigs;

	public ProcessEntity(IFile file, IPath javaPath, IType javaType, Set<Signature> processSigs, //
			boolean isChanged, ClassLoader projectCL) throws ClassNotYetCompiledException {

		try {
			EclipseUtils.getClass(javaType, projectCL);
		} catch (Throwable e) {
			throw new ClassNotYetCompiledException();
		}

		this.file = file;
		this.javaPath = javaPath;
		this.javaType = javaType;
		this.processSigs = processSigs;
		this.isChanged = isChanged;
	}

	/**
	 * Adds the sigs from the other ProcessEntity to this one.
	 * NOTE: After calling this is is the client's responsibility to sort/re-sort the processSigs.
	 * 
	 * @param other
	 */
	protected void addProcessSigs(ProcessEntity other) {

		// NOTE: If processSigs is null, assume all class sigs therefore no need to add
		if (processSigs != null) {
			if (other.processSigs == null)
				processSigs = other.processSigs;
			else
				processSigs.addAll(other.processSigs);
		}
	}

	public IFile getFile() {

		return file;
	}

	public IPath getJavaPath() {

		return javaPath;
	}

	public IType getJavaType() {

		return javaType;
	}

	/**
	 * Returns whether the given signature is part of this process entity, i.e.
	 * returns true if the use-case/named instance/etc with the given signature
	 * should be processed.
	 * 
	 * @param sig The signature of a process entity (use case, named instance, etc)
	 * @return True if the process entity with the given signature should be processed.
	 */
	public boolean doProcessSig(Signature sig) {

		return processSigs == null || processSigs.contains(sig);
	}

	/**
	 * Gets whether this ProcessEntity has changed since the last build
	 * 
	 * @return
	 */
	public boolean isChanged() {

		return isChanged;
	}

	public void calculateDependencies(SourceModelFactory smFactory, ClassLoader classLoader, SourceFile sourceFile, //
			SourceFileFactory sfFactory, JavaPathData javaPathData) throws BaseSAException, ClassNotFoundException, NamedInstanceNotFoundException, UseCaseException, SecurityException,
			NoSuchMethodException, CoreException, IOException {

		Timer timer = new Timer("ProcessEntity.calculateDependencies for " + javaType.getFullyQualifiedName());
		computeAllUCAndTSModels(smFactory, classLoader, sourceFile, sfFactory, javaPathData);
		getDependentSigs(smFactory, classLoader, sfFactory);
		timer.printExpiredTime();
	}

	public List<UseCaseModel> getUseCaseModels(Signature methodSig) throws JavaModelException, TypeConverterException, NamedInstanceNotFoundException, ClassNotFoundException {

		if (useCaseModels == null) {
			throw new IllegalArgumentException("Must execute calculateDependencies first");
		}
		return useCaseModels.get(methodSig);
	}

	public List<UseCaseModel> getUseCaseModels() {

		List<UseCaseModel> allUCModels = new ArrayList<UseCaseModel>();
		if (useCaseModels != null) {
			for (Entry<Signature, List<UseCaseModel>> entry : useCaseModels.entrySet()) {
				allUCModels.addAll(entry.getValue());
			}
		}
		return allUCModels;
	}

	public List<TestStateModel> getTestStateModels(Signature methodSig) throws JavaModelException, TypeConverterException, NamedInstanceNotFoundException, ClassNotFoundException {

		if (testStateModels == null) {
			throw new IllegalArgumentException("Must execute calculateDependencies first");
		}
		return testStateModels.get(methodSig);
	}

	public List<TestStateModel> getTestStateModels() {

		List<TestStateModel> allTSModels = new ArrayList<TestStateModel>();
		if (testStateModels != null) {
			for (Entry<Signature, List<TestStateModel>> entry : testStateModels.entrySet()) {
				allTSModels.addAll(entry.getValue());
			}
		}
		return allTSModels;
	}

	public UseCaseMetadata getUseCaseMetadata(Signature methodSig) throws JavaModelException, TypeConverterException, NamedInstanceNotFoundException, ClassNotFoundException {

		if (useCaseModels == null) {
			throw new IllegalArgumentException("Must execute calculateDependencies first");
		}
		return ucMetadata.get(methodSig);
	}

	private void computeAllUCAndTSModels(SourceModelFactory smFactory, ClassLoader classLoader, SourceFile sourceFile, //
			SourceFileFactory sfFactory, JavaPathData javaPathData) //
			throws ClassNotFoundException, NamedInstanceNotFoundException, UseCaseException, SecurityException, NoSuchMethodException, CoreException, IOException {

		final Map<Signature, List<UseCaseModel>> _useCaseModels = new HashMap<Signature, List<UseCaseModel>>();
		Map<Signature, String> ucNames = new HashMap<Signature, String>();
		ucMetadata = new HashMap<Signature, UseCaseMetadata>();
		testStateModels = new HashMap<Signature, List<TestStateModel>>();
		try {
			classLoader.loadClass(javaType.getFullyQualifiedName());
		} catch (Throwable e) {
			processSigs = new LinkedHashSet<Signature>(_useCaseModels.keySet());
			useCaseModels = new LinkedHashMap<Signature, List<UseCaseModel>>();
			return;
		}
		SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaType.getCompilationUnit());
		int methodLineNum = -1;
		// ITypeHierarchy typeHierarchy = javaType.newSupertypeHierarchy(null);
		Set<IType> superTypes = EclipseUtils.getSuperTypes(javaType, classLoader, true);

		for (int fieldIndex = 0; fieldIndex < javaType.getFields().length; fieldIndex++) {

			IField field = javaType.getFields()[fieldIndex];
			Signature fieldSig = SignatureTableFactory.instance.getSignature(//
					javaType.getFullyQualifiedName(), field.getElementName());
			Map<String, IAnnotation> annotations = EclipseUtils.getAnnotations(field);
			IAnnotation tsAnnotation = annotations.get(TestState.class.getSimpleName());
			if (tsAnnotation != null && tsAnnotation.exists()) {
				TestStateModel tsModel = new TestStateModel(tsAnnotation.getMemberValuePairs(), fieldSig);
				BasicUtils.mapListAdd(testStateModels, fieldSig, tsModel);
			}
		}
		NamedUseCaseModelFactory nucmFactory = new NamedUseCaseModelFactory(//
				javaType, sourceModel, classLoader, sourceModel.getSourceFile());

		for (int methodIndex = 0; methodIndex < javaType.getMethods().length; methodIndex++) {

			IMethod iMethod = javaType.getMethods()[methodIndex];
			Signature methodSig = sourceModel.getSignature(iMethod);

			// Only process use-cases with signatures identified in the ProcessEntity
			if (doProcessSig(methodSig)) {

				IntHolder startOffset = new IntHolder(-1);
				IntHolder currentLineNumHolder = new IntHolder(methodLineNum);
				List<UseCaseModel> ucModels = computeUCModels(iMethod, methodSig, //
						startOffset, sourceFile, sourceFile, currentLineNumHolder, smFactory, sfFactory, //
						null, superTypes, new HashSet<Signature>(), classLoader, javaPathData, nucmFactory);

				int endOffset = startOffset.getValue() + iMethod.getNameRange().getLength();
				int startLine = sourceFile.getLineNum(startOffset.getValue());
				int endLine = sourceFile.getLineNum(endOffset);
				UseCaseMetadata metadata = new UseCaseMetadata(//
						startLine, startOffset.getValue(), endLine);

				_useCaseModels.put(methodSig, ucModels);
				ucMetadata.put(methodSig, metadata);
				//
				// Set<Signature> sigDepends = new HashSet<Signature>();
				// for (UseCaseModel ucModel : ucModels) {
				// Signature thisSig = ucModel.getSignature();
				//
				// // TODO: Clear all registered dependencies
				// // PersistentDataFactory.getInstance().clearDependencyProcessEntities(thisSig);
				//
				// // Register and add statically-determined (above) dependencies
				// if (ucModel.getDepends() != null) {
				// for (String depend : ucModel.getDepends()) {
				// PersistentDataFactory.getInstance().registerDependency(//
				// PersistentDataFactory.getInstance().getDeclaringSignature(depend), //
				// thisSig, true);
				// }
				// }
				// if (ucModel.getInstance() != null) {
				// PersistentDataFactory.getInstance().registerDependency(//
				// PersistentDataFactory.getInstance().getDeclaringSignature(ucModel.getInstance()),
				// //
				// thisSig, true);
				// }
				// if (ucModel.getConstructor() != null) {
				// PersistentDataFactory.getInstance().registerDependency(//
				// PersistentDataFactory.getInstance().getDeclaringSignature(ucModel.getConstructor()),
				// //
				// thisSig, true);
				// }
				// sigDepends.addAll(PersistentDataFactory.getInstance().getDependencyUseCases(thisSig));
				// }
				// ucSigDepends.put(methodSig, sigDepends);
			}
		}
		// // Sort _processSigs according to intra-class UseCase dependencies
		// Collections.sort(_processSigs, new Comparator<Signature>() {
		//
		// public int compare(Signature sig1, Signature sig2) {
		//
		// boolean sig1DependsOnSig2 = ucSigDepends.get(sig1).contains(sig2);
		// boolean sig2DependsOnSig1 = ucSigDepends.get(sig2).contains(sig1);
		// if (sig1DependsOnSig2 && sig2DependsOnSig1) {
		// throw new CircularDependencyException("Circluar dependencies are not permitted.  " + //
		// "The following two signatures declare dependencies on each-other: " + //
		// sig1.toString() + " and " + sig2.toString());
		// }
		// if (sig1DependsOnSig2)
		// return 1;
		// else if (sig2DependsOnSig1)
		// return -1;
		// else
		// return 0;
		// }
		// });
		processSigs = new LinkedHashSet<Signature>(_useCaseModels.keySet());

		useCaseModels = new LinkedHashMap<Signature, List<UseCaseModel>>();
		Map<String, Signature> nameSigs = new HashMap<String, Signature>();
		for (Signature sig : processSigs) {
			useCaseModels.put(sig, _useCaseModels.get(sig));
			for (UseCaseModel ucModel : _useCaseModels.get(sig)) {
				if (nameSigs.containsKey(ucModel.getName())) {

					ucModel.setError("This UseCase has the same name as the UseCase declared at " + //
							nameSigs.get(ucModel.getName()).toString());
					// ucModel.setName(ucModel.getName() + "$" + UUID.randomUUID());
					// throw new
					// UseCaseException("The following two signatures declare the same name: " + //
					// sig.toString() + " and " + nameSigs.get(ucModel.getName()).toString(), //
					// ucModel.getDeclaredLineNum(), null);
				}
				ucNames.put(sig, ucModel.getName());
				if (ucModel.getName() != null)
					nameSigs.put(ucModel.getName(), sig);
			}
		}
	}

	/**
	 * Gets the UseCaseModels for the given method.
	 * 
	 * @param iMethod
	 * @param method
	 * @param ucSourceStartOffset
	 * @param implSourceFile
	 * @param ucLineNum
	 * @param sourceModel
	 * @param mmFactory
	 * @param calleeUCSig The sig of the parent UC, e.g. super-class UC
	 * @param superTypes the super-types of the type defining the method
	 * @return
	 * @throws TypeConverterException
	 * @throws NamedInstanceNotFoundException
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws CoreException
	 * @throws IOException
	 */
	private List<UseCaseModel> computeUCModels(IMethod iMethod, Signature methodSig, //
			IntHolder ucSourceStartOffset, SourceFile implSourceFile, SourceFile thisSourceFile, IntHolder ucLineNum,//
			SourceModelFactory smFactory, SourceFileFactory sfFactory, Stack<Signature> subUCSigs, //
			Set<IType> superTypes, Set<Signature> processedMethodSigs, ClassLoader classLoader, JavaPathData javaPathData, //
			NamedUseCaseModelFactory nucmFactory) //
			throws TypeConverterException, NamedInstanceNotFoundException, ClassNotFoundException, SecurityException, NoSuchMethodException, CoreException, IOException {

		if (subUCSigs == null)
			subUCSigs = new Stack<Signature>();
		if (processedMethodSigs.contains(methodSig))
			return Collections.emptyList();
		processedMethodSigs.add(methodSig);

		if (subUCSigs != null) {
			for (Signature subUCSig : subUCSigs) {
				PersistentDataFactory.getInstance().registerDependency(methodSig, subUCSig, true, true);
			}
		}

		Map<String, IAnnotation> annotations = EclipseUtils.getAnnotations(iMethod);
		IAnnotation ucAnnotation = annotations.get(Exemplar.class.getSimpleName());
		if (ucAnnotation == null || !ucAnnotation.exists())
			ucAnnotation = annotations.get(UseCase.class.getSimpleName());
		IAnnotation multiUcAnnotation = annotations.get(Exemplars.class.getSimpleName());
		if (multiUcAnnotation == null || !multiUcAnnotation.exists())
			multiUcAnnotation = annotations.get(MultiUseCase.class.getSimpleName());

		Signature inheritedFromSig = null;
		if (!subUCSigs.isEmpty())
			inheritedFromSig = methodSig;

		// Get MultiUseCases
		List<UseCaseModel> ucModels = new ArrayList<UseCaseModel>();
		if (multiUcAnnotation != null && multiUcAnnotation.exists()) {
			int multiUCLineNum = ucLineNum.isLocked() ? ucLineNum.getValue() : //
			thisSourceFile.getLineNum(multiUcAnnotation.getSourceRange().getOffset());
			// SourceModel sourceModel = smFactory.getSourceModel(thisSourceFile,
			// javaType.getCompilationUnit());
			MultiUseCaseModel multiUCModel = new MultiUseCaseModel(//
					multiUcAnnotation.getMemberValuePairs(), //
					methodSig, multiUCLineNum, thisSourceFile, inheritedFromSig, //
					EclipseUtils.isUseCaseAn(multiUcAnnotation), nucmFactory);
			ucModels = multiUCModel.getUseCases();
			ucSourceStartOffset.setValue(multiUcAnnotation.getSourceRange().getOffset());

			// if (EclipseUtils.getClass(javaType, classLoader).isEnum()) {
			// for (UseCaseModel ucModel : ucModels) {
			// ucModel.setError("UseCases on enum types not yet supported.");
			// }
			// }

			// Get UseCases
		} else if (ucAnnotation != null && ucAnnotation.exists()) {
			ucLineNum.setValue(thisSourceFile.getLineNum(ucAnnotation.getSourceRange().getOffset()));
			// SourceModel sourceModel = smFactory.getSourceModel(thisSourceFile,
			// javaType.getCompilationUnit());
			UseCaseModel ucModel = ModelFactory.newUseCaseModel(ucAnnotation.getMemberValuePairs(), //
					methodSig, ucLineNum.getValue(), inheritedFromSig, EclipseUtils.isUseCaseAn(ucAnnotation), nucmFactory);
			ucModels.add(ucModel);
			ucSourceStartOffset.setValue(ucAnnotation.getSourceRange().getOffset());

			// if (EclipseUtils.getClass(javaType, classLoader).isEnum()) {
			// ucModel.setError("UseCases on enum types not yet supported.");
			// }
		}

		// Validate annotations
		if (ucAnnotation != null && ucAnnotation.exists() && //
				multiUcAnnotation != null && multiUcAnnotation.exists()) {
			// throw new SARuntimeException("Cannot define both " + UseCase.class.getName() + //
			// " and " + MultiUseCase.class.getName() + " for a method");
			ucLineNum.setValue(thisSourceFile.getLineNum(ucAnnotation.getSourceRange().getOffset()));
			// SourceModel sourceModel = smFactory.getSourceModel(thisSourceFile,
			// javaType.getCompilationUnit());
			UseCaseModel ucModel = ModelFactory.newUseCaseModel(ucAnnotation.getMemberValuePairs(), //
					methodSig, ucLineNum.getValue(), inheritedFromSig, EclipseUtils.isUseCaseAn(ucAnnotation), nucmFactory);
			ucModels.add(ucModel);
			ucModel.setError("Cannot define both " + UseCase.class.getName() + //
					" and " + MultiUseCase.class.getName() + " for a method");
			ucSourceStartOffset.setValue(ucAnnotation.getSourceRange().getOffset());
			return ucModels;
		}

		// Add super-class and interface use-cases

		Set<IMethod> superMethods = EclipseUtils.getSuperMethods(iMethod, superTypes);
		for (IMethod superMethod : superMethods) {
			IType superMethodType = superMethod.getDeclaringType();

			// Any super-method UCs are treated as belonging to this source file.
			// ucSourceStartOffset.setValue(iMethod.getSourceRange().getOffset());
			// ucLineNum.setValue(sourceFile.getLineNum(iMethod.getSourceRange().getOffset()));
			// ucSourceStartOffset.lock();
			// ucLineNum.lock();
			PersistentDataFactory pdf = PersistentDataFactory.getInstance();
			String superPathStr = pdf.getJavaPathStr(superMethodType.getFullyQualifiedName());
			IFile file = javaPathData.getFile(new Path(superPathStr), superMethodType.getJavaProject().getProject().getWorkspace());
			SourceFile superSourceFile = sfFactory.getSourceFile(file);

			// Add super-method UCs to this method's UCs
			SourceModel superSourceModel = smFactory.getSourceModel(superSourceFile, superMethodType.getCompilationUnit());
			Signature superMethodSig = superSourceModel.getSignature(superMethod);
			PersistentDataFactory.getInstance().registerStaticDependency(methodSig, superMethodSig, true);
			PersistentDataFactory.getInstance().registerStaticDependency(superMethodSig, methodSig, false);
			// PersistentDataFactory.getInstance().registerDependency(//
			// SignatureTableFactory.instance.getSignature(superSourceModel.getMethod(superMethod)),//
			// methodSig, true, true);
			subUCSigs.push(methodSig);

			// Get supertypes of the supertype
			Set<IType> superSuperTypes = EclipseUtils.getSuperTypes(superMethodType, classLoader, true);

			NamedUseCaseModelFactory superNUMCFactory = new NamedUseCaseModelFactory(//
					superMethodType, superSourceModel, classLoader, superSourceModel.getSourceFile());

			addSuperUseCases(methodSig, ucModels, computeUCModels(superMethod, //
					superMethodSig, ucSourceStartOffset, implSourceFile, superSourceFile, ucLineNum, //
					smFactory, sfFactory, subUCSigs, superSuperTypes, processedMethodSigs, classLoader, //
					javaPathData, superNUMCFactory));
		}

		// ucSourceStartOffset.unlock();
		// ucLineNum.unlock();

		return ucModels;
	}

	/**
	 * Adds the given list of use-cases from superclasses to the given list of UseCases from
	 * a given subclass method.
	 * 
	 * @param methodSig The signature of the subclass method
	 * @param ucModels The list of UseCases from a subclass method
	 * @param superUCModels The list of UseCases to add to ucModels from super-class methods.
	 */
	private void addSuperUseCases(Signature methodSig, List<UseCaseModel> ucModels, List<UseCaseModel> superUCModels) {

		for (UseCaseModel superUCModel : superUCModels) {
			UseCaseModel newUCModel = superUCModel.clone(methodSig);
			ucModels.add(newUCModel);
		}
	}

	/**
	 * Gets the sorted set of signatures to process.
	 * 
	 * @return
	 */
	public Set<Signature> getProcessSigs() {

		return processSigs;
	}

	private synchronized Set<Signature> getDependentSigs(SourceModelFactory mmFactory, //
			ClassLoader classLoader, SourceFileFactory sfFactory) throws ClassNotFoundException, NamedInstanceNotFoundException {

		if (dependsSigs == null) {
			dependsSigs = new HashSet<Signature>();

			for (Entry<Signature, List<TestStateModel>> tsModelsEntry : testStateModels.entrySet()) {

				for (TestStateModel tsModel : tsModelsEntry.getValue()) {
					if (tsModel.getValue() != null) {
						try {
							SINType sinType = SINType.newFromRaw(tsModel.getValue());
							TypeConverter<?> tc = TypeConverterFactory.instance.getTypeConverterForValue(sinType);
							tc.registerDepends(sinType, tsModel, classLoader);
						} catch (TypeConverterException tce) {
							tsModel.setError(tce.getMessage());
						}
					}

					// Add the above plus any previously dynamically-determined dependencies
					dependsSigs.addAll(PersistentDataFactory.getInstance().getDependencyUseCases(//
							tsModel.getSignature()));
				}

			}

			for (Entry<Signature, List<UseCaseModel>> ucModelsEntry : useCaseModels.entrySet()) {

				Signature thisSig = ucModelsEntry.getKey();

				for (UseCaseModel ucModel : ucModelsEntry.getValue()) {

					try {
						// Get declared depends
						if (ucModel.getDepends() != null) {
							for (String depend : ucModel.getDepends()) {
								Signature dependSig = PersistentDataFactory.getInstance().getDeclaringSignature(depend);
								if (dependSig != null) {
									PersistentDataFactory.getInstance().registerDependency(dependSig, thisSig, true);
								}
							}
						}
						if (ucModel.getInstance() != null) {

							ISINExpression sinExpr = ucModel.getInstance();
							SINExpressionTC tc = TypeConverterFactory.instance.getSINExpressionTC();
							try {
								tc.registerDepends(sinExpr, ucModel, classLoader);
							} catch (TypeConverterException tce) {
								ucModel.setError(tce.getMessage());
							}
						}
						if (ucModel.getConstructor() != null) {
							PersistentDataFactory.getInstance().registerDependency(//
									PersistentDataFactory.getInstance().getDeclaringSignature(ucModel.getConstructor()), thisSig, true);
						}

						// Get referenced named instance sigs
						if (ucModel.getArgSINTypes() != null) {
							for (SINType sinType : ucModel.getArgSINTypes()) {
								try {
									TypeConverter<?> tc = TypeConverterFactory.instance.getTypeConverterForValue(sinType);
									tc.registerDepends(sinType, ucModel, classLoader);
								} catch (TypeConverterException tce) {
									ucModel.setError(tce.getMessage());
								}
							}
						}
						if (ucModel.getBefore() != null) {
							for (ISINExpression sinExpr : ucModel.getBefore()) {
								SINExpressionTC tc = TypeConverterFactory.instance.getSINExpressionTC();
								try {
									tc.registerDepends(sinExpr, ucModel, classLoader);
								} catch (TypeConverterException tce) {
									ucModel.setError(tce.getMessage());
								}
							}
						}
						if (ucModel.getAfter() != null) {
							for (ISINExpression sinExpr : ucModel.getAfter()) {
								SINExpressionTC tc = TypeConverterFactory.instance.getSINExpressionTC();
								try {
									tc.registerDepends(sinExpr, ucModel, classLoader);
								} catch (TypeConverterException tce) {
									ucModel.setError(tce.getMessage());
								}
							}
						}
						if (ucModel.getExpects() != null) {
							for (ISINExpression sinExpr : ucModel.getExpects()) {
								SINExpressionTC tc = TypeConverterFactory.instance.getSINExpressionTC();
								try {
									tc.registerDepends(sinExpr, ucModel, classLoader);
								} catch (TypeConverterException tce) {
									ucModel.setError(tce.getMessage());
								}
							}
						}
						if (ucModel.getVerify() != null) {
							for (ISINExpression sinExpr : ucModel.getVerify()) {
								SINExpressionTC tc = TypeConverterFactory.instance.getSINExpressionTC();
								try {
									tc.registerDepends(sinExpr, ucModel, classLoader);
								} catch (TypeConverterException tce) {
									ucModel.setError(tce.getMessage());
								}
							}
						}
						if (ucModel.getStubs() != null) {
							for (SaUCStub stub : ucModel.getStubs()) {
								if (!stub.isValid()) {
									if (ucModel.isValid())
										ucModel.setError(stub.getError());
								} else if (stub.getInvokeExpressions() != null) {
									for (SINType stubExpr : stub.getInvokeExpressions()) {
										try {
											TypeConverter<?> tc = TypeConverterFactory.instance.getTypeConverterForValue(stubExpr);
											tc.registerDepends(stubExpr, ucModel, classLoader);
										} catch (TypeConverterException tce) {
											ucModel.setError(tce.getMessage());
										}
									}
									if (stub instanceof MethodStub) {
										ISINExpression matchExpr = ((MethodStub) stub).getMethodMatchExpression();
										try {
											SINExpressionTC tc = new SINExpressionTC();
											tc.registerDepends(matchExpr, ucModel, classLoader);
										} catch (TypeConverterException tce) {
											ucModel.setError(tce.getMessage());
										}
									}
								}
							}
						}
					} catch (TypeConverterException tce) {
						ucModel.setError(tce.getMessage());
					}

					// Add the above plus any previously dynamically-determined dependencies
					dependsSigs.addAll(PersistentDataFactory.getInstance().getDependencyUseCases(ucModel.getSignature()));

				}
			}
		}
		return dependsSigs;
	}

	//
	// public int compareTo(ProcessEntity that) {
	//
	// return Collections.disjoint(this.getDepends(), that.getProcessSigs()) ? 0 : -1;
	// }

	/**
	 * <li>Defaults the values of values of model parameters in the given list of ProcessEntities
	 * where necessary</li><br/>
	 * <li>Sorts the given list according to dependencies.</li><br/>
	 * <li>Sorts the processSigs in each UseCaseModel according to intra-PE dependencies.</li>
	 * 
	 * @param processEntities
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	public static List<ProcessEntity> setDefaultsAndSort(List<ProcessEntity> processEntities, //
			ClassLoader classLoader, SourceModelFactory smFactory, SourceFileFactory sfFactory) throws Exception {

		/** Contains all use-case models keyed by Signature */
		Map<Signature, List<UseCaseModel>> ucModelsBySig = new LinkedHashMap<Signature, List<UseCaseModel>>();

		Map<Signature, ProcessEntity> peBySig = new LinkedHashMap<Signature, ProcessEntity>();

		for (ProcessEntity pe : processEntities) {

			// Add this PE's sigs' UC Models
			if (pe.useCaseModels != null)
				ucModelsBySig.putAll(pe.useCaseModels);
			Signature classSig = SignatureTableFactory.instance.getSignatureForClass(//
					pe.getJavaType().getFullyQualifiedName());
			peBySig.put(classSig, pe);
		}

		for (ProcessEntity pe : processEntities) {

			for (UseCaseModel ucModel : pe.getUseCaseModels()) {

				peBySig.put(ucModel.getSignature(), pe);

				// All UC Models defined on a constructor must define a name.
				// If no name is specified, set a unique internally-generated one.
				if (ucModel.getSignature().isConstructor() && ucModel.getName() == null) {
					String name = NamedInstanceFactory.DEFAULT_CONSTRUCTOR_NAME_PREFIX + BasicUtils.getUUID();
					ucModel.setName(name);
					PersistentDataFactory.getInstance().registerDeclaringSignature(ucModel.getSignature(), name);
				}
			}

			for (UseCaseModel ucModel : pe.getUseCaseModels()) {

				if (ucModel.getSignature().isConstructor()) {
					// Constructors cannot declare instance or constructor
					if (ucModel.getInstance() != null) {
						ucModel.setError("It is invalid to specify instance on a constructor UseCase");
					}
					if (ucModel.getConstructor() != null) {
						ucModel.setError("It is invalid to specify constructor on a constructor UseCase");
					}
					// Default instance to the name of the constructor itself
					// This is required by UCs dependent on the constructor
					ucModel.setInstance(SINExpressionFactory.get(ucModel.getName()));

				} else {

					// Cannot specify more than one of: instance, constructor, depends.
					int numConstructDirectives = 0;
					if (ucModel.getInstance() != null)
						numConstructDirectives++;
					if (ucModel.getConstructor() != null)
						numConstructDirectives++;
					if (ucModel.getDepends() != null && ucModel.getDepends().length > 0)
						numConstructDirectives++;
					if (numConstructDirectives > 1) {
						ucModel.setError("It is invalid to specify more than one of: instance, constructor, depends.");
					} else if (numConstructDirectives == 0) {
						// none of instance, constructor or depends is specified; default the value
						// of constructor.
						try {
							Class<?> ucModelClass = classLoader.loadClass(ucModel.getSignature().getClassName());
							SourceFile sourceFile = sfFactory.getSourceFile(pe.file);

							// if the method is static, no constructor is required
							SourceModel sourceModel = smFactory.getSourceModel(sourceFile, pe.javaType.getCompilationUnit());
							IMethod iMethod = sourceModel.getMethod(ucModel.getSignature());
							if (iMethod == null) {
								ucModel.setError("Error - could not find method");

							} else if (Flags.isStatic(iMethod.getFlags())) {
								ucModel.setConstructor(UseCaseModel.NO_CONSTRUCTOR);

							} else {

								// if there is a single UseCase-annotated constructor, set
								// constructor
								// to the name of this UC (it must have already been assigned a
								// name).
								String lastConstructorName = null;
								int numSingleAnnotatedConstructors = 0;
								for (Constructor<?> constructor : ucModelClass.getDeclaredConstructors()) {
									Signature constructorSig = SignatureTableFactory.instance.getSignature(constructor);
									List<UseCaseModel> constructorUCModels = ucModelsBySig.get(constructorSig);
									if (constructorUCModels != null && constructorUCModels.size() == 1) {
										lastConstructorName = constructorUCModels.get(0).getName();
										numSingleAnnotatedConstructors++;
										PersistentDataFactory.getInstance().registerDependency(//
												constructorSig, ucModel.getSignature(), true);
									}
								}
								if (numSingleAnnotatedConstructors == 1)
									ucModel.setConstructor(lastConstructorName);

								// Otherwise, if there is a no-args constructor, set constructor to
								// UseCaseModel constant DEFAULT_NOARGS_CONSTRUCTOR.
								if (ucModel.getConstructor() == null) {
									for (Constructor<?> constructor : ucModelClass.getDeclaredConstructors()) {
										Signature constructorSig = SignatureTableFactory.instance.getSignature(constructor);
										if (constructorSig.getParamClassNames().length == 0) {
											ucModel.setConstructor(UseCaseModel.DEFAULT_NOARGS_CONSTRUCTOR);
										}
									}
								}

								if (ucModel.getConstructor() == null) {

									ucModel.setError("If none of instance, constructor or depends are specified, " + //
											"the class must either have a single UseCase-annotated constructor or " + //
											" a no-args constructor.");
								}
							}

						} catch (ClassNotFoundException e) {
							ucModel.setError("Could not load class " + ucModel.getSignature().getClassName());
						}
					}
				}
			}
		} // end ProcessEntity loop

		// Handle depends: set instance or constructor according to dependent UC
		for (ProcessEntity pe : processEntities) {
			for (UseCaseModel ucModel : pe.getUseCaseModels()) {
				if (ucModel.isValid() && !ucModel.getSignature().isConstructor() && //
						ucModel.getDepends() != null && ucModel.getDepends().length > 0) {
					// Depends is specified, set instance. Get the UCModel of the last depends:
					String dependName = ucModel.getDepends()[ucModel.getDepends().length - 1];
					setInstanceFromDepends(ucModel, dependName, ucModelsBySig);
				}
			}
		}

		// Sort ProcessEntities and the UseCaseModels within each ProcessEntity
		LinkedHashSet<ProcessEntity> sortedPEs = new LinkedHashSet<ProcessEntity>(processEntities.size());
		Set<ProcessEntity> processedPEs = new HashSet<ProcessEntity>();
		for (ProcessEntity pe : processEntities) {
			addPEToSortedSet(pe, sortedPEs, processedPEs, peBySig, 0, classLoader, smFactory, sfFactory);
			pe.sortProcessSigs();
		}
		assert sortedPEs.size() == processEntities.size();

		return new ArrayList<ProcessEntity>(sortedPEs);
	}

	private static void addPEToSortedSet(ProcessEntity pe, LinkedHashSet<ProcessEntity> sortedPEs, //
			Set<ProcessEntity> processedPEs, Map<Signature, ProcessEntity> peBySig, int recurseCount, //
			ClassLoader classLoader, SourceModelFactory mmFactory, SourceFileFactory sfFactory) throws Exception {

		if (!processedPEs.contains(pe)) {
			processedPEs.add(pe);
			for (Signature dependSig : pe.getDependentSigs(mmFactory, classLoader, sfFactory)) {
				ProcessEntity dependPE = peBySig.get(dependSig);
				if (dependPE != null)
					addPEToSortedSet(dependPE, sortedPEs, processedPEs, peBySig, recurseCount++, //
							classLoader, mmFactory, sfFactory);
			}

			if (!sortedPEs.contains(pe)) {
				sortedPEs.add(pe);
			}
		}
	}

	private static void addSigToSortedSet(Signature sig, Set<Signature> sortedSigs, //
			Set<Signature> processedSigs, Set<Signature> allSigs) {

		if (!processedSigs.contains(sig)) {
			processedSigs.add(sig);

			// Add dependent sigs that are
			Set<Signature> dependSigs = PersistentDataFactory.getInstance().getDependencyUseCases(sig);
			for (Signature dependSig : dependSigs) {
				if (allSigs.contains(dependSig))
					addSigToSortedSet(dependSig, sortedSigs, processedSigs, allSigs);
			}

			if (!sortedSigs.contains(sig)) {
				sortedSigs.add(sig);
			}
		}
	}

	/**
	 * Sorts the linked set of processSigs by intra-dependencies such that sigs
	 * that have dependencies are executed after those on which others are dependent.
	 */
	public void sortProcessSigs() {

		// System.out.println(">>>>>>>>>>>>>Process Entity: " + javaPath.toString());
		// System.out.println(">>>>>>>>>>>>>>>>>>>>>>Unsorted Sigs");
		// for (Signature sig : processSigs) {
		// System.out.println(sig.toString());
		// }
		LinkedHashSet<Signature> sortedSigs = new LinkedHashSet<Signature>(processSigs.size());
		Set<Signature> processedSigs = new HashSet<Signature>();
		for (Signature sig : processSigs) {
			addSigToSortedSet(sig, sortedSigs, processedSigs, processSigs);
		}
		processSigs = sortedSigs;
		// System.out.println(">>>>>>>>>>>>>>>>>>>>>>Sorted Sigs");
		// for (Signature sig : processSigs) {
		// System.out.println(sig.toString());
		// }
	}

	// private static boolean depends(ProcessEntity p1, ProcessEntity p2) throws
	// TypeConverterException, NamedInstanceNotFoundException {
	//
	// return Collections.disjoint(p1.getDependentSigs(), p2.getProcessSigs()) ? false : true;
	// }

	private static void setInstanceFromDepends(UseCaseModel ucModel, String dependName, //
			Map<Signature, List<UseCaseModel>> ucModelsBySig) {

		Signature dependSig = PersistentDataFactory.getInstance().getDeclaringSignature(dependName);
		if (dependSig == null) {
			if (ucModel.getSignature() != null && ucModel.getSignature().getClassName() != null) {
				dependName = BasicUtils.getSimpleClassName(ucModel.getSignature().getClassName()) + //
						"/" + dependName;
				dependSig = PersistentDataFactory.getInstance().getDeclaringSignature(dependName);
			}
		}
		if (dependSig == null) {
			ucModel.setError("No UseCase or NamedInstance found with name \"" + dependName + "\"");
		} else {
			for (UseCaseModel dependsModel : ucModelsBySig.get(dependSig)) {
				if (dependsModel.getName() != null && dependsModel.getName().equals(dependName)) {
					// if constructor or instance are set on the depends, set instance to
					// the value of instance or constructor on depends.
					if (!dependsModel.isValid()) {
						ucModel.setError("Cannot execute: the dependant UseCase \"" + //
								dependName + "\" declared on " + dependSig.toString() + " is invalid.");
					} else if (dependsModel.getInstance() != null) {
						ucModel.setInstance(dependsModel.getInstance());
					} else if (dependsModel.getConstructor() != null) {
						ucModel.setConstructor(dependsModel.getConstructor());
					} else if (dependsModel.getDepends() != null && dependsModel.getDepends().length > 0) {
						String dependDependName = dependsModel.getDepends()[dependsModel.getDepends().length - 1];
						setInstanceFromDepends(ucModel, dependDependName, ucModelsBySig);
					} else {
						// Do nothing; instance is being set in last depends using default
					}
				}
			}
		}
	}

	public void registerNameDeclarations(ClassLoader classLoader, //
			SourceModelFactory smFactory, SourceFile sourceFile) throws Exception {

		Timer timer = new Timer("ProcessEntity.registerNameDeclarations for " + javaType.getFullyQualifiedName());

		// Handle NamedClass
		String className = javaType.getFullyQualifiedName();
		Class<?> clazz;
		try {
			clazz = classLoader.loadClass(className);
		} catch (Throwable e) {
			return;
		}
		IAnnotation namedClassAn = EclipseUtils.getAnnotations(javaType).get(NamedClass.class.getSimpleName());
		if (namedClassAn != null && namedClassAn.exists()) {
			NamedClassModel ncModel = new NamedClassModel(namedClassAn.getMemberValuePairs());
			Signature sig = SignatureTableFactory.instance.getSignature(clazz);
			NamedInstanceFactory.getInstance().addNamedInstance(ncModel.getName(), clazz, sig);
			PersistentDataFactory.getInstance().registerProjectUC(sig);
		}

		// Handle class annotation
		IAnnotation namedInstanceAn = EclipseUtils.getAnnotations(javaType).get(NamedInstance.class.getSimpleName());
		if (namedInstanceAn != null && namedInstanceAn.exists()) {
			Signature classSig = SignatureTableFactory.instance.getSignature(clazz);
			NamedInstanceModel niModel = new NamedInstanceModel(namedInstanceAn.getMemberValuePairs(), classSig);
			PersistentDataFactory.getInstance().registerDeclaringSignature(classSig, niModel.getName());
			PersistentDataFactory.getInstance().registerProjectUC(classSig);
		}
		IAnnotation hasJUnitAn = EclipseUtils.getAnnotations(javaType).get(HasJUnit.class.getSimpleName());
		if (hasJUnitAn != null && hasJUnitAn.exists()) {
			Signature classSig = SignatureTableFactory.instance.getSignature(clazz);
			PersistentDataFactory.getInstance().registerProjectUC(classSig);
		}

		// Handle method annotations
		SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaType.getCompilationUnit());
		NamedUseCaseModelFactory namedUCMFactory = new NamedUseCaseModelFactory(//
				javaType, sourceModel, classLoader, sourceFile);
		for (IMethod iMethod : javaType.getMethods()) {
			// Register NamedInstance declarations
			namedInstanceAn = EclipseUtils.getAnnotations(iMethod).get(NamedInstance.class.getSimpleName());
			if (namedInstanceAn != null && namedInstanceAn.exists()) {
				Signature sig = sourceModel.getSignature(iMethod);
				NamedInstanceModel niModel = new NamedInstanceModel(namedInstanceAn.getMemberValuePairs(), sig);
				PersistentDataFactory.getInstance().registerDeclaringSignature(sig, niModel.getName());
				PersistentDataFactory.getInstance().registerProjectUC(sig);
			}

			// Register instance name declarations on UseCases
			List<UseCaseModel> methodUCs = new ArrayList<UseCaseModel>();
			Signature sig = null;
			IAnnotation mucAn = EclipseUtils.getAnnotations(iMethod).get(Exemplars.class.getSimpleName());
			if (mucAn == null || !mucAn.exists())
				mucAn = EclipseUtils.getAnnotations(iMethod).get(MultiUseCase.class.getSimpleName());
			if (mucAn != null && mucAn.exists()) {
				sig = sourceModel.getSignature(iMethod);
				MultiUseCaseModel mucModel = new MultiUseCaseModel(mucAn.getMemberValuePairs(), sig, -1, //
						sourceFile, null, EclipseUtils.isUseCaseAn(mucAn), namedUCMFactory);
				methodUCs = mucModel.getUseCases();
			}
			IAnnotation ucAn = EclipseUtils.getAnnotations(iMethod).get(Exemplar.class.getSimpleName());
			if (ucAn == null || !ucAn.exists())
				ucAn = EclipseUtils.getAnnotations(iMethod).get(UseCase.class.getSimpleName());
			if (ucAn != null && ucAn.exists()) {
				sig = sourceModel.getSignature(iMethod);
				methodUCs.add(ModelFactory.newUseCaseModel(ucAn.getMemberValuePairs(), sig, -1, null, EclipseUtils.isUseCaseAn(ucAn), namedUCMFactory));
			}
			for (UseCaseModel ucModel : methodUCs) {
				if (ucModel.getName() != null) {
					PersistentDataFactory.getInstance().registerDeclaringSignature(sig, ucModel.getName());
				}
				if (ucModel.getInstanceout() != null) {
					PersistentDataFactory.getInstance().registerDeclaringSignature(sig, ucModel.getInstanceout());
				}
			}
			if (!methodUCs.isEmpty())
				PersistentDataFactory.getInstance().registerProjectUC(sig);
		}

		// Handle field annotations
		for (IField iField : javaType.getFields()) {
			namedInstanceAn = EclipseUtils.getAnnotations(iField).get(NamedInstance.class.getSimpleName());
			if (namedInstanceAn != null && namedInstanceAn.exists()) {
				Field field = clazz.getDeclaredField(iField.getElementName());
				Signature sig = SignatureTableFactory.instance.getSignature(field);
				NamedInstanceModel niModel = new NamedInstanceModel(namedInstanceAn.getMemberValuePairs(), sig);
				PersistentDataFactory.getInstance().registerDeclaringSignature(sig, niModel.getName());
				PersistentDataFactory.getInstance().registerProjectUC(sig);
			}
		}

		timer.printExpiredTime();
	}

	@Override
	public String toString() {

		return "ProcessEntity: " + javaType.getFullyQualifiedName();
	}

	public static Set<IPath> getPaths(Set<ProcessEntity> processEntities) {

		Set<IPath> paths = new HashSet<IPath>();
		for (ProcessEntity pe : processEntities) {
			paths.add(pe.getJavaPath());
		}
		return paths;
	}

	public static class UseCaseMetadata {

		public final int startLine;
		public final int startOffset;
		public final int endLine;

		public UseCaseMetadata(int startLine, int startOffset, int endLine) {

			this.startLine = startLine;
			this.startOffset = startOffset;
			this.endLine = endLine;
		}
	}
}
