/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class IntegerTC extends AbstractTypeConverter<Integer> {

	public Class<Integer> getType() {

		return Integer.class;
	}

	public String getPrefixID() {

		return "i";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Integer.toString((Integer) value);
	}

	public Integer toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Integer.parseInt(sinType.getSINValue());
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
