/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class LongTC extends AbstractTypeConverter<Long> {

	public Class<Long> getType() {

		return Long.class;
	}

	public String getPrefixID() {

		return "lg";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Long.toString((Long) value) + "l";
	}

	public Long toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Long.parseLong(sinType.getSINValue());
		} catch (NumberFormatException e) {
			if (sinType.getSINValue() != null && sinType.getSINValue().length() == 0)
				return 0l;
			else
				throw e;
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

}
