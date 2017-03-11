/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.BuiltInMethods;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.NamedInstanceException;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.SINExpressionFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.exception.EvaluatorException;
import com.sureassert.uc.runtime.typeconverter.NamedInstanceTC;

/**
 * Simple Invocation Notation Expression. A SINExpression encapsulates
 * the data and behaviour required to invoke a method on an instance.
 * 
 * @author Nathan Dolan
 * 
 */
public class SINExpression implements ISINExpression {

	/** The SIN Expression string */
	private final String rawSINExpression;

	private boolean parsed = false;

	/**
	 * The type on which the expression is invoked.
	 * <p>
	 * Usually, this will just be a named instance or class. <br>
	 * Can be NamedInstanceFactory.CHAINED_INSTANCE_NAME if this a chained expression, or
	 * NamedInstanceFactory.THIS_INSTANCE_NAME if the method is to be invoked on the instance being
	 * executed.
	 */
	private SINType instance;

	/**
	 * The method/constructor to execute on the instance/class, or null if this expression is a SIN
	 * Type
	 */
	private String methodName;

	/** The parameters to pass to the method, or null if this expression is a SIN Type */
	private List<SINType> params;

	/**
	 * Optionally a map of field names to values to set. The method/constructor must return an
	 * object if field setters are specified. <br>
	 * e.g. new C1() {fieldA=[s:'value1'], fieldB=[i:2]}
	 */
	private Map<String, SINType> fieldSetters;

	/**
	 * Optionally an expression to execute with the result of this expression, e.g.
	 * i1.m1().chained()
	 */
	private ISINExpression chainedExpression;

	/** An optional display message to associate with this expression */
	private String associatedMessage;

	/** Populate with any errors encountered pre-parsing */
	private TypeConverterException error;

	/** Optionally a display string to represent this expression, overriding the default toString */
	private String displayExpr;

	/** The method/constructor/field signature that this expression pertains to */
	private Signature signature;

	public static final SINExpression NULL_EXPRESSION = new SINExpression("");

	/**
	 * DO NOT USE: Get instance from SINExpressionFactory instead,
	 * 
	 * @param rawSINExpression
	 * @return
	 */
	public static ISINExpression createNew(String rawSINExpression) {

		return new SINExpression(rawSINExpression);
	}

	private SINExpression(String rawSINExpression) {

		if (rawSINExpression.matches("'[\\w ]+':.+")) { // UGLY...
			// Get associated display message
			int messageEndIndex = rawSINExpression.indexOf("'", 1) + 1;
			if (messageEndIndex == -1) {
				error = new TypeConverterException("SIN expressions starting with a display message must be followed by an expression, separated with a colon (msg: SIN)");
				this.rawSINExpression = rawSINExpression;
				return;
			}
			this.associatedMessage = rawSINExpression.substring(1, messageEndIndex - 1);
			rawSINExpression = preprocessRawSINExpression(rawSINExpression.substring(messageEndIndex + 1));
		}
		this.rawSINExpression = preprocessRawSINExpression(rawSINExpression);
	}

