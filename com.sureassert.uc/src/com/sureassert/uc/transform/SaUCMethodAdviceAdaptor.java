package com.sureassert.uc.transform;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import _sauc.SAInterceptor.SaUCAdviceResponse;

import com.sureassert.uc.runtime.IntHolder;
import com.sureassert.uc.runtime.Signature;

public class SaUCMethodAdviceAdaptor extends AdviceAdapter {

	private final String desc;

	private final Signature methodSig;

	private final boolean isStaticMethod;

	private static final Type adviceResponseType = Type.getObjectType(SaUCAdviceResponse.class.getName().replace('.', '/'));

	public SaUCMethodAdviceAdaptor(int access, String name, String desc, MethodVisitor mv, Signature methodSig, //
			boolean isStaticMethod) {

		super(mv, access, name, desc);
		this.methodSig = methodSig;
		this.desc = desc;
		this.isStaticMethod = isStaticMethod;
	}

	@Override
	protected void onMethodEnter() {

		if (methodSig.getMemberName() != null && methodSig.getMemberName().equals("$jacocoInit"))
			return;

		mv.visitFieldInsn(GETSTATIC, "com/sureassert/uc/runtime/SignatureTableFactory", "instance", "Lcom/sureassert/uc/runtime/SignatureTableFactory;");
		mv.visitLdcInsn(methodSig.getClassName());
		mv.visitLdcInsn(methodSig.getMemberName());
		mv.visitLdcInsn(methodSig.getParamClassNamesAppended());

		mv.visitMethodInsn(INVOKEVIRTUAL, "com/sureassert/uc/runtime/SignatureTableFactory", "getSignature",
				"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/sureassert/uc/runtime/Signature;");
		int varSig = newLocal(adviceResponseType);
		mv.visitVarInsn(ASTORE, varSig);

		mv.visitFieldInsn(GETSTATIC, "_sauc/SAInterceptor", "instance", "L_sauc/SAInterceptor;");
		mv.visitVarInsn(ALOAD, varSig);
		int currentRegisterIdx = 0;
		if (isStaticMethod)
			mv.visitInsn(ACONST_NULL); // null (static method)
		else
			mv.visitVarInsn(ALOAD, currentRegisterIdx++); // this instance

		writeArgInsns(currentRegisterIdx); // add arguments

		mv.visitMethodInsn(INVOKEVIRTUAL, "_sauc/SAInterceptor", "performAdvice",
				"(Lcom/sureassert/uc/runtime/Signature;Ljava/lang/Object;[Ljava/lang/Object;)L_sauc/SAInterceptor$SaUCAdviceResponse;");
		int varAdviceResponse = newLocal(adviceResponseType);
		mv.visitVarInsn(ASTORE, varAdviceResponse);

		mv.visitVarInsn(ALOAD, varAdviceResponse);
		mv.visitFieldInsn(GETFIELD, "_sauc/SAInterceptor$SaUCAdviceResponse", "doProceed", "Z");
		Label methodStartLabel = new Label();
		mv.visitJumpInsn(IFNE, methodStartLabel);

		Type returnType = Type.getReturnType(desc);
		if (returnType != null && returnType != Type.VOID_TYPE) {
			writeStubReturnInsns(returnType, varAdviceResponse);
		}

		mv.visitLabel(methodStartLabel);

		// mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		// mv.visitLdcInsn("Intercepted method: " + methodSig.toSimpleString());
		// mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
		// "(Ljava/lang/String;)V");
	}

	/**
	 * Writes a BIPUSH-like instruction with the given operand. If the given
	 * operand is under 6, uses ICONST_0/1/2/3/4/5 instead.
	 * 
	 * @param operand
	 * @return
	 */
	private void writeBIPUSHInsn(int operand) {

		if (operand == 0)
			mv.visitInsn(ICONST_0);
		else if (operand == 1)
			mv.visitInsn(ICONST_1);
		else if (operand == 2)
			mv.visitInsn(ICONST_2);
		else if (operand == 3)
			mv.visitInsn(ICONST_3);
		else if (operand == 4)
			mv.visitInsn(ICONST_4);
		else if (operand == 5)
			mv.visitInsn(ICONST_5);
		else
			mv.visitIntInsn(BIPUSH, operand);
	}

	/**
	 * Write the instructions to add the method arguments to the stack for use in
	 * SAInterceptor.performAdvice
	 */
	private void writeArgInsns(int currentRegisterIdx) {

		Type[] argTypes = Type.getArgumentTypes(desc);
		if (argTypes == null)
			argTypes = new Type[] {};
		int numArgs = argTypes.length;

		// get new Object array for arg values
		writeBIPUSHInsn(numArgs);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

		if (numArgs == 0)
			return;

		mv.visitInsn(DUP);

		// populate array with arg values
		IntHolder registerIdx = new IntHolder(currentRegisterIdx);
		for (int argIdx = 0; argIdx < numArgs; argIdx++) {
			writeBIPUSHInsn(argIdx);
			writeArrayArgInsertInsns(argTypes[argIdx], registerIdx);
			mv.visitInsn(AASTORE);
			if (argIdx < numArgs - 1)
				mv.visitInsn(DUP);
		}
	}

