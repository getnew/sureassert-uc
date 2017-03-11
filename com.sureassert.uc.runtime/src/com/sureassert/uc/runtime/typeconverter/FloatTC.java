/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class FloatTC extends AbstractTypeConverter<Float> {

	public Class<Float> getType() {

		return Float.class;
	}

	public String getPrefixID() {

		return "float";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Float.toString((Float) value) + "f";
	}

	public Float toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Float.parseFloat(sinType.getSINValue().replace(DoubleTC.DECIMAL_POINT_REPLACEMENT, '.'));
		} catch (NumberFormatException e) {
			if (sinType.getSINValue() != null && sinType.getSINValue().length() == 0)
				return 0f;
			else
				throw e;
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

}
