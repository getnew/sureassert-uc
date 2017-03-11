package com.sureassert.uc.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class WriteCoverageCallClassVisitor extends ClassVisitor {

	private String className;
	private boolean isInterface;

	public WriteCoverageCallClassVisitor(ClassVisitor cv) {

		super(Opcodes.ASM4, cv);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		// System.out.println("Class: " + name);
		isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		className = name;
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {

		MethodVisitor mv = cv == null ? null : cv.visitMethod(access, name, desc, signature, exceptions);
		if (isInterface)
			return mv;
		else
			return new WriteCoverageCallMethodVisitor(mv, className, name);
	}
}