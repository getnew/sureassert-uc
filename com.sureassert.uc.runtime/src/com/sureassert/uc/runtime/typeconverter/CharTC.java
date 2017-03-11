/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class CharTC extends AbstractTypeConverter<Character> {

	public Class<Character> getType() {

		return Character.class;
	}

	public String getPrefixID() {

		return "c";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return "c:" + Character.toString((Character) value);
	}

	public Character toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return (char) Integer.parseInt(sinType.getSINValue());
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
