/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.internal.Escaper;

public class StringTC extends AbstractTypeConverter<String> {

	public static final String PREFIX = "str";
	public static final String PREFIX_WITH_SEPARATOR = PREFIX + ":";

	public Class<String> getType() {

		return String.class;
	}

	public String getPrefixID() {

		return PREFIX;
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return "'" + Escaper.escapeControlChars(value.toString()) + "'";
	}

	public String toInstance(SINType sinType, ClassLoader classLoader) {

		return sinType.getEscaper().toRaw(sinType.getSINValue());
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}
}
