/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.internal;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.PersistentDataLoadException;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TestDouble;

/**
 * PersistentData for a single project. This data is persisted in memory across project builds
 * and used to support incremental building. It is cleared before a clean (full) build.
 * 
 * @author Nathan Dolan
 */
public class PersistentData implements Serializable {

	private static final long serialVersionUID = 11L;

	private final Map<String, Set<String>> classNamesByJavaPath = new HashMap<String, Set<String>>();

	private final Map<String, String> javaPathByClassName = new HashMap<String, String>();

	/** The set of signatures of members on which each use-case signature (key) is dependent */
	private final Map<Signature, Set<Signature>> dependentMembersByUCSig = new HashMap<Signature, Set<Signature>>();

	/**
	 * The set of signatures of UseCases (e.g. member declaring a named instance) on which
	 * each use-case signature (key) is dependent
	 */
	private final Map<Signature, Set<Signature>> dependencyUCsByUCSig = new HashMap<Signature, Set<Signature>>();

	/**
	 * For runtime dependences, the set of signatures of members on which each use-case signature
	 * (key) is dependent
	 */
	private final Map<Signature, Set<Signature>> runtimeDependentMembersByUCSig = new HashMap<Signature, Set<Signature>>();

	/** The set of signatures of use-cases which invoke each member (key) */
	private final Map<Signature, Set<Signature>> ucClientsByMemberSig = new HashMap<Signature, Set<Signature>>();

	/** For runtime dependences, the set of signatures of use-cases which invoke each member (key) */
	private final Map<Signature, Set<Signature>> runtimeUcClientsByMemberSig = new HashMap<Signature, Set<Signature>>();

	/**
	 * The set of UseCase signatures from other projects on which use-cases from this project
	 * are dependent
	 */
	// private final Set<Signature> foreignUCSigs = new HashSet<Signature>();

	/**
	 * The set of use-case signatures from this project.
	 */
	private final Set<Signature> localUCSigs = new HashSet<Signature>();

	/**
	 * The map of statically-determined dependencies from a given signature (the key) to another
	 * given signature (the value).<br>
	 * A use-case U is said to be dependent on a signature Y if is is known to be dependent on
	 * signature X, and a statically-determined dependency is defined in this map from signature X
	 * to signature Y.
	 */
	private final Map<Signature, Set<Signature>> staticDependsByFrom = new HashMap<Signature, Set<Signature>>();

	/**
	 * The map of statically-determined dependencies to a given signature (the key) from another
	 * given signature (the value). <br>
	 * A use-case U is said to be a client of a signature Y if is is known to be a client of
	 * signature X, and a statically-determined dependency is defined in this map to signature Y
	 * to signature X.
	 */
	private final Map<Signature, Set<Signature>> staticDependsByTo = new HashMap<Signature, Set<Signature>>();

	/** Key = name; value = full class name */
	private final Map<String, String> namedClasses = new HashMap<String, String>();

	private final Map<String, String> fullyQualifiedBySimpleClassName = new HashMap<String, String>();

	private final Set<String> duplicateSimpleClassNames = new HashSet<String>();

	/**
	 * The signature of the member that declares each named instance.
	 * Key=named instance name; value=Signature of declaring member.
	 */
	private final Map<String, Signature> sigByNI = new HashMap<String, Signature>();
	private final Map<Signature, Set<String>> niBySig = new HashMap<Signature, Set<String>>();

	private final Map<String, TestDouble> testDoubleByDoubledClassName = new HashMap<String, TestDouble>();
	private final Map<String, TestDouble> testDoubleByTDClassName = new HashMap<String, TestDouble>();
	private final Map<String, TestDouble> testDoubleByJavaPathStr = new HashMap<String, TestDouble>();
	private final Map<String, String> doubledSourceFilePathByTDClassName = new HashMap<String, String>();
	private final Map<String, String> tdSourceFilePathByTDClassName = new HashMap<String, String>();

	private final Map<String, Signature> sinTypeMethodByPrefix = new HashMap<String, Signature>();
	private final Map<Signature, String> sinTypePrefixByMethod = new HashMap<Signature, String>();

	private final Map<String, float[]> lineCoveragePercentByClassName = new HashMap<String, float[]>();
	
	private final Map<String, List<Integer>> ignoreCoverageRangesByClassName = new HashMap<String, List<Integer>>();

	private final Object cacheLock = new String();

	private final String projectName;

