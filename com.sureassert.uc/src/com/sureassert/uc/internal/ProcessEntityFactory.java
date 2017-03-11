/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.SAException;
import com.sureassert.uc.builder.BuildManager;
import com.sureassert.uc.builder.ClassNotYetCompiledException;
import com.sureassert.uc.builder.DefaultClassSrcGenerator;
import com.sureassert.uc.builder.DefaultClassSrcGenerator.SrcGenerateException;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.evaluator.model.TestDoubleModel;
import com.sureassert.uc.interceptor.CompileErrorsException;
import com.sureassert.uc.interceptor.SATranslator;
import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.interceptor.SourceModelFactory;
import com.sureassert.uc.internal.SourceFile.SourceFileFactory;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TestDouble;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.SAUCBuildInterruptedError;
import com.sureassert.uc.runtime.model.UseCaseModel;

public class ProcessEntityFactory {

	private final JavaPathData javaPathData;
	private final BuildManager buildManager;

	private final SATranslator translator = new SATranslator();

	private final PersistentDataFactory pdf = PersistentDataFactory.getInstance();

	public ProcessEntityFactory(JavaPathData javaPathData, BuildManager buildManager) {

		this.javaPathData = javaPathData;
		this.buildManager = buildManager;
	}

	/**
	 * If delta is specified, gets all ProcessEntities for the given delta.<br>
	 * If projectResource is specified, gets all ProcessEntities for the given project.
	 * 
	 * @param delta
	 * @param projectResource
	 * @param signatures
	 * @param project
	 * @param javaProject
	 * @return
	 * @throws SAException
	 * @throws CompileErrorsException
	 * @throws Exception
	 */
	public Map<IJavaProject, Set<ProcessEntity>> getProcessEntitiesFullBuild(//
			IResource projectResource, IProject project, IJavaProject javaProject, //
			ProjectClassLoaders projectCLs, boolean hasProjectChanged, SourceModelFactory smFactory, //
			SourceFileFactory sfFactory, IProgressMonitor monitor) throws SAException, CompileErrorsException {

		/*
		 * if (!((SAUCProjectClassLoader) projectCLs.projectCL).isSaUCOnClasspath()) {
		 * EclipseUtils.reportError("Sureassert UC library not on classpath of project " +
		 * project.getName());
		 * return new HashMap<IJavaProject, Set<ProcessEntity>>();
		 * }
		 */

		if (javaProject == null || !javaProject.exists())
			return new HashMap<IJavaProject, Set<ProcessEntity>>();

		IFile file = null;
		try {
			ProcessEntityStore peStore = new ProcessEntityStore();
			Set<IPath> allJavaFilePaths = EclipseUtils.getAllFiles(projectResource, "java");
			removeUncompiledFiles(allJavaFilePaths, javaProject, projectCLs);
			Set<IPath> processJavaFilePaths = new LinkedHashSet<IPath>(allJavaFilePaths);
			IWorkspace workspace = project.getWorkspace();

			int numFiles = processJavaFilePaths.size();
			int stage1Weight = 1; // per file
			int stage2Weight = 50; // per file
			int stage3Weight = numFiles * 1; // one-off
			int stage4Weight = numFiles * 1; // one-off
			int stage5Weight = 2; // per file
			int stage6Weight = numFiles; // one-off
			int stage7Weight = numFiles * 1; // one-off
			int stage8Weight = numFiles * 5; // one-off
			int totalTasks = (stage1Weight * numFiles) + //
					(stage2Weight * numFiles) + //
					(stage3Weight) + //
					(stage4Weight) + //
					(stage5Weight * numFiles) + //
					(stage6Weight) + //
					(stage7Weight) + //
					(stage8Weight);

			monitor.beginTask("Preparing", totalTasks);

			// ==========
			// Full build
			// ==========

			Map<String, Set<Signature>> processedSigsByProjectName = new HashMap<String, Set<Signature>>();

			pdf.clear();
			initBuild(javaProject.getProject(), null);

			Timer timer = new Timer("Add ProcessEntities");
			Set<IPath> removePaths = new HashSet<IPath>();
			boolean transformedAnySourceFiles = false;

			// Stage 1
			// Add ProcessEntities and register test doubles
			// NOTE: TestDoubles MUST be registered before SourceModels are created
			for (IPath javaPath : processJavaFilePaths) {

				monitor.worked(stage1Weight);
				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				file = javaPathData.getFile(javaPath, workspace);
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
				boolean hasJavaErrors = EclipseUtils.hasJavaErrors(file);
				if (javaUnit.exists() && !hasJavaErrors) {
					IType[] javaTypes = javaUnit.getAllTypes();
					boolean addedNewPE = false;
					for (IType javaType : javaTypes) {
						try {
							ProcessEntity processEntity = new ProcessEntity(file, //
									javaPath, javaType, null, hasProjectChanged, projectCLs.projectCL); // foreignSigsByPath.get(javaPath)
							ProcessEntity newPE = peStore.addProcessEntity(javaProject, processEntity);
							if (newPE == processEntity)
								addedNewPE = true;
							if (addedNewPE && EclipseUtils.hasClassAnnotation(javaType, TestDouble.class.getSimpleName())) {
								if (processEntity != null)
									registerTestDouble(projectCLs.projectCL, javaType, javaPath, sfFactory.getSourceFile(file));
							}
						} catch (ClassNotYetCompiledException cnyce) {
						}
					}
				} else {
					// Register deleted class or class with compilation errors
					// if (hasJavaErrors) {
					// translator.copyClasses(sfFactory.getSourceFile(file), javaProject,
					// projectCLs);
					// }
					Set<String> classNames = pdf.getClassNames(javaPath.toString());
					for (String className : classNames) {
						pdf.unregisterAllClassData(className, null);
					}
					removePaths.add(javaPath);
				}
			}

			// Create and write transformed files
			// Stage 2
			int pathIndex = -1;
			for (IPath javaPath : processJavaFilePaths) {

				monitor.worked(stage2Weight);

				pathIndex++;
				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				file = javaPathData.getFile(javaPath, workspace);
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
				boolean hasJavaErrors = EclipseUtils.hasJavaErrors(file);
				if (javaUnit.exists() && !hasJavaErrors) {
					if (hasProjectChanged) {
						transformedAnySourceFiles = true;
						int percentTransformed = (int) (((double) pathIndex / (double) processJavaFilePaths.size()) * 100);
						SourceFile sourceFile = sfFactory.getSourceFile(file);
						SourceModel sourceModel = smFactory.getSourceModel(sourceFile, javaUnit);
						monitor.setTaskName("Sureassert UC: Instrumenting (" + //
								percentTransformed + "%): " + javaUnit.getElementName());
						translator.transformSource(javaUnit, projectCLs, sourceModel);
					}
				}
			}

			// Remove deleted classes from change set
			for (IPath removePath : removePaths) {
				processJavaFilePaths.remove(removePath);
				allJavaFilePaths.remove(removePath);
			}

			// Replace test double classes
			translator.replaceTDClasses(smFactory.getTestDoubleClassNames());

			// Calculate dependencies
			// Stage 3
			monitor.setTaskName("Sureassert UC: Calculating dependencies");
			timer.printExpiredTime();
			timer = new Timer("Calculate dependencies");
			for (Entry<IJavaProject, Set<ProcessEntity>> projectToPEs : peStore.peByProject.entrySet()) {

				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				Set<ProcessEntity> projectPEs = projectToPEs.getValue();
				List<ProcessEntity> currentProcessEntities = new ArrayList<ProcessEntity>(projectPEs);
				for (ProcessEntity processEntity : currentProcessEntities) {
					file = processEntity.getFile();
					SourceFile sourceFile = sfFactory.getSourceFile(file);
					processEntity.registerNameDeclarations(projectCLs.projectCL, smFactory, sourceFile);
				}
			}
			monitor.worked(stage3Weight);

			// Stage 4
			for (Entry<IJavaProject, Set<ProcessEntity>> projectToPEs : peStore.peByProject.entrySet()) {

				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				Set<ProcessEntity> projectPEs = projectToPEs.getValue();
				List<ProcessEntity> currentProcessEntities = new ArrayList<ProcessEntity>(projectPEs);
				for (ProcessEntity processEntity : currentProcessEntities) {
					file = processEntity.getFile();
					SourceFile sourceFile = sfFactory.getSourceFile(file);
					processEntity.calculateDependencies(smFactory, projectCLs.projectCL, sourceFile, sfFactory, javaPathData);
				}
			}
			monitor.worked(stage4Weight);

			// Add dependent ProcessEntities
			// Stage 5
			timer.printExpiredTime();
			timer = new Timer("Add dependent ProcessEntities");
			Map<IJavaProject, Set<ProcessEntity>> addedPEs = new HashMap<IJavaProject, Set<ProcessEntity>>();
			for (IPath javaPath : processJavaFilePaths) {
				monitor.worked(stage5Weight);
				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				if (javaProject != null && javaProject.exists()) {
					file = javaPathData.getFile(javaPath, workspace);
					ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
					IType[] javaTypes = javaUnit.getAllTypes();
					for (IType javaType : javaTypes) {
						try {
							Set<Signature> classSigs = SignatureTableFactory.instance.getAllSignatures(//
									javaType.getFullyQualifiedName(), projectCLs.projectCL);
							for (Signature sig : classSigs) {
								SigDependencies sigDepends = new SigDependencies(sig, javaPath, workspace, //
										processedSigsByProjectName, false, null, javaPathData);

								addDepends(sigDepends, allJavaFilePaths, javaProject, peStore, //
										projectCLs, addedPEs);
							}
						} catch (NoClassDefFoundError ncdfe) {
							EclipseUtils.reportError(ncdfe);
						} catch (ClassNotFoundException cnfe) {
							EclipseUtils.reportError(cnfe);
						}
					}
				}
			}

			// Stage 6
			for (Entry<IJavaProject, Set<ProcessEntity>> projectToAddedPEs : addedPEs.entrySet()) {
				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				Set<ProcessEntity> projectAddedPEs = projectToAddedPEs.getValue();
				for (ProcessEntity processEntity : projectAddedPEs) {
					try {
						file = processEntity.getFile();
						SourceFile sourceFile = sfFactory.getSourceFile(file);
						processEntity.calculateDependencies(smFactory, projectCLs.projectCL, sourceFile, sfFactory, javaPathData);
					} catch (NoClassDefFoundError ncdfe) {
						EclipseUtils.reportError(ncdfe);
					} catch (ClassNotFoundException cnfe) {
						EclipseUtils.reportError(cnfe);
					}
				}
			}
			monitor.worked(stage6Weight);

			// Generate default classes
			// Stage 7
			generateDefaultClasses(projectCLs, javaProject, peStore, sfFactory, smFactory);
			monitor.worked(stage7Weight);

			// Stage 8
			if (transformedAnySourceFiles)
				translator.compileAllTransformed(javaProject, projectCLs, monitor);
			monitor.worked(stage8Weight);

			timer.printExpiredTime();

			return peStore.peByProject;

		} catch (CompileErrorsException cee) {
			throw cee;
		} catch (Exception e) {
			throw new SAException(e, file);
		} finally {
			monitor.done();
		}
	}

