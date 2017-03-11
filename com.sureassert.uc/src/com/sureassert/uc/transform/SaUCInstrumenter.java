package com.sureassert.uc.transform;

import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.internal.data.CRC64;
import org.jacoco.core.internal.flow.ClassProbesAdapter;
import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class SaUCInstrumenter extends Instrumenter {

	private final IExecutionDataAccessorGenerator accessGenerator;

	public SaUCInstrumenter(IExecutionDataAccessorGenerator runtime) {

		super(runtime);
		accessGenerator = runtime;
	}

	public byte[] instrument(String name, byte[] clazzBytes, boolean instrumentCoverage) {

		ClassReader reader = new ClassReader(clazzBytes);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

		ClassVisitor saucAdviceVisitor = new SaUCClassAdviceAdaptor(writer);
		if (instrumentCoverage) {
			ClassVisitor coverageProbesVisitor = new ClassProbesAdapter(//
					new SaUCClassInstrumenter(name, CRC64.checksum(reader.b), accessGenerator, saucAdviceVisitor));
			reader.accept(coverageProbesVisitor, ClassReader.EXPAND_FRAMES);
		} else {
			reader.accept(saucAdviceVisitor, ClassReader.EXPAND_FRAMES);
		}
		return writer.toByteArray();
	}
}