	public PersistentData(String projectName) {

		this.projectName = projectName;
	}

	public String getProjectName() {

		return projectName;
	}

	public void registerProjectClass(String pathStr, String className) throws ClassNotFoundException {

		BasicUtils.mapSetAdd(classNamesByJavaPath, pathStr, className);
		javaPathByClassName.put(className, pathStr);
	}

	public boolean isProjectClass(String className) {

		return javaPathByClassName.containsKey(className);
	}

	public void registerProjectUC(Signature ucSig) {

		localUCSigs.add(ucSig);
	}

	public boolean isProjectUC(Signature ucSig) {

		return localUCSigs.contains(ucSig);
	}

	public int getNumUseCaseSigs() {

		return localUCSigs.size();
	}
	
	public Set<String> getAllProjectClasses() {
		
		return javaPathByClassName.keySet();
	}

	public Set<String> getClassNames(String pathStr) {

		Set<String> classNames = classNamesByJavaPath.get(pathStr.toString());
		return classNames == null ? new HashSet<String>() : classNames;
	}

	public String getJavaPathStr(String className) {

		return javaPathByClassName.get(className);
	}
	
	public void registerIgnoreCoverageClass(String className) {
		
		ignoreCoverageRangesByClassName.put(className, null);
	}
	
	public void registerIgnoreCoverageMethods(String className, List<Integer> methodStartEndLinePairs) {
		
		ignoreCoverageRangesByClassName.put(className, methodStartEndLinePairs);
	}
	
	public boolean isIgnoreCoverageClass(String className) {
		
		return ignoreCoverageRangesByClassName.containsKey(className) && //
			ignoreCoverageRangesByClassName.get(className) == null;
	}
	
	public boolean hasIgnoreCoverageMethods(String className) {
		
		return ignoreCoverageRangesByClassName.containsKey(className) && //
			ignoreCoverageRangesByClassName.get(className) != null;
	}
	
	public List<Integer> getIgnoreCoverageStartEndLines(String className) {
		
		return ignoreCoverageRangesByClassName.get(className);
	}

	/**
	 * Registers that a use-case has invoked (directly or directly) the given member signature.
	 * 
	 * @param called The signature of a member (method/constructor/field) that has been invoked.
	 * @param useCase The use-case being executed.
	 * @param isUseCaseDepends True if the called member is a UseCase, e.g. defining a named
	 *            instance, and the dependency is on the UseCase itself rather than the member.
	 */
	public void registerDependency(Signature called, Signature useCase, boolean isUseCaseDepends) {

		_registerDependency(called, useCase, isUseCaseDepends, false);
	}

	/**
	 * ---ONLY FOR USE DURING USE-CASE EXECUTION---
	 * Registers that a use-case has invoked (directly or directly) the given member signature.
	 * 
	 * @param called The signature of a member (method/constructor/field) that has been invoked.
	 * @param useCase The use-case being executed.
	 * @param isUseCaseDepends True if the called member is a UseCase, e.g. defining a named
	 *            instance, and the dependency is on the UseCase itself rather than the member.
	 */
	public void registerRuntimeDependency(Signature called, Signature useCase, boolean isUseCaseDepends) {

		_registerDependency(called, useCase, isUseCaseDepends, true);
	}

