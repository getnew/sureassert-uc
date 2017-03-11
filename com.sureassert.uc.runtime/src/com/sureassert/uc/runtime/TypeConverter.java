/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;


public interface TypeConverter<T> {

	public SINType toSINType(Object value) throws TypeConverterException;

	public T toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException;

	public String getPrefixID();

	public Class<T> getType();

	public void registerDepends(SINType sinType, ErrorContainingModel ucModel, ClassLoader classLoader) throws ClassNotFoundException;
}
