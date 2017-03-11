/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.core.resources.IContainer;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.MultiUseCase;
import org.sureassert.uc.annotation.UseCase;

import com.sureassert.uc.builder.Range;
import com.sureassert.uc.builder.SAUCBuilder;
import com.sureassert.uc.builder.persistent.Path;
import com.sureassert.uc.internal.ProjectClassLoaders;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ExecutorResult;
import com.sureassert.uc.runtime.ExecutorResult.Type;
import com.sureassert.uc.runtime.PersistentDataFactory;

public class EclipseUtils {

	public static boolean displayingDialog = false;

	private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(//
			DateFormat.SHORT, DateFormat.MEDIUM);

	/**
	 * Gets the names of the annotations on the given type member mapped against their IAnnotation
	 * objects.
	 * NOTE: The Eclipse SDK IMethod.getAnnotation(String) method returns
	 * annotations that report being existent after they've been removed from the source; this
	 * method does not.
	 * 
	 * @param method
	 * @return
	 * @throws JavaModelException
	 */
	public static Map<String, IAnnotation> getAnnotations(IAnnotatable member) throws JavaModelException {

		Map<String, IAnnotation> annotations = new HashMap<String, IAnnotation>();
		if (member.getAnnotations() != null) {
			for (IAnnotation annotation : member.getAnnotations()) {
				annotations.put(annotation.getElementName(), annotation);
			}
		}
		return annotations;
	}

