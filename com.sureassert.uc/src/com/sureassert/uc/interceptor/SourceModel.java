/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.interceptor;

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.sureassert.uc.annotation.IgnoreTestCoverage;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.ProjectClassLoaders;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ICoverageAware;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TestDouble;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.runtime.exception.SARuntimeException;

public class SourceModel {

	private static final boolean COVERAGE_REPORTING_ENABLED = true;

	public static final String SIG_FINAL_PREFIX = "__$_SA_$SIG";

	// final String interruptedCheckPlusCodeCovered =
	// "_sauc.SAInterceptor.instance.interruptedCheck(%d,\"%s\");";

	// public static final String interruptedCheck =
	// "_sauc.SAInterceptor.instance.interruptedCheck();";

	// public static final String codeCoveredSrc = COVERAGE_REPORTING_ENABLED ?
	// "_sauc.SAInterceptor.instance.codeCovered(%d,\"%s\");" : "";

	// public static final String codeCoveredTrueSrc = COVERAGE_REPORTING_ENABLED ?
	// "_sauc.SAInterceptor.instance.codeCoveredTrue(%d,\"%s\") && " : "";

	public static final String stubExecutedSrc = COVERAGE_REPORTING_ENABLED ? "_sauc.SAInterceptor.instance.stubExecuted(%d,%d,\"%s\");" : "";

	// public static final String registerBeforeSrc =
	// "_sauc.SAInterceptor.instance.registerMethodStart(%s);";

	// NOTE: |%s| is to avoid transform of test double class name to doubled class name; see
	// SATranslator.replaceTDClasses
	// final String beforeTDSrc =
	// "_sauc.SAInterceptor.instance.registerMethodStart(" + //
	// "com.sureassert.uc.runtime.SignatureTableFactory.instance.getSignatureForClass(\"%s\"), true);";

	public static final String sourceStubSrc = "_sauc.SAInterceptor.instance.execStub(\"%s\")";

	public static final String checkSourceStubSrc = "_sauc.SAInterceptor.instance.isStubbed(\"%s\")";

	// public static final String methodStubBeforeSrc =
	// "if (_sauc.SAInterceptor.instance.isStubbed(%s)) " + //
	// "%s return (%s)_sauc.SAInterceptor.instance.execStub(%s); %s";
	//
	// public static final String methodStubBeforeSrcPrimitive =
	// "if (_sauc.SAInterceptor.instance.isStubbed(" + //
	// "%s)) " + //
	// "%s return %s(_sauc.SAInterceptor.instance.execStub(" + //
	// "%s).toString()); %s";
	//
	// public static final String methodStubBeforeSrcVoid =
	// "if (_sauc.SAInterceptor.instance.isStubbed(" + //
	// "%s)) { %s _sauc.SAInterceptor.instance.execStub(%s); return; %s}";

	private static final String specialCharOpen = Character.toString('\uE206') + Character.toString('\uE206');
	private static final String specialCharClose = Character.toString('\uE207') + Character.toString('\uE207');

	private final List<ICoverageAware> coverageNotifiers;

	private final ICompilationUnit javaUnit;
	private final SourceModelFactory smFactory;
	private final String transformedSrcPath;
	private final SourceInsertionSet sourceInsertions;
	private final Map<Signature, IMethod> methodsBySig = new HashMap<Signature, IMethod>();
	private final Map<IMethod, Signature> sigByMethod = new HashMap<IMethod, Signature>();
	private final SourceFile sourceFile;
	private final IPath javaPath;
	private final IWorkspace workspace;
	private final Set<SourceModelError> errors = new HashSet<SourceModelError>();
	private final Map<String, String> resolvedTypeByTypeParam = new HashMap<String, String>();
	private final boolean sourceStubsEnabled;
	private final String source;
	private boolean containsTDOrDoubledClass;

	public SourceModel(SourceFile file, ICompilationUnit javaUnit, IJavaProject javaProject, //
			final ProjectClassLoaders classLoaders, SourceModelFactory smFactory) throws CoreException {

		this.sourceFile = file;
		this.javaUnit = javaUnit;
		this.smFactory = smFactory;
		this.workspace = javaProject.getProject().getWorkspace();
		this.javaPath = file.getFile().getFullPath();
		File srcFile = EclipseUtils.getRawPath(file.getFile().getFullPath(), javaProject.getProject()).toFile();
		this.transformedSrcPath = classLoaders.findTransformedSourceFile(srcFile.getAbsolutePath());
		this.sourceInsertions = new SourceInsertionSet();
		this.coverageNotifiers = new ArrayList<ICoverageAware>();
		this.sourceStubsEnabled = SaUCPreferences.getIsSourceStubsEnabled();

		init();

		if (BasicUtils.isJUnitTestClass(file.getSource())) {

			BasicUtils.debug("Ignoring JUnit test class " + srcFile.getName());
			this.source = file.getSource();

		} else {
			// File transformedSrcFile = new File(transformedSrcPath);
			// FileUtils.copyFile(srcFile, transformedSrcFile);
			// String encoding = file.getFile().getCharset();
			// StringBuffer source = new StringBuffer(FileUtils.readFileToString(srcFile,
			// encoding));

			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(javaUnit);
			this.source = javaUnit.getSource();
			final CompilationUnit root = (CompilationUnit) parser.createAST(null);
			@SuppressWarnings("unchecked")
			List<SourceStubDef> sourceStubs = getStubComments(root.getCommentList(), source);

			BasicUtils.debug("Visiting " + srcFile.getName());
			Timer timer = new Timer("Visiting " + srcFile.getAbsolutePath());
			try {
				SAASTVisitor visitor = new SAASTVisitor(sourceStubs);
				root.accept(visitor);
				containsTDOrDoubledClass = visitor.containsTDOrDoubledClass;
			} catch (Throwable e) {
				reportError(e);
				containsTDOrDoubledClass = false;
			}
			timer.printExpiredTime();
		}
		validateSourceInsertions();
	}

