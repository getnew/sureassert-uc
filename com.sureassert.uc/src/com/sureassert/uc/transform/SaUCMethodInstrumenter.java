package com.sureassert.uc.transform;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;

import org.jacoco.core.internal.flow.IMethodProbesVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;

import com.sureassert.uc.runtime.Signature;

public class SaUCMethodInstrumenter implements IMethodProbesVisitor {

	private final IMethodProbesVisitor decorated;

	private final Signature methodSig;

	private int currentLocalVarIdx = -1;

	public SaUCMethodInstrumenter(IMethodProbesVisitor decorated, Signature methodSig) {

		this.decorated = decorated;
		this.methodSig = methodSig;
	}

	public void visitCode() {

		decorated.visitCode();

		String[] paramTypes = methodSig.getParamClassNames();

		if (methodSig.getParamClassNames() != null && paramTypes.length == 0) {
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
			Label l3 = new Label();
			visitLabel(l3);
			visitLineNumber(47, l3);
			visitFieldInsn(GETSTATIC, "_sauc/SAInterceptor", "instance", "L_sauc/SAInterceptor;");
			Label l4 = new Label();
			visitLabel(l4);
			visitLineNumber(48, l4);
			visitFieldInsn(GETSTATIC, "com/sureassert/uc/runtime/SignatureTableFactory", "instance", "Lcom/sureassert/uc/runtime/SignatureTableFactory;");
			Label l5 = new Label();
			visitLabel(l5);
			visitLineNumber(49, l5);
			visitLdcInsn("myClass");
			visitLdcInsn("myMethodName");
			visitLdcInsn("myParamsList");
			Label l6 = new Label();
			visitLabel(l6);
			visitLineNumber(48, l6);
			visitMethodInsn(INVOKEVIRTUAL, "com/sureassert/uc/runtime/SignatureTableFactory", "getSignature",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/sureassert/uc/runtime/Signature;");
			Label l7 = new Label();
			visitLabel(l7);
			visitLineNumber(49, l7);
			visitVarInsn(ALOAD, 0);
			visitInsn(ICONST_0);
			visitTypeInsn(ANEWARRAY, "java/lang/Object");
			Label l8 = new Label();
			visitLabel(l8);
			visitLineNumber(47, l8);
			visitMethodInsn(INVOKEVIRTUAL, "_sauc/SAInterceptor", "performAdvice",
					"(Lcom/sureassert/uc/runtime/Signature;Ljava/lang/Object;[Ljava/lang/Object;)L_sauc/SAInterceptor$SaUCAdviceResponse;");
			visitVarInsn(ASTORE, 8);
			Label l9 = new Label();
			visitLabel(l9);
			visitLineNumber(50, l9);
			visitVarInsn(ALOAD, 8);
			visitFieldInsn(GETFIELD, "_sauc/SAInterceptor$SaUCAdviceResponse", "doProceed", "Z");
			visitJumpInsn(IFNE, l0);
			Label l10 = new Label();
			visitLabel(l10);
			visitLineNumber(51, l10);
			visitVarInsn(ALOAD, 8);
			visitFieldInsn(GETFIELD, "_sauc/SAInterceptor$SaUCAdviceResponse", "stubRetval", "Ljava/lang/Object;");
			visitTypeInsn(CHECKCAST, "java/lang/String");
			visitInsn(ARETURN);
			visitLabel(l0);
			visitLineNumber(56, l0);
			visitLdcInsn("rest of method");
			visitVarInsn(ASTORE, 8);
			Label l11 = new Label();
			visitLabel(l11);
			visitLineNumber(57, l11);
			visitVarInsn(ALOAD, 8);
			visitLabel(l1);
			visitInsn(ARETURN);
			visitLabel(l2);
			visitLineNumber(58, l2);
			visitVarInsn(ASTORE, 8);
			Label l12 = new Label();
			visitLabel(l12);
			visitLineNumber(59, l12);
			visitTypeInsn(NEW, "java/lang/RuntimeException");
			visitInsn(DUP);
			visitVarInsn(ALOAD, 8);
			visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
			visitInsn(ATHROW);
			Label l13 = new Label();
			visitLabel(l13);
			visitLocalVariable("this", "Lcom/sureassert/uc/transformed/SaUCClassInstrumenter;", null, l3, l13, 0);
			visitLocalVariable("arg1", "D", null, l3, l13, 1);
			visitLocalVariable("arg2", "Ljava/lang/String;", null, l3, l13, 3);
			visitLocalVariable("arg3", "[Lcom/sureassert/uc/runtime/IntHolder;", null, l3, l13, 4);
			visitLocalVariable("arg4", "Z", null, l3, l13, 5);
			visitLocalVariable("arg5", "D", null, l3, l13, 6);
			visitLocalVariable("response", "L_sauc/SAInterceptor$SaUCAdviceResponse;", null, l9, l0, 8);
			visitLocalVariable("myMethodExec", "Ljava/lang/String;", null, l11, l2, 8);
			visitLocalVariable("myMethodThrowable", "Ljava/lang/Throwable;", null, l12, l13, 8);
		}
	}

	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {

		this.currentLocalVarIdx = index;
		decorated.visitLocalVariable(name, desc, signature, start, end, index);
	}

	public void visitEnd() {

		// visitLocalVariable("response", "L_sauc/SAInterceptor$SaUCAdviceResponse;", null, l9, l0,
		// 8);
		decorated.visitEnd();
	}

	// ---------------------------------------------------

	public void visitProbe(int paramInt) {

		decorated.visitProbe(paramInt);
	}

	public void visitJumpInsnWithProbe(int paramInt1, Label paramLabel, int paramInt2) {

		decorated.visitJumpInsnWithProbe(paramInt1, paramLabel, paramInt2);
	}

	public void visitInsnWithProbe(int paramInt1, int paramInt2) {

		decorated.visitInsnWithProbe(paramInt1, paramInt2);
	}

	public void visitTableSwitchInsnWithProbes(int paramInt1, int paramInt2, Label paramLabel, Label[] paramArrayOfLabel) {

		decorated.visitTableSwitchInsnWithProbes(paramInt1, paramInt2, paramLabel, paramArrayOfLabel);
	}

	public void visitLookupSwitchInsnWithProbes(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel) {

		decorated.visitLookupSwitchInsnWithProbes(paramLabel, paramArrayOfInt, paramArrayOfLabel);
	}

	public AnnotationVisitor visitAnnotationDefault() {

		return decorated.visitAnnotationDefault();
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

		return decorated.visitAnnotation(desc, visible);
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {

		return decorated.visitParameterAnnotation(parameter, desc, visible);
	}

	public void visitAttribute(Attribute attr) {

		decorated.visitAttribute(attr);
	}

	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {

		decorated.visitFrame(type, nLocal, local, nStack, stack);
	}

	public void visitInsn(int opcode) {

		decorated.visitInsn(opcode);
	}

	public void visitIntInsn(int opcode, int operand) {

		decorated.visitIntInsn(opcode, operand);
	}

	public void visitVarInsn(int opcode, int var) {

		decorated.visitVarInsn(opcode, var);
	}

	public void visitTypeInsn(int opcode, String type) {

		decorated.visitTypeInsn(opcode, type);
	}

	public void visitFieldInsn(int opcode, String owner, String name, String desc) {

		decorated.visitFieldInsn(opcode, owner, name, desc);
	}

	public void visitMethodInsn(int opcode, String owner, String name, String desc) {

		decorated.visitMethodInsn(opcode, owner, name, desc);
	}

	public void visitJumpInsn(int opcode, Label label) {

		decorated.visitJumpInsn(opcode, label);
	}

	public void visitLabel(Label label) {

		decorated.visitLabel(label);
	}

	public void visitLdcInsn(Object cst) {

		decorated.visitLdcInsn(cst);
	}

	public void visitIincInsn(int var, int increment) {

		decorated.visitIincInsn(var, increment);
	}

	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {

		decorated.visitTableSwitchInsn(min, max, dflt, labels);
	}

	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {

		decorated.visitLookupSwitchInsn(dflt, keys, labels);
	}

	public void visitMultiANewArrayInsn(String desc, int dims) {

		decorated.visitMultiANewArrayInsn(desc, dims);
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {

		decorated.visitTryCatchBlock(start, end, handler, type);
	}

	public void visitLineNumber(int line, Label start) {

		decorated.visitLineNumber(line, start);
	}

	public void visitMaxs(int maxStack, int maxLocals) {

		decorated.visitMaxs(maxStack, maxLocals);
	}

}
