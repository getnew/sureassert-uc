/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;


public class ExecutorResult {

	public enum Type {
		ERROR, WARNING, INFO
	};

	private String description;
	private final Type type;
	private final Object returnedValue;
	private final int errorLineNum;
	private String className;

	public ExecutorResult(String description, Type type, Object returnedValue) {

		this(description, type, returnedValue, -1);
	}

	public ExecutorResult(String description, Type type, Object returnedValue, int errorLineNum) {

		this(description, type, returnedValue, errorLineNum, null);
	}

	public ExecutorResult(String description, Type type, Object returnedValue, int errorLineNum, String className) {

		this.description = description;
		this.type = type;
		this.returnedValue = returnedValue;
		this.errorLineNum = errorLineNum;
		this.className = className;
	}

	public String getDescription() {

		return description;
	}

	public void setDescription(String description) {

		this.description = description;
	}

	public void setClassName(String className) {

		this.className = className;
	}

	public Type getType() {

		return type;
	}

	public Object getReturnedValue() {

		return returnedValue;
	}

	public int getErrorLineNum() {

		return errorLineNum;
	}

	public String getClassName() {

		return className;
	}

	@Override
	public String toString() {

		return type.name() + ": " + description;
	}
}