	public String getSource() {

		return source;
	}

	public boolean containsTDOrDoubledClass() {

		return containsTDOrDoubledClass;
	}

	private void validateSourceInsertions() {

		// Remove any SourceInsertions that fall within a SourceReplacement
		List<SourceReplacement> sourceReplacements = new ArrayList<SourceReplacement>();
		for (SourceInsertion sourceInsertion : sourceInsertions) {
			if (sourceInsertion instanceof SourceReplacement) {
				sourceReplacements.add((SourceReplacement) sourceInsertion);
			}
		}
		if (!sourceReplacements.isEmpty()) {
			Set<SourceInsertion> removeInsertions = new HashSet<SourceInsertion>();
			for (SourceInsertion insertion : sourceInsertions) {
				for (SourceReplacement replacement : sourceReplacements) {
					if (replacement != insertion && insertion.index >= replacement.index && insertion.index < replacement.endReplacementIndex) {
						removeInsertions.add(insertion);
					}
				}
			}
			sourceInsertions.removeAll(removeInsertions);
		}
	}

	public Set<SourceModelError> getErrors() {

		return errors;
	}

	public void init() {

		IConfigurationElement[] configEls = Platform.getExtensionRegistry().getConfigurationElementsFor(//
				ICoverageAware.COVERAGE_NOTIFIER_EXTENSION_ID);
		try {
			for (IConfigurationElement configEl : configEls) {
				Object execExtension = configEl.createExecutableExtension("class");
				if (execExtension instanceof ICoverageAware) {
					coverageNotifiers.add((ICoverageAware) execExtension);
				}
			}
		} catch (Throwable e) {
			reportError(e);
		}
	}

	public SourceFile getSourceFile() {

		return sourceFile;
	}

	private List<SourceStubDef> getStubComments(List<Comment> comments, String src) {

		List<SourceStubDef> stubDefs = new ArrayList<SourceStubDef>();
		for (Comment comment : comments) {
			try {
				if (sourceStubsEnabled && !(comment instanceof Javadoc)) {
					String commentStr = src.substring(comment.getStartPosition(), comment.getStartPosition() + comment.getLength());

					if (commentStr.contains("[@StubInsert")) {
						int startIdx = commentStr.indexOf("[@StubInsert");
						if (commentStr.substring(0, startIdx).contains("@Ignore"))
							continue;
						if (commentStr.indexOf("]", startIdx + 12) == -1) {
							errors.add(new SourceModelError("\"[@StubInsert\" in source without closing \"]\"", //
									sourceFile.getLineNum(comment.getStartPosition())));
						} else {
							String replaceStr = "";
							commentStr = commentStr.replace("/[", specialCharOpen);
							commentStr = commentStr.replace("/]", specialCharClose);
							commentStr = commentStr.replaceAll("\\[([\\$\\?][\\w_]+)\\]", specialCharOpen + "$1" + specialCharClose);
							int endIdx = commentStr.indexOf(']', startIdx + 13);
							replaceStr = commentStr.substring(startIdx + 13, endIdx);
							replaceStr = replaceStr.replace(specialCharOpen, "[");
							replaceStr = replaceStr.replace(specialCharClose, "]");
							stubDefs.add(new SourceStubDef(comment.getStartPosition(), comment.getStartPosition() + comment.getLength(), //
									replaceStr, true));
						}

					} else if (commentStr.contains("[@Stub")) {
						int startIdx = commentStr.indexOf("[@Stub");
						if (commentStr.substring(0, startIdx).contains("@Ignore"))
							continue;
						if (commentStr.indexOf("]", startIdx + 6) == -1) {
							errors.add(new SourceModelError("\"[@Stub\" in source without closing \"]\"", //
									sourceFile.getLineNum(comment.getStartPosition())));
						} else {
							String replaceStr = "";
							if (!commentStr.contains("[@Stub]")) {
								commentStr = commentStr.replace("/[", specialCharOpen);
								commentStr = commentStr.replace("/]", specialCharClose);
								commentStr = commentStr.replaceAll("\\[([\\$\\?][\\w_]+)\\]", specialCharOpen + "$1" + specialCharClose);
								int endIdx = commentStr.indexOf(']', startIdx + 7);
								replaceStr = commentStr.substring(startIdx + 7, endIdx);
								replaceStr = replaceStr.replace(specialCharOpen, "[");
								replaceStr = replaceStr.replace(specialCharClose, "]");
							}
							stubDefs.add(new SourceStubDef(comment.getStartPosition(), comment.getStartPosition() + comment.getLength(), //
									replaceStr, false));
						}
					}
				} // end if not javadoc
			} catch (Exception e) {
				errors.add(new SourceModelError("Invalid stub definition", //
						sourceFile.getLineNum(comment.getStartPosition())));
			}
		} // end comment loop
		return stubDefs;
	}

