/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.ProjectClassLoaders;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ClassDouble;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.Timer;

public class SATranslator {

	/**
	 * Transforms the given source file and writes to the transformed source directory.
	 * 
	 * @param project
	 * @param file
	 * @throws JavaModelException
	 */
	public void transformSource(ICompilationUnit javaUnit, //
			final ProjectClassLoaders classLoaders, SourceModel sourceModel) throws JavaModelException {

		try {
			if ((sourceModel.getSourceInsertions() == null || sourceModel.getSourceInsertions().isEmpty()) && //
					!sourceModel.containsTDOrDoubledClass())
				return;

			IFile file = sourceModel.getSourceFile().getFile();
			IJavaProject javaProject = javaUnit.getJavaProject();
			File srcFile = EclipseUtils.getRawPath(file.getFullPath(), javaProject.getProject()).toFile();
			final String transformedSrcPath = classLoaders.findTransformedSourceFile(srcFile.getAbsolutePath());
			File transformedSrcFile = new File(transformedSrcPath);
			// FileUtils.copyFile(srcFile, transformedSrcFile);
			String encoding = file.getCharset();
			StringBuffer source = new StringBuffer(sourceModel.getSource());

			// Apply source insertions
			for (SourceInsertion ins : sourceModel.getSourceInsertions()) {

				if (ins instanceof BlockAwareSourceInsertion) {
					if (source.charAt(ins.index) == ';') {
						// loops with no body at all
						source.insert(ins.index + 1, ins.insertString);
						source.deleteCharAt(ins.index);

					} else if (source.charAt(ins.index) != '{') {
						// loops with one-statement body
						source.insert(((BlockAwareSourceInsertion) ins).endBlockindex, '}');
						source.insert(ins.index, '{');
						source.insert(ins.index + 1, ins.insertString);

					} else {
						// block loops {}
						source.insert(ins.index + 1, ins.insertString);
					}

				} else if (ins instanceof SourceReplacement) {

					// Replace source
					source.replace(ins.index, ((SourceReplacement) ins).endReplacementIndex, ins.insertString);

				} else {

					// Normal source insertion
					source.insert(ins.index, ins.insertString);
				}
			}

			// Write transformed source file
			// com.sureassert.uc.runtime.BasicUtils.debug(source.toString());
			BasicUtils.mkdirs(transformedSrcFile.getParentFile());
			if (transformedSrcFile.exists())
				transformedSrcFile.delete();
			FileUtils.writeStringToFile(transformedSrcFile, source.toString(), encoding);
			if (!transformedSrcFile.exists())
				EclipseUtils.reportError("Could not write transformed source file " + transformedSrcPath);

		} catch (Throwable e) {
			EclipseUtils.reportError(e);
		}
	}

	/**
	 * Copies all class files generated for the given source file from the project bin directory
	 * to the transformed bin directory.
	 * 
	 * @param project
	 * @param file
	 */
	/*
	 * public void copyClasses(SourceFile file, IJavaProject javaProject, ProjectClassLoaders
	 * classLoaders) {
	 * 
	 * try {
	 * File srcFile = EclipseUtils.getRawPath(file.getFile().getFullPath(),
	 * javaProject.getProject()).toFile();
	 * String srcDir = classLoaders.findSourceDir(srcFile.getAbsolutePath());
	 * String binDir = classLoaders.getBinaryDir(srcDir);
	 * String transformedBinDir = classLoaders.getTransformedBinaryDir(srcDir);
	 * String classDir = binDir + srcFile.getParent().substring(srcDir.length());
	 * String transformedClassDir = transformedBinDir +
	 * srcFile.getParent().substring(srcDir.length());
	 * String[] classFileNames = findClassFiles(srcFile.getName(), new File(classDir));
	 * if (classFileNames != null) {
	 * for (String classFileName : classFileNames) {
	 * File classFile = new File(classDir, classFileName);
	 * com.sureassert.uc.runtime.BasicUtils.debug("Copying class for source file with errors: " + //
	 * classFile.getAbsolutePath());
	 * FileUtils.copyFile(classFile, new File(transformedClassDir, classFileName));
	 * }
	 * }
	 * } catch (Exception e) {
	 * EclipseUtils.reportError(e);
	 * }
	 * }
	 */

