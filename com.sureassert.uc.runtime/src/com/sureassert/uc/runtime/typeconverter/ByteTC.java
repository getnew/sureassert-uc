/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class ByteTC extends AbstractTypeConverter<Byte> {

	public Class<Byte> getType() {

		return Byte.class;
	}

	public String getPrefixID() {

		return "byte";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return Byte.toString((Byte) value) + "b";
	}

	public Byte toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			return Byte.parseByte(sinType.getSINValue());
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
