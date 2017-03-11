/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class DoubleTC extends AbstractTypeConverter<Double> {

	public static final Character DECIMAL_POINT_REPLACEMENT = '\uE208';

	public Class<Double> getType() {

		return Double.class;
	}

	public String getPrefixID() {

		return "d";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Double.toString((Double) value) + "d";
	}

	public Double toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Double.parseDouble(sinType.getSINValue().replace(DECIMAL_POINT_REPLACEMENT, '.'));
		} catch (NumberFormatException e) {
			if (sinType.getSINValue() != null && sinType.getSINValue().length() == 0)
				return 0d;
			else
				throw e;
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

}
