/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.IUCReexecutor;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.integration.EclipseDelegate;
import com.sureassert.uc.runtime.internal.Escaper;

/**
 * Converts an instance name to the named instance.
 * 
 * @author Nathan Dolan
 */
public class NamedInstanceTC extends AbstractTypeConverter<Object> {

	public static final String PREFIX = "ni";

	public static final String UC_REF_CHAR = "!";

	private final IUCReexecutor ucReexecutor;

	public NamedInstanceTC() {

		ucReexecutor = EclipseDelegate.getUCReexecutor();
	}

	public Class<Object> getType() {

		return Object.class;
	}

	public String getPrefixID() {

		return PREFIX;
	}

	public static boolean isValidInstanceName(String name) {

		return name != null && !name.equals(NamedInstanceFactory.CHAINED_INSTANCE_NAME) && //
				!name.equals(NamedInstanceFactory.RETVAL_INSTANCE_NAME) && //
				!name.equals(NamedInstanceFactory.THIS_INSTANCE_NAME) && //
				!name.equals(NamedInstanceFactory.NULL_INSTANCE_NAME) && //
				!name.startsWith(NamedInstanceFactory.SPECIAL_CHAR) && //
				!name.equals(NamedInstanceFactory.DEFAULT_INSTANCE_NAME) && //
				!name.equals(NamedInstanceFactory.BUILT_IN_METHODS_NAME) && //
				!name.startsWith(NamedInstanceFactory.ARG_PREFIX) && //
				!name.startsWith(NamedInstanceFactory.VERIFY_ARG_INSTANCE_NAME) && //
				!name.equals(NamedInstanceFactory.VERIFY_OBJ_INSTANCE_NAME);
	}

	public static boolean isNumericInstance(String instanceName) {

		return instanceName != null && instanceName.length() > 0 && //
				(Character.isDigit(instanceName.charAt(0)) || instanceName.charAt(0) == '-');
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		String name = NamedInstanceFactory.getInstance().getInstanceName(value);
		if (!isValidInstanceName(name))
			return "'" + Escaper.escapeControlChars(value.toString()) + "'";
		else
			return name;
	}

	public Object toInstance(SINType sinType, ClassLoader classLoader) throws NamedInstanceNotFoundException {

		String instanceName = sinType.getSINValue();
		if (instanceName.equals(NamedInstanceFactory.NULL_INSTANCE_NAME))
			return null;
		if (instanceName.length() > 0 && //
				(Character.isDigit(instanceName.charAt(0)) || instanceName.charAt(0) == '-')) {
			// Numeric instance literal
			char lastChar = instanceName.charAt(instanceName.length() - 1);
			String numStr = instanceName.substring(0, instanceName.length() - 1);
			numStr = numStr.replace(DoubleTC.DECIMAL_POINT_REPLACEMENT, '.');
			if (lastChar == 'd')
				return (double) new Double(numStr);
			else if (lastChar == 'l')
				return (long) new Long(numStr);
			else if (lastChar == 'c')
				return (char) (int) new Integer(numStr);
			else if (lastChar == 'b')
				return (byte) new Byte(numStr);
			else if (lastChar == 's')
				return (short) new Short(numStr);
			else if (lastChar == 'f')
				return (float) new Float(numStr);
			else if (Character.isDigit(lastChar)) {
				if (instanceName.indexOf(DoubleTC.DECIMAL_POINT_REPLACEMENT) > -1)
					return (double) new Double(instanceName.replace(DoubleTC.DECIMAL_POINT_REPLACEMENT, '.'));
				else
					return (int) new Integer(instanceName);
			} else
				throw new SARuntimeException("Numeric literal ends with an invalid type postfix");
		} else if (instanceName.equals("true")) {
			return true;
		} else if (instanceName.equals("false")) {
			return false;
		} else if (instanceName.contains(UC_REF_CHAR)) {
			// Re-execute named use-case
			String ucName = instanceName.substring(0, instanceName.indexOf(UC_REF_CHAR));
			if (!instanceName.endsWith(UC_REF_CHAR)) {
				// Check if this named use-case instance has already been created
				try {
					return NamedInstanceFactory.getInstance().getNamedInstance(instanceName, classLoader);
				} catch (NamedInstanceNotFoundException ninfe) {
				}
			}
			boolean executed = reexecuteUseCase(ucName, instanceName);
			if (!executed) {
				throw new NamedInstanceNotFoundException("No UseCase found with name \"" + ucName + "\"");
			}
		}

		return NamedInstanceFactory.getInstance().getNamedInstance(instanceName, classLoader);
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

	private boolean reexecuteUseCase(String ucName, String reexecName) {

		return ucReexecutor.reExecuteUseCase(ucName, reexecName);
	}
}