	public ICompilationUnit getCompilationUnit() {

		return javaUnit;
	}

	public Set<SourceInsertion> getSourceInsertions() {

		return sourceInsertions;
	}

	public IMethod getMethod(Signature sig) {

		return methodsBySig.get(sig);
	}

	public Set<Signature> getMethodSigs() {

		return Collections.unmodifiableSet(methodsBySig.keySet());
	}

	public Signature getSignature(IMethod method) {

		return sigByMethod.get(method);
	}

	public AccessibleObject getMethod(IMethod method, ClassLoader classLoader) throws ClassNotFoundException, SecurityException, NoSuchMethodException {

		Signature sig = sigByMethod.get(method);
		if (sig == null)
			return null;
		boolean isConstructor = sig.getMemberName().equals(Signature.CONSTRUCTOR_METHOD_NAME);
		Class<?> clazz = classLoader.loadClass(sig.getClassName());
		Class<?> enclosingClass = clazz.getEnclosingClass();
		boolean isStatic = Modifier.isStatic(clazz.getModifiers());
		boolean hasEnclodingClassParam = isConstructor && enclosingClass != null && !isStatic;
		String[] paramClassNames = sig.getParamClassNames();
		int numParams = hasEnclodingClassParam ? paramClassNames.length + 1 : paramClassNames.length;
		Class<?>[] paramClasses = new Class<?>[numParams];
		int paramClassesIdx = 0;
		if (hasEnclodingClassParam)
			paramClasses[paramClassesIdx++] = enclosingClass;
		for (int paramClassNamesIdx = 0; paramClassNamesIdx < paramClassNames.length; paramClassNamesIdx++, paramClassesIdx++) {
			paramClasses[paramClassesIdx] = BasicUtils.getClass(paramClassNames[paramClassNamesIdx], classLoader);
		}
		try {
			if (isConstructor) {
				return clazz.getDeclaredConstructor(paramClasses);
			} else {
				return clazz.getDeclaredMethod(sig.getMemberName(), paramClasses);
			}
		} catch (NoSuchMethodException e) {
			if (clazz.isEnum()) {
				Class<?>[] enumPrefixClasses = new Class<?>[] { String.class, Integer.TYPE };
				paramClasses = (Class<?>[]) ArrayUtils.addAll(enumPrefixClasses, paramClasses);
				if (isConstructor) {
					return clazz.getDeclaredConstructor(paramClasses);
				} else {
					return clazz.getDeclaredMethod(sig.getMemberName(), paramClasses);
				}
			} else {
				throw e;
			}
		}
	}

	private void registerMethod(IMethod method, Signature sig) {

		methodsBySig.put(sig, method);
		sigByMethod.put(method, sig);
	}

	public static String getThisSrc(IMethod method) {

		try {
			return Flags.isStatic(method.getFlags()) || method.isConstructor() ? "null" : "this";
		} catch (JavaModelException e) {
			throw new SARuntimeException(e);
		}
	}

	public static String getThisSrc(IMethodBinding methodBinding) {

		return org.eclipse.jdt.core.dom.Modifier.isStatic(methodBinding.getModifiers()) || //
				methodBinding.isConstructor() ? "null" : "this";
	}

	public static String getParamNamesSrc(MethodDeclaration methodNode) {

		StringBuilder paramsStr = new StringBuilder();
		for (Iterator<?> paramIt = methodNode.parameters().iterator(); paramIt.hasNext();) {
			paramsStr.append(",");
			paramsStr.append(((SingleVariableDeclaration) paramIt.next()).getName().toString());
		}
		return paramsStr.toString();
	}

	private class SAASTVisitor extends ASTVisitor {

		private String currentClassBinaryName = null;
		private String currentClassSimpleName = null;
		// private String currentClassQualifiedName = null;
		private boolean coverageReportingActive;
		private boolean methodCoverageReportingActive;
		private Signature currentMethodSig = null;
		// private SourceInsertion currentConstructorInsertion;
		private TestDouble testDoubleForDoubledClass;
		private TestDouble testDoubleForTDClass;
		private Signature doubledMethodSig = null;
		private final Iterator<SourceStubDef> sourceStubIt;
		private SourceStubDef nextStub = null;
		private boolean containsTDOrDoubledClass = false;
		private boolean isEnum;

		// private List<SourceInsertion> currentMethodCoverageInsertions;

		SAASTVisitor(List<SourceStubDef> sourceStubs) {

			this.sourceStubIt = sourceStubs.iterator();
			nextStub = sourceStubIt.hasNext() ? sourceStubIt.next() : null;
		}

