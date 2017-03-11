/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.HasJUnit;
import org.sureassert.uc.annotation.IgnoreTestCoverage;
import org.sureassert.uc.annotation.MultiUseCase;
import org.sureassert.uc.annotation.NamedClass;
import org.sureassert.uc.annotation.NamedInstance;
import org.sureassert.uc.annotation.NoAutoRun;
import org.sureassert.uc.annotation.SINType;
import org.sureassert.uc.annotation.UseCase;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.LicenseBreachException;
import com.sureassert.uc.SAException;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.evaluator.JUnitExecutor;
import com.sureassert.uc.evaluator.UseCaseExecutor;
import com.sureassert.uc.evaluator.model.HasJUnitModel;
import com.sureassert.uc.evaluator.model.ModelFactory;
import com.sureassert.uc.evaluator.model.MultiUseCaseModel;
import com.sureassert.uc.evaluator.model.NamedClassModel;
import com.sureassert.uc.evaluator.model.NamedInstanceModel;
import com.sureassert.uc.evaluator.model.SINTypeModel;
import com.sureassert.uc.interceptor.CompileError;
import com.sureassert.uc.interceptor.CompileErrorsException;
import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.interceptor.SourceModel.SourceModelError;
import com.sureassert.uc.interceptor.SourceModelFactory;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.NamedUseCaseModelFactory;
import com.sureassert.uc.internal.ProcessEntity;
import com.sureassert.uc.internal.ProcessEntity.UseCaseMetadata;
import com.sureassert.uc.internal.ProcessEntityFactory;
import com.sureassert.uc.internal.ProjectClassLoaders;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.internal.UseCaseExecutionCommand;
import com.sureassert.uc.internal.UseCaseExecutionDelegate;
import com.sureassert.uc.propertyfile.PropertyFile;
import com.sureassert.uc.runtime.AspectJSigCache;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.CircularDependencyException;
import com.sureassert.uc.runtime.ExecutorResult;
import com.sureassert.uc.runtime.ExecutorResult.Type;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.PersistentDataLoadException;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TestDouble;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.EvaluatorException;
import com.sureassert.uc.runtime.exception.SAUCBuildInterruptedError;
import com.sureassert.uc.runtime.exception.UseCaseException;
import com.sureassert.uc.runtime.model.UseCaseModel;

public class SAUCBuildWorker implements BuildManager {

	private final int maxUseCases = Integer.MAX_VALUE;

	private SourceFileFactory sfFactory;

	private JavaPathData javaPathData;

	private final IProject project;

	public boolean isStandaloneBuild;

	private IResourceDelta delta;

	private IProgressMonitor progressMonitor;

	private IResource resource;

	public SAUCBuildWorker(IProject project, boolean isStandaloneBuild) {

		this.project = project;
		this.isStandaloneBuild = isStandaloneBuild;
	}

	public SAUCBuildWorker(IProject project, boolean isStandaloneBuild, IResource resource, IResourceDelta delta, IProgressMonitor progressMonitor) {

		this.project = project;
		this.isStandaloneBuild = isStandaloneBuild;
		this.resource = resource;
		this.progressMonitor = progressMonitor;
		this.delta = delta;
	}

	public void setProgressMonitor(IProgressMonitor monitor) {

		this.progressMonitor = monitor;
	}

	public void run() {

		if (System.getProperty(SaUCApplicationLaunchShortcut.IS_KILL_BUILD_ENV_VAR_NAME) != null)
			return;

		/*
		 * if (!SaUCPreferences.isLicenseKeyValid()) {
		 * // Display license nag message if its time and restart the nag counter.
		 * if (licenseNagRunner != null && !licenseNagRunner.isAlive()) {
		 * EclipseUtils.displayDialog(LicenseNagRunner.LICENSE_NAG_MESSAGE_TITLE, //
		 * LicenseNagRunner.LICENSE_NAG_MESSAGE, false, IStatus.INFO);
		 * licenseNagRunner = new LicenseNagRunner();
		 * licenseNagRunner.start();
		 * }
		 * }
		 */

		IJavaProject javaProject = EclipseUtils.getJavaProject(resource);
		if (javaProject != null) {
			// try {
			Set<IPath> affectedFiles = (delta == null) ? null : EclipseUtils.getAffectedFiles(delta, "java");

			try {

				// In case runner killed before backup restore...
				WorkspaceProperties.restorePreInitPropFileBackups(//
						EclipseUtils.getWorkspaceLocation(javaProject.getProject().getWorkspace()).getAbsolutePath());
				// Execute SA Builder on affected files
				processJavaProject(new ProjectProcessEntity(javaProject, affectedFiles), //
						new HashSet<ProjectProcessEntity>(), true, progressMonitor);

			} catch (SAUCBuildInterruptedError ie) {
				throw new OperationCanceledException();
			} finally {
				Timer timer = new Timer("Cleanup");

				// Seems to help avoid file locks
				// System.gc();

				if (PersistentDataFactory.getInstance().getNumSystemErrors() > 0) {
					EclipseUtils.displayDialog("Sureassert UC Error", //
							"Warning: Sureassert UC encountered unexpected errors, please check the Eclipse Error Log (Window->Show View->Error Log)", //
							true, IStatus.WARNING);
				}

				if (PersistentDataFactory.getInstance().isLicensedBreached()) {
					EclipseUtils.displayDialog("Sureassert UC License Restriction", //
							"Cannot execute UseCases - the maximum number of UseCase methods permitted by free license has been reached (" + maxUseCases + ").  " + //
									"To continue use, please purchase a commercial license from www.sureassert.com and enter the key " + //
									"in the Sureassert UC Preferences Page (Window->Preferences).", //
							true, IStatus.WARNING);
				}

				if (!isBuildCancelled(progressMonitor)) {
					// Serialize PersistentDataFactory
					PersistentDataFactory.getInstance().serialize(project.getName(), //
							EclipseUtils.getRawPath(project).toFile());
				} else {
					// Re-load old PersistentDataFactory
					try {
						PersistentDataFactory.getInstance().load(project.getName(), //
								EclipseUtils.getRawPath(project).toFile(),//
								javaProject.getProject().getWorkspace().getRoot().getLocation().toFile());
					} catch (PersistentDataLoadException e) {
						EclipseUtils.reportError(e, false);
					}
				}

				timer.printExpiredTime();
			}
		}
	}

