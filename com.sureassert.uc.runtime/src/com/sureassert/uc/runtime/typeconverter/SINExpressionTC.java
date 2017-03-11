/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ErrorContainingModel;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.ISINExpression.DefaultToType;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINExpressionFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.BaseSAException;
import com.sureassert.uc.runtime.internal.Escaper;

/**
 * The SINExpression Type Converter enables a raw SINExpression to be used
 * as a SINType.
 * 
 * @author Nathan Dolan
 * 
 */
public class SINExpressionTC extends AbstractTypeConverter<Object> {

	public Class<Object> getType() {

		return Object.class;
	}

	public String getPrefixID() {

		return "";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		String name = NamedInstanceFactory.getInstance().getInstanceName(value);
		if (name == null || name.equals(NamedInstanceFactory.CHAINED_INSTANCE_NAME) || //
				name.equals(NamedInstanceFactory.RETVAL_INSTANCE_NAME))
			return "'" + Escaper.escapeControlChars(value.toString()) + "\'";
		else
			return name;
	}

	public Object toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		try {
			ISINExpression expr = SINExpressionFactory.get(sinType.getEscaper().toRaw(sinType.getSINValue()));
			return expr.invoke(expr.getInstance(classLoader), classLoader, DefaultToType.NONE);
		} catch (Exception e) {
			if (e instanceof NamedInstanceNotFoundException)
				throw (NamedInstanceNotFoundException) e;
			if (e instanceof TypeConverterException)
				throw (TypeConverterException) e;
			else
				throw new TypeConverterException("Error executing SIN Expression \"" + //
						sinType.getEscaper().toRaw(sinType.getSINValue()) + "\": " + BasicUtils.toDisplayStr(e));
		}
	}

	@Override
	public void registerDepends(SINType sinType, ErrorContainingModel ucModel, ClassLoader classLoader) {

		if (sinType.getTypePrefix() == null) {
			ISINExpression sinExpr = SINExpressionFactory.get(sinType.getEscaper().toRaw(sinType.getSINValue()));
			try {
				registerDepends(sinExpr, ucModel, classLoader);
			} catch (BaseSAException e) {
				ucModel.setError(e.getMessage());
			}
		}
	}

	public void registerDepends(ISINExpression sinExpr, ErrorContainingModel ucModel, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		try {
			SINType type = sinExpr.getInstanceType();
			Signature ucSig = ucModel.getSignature();
			if (type != null && type.getTypePrefix() != null && type.getTypePrefix().equals(NamedInstanceTC.PREFIX)) {
				String instanceName = type.getSINValue();
				if (instanceName.contains(NamedInstanceTC.UC_REF_CHAR))
					instanceName = instanceName.substring(0, instanceName.indexOf(NamedInstanceTC.UC_REF_CHAR));
				Signature called = PersistentDataFactory.getInstance().getDeclaringSignature(instanceName);
				if (called == null && ucSig != null && !NamedInstanceTC.isNumericInstance(instanceName)) {
					// try prefixing current class name
					instanceName = BasicUtils.getSimpleClassName(ucSig.getClassName()) + "/" + instanceName;
					called = PersistentDataFactory.getInstance().getDeclaringSignature(instanceName);
				}
				// NOTE: called may be a numeric i.e. without a declaring signature
				if (called != null) {
					String methodName = sinExpr.getMethodName();
					if (methodName != null && called.getMemberName() == null) {
						// NOTE: must get all sigs with this member name (i.e. including any
						// overloaded
						// sigs)
						// as very hard to determine statically which would be called at runtime.
						// Therefore register dependency on all overloaded sigs.
						Set<Signature> memberSigs = SignatureTableFactory.instance.getAllSignatures(called.getClassName(), //
								methodName, classLoader);
						for (Signature memberSig : memberSigs) {
							boolean isUCDepends = PersistentDataFactory.getInstance().isUseCaseSignature(memberSig);
							PersistentDataFactory.getInstance().registerDependency(memberSig, ucSig, isUCDepends);
						}
					}
					if (PersistentDataFactory.getInstance().isUseCaseSignature(called))
						PersistentDataFactory.getInstance().registerDependency(called, ucSig, true);
				}
			}
			if (sinExpr.getParams() != null) {
				for (SINType param : sinExpr.getParams()) {
					registerDepends(param, ucModel, classLoader);
				}
			}
		} catch (ClassNotFoundException e) {
			ucModel.setError("Class not found: " + e.getMessage());
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

}
