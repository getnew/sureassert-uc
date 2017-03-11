/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.JavaModelException;

import com.sureassert.uc.internal.NamedUseCaseModelFactory;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SaUCStub;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.model.UseCaseModel;

public class MultiUseCaseModel {

	private final List<UseCaseModel> useCases;
	@SuppressWarnings("unused")
	private final Signature signature;
	private final String[] stubs;

	// private final boolean definedAsUseCase;

	/*
	 * private UseCase[] def(UseCase[] s1, UseCase[] s2) {
	 * 
	 * return s1 == null || s1.length == 0 ? s2 : s1;
	 * }
	 * 
	 * public MultiUseCaseModel(MultiUseCase multiUseCase, Signature signature, Signature
	 * inheritedFromSig) throws TypeConverterException, NamedInstanceNotFoundException {
	 * 
	 * useCases = new ArrayList<UseCaseModel>();
	 * this.signature = signature;
	 * UseCase[] _usecases = def(multiUseCase.usecases(), multiUseCase.uc());
	 * for (UseCase uc : _usecases) {
	 * useCases.add(new UseCaseModel(uc, signature, inheritedFromSig));
	 * }
	 * this.stubs = multiUseCase.stubs();
	 * }
	 */

	public MultiUseCaseModel(IMemberValuePair[] params, Signature signature, int declaredLineNum, //
			SourceFile sourceFile, Signature inheritedFromSig, boolean definedAsUseCase, NamedUseCaseModelFactory nucmFactory) //
			throws TypeConverterException, NamedInstanceNotFoundException, JavaModelException {

		this.signature = signature;
		// this.definedAsUseCase = definedAsUseCase;
		String[] _stubs = null;
		useCases = new ArrayList<UseCaseModel>();
		for (IMemberValuePair param : params) {

			if (param.getMemberName().equals("usecases") || param.getMemberName().equals("uc") || //
					param.getMemberName().equals("set") || param.getMemberName().equals("s")) {
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				for (int i = 0; i < value.length; i++) {
					IAnnotation ucAnnotation = (IAnnotation) value[i];
					UseCaseModel ucModel = ModelFactory.newUseCaseModel(ucAnnotation.getMemberValuePairs(), //
							signature, //
							sourceFile.getLineNum(ucAnnotation.getSourceRange().getOffset()), //
							inheritedFromSig, definedAsUseCase, nucmFactory);
					useCases.add(ucModel);
				}
			}
			if (param.getMemberName().equals("stubs")) {
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_stubs = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_stubs[i] = (String) value[i];
				}
			}
		}
		stubs = _stubs;

		// Add stubs to all child UseCases
		for (UseCaseModel ucModel : useCases) {
			ucModel.addStubs(stubs);
		}
	}

	public List<UseCaseModel> getUseCases() {

		return useCases;
	}

	public SaUCStub[] getStubs() throws TypeConverterException {

		if (stubs == null)
			return new SaUCStub[] {};
		SaUCStub[] methodStubs = new SaUCStub[stubs.length];
		for (int i = 0; i < stubs.length; i++) {

			methodStubs[i] = SaUCStub.newSaUCStub(stubs[i]);
		}
		return methodStubs;
	}

}
