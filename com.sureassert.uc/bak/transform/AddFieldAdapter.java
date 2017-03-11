package com.sureassert.uc.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class AddFieldAdapter extends ClassVisitor {

	private final int fAcc;
	private final String fName;
	private final String fDesc;
	private boolean isFieldPresent;
	private boolean isInterface;

	public AddFieldAdapter(ClassVisitor cv, int fAcc, String fName, String fDesc) {

		super(Opcodes.ASM4, cv);
		this.fAcc = fAcc;
		this.fName = fName;
		this.fDesc = fDesc;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		// System.out.println("Class: " + name);
		isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

		if (name.equals(fName)) {
			isFieldPresent = true;
		}
		return cv.visitField(access, name, desc, signature, value);
	}

	@Override
	public void visitEnd() {

		if (!isFieldPresent && !isInterface) {
			FieldVisitor fv = cv.visitField(fAcc, fName, fDesc, null, null);
			if (fv != null) {
				fv.visitEnd();
			}
		}
		cv.visitEnd();
	}
}