	/*
	 * private String[] findClassFiles(String srcFileName, File binDir) {
	 * 
	 * String className = srcFileName.substring(0, srcFileName.indexOf(".java"));
	 * final String matchName = className + ".class";
	 * final String matchInnerName = className + "$";
	 * return binDir.list(new FilenameFilter() {
	 * 
	 * public boolean accept(File dir, String name) {
	 * 
	 * return name.equals(matchName) || name.startsWith(matchInnerName);
	 * }
	 * });
	 * }
	 */

	/**
	 * Replaces the doubled class source file with the test double source for all class doubles.
	 * 
	 * @param doubledSourceFilePathByTDClassName
	 * @param tdSourceFilePathByTDClassName
	 * @throws IOException
	 */
	public void replaceTDClasses(Set<String> testDoubleClassNames) throws IOException {

		for (String tdClassName : testDoubleClassNames) {
			String doubledSourceFilePath = PersistentDataFactory.getInstance().getDoubledClassSourcePath(tdClassName);
			String tdSourceFilePath = PersistentDataFactory.getInstance().getTDClassSourcePath(tdClassName);
			if (tdSourceFilePath != null && doubledSourceFilePath != null) {
				ClassDouble classDouble = (ClassDouble) PersistentDataFactory.getInstance().//
						getClassDoubleForTDClassName(tdClassName);

				// Copy transformed test double class over doubled class
				FileUtils.copyFile(new File(tdSourceFilePath), new File(doubledSourceFilePath));
				String src = FileUtils.readFileToString(new File(doubledSourceFilePath));

				// Retain test double class name references marked with |%s|
				// String tdReplaceToken = "$|SA-" + UUID.randomUUID().toString();
				// src = src.replace("|" + classDouble.getTestDoubleClassName() + "|",
				// tdReplaceToken);

				// Replace all references to test double class name with doubled class name
				src = src.replace(classDouble.getTestDoubleClassName(), classDouble.getDoubledClassName());
				src = src.replace(BasicUtils.getSimpleClassName(classDouble.getTestDoubleClassName()), //
						BasicUtils.getSimpleClassName(classDouble.getDoubledClassName()));

				// Restore retained test double class name references
				// src = src.replace(tdReplaceToken, classDouble.getTestDoubleClassName());

				// Write
				FileUtils.writeStringToFile(new File(doubledSourceFilePath), src);
			}
		}
	}

	/**
	 * Compiles all java files in the transformed source directory of the given project to the
	 * transformed binary directory.
	 * Uses build settings from the given project and the classpath from the given
	 * ProjectClassLoaders.
	 * 
	 * @param project
	 * @param projectCLs
	 * @throws JavaModelException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws CompileErrorsException
	 */
	public void compileAllTransformed(IJavaProject project, ProjectClassLoaders projectCLs, //
			IProgressMonitor monitor) throws JavaModelException, MalformedURLException, URISyntaxException, CompileErrorsException {

		monitor.setTaskName("Sureassert UC: Compiling project " + project.getElementName());

		// Build class file
		CompilationProgress progress = new NullCompilationProgress();

		// Build classpath from projectCL, excluding the source output directories
		StringBuilder classPath = new StringBuilder();
		Set<URL> sourceOutputURLs = new HashSet<URL>();
		for (String sourceDir : projectCLs.getSourceDirs()) {
			String sourceOutputDir = projectCLs.getBinaryDir(sourceDir);
			sourceOutputURLs.add(new File(sourceOutputDir).toURI().toURL());
		}
		for (URL cpEntry : ((URLClassLoader) projectCLs.projectCL).getURLs()) {
			// if (!sourceOutputURLs.contains(cpEntry)) {
			classPath.append("\"").append(new File(cpEntry.toURI()).getAbsolutePath()).//
					append("\"").append(File.pathSeparator);
			// }
		}
		classPath.deleteCharAt(classPath.length() - 1);

		StringBuilder sourcePath = new StringBuilder();
		for (String sourceDir : projectCLs.getSourceDirs()) {
			sourcePath.append("\"").append(sourceDir).append("\"").append(File.pathSeparator);
		}
		sourcePath.deleteCharAt(sourcePath.length() - 1);

		String compilerCompliance = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		String compilerSource = project.getOption(JavaCore.COMPILER_SOURCE, true);

		Set<String> transformedSourceDirs = new HashSet<String>();
		for (String sourceDir : projectCLs.getSourceDirs()) {
			transformedSourceDirs.add(projectCLs.getTransformedSourceDir(sourceDir));
		}

		// NOTE: only one .sasrc so just get it from first project source dir
		String sourceDir = projectCLs.getSourceDirs().iterator().next();
		Timer timer = new Timer("Compiling " + sourceDir);

		String transformedSrcDir = projectCLs.getTransformedSourceDir(sourceDir);
		if (BasicUtils.exists(new File(transformedSrcDir), ".java", new HashSet<File>())) {

			String targetDir = projectCLs.getTransformedBinaryDir(sourceDir);
			BasicUtils.mkdirs(new File(targetDir));

			String args = String.format("\"%s\" -sourcepath \"%s\" -classpath %s -time -g -source %s -%s -warn:none -d \"%s\"", //
					transformedSrcDir, transformedSrcDir, classPath, compilerSource, compilerCompliance, targetDir);
			com.sureassert.uc.runtime.BasicUtils.debug("Compiling " + transformedSrcDir + " with args: " + args);
			ByteArrayOutputStream errOut = new ByteArrayOutputStream();
			BatchCompiler.compile(args, new PrintWriter(System.out), new PrintWriter(errOut), progress);
			parseErrorStream(errOut, project, projectCLs);
		}
		timer.printExpiredTime();
	}