	private void _registerDependency(Signature called, Signature useCase, boolean isUseCaseDepends, boolean isRuntimeDepends) {

		if (useCase == null || called == null || useCase.equals(called))
			return;
		if (isRuntimeDepends) {
			BasicUtils.mapSetAdd(runtimeDependentMembersByUCSig, useCase, called);
			BasicUtils.mapSetAdd(runtimeUcClientsByMemberSig, called, useCase);
		} else {
			BasicUtils.mapSetAdd(dependentMembersByUCSig, useCase, called);
			BasicUtils.mapSetAdd(ucClientsByMemberSig, called, useCase);
		}
		if (isUseCaseDepends)
			BasicUtils.mapSetAdd(dependencyUCsByUCSig, useCase, called);

		// If the called class has a test double, add a dependency from the current UC to the test
		// double class.
		TestDouble classDouble = getClassDoubleForDoubledClassName(called.getClassName());
		if (classDouble != null) {
			Signature testDoubleClassSig = SignatureTableFactory.instance.getSignatureForClass(//
					classDouble.getTestDoubleClassName());
			_registerDependency(testDoubleClassSig, useCase, isUseCaseDepends, isRuntimeDepends);
		}
		// If the called class does not exist in this project add it to the foreignPESigs set.
		// if (isUseCaseDepends && !javaPathByClassName.keySet().contains(called.getClassName())) {
		// foreignUCSigs.add(called);
		// }
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

		// System.out.println(">>>>Register from " + from.toString() + " to " + to.toString());
		BasicUtils.mapSetAdd(staticDependsByFrom, from, to);
		BasicUtils.mapSetAdd(staticDependsByTo, to, from);
		if (toIsSuperSig) {
			// NOTE: All client sigs must be UC sigs except for implementation sigs
			// of a superclass/interface UC, in which case the client sig will be registered
			// as a UseCase signature as it inherits a UseCase
			localUCSigs.add(from);
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

		return getClientUseCases(sig, new HashSet<Signature>());
	}

	private Set<Signature> getClientUseCases(Signature sig, Set<Signature> processedSigs) {

		// Note: Client relationships can be recursive so guard against this
		if (processedSigs.contains(sig))
			return new HashSet<Signature>();
		processedSigs.add(sig);
		Set<Signature> ucSigs = ucClientsByMemberSig.get(sig);
		if (ucSigs == null)
			ucSigs = new HashSet<Signature>();
		Set<Signature> runtimeUcSigs = runtimeUcClientsByMemberSig.get(sig);
		if (runtimeUcSigs != null)
			ucSigs.addAll(runtimeUcSigs);
		// Add sigs of client UCs of sigs which depend on the given sig
		Set<Signature> clientSigsOfSig = staticDependsByTo.get(sig);
		if (clientSigsOfSig != null) {
			for (Signature clientSigOfSig : clientSigsOfSig) {
				ucSigs.addAll(getClientUseCases(clientSigOfSig, processedSigs));
				if (PersistentDataFactory.getInstance().isUseCaseSignature(clientSigOfSig))
					ucSigs.add(clientSigOfSig);
			}
		}
		// For member sigs, Add any sigs defined as clients of the defining class
		if (sig.getMemberName() != null) {
			Signature classSig = SignatureTableFactory.instance.getSignatureForClass(sig.getClassName());
			ucSigs.addAll(getClientUseCases(classSig, processedSigs));
		}
		return ucSigs;
	}

	/**
	 * Gets the set of signatures of UseCases which the UseCase with the given
	 * signature requires for successful execution.
	 * 
	 * @param ucSig
	 * @return
	 */
	public Set<Signature> getDependencyUseCases(Signature ucSig, Set<Signature> processedSigs) {

		// Note: UC depends relationships can be recursive so guard against this
		if (ucSig == null || processedSigs.contains(ucSig))
			return new HashSet<Signature>();
		processedSigs.add(ucSig);

		Set<Signature> sigs = dependencyUCsByUCSig.get(ucSig);
		if (sigs == null)
			sigs = new HashSet<Signature>();

		// Add depends of sigs which are statically associated with the UC (e.g. inherited)
		// System.out.println("!>>>Get depends for " + ucSig.toString());
		Set<Signature> ucDependSigsOfSig = staticDependsByTo.get(ucSig);
		if (ucDependSigsOfSig != null) {
			// PersistentDataFactory pdf = PersistentDataFactory.getInstance();
			for (Signature ucDependSigOfSig : ucDependSigsOfSig) {
				sigs.addAll(getDependencyUseCases(ucDependSigOfSig, processedSigs));
			}
		}

		return sigs;
	}

	/**
	 * Gets the set of signatures of non-UseCases which the use-case with the given
	 * signature requires for successful execution.
	 * 
	 * @param ucSig
	 * @return
	 */
	public Set<Signature> getDependencyMembers(Signature ucSig) {

		return getDependencyMembers(ucSig, new HashSet<Signature>());
	}

	private Set<Signature> getDependencyMembers(Signature ucSig, Set<Signature> processedSigs) {

		Set<Signature> sigs = dependentMembersByUCSig.get(ucSig);
		if (sigs == null) {
			sigs = new HashSet<Signature>();
		}
		Set<Signature> runtimeSigs = runtimeDependentMembersByUCSig.get(ucSig);
		if (runtimeSigs != null)
			sigs.addAll(runtimeSigs);
		// } else {
		// Add sigs on which ucSig or any dependency of ucSig are dependent
		for (Signature sig : new HashSet<Signature>(sigs)) {
			sigs.addAll(getDependencyMembers(sig, processedSigs));
		}
		sigs.addAll(BasicUtils.mapGetSetNoNull(staticDependsByFrom, ucSig));
		// }
		return sigs;
	}

	public void addNamedClass(String name, String className) {

		String simpleClassName = BasicUtils.getSimpleClassName(className);
		if (simpleClassName.equals(name)) {
			// simple->fully qualified name
			// if (fullyQualifiedBySimpleClassName.containsKey(simpleClassName) && //
			// !fullyQualifiedBySimpleClassName.get(simpleClassName).equals(name)) {
			// // register duplicate; remove simple class name mappings
			// duplicateSimpleClassNames.add(simpleClassName);
			// System.err.println(projectName + ": removeName(" + simpleClassName + ")");
			// fullyQualifiedBySimpleClassName.remove(simpleClassName);
			// } else {
			fullyQualifiedBySimpleClassName.put(simpleClassName, className);
			// }
		} else {
			// user-defined class name mapping
			namedClasses.put(name, className);
		}
	}

	public String getFullClassName(String simpleClassName) {

		String fullClassName = fullyQualifiedBySimpleClassName.get(simpleClassName);
		if (fullClassName == null)
			fullClassName = namedClasses.get(simpleClassName);
		return fullClassName;
	}

	// public Set<Signature> getForeignUCSignatures() {
	//
	// return foreignUCSigs;
	// }

	public void registerDeclaringSignature(Signature sig, String instanceName) {

		sigByNI.put(instanceName, sig);
		BasicUtils.mapSetAdd(niBySig, sig, instanceName);
	}

	public Signature getDeclaringSignature(String instanceName) {

		return sigByNI.get(instanceName);
	}

	public void registerSINType(String prefix, Signature sig) {

		sinTypeMethodByPrefix.put(prefix, sig);
		sinTypePrefixByMethod.put(sig, prefix);
	}

	public String getSinTypePrefixByMethod(Signature sig) {

		return sinTypePrefixByMethod.get(sig);
	}

	public Signature getSinTypeMethodByPrefix(String prefix) {

		return sinTypeMethodByPrefix.get(prefix);
	}

	public void addClassDouble(TestDouble classDouble, Signature declaringMemberSig) throws ClassNotFoundException {

		synchronized (cacheLock) {
			testDoubleByDoubledClassName.put(classDouble.getDoubledClassName(), classDouble);
			testDoubleByTDClassName.put(classDouble.getTestDoubleClassName(), classDouble);
			testDoubleByJavaPathStr.put(classDouble.getJavaPathStr(), classDouble);
		}
	}

	public void setDoubledClassSourcePath(String tdClass, String doubledClassSourcePath) {

		doubledSourceFilePathByTDClassName.put(tdClass, doubledClassSourcePath);
	}

	public void setTDClassSourcePath(String tdClass, String tdClassSourcePath) {

		tdSourceFilePathByTDClassName.put(tdClass, tdClassSourcePath);
	}

	public String getDoubledClassSourcePath(String tdClass) {

		return doubledSourceFilePathByTDClassName.get(tdClass);
	}

	public String getTDClassSourcePath(String tdClass) {

		return tdSourceFilePathByTDClassName.get(tdClass);
	}

	// /**
	// * Registers that the given class has the given number of source code lines.
	// * Must be called for any changed class before any calls to registerCoverage.
	// *
	// * @param className The full class name
	// * @param numLines The number of lines in the class
	// */
	// public void initCoverageLines(String className, int numLines) {
	//
	// byte[] coveredLines = coveredLinesByClassName.get(className);
	// if (coveredLines == null || coveredLines.length != numLines)
	// coveredLinesByClassName.put(className, new byte[numLines]);
	// }

	public void setCoverage(String className, float[] percentCoverageByLine) {

		lineCoveragePercentByClassName.put(className, percentCoverageByLine);
	}

	public float[] getCoverage(String className) {

		return lineCoveragePercentByClassName.get(className);
	}

	public float getCoveragePercentForLine(String className, char line) {

		return lineCoveragePercentByClassName.get(className)[line];
	}

	public TestDouble getClassDoubleForTDClassName(String tdClassName) {

		return testDoubleByTDClassName.get(tdClassName);
	}

	public TestDouble getClassDoubleForDoubledClassName(String doubledClassName) {

		return testDoubleByDoubledClassName.get(doubledClassName);
	}

	public TestDouble getClassDoubleForJavaPath(String javaPathStr) {

		return testDoubleByJavaPathStr.get(javaPathStr);
	}

	public void serialize(String projectName, File projectLocation) {

		File pdFile = new File(projectLocation, ".sauc.obj");
		// BasicUtils.debug("Serializing PersistentData for project " + projectName + " to " +
		// pdFile.getAbsolutePath());
		BasicUtils.serialize(this, pdFile);
	}

	public void deletePersistentStore(String projectName, File projectLocation) {

		File pdFile = new File(projectLocation, ".sauc.obj");
		// BasicUtils.debug("Deleting PersistentData for project " + projectName + " from " +
		// pdFile.getAbsolutePath());
		pdFile.delete();
	}

	public static PersistentData load(String projectName, File projectLocation) throws PersistentDataLoadException {

		try {
			File pdFile = new File(projectLocation, ".sauc.obj");
			if (!pdFile.canRead())
				throw new PersistentDataLoadException("Cannot read file " + pdFile.getAbsolutePath());
			// BasicUtils.debug("Loading PersistentData for project " + projectName + " from " +
			// pdFile.getAbsolutePath());
			return (PersistentData) BasicUtils.deserialize(pdFile);

		} catch (Throwable e) {
			throw new PersistentDataLoadException(e);
		}
	}

	public void unregisterStaticUCData(Signature ucSig) {

		// Unregister static dependencies of this UC
		Set<Signature> dependSigs = staticDependsByFrom.remove(ucSig);
		if (dependSigs != null) {
			for (Signature dependSig : dependSigs) {
				// Find UC in static client sigs
				Set<Signature> dependClients = staticDependsByTo.get(dependSig);
				if (dependClients.remove(ucSig)) {
					if (dependClients.isEmpty()) {
						staticDependsByTo.remove(dependSig);
					}
				}
			}
		}

		dependencyUCsByUCSig.remove(ucSig);
		localUCSigs.remove(ucSig);
		Set<String> sigNIs = niBySig.remove(ucSig);
		if (sigNIs != null)
			BasicUtils.mapRemoveAll(sigByNI, sigNIs);

		// Unregister non-runtime dependencies of this UC
		dependSigs = dependentMembersByUCSig.remove(ucSig);
		if (dependSigs != null) {
			for (Signature dependSig : dependSigs) {
				// Find UC in client sigs
				Set<Signature> dependClients = ucClientsByMemberSig.get(dependSig);
				if (dependClients.remove(ucSig)) {
					if (dependClients.isEmpty()) {
						ucClientsByMemberSig.remove(dependSig);
					}
				}
			}
		}

		String removedSINTypePrefix = sinTypePrefixByMethod.remove(ucSig);
		if (removedSINTypePrefix != null)
			sinTypeMethodByPrefix.remove(removedSINTypePrefix);
	}

	public void unregisterRuntimeUseCaseData(Signature ucSig) {

		// Unregister runtime dependencies of this UC
		Set<Signature> dependSigs = runtimeDependentMembersByUCSig.remove(ucSig);
		if (dependSigs != null) {
			for (Signature dependSig : dependSigs) {
				// Find UC in client sigs
				Set<Signature> dependClients = runtimeUcClientsByMemberSig.get(dependSig);
				if (dependClients.remove(ucSig)) {
					if (dependClients.isEmpty()) {
						runtimeUcClientsByMemberSig.remove(dependSig);
					}
				}
			}
		}
	}

	public void unregisterAllClassData(String className, ClassLoader classLoader) {

		if (classLoader != null) {
			try {
				Set<Signature> classSigs = SignatureTableFactory.instance.getAllSignatures(className, classLoader);
				for (Signature classSig : classSigs) {
					unregisterRuntimeUseCaseData(classSig);
					unregisterStaticUCData(classSig);
				}
			} catch (ClassNotFoundException cnfe) {
				// Ignore
			}
		} else {
			for (Signature projectSig : new HashSet<Signature>(localUCSigs)) {
				if (projectSig != null && projectSig.getClassName() != null && projectSig.getClassName().equals(className)) {
					unregisterRuntimeUseCaseData(projectSig);
					unregisterStaticUCData(projectSig);
				}
			}
		}
	}

	public void unregisterStaticClassData(String className, ClassLoader classLoader) {

		String javaPath = javaPathByClassName.remove(className);
		classNamesByJavaPath.remove(javaPath);
		fullyQualifiedBySimpleClassName.remove(BasicUtils.getSimpleClassName(className));
		ignoreCoverageRangesByClassName.remove(className);

		// Remove TestDouble (if className is the @TestDouble)
		doubledSourceFilePathByTDClassName.remove(className);
		TestDouble removedTD = testDoubleByTDClassName.remove(className);
		if (removedTD != null) {
			testDoubleByDoubledClassName.remove(removedTD.getDoubledClassName());
			testDoubleByJavaPathStr.remove(removedTD.getJavaPathStr());
		}

		// Removed named class
		String removeNamedClass = null;
		for (Entry<String, String> namedClassesEntry : namedClasses.entrySet()) {
			if (namedClassesEntry.getValue() != null && namedClassesEntry.getValue().equals(className)) {
				removeNamedClass = namedClassesEntry.getKey();
				break;
			}
		}
		if (removeNamedClass != null)
			namedClasses.remove(removeNamedClass);

		// Remove coverage data
		lineCoveragePercentByClassName.remove(className);

		// Unregister dependencies of this class
		if (classLoader != null) {
			try {
				Set<Signature> classSigs = SignatureTableFactory.instance.getAllSignatures(className, classLoader);
				for (Signature classSig : classSigs) {
					unregisterStaticUCData(classSig);
				}
			} catch (ClassNotFoundException cnfe) {
				// Ignore
			} catch (NoClassDefFoundError ncdfe) {
				// Ignore
			}
		} else {
			for (Signature projectSig : new HashSet<Signature>(localUCSigs)) {
				if (projectSig.getClassName().equals(className)) {
					unregisterStaticUCData(projectSig);
				}
			}
		}
	}

	// public void registerDeletedClass(String className) {
	//
	// // EAUtils.mapCollectionRemove(classNamesByJavaPath, className);
	// javaPathByClassName.remove(className);
	// EAUtils.mapCollectionRemove(dependentMembersByUCSig, className);
	// EAUtils.mapRemoveAll(dependentMembersByUCSig, EAUtils.getClassSigs(className,
	// dependentMembersByUCSig.keySet()));
	// EAUtils.mapCollectionRemove(ucClientsByMemberSig, className);
	// EAUtils.mapRemoveAll(ucClientsByMemberSig, EAUtils.getClassSigs(className,
	// ucClientsByMemberSig.keySet()));
	// EAUtils.mapCollectionRemove(dependencyUCsByUCSig, className);
	// EAUtils.mapRemoveAll(dependencyUCsByUCSig, EAUtils.getClassSigs(className,
	// dependencyUCsByUCSig.keySet()));
	// foreignUCSigs.removeAll(EAUtils.getClassSigs(className, foreignUCSigs));
	// localUCSigs.removeAll(EAUtils.getClassSigs(className, localUCSigs));
	// EAUtils.mapCollectionRemove(staticDependsByFrom, className);
	// EAUtils.mapRemoveAll(staticDependsByFrom, EAUtils.getClassSigs(className,
	// staticDependsByFrom.keySet()));
	// EAUtils.mapCollectionRemove(staticDependsByTo, className);
	// EAUtils.mapRemoveAll(staticDependsByTo, EAUtils.getClassSigs(className,
	// staticDependsByTo.keySet()));
	// EAUtils.mapRemoveAll(niDeclaredSigs, EAUtils.getClassKeys(className, niDeclaredSigs));
	// }

	public void clear() {

		classNamesByJavaPath.clear();
		javaPathByClassName.clear();
		dependentMembersByUCSig.clear();
		runtimeDependentMembersByUCSig.clear();
		ucClientsByMemberSig.clear();
		runtimeUcClientsByMemberSig.clear();
		dependencyUCsByUCSig.clear();
		// foreignUCSigs.clear(.clear();
		localUCSigs.clear();
		staticDependsByFrom.clear();
		staticDependsByTo.clear();
		sigByNI.clear();
		niBySig.clear();
		namedClasses.clear();
		fullyQualifiedBySimpleClassName.clear();
		duplicateSimpleClassNames.clear();
		testDoubleByDoubledClassName.clear();
		testDoubleByTDClassName.clear();
		testDoubleByJavaPathStr.clear();
		tdSourceFilePathByTDClassName.clear();
		doubledSourceFilePathByTDClassName.clear();
		sinTypeMethodByPrefix.clear();
		sinTypePrefixByMethod.clear();
		lineCoveragePercentByClassName.clear();
		ignoreCoverageRangesByClassName.clear();
	}

}