	public Map<IJavaProject, Set<ProcessEntity>> getProcessEntitiesIncBuild(//
			Map<IPath, Set<Signature>> affectedFileSigs, //
			IResource projectResource, IProject project, IJavaProject javaProject, //
			ProjectClassLoaders projectCLs, boolean hasProjectChanged, SourceModelFactory smFactory, //
			SourceFileFactory sfFactory, IProgressMonitor monitor) throws Exception {

		Set<IPath> affectedFiles = new HashSet<IPath>(affectedFileSigs.keySet());

		/*
		 * if (!((SAUCProjectClassLoader) projectCLs.projectCL).isSaUCOnClasspath()) {
		 * EclipseUtils.reportError("Sureassert UC library not on classpath of project " +
		 * project.getName());
		 * return new HashMap<IJavaProject, Set<ProcessEntity>>();
		 * }
		 */

		IFile file = null;
		try {

			int numFiles = affectedFiles.size();
			int stage1Weight = hasProjectChanged ? 0 : 1; // per file
			int stage2Weight = hasProjectChanged ? 0 : 50; // per file
			int stage3Weight = 2; // per file
			int stage4Weight = numFiles * 1; // one-off
			int stage5Weight = numFiles * 2; // one-off
			int stage6Weight = numFiles * 1; // one-off
			int stage7Weight = numFiles * 1; // one-off
			int stage8Weight = numFiles * 5; // one-off
			int totalTasks = (stage1Weight * numFiles) + //
					(stage2Weight * numFiles) + //
					(stage3Weight * numFiles) + //
					(stage4Weight) + //
					(stage5Weight) + //
					(stage6Weight) + //
					(stage7Weight) + //
					(stage8Weight);

			monitor.beginTask("Preparing", totalTasks);

			ProcessEntityStore peStore = new ProcessEntityStore();
			Set<IPath> processJavaFilePaths = new LinkedHashSet<IPath>(affectedFiles);
			Map<String, Set<SigDependencies>> sigDependsByClassName = new HashMap<String, Set<SigDependencies>>();
			IWorkspace workspace = project.getWorkspace();
			boolean doCompile = false;

			// Clear all persistent data for changed classes and check for deletions
			if (hasProjectChanged) {

				// Register TestDoubles and determine uncompiled classes
				// Stage 1
				Set<IPath> removePaths = new HashSet<IPath>();
				Set<IPath> addPaths = new HashSet<IPath>();
				for (IPath javaPath : processJavaFilePaths) {
					monitor.worked(stage1Weight);

					file = javaPathData.getFile(javaPath, workspace);
					ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
					if (javaUnit.exists()) {
						IType[] javaTypes = javaUnit.getAllTypes();
						for (IType javaType : javaTypes) {
							String className = javaType.getFullyQualifiedName();

							// If this is *registered* as a test double class (may no longer be),
							// add the doubled class to the build
							TestDouble td = pdf.getClassDoubleForDoubledClassName(className);
							if (td != null && isCompiled(projectCLs.projectCL, td.getTestDoubleClassName())) {
								Path addPath = new Path(pdf.getJavaPathStr(td.getTestDoubleClassName()));
								affectedFiles.add(addPath);
								addPaths.add(addPath);
							}
							// If this is *registered* as a doubled class (may no longer be), add
							// the test double class to the build
							td = pdf.getClassDoubleForTDClassName(className);
							if (td != null && isCompiled(projectCLs.projectCL, td.getDoubledClassName())) {
								addPaths.add(new Path(pdf.getJavaPathStr(td.getDoubledClassName())));
								Path addPath = new Path(pdf.getJavaPathStr(td.getDoubledClassName()));
								affectedFiles.add(addPath);
								addPaths.add(addPath);
							}

							// Unregister all statically-determined data pertaining to this class
							pdf.unregisterStaticClassData(className, projectCLs.projectCL);

							pdf.registerProjectClass(javaPath.toString(), className, javaType.getElementName());

							// Check if a class has been compiled yet
							try {
								projectCLs.projectCL.loadClass(javaType.getFullyQualifiedName());

								// Register TestDouble
								// NOTE: TestDoubles MUST be registered before SourceModels are
								// created
								if (EclipseUtils.hasClassAnnotation(javaType, TestDouble.class.getSimpleName())) {
									td = registerTestDouble(//
											projectCLs.projectCL, javaType, javaPath, sfFactory.getSourceFile(file));
									if (td != null) {
										// Add doubled class
										Path addPath = new Path(pdf.getJavaPathStr(td.getDoubledClassName()));
										affectedFiles.add(addPath);
										addPaths.add(addPath);
									}
								}
							} catch (Throwable e) {
								BasicUtils.debug(e.getClass().getSimpleName() + " encountered - removing source file " + javaPath.toString());
								removePaths.add(javaPath);
							}
						}
					} // end if javaUnit exists
					else {
						// Check for deleted test double class
						TestDouble deletedTestDouble = pdf.getClassDoubleForJavaPath(javaPath.toString());
						if (deletedTestDouble != null) {
							BasicUtils.debug("Removing class double " + deletedTestDouble.getTestDoubleClassName());
							// Add doubled class to force recompile of original
							String doubledJavaPath = pdf.getJavaPathStr(deletedTestDouble.getDoubledClassName());
							if (doubledJavaPath != null) {
								Path addPath = new Path(doubledJavaPath);
								affectedFiles.add(addPath);
								addPaths.add(addPath);
							}
							// Unregister all statically-determined data pertaining to this class
							pdf.unregisterStaticClassData(deletedTestDouble.getTestDoubleClassName(), //
									projectCLs.projectCL);
						}
					}
				}
				processJavaFilePaths.addAll(addPaths);

				// Transform source and register removed paths
				// Stage 2
				int pathIndex = -1;
				for (IPath javaPath : processJavaFilePaths) {
					monitor.worked(stage2Weight);

					pathIndex++;
					file = javaPathData.getFile(javaPath, workspace);
					ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
					if (javaUnit.exists()) {
						if (!removePaths.contains(javaPath)) {

							// Transform and re-compile changed classes
							// Add ProcessEntities
							int percentTransformed = (int) (((double) pathIndex / (double) processJavaFilePaths.size()) * 100);
							monitor.setTaskName("Sureassert UC: Instrumenting (" + //
									percentTransformed + "%): " + javaUnit.getElementName());

							file = javaPathData.getFile(javaPath, workspace);
							boolean hasJavaErrors = EclipseUtils.hasJavaErrors(file);
							if (javaUnit.exists() && !hasJavaErrors) {
								SourceFile sourceFile = sfFactory.getSourceFile(file);
								// Create and write transformed file
								translator.transformSource(javaUnit, //
										projectCLs, smFactory.getSourceModel(sourceFile, javaUnit));
							} else {
								// Register deleted class or class with compilation errors
								// if (hasJavaErrors) {
								// translator.copyClasses(sfFactory.getSourceFile(file),
								// javaProject, projectCLs);
								// }
								Set<String> classNames = pdf.getClassNames(javaPath.toString());
								for (String className : classNames) {
									pdf.unregisterAllClassData(className, null);
								}
								removePaths.add(javaPath);
							}
						}

					} else {
						// Register deleted class
						Set<String> classNames = pdf.getClassNames(javaPath.toString());
						for (String className : classNames) {
							pdf.unregisterAllClassData(className, null);
						}
						removePaths.add(javaPath);
					}
				}

				// Remove deleted classes from change set
				for (IPath removePath : removePaths) {
					processJavaFilePaths.remove(removePath);
					affectedFiles.remove(removePath);
				}

				// Replace test double classes
				translator.replaceTDClasses(smFactory.getTestDoubleClassNames());

				doCompile = true;

				// Register changed classes
				for (IPath javaPath : processJavaFilePaths) {
					file = javaPathData.getFile(javaPath, workspace);
					ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
					IType[] javaTypes = javaUnit.getAllTypes();
					for (IType javaType : javaTypes) {
						pdf.registerProjectClass(javaPath.toString(), //
								javaType.getFullyQualifiedName(), javaType.getElementName());
					}
				}
			} // end if project has changed

			// Get ProcessEntities
			// Stage 3
			monitor.setTaskName("Sureassert UC: Calculating dependencies");
			for (IPath javaPath : processJavaFilePaths) {
				monitor.worked(stage3Weight);

				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				BasicUtils.debug("Fetching process entities for " + javaPath.toString());
				file = javaPathData.getFile(javaPath, workspace);
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
				IType[] javaTypes = javaUnit.getAllTypes();
				Set<Signature> processSigs = affectedFileSigs.get(javaPath);
				// Get all signatures in this file. Sigs in the same file don't count as client
				// sigs.
				// If they did, clients of the internal clients wouldn't get added as the internal
				// client will already have been processed.
				if (processSigs == null) {
					// Add all sigs from all classes in java file
					processSigs = new LinkedHashSet<Signature>();
					for (IType javaType : javaTypes) {
						String className = javaType.getFullyQualifiedName();
						Set<Signature> classSigs = SignatureTableFactory.instance.getAllSignatures(//
								className, projectCLs.projectCL);
						processSigs.addAll(classSigs);
					}
				}

				Map<String, Set<Signature>> processedSigsByProjectName = new HashMap<String, Set<Signature>>();
				for (Signature processSig : processSigs) {
					// Add the dependencies of every signature
					SigDependencies sigDepends = new SigDependencies(processSig, javaPath, //
							workspace, processedSigsByProjectName, hasProjectChanged, processSigs, javaPathData);
					BasicUtils.mapSetAdd(sigDependsByClassName, processSig.getClassName(), sigDepends);
				}
			}

			// Add ProcessEntities
			// Stage 4
			for (Entry<String, Set<SigDependencies>> sigDependsEntry : sigDependsByClassName.entrySet()) {

				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();

				String className = sigDependsEntry.getKey();
				Set<SigDependencies> sigDependsSet = sigDependsEntry.getValue();
				if (!sigDependsSet.isEmpty()) {
					// NOTE: All SigDependencies in set are for same class and must have same
					// javaUnit and type
					SigDependencies sigDepends0 = sigDependsSet.iterator().next();
					file = sigDepends0.file;
					Set<Signature> sigs = SigDependencies.getSigs(sigDependsSet);

					// Add the ProcessEntity for this javaType
					boolean isChangedFile = hasProjectChanged && processJavaFilePaths.contains(sigDepends0.path);
					IType javaType = getType(sigDepends0.javaUnit, className);

					try {
						ProcessEntity processEntity = new ProcessEntity(sigDepends0.file, //
								sigDepends0.path, javaType, sigs, isChangedFile, projectCLs.projectCL);
						peStore.addProcessEntity(javaProject, processEntity);
					} catch (ClassNotYetCompiledException cnyce) {
					}
				}
			}
			monitor.worked(stage4Weight);

			// Calculate dependencies
			// Stage 5
			for (Entry<IJavaProject, Set<ProcessEntity>> projectToPEs : peStore.peByProject.entrySet()) {
				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				Set<ProcessEntity> projectPEs = projectToPEs.getValue();
				List<ProcessEntity> currentProcessEntities = new ArrayList<ProcessEntity>(projectPEs);
				for (ProcessEntity processEntity : currentProcessEntities) {
					file = processEntity.getFile();
					SourceFile sourceFile = sfFactory.getSourceFile(file);
					processEntity.registerNameDeclarations(projectCLs.projectCL, smFactory, sourceFile);
				}
			}
			for (Entry<IJavaProject, Set<ProcessEntity>> projectToPEs : peStore.peByProject.entrySet()) {

				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				Set<ProcessEntity> projectPEs = projectToPEs.getValue();
				List<ProcessEntity> currentProcessEntities = new ArrayList<ProcessEntity>(projectPEs);
				for (ProcessEntity processEntity : currentProcessEntities) {
					file = processEntity.getFile();
					SourceFile sourceFile = sfFactory.getSourceFile(file);
					processEntity.calculateDependencies(smFactory, projectCLs.projectCL, sourceFile, sfFactory, javaPathData);
				}
			}
			monitor.worked(stage5Weight);

			// Add dependent ProcessEntities
			Map<IJavaProject, Set<ProcessEntity>> addedPEs = new HashMap<IJavaProject, Set<ProcessEntity>>();
			for (Entry<String, Set<SigDependencies>> sigDependsEntry : sigDependsByClassName.entrySet()) {

				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();

				Set<SigDependencies> sigDependsSet = sigDependsEntry.getValue();

				// Add all dependent ProcessEntities and their dependencies, under this project
				Map<String, Set<Signature>> processedSigsByProjectName = new HashMap<String, Set<Signature>>();
				for (SigDependencies sigDepends : sigDependsSet) {

					file = sigDepends.file;

					// Add calculated dependencies and depends of clients to this sig
					sigDepends.addDependsAndClients(workspace, processedSigsByProjectName, hasProjectChanged, //
							SigDependencies.getSigs(sigDependsSet), javaPathData, true);

					// Add dependencies
					addDepends(sigDepends, processJavaFilePaths, javaProject, peStore, //
							projectCLs, addedPEs);
				}

				if (hasProjectChanged && !pdf.isStandaloneBuild()) {
					// Add all client ProcessEntities and their dependencies, under the client's
					// project
					for (SigDependencies sigDepends : sigDependsSet) {
						file = sigDepends.file;
						addClients(sigDepends, processJavaFilePaths, peStore, projectCLs, addedPEs);
					}
				}
			}
			for (Entry<IJavaProject, Set<ProcessEntity>> projectToAddedPEs : addedPEs.entrySet()) {
				if (buildManager.isBuildCancelled(monitor))
					throw new SAUCBuildInterruptedError();
				Set<ProcessEntity> projectAddedPEs = projectToAddedPEs.getValue();
				for (ProcessEntity processEntity : projectAddedPEs) {
					file = processEntity.getFile();
					SourceFile sourceFile = sfFactory.getSourceFile(file);
					processEntity.calculateDependencies(smFactory, projectCLs.projectCL, sourceFile, sfFactory, javaPathData);
				}
			}
			monitor.worked(stage6Weight);

			// Generate default classes
			List<String> genDefaultSrcPaths = generateDefaultClasses(projectCLs, javaProject, peStore, sfFactory, smFactory);
			monitor.worked(stage7Weight);

			if (doCompile) {
				// Compile transformed classes
				translator.compileTransformed(affectedFiles, javaProject, projectCLs, genDefaultSrcPaths, monitor);
			}
			monitor.worked(stage8Weight);
			return peStore.peByProject;

		} catch (CompileErrorsException cee) {
			throw cee;
		} catch (RuntimeException e) {
			throw e;
		} catch (SAException e) {
			throw e;
		} catch (Exception e) {
			throw new SAException(e, file);
		} finally {
			monitor.done();
		}
	}