	public synchronized void processJavaProject(ProjectProcessEntity projectProcessEntity, //
			Set<ProjectProcessEntity> alreadyProcessed, boolean hasProjectChanged, IProgressMonitor progressMonitor) {

		// if (!isStandaloneBuild) {
		// try {
		// BasicUtils.debug("Starting build...");
		// int numErrors = BuildClient.getInstance().sendMessage(//
		// new StartBuildPPEMessage(new SerializableProjectProcessEntity(projectProcessEntity)));
		// } catch (Exception e) {
		// throw new RuntimeException(e);
		// } finally {
		// BasicUtils.debug("Build complete");
		// }
		// return;
		// }

		// JaCoCoClient jacocoClient = new JaCoCoClient(new SAUCEditorCoverageListenner());
		CoveragePrinter coveragePrinter = new CoveragePrinter(new SAUCEditorCoverageListenner());

		alreadyProcessed.add(projectProcessEntity);
		AspectJSigCache.clear();
		Map<IJavaProject, Set<ProcessEntity>> pEsByProject = null;
		IJavaProject javaProject = projectProcessEntity.getJavaProject();
		Map<IPath, Set<Signature>> affectedFileSigs = projectProcessEntity.getAffectedFileSigs();
		Set<IPath> affectedFiles = null;
		if (affectedFileSigs != null) {
			affectedFiles = affectedFileSigs.keySet();
			if (affectedFiles != null && affectedFiles.isEmpty())
				affectedFiles = null;
		}
		IWorkspace workspace = javaProject.getProject().getWorkspace();
		ProjectClassLoaders projectCLs = null;
		IFile file = null;
		IResource resource = javaProject.getResource();
		long startTime = System.nanoTime();
		sfFactory = new SourceFileFactory();
		javaPathData = new JavaPathData();
		InstrumentationSession jaCoCoSession = new InstrumentationSession(SaUCPreferences.getIsCoverageEnabled());
		boolean fullBuild = (affectedFiles == null);
		Set<ProcessEntity> allProcessEntities = null;
		IProject project = (IProject) resource;
		progressMonitor.beginTask("Processing " + project.getName(), 2000);
		try {
			BasicUtils.debug("-------------------------------------------------");
			BasicUtils.debug("Sureassert UC building project " + project.getName());
			BasicUtils.debug("ProcessEntities: " + projectProcessEntity.toString());

			// Get classloader using classpath of this project
			jaCoCoSession.start();
			projectCLs = ProjectClassLoaders.getClassLoaders(javaProject, affectedFiles == null, jaCoCoSession);
			PersistentDataFactory.getInstance().setCurrentUseCase(null, null, null, null, null, false, null, false);
			PersistentDataFactory.getInstance().setCurrentProject(project.getName(), //
					EclipseUtils.getRawPath(project).toString());
			PropertyFile propertyFile = new PropertyFile(new File(project.getLocation().toFile(), //
					PropertyFile.DEFAULT_PROPERTY_FILENAME));
			propertyFile.loadExtensions(projectCLs.projectCL);
			SourceModelFactory smFactory = new SourceModelFactory(projectCLs);
			UseCaseExecutionDelegate.INSTANCE.init(javaPathData, sfFactory, projectCLs.transformedCL);

			long getPEsStartTime = System.nanoTime();
			// Delete markers for all changed files
			if (hasProjectChanged)
				MarkerUtils.deleteAllMarkers(affectedFiles, project, sfFactory, javaPathData);
			ProcessEntityFactory peFactory = new ProcessEntityFactory(javaPathData, this);
			// Get ProcessEntities
			// Stage 1
			if (fullBuild) {
				ProjectClassLoaders.cleanTransformedDirs(javaProject);
				pEsByProject = peFactory.getProcessEntitiesFullBuild(resource, project, //
						javaProject, projectCLs, hasProjectChanged, smFactory, sfFactory, //
						new SubProgressMonitor(progressMonitor, 1000));
			} else {
				pEsByProject = peFactory.getProcessEntitiesIncBuild(affectedFileSigs, resource, project, //
						javaProject, projectCLs, hasProjectChanged, smFactory, sfFactory, //
						new SubProgressMonitor(progressMonitor, 1000));
			}
			BasicUtils.debug("Got ProcessEntities in " + ((System.nanoTime() - getPEsStartTime) / 1000000) + //
					"ms");
			Set<ProcessEntity> unprocessedEntities = pEsByProject.get(javaProject);
			if (unprocessedEntities == null)
				unprocessedEntities = new HashSet<ProcessEntity>();
			allProcessEntities = new HashSet<ProcessEntity>(unprocessedEntities);

			// Pre-process 1
			for (ProcessEntity processEntity : unprocessedEntities) {
				file = processEntity.getFile();
				SourceFile sourceFile = sfFactory.getSourceFile(file);
				// Delete internal/class level error markers
				MarkerUtils.deleteUCMarkers(sourceFile, -1, 1, null, null);
				MarkerUtils.deleteJUnitMarkers(sourceFile, -1, 1, null, null);

				executeNamedInstances(processEntity, projectCLs.transformedCL, smFactory, sfFactory);
				registerSINTypes(processEntity, projectCLs.transformedCL, smFactory, sfFactory);
			}

			// Set default values in UseCases and sort the PEs according to dependencies
			List<ProcessEntity> currentProcessEntities = new ArrayList<ProcessEntity>(unprocessedEntities);
			// System.out.println(">Unsorted: " + currentProcessEntities.toString());
			currentProcessEntities = ProcessEntity.setDefaultsAndSort(currentProcessEntities, //
					projectCLs.transformedCL, smFactory, sfFactory);
			// System.out.println(">Sorted: " + currentProcessEntities.toString());

			// Process
			// Stage 2
			// Iterate over unprocessed java files

			// jacocoClient.startSession();

			boolean lastPass = false;
			IProgressMonitor processStageMonitor = new SubProgressMonitor(progressMonitor, 1000);
			try {
				while (!currentProcessEntities.isEmpty()) {

					int peIndex = 0;
					processStageMonitor.beginTask("Executing", currentProcessEntities.size());
					for (ProcessEntity processEntity : currentProcessEntities) {
						processStageMonitor.worked(1);
						int percentComplete = (int) (((double) peIndex / (double) currentProcessEntities.size()) * 100);
						file = processEntity.getFile();
						SourceFile sourceFile = sfFactory.getSourceFile(file);
						try {
							if (javaProject != null && javaProject.exists()) {
								processJavaType(processEntity, projectCLs, javaProject, smFactory, //
										sourceFile, processStageMonitor, percentComplete);
							}
							// File processed successfully, remove from list.
							unprocessedEntities.remove(processEntity);

						} catch (NamedInstanceNotFoundException ninfe) {
							if (lastPass) {
								// The last pass is activated when all files remain in the list; on
								// this last pass we must report the error.
								int lineNum = sourceFile.getLineNum(ninfe.getSourceLocation());
								MarkerUtils.addMarker(sfFactory, javaPathData, sourceFile, ninfe.getMessage(), lineNum, //
										IMarker.SEVERITY_ERROR, false);
								unprocessedEntities.remove(processEntity);
							} else {
								// The named instance may be loaded in a subsequent file,
								// leave this file in the list.
							}

						} catch (SAUCBuildInterruptedError e) {
							throw e;
						} catch (LicenseBreachException e) {
							throw e;
						} catch (Throwable e) {
							String msg = e instanceof CircularDependencyException ? "" : "Sureassert UC error: ";
							MarkerUtils.addMarker(sfFactory, javaPathData, sourceFile, msg + BasicUtils.toDisplayStr(e), -1, IMarker.SEVERITY_ERROR, false);

							// iretrievable error, remove from list.
							unprocessedEntities.remove(processEntity);
						}
						peIndex++;
					} // end execute process entity loop

					if (lastPass)
						break;

					if (unprocessedEntities.size() == currentProcessEntities.size()) {
						// Every file resulted in a retry request for the next pass;
						// therefore on the next and last pass they must report their errors.
						lastPass = true;
					}

					// Replace working list with cut-down list
					currentProcessEntities = new ArrayList<ProcessEntity>(unprocessedEntities);
				}
			} finally {
				processStageMonitor.done();
			}

			addSourceModelErrors(smFactory);

		} catch (SAUCBuildInterruptedError e) {
			throw e;
		} catch (Throwable e) {
			if (e instanceof LicenseBreachException) {
				PersistentDataFactory.getInstance().registerLicenseBreached();
			} else if (e instanceof CompileErrorsException) {
				try {
					handleCompileErrors((CompileErrorsException) e, sfFactory);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} else {
				if (e instanceof SAException)
					file = ((SAException) e).getFile();
				EclipseUtils.reportError(e);
				if (file != null) {
					String msg = e instanceof CircularDependencyException ? "" : "Sureassert UC error: ";
					try {
						MarkerUtils.addMarker(sfFactory, javaPathData, sfFactory.getSourceFile(file), msg + BasicUtils.toDisplayStr(e), -1, IMarker.SEVERITY_ERROR, false);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
			// PersistentDataFactory.getInstance().setNewPersistentStore(javaProject.getProject().getName());

		} finally {

			// Add coverage markers

			jaCoCoSession.end(sfFactory, javaPathData, new SubProgressMonitor(progressMonitor, 1000), this);

			// jacocoClient.dumpExecData();
			// jacocoClient.printInfo(sfFactory, javaPathData, new
			// SubProgressMonitor(progressMonitor, 1000), this);
			// coveragePrinter.printInfo(sfFactory, javaPathData, new
			// SubProgressMonitor(progressMonitor, 1000), this, null);

			// Update coverage report
			Set<IPath> checkPaths = javaPathData.getAllCachedFilePaths();
			checkPaths.addAll(EclipseUtils.toPaths(PersistentDataFactory.getInstance().getMarkedFilePaths()));
			if (allProcessEntities != null)
				checkPaths.addAll(ProcessEntity.getPaths(allProcessEntities));
			// new CoverageReporter().reportCoverage(checkPaths, javaPathData, workspace,
			// sfFactory);

			// Clear caches
			NamedInstanceFactory.getInstance().clear();
			PersistentDataFactory.getInstance().clearMarkedFilePaths();
			UseCaseExecutionDelegate.INSTANCE.dispose();
			if (projectCLs != null)
				projectCLs.clear();
			sfFactory = null;
			BasicUtils.debug("-------------------------------------------------");
			BasicUtils.debug("Sureassert UC builder finished \"" + javaProject.getElementName() + //
					"\" in " + ((System.nanoTime() - startTime) / 1000000) + "ms");

			// Get foreign client project process entities that now require building
			if (pEsByProject != null) {
				for (Entry<IJavaProject, Set<ProcessEntity>> projectPEs : pEsByProject.entrySet()) {
					if (!projectPEs.getKey().equals(javaProject)) {
						ProjectProcessEntity ppe = ProjectProcessEntity.newFromProcessEntities(//
								projectPEs.getKey(), projectPEs.getValue());
						if (!alreadyProcessed.contains(ppe))
							processJavaProject(ppe, alreadyProcessed, false, new SubProgressMonitor(progressMonitor, 0));
					}
				}
			}
			progressMonitor.setTaskName("Building workspace"); // occasional Eclipse bug workaround
			progressMonitor.done();
		}
	}

	private void addSourceModelErrors(SourceModelFactory smFactory) {

		for (SourceModel sourceModel : smFactory.getAllCachedSourceModels()) {
			for (SourceModelError error : sourceModel.getErrors()) {
				MarkerUtils.addMarker(sfFactory, javaPathData, sourceModel.getSourceFile(), error.getMessage(), error.getLineNum(), IMarker.SEVERITY_ERROR, false);
			}
		}
	}

	private void handleCompileErrors(CompileErrorsException e, SourceFileFactory sfFactory) throws IOException, CoreException {

		for (CompileError error : e.getCompileErrors()) {
			BasicUtils.debug("Adding compile error marker");
			MarkerUtils.addMarker(sfFactory, javaPathData, sfFactory.getSourceFile(error.getFile()), error.getMsg(), error.getLineNum(), IMarker.SEVERITY_ERROR, false);
		}
	}

	private void registerSINTypes(ProcessEntity processEntity, ClassLoader classLoader, //
			SourceModelFactory smFactory, SourceFileFactory sfFactory) throws Exception {

		IType javaType = processEntity.getJavaType();

		if (EclipseUtils.hasMethodAnnotations(javaType, SINType.class.getSimpleName(), classLoader, false)) {

			SourceFile sourceFile = sfFactory.getSourceFile(processEntity.getFile());
			SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaType.getCompilationUnit());
			for (IMethod iMethod : javaType.getMethods()) {
				IAnnotation sinTypeAn = EclipseUtils.getAnnotations(iMethod).get(SINType.class.getSimpleName());
				if (sinTypeAn != null && sinTypeAn.exists()) {

					AccessibleObject method = sourceModel.getMethod(iMethod, classLoader);
					// if (iMethod.getReturnType() == null ||
					// iMethod.getReturnType().equals("void")) {
					// }
					Signature sig = SignatureTableFactory.instance.getSignature(method);
					SINTypeModel stModel = new SINTypeModel(sinTypeAn.getMemberValuePairs(), sig);
					PersistentDataFactory.getInstance().registerSINType(stModel.getPrefix(), sig);
				}
			}
		}
	}

	private void executeNamedInstances(ProcessEntity processEntity, ClassLoader classLoader, //
			SourceModelFactory smFactory, SourceFileFactory sfFactory) throws Exception {

		IType javaType = processEntity.getJavaType();

		handleNamedInstancesAndOtherSAAns(classLoader, processEntity, smFactory, sfFactory);
		if (EclipseUtils.hasClassAnnotation(javaType, NamedClass.class.getSimpleName())) {
			handleNamedClass(classLoader, processEntity);
		}
	}

	private void processJavaType(ProcessEntity processEntity, ProjectClassLoaders projectCLs, //
			IJavaProject project, SourceModelFactory smFactory, SourceFile sourceFile, //
			IProgressMonitor monitor, int percentComplete) throws Exception {

		IType javaType = processEntity.getJavaType();

		boolean clearRuntimePersistentData = true;
		if (EclipseUtils.hasMethodAnnotations(javaType, Exemplar.class.getSimpleName(), projectCLs.projectCL, true) || //
				EclipseUtils.hasMethodAnnotations(javaType, Exemplars.class.getSimpleName(), projectCLs.projectCL, true) || //
				EclipseUtils.hasMethodAnnotations(javaType, UseCase.class.getSimpleName(), projectCLs.projectCL, true) || //
				EclipseUtils.hasMethodAnnotations(javaType, MultiUseCase.class.getSimpleName(), projectCLs.projectCL, true)) {

			TestDouble testDouble = PersistentDataFactory.getInstance().getClassDoubleForDoubledClassName(//
					javaType.getFullyQualifiedName());

			if (testDouble != null) {
				for (Signature methodSig : processEntity.getProcessSigs()) {
					List<UseCaseModel> ucModels = processEntity.getUseCaseModels(methodSig);
					for (UseCaseModel ucModel : ucModels) {
						ucModel.setError("Cannot define UseCase - class " + javaType.getFullyQualifiedName() + //
								" has TestDouble " + testDouble.getTestDoubleClassName());
					}
				}
			} else {
				handleUseCases(projectCLs.transformedCL, processEntity, project, smFactory, sourceFile, //
						monitor, percentComplete);
				clearRuntimePersistentData = false;
			}
		}

		if (EclipseUtils.hasClassAnnotation(javaType, HasJUnit.class.getSimpleName())) {
			handleJUnits(projectCLs, javaType, sourceFile, smFactory, clearRuntimePersistentData, //
					monitor, project.getProject(), percentComplete);
		}

		if (BasicUtils.isJUnitTestClass(sourceFile.getSource())) {
			handleJUnits2(projectCLs, javaType, sourceFile, monitor, project, percentComplete);
		}
	}

	private void handleNamedClass(ClassLoader classLoader, ProcessEntity processEntity) throws Exception {

		IType javaType = processEntity.getJavaType();
		String className = javaType.getFullyQualifiedName();
		Class<?> clazz = classLoader.loadClass(className);
		IAnnotation namedClassAn = EclipseUtils.getAnnotations(javaType).get(NamedClass.class.getSimpleName());
		if (namedClassAn != null && namedClassAn.exists()) {
			NamedClassModel ncModel = new NamedClassModel(namedClassAn.getMemberValuePairs());
			Signature sig = SignatureTableFactory.instance.getSignature(clazz);
			NamedInstanceFactory.getInstance().addNamedInstance(ncModel.getName(), clazz, sig);
		}
	}

	private void handleNamedInstancesAndOtherSAAns(ClassLoader classLoader, ProcessEntity processEntity, //
			SourceModelFactory smFactory, SourceFileFactory sfFactory) throws Exception {

		// Handle class annotation
		SourceFile sourceFile = sfFactory.getSourceFile(processEntity.getFile());
		String className = processEntity.getJavaType().getFullyQualifiedName();
		Signature classSig = SignatureTableFactory.instance.getSignatureForClass(className);
		Class<?> clazz = classLoader.loadClass(className);
		IType javaType = processEntity.getJavaType();
		IAnnotation namedInstanceAn = EclipseUtils.getAnnotations(javaType).get(NamedInstance.class.getSimpleName());
		if (namedInstanceAn != null && namedInstanceAn.exists()) {
			NamedInstanceModel niModel = new NamedInstanceModel(namedInstanceAn.getMemberValuePairs(), classSig);
			Object instance;
			try {
				Constructor<?> constructor = clazz.getConstructor();
				constructor.setAccessible(true);
				instance = constructor.newInstance();
			} catch (Exception e) {
				throw new TypeConverterException("Cannot annotate class with @" + NamedInstance.class.getSimpleName() + //
						" as there is no default constructor.  Annotate a constructor instead.");
			}
			Signature sig = SignatureTableFactory.instance.getSignature(clazz);
			NamedInstanceFactory.getInstance().addNamedInstance(niModel.getName(), instance, sig);
		}
		
		IAnnotation ignoreCoverageAn = EclipseUtils.getAnnotations(javaType).get(IgnoreTestCoverage.class.getSimpleName());
		if (ignoreCoverageAn != null && ignoreCoverageAn.exists()) {
			PersistentDataFactory.getInstance().registerIgnoreCoverageClass(className);
		}

		// Handle method annotations
		SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaType.getCompilationUnit());
		NamedUseCaseModelFactory nucmFactory = new NamedUseCaseModelFactory(//
				javaType, sourceModel, classLoader, sourceFile);
		List<Integer> ignoreMethodCoverageStartEndLines = new ArrayList<Integer>();
		for (IMethod iMethod : javaType.getMethods()) {

			ignoreCoverageAn = EclipseUtils.getAnnotations(iMethod).get(IgnoreTestCoverage.class.getSimpleName());
			if (ignoreCoverageAn != null && ignoreCoverageAn.exists()) {
				ISourceRange sourceRange = iMethod.getSourceRange();
				int startLine = sourceFile.getLineNum(sourceRange.getOffset());
				int endLine = sourceFile.getLineNum(sourceRange.getOffset() + sourceRange.getLength());
				ignoreMethodCoverageStartEndLines.add(startLine);
				ignoreMethodCoverageStartEndLines.add(endLine);
			}
			
			namedInstanceAn = EclipseUtils.getAnnotations(iMethod).get(NamedInstance.class.getSimpleName());
			if (namedInstanceAn != null && namedInstanceAn.exists()) {
				if (iMethod.getParameterNames().length > 0) {
					throw new TypeConverterException("Cannot annotate method with @" + NamedInstance.class.getSimpleName() + //
							" as the method has parameters.  Annotate the method with @" + UseCase.class.getSimpleName() + //
							" instead.");
				}
				AccessibleObject method = sourceModel.getMethod(iMethod, classLoader);
				method.setAccessible(true);
				Object instance = null;
				if (method instanceof Method && !Modifier.isStatic(((Method) method).getModifiers())) {
					try {
						instance = BasicUtils.newInstance(clazz);
					} catch (Exception e) {
						throw new TypeConverterException("Cannot annotate method with @" + NamedInstance.class.getSimpleName() + //
								" as there is no default constructor and the method is not static.  To resolve annotate the method with @" + //
								UseCase.class.getSimpleName() + " or make it static.");
					}
				}
				Signature sig = SignatureTableFactory.instance.getSignature(method);
				Object retval = method instanceof Method ? //
				((Method) method).invoke(instance) : ((Constructor<?>) method).newInstance();
				NamedInstanceModel niModel = new NamedInstanceModel(namedInstanceAn.getMemberValuePairs(), sig);
				NamedInstanceFactory.getInstance().addNamedInstance(niModel.getName(), retval, sig);
			}

			// Register instance name declarations
			List<UseCaseModel> methodUCs = new ArrayList<UseCaseModel>();
			Signature sig = null;
			IAnnotation mucAn = EclipseUtils.getAnnotations(iMethod).get(Exemplars.class.getSimpleName());
			if (mucAn == null || !mucAn.exists())
				mucAn = EclipseUtils.getAnnotations(iMethod).get(MultiUseCase.class.getSimpleName());
			if (mucAn != null && mucAn.exists()) {
				AccessibleObject method = sourceModel.getMethod(iMethod, classLoader);
				sig = SignatureTableFactory.instance.getSignature(method);
				MultiUseCaseModel mucModel = new MultiUseCaseModel(mucAn.getMemberValuePairs(), sig, -1, //
						sourceFile, null, EclipseUtils.isUseCaseAn(mucAn), nucmFactory);
				methodUCs = mucModel.getUseCases();
			}
			IAnnotation ucAn = EclipseUtils.getAnnotations(iMethod).get(UseCase.class.getSimpleName());
			if (ucAn == null || !ucAn.exists())
				ucAn = EclipseUtils.getAnnotations(iMethod).get(Exemplar.class.getSimpleName());
			if (ucAn != null && ucAn.exists()) {
				AccessibleObject method = sourceModel.getMethod(iMethod, classLoader);
				sig = SignatureTableFactory.instance.getSignature(method);
				methodUCs.add(ModelFactory.newUseCaseModel(ucAn.getMemberValuePairs(), sig, -1, null, //
						EclipseUtils.isUseCaseAn(ucAn), nucmFactory));
			}
			for (UseCaseModel ucModel : methodUCs) {
				if (ucModel.getName() != null) {
					PersistentDataFactory.getInstance().registerDeclaringSignature(sig, ucModel.getName());
				}
			}
		}
		if (!ignoreMethodCoverageStartEndLines.isEmpty())
			PersistentDataFactory.getInstance().registerIgnoreCoverageMethods(className, ignoreMethodCoverageStartEndLines);

		// Handle field annotations
		for (IField iField : javaType.getFields()) {
			namedInstanceAn = EclipseUtils.getAnnotations(iField).get(NamedInstance.class.getSimpleName());
			if (namedInstanceAn != null && namedInstanceAn.exists()) {
				Field field = clazz.getDeclaredField(iField.getElementName());
				field.setAccessible(true);
				Object instance = null;
				if (!Modifier.isStatic(field.getModifiers())) {
					try {
						instance = BasicUtils.newInstance(clazz);
					} catch (Exception e) {
						throw new TypeConverterException("Cannot annotate field with @" + NamedInstance.class.getSimpleName() + //
								" as there is no default constructor and the field is not static.  To resolve add a default constructor or " + //
								" make the field static.");
					}
				}
				Object retval = field.get(instance);
				Signature sig = SignatureTableFactory.instance.getSignature(field);
				NamedInstanceModel niModel = new NamedInstanceModel(namedInstanceAn.getMemberValuePairs(), sig);
				NamedInstanceFactory.getInstance().addNamedInstance(niModel.getName(), retval, sig);
			}
		}
	}

	private void handleJUnits(ProjectClassLoaders projectCLs, IType javaType, //
			SourceFile sourceFile, SourceModelFactory smFactory, boolean clearRuntimePersistentData, //
			IProgressMonitor monitor, IProject project, int percentComplete) throws ClassNotFoundException, IOException, CoreException, TypeConverterException, NamedInstanceNotFoundException {

		if (isBuildCancelled(monitor))
			throw new SAUCBuildInterruptedError();

		String className = javaType.getFullyQualifiedName();
		SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaType.getCompilationUnit());

		int defaultLineNum = -1;

		Map<String, IAnnotation> annotations = EclipseUtils.getAnnotations(javaType);
		IAnnotation hjAnnotation = annotations.get(HasJUnit.class.getSimpleName());
		int startOffset = javaType.getSourceRange().getOffset();
		int startLine = sourceFile.getLineNum(startOffset);
		int endOffset = startOffset + javaType.getSourceRange().getLength();
		int endLine = sourceFile.getLineNum(endOffset);
		MarkerUtils.deleteJUnitMarkers(sourceFile, startLine, endLine, EclipseUtils.getSubtypeRanges(javaType, sourceFile), null);

		if (hjAnnotation != null && hjAnnotation.exists()) {
			Signature clazzSig = SignatureTableFactory.instance.getSignatureForClass(className);
			defaultLineNum = sourceFile.getLineNum(hjAnnotation.getNameRange().getOffset());
			HasJUnitModel hjModel = new HasJUnitModel(hjAnnotation.getMemberValuePairs(), clazzSig);
			ClassLoader classLoader = hjModel.useInstrumentedSource() ? //
			projectCLs.transformedCL : projectCLs.projectCL;

			Signature classSig = SignatureTableFactory.instance.getSignatureForClass(javaType.getFullyQualifiedName());
			PersistentDataFactory.getInstance().registerStaticDependency(classSig, hjModel.getDefinedOnClassSig(), false);

			JUnitExecutor juExecutor = new JUnitExecutor(hjModel, sfFactory, javaPathData);
			List<ExecutorResult> results;
			try {
				results = juExecutor.execute(classLoader, defaultLineNum, //
						sourceFile.getSigLineNums(javaType, sourceModel, classLoader), //
						clearRuntimePersistentData, javaType, monitor, percentComplete, //
						SaUCPreferences.getJUnitExcludeFilter());
			} catch (Exception e) {
				results = new ArrayList<ExecutorResult>();
				results.add(new ExecutorResult(BasicUtils.toDisplayStr(e), Type.ERROR, null, defaultLineNum));
			}

			for (ExecutorResult result : results) {
				if (!UseCaseExecutionDelegate.ignoreResult(result)) {
					int lineNum = result.getErrorLineNum();
					if (result.getClassName() == null) {
						MarkerUtils.addMarker(sfFactory, javaPathData, sourceFile, result.getDescription(), lineNum, EclipseUtils.getSeverity(result), true);
					} else {
						String javaPathStr = PersistentDataFactory.getInstance().getJavaPathStr(result.getClassName());
						if (javaPathStr == null) {
							EclipseUtils.reportError("Could not determine source file for class " + result.getClassName());
						} else {
							IFile file = javaPathData.getFile(new Path(javaPathStr), project.getWorkspace());
							SourceFile resultSourceFile = sfFactory.getSourceFile(file);
							MarkerUtils.addMarker(sfFactory, javaPathData, resultSourceFile, result.getDescription(), lineNum, EclipseUtils.getSeverity(result), true);
						}
					}
				}
			}
		}
	}

	private void handleJUnits2(ProjectClassLoaders projectCLs, IType jUnitType, SourceFile sourceFile, //
			IProgressMonitor monitor, IJavaProject javaProject, int percentComplete) //
			throws ClassNotFoundException, IOException, CoreException, TypeConverterException, NamedInstanceNotFoundException {

		if (isBuildCancelled(monitor))
			throw new SAUCBuildInterruptedError();

		if (EclipseUtils.hasClassAnnotation(jUnitType, NoAutoRun.class.getSimpleName()) || //
				!SaUCPreferences.getIsJUnitAutomationEnabled())
			return;

		ClassLoader classLoader = projectCLs.transformedCL;

		// Delete all markers as all UseCases will be executed
		int startOffset = jUnitType.getSourceRange().getOffset();
		int startLine = sourceFile.getLineNum(startOffset);
		int endOffset = startOffset + jUnitType.getSourceRange().getLength();
		int endLine = sourceFile.getLineNum(endOffset);

		// Exclude sub-types
		List<Range> excludeRanges = EclipseUtils.getSubtypeRanges(jUnitType, sourceFile);
		MarkerUtils.deleteJUnitMarkers(sourceFile, startLine, endLine, excludeRanges, null);

		JUnitExecutor juExecutor = new JUnitExecutor(jUnitType, sfFactory, javaPathData);
		List<ExecutorResult> results;
		try {
			results = juExecutor.execute(classLoader, javaProject, true, monitor, percentComplete, //
					SaUCPreferences.getJUnitExcludeFilter());
		} catch (Exception e) {
			results = new ArrayList<ExecutorResult>();
			results.add(new ExecutorResult(BasicUtils.toDisplayStr(e), Type.ERROR, null));
		}

		for (ExecutorResult result : results) {
			if (!UseCaseExecutionDelegate.ignoreResult(result)) {
				String resultClassName = result.getClassName();
				if (resultClassName == null)
					resultClassName = jUnitType.getFullyQualifiedName();
				int lineNum = result.getErrorLineNum();
				String javaPathStr = PersistentDataFactory.getInstance().getJavaPathStr(resultClassName);
				if (javaPathStr == null) {
					EclipseUtils.reportError("Could not determine source file for class " + resultClassName);
				} else {
					IFile file = javaPathData.getFile(new Path(javaPathStr), javaProject.getProject().getWorkspace());
					SourceFile resultSourceFile = sfFactory.getSourceFile(file);
					MarkerUtils.addMarker(sfFactory, javaPathData, resultSourceFile, result.getDescription(), lineNum, EclipseUtils.getSeverity(result), true);
				}
			}
		}
	}

	/**
	 * Executes all JUnits in the given project
	 * 
	 * @param project
	 * @throws CoreException
	 */
	@SuppressWarnings("unused")
	private void executeAllJUnits(IJavaProject javaProject, ProjectClassLoaders projectCLs, //
			IProgressMonitor monitor, int percentComplete) throws CoreException {

		IProject project = javaProject.getProject();
		IWorkspace workspace = project.getWorkspace();
		Set<IPath> allPaths = EclipseUtils.getAllFiles(project, "java");
		for (IPath path : allPaths) {
			try {
				IFile file = javaPathData.getFile(path, workspace);
				SourceFile sourceFile = sfFactory.getSourceFile(file);
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(path, workspace);
				if (javaUnit.exists() && BasicUtils.isJUnitTestClass(sourceFile.getSource())) {
					for (IType type : javaUnit.getAllTypes()) {
						handleJUnits2(projectCLs, type, sourceFile, monitor, javaProject, percentComplete);
					}
				}

			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		}
	}

	/*
	 * private void executeAffectedJUnits(IJavaProject javaProject, ProjectClassLoaders projectCLs,
	 * //
	 * Set<IPath> affectedPaths, IProgressMonitor monitor) throws CoreException {
	 * 
	 * IProject project = javaProject.getProject();
	 * IWorkspace workspace = project.getWorkspace();
	 * List<String> jUnitClassNames = new ArrayList<String>();
	 * try {
	 * for (IPath path : affectedPaths) {
	 * Set<String> classNames = PersistentDataFactory.getInstance().getClassNames(path.toString());
	 * for (String className : classNames) {
	 * Set<Signature> clientUCs = PersistentDataFactory.getInstance().getClientUseCases(//
	 * SignatureTableFactory.instance.getSignatureForClass(className));
	 * for (Signature clientUC : clientUCs) {
	 * if (clientUC.isClass()) {
	 * // add all class sigs as JUnit classes, not necessarily the case
	 * jUnitClassNames.add(clientUC.getClassName());
	 * }
	 * }
	 * }
	 * }
	 * for (String jUnitClassName : jUnitClassNames) {
	 * String javaPathStr = PersistentDataFactory.getInstance().getJavaPathStr(jUnitClassName);
	 * if (javaPathStr != null) {
	 * Path javaPath = new Path(javaPathStr);
	 * IFile file = javaPathData.getFile(javaPath, workspace);
	 * SourceFile sourceFile = sfFactory.getSourceFile(file);
	 * ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
	 * if (javaUnit.exists() && BasicUtils.isJUnitTestClass(sourceFile.getSource())) {
	 * for (IType type : javaUnit.getAllTypes()) {
	 * handleJUnits2(projectCLs, type, monitor, javaProject);
	 * }
	 * }
	 * 
	 * }
	 * }
	 * 
	 * } catch (Exception e) {
	 * EclipseUtils.reportError(e);
	 * }
	 * 
	 * }
	 */

	private int getStartLineNumIncUseCase(IMethod method, SourceFile sourceFile) throws JavaModelException {

		IAnnotation annotation = EclipseUtils.getAnnotation(method, Exemplar.class.getSimpleName());
		if (annotation == null)
			annotation = EclipseUtils.getAnnotation(method, Exemplars.class.getSimpleName());
		if (annotation == null)
			annotation = EclipseUtils.getAnnotation(method, UseCase.class.getSimpleName());
		if (annotation == null)
			annotation = EclipseUtils.getAnnotation(method, MultiUseCase.class.getSimpleName());
		if (annotation != null)
			return sourceFile.getLineNum(annotation.getNameRange().getOffset());
		else
			return sourceFile.getLineNum(method.getNameRange().getOffset());
	}

	private void handleUseCases(ClassLoader classLoader, ProcessEntity processEntity, IJavaProject project, //
			SourceModelFactory smFactory, SourceFile sourceFile, IProgressMonitor monitor, int percentComplete) throws ClassNotFoundException, IOException, CoreException, SecurityException,
			IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, UseCaseException, NamedInstanceNotFoundException, InvocationTargetException,
			EvaluatorException, NoSuchMethodException, LicenseBreachException {

		//
		// if ((processEntity.getProcessSigs().isEmpty() || (processEntity.getProcessSigs().size()
		// == 1 && processEntity.getProcessSigs().contains(null))) && //
		// BasicUtils.isJUnitTestClass(sourceFile.getSource())) {
		// addMarker(sourceFile, "Cannot define UseCases on a JUnit test class unless \"" + //
		// WorkspacePreferences.instance.getVisitTestClassesOptionName() + "\"" + //
		// " option is enabled in preferences.", -1, IMarker.SEVERITY_ERROR, false);
		// return;
		// }

		IType javaType = processEntity.getJavaType();
		String className = javaType.getFullyQualifiedName();
		Class<?> clazz = classLoader.loadClass(className);

		if (processEntity.isChanged()) {
			// Delete all markers as all UseCases will be executed
			int startOffset = javaType.getSourceRange().getOffset();
			int startLine = sourceFile.getLineNum(startOffset);
			int endOffset = startOffset + javaType.getSourceRange().getLength();
			int endLine = sourceFile.getLineNum(endOffset);

			// Exclude sub-types
			List<Range> excludeRanges = EclipseUtils.getSubtypeRanges(javaType, sourceFile);
			MarkerUtils.deleteUCMarkers(sourceFile, startLine, endLine, excludeRanges, null);
		}

		if (clazz.isInterface())
			return;

		// Instantiate class
		SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaType.getCompilationUnit());
		int currentLineNum = -1;

		try {

			for (Signature methodSig : processEntity.getProcessSigs()) {

				if (isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();

				// NOTE: Runtime persistent data must only be deleted once before all
				// UseCases that pertain to any given signature, as data is keyed
				// by signature, not by specific UseCase.
				boolean clearRuntimePersistentData = true;

				List<UseCaseModel> ucModels = processEntity.getUseCaseModels(methodSig);
				UseCaseMetadata ucMetadata = processEntity.getUseCaseMetadata(methodSig);
				IMethod method = sourceModel.getMethod(methodSig);
				currentLineNum = getStartLineNumIncUseCase(method, sourceFile);

				// Delete current markers on this method
				// currentLineNum = ucMetadata.startLine;
				if (ucMetadata.startOffset > -1) {
					// int startLine = sourceFile.getLineNum(ucMetadata.startOffset);
					int endOffset = method.getSourceRange().getOffset() + method.getSourceRange().getLength();
					int endLine = sourceFile.getLineNum(endOffset);
					MarkerUtils.deleteUCMarkers(sourceFile, currentLineNum, endLine, null, null);
				}

				for (UseCaseModel ucModel : ucModels) {
					Signature superSig = ucModel.getInheritedFromSignature();
					if (superSig != null) {
						// Delete markers from affected superclass/interface UseCase methods
						try {
							String superPathStr = PersistentDataFactory.getInstance().getJavaPathStr(superSig.getClassName());
							Path superPath = new Path(superPathStr);
							IFile superFile = javaPathData.getFile(superPath, project.getProject().getWorkspace());
							ICompilationUnit superUnit = javaPathData.getJavaUnit(superPath, project.getProject().getWorkspace());
							SourceFile superSourceFile = sfFactory.getSourceFile(superFile);
							SourceModel superSourceModel = smFactory.getSourceModel(superSourceFile, superUnit);
							IMethod superMethod = superSourceModel.getMethod(superSig);
							int superLineNum = ucModel.getDeclaredLineNum();
							int endOffset = superMethod.getSourceRange().getOffset() + superMethod.getSourceRange().getLength();
							int endLine = superSourceFile.getLineNum(endOffset);
							MarkerUtils.deleteUCMarkers(superSourceFile, superLineNum, endLine, null, UseCaseExecutor.getSubUCMarkerPostfix(ucModel));
						} catch (Throwable e) {
							EclipseUtils.reportError(e);
						}
					}
				}

				for (UseCaseModel ucModel : ucModels) {

					if (isBuildCancelled(monitor))
						throw new SAUCBuildInterruptedError();

					// Check license
					if (!SaUCPreferences.isLicenseKeyValid() && PersistentDataFactory.getInstance().getNumUseCaseSigs() >= maxUseCases) {
						throw new LicenseBreachException();
					}

					currentLineNum = ucModel.getDeclaredLineNum();
					UseCaseExecutionCommand ucExecCommand = new UseCaseExecutionCommand(ucModel, sourceModel, //
							sourceFile, javaType, project.getProject(), methodSig);
					UseCaseExecutionDelegate.INSTANCE.executeUseCase(ucExecCommand, monitor, percentComplete, //
							clearRuntimePersistentData, true);

					clearRuntimePersistentData = false;
				} // end UseCaseModel loop
			} // end method loop

		} catch (UseCaseException uce) {
			UseCaseExecutionDelegate.handleUCException(project.getProject(), uce, currentLineNum, sourceFile, sfFactory, javaPathData);
		}
	}

	public boolean isBuildCancelled(IProgressMonitor monitor) {

		return (!isStandaloneBuild && PersistentDataFactory.getInstance().isBuildInterrupted()) || //
				monitor.isCanceled();
	}

}
