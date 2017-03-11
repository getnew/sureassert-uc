/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator.function;

public class EqualsFunction implements Function<Object, Boolean> {

	public String getPrefixID() {

		return "eq";
	}

	public Boolean execute(Object instance, Object... params) {

		return instance.equals(params[0]);
	}

	public boolean supportsParams(Object... params) {

		return params != null && params.length == 1;
	}

}
