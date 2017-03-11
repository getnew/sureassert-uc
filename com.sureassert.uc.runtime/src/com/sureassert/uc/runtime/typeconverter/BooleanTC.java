/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class BooleanTC extends AbstractTypeConverter<Boolean> {

	public Class<Boolean> getType() {

		return Boolean.class;
	}

	public String getPrefixID() {

		return "b";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Boolean.toString((Boolean) value);
	}

	public Boolean toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Boolean.parseBoolean(sinType.getSINValue());
		} catch (NumberFormatException e) {
			if (sinType.getSINValue() != null && sinType.getSINValue().length() == 0)
				return false;
			else
				throw e;
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}
}