	private void writeArrayArgInsertInsns(Type type, IntHolder stackIdx) {

		if (type == Type.BOOLEAN_TYPE) {
			mv.visitVarInsn(ILOAD, stackIdx.getValue());
			stackIdx.add(1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
		} else if (type == Type.CHAR_TYPE) {
			mv.visitVarInsn(ILOAD, stackIdx.getValue());
			stackIdx.add(1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
		} else if (type == Type.BYTE_TYPE) {
			mv.visitVarInsn(ILOAD, stackIdx.getValue());
			stackIdx.add(1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
		} else if (type == Type.SHORT_TYPE) {
			mv.visitVarInsn(ILOAD, stackIdx.getValue());
			stackIdx.add(1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
		} else if (type == Type.INT_TYPE) {
			mv.visitVarInsn(ILOAD, stackIdx.getValue());
			stackIdx.add(1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
		} else if (type == Type.FLOAT_TYPE) {
			mv.visitVarInsn(FLOAD, stackIdx.getValue());
			stackIdx.add(1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
		} else if (type == Type.LONG_TYPE) {
			mv.visitVarInsn(LLOAD, stackIdx.getValue());
			stackIdx.add(2);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
		} else if (type == Type.DOUBLE_TYPE) {
			mv.visitVarInsn(DLOAD, stackIdx.getValue());
			stackIdx.add(2);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
		} else {
			// Object/array type
			mv.visitVarInsn(ALOAD, stackIdx.getValue());
			stackIdx.add(1);
		}
	}

	/*
	 * mv.visitFieldInsn(GETSTATIC, "_sauc/SAInterceptor", "instance", "L_sauc/SAInterceptor;");
	 * 
	 * mv.visitVarInsn(ALOAD, 12); // new local
	 * 
	 * mv.visitVarInsn(ALOAD, 0); // this
	 * 
	 * mv.visitIntInsn(BIPUSH, 8); // 8 = num elements
	 * mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
	 * mv.visitInsn(DUP);
	 * 
	 * 
	 * mv.visitInsn(ICONST_0); // arg1 : double
	 * mv.visitVarInsn(DLOAD, 1); // DLOAD for double (INCx2 for Double and Long)
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
	 * mv.visitInsn(AASTORE); // always
	 * mv.visitInsn(DUP); // all but last param
	 * mv.visitInsn(ICONST_1); // arg2 : String
	 * mv.visitVarInsn(ALOAD, 3); // ALOAD for Object/array ref
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitInsn(ICONST_2); // arg3 : IntHolder[]
	 * mv.visitVarInsn(ALOAD, 4); // ALOAD for Object/array ref
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitInsn(ICONST_3); // arg4 : boolean
	 * mv.visitVarInsn(ILOAD, 5);
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitInsn(ICONST_4); // arg5 : double
	 * mv.visitVarInsn(DLOAD, 6);
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitInsn(ICONST_5);// arg6 : float
	 * mv.visitVarInsn(FLOAD, 8);
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitIntInsn(BIPUSH, 6); // arg7 : long -- NOTE BIPUSH replaces ICONST_n
	 * mv.visitVarInsn(LLOAD, 9);
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitIntInsn(BIPUSH, 7); // arg8 : double -- NOTE BIPUSH replaces ICONST_n
	 * mv.visitVarInsn(DLOAD, 11);
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitIntInsn(BIPUSH, 8); // arg9 : IntHolder[] -- NOTE BIPUSH replaces ICONST_n
	 * mv.visitVarInsn(ALOAD, 13);
	 * mv.visitInsn(AASTORE);
	 * mv.visitInsn(DUP);
	 * mv.visitIntInsn(BIPUSH, 9); // arg10 : short -- NOTE BIPUSH replaces ICONST_n
	 * mv.visitVarInsn(ILOAD, 14);
	 * mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
	 * mv.visitInsn(AASTORE);
	 * mv.visitMethodInsn(INVOKEVIRTUAL, "_sauc/SAInterceptor", "performAdvice",
	 * "(Lcom/sureassert/uc/runtime/Signature;Ljava/lang/Object;[Ljava/lang/Object;)L_sauc/SAInterceptor$SaUCAdviceResponse;"
	 * );
	 * mv.visitVarInsn(ASTORE, 16);
	 */

	/**
	 * Write the instructions to return the stub value (cast to the method return type)
	 */
	private void writeStubReturnInsns(Type returnType, int varAdviceResponse) {

		mv.visitVarInsn(ALOAD, varAdviceResponse);
		mv.visitFieldInsn(GETFIELD, "_sauc/SAInterceptor$SaUCAdviceResponse", "stubRetval", "Ljava/lang/Object;");
		if (returnType == Type.BOOLEAN_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
			mv.visitInsn(IRETURN);
		} else if (returnType == Type.BYTE_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
			mv.visitInsn(IRETURN);
		} else if (returnType == Type.CHAR_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
			mv.visitInsn(IRETURN);
		} else if (returnType == Type.DOUBLE_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
			mv.visitInsn(DRETURN);
		} else if (returnType == Type.FLOAT_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
			mv.visitInsn(FRETURN);
		} else if (returnType == Type.INT_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
			mv.visitInsn(IRETURN);
		} else if (returnType == Type.LONG_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
			mv.visitInsn(LRETURN);
		} else if (returnType == Type.SHORT_TYPE) {
			mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
			mv.visitInsn(IRETURN);
		} else {
			// Object type
			mv.visitTypeInsn(CHECKCAST, returnType.getInternalName());
			mv.visitInsn(ARETURN);
		}
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {

		super.visitMaxs(maxStack + 4, maxLocals);
	}
}