	private boolean isCompiled(ClassLoader cl, String className) {

		try {
			cl.loadClass(className);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Generates all default classes required by the UseCases defined on the
	 * given set of changed files.
	 * 
	 * @param paramClassName
	 * @param projectCLs
	 * @param javaProject
	 * @return The list of generated source file full paths
	 * @throws TypeConverterException
	 * @throws CoreException
	 */
	private List<String> generateDefaultClasses(ProjectClassLoaders projectCLs, //
			IJavaProject javaProject, ProcessEntityStore peStore, SourceFileFactory sfFactory, SourceModelFactory smFactory) throws IOException, ClassNotFoundException, TypeConverterException,
			CoreException {

		// Generate default classes
		Set<String> genSuperclassNames = new HashSet<String>();
		List<String> genDefaultSrcPaths = new ArrayList<String>();
		if (peStore.peByProject.get(javaProject) != null) {
			for (ProcessEntity pe : peStore.peByProject.get(javaProject)) {
				for (UseCaseModel ucModel : pe.getUseCaseModels()) {
					int paramIndex = 0;
					Signature methodSig = ucModel.getSignature();
					SINType[] argSINTypes = ucModel.getArgSINTypes();
					if (methodSig.getParamClassNames().length == argSINTypes.length) {
						int argIdx = -1;
						for (SINType argType : argSINTypes) {
							argIdx++;
							if (argType.getSINValue().equals("*")) {
								String paramClassName = methodSig.getParamClassNames()[paramIndex];
								if (!genSuperclassNames.contains(paramClassName)) {
									genSuperclassNames.add(paramClassName);

									IType superType = javaProject.findType(paramClassName, new NullProgressMonitor());
									SourceModel sourceModel = null;
									if (superType != null && !superType.isBinary()) {
										ICompilationUnit javaUnit = javaPathData.getJavaUnit(superType.getPath(), //
												javaProject.getProject().getWorkspace());
										IFile file = javaPathData.getFile(superType.getPath(), superType.getJavaProject().getProject().getWorkspace());
										SourceFile sourceFile = sfFactory.getSourceFile(file);
										sourceModel = smFactory.getSourceModel(sourceFile, javaUnit);
									}
									try {
										generateDefaultClass(methodSig, paramClassName, sourceModel, //
												projectCLs, javaProject, genDefaultSrcPaths);
									} catch (SrcGenerateException e) {
										EclipseUtils.reportError(e, false);
										ucModel.setError("Cannot generate default class for argument " + (argIdx + 1) + //
												" - argument type must be interface or abstract class");
									}
								}
							}

							paramIndex++;
						}
					}
					if (ucModel.getDefaultImpls() != null) {
						for (String defaultImplClassName : ucModel.getDefaultImpls()) {
							if (!genSuperclassNames.contains(defaultImplClassName)) {
								genSuperclassNames.add(defaultImplClassName);

								IType superType = javaProject.findType(defaultImplClassName, new NullProgressMonitor());
								SourceModel sourceModel = null;
								if (superType != null && !superType.isBinary()) {
									ICompilationUnit javaUnit = javaPathData.getJavaUnit(superType.getPath(), //
											javaProject.getProject().getWorkspace());
									IFile file = javaPathData.getFile(superType.getPath(), superType.getJavaProject().getProject().getWorkspace());
									SourceFile sourceFile = sfFactory.getSourceFile(file);
									sourceModel = smFactory.getSourceModel(sourceFile, javaUnit);
								}
								try {
									generateDefaultClass(methodSig, defaultImplClassName, sourceModel, //
											projectCLs, javaProject, genDefaultSrcPaths);
								} catch (SrcGenerateException e) {
									ucModel.setError("Cannot generate default class for " + defaultImplClassName + //
											" - specified class must be interface or abstract");
								}
							}
						}
					}
				}
			}
		}
		return genDefaultSrcPaths;
	}

	/**
	 * Generates a default class
	 * 
	 * @param methodSig
	 * @param genSuperClassName
	 * @param projectCLs
	 * @param javaProject
	 * @param genDefaultSrcPaths
	 * @return The class name of the generated class
	 * @throws JavaModelException
	 * @throws SrcGenerateException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private String generateDefaultClass(Signature methodSig, String genSuperClassName, SourceModel superSourceModel, ProjectClassLoaders projectCLs, //
			IJavaProject javaProject, List<String> genDefaultSrcPaths) throws SrcGenerateException, JavaModelException, IOException, ClassNotFoundException {

		// If class is project class, register use-case as a client
		// so default class is re-created when its
		// interface/superclass changes
		if (pdf.isAnyProjectClass(genSuperClassName)) {
			pdf.registerDependency(//
					SignatureTableFactory.instance.getSignatureForClass(genSuperClassName), //
					methodSig, true);
		}
		// Generate source for default implementation of this class
		DefaultClassSrcGenerator defaultClassGen = new DefaultClassSrcGenerator(genSuperClassName, //
				superSourceModel, projectCLs.projectCL, javaProject);
		String sourceDir = projectCLs.getSourceDirs().iterator().next();
		String transformedSrcDir = projectCLs.getTransformedSourceDir(sourceDir);
		String genSrcFilePath = defaultClassGen.writeSrc(new File(transformedSrcDir));
		genDefaultSrcPaths.add(genSrcFilePath);
		String genClassName = defaultClassGen.getGeneratedClassName();
		String simpleGenClassName = BasicUtils.getSimpleClassName(genClassName);
		pdf.registerProjectClass(genSrcFilePath, genClassName, simpleGenClassName);
		pdf.registerDefaultClass(simpleGenClassName, genClassName, genSuperClassName);
		return genClassName;
	}

	private void removeUncompiledFiles(Set<IPath> allJavaFilePaths, IJavaProject javaProject, ProjectClassLoaders projectCLs) throws JavaModelException {

		// Check if a class has been compiled yet
		Set<IPath> removePaths = new HashSet<IPath>();
		IWorkspace workspace = javaProject.getProject().getWorkspace();
		for (IPath javaPath : allJavaFilePaths) {
			if (javaProject != null && javaProject.exists()) {
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(javaPath, workspace);
				if (javaUnit.exists()) {
					IType[] javaTypes = javaUnit.getAllTypes();
					for (IType javaType : javaTypes) {
						try {
							projectCLs.projectCL.loadClass(javaType.getFullyQualifiedName());
						} catch (Throwable e) {
							EclipseUtils.reportError(e, false);
							removePaths.add(javaPath);
						}
					}
				}
			}
		}
		for (IPath removePath : removePaths) {
			BasicUtils.debug("Removing path " + removePath.toString());
			allJavaFilePaths.remove(removePath);
		}
	}

	private TestDouble registerTestDouble(ClassLoader classLoader, IType javaType, IPath javaPath, SourceFile sourceFile) throws Exception {

		String className = javaType.getFullyQualifiedName();
		IAnnotation tdAn = EclipseUtils.getAnnotations(javaType).get(TestDouble.class.getSimpleName());
		if (tdAn != null && tdAn.exists()) {
			TestDoubleModel tdModel = new TestDoubleModel(tdAn.getMemberValuePairs(), //
					className, javaType, classLoader);
			TestDouble testDouble = tdModel.newTestDouble();
			pdf.addClassDouble(testDouble, //
					SignatureTableFactory.instance.getSignature(className, Signature.ANY_MEMBER_NAME));
			pdf.registerProjectClass(javaPath.toString(), className, //
					javaType.getElementName());
			return testDouble;
		}
		return null;
	}

	private IType getType(ICompilationUnit unit, String className) throws JavaModelException {

		for (IType type : unit.getAllTypes()) {
			if (type.getFullyQualifiedName().equals(className))
				return type;
		}
		return null;
	}

	private void initBuild(IProject project, Set<IPath> affectedFiles) {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		if (affectedFiles == null) {
			try {
				affectedFiles = EclipseUtils.getAllFiles(project, "java");
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		}
		for (IPath path : affectedFiles) {
			try {
				ICompilationUnit javaUnit = javaPathData.getJavaUnit(path, workspace);
				if (javaUnit.exists()) {
					for (IType javaType : javaUnit.getAllTypes()) {
						pdf.registerProjectClass(path.toString(), //
								javaType.getFullyQualifiedName(), javaType.getElementName());
					}
				}
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			}
		}
	}

	private void addDepends(SigDependencies sigDepends, Set<IPath> deltaJavaFilePaths, //
			IJavaProject registerUnderProject, ProcessEntityStore peStore, //
			ProjectClassLoaders classLoaders, Map<IJavaProject, Set<ProcessEntity>> addedPEs) throws JavaModelException {

		Set<SigDependencies> removeDepends = new HashSet<SigDependencies>();
		for (SigDependencies dependSigDepends : sigDepends.dependSigDepends) {
			if (dependSigDepends.javaUnit.exists()) {
				for (IType javaType : dependSigDepends.javaUnit.getAllTypes()) {

					try {
						ProcessEntity processEntity = new ProcessEntity(dependSigDepends.file, //
								dependSigDepends.path, javaType, BasicUtils.newSet(dependSigDepends.sig), false, //
								classLoaders.projectCL);

						BasicUtils.mapSetAdd(addedPEs, registerUnderProject, //
								peStore.addProcessEntity(registerUnderProject, processEntity));
					} catch (ClassNotYetCompiledException cnyce) {
					}
				}

				// Add depends of dependSigDepends
				addDepends(dependSigDepends, deltaJavaFilePaths, registerUnderProject, //
						peStore, classLoaders, addedPEs);
			} else {
				removeDepends.add(dependSigDepends);
			}
		}
		for (SigDependencies removeDepend : removeDepends) {
			sigDepends.clientSigDepends.remove(removeDepend);
		}
	}

	private void addClients(SigDependencies sigDepends, Set<IPath> deltaJavaFilePaths, //
			ProcessEntityStore peStore, //
			ProjectClassLoaders classLoaders, Map<IJavaProject, Set<ProcessEntity>> addedPEs) throws JavaModelException {

		Set<SigDependencies> removeDepends = new HashSet<SigDependencies>();
		for (SigDependencies clientSigDepends : sigDepends.clientSigDepends) {

			if (clientSigDepends.javaUnit.exists()) {
				for (IType javaType : clientSigDepends.javaUnit.getAllTypes()) {

					try {
						ProcessEntity processEntity = new ProcessEntity(clientSigDepends.file, //
								clientSigDepends.path, javaType, BasicUtils.newSet(clientSigDepends.sig), false, //
								classLoaders.projectCL);

						BasicUtils.mapSetAdd(addedPEs, clientSigDepends.project, //
								peStore.addProcessEntity(clientSigDepends.project, processEntity));
					} catch (ClassNotYetCompiledException cnyce) {
					}
				}

				// Add depends of clientSigDepends
				addDepends(clientSigDepends, deltaJavaFilePaths, clientSigDepends.project, //
						peStore, classLoaders, addedPEs);

				// NOTE: clients are not added recursively -- only clients of the changed files set
				// are
				// added, and their recursive depends.
			} else {
				removeDepends.add(clientSigDepends);
			}
		}
		for (SigDependencies removeDepend : removeDepends) {
			sigDepends.clientSigDepends.remove(removeDepend);
		}
	}

}