	private String preprocessRawSINExpression(String rawSINExpression) {

		// Trim
		rawSINExpression = rawSINExpression.trim();
		// Remove superfluous brackets
		while (rawSINExpression.startsWith("(") && rawSINExpression.endsWith(")")) {
			rawSINExpression = rawSINExpression.substring(1, rawSINExpression.length() - 1).trim();
		}
		return rawSINExpression;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#parse()
	 */
	public void parse() throws TypeConverterException, NamedInstanceNotFoundException {

		if (parsed)
			return;
		try {
			this.parsed = true;

			if (error != null)
				throw error;

			Escaper escaper = new Escaper(rawSINExpression);

			// Parse raw SIN expression
			String escapedSIN = escaper.getEscapedSIN();
			int numOpens = StringUtils.countMatches(escapedSIN, "(");
			int numCloses = StringUtils.countMatches(escapedSIN, ")");
			if (numOpens > numCloses) {
				throw new TypeConverterException("Expression \"" + escapedSIN + "\" contains \"(\" without matching \")\"");
			}
			if (numOpens < numCloses) {
				throw new TypeConverterException("Expression \"" + escapedSIN + "\" contains \")\" without matching \"(\"");
			}
			int openBracketIndex = escapedSIN.indexOf("(");
			int methodDotIndex = escapedSIN.indexOf(".");
			int fieldSetterStartIdx = escapedSIN.indexOf("{");
			int chainedExprDotIndex = escapedSIN.indexOf(".", methodDotIndex + 1);
			int closeBracketIndex = -1;
			int endIndex;
			if (chainedExprDotIndex == -1) {
				// no chained expression?
				endIndex = escapedSIN.length();
				if (openBracketIndex > -1 && methodDotIndex > openBracketIndex) {
					// . is after ( -- chained expression after no-method expression
					// new MyClass().chainedExpr
					// =(1).toString()
					chainedExprDotIndex = methodDotIndex;
					methodDotIndex = -1;
				}
			} else if (openBracketIndex != -1 && openBracketIndex < chainedExprDotIndex) {
				// second dot is chained expression
				closeBracketIndex = escapedSIN.indexOf(")", openBracketIndex);
				chainedExprDotIndex = escapedSIN.indexOf(".", closeBracketIndex + 1);
				endIndex = closeBracketIndex + 1;
			} else {
				// chained expression with field
				openBracketIndex = -1;
				endIndex = chainedExprDotIndex;
			}
			if (fieldSetterStartIdx > -1 && endIndex > fieldSetterStartIdx) {
				endIndex = fieldSetterStartIdx;
			}

			String instanceAndOp = escapedSIN.substring(0, endIndex);
			closeBracketIndex = openBracketIndex == -1 ? -1 : instanceAndOp.indexOf(")", openBracketIndex + 1);

			// Get instance name/SINType
			// NOTE: instance MUST have a prefix else an infinite recursion of SINExpressions will
			// occur.
			if (methodDotIndex == -1) {

				if (escapedSIN.startsWith("new ")) {
					// a method is invoked on a class
					// new Class()
					// NDsinType = newNISinType(escapedSIN.substring(4, openBracketIndex).trim());
					instance = SINType.newFromEscaped(addPrefix(escapedSIN.substring(4, openBracketIndex).trim()), escaper);
				} else if (openBracketIndex > -1) {
					// no instance name -- either implicit "this.X" or built-in method (e.g. =(x,y))
					methodName = instanceAndOp.substring(0, openBracketIndex);
					if (BuiltInMethods.isBuiltInMethodSymbol(methodName)) {
						// built-in method (e.g. =(x,y))
						instance = SINType.newFromEscaped(//
								"ni:" + BuiltInMethods.class.getName().replace('.', '/'), escaper);
					} else {
						// implicit "this.X"
						instance = SINType.newFromEscaped("ni:this", escaper);
					}
				} else {
					instance = SINType.newFromEscaped(addPrefix(escaper.unescape(instanceAndOp.trim())), escaper);
				}
			} else if (methodDotIndex > 0) {
				// a method is invoked on an instance
				String sinTypeStr = escapedSIN.substring(0, methodDotIndex).trim();
				instance = SINType.newFromEscaped(addPrefix(escaper.unescape(sinTypeStr)), escaper);
			} else if (methodDotIndex == 0) {
				// this is a chained expression
				instance = SINType.newFromEscaped(NamedInstanceTC.PREFIX + ":" + NamedInstanceFactory.CHAINED_INSTANCE_NAME, escaper);
			}

			// Get method name
			if (methodDotIndex > -1 && methodName == null) {
				if (openBracketIndex == -1)
					methodName = instanceAndOp.substring(methodDotIndex + 1).trim();
				else
					methodName = instanceAndOp.substring(methodDotIndex + 1, openBracketIndex).trim();
			}

			// Get parameters
			if (openBracketIndex > -1 && closeBracketIndex > -1) {
				String sinParamsStr = escapedSIN.substring(openBracketIndex + 1, closeBracketIndex);
				params = new ArrayList<SINType>();
				if (sinParamsStr.trim().length() > 0) {
					String[] sinParams = sinParamsStr.split(",");
					for (String sinParam : sinParams) {
						params.add(SINType.newFromEscaped(escaper.unescape(sinParam.trim()), escaper));
					}
				}
			}

			// Get field setters
			int fieldSetterEndIdx = escapedSIN.indexOf("}");
			if (fieldSetterStartIdx == -1 && fieldSetterEndIdx > -1)
				throw new TypeConverterException("} specified without matching {");
			else if (fieldSetterStartIdx > -1 && fieldSetterEndIdx == -1)
				throw new TypeConverterException("{ specified without matching }");
			else if (fieldSetterStartIdx > -1 && fieldSetterEndIdx > -1) {

				String fieldSetterDef = escapedSIN.substring(fieldSetterStartIdx + 1, fieldSetterEndIdx).trim();
				fieldSetters = new LinkedHashMap<String, SINType>();
				// NOTE: field setter values MUST have a prefix else an infinite recursion of
				// SINExpressions will occur.
				String[] entries = fieldSetterDef.split(",");
				for (String entry : entries) {
					entry = entry.trim();
					String[] nameValue = entry.split("=");
					if (nameValue.length != 2)
						throw new TypeConverterException("Field dictionary must contain comma-separated list of name=value pairs");
					String key = nameValue[0].trim();
					SINType value = SINType.newFromEscaped(escaper.unescape(nameValue[1].trim()), escaper);
					fieldSetters.put(key, value);
				}
			}

			// Get chained expression
			if (chainedExprDotIndex > -1) {
				chainedExpression = SINExpressionFactory.get(escaper.toRaw(escapedSIN.substring(chainedExprDotIndex)));
			} else {
				chainedExpression = null;
			}

			SINExpressionFactory.addCachedSINExp(rawSINExpression, new WeakReference<SINExpression>(this));

		} catch (TypeConverterException e) {
			throw e;
		} catch (Exception e) {
			throw new TypeConverterException("Syntax of expression \"" + rawSINExpression + "\" is invalid");
		}

		// Post-conditions
		assert instance.getTypePrefix() != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#getInstance(java.lang.ClassLoader)
	 */
	public Object getInstance(ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		if (!parsed)
			parse();
		return TypeConverterFactory.instance.typeConvert(instance, classLoader);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#getInstanceType()
	 */
	public SINType getInstanceType() throws TypeConverterException, NamedInstanceNotFoundException {

		if (!parsed)
			parse();
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#getMethodName()
	 */
	public String getMethodName() throws TypeConverterException, NamedInstanceNotFoundException {

		if (!parsed)
			parse();
		return methodName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#getParams()
	 */
	public List<SINType> getParams() throws TypeConverterException, NamedInstanceNotFoundException {

		if (!parsed)
			parse();
		return params;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#getRawSINExpression()
	 */
	public String getRawSINExpression() throws TypeConverterException, NamedInstanceNotFoundException {

		if (!parsed)
			parse();
		return rawSINExpression;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#getAssociatedMessage()
	 */
	public String getAssociatedMessage() {

		return associatedMessage;
	}

	public class Member {

		private final AccessibleObject member;
		private final List<Object> paramObjects;

		public Member(AccessibleObject member, List<Object> paramObjects) {

			this.member = member;
			this.paramObjects = paramObjects;
		}

		public AccessibleObject getMember() {

			return member;
		}

		public List<Object> getParamObjects() {

			return paramObjects;
		}
	}

	public Signature getSignature(ClassLoader cl) throws TypeConverterException, NamedInstanceNotFoundException, EvaluatorException {

		if (signature != null)
			return signature;
		Object instanceObj = getInstance(cl);
		Member member = getMember(instanceObj, cl);
		if (member == null)
			return null;
		signature = SignatureTableFactory.instance.getSignature(member.getMember());
		return signature;
	}

	public Member getMember(Object instanceObj, ClassLoader classLoader) throws EvaluatorException, TypeConverterException, NamedInstanceNotFoundException {

		if (methodName == null && params != null) {
			// Get constructor
			// ---------------
			if (!(instanceObj instanceof Class<?>)) {
				throw new EvaluatorException("\"new\" keyword cannot be used with a named instance (must be a class)");
			}
			List<Object> paramObjects = getParamObjects(classLoader);
			Constructor<?> constructor = BasicUtils.getConstructor((Class<?>) instanceObj, paramObjects);
			if (constructor == null) {
				throw new EvaluatorException("No constructor in class " + ((Class<?>) instanceObj).getName() + //
						" with parameter classes compatible with " + getClassNames(paramObjects));
			}
			return new Member(constructor, paramObjects);
		}
		if (methodName != null) {
			Class<?> clazz = instanceObj instanceof Class<?> ? (Class<?>) instanceObj : instanceObj.getClass();
			if (params == null) {
				// Get field (instance is the class object; methodName is the field name)
				// ---------------
				Field field = BasicUtils.getField(clazz, methodName);
				if (field == null) {
					// Check if field exists on class; only the case where field is defined by
					// java.lang.Class.
					field = BasicUtils.getField(instanceObj.getClass(), methodName);
					if (field == null) {
						throw new EvaluatorException("No field in class " + clazz.getName() + //
								" with name \"" + methodName + "\"");
					}
				}
				return new Member(field, null);

			} else {
				// Get method
				// ---------------
				List<Object> paramObjects = getParamObjects(classLoader);
				Method method = BasicUtils.getMethod(clazz, methodName, paramObjects);
				if (method == null) {
					// Check if method exists on class; only the case where method is defined by
					// java.lang.Class.
					method = BasicUtils.getMethod(instanceObj.getClass(), methodName, paramObjects);
					if (method == null) {
						throw new EvaluatorException("No method in class " + clazz.getName() + //
								" with name \"" + methodName + "\" and parameter classes compatible with " + //
								getClassNames(paramObjects));
					}
				}
				return new Member(method, paramObjects);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sureassert.uc.runtime.ISINExpression#invoke(java.lang.Object, java.lang.ClassLoader,
	 * boolean)
	 */
	public Object invoke(Object instanceObj, ClassLoader classLoader, DefaultToType defaultTo) throws SecurityException, NoSuchMethodException, EvaluatorException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, TypeConverterException, NamedInstanceNotFoundException, InstantiationException, NamedInstanceException {

		if (!parsed)
			parse();

		if (methodName != null) {
			String builtInMethodName = BuiltInMethods.getBuiltInMethodName(methodName);
			if (builtInMethodName != null) {
				instanceObj = BuiltInMethods.class;
				methodName = builtInMethodName;
			}
		}

		// NOTE: Need to be able to tell where instance is a class on which a static method should
		// be executed in which case clazz is not required. Need additional check for method defined
		// on java.lang.Class itself e.g. Sin exp: x.getClass().getSimpleName()
		Object result = instanceObj;

		if (methodName != null) {
			if (instanceObj == null) {
				throw new EvaluatorException(instance.getSINValue() + " returned null: cannot invoke " + methodName);
			}
		}

		// Execute method/constructor/field
		if (isArrayLength(instanceObj)) {
			// Note array.length is not a Field returned by reflection API
			result = Array.getLength(instanceObj);
		} else {
			Member memberDetails = getMember(instanceObj, classLoader);
			if (memberDetails != null) {
				AccessibleObject member = memberDetails.member;
				if (member instanceof Field) {
					((Field) member).setAccessible(true);
					result = ((Field) member).get(instanceObj);
				} else if (member instanceof Method) {
					((Method) member).setAccessible(true);
					result = ((Method) member).invoke(instanceObj, memberDetails.paramObjects.toArray());
				} else if (member instanceof Constructor) {
					((Constructor<?>) member).setAccessible(true);
					result = ((Constructor<?>) member).newInstance(memberDetails.paramObjects.toArray());
				}
			}
		}

		// If set execute field setters on returned instance
		if (fieldSetters != null) {
			if (result == null) {
				throw new EvaluatorException("Cannot set field values on null instance");
			}
			Class<?> resultClass = result.getClass();
			for (Entry<String, SINType> fieldSetter : fieldSetters.entrySet()) {
				Field field = BasicUtils.getField(resultClass, fieldSetter.getKey());
				if (field == null) {
					throw new EvaluatorException("Field \"" + fieldSetter.getKey() + //
							"\" not found in class " + resultClass.getName());
				}
				field.setAccessible(true);
				field.set(result, TypeConverterFactory.instance.typeConvert(fieldSetter.getValue(), null));
			}
		}

		// If set execute chained expression using this result as the named instance
		if (chainedExpression != null) {
			NamedInstanceFactory.getInstance().addNamedInstance(//
					NamedInstanceFactory.CHAINED_INSTANCE_NAME, result, null);
			result = chainedExpression.invoke(chainedExpression.getInstance(classLoader), classLoader, DefaultToType.NONE);
		}

		boolean isResultBoolean = result != null && (result instanceof Boolean || Boolean.TYPE.isAssignableFrom(result.getClass()));
		boolean isRawExprBooleanLiteral = "false".equals(rawSINExpression) || "true".equals(rawSINExpression);
		if (defaultTo != DefaultToType.NONE && (!isResultBoolean || isRawExprBooleanLiteral)) {

			Object retval;
			if (defaultTo == DefaultToType.RETVAL) {
				// Result is non-boolean; assume veq(retval, result) or if class, isa(retval,
				// result)
				displayExpr = "=(retval, " + rawSINExpression + ")";
				retval = NamedInstanceFactory.getInstance().getNamedInstance(//
						NamedInstanceFactory.RETVAL_INSTANCE_NAME, classLoader);
			} else {
				// Result is non-boolean; assume veq(arg, result) or if class, isa(arg, result)
				displayExpr = "=(arg, " + rawSINExpression + ")";
				retval = NamedInstanceFactory.getInstance().getNamedInstance(//
						NamedInstanceFactory.VERIFY_ARG_INSTANCE_NAME, classLoader);
			}
			boolean equals = BasicUtils.equals(retval, result);
			boolean isAClass = result != null && result instanceof Class<?>;
			if (!equals && isAClass) {
				// Default to isa(x)
				displayExpr = "isa" + displayExpr.substring(1);
				return BuiltInMethods.__SA_isa(retval, (Class<?>) result);
			} else {
				return equals;
			}
		}

		return result;
	}

	private boolean isArrayLength(Object instanceObj) {

		return instanceObj != null && instanceObj.getClass().isArray() && methodName != null && methodName.equals("length");
	}

	private List<Object> getParamObjects(ClassLoader classLoader) throws TypeConverterException, NamedInstanceNotFoundException {

		List<Object> paramObjects = new ArrayList<Object>();
		for (SINType param : params) {
			paramObjects.add(TypeConverterFactory.instance.typeConvert(param, classLoader));
		}
		return paramObjects;
	}

	private static String getClassNames(List<Object> objects) {

		StringBuilder classNames = new StringBuilder("[");
		for (int i = 0; i < objects.size(); i++) {
			classNames.append(objects.get(i).getClass().getName());
			if (i < objects.size() - 1)
				classNames.append(", ");
		}
		classNames.append("]");
		return classNames.toString();
	}

	/**
	 * If the given SINType doesn't have a prefix adds one.
	 * 
	 * @param escapedSinType
	 * @return
	 */
	private String addPrefix(String sinTypeStr) {

		if (sinTypeStr.indexOf(":") == -1) {
			return NamedInstanceTC.PREFIX + ":" + sinTypeStr;
		}
		return sinTypeStr;
	}

	// private SINType newStringLiteralSinType(String instanceName) throws TypeConverterException {
	//
	// return SINType.newFromRaw(instanceName);
	// }

	public boolean isEmpty() {

		return rawSINExpression == null || rawSINExpression.length() == 0;
	}

	@Override
	public String toString() {

		return displayExpr == null ? rawSINExpression : displayExpr;
	}
}
