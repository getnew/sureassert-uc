package com.sureassert.uc.runtime;

public class ObjHolder<T> {

	T val;

	public ObjHolder() {

	}

	public ObjHolder(T val) {

		this.val = val;
	}

	public void set(T val) {

		this.val = val;
	}

	public T get() {

		return val;
	}
}
