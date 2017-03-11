package com.sureassert.uc.transform;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import aj.org.objectweb.asm.Opcodes;

import com.sureassert.uc.interceptor.SignatureUtils;
import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.Signature;

public class SaUCClassAdviceAdaptor extends ClassAdapter {

	private String className;

	private boolean isClass;

	public SaUCClassAdviceAdaptor(ClassVisitor cv) {

		super(cv);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		className = BasicUtils.getClassNameFromBinaryClassName(name);
		super.visit(version, access, name, signature, superName, interfaces);

		isClass = ((access & 0x200) == 0);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

		MethodVisitor mv;
		mv = cv.visitMethod(access, name, desc, signature, exceptions);
		if (mv != null && isClass) {
			Signature sig = SignatureUtils.getSignature(className, name, desc);
			boolean isStatic = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
			mv = new SaUCMethodAdviceAdaptor(access, name, desc, mv, sig, isStatic);
		}
		return mv;
	}
}