	/**
	 * Compiles the specified java files in the transformed source directory of the given project to
	 * the
	 * transformed binary directory.
	 * Uses build settings from the given project and the classpath from the given
	 * ProjectClassLoaders.
	 * 
	 * @param project
	 * @param projectCLs
	 * @throws JavaModelException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws CompileErrorsException
	 */
	public void compileTransformed(Set<IPath> sourcePaths, IJavaProject project, ProjectClassLoaders projectCLs, //
			List<String> genDefaultSrcPaths, IProgressMonitor monitor) throws JavaModelException, MalformedURLException, URISyntaxException, CompileErrorsException {

		monitor.setTaskName("Sureassert UC: Compiling Sureassert project " + project.getElementName());

		// Build class file
		CompilationProgress progress = new NullCompilationProgress();

		// Build compile path
		Map<String, Set<String>> compileFilesBySourcePath = new HashMap<String, Set<String>>();
		for (IPath sourcePath : sourcePaths) {
			String fullPath = EclipseUtils.getRawPath(sourcePath, project.getProject()).toFile().getAbsolutePath();
			String compileFile = projectCLs.findTransformedSourceFile(fullPath);
			for (String sourceDir : projectCLs.getSourceDirs()) {
				String transformedSrcDir = projectCLs.getTransformedSourceDir(sourceDir);
				if (compileFile.startsWith(transformedSrcDir)) {
					BasicUtils.mapSetAdd(compileFilesBySourcePath, sourceDir, compileFile);
				}
			}
		}

		// Build classpath from projectCL, excluding the source output directories
		StringBuilder classPath = new StringBuilder();
		Set<URL> sourceOutputURLs = new HashSet<URL>();
		for (String sourceDir : projectCLs.getSourceDirs()) {
			String sourceOutputDir = projectCLs.getBinaryDir(sourceDir);
			sourceOutputURLs.add(new File(sourceOutputDir).toURI().toURL());
		}
		for (URL cpEntry : ((URLClassLoader) projectCLs.projectCL).getURLs()) {
			// if (!sourceOutputURLs.contains(cpEntry)) {
			classPath.append("\"").append(new File(cpEntry.toURI()).getAbsolutePath()).//
					append("\"").append(File.pathSeparator);
			// }
		}
		classPath.deleteCharAt(classPath.length() - 1);

		StringBuilder sourcePath = new StringBuilder();
		for (String sourceDir : projectCLs.getSourceDirs()) {
			sourcePath.append("\"").append(sourceDir).append("\"").append(File.pathSeparator);
		}
		sourcePath.deleteCharAt(sourcePath.length() - 1);

		String compilerCompliance = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		String compilerSource = project.getOption(JavaCore.COMPILER_SOURCE, true);

		// NOTE: only one .sasrc so just get it from first project source dir
		String sourceDir = projectCLs.getSourceDirs().iterator().next();
		Timer timer = new Timer("Compiling " + sourceDir);

		String transformedSrcDir = projectCLs.getTransformedSourceDir(sourceDir);

		Set<String> compileFiles = compileFilesBySourcePath.get(sourceDir);
		if (compileFiles != null)
			compileFiles.addAll(genDefaultSrcPaths);

		if (compileFiles != null && !compileFiles.isEmpty()) {

			StringBuilder compilePath = new StringBuilder();
			for (Iterator<String> i = compileFiles.iterator(); i.hasNext();) {
				compilePath.append("\"").append(i.next()).append("\"");
				if (i.hasNext())
					compilePath.append(" ");
			}

			if (BasicUtils.exists(new File(transformedSrcDir), ".java", new HashSet<File>())) {

				String targetDir = projectCLs.getTransformedBinaryDir(sourceDir);

				String args = String.format("%s -sourcepath \"%s\" -classpath %s -time -g -source %s -%s -warn:none -d \"%s\"", //
						compilePath.toString(), transformedSrcDir, classPath, compilerSource, compilerCompliance, targetDir);
				com.sureassert.uc.runtime.BasicUtils.debug("Executing java compiler on " + compilePath + " with args: " + args);
				ByteArrayOutputStream errOut = new ByteArrayOutputStream();
				BatchCompiler.compile(args, new PrintWriter(System.out), new PrintWriter(errOut), progress);
				parseErrorStream(errOut, project, projectCLs);
			}
			timer.printExpiredTime();
		}
	}

