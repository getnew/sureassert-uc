/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.internal.Escaper;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SetTC extends AbstractTypeConverter<Set> {

	public Set toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		// [s:[v1],[v2],[v3]]
		Escaper escaper = sinType.getEscaper();
		Set<Object> list = new LinkedHashSet();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return list;
		String[] entries = Escaper.hasPrefix(sinVal) ? new String[] { sinVal } : sinVal.split(",");
		for (String entry : entries) {
			Object value = TypeConverterFactory.instance.typeConvert(//
					SINType.newFromEscaped(escaper.unescape(entry.trim()), escaper), classLoader);
			list.add(value);
		}
		return list;
	}

	public String getPrefixID() {

		return "s";
	}

	public Class<Set> getType() {

		return Set.class;
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		Set set = (Set) value;
		StringBuilder sin = new StringBuilder(getPrefixID()).append(":");
		for (Iterator i = set.iterator(); i.hasNext();) {
			sin.append(TypeConverterFactory.instance.toSINType(i.next()).getRawSINType());
			if (i.hasNext())
				sin.append(",");
		}
		return sin.toString();
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		// [s:[v1],[v2],[v3]]
		Escaper escaper = sinType.getEscaper();
		List<SINType> list = new ArrayList<SINType>();
		String sinVal = sinType.getSINValue();
		if (sinVal.trim().equals(""))
			return list;
		String[] entries = Escaper.hasPrefix(sinVal) ? new String[] { sinVal } : sinVal.split(",");
		for (String entry : entries) {
			list.add(SINType.newFromEscaped(entry.trim(), escaper));
		}
		return list;
	}
}
