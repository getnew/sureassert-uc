/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.builder;

public class InsertMultiUseCaseActionHandler extends InsertUseCaseActionHandler {

	@Override
	protected String getInsertString(String spacer, int numArgs, String[] paramTypes, boolean isExemplar) {

		return spacer + (isExemplar ? "@Exemplars(set={\n" : "@MultiUseCase(usecases={\n") + //
				super.getInsertString(spacer, numArgs, paramTypes, isExemplar) + ",\n" + //
				super.getInsertString(spacer, numArgs, paramTypes, isExemplar) + " })";
	}
}