	private static final Pattern COMPILE_ERROR_PATTERN = Pattern.compile("\\s*\\d+\\. ERROR in .*");

	private void parseErrorStream(ByteArrayOutputStream errOut, IJavaProject javaProject, //
			ProjectClassLoaders projectCLs) throws CompileErrorsException {

		List<CompileError> compileErrors = new ArrayList<CompileError>();
		try {
			String errorsStr = errOut.toString();
			if (errorsStr != null && errorsStr.length() > 0) {
				System.err.println(errorsStr);
				String[] errors = errorsStr.split("----------");
				for (String error : errors) {
					Matcher matcher = COMPILE_ERROR_PATTERN.matcher(error);
					if (matcher.find() && matcher.start() == 0) {
						String srcFilePath = error.substring(error.indexOf("ERROR in ") + 9, //
								error.indexOf(".java (at line ") + 5);
						int lineNumIdx = error.indexOf(".java (at line ") + 15;
						int lineNum = Integer.parseInt(error.substring(lineNumIdx, //
								error.indexOf(")", lineNumIdx)));
						String errorDesc = "Compilation error in instrumented source: " + error.substring(error.indexOf("\n", lineNumIdx) + 1);
						errorDesc = errorDesc.replaceAll("(?m)^\\s*\\^+", "");
						errorDesc = errorDesc.replace("\n", "    ");
						errorDesc = errorDesc.replace("\r", "");
						String projSrcFilePath = projectCLs.findOriginalSourceFile(srcFilePath);
						if (projSrcFilePath != null) {
							File srcFile = new File(projSrcFilePath);
							IFile file = javaProject.getProject().getWorkspace().getRoot().findFilesForLocationURI(srcFile.toURI())[0];
							if (file != null && file.exists())
								compileErrors.add(new CompileError(errorDesc, file, lineNum));
						} else {
							EclipseUtils.reportError(errorDesc);
						}
					}
				}
			}
		} catch (Exception e) {
			// Parse (screen-scrape) error
			e.printStackTrace();
			return;
		}
		if (!compileErrors.isEmpty()) {
			throw new CompileErrorsException(compileErrors);
		}
	}

	class NullCompilationProgress extends CompilationProgress {

		@Override
		public void begin(int remainingWork) {

		}

		@Override
		public void done() {

		}

		@Override
		public boolean isCanceled() {

			return false;
		}

		@Override
		public void setTaskName(String name) {

		}

		@Override
		public void worked(int workIncrement, int remainingWork) {

		}
	};
}