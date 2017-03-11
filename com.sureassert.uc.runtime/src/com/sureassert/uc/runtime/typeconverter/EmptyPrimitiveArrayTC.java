/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.lang.reflect.Array;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class EmptyPrimitiveArrayTC extends PrimitiveArrayTC {

	public static final String PREFIX = "ea";

	@Override
	public Object toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		// [ea:[v1]]
		Object instance = super.toInstance(sinType, classLoader);
		if (Array.getLength(instance) == 0)
			throw new TypeConverterException("Error at \"" + sinType.getRawSINType() + //
					"\" - no array type given, e.g. for empty double array use " + PREFIX + ":[d:]");
		Object first = Array.get(instance, 0);
		Class<?> type = BasicUtils.toPrimitiveType(first.getClass());
		return Array.newInstance(type, 0);
	}

	@Override
	public String getPrefixID() {

		return PREFIX;
	}

	@Override
	public Class<Object> getType() {

		return Object.class;
	}

}
