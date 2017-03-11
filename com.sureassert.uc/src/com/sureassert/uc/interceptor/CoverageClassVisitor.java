package com.sureassert.uc.interceptor;


/**
 * ASM Visitor - add coverage code to project classes.
 * 
 * <ul>
 * <li>Add boolean[] for probes as per http://www.eclemma.org/jacoco/trunk/doc/flow.html
 * <li>Insert probes at each graph edge
 * </ul>
 * To obtain coverage metrics in INCREMENTAL BUILD
 * <ul>
 * <li>Remember classes loaded by SAUCProjectClassLoader during test run
 * <li>For each class in editor, generate coverage data using probes array and class file line
 * number map - @see CoverageData
 * <li>Record coverage data in one Serialized CoverageReport per source file in .sacoverage folder
 * <li>Load and update CoverageReport instance if already existent on filesystem (remove on clean)
 * <li>Generate markers and reports based on data in CoverageReport - read one at a time to obviate
 * memory issues. Reports per file, package, project or workspace.
 * </ul>
 * 
 * Coverage recorded
 * <ul>
 * <li>Line-level markers: NONE/PARTIAL/FULL
 * <li>File-level markers: CoverageData knows all the line numbers; calculate % and report
 * WARN/ERROR
 * <li>
 * </ul>
 * 
 * @author Nathan Dolan
 * 
 */
//public class CoverageClassVisitor extends ClassVisitor {
//
//	public CoverageClassVisitor(ClassVisitor cv) {
//
//		super(Opcodes.ASM4, cv);
//	}
//
//	@Override
//	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//
//		cv.visit(Opcodes.V1_5, access, name, signature, superName, interfaces);
//	}
//
// }
