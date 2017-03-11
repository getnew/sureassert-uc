package com.sureassert.uc.runtime.typeconverter;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.exception.SARuntimeException;

public class CustomTC extends AbstractTypeConverter<Object> {

	public ListTC listTC;

	public CustomTC(ListTC listTC) {

		this.listTC = listTC;
	}

	public Object toInstance(SINType sinType, ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		Signature methodSig = PersistentDataFactory.getInstance().getSinTypeMethodByPrefix(sinType.getTypePrefix());
		if (methodSig == null)
			throw new TypeConverterException("No SIN Type exists with prefix: " + sinType.getTypePrefix());
		List<?> list = listTC.toInstance(sinType, classLoader);
		int len = list.size();
		Object[] params = new Object[len];
		for (int i = 0; i < len; i++) {
			params[i] = list.get(i);
		}

		try {
			Class<?> clazz = classLoader.loadClass(methodSig.getClassName());
			Object instance = null;
			Method method = BasicUtils.findMethod(clazz, classLoader, methodSig);
			if (method == null)
				throw new TypeConverterException("Couldn't find method " + methodSig.toString());
			if (!Modifier.isStatic(method.getModifiers())) {
				try {
					instance = clazz.newInstance();
				} catch (Exception e) {
				}
			}
			if (method.getReturnType() == Void.TYPE) {
				throw new TypeConverterException("Methods annotated with SINType cannot be void.");
			}
			if (!Modifier.isStatic(method.getModifiers()) && instance == null) {
				throw new TypeConverterException("Methods annotated with SINType must be static or " + //
						"defined on a class with a no-args constructor.");
			}
			return method.invoke(instance, params);
		} catch (Exception e) {
			throw new SARuntimeException(e);
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return listTC.getChildSINs(sinType);
	}

	public String getPrefixID() {

		return "??";
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
