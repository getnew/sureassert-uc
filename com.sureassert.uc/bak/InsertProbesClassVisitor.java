package com.sureassert.uc.interceptor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class InsertProbesClassVisitor extends ClassVisitor {

	public InsertProbesClassVisitor(int paramInt) {

		super(Opcodes.ASM4);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		if (mv != null) {
			mv = new InsertProbesMethodVisitor(mv);
		}
		return mv;
	}

	static class InsertProbesMethodVisitor extends MethodVisitor {

		public InsertProbesMethodVisitor(MethodVisitor mv) {

			super(Opcodes.ASM4, mv);
		}

		@Override
		public void visitLineNumber(int line, Label start) {

			mv.visitLineNumber(line, start);
		}
 
		@Override
		public void visitInsn(int opcode) {

			mv.visitInsn(opcode);
		}
	}
}
