/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.internal.Escaper;

public class ArrayTC extends AbstractTypeConverter<Object> {

	public Object toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		// [a:[v1],[v2],[v3]]
		Escaper escaper = sinType.getEscaper();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return new Object[] {};
		String[] entries = Escaper.hasPrefix(sinVal) ? new String[] { sinVal } : sinVal.split(",");
		Class<?> valueClass = null;
		List<Object> values = new ArrayList<Object>();
		// Get type, validate entries
		for (String entry : entries) {
			Object value = TypeConverterFactory.instance.typeConvert(//
					SINType.newFromEscaped(escaper.unescape(entry.trim()), escaper), classLoader);
			if (valueClass == null && value != null)
				valueClass = value.getClass();
			else if (value != null && !value.getClass().equals(valueClass))
				throw new TypeConverterException("All elements in an array must be the same type");
			values.add(value);
		}
		if (valueClass == null)
			valueClass = Object.class;

		Object array = Array.newInstance(valueClass, values.size());
		for (int i = 0; i < values.size(); i++) {
			Array.set(array, i, values.get(i));
		}

		return array;
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		// [a:[v1],[v2],[v3]] OR
		// [a:v1]
		List<SINType> childSINs = new ArrayList<SINType>();
		Escaper escaper = sinType.getEscaper();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return childSINs;
		String[] entries = Escaper.hasPrefix(sinVal) ? new String[] { sinVal } : sinVal.split(",");
		// Get type, validate entries
		for (String entry : entries) {
			childSINs.add(SINType.newFromEscaped(escaper.unescape(entry.trim()), escaper));
		}
		return childSINs;
	}

	public String getPrefixID() {

		return "a";
	}

	public Class<Object> getType() {

		return Object.class;
	}

	@Override
	public String toRawSIN(Object array) throws TypeConverterException {

		StringBuilder sin = new StringBuilder(getPrefixID()).append(":");
		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			sin.append(TypeConverterFactory.instance.toSINType(Array.get(array, i)).getRawSINType());
			if (i < length - 1)
				sin.append(",");
		}
		return sin.toString();
	}

}