		// Insert before and around advice on constructors and methods
		//
		// -----------------------------------------------------------------

		/**
		 * Checks if this node belongs to a different class than was registered last.
		 * This happens with nested classes. If it does, corrects the registered class.
		 */
		private void setCurrentClass(ASTNode node) {

			while (node != null && !(node instanceof AbstractTypeDeclaration)) {
				node = node.getParent();
			}
			if (node != null && !((AbstractTypeDeclaration) node).resolveBinding().getBinaryName().equals(currentClassBinaryName)) {
				setCurentClass(((AbstractTypeDeclaration) node).resolveBinding().getBinaryName(), //
						((AbstractTypeDeclaration) node).resolveBinding().getQualifiedName(), ((AbstractTypeDeclaration) node).resolveBinding().getName());
			}
		}

		private void setCurentClass(String binaryName, String qualifiedName, String simpleName) {

			currentClassBinaryName = binaryName;
			currentClassSimpleName = simpleName;
			// currentClassQualifiedName = qualifiedName;
			currentMethodSig = SignatureTableFactory.instance.getSignatureForClass(currentClassBinaryName);
			testDoubleForDoubledClass = PersistentDataFactory.getInstance().//
					getClassDoubleForDoubledClassName(qualifiedName);
			if (testDoubleForDoubledClass != null) {
				smFactory.addTestDoubleClassName(testDoubleForDoubledClass.getTestDoubleClassName());
				PersistentDataFactory.getInstance().setDoubledClassSourcePath(//
						testDoubleForDoubledClass.getTestDoubleClassName(), transformedSrcPath);
			}
			testDoubleForTDClass = PersistentDataFactory.getInstance().//
					getClassDoubleForTDClassName(qualifiedName);
			if (testDoubleForTDClass != null) {
				smFactory.addTestDoubleClassName(testDoubleForTDClass.getTestDoubleClassName());
				PersistentDataFactory.getInstance().setTDClassSourcePath(//
						testDoubleForTDClass.getTestDoubleClassName(), transformedSrcPath);
			}
			coverageReportingActive = true;
			methodCoverageReportingActive = true;
			IType javaType = javaUnit.getType(currentClassSimpleName);
			if (javaType != null) {
				try {
					coverageReportingActive = !EclipseUtils.hasClassAnnotation(javaType, IgnoreTestCoverage.class.getSimpleName()) && //
							SaUCPreferences.getIsCoverageEnabled();
				} catch (JavaModelException e) {
					reportError(e);
				}
			}
			containsTDOrDoubledClass = containsTDOrDoubledClass || testDoubleForTDClass != null || testDoubleForDoubledClass != null;
		}

