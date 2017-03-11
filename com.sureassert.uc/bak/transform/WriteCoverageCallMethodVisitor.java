package com.sureassert.uc.transform;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.PersistentDataFactory;

public final class WriteCoverageCallMethodVisitor extends MethodVisitor {

	private final String binClassName;
	private final String methodName;
	private int maxLine = 0;
	private int currentLine = 0;
	private final Map<Integer, Integer> numInstructionsByLine = new HashMap<Integer, Integer>();

	public WriteCoverageCallMethodVisitor(MethodVisitor mv, String className, String methodName) {

		super(Opcodes.ASM4, mv);
		this.binClassName = className;
		this.methodName = methodName;
	}

	@Override
	public void visitCode() {

		// System.out.println("VisitCode (" + className + "." + methodName + ")");
		super.visitCode();

		// String className = BasicUtils.getClassNameFromJaCoCoClassName(binClassName);
		// mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
		// "Ljava/io/PrintStream;");
		// mv.visitLdcInsn(className + "." + methodName);
		// mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
		// "(Ljava/lang/String;)V");
	}

	@Override
	public void visitLineNumber(int line, Label start) {

		currentLine = line;
		if (line > maxLine)
			maxLine = line;

		super.visitLineNumber(line, start);

		/*
		 * INVOKESTATIC testPackage/TestSingleton.getInstance ()LtestPackage/TestSingleton;
		 * LDC "llamaClassName"
		 * SIPUSH 1979
		 * INVOKEVIRTUAL testPackage/TestSingleton.registerCoverage (Ljava/lang/String;I)V
		 */

		String className = BasicUtils.getClassNameFromJaCoCoClassName(binClassName);
		// System.out.println("VisitLineNumber (" + className + ":" + line);
		PersistentDataFactory.getInstance().registerCoverageRequired(className, line);

		// mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
		// "Ljava/io/PrintStream;");
		// mv.visitLdcInsn(className + "." + methodName + " line " + line);
		// mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
		// "(Ljava/lang/String;)V");

		visitMethodInsn(Opcodes.INVOKESTATIC, "com/sureassert/uc/runtime/PersistentDataFactory", //
				"getInstance", "()Lcom/sureassert/uc/runtime/PersistentDataFactory;");
		visitLdcInsn(className);
		visitIntInsn(Opcodes.SIPUSH, line);
		visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/sureassert/uc/runtime/PersistentDataFactory", //
				"registerCoverage", "(Ljava/lang/String;I)V");

	}

	@Override
	public void visitEnd() {

		super.visitEnd();

		// PersistentDataFactory.getInstance().initCoverageLines(BasicUtils.getClassNameFromJaCoCoClassName(className),
		// maxLine);
	}

	// VISIT INSTRUCTIONS

	private void incrementNumInstructionsForLine(int lineNum) {

		Integer numInsns = numInstructionsByLine.get(lineNum);
		if (numInsns == null) {
			numInstructionsByLine.put(lineNum, 1);
		} else {
			numInstructionsByLine.put(lineNum, numInsns + 1);
		}
	}

	@Override
	public void visitInsn(int opcode) {

		incrementNumInstructionsForLine(currentLine);
		super.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {

		incrementNumInstructionsForLine(currentLine);
		super.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {

		incrementNumInstructionsForLine(currentLine);
		super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {

		incrementNumInstructionsForLine(currentLine);
		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {

		incrementNumInstructionsForLine(currentLine);
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {

		incrementNumInstructionsForLine(currentLine);
		super.visitMethodInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {

		incrementNumInstructionsForLine(currentLine);
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {

		incrementNumInstructionsForLine(currentLine);
		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLdcInsn(Object cst) {

		incrementNumInstructionsForLine(currentLine);
		super.visitLdcInsn(cst);
	}

	@Override
	public void visitIincInsn(int var, int increment) {

		incrementNumInstructionsForLine(currentLine);
		super.visitIincInsn(var, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {

		incrementNumInstructionsForLine(currentLine);
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {

		incrementNumInstructionsForLine(currentLine);
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {

		incrementNumInstructionsForLine(currentLine);
		super.visitMultiANewArrayInsn(desc, dims);
	}

}