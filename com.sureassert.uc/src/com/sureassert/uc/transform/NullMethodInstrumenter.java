package com.sureassert.uc.transform;

import org.jacoco.core.internal.flow.IMethodProbesVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;

public class NullMethodInstrumenter implements IMethodProbesVisitor {

	public AnnotationVisitor visitAnnotationDefault() {

		return null;
	}

	public AnnotationVisitor visitAnnotation(String paramString, boolean paramBoolean) {

		// TODO Auto-generated method stub
		return null;
	}

	public AnnotationVisitor visitParameterAnnotation(int paramInt, String paramString, boolean paramBoolean) {

		// TODO Auto-generated method stub
		return null;
	}

	public void visitAttribute(Attribute paramAttribute) {

		// TODO Auto-generated method stub

	}

	public void visitCode() {

		// TODO Auto-generated method stub

	}

	public void visitFrame(int paramInt1, int paramInt2, Object[] paramArrayOfObject1, int paramInt3, Object[] paramArrayOfObject2) {

		// TODO Auto-generated method stub

	}

	public void visitInsn(int paramInt) {

		// TODO Auto-generated method stub

	}

	public void visitIntInsn(int paramInt1, int paramInt2) {

		// TODO Auto-generated method stub

	}

	public void visitVarInsn(int paramInt1, int paramInt2) {

		// TODO Auto-generated method stub

	}

	public void visitTypeInsn(int paramInt, String paramString) {

		// TODO Auto-generated method stub

	}

	public void visitFieldInsn(int paramInt, String paramString1, String paramString2, String paramString3) {

		// TODO Auto-generated method stub

	}

	public void visitMethodInsn(int paramInt, String paramString1, String paramString2, String paramString3) {

		// TODO Auto-generated method stub

	}

	public void visitJumpInsn(int paramInt, Label paramLabel) {

		// TODO Auto-generated method stub

	}

	public void visitLabel(Label paramLabel) {

		// TODO Auto-generated method stub

	}

	public void visitLdcInsn(Object paramObject) {

		// TODO Auto-generated method stub

	}

	public void visitIincInsn(int paramInt1, int paramInt2) {

		// TODO Auto-generated method stub

	}

	public void visitTableSwitchInsn(int paramInt1, int paramInt2, Label paramLabel, Label[] paramArrayOfLabel) {

		// TODO Auto-generated method stub

	}

	public void visitLookupSwitchInsn(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel) {

		// TODO Auto-generated method stub

	}

	public void visitMultiANewArrayInsn(String paramString, int paramInt) {

		// TODO Auto-generated method stub

	}

	public void visitTryCatchBlock(Label paramLabel1, Label paramLabel2, Label paramLabel3, String paramString) {

		// TODO Auto-generated method stub

	}

	public void visitLocalVariable(String paramString1, String paramString2, String paramString3, Label paramLabel1, Label paramLabel2, int paramInt) {

		// TODO Auto-generated method stub

	}

	public void visitLineNumber(int paramInt, Label paramLabel) {

		// TODO Auto-generated method stub

	}

	public void visitMaxs(int paramInt1, int paramInt2) {

		// TODO Auto-generated method stub

	}

	public void visitEnd() {

		// TODO Auto-generated method stub

	}

	public void visitProbe(int paramInt) {

		// TODO Auto-generated method stub

	}

	public void visitJumpInsnWithProbe(int paramInt1, Label paramLabel, int paramInt2) {

		// TODO Auto-generated method stub

	}

	public void visitInsnWithProbe(int paramInt1, int paramInt2) {

		// TODO Auto-generated method stub

	}

	public void visitTableSwitchInsnWithProbes(int paramInt1, int paramInt2, Label paramLabel, Label[] paramArrayOfLabel) {

		// TODO Auto-generated method stub

	}

	public void visitLookupSwitchInsnWithProbes(Label paramLabel, int[] paramArrayOfInt, Label[] paramArrayOfLabel) {

		// TODO Auto-generated method stub

	}

}
