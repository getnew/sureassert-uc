/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.internal.Escaper;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class MapTC extends AbstractTypeConverter<Map> {

	public Map toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		// [m:[k1]=[v1]]
		Escaper escaper = sinType.getEscaper();
		Map map = new LinkedHashMap();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return map;
		String[] entries = Escaper.hasPrefix(sinVal) ? new String[] { sinVal } : sinVal.split(",");
		for (String entry : entries) {
			entry = entry.trim();
			String[] nameValue = entry.split("=");
			if (nameValue.length != 2)
				throw new TypeConverterException("Map must contain comma-separated list of name=value pairs");
			Object key = TypeConverterFactory.instance.typeConvert(//
					SINType.newFromEscaped(escaper.unescape(nameValue[0].trim()), escaper), classLoader);
			Object value = TypeConverterFactory.instance.typeConvert(//
					SINType.newFromEscaped(escaper.unescape(nameValue[1].trim()), escaper), classLoader);
			map.put(key, value);
		}
		return map;
	}

	public Class<Map> getType() {

		return Map.class;
	}

	public String getPrefixID() {

		return "m";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		Map map = (Map) value;
		StringBuilder sin = new StringBuilder(getPrefixID()).append(":");
		for (Iterator<Entry> i = map.entrySet().iterator(); i.hasNext();) {
			Entry entry = i.next();
			sin.append(TypeConverterFactory.instance.toSINType(entry.getKey()).getRawSINType());
			sin.append("=");
			sin.append(TypeConverterFactory.instance.toSINType(entry.getValue()).getRawSINType());
			if (i.hasNext())
				sin.append(",");
		}
		return sin.toString();
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		// [m:[k1]=[v1]]
		Escaper escaper = sinType.getEscaper();
		List<SINType> childSINs = new ArrayList<SINType>();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return childSINs;
		String[] entries = Escaper.hasPrefix(sinVal) ? new String[] { sinVal } : sinVal.split(",");
		for (String entry : entries) {
			entry = entry.trim();
			String[] nameValue = entry.split("=");
			if (nameValue.length != 2)
				throw new TypeConverterException("Map must contain comma-separated list of name=value pairs");
			childSINs.add(SINType.newFromEscaped(escaper.unescape(nameValue[0].trim()), escaper));
			childSINs.add(SINType.newFromEscaped(escaper.unescape(nameValue[1].trim()), escaper));
		}
		return childSINs;
	}
}