	/**
	 * Gets the annotations on the given type member with the given name.
	 * 
	 * NOTE: The Eclipse SDK IMethod.getAnnotation(String) method returns
	 * annotations that report being existent after they've been removed from the source; this
	 * method does not.
	 * 
	 * @param method
	 * @return
	 * @throws JavaModelException
	 */
	public static IAnnotation getAnnotation(IAnnotatable member, String annotationName) throws JavaModelException {

		try {
			if (member.getAnnotations() != null) {
				for (IAnnotation annotation : member.getAnnotations()) {
					if (annotationName.equals(annotation.getElementName()))
						return annotation;
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Returns whether the given file has java compilation errors
	 * 
	 * @param file
	 * @return
	 * @throws CoreException
	 */
	public static boolean hasJavaErrors(IFile file) throws CoreException {

		IMarker[] javaProblems = file.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, //
				true, IResource.DEPTH_INFINITE);
		for (IMarker javaProblem : javaProblems) {
			if (javaProblem.getAttribute(IMarker.SEVERITY, Integer.MIN_VALUE) == IMarker.SEVERITY_ERROR) {
				reportError(file.getName() + " has errors; not transforming", false);
				return true;
			}
		}
		return false;
	}

	public static boolean hasMethodAnnotations(IType javaType, String annotationName, ClassLoader classLoader, //
			boolean includeSupertypes) throws JavaModelException {

		if (!javaType.exists())
			return false;
		for (IMethod method : javaType.getMethods()) {
			IAnnotation iAnnotation = getAnnotation(method, annotationName);
			if (iAnnotation != null && iAnnotation.exists())
				return true;
		}
		if (includeSupertypes) {
			Class<?> clazz;
			try {
				clazz = EclipseUtils.getClass(javaType, classLoader);
				Set<Class<?>> superClasses = BasicUtils.getSuperClassAndInterfaces(clazz);
				for (Class<?> superClass : superClasses) {
					if (superClass != null && !superClass.getName().equals(Object.class.getName())) {
						IType superType = javaType.getJavaProject().findType(getITypeName(superClass.getName()), //
								(IProgressMonitor) null);
						if (superType != null) {
							if (hasMethodAnnotations(superType, annotationName, classLoader, true))
								return true;
						}
					}
				}
			} catch (ClassNotFoundException e) {
				reportError(e);
				return false;
			}

			// ITypeHierarchy typeHierarchy = javaType.newSupertypeHierarchy(null);
			// IType superclass = typeHierarchy.getSuperclass(javaType);
			// if (superclass != null)
			// return hasMethodAnnotations(superclass, annotationName, true);
		}
		return false;
	}

	public static boolean hasFieldAnnotations(IType javaType, String annotationName) throws JavaModelException {

		if (!javaType.exists())
			return false;
		for (IField field : javaType.getFields()) {
			IAnnotation iAnnotation = getAnnotation(field, annotationName);
			if (iAnnotation != null && iAnnotation.exists())
				return true;
		}
		return false;
	}

	public static boolean hasClassAnnotation(IType javaType, String annotationName) throws JavaModelException {

		if (!javaType.exists())
			return false;
		IAnnotation iAnnotation = getAnnotation(javaType, annotationName);
		return iAnnotation != null && iAnnotation.exists();
	}

	public static int getSeverity(ExecutorResult result) {

		if (result.getType() == Type.ERROR)
			return IMarker.SEVERITY_ERROR;
		else if (result.getType() == Type.WARNING)
			return IMarker.SEVERITY_WARNING;
		else
			return IMarker.SEVERITY_INFO;
	}

	public static Set<IPath> getAllFiles(IResource resource, String extension) throws CoreException {

		Set<IPath> affectedClasses = new LinkedHashSet<IPath>();
		if (resource == null)
			return affectedClasses;
		IPath resourcePath = resource.getFullPath();
		String fileExt = resourcePath.getFileExtension() == null ? null : resourcePath.getFileExtension();
		if (fileExt != null && fileExt.equals(extension)) {
			// String fileName = resourcePath.lastSegment();
			if (!resourcePath.toString().contains("/" + ProjectClassLoaders.TRANSFORMED_SRC_NAME + "/"))
				affectedClasses.add(resourcePath);
		}
		if (resource instanceof IContainer) {
			for (IResource childResource : ((IContainer) resource).members()) {
				affectedClasses.addAll(getAllFiles(childResource, extension));
			}
		}
		return affectedClasses;
	}

	public static Set<IPath> getAffectedFiles(IResourceDelta delta, String extension) {

		Set<IPath> affectedClasses = new LinkedHashSet<IPath>();
		IPath resourcePath = delta.getFullPath();
		String fileExt = resourcePath.getFileExtension() == null ? null : resourcePath.getFileExtension();
		if (extension == null || (fileExt != null && fileExt.equals(extension))) {
			// String fileName = resourcePath.lastSegment();
			affectedClasses.add(resourcePath);
		}
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			affectedClasses.addAll(getAffectedFiles(childDelta, extension));
		}
		return affectedClasses;
	}

	/**
	 * Gets all super-methods of the given method from the given super-types.
	 * 
	 * @param method A method.
	 * @return The list of super-methods for this method.
	 * @throws JavaModelException
	 */
	public static Set<IMethod> getSuperMethods(IMethod method, Set<IType> superTypes) {

		Set<IMethod> superMethods = new LinkedHashSet<IMethod>();
		for (IType superType : superTypes) {
			try {
				IMethod superMethod = superType.getMethod(method.getElementName(), method.getParameterTypes());
				if (superMethod != null && superMethod.exists())
					superMethods.add(superMethod);
			} catch (Throwable e) {
				reportError(e);
			}
		}
		return superMethods;
	}

	/**
	 * Gets the IType name for the given fully-qualified class name.
	 * 
	 * @param className
	 * @return
	 */
	private static String getITypeName(String className) {

		return className.replace("$", ".");
	}

	public static Class<?> getClass(IType type, ClassLoader cl) throws ClassNotFoundException {

		return cl.loadClass(type.getFullyQualifiedName('$'));
	}

	/**
	 * Returns all supertypes of the given type, optionally excluding those without any UseCases
	 * 
	 * @param type
	 * @param classLoader
	 * @param withUCsOnly If true, only returns project types with UseCases
	 * @return
	 * @throws JavaModelException
	 */
	public static Set<IType> getSuperTypes(IType type, ClassLoader classLoader, boolean withUCsOnly) throws JavaModelException {

		Set<IType> superInterfaces = new HashSet<IType>();
		try {
			Class<?>[] superInterfacesCl = EclipseUtils.getClass(type, classLoader).getInterfaces();
			for (Class<?> superInterfaceCl : superInterfacesCl) {
				if (superInterfaceCl != null && !superInterfaceCl.getName().equals(Object.class.getName())) {
					IType superInterface = type.getJavaProject().findType(getITypeName(superInterfaceCl.getName()), //
							(IProgressMonitor) null);
					if (superInterface != null)
						superInterfaces.add(superInterface);
				}
			}
		} catch (ClassNotFoundException e) {
			reportError(e);
		}
		IType superClass = null;
		try {
			Class<?> superClassCl = EclipseUtils.getClass(type, classLoader).getSuperclass();
			if (superClassCl != null && !superClassCl.getName().equals(Object.class.getName()))
				superClass = type.getJavaProject().findType(getITypeName(superClassCl.getName()), (IProgressMonitor) null);
		} catch (ClassNotFoundException e) {
			EclipseUtils.reportError(e);
		}
		// IType superClass = typeHierarchy.getSuperclass(type);
		// IType[] superInterfaces = typeHierarchy.getSuperInterfaces(type);
		Set<IType> superTypes = new LinkedHashSet<IType>();
		if (superClass != null && (!withUCsOnly || hasUseCases(superClass, classLoader)))
			superTypes.add(superClass);
		for (IType superInterface : superInterfaces) {
			if (!withUCsOnly || hasUseCases(superInterface, classLoader))
				superTypes.add(superInterface);
		}
		// Add super-super types
		Set<IType> newSuperTypes = new HashSet<IType>();
		for (IType superType : superTypes) {
			newSuperTypes = getSuperTypes(superType, classLoader, withUCsOnly);
		}
		superTypes.addAll(newSuperTypes);
		return superTypes;
	}

	public static boolean hasUseCases(IType type, ClassLoader classLoader) throws JavaModelException {

		return PersistentDataFactory.getInstance().isAnyProjectClass(type.getFullyQualifiedName()) && //
				(EclipseUtils.hasMethodAnnotations(type, Exemplar.class.getSimpleName(), classLoader, false) || //
						EclipseUtils.hasMethodAnnotations(type, Exemplars.class.getSimpleName(), classLoader, false) || //
						EclipseUtils.hasMethodAnnotations(type, UseCase.class.getSimpleName(), classLoader, false) || //
				EclipseUtils.hasMethodAnnotations(type, MultiUseCase.class.getSimpleName(), classLoader, false));
	}

	public static boolean equals(IMethod m1, IMethod m2) throws JavaModelException {

		if (m1 == m2)
			return true;
		if (m1 == null || m2 == null)
			return false;
		if (!m1.getElementName().equals(m2.getElementName()))
			return false;
		if (!Arrays.deepEquals(m1.getParameterTypes(), m2.getParameterTypes()))
			return false;

		return true;
	}

	/**
	 * Gets the given resource as an IJavaProject, if it is one. Otherwise returns null.
	 * 
	 * @param project
	 * @return
	 */
	public static IJavaProject getJavaProject(IResource resource) {

		boolean isJavaProject;
		try {
			isJavaProject = resource instanceof IProject && ((IProject) resource).hasNature(JavaCore.NATURE_ID);
		} catch (Exception e) {
			return null;
		}
		if (isJavaProject) {

			// Load Java project
			IProject project = (IProject) resource;
			return JavaCore.create(project);
		}
		return null;
	}

	public static File getWorkspaceLocation(IWorkspace workspace) {

		return new File(workspace.getRoot().getRawLocationURI());
	}

	public static IPath getRawPath(IPath path, IProject project) {

		return project.getWorkspace().getRoot().getFile(path).getRawLocation();
	}

	public static IPath getRawPath(IPath path, IWorkspace workspace) {

		return workspace.getRoot().getFile(path).getRawLocation();
	}

	public static IPath getProjectRootedPath(IPath path, IWorkspace workspace) {

		if (path.segmentCount() > 1) {
			IProject project = workspace.getRoot().getProject(path.segment(0));
			if (project.exists())
				return project.getFile(path.removeFirstSegments(1)).getRawLocation();
		}
		return null;
	}

	public static IPath getRawPath(IProject project) {

		return project.getLocation();
	}

	public static void reportError(Throwable exception) {

		reportError(exception, true);
	}

	public static void reportError(Throwable exception, boolean addToErrorLog) {

		reportError("Sureassert UC encountered a system exception", exception, addToErrorLog);
	}

	public static void reportError(String msg) {

		reportError(msg, null);
	}

	public static void reportError(String msg, boolean addToErrorLoad) {

		reportError(msg, null, addToErrorLoad);
	}

	public static void reportError(String msg, Throwable exception) {

		reportError(msg, exception, true);
	}

	public static void reportError(final String msg, Throwable exception, final boolean addToErrorLog) {

		System.err.println(dateFormat.format(new Date()) + ": " + msg + (exception == null ? "" : //
		": " + ExceptionUtils.getFullStackTrace(exception)));
		if (addToErrorLog) {
			try {
				IStatus status = new Status(IStatus.ERROR, SAUCBuilder.PLUGIN_ID, msg, exception);
				SaUCBuilderActivator.getDefault().getLog().log(status);
				PersistentDataFactory.getInstance().registerSystemError();
			} catch (Exception e) {
				System.err.println("Error writing error to Eclipse log: " + ExceptionUtils.getFullStackTrace(e));
			}
		}

		/*
		 * try {
		 * final IStatus warning = new Status(IStatus.WARNING, SAUCBuilder.PLUGIN_ID, 1, suggestion,
		 * null);
		 * IWorkbench workbench = PlatformUI.getWorkbench();
		 * if (workbench != null) {
		 * final Display display = workbench.getDisplay();
		 * if (display != null) {
		 * display.asyncExec(new Runnable() {
		 * 
		 * public void run() {
		 * 
		 * Shell shell = display.getActiveShell();
		 * if (shell != null) {
		 * ErrorDialog.openError(shell, "Sureassert UC Error", msg, warning);
		 * }
		 * }
		 * });
		 * }
		 * }
		 * } catch (Exception e) {
		 * System.err.println("Error creating Eclipse error" + ExceptionUtils.getFullStackTrace(e));
		 * }
		 */
	}

	public static List<IType> getChildTypes(IType javaType) throws JavaModelException {

		List<IType> childTypes = new ArrayList<IType>();
		for (IJavaElement childEl : javaType.getChildren()) {
			if (childEl instanceof IType) {
				childTypes.add((IType) childEl);
				childTypes.addAll(getChildTypes((IType) childEl));
			}
		}
		return childTypes;
	}

	public static List<Range> getSubtypeRanges(IType javaType, SourceFile sourceFile) throws JavaModelException {

		// Exclude sub-types
		List<Range> excludeRanges = new ArrayList<Range>();
		for (IType subtype : getChildTypes(javaType)) {
			if (!subtype.getFullyQualifiedName().equals(javaType.getFullyQualifiedName())) {
				int subtypeStartOffset = subtype.getSourceRange().getOffset();
				int subtypeStartLine = sourceFile.getLineNum(subtypeStartOffset);
				int subtypeEndOffset = subtypeStartOffset + subtype.getSourceRange().getLength();
				int subtypeEndLine = sourceFile.getLineNum(subtypeEndOffset);
				excludeRanges.add(new Range(subtypeStartLine, subtypeEndLine));
			}
		}
		return excludeRanges;
	}

	public static Set<IPath> toPaths(Set<String> pathStrs) {

		Set<IPath> paths = new HashSet<IPath>();
		for (String path : pathStrs) {
			paths.add(new Path(path));
		}
		return paths;
	}

	public static void displayDialog(final String title, final String message, final boolean async, final int iStatus) {

		if (displayingDialog || PersistentDataFactory.getInstance().isStandaloneBuild())
			return;
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (workbench != null) {
				final Display display = workbench.getDisplay();
				if (display != null) {
					displayingDialog = true;
					Runnable runnable = new Runnable() {

						public void run() {

							Shell shell = display.getActiveShell();
							try {
								if (shell != null) {
									if (iStatus == IStatus.INFO)
										MessageDialog.openInformation(shell, title, message);
									else if (iStatus == IStatus.WARNING)
										MessageDialog.openWarning(shell, title, message);
									else
										MessageDialog.openError(shell, title, message);
								}
							} finally {
								displayingDialog = false;
							}
						}
					};
					if (async)
						display.asyncExec(runnable);
					else
						display.syncExec(runnable);
				}
			}
		} catch (Exception e) {
			System.err.println("Could not display dialog: " + ExceptionUtils.getFullStackTrace(e));
		}
	}

	public static void runJob(Job job, boolean showProgress) {

		if (showProgress) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IProgressService iProgressService = PlatformUI.getWorkbench().getProgressService();
			Shell shell = window.getShell();
			iProgressService.showInDialog(shell, job);
		}
		job.schedule();
	}

	public static void registerPartListener(IPartListener2 partListener) {

		IWorkbenchPage page = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			page = window.getActivePage();
		}

		if (page == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			for (int i = 0; i < windows.length; i++) {
				if (windows[i] != null) {
					window = windows[i];
					page = windows[i].getActivePage();
					if (page != null)
						break;
				}
			}
		}

		if (page != null)
			page.addPartListener(partListener);
	}

	public static boolean isUseCaseAn(IAnnotation an) {

		return an.getElementName().endsWith(UseCase.class.getSimpleName());
	}

	public static void refreshWorkbench(final IWorkbench workbench) {

		if (workbench != null) {

			workbench.getDisplay().syncExec(new Runnable() {

				public void run() {

					IDecoratorManager manager = workbench.getDecoratorManager();
					manager.update(CoverageDecorator.COVERAGE_DECORATOR_ID);
				}
			});
		}
	}
}
