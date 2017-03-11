/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class ShortTC extends AbstractTypeConverter<Short> {

	public Class<Short> getType() {

		return Short.class;
	}

	public String getPrefixID() {

		return "short";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Short.toString((Short) value) + "s";
	}

	public Short toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Short.parseShort(sinType.getSINValue());
		} catch (NumberFormatException e) {
			if (sinType.getSINValue() != null && sinType.getSINValue().length() == 0)
				return 0;
			else
				throw e;
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

}