		@Override
		public boolean visit(EnumDeclaration node) {

			return onNewType(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {

			return onNewType(node);
		}

		private boolean onNewType(AbstractTypeDeclaration node) {

			try {
				// if (!foundTopLevelType) {
				ITypeBinding typeBinding = node.resolveBinding();
				String classBinaryName = typeBinding.getBinaryName();

				isEnum = node instanceof EnumDeclaration;
				setCurentClass(classBinaryName, //
						typeBinding.getQualifiedName(), typeBinding.getName());
			} catch (Throwable e) {
				reportError(e);
			}
			return true;
		}

		/*
		 * @Override
		 * public void endVisit(EnumDeclaration node) {
		 * 
		 * _endVisit(node);
		 * }
		 * 
		 * @Override
		 * public void endVisit(TypeDeclaration node) {
		 * 
		 * _endVisit(node);
		 * }
		 * 
		 * private void _endVisit(AbstractTypeDeclaration node) {
		 * 
		 * // String classBinaryName = node.resolveBinding().getBinaryName();
		 * // TypeModel typeModel = getTypeModel(classBinaryName);
		 * //
		 * // // Add generated final attributes
		 * // if (typeModel != null && typeModel.sigFinalsInsertIdx > -1 && typeModel.sigFinalsSrc
		 * // != null && //
		 * // typeModel.sigFinalsSrc.length() > 0) {
		 * // sourceInsertions.add(new SourceInsertion(typeModel.sigFinalsSrc.toString(), //
		 * // typeModel.sigFinalsInsertIdx, -1));
		 * // }
		 * 
		 * // sigFinalsInsertIdx = -1;
		 * // sigFinalsSrc = new StringBuilder();
		 * 
		 * registerMethodEnd();
		 * }
		 * 
		 * @Override
		 * public void endVisit(CompilationUnit node) {
		 * 
		 * registerMethodEnd();
		 * }
		 */

		/*
		 * @Override
		 * public boolean visit(Initializer node) {
		 * 
		 * setCurrentClass(node);
		 * registerMethodEnd();
		 * try {
		 * currentMethodSig =
		 * SignatureTableFactory.instance.getSignatureForClass(currentClassBinaryName);
		 * 
		 * // TODO: Source translator for Initializer
		 * 
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 */

		/*
		 * private void registerMethodEnd() {
		 * 
		 * if (currentMethodCoverageInsertions != null) {
		 * // Remove coverage SourceInsertions where there are fewer than the designated
		 * // threshold in the current method
		 * int coverageRequiredThreshold = SaUCPreferences.getCoverageRequiredThreshold();
		 * if (currentMethodCoverageInsertions.size() <= coverageRequiredThreshold) {
		 * sourceInsertions.removeAll(currentMethodCoverageInsertions);
		 * } else {
		 * // Notify GUI to add coverage required markers
		 * for (SourceInsertion insertion : currentMethodCoverageInsertions) {
		 * notifyCoverageRequired(insertion.lineNum, javaPath, currentClassBinaryName, workspace);
		 * }
		 * }
		 * }
		 * currentMethodCoverageInsertions = new ArrayList<SourceInsertion>();
		 * }
		 */

		@Override
		public boolean visit(MethodDeclaration methodNode) {

			setCurrentClass(methodNode);
			// registerMethodEnd();
			try {
				IMethodBinding methodBinding = methodNode.resolveBinding();
				currentMethodSig = SignatureUtils.getSignature(methodBinding, resolvedTypeByTypeParam);
				// String paramClassNames =
				// BasicUtils.toSrcString(currentMethodSig.getParamClassNames());
				IMethod method = (IMethod) methodBinding.getJavaElement();
				registerMethod(method, currentMethodSig);

				IAnnotation igCovRegAn = EclipseUtils.getAnnotations(method).get(IgnoreTestCoverage.class.getSimpleName());
				methodCoverageReportingActive = igCovRegAn == null || !igCovRegAn.exists();

				if (testDoubleForTDClass != null) {
					doubledMethodSig = SignatureTableFactory.instance.getSignature(//
							testDoubleForTDClass.getDoubledClassName(), //
							currentMethodSig.getMemberName(), currentMethodSig.getParamClassNames());
				} else {
					doubledMethodSig = null;
				}

				if (methodNode.isConstructor() && !isEnum) {

					if (testDoubleForTDClass != null) {
						PersistentDataFactory.getInstance().registerStaticDependency(doubledMethodSig, //
								currentMethodSig, false);
					}

				} else if (!isEnum) { // is method

					int modifiers = methodNode.getModifiers();
					if (methodNode.getBody() != null && !Modifier.isAbstract(modifiers) && !Modifier.isNative(modifiers)) {

						// Register test double dependency
						if (testDoubleForTDClass != null) {
							PersistentDataFactory.getInstance().registerStaticDependency(doubledMethodSig, //
									currentMethodSig, false);
						}
					}
				}

			} catch (Throwable e) {
				reportError(e);
			}
			return true;
		}

		/*
		 * private boolean remove(SourceInsertion rem) {
		 * 
		 * for (Iterator<SourceInsertion> i = sourceInsertions.iterator(); i.hasNext();) {
		 * if (i.next() == currentConstructorInsertion) {
		 * i.remove();
		 * return true;
		 * }
		 * }
		 * return false;
		 * }
		 */

		/*
		 * @Override
		 * public boolean visit(SuperConstructorInvocation node) {
		 * 
		 * try {
		 * if (currentConstructorInsertion != null) { // can be null for enums
		 * // Move constructor insertion to after super()
		 * int newIndex = node.getStartPosition() + node.getLength();
		 * SourceInsertion newConstructorInsertion = new SourceInsertion(//
		 * currentConstructorInsertion.insertString, newIndex, -1);
		 * remove(currentConstructorInsertion);
		 * sourceInsertions.add(newConstructorInsertion);
		 * currentConstructorInsertion = newConstructorInsertion;
		 * }
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 * 
		 * @Override
		 * public boolean visit(ConstructorInvocation node) {
		 * 
		 * try {
		 * if (currentConstructorInsertion != null) { // can be null for enums
		 * // Move constructor insertion to after this()
		 * int newIndex = node.getStartPosition() + node.getLength();
		 * SourceInsertion newConstructorInsertion = new SourceInsertion(//
		 * currentConstructorInsertion.insertString, newIndex, -1);
		 * remove(currentConstructorInsertion);
		 * sourceInsertions.add(newConstructorInsertion);
		 * currentConstructorInsertion = newConstructorInsertion;
		 * }
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 */

		// -----------------------------------------------------------------
		//
		// Register external class field access dependencies

		@Override
		public boolean visit(QualifiedName node) {

			setCurrentClass(node);
			try {
				IBinding fieldBinding = node.resolveBinding();
				if (fieldBinding == null || fieldBinding.getKind() != IBinding.VARIABLE || node.getQualifier() == null)
					return true;
				ITypeBinding lhsType = node.getQualifier().resolveTypeBinding();
				if (lhsType == null)
					return true;
				String declaringClassBinaryName = lhsType.getBinaryName();
				if (currentMethodSig != null && declaringClassBinaryName != null && !declaringClassBinaryName.equals(currentClassBinaryName) && //
						!declaringClassBinaryName.endsWith("[]") && !declaringClassBinaryName.startsWith("java.")) {

					String fieldName = node.getName().getFullyQualifiedName();
					Signature to = SignatureTableFactory.instance.getSignature(declaringClassBinaryName, fieldName);
					PersistentDataFactory.getInstance().registerStaticDependency(currentMethodSig, to, false);
				}
			} catch (Throwable e) {
				reportError(e);
			}
			return true;
		}

		@Override
		public boolean visit(FieldAccess node) {

			setCurrentClass(node);
			try {
				IVariableBinding fieldBinding = node.resolveFieldBinding();
				ITypeBinding declaringClass = fieldBinding.getDeclaringClass();
				if (currentMethodSig != null && declaringClass != null) {
					String fieldClassName = declaringClass.getBinaryName();
					if (!fieldClassName.equals(currentClassBinaryName)) {
						String fieldName = node.getName().getFullyQualifiedName();
						Signature to = SignatureTableFactory.instance.getSignature(fieldClassName, fieldName);
						PersistentDataFactory.getInstance().registerStaticDependency(currentMethodSig, to, false);
					}
				}
			} catch (Throwable e) {
				reportError(e);
			}
			return true;
		}

		// -----------------------------------------------------------------
		//
		// Handle Interrupted checks

		/*
		 * @Override
		 * public boolean visit(DoStatement node) {
		 * 
		 * setCurrentClass(node);
		 * try {
		 * if (node.getBody() != null) {
		 * // int blockStart = node.getBody().getStartPosition();
		 * // int blockEnd = node.getBody().getStartPosition() +
		 * // node.getBody().getLength();
		 * int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * if (doCoverageReporting() && !parentNodeIsLabeledStatement(node)) {
		 * SourceInsertion insertion = new SourceInsertion(String.format(//
		 * codeCoveredSrc, lineNum, currentClassBinaryName), node.getStartPosition(), lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * // sourceInsertions.add(new BlockAwareSourceInsertion(interruptedCheck,
		 * // blockStart, blockEnd, lineNum));
		 * }
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 */

		/*
		 * @Override
		 * public boolean visit(WhileStatement node) {
		 * 
		 * setCurrentClass(node);
		 * try {
		 * if (node.getBody() != null) {
		 * // int blockStart = node.getBody().getStartPosition();
		 * // int blockEnd = node.getBody().getStartPosition() +
		 * // node.getBody().getLength();
		 * int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * if (doCoverageReporting() && !parentNodeIsLabeledStatement(node)) {
		 * SourceInsertion insertion = new SourceInsertion(String.format(//
		 * codeCoveredSrc, lineNum, currentClassBinaryName), node.getStartPosition(), lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * // sourceInsertions.add(new BlockAwareSourceInsertion(interruptedCheck,
		 * // blockStart, blockEnd, lineNum));
		 * }
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 */

		/*
		 * @Override
		 * public boolean visit(ForStatement node) {
		 * 
		 * setCurrentClass(node);
		 * try {
		 * if (node.getBody() != null) {
		 * // int blockStart = node.getBody().getStartPosition();
		 * // int blockEnd = node.getBody().getStartPosition() +
		 * // node.getBody().getLength();
		 * int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * if (doCoverageReporting() && !parentNodeIsLabeledStatement(node)) {
		 * SourceInsertion insertion = new SourceInsertion(String.format(//
		 * codeCoveredSrc, lineNum, currentClassBinaryName), node.getStartPosition(), lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * // sourceInsertions.add(new BlockAwareSourceInsertion(interruptedCheck,
		 * // blockStart, blockEnd, lineNum));
		 * }
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 */

		/*
		 * @Override
		 * public boolean visit(EnhancedForStatement node) {
		 * 
		 * setCurrentClass(node);
		 * try {
		 * if (node.getBody() != null) {
		 * // int blockStart = node.getBody().getStartPosition();
		 * // int blockEnd = node.getBody().getStartPosition() +
		 * // node.getBody().getLength();
		 * int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * if (doCoverageReporting() && !parentNodeIsLabeledStatement(node)) {
		 * SourceInsertion insertion = new SourceInsertion(String.format(//
		 * codeCoveredSrc, lineNum, currentClassBinaryName), node.getStartPosition(), lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * // sourceInsertions.add(new BlockAwareSourceInsertion(interruptedCheck,
		 * // blockStart, blockEnd, lineNum));
		 * }
		 * } catch (Throwable e) {
		 * reportError(e);
		 * }
		 * return true;
		 * }
		 */

		/*
		 * @Override
		 * public boolean visit(IfStatement node) {
		 * 
		 * setCurrentClass(node);
		 * 
		 * int blockStart = node.getThenStatement().getStartPosition();
		 * int blockEnd = node.getThenStatement().getStartPosition() +
		 * node.getThenStatement().getLength();
		 * 
		 * if (doCoverageReporting() && !parentNodeIsLabeledStatement(node)) {
		 * int lineNum;
		 * if (!(node.getParent() instanceof IfStatement)) {
		 * lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * SourceInsertion insertion = new SourceInsertion(String.format(codeCoveredSrc, //
		 * lineNum, currentClassBinaryName), node.getStartPosition(), lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * } else {
		 * lineNum = sourceFile.getLineNum(node.getExpression().getStartPosition());
		 * SourceInsertion insertion = new SourceInsertion(String.format(codeCoveredTrueSrc, //
		 * lineNum, currentClassBinaryName), node.getExpression().getStartPosition(), lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * int blockStartLineNum = sourceFile.getLineNum(blockStart);
		 * SourceInsertion insertion = new BlockAwareSourceInsertion(String.format(//
		 * codeCoveredSrc, blockStartLineNum, currentClassBinaryName), blockStart, blockEnd,
		 * blockStartLineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * 
		 * if (node.getElseStatement() != null) {
		 * if (!(node.getElseStatement() instanceof IfStatement)) {
		 * // not an "if-else" statement
		 * blockStart = node.getElseStatement().getStartPosition();
		 * blockEnd = node.getElseStatement().getStartPosition() +
		 * node.getElseStatement().getLength();
		 * lineNum = sourceFile.getLineNum(blockStart);
		 * insertion = new BlockAwareSourceInsertion(String.format(//
		 * codeCoveredSrc, lineNum, currentClassBinaryName), blockStart, blockEnd, lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * }
		 * }
		 * return true;
		 * }
		 */

		/*
		 * private boolean parentNodeIsLabeledStatement(ASTNode node) {
		 * 
		 * return node.getParent() != null && node instanceof Statement && node.getParent()
		 * instanceof LabeledStatement;
		 * }
		 */

		// -------------------------
		//
		// Handle Coverage Reporting

		/*
		 * @Override
		 * public void postVisit(ASTNode node) {
		 * 
		 * if (doCoverageReporting()) {
		 * setCurrentClass(node);
		 * if (node != null && (node instanceof SuperConstructorInvocation || node instanceof
		 * ConstructorInvocation)) {
		 * int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * SourceInsertion insertion = new SourceInsertion(String.format(codeCoveredSrc, lineNum, //
		 * currentClassBinaryName), node.getStartPosition() + node.getLength() + 1, lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * 
		 * } else if (node != null && (node instanceof SwitchCase)) {
		 * // no-op
		 * } else if (node != null && (node instanceof Statement) && !(node instanceof Block) &&
		 * !(node instanceof IfStatement)) {
		 * int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * int insertPos = parentNodeIsLabeledStatement(node) ? node.getParent().getStartPosition()
		 * : node.getStartPosition();
		 * SourceInsertion insertion = new SourceInsertion(String.format(codeCoveredSrc, lineNum, //
		 * currentClassBinaryName), insertPos, lineNum);
		 * sourceInsertions.add(insertion);
		 * currentMethodCoverageInsertions.add(insertion);
		 * }
		 * // }
		 * }
		 * }
		 * 
		 * @Override
		 * public boolean visit(ReturnStatement node) {
		 * 
		 * // try {
		 * // setCurrentClass(node);
		 * //
		 * // //String retExpr = node.getExpression().toString();
		 * // int lineNum = sourceFile.getLineNum(node.getStartPosition());
		 * // String covSrc = String.format(codeCoveredSrc, lineNum, currentClassBinaryName);
		 * // sourceInsertions.add(new SourceReplacement("{System.out.println(\"replaced\"); " +
		 * // covSrc + " return " + retExpr + ";}", //
		 * // node.getStartPosition(), node.getStartPosition() + node.getLength(), lineNum));
		 * // } catch (Throwable e) {
		 * // reportError(e);
		 * // }
		 * return true;
		 * }
		 */
		// -------------------------
		//
		// Handle Source Stubs

		@Override
		public void preVisit(ASTNode node) {

			try {
				if (nextStub != null && nextStub.position < node.getStartPosition()) {

					// Get source
					String replacedSrc; // The source code being replaced
					String insertSrc = nextStub.replaceStr; // The source code being inserted
					if (nextStub.isPaste) {
						replacedSrc = sourceFile.getSource().substring(nextStub.position, //
								nextStub.endPosition);
					} else {
						replacedSrc = sourceFile.getSource().substring(nextStub.position, //
								node.getStartPosition() + node.getLength());
					}

					// Remove newlines and block comment characters
					insertSrc = insertSrc.replaceAll("(?m)^\\s*\\*", " ");
					insertSrc = insertSrc.replace("\r\n", " ");
					insertSrc = insertSrc.replace('\n', ' ');

					// Substitute source stubs
					insertSrc = insertSrc.replaceAll("\\[\\$([\\w_]+)\\]", //
							String.format(sourceStubSrc, "$1"));
					insertSrc = insertSrc.replaceAll("\\[\\?([\\w_]+)\\]", //
							String.format(checkSourceStubSrc, "$1"));

					// Create SourceReplacement
					StringBuilder replaceStr = new StringBuilder(insertSrc);
					int lastMatchIdx = -1;
					while ((lastMatchIdx = replacedSrc.indexOf('\n', lastMatchIdx + 1)) > -1) {
						replaceStr.append("\n");
					}

					int startLineNum = sourceFile.getLineNum(nextStub.position);
					if (nextStub.isPaste) {
						int endLineNum = sourceFile.getLineNum(nextStub.endPosition);
						if (doCoverageReporting()) {
							replaceStr.insert(0, String.format(stubExecutedSrc, startLineNum, endLineNum, currentClassBinaryName));
						}
						sourceInsertions.add(new SourceReplacement(replaceStr.toString(), //
								nextStub.position, nextStub.endPosition, startLineNum));
					} else {
						// Find replaced line numbers and insert register stub executed code
						int endLineNum = sourceFile.getLineNum(node.getStartPosition() + node.getLength());
						if (doCoverageReporting()) {
							replaceStr.insert(0, String.format(stubExecutedSrc, startLineNum, endLineNum, currentClassBinaryName));
						}
						sourceInsertions.add(new SourceReplacement(replaceStr.toString(), //
								nextStub.position, node.getStartPosition() + node.getLength(), startLineNum));
					}

					// Move to next source stub
					nextStub = sourceStubIt.hasNext() ? sourceStubIt.next() : null;
				}
			} catch (Throwable e) {
				reportError(e);
			}
		}

		// -------------------------

		@SuppressWarnings("unused")
		private String corruptClassName(String className) {

			if (testDoubleForDoubledClass == null && testDoubleForTDClass == null)
				return className.charAt(0) + '\uE206' + className.substring(1);
			else
				return className;
		}

		private boolean doCoverageReporting() {

			return coverageReportingActive && methodCoverageReportingActive && !containsTDOrDoubledClass;
		}
	}

	/*
	 * private void notifyCoverageRequired(int lineNum, IPath javaPath, String className, IWorkspace
	 * workspace) {
	 * 
	 * for (ICoverageAware coverageNotifier : coverageNotifiers) {
	 * coverageNotifier.notifyCoverageRequired(lineNum, javaPath.toString(), className);
	 * }
	 * }
	 */

	public Map<String, String> getResolvedTypeByTypeParam() {

		return resolvedTypeByTypeParam;
	}

	public static class SourceInsertionSet extends TreeSet<SourceInsertion> {

		private static final long serialVersionUID = 1L;
		private final Set<Integer> blockAwareInsertLocations = new HashSet<Integer>();

		@Override
		public boolean add(SourceInsertion element) {

			if (blockAwareInsertLocations.contains(element.index)) {
				return false;
			} else {
				blockAwareInsertLocations.add(element.index);
			}
			return super.add(element);
		}
	}

	private class SourceStubDef {

		private final int position;
		private final int endPosition;
		private final String replaceStr;
		private final boolean isPaste;

		public SourceStubDef(int position, int endPosition, String replaceStr, boolean isPaste) {

			this.position = position;
			this.replaceStr = replaceStr;
			this.isPaste = isPaste;
			this.endPosition = endPosition;
		}
	}

	public static class SourceModelError {

		private final String message;
		private final int lineNum;

		public SourceModelError(String message, int lineNum) {

			this.message = message;
			this.lineNum = lineNum;
		}

		public String getMessage() {

			return message;
		}

		public int getLineNum() {

			return lineNum;
		}
	}

	private void reportError(Throwable e) {

		EclipseUtils.reportError("Error parsing " + javaPath.toString(), e);
	}

	public static String getTrySrc(String[] excTypeNames) {

		if (excTypeNames == null || excTypeNames.length == 0)
			return "";
		else
			return "try {";
	}

	public static String getTrySrc(IMethodBinding methodBinding) {

		String trySrc = "";
		ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
		if (exceptionTypes != null && exceptionTypes.length > 0) {
			trySrc = "try {";
		}
		return trySrc;
	}

	public static String getCatchSrc(String[] excTypeNames) {

		String catchSrc = "";
		if (excTypeNames != null && excTypeNames.length > 0) {
			StringBuilder catchSrcBuf = new StringBuilder(//
					"} catch (_sauc.SAUCWrapperException __saucWE_) {");
			for (int i = 0; i < excTypeNames.length; i++) {
				String eSrcName = excTypeNames[i];
				catchSrcBuf.append("if (__saucWE_.getCause() instanceof ").append(eSrcName).append(//
						") throw (").append(eSrcName).append(")__saucWE_.getCause(); ");
			}
			catchSrcBuf.append("throw new _sauc.StubThrowingException(__saucWE_); }");
			catchSrc = catchSrcBuf.toString();
		}
		return catchSrc;
	}

	public static String getCatchSrc(IMethodBinding methodBinding) {

		String catchSrc = "";
		ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
		if (exceptionTypes != null && exceptionTypes.length > 0) {
			StringBuilder catchSrcBuf = new StringBuilder(//
					"} catch (_sauc.SAUCWrapperException __saucWE_) {");
			for (int i = 0; i < exceptionTypes.length; i++) {
				String eSrcName = exceptionTypes[i].getQualifiedName();
				catchSrcBuf.append("if (__saucWE_.getCause() instanceof ").append(eSrcName).append(//
						") throw (").append(eSrcName).append(")__saucWE_.getCause(); ");
			}
			catchSrcBuf.append("throw new _sauc.StubThrowingException(__saucWE_); }");
			catchSrc = catchSrcBuf.toString();
		}
		return catchSrc;
	}
}
