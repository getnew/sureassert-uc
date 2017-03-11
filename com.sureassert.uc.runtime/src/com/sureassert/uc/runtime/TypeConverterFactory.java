/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sureassert.uc.runtime.typeconverter.ArrayTC;
import com.sureassert.uc.runtime.typeconverter.BooleanTC;
import com.sureassert.uc.runtime.typeconverter.ByteTC;
import com.sureassert.uc.runtime.typeconverter.CharTC;
import com.sureassert.uc.runtime.typeconverter.CustomTC;
import com.sureassert.uc.runtime.typeconverter.DoubleTC;
import com.sureassert.uc.runtime.typeconverter.EmptyPrimitiveArrayTC;
import com.sureassert.uc.runtime.typeconverter.FileTC;
import com.sureassert.uc.runtime.typeconverter.FloatTC;
import com.sureassert.uc.runtime.typeconverter.IntegerTC;
import com.sureassert.uc.runtime.typeconverter.ListTC;
import com.sureassert.uc.runtime.typeconverter.LongTC;
import com.sureassert.uc.runtime.typeconverter.MapTC;
import com.sureassert.uc.runtime.typeconverter.NamedInstanceTC;
import com.sureassert.uc.runtime.typeconverter.PrimitiveArrayTC;
import com.sureassert.uc.runtime.typeconverter.SINExpressionTC;
import com.sureassert.uc.runtime.typeconverter.SetTC;
import com.sureassert.uc.runtime.typeconverter.ShortTC;
import com.sureassert.uc.runtime.typeconverter.StringFromFileTC;
import com.sureassert.uc.runtime.typeconverter.StringTC;
import com.sureassert.uc.runtime.typeconverter.StubExpressionListTC;

public class TypeConverterFactory {

	public static final TypeConverterFactory instance = new TypeConverterFactory();

	private final List<TypeConverter<?>> typeConverters;

	private final SINExpressionTC defaultTC;

	private final Map<String, TypeConverter<?>> tcByPrefix;

	@SuppressWarnings("rawtypes")
	private final Map<Class, TypeConverter> tcByClass;

	private final NamedInstanceTC namedInstanceTC;

	private final ArrayTC arrayTC;

	private final PrimitiveArrayTC primitiveArrayTC;

	private final EmptyPrimitiveArrayTC emptyPrimitiveArrayTC;

	private final CustomTC customTC;

	private final Object cacheLock = new Object();

	@SuppressWarnings("rawtypes")
	private TypeConverterFactory() {

		typeConverters = new ArrayList<TypeConverter<?>>();
		tcByPrefix = new HashMap<String, TypeConverter<?>>();
		tcByClass = new HashMap<Class, TypeConverter>();
		defaultTC = new SINExpressionTC();
		namedInstanceTC = new NamedInstanceTC();
		arrayTC = new ArrayTC();
		primitiveArrayTC = new PrimitiveArrayTC();
		emptyPrimitiveArrayTC = new EmptyPrimitiveArrayTC();
		customTC = new CustomTC(new ListTC());

		// Add built-in type converters here
		// NOTE: When determining TC by class, last TCs are checked first; therefore
		// put more specific TCs at the end of this list.
		// typeConverters.add(new SINExpressionTC());
		typeConverters.add(defaultTC);
		typeConverters.add(arrayTC);
		typeConverters.add(primitiveArrayTC);
		typeConverters.add(emptyPrimitiveArrayTC);
		typeConverters.add(namedInstanceTC);
		typeConverters.add(new StringTC());
		typeConverters.add(new IntegerTC());
		typeConverters.add(new CharTC());
		typeConverters.add(new LongTC());
		typeConverters.add(new ListTC());
		typeConverters.add(new StubExpressionListTC());
		typeConverters.add(new MapTC());
		typeConverters.add(new SetTC());
		typeConverters.add(new ListTC());
		typeConverters.add(new BooleanTC());
		typeConverters.add(new ByteTC());
		typeConverters.add(new DoubleTC());
		typeConverters.add(new FileTC());
		typeConverters.add(new FloatTC());
		typeConverters.add(new ShortTC());
		typeConverters.add(new StringFromFileTC());
		typeConverters.add(new StringTC());

		for (TypeConverter<?> tc : typeConverters) {
			tcByPrefix.put(tc.getPrefixID(), tc);
			tcByClass.put(BasicUtils.toNonPrimitiveType(tc.getType()), tc);
		}
	}

	public void registerTypeConverter(TypeConverter<?> tc) {

		typeConverters.add(tc);
		tcByPrefix.put(tc.getPrefixID(), tc);
		tcByClass.put(BasicUtils.toNonPrimitiveType(tc.getType()), tc);
	}

	public Object typeConvert(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		return getTypeConverterForValue(sinType).toInstance(sinType, classLoader);
	}

	public Object[] typeConvert(SINType[] sinTypes) throws TypeConverterException, NamedInstanceNotFoundException {

		Object[] results = new Object[sinTypes.length];
		for (int i = 0; i < sinTypes.length; i++) {
			results[i] = typeConvert(sinTypes[i], null);
		}
		return results;
	}

	public SINType toSINType(Object convertedInstance, String... ignoreInstanceNames) throws TypeConverterException {

		if (convertedInstance == null)
			return SINType.newFromRaw(NamedInstanceFactory.NULL_INSTANCE_NAME);
		else {
			String name = NamedInstanceFactory.getInstance().getInstanceName(convertedInstance);
			if (NamedInstanceTC.isValidInstanceName(name) && !BasicUtils.arrayContains(ignoreInstanceNames, name)) {
				return namedInstanceTC.toSINType(convertedInstance);
			} else if (convertedInstance.getClass().isArray()) {
				if (convertedInstance.getClass().getComponentType().isPrimitive())
					return primitiveArrayTC.toSINType(convertedInstance);
				else
					return arrayTC.toSINType(convertedInstance);
			} else {
				return getTypeConverter(convertedInstance.getClass()).toSINType(convertedInstance);
			}
		}
	}

	public SINExpressionTC getSINExpressionTC() {

		return defaultTC;
	}

	public TypeConverter<?> getTypeConverterForValue(SINType sinType) throws TypeConverterException {

		String typePrefix = sinType.getTypePrefix();
		return typePrefix == null ? defaultTC : getTypeConverterForPrefix(typePrefix);
	}

	public TypeConverter<?> getTypeConverterForPrefix(String sinTypePrefix) throws TypeConverterException {

		TypeConverter<?> tc = tcByPrefix.get(sinTypePrefix);
		if (sinTypePrefix != null && tc == null) {
			return customTC;
		}
		return tc == null ? defaultTC : tc;
	}

	@SuppressWarnings("unchecked")
	public <T> TypeConverter<T> getTypeConverter(Class<?> clazz) {

		synchronized (cacheLock) {
			clazz = BasicUtils.toNonPrimitiveType(clazz);
			TypeConverter<T> tc = tcByClass.get(clazz);
			if (tc == null) {
				TypeConverter<?> thisTC;
				for (int i = typeConverters.size() - 1; i >= 0; i--) {
					thisTC = typeConverters.get(i);
					if (thisTC.getType().isAssignableFrom(clazz)) {
						tc = (TypeConverter<T>) thisTC;
						tcByClass.put(clazz, tc);
						break;
					}
				}
			}
			return tc;
		}
	}

}
