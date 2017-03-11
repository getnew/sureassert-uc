/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.internal.Escaper;

/**
 * Executes the UseCase with the given name and returns the instance returned by the UseCase.
 * 
 * @author Nathan Dolan
 */
public class NamedUseCaseTC extends AbstractTypeConverter<Object> {

	public static final String PREFIX = "uc";

	public Class<Object> getType() {

		return Object.class;
	}

	public String getPrefixID() {

		return PREFIX;
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		String name = NamedInstanceFactory.getInstance().getInstanceName(value);
		if (!NamedInstanceTC.isValidInstanceName(name))
			return "'" + Escaper.escapeControlChars(value.toString()) + "'";
		else
			return PREFIX + ":" + name;
	}

	public Object toInstance(SINType sinType, ClassLoader classLoader) throws NamedInstanceNotFoundException {

		String instanceName = sinType.getSINValue();
		if (instanceName.length() > 0 && //
				(Character.isDigit(instanceName.charAt(0)) || instanceName.charAt(0) == '-')) {
			// Numeric instance literal
			char lastChar = instanceName.charAt(instanceName.length() - 1);
			String numStr = instanceName.substring(0, instanceName.length() - 1);
			if (lastChar == 'd')
				return (double) new Double(numStr);
			else if (lastChar == 'l')
				return (long) new Long(numStr);
			else if (lastChar == 'c')
				return (char) (int) new Integer(lastChar);
			else if (lastChar == 'b')
				return (byte) new Byte(numStr);
			else if (lastChar == 's')
				return (short) new Short(numStr);
			else if (lastChar == 'f')
				return (float) new Float(numStr);
			else if (Character.isDigit(lastChar))
				return (int) new Integer(instanceName);
			else
				throw new SARuntimeException("Numeric literal ends with an invalid type postfix");
		} else if (instanceName.equals("true")) {
			return true;
		} else if (instanceName.equals("false")) {
			return false;
		}

		return NamedInstanceFactory.getInstance().getNamedInstance(instanceName, classLoader);
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}
}
