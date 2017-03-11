/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IType;

import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.model.UseCaseModel;

public class UseCaseExecutionCommand {

	UseCaseModel ucModel;
	SourceModel sourceModel;
	SourceFile sourceFile;
	IType javaType;
	IProject project;
	Signature methodSig;

	public UseCaseExecutionCommand(UseCaseModel ucModel, SourceModel sourceModel, SourceFile sourceFile, IType javaType, IProject project, Signature methodSig) {

		this.ucModel = ucModel;
		this.sourceModel = sourceModel;
		this.sourceFile = sourceFile;
		this.javaType = javaType;
		this.project = project;
		this.methodSig = methodSig;
	}
}