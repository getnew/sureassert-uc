/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.ArrayList;
import java.util.List;

import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.internal.Escaper;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ListTC extends AbstractTypeConverter<List> {

	public List toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		// [l:[v1],[v2],[v3]]
		Escaper escaper = sinType.getEscaper();
		List<Object> list = newList();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return list;
		String[] entries = sinVal.split(",");
		for (String entry : entries) {
			Object value = TypeConverterFactory.instance.typeConvert(//
					SINType.newFromEscaped(escaper.unescape(entry.trim()), escaper), classLoader);
			list.add(value);
		}
		return list;
	}

	protected List newList() {

		return new ArrayList();
	}

	public String getPrefixID() {

		return "l";
	}

	public Class<List> getType() {

		return List.class;
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		List list = (List) value;
		StringBuilder sin = new StringBuilder(getPrefixID()).append(":");
		for (int i = 0; i < list.size(); i++) {
			sin.append(TypeConverterFactory.instance.toSINType(list.get(i)).getRawSINType());
			if (i < list.size() - 1)
				sin.append(",");
		}
		return sin.toString();
	}

	public List<String> getRawSINElements(SINType sinType) {

		// [l:[v1],[v2],[v3]]
		Escaper escaper = sinType.getEscaper();
		List<String> list = new ArrayList<String>();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return list;
		String[] entries = sinVal.split(",");
		for (String entry : entries) {
			list.add(escaper.toRaw(entry.trim()));
		}
		return list;
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		// [l:[v1],[v2],[v3]]
		Escaper escaper = sinType.getEscaper();
		List<SINType> list = new ArrayList<SINType>();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return list;
		String[] entries = sinVal.split(",");
		for (String entry : entries) {
			list.add(SINType.newFromEscaped(entry.trim(), escaper));
		}
		return list;
	}
}
