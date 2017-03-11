package com.sureassert.uc.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class InitializeProbeArrayClassVisitor extends ClassVisitor {

	public InitializeProbeArrayClassVisitor(ClassVisitor delegate) {

		super(Opcodes.ASM4, delegate);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodVisitor r = super.visitMethod(access, name, desc, signature, exceptions);
		if ("<clinit>".equals(name)) {
			r = new InitializeProbeArrayMethodVisitor(r);
		}
		return r;
	}

	static class InitializeProbeArrayMethodVisitor extends MethodVisitor {

		InitializeProbeArrayMethodVisitor(MethodVisitor delegate) {

			super(Opcodes.ASM4, delegate);
		}

		@Override
		public void visitCode() {

			super.visitCode();
			// build my static initializer by calling
			// visitFieldInsn(int opcode, String owner, String name, String desc)
			// or the like
		}
	}

	public static void push(final MethodVisitor mv, final int value) {

		if (value >= -1 && value <= 5) {
			mv.visitInsn(Opcodes.ICONST_0 + value);
		} else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.BIPUSH, value);
		} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.SIPUSH, value);
		} else {
			mv.visitLdcInsn(Integer.valueOf(value));
		}
	}
}