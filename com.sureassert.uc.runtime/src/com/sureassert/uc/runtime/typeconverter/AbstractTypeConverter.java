/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.List;

import com.sureassert.uc.runtime.ErrorContainingModel;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverter;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.exception.BaseSAException;

public abstract class AbstractTypeConverter<T> implements TypeConverter<T> {

	public final SINType toSINType(Object value) throws TypeConverterException {

		if (value == null)
			return SINType.newFromRaw(NamedInstanceFactory.NULL_INSTANCE_NAME);
		else {
			String rawSIN = toRawSIN(value);
			if (rawSIN != null && rawSIN.length() > 0 && rawSIN.charAt(0) != '[' && //
					rawSIN.charAt(0) != '\'' && !Character.isDigit(rawSIN.charAt(0)))
				return SINType.newFromRaw("[" + rawSIN + "]");
			else
				return SINType.newFromRaw(rawSIN);
		}
	}

	/**
	 * Converts the given value to a raw SIN string.
	 * The value is not null.
	 * 
	 * @param value a non-null value compatible with this TC.
	 * @return a raw SIN type string.
	 * @throws TypeConverterException
	 */
	protected abstract String toRawSIN(Object value) throws TypeConverterException;

	public void registerDepends(SINType sinType, ErrorContainingModel ucModel, ClassLoader classLoader) throws ClassNotFoundException {

		try {
			for (SINType childSIN : getChildSINs(sinType)) {
				TypeConverterFactory.instance.getTypeConverterForValue(childSIN).registerDepends(//
						childSIN, ucModel, classLoader);
			}
		} catch (BaseSAException e) {
			ucModel.setError(e.getMessage());
		}
	}

	/**
	 * Gets the list of child SINs included in the given SINType, which is guaranteed
	 * to be of the correct type for this TypeConverter.
	 * 
	 * @param sinType
	 * @return A list of SINTypes or an empty list if there are none.
	 * @throws TypeConverterException
	 */
	protected abstract List<SINType> getChildSINs(SINType sinType) throws TypeConverterException;
}
