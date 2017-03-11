/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.model;

import org.apache.commons.lang.ArrayUtils;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ErrorContainingModel;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINExpressionFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.SaUCStub;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.typeconverter.NamedInstanceTC;

public class UseCaseModel implements Cloneable, ErrorContainingModel {

	public static int numDefinitions = 0;

	public static final String DEFAULT_NOARGS_CONSTRUCTOR = //
	NamedInstanceFactory.SPECIAL_CHAR + "noargs" + NamedInstanceFactory.SPECIAL_CHAR;
	public static final String NO_CONSTRUCTOR = //
	NamedInstanceFactory.SPECIAL_CHAR + "static" + NamedInstanceFactory.SPECIAL_CHAR;

	private String name;
	private final String description;
	private String instanceout;
	private String[] args;
	private ISINExpression[] expect;
	private ISINExpression[] verify;
	private ISINExpression instance;
	private String constructor;
	private ISINExpression[] before;
	private ISINExpression[] after;
	private String[] depends;
	private final int declaredLineNum;
	private final Signature signature;
	private final Signature inheritedFromSig;
	private String error;
	private String[] stubs;
	private String[] vstubs;
	private SaUCStub[] stubObjs;
	private ISINExpression[] verifyObjs;
	private String[] defaultimpls;
	private ISINExpression[] debug;
	private String expectexception;
	private final boolean definedAsUseCase;

	SpecifiedProps specifiedProps = new SpecifiedProps();

	/*
	 * private String def(String s1, String s2) {
	 * 
	 * return s1 == null || s1.length() == 0 ? s2 : s1;
	 * }
	 * 
	 * private String[] def(String[] s1, String[] s2) {
	 * 
	 * return s1 == null || s1.length == 0 ? s2 : s1;
	 * }
	 * 
	 * public UseCaseModel(UseCase uc, Signature signature, Signature inheritedFromSig) {
	 * 
	 * name = def(uc.name(), uc.n());
	 * description = def(uc.description(), uc.d());
	 * instanceout = def(uc.instanceout(), uc.io());
	 * args = def(uc.args(), uc.a());
	 * 
	 * String[] _expects = def(uc.expect(), uc.e());
	 * expect = new ISINExpression[_expects.length];
	 * for (int i = 0; i < _expects.length; i++) {
	 * expect[i] = SINExpressionFactory.get(_expects[i]);
	 * }
	 * String[] _verify = def(uc.expect(), uc.e());
	 * verify = new ISINExpression[_verify.length];
	 * for (int i = 0; i < _verify.length; i++) {
	 * verify[i] = SINExpressionFactory.get(_verify[i]);
	 * }
	 * instance = SINExpressionFactory.get(def(uc.instance(), uc.i()));
	 * before = new ISINExpression[uc.before().length];
	 * for (int i = 0; i < uc.before().length; i++) {
	 * before[i] = SINExpressionFactory.get(uc.before()[i]);
	 * }
	 * depends = uc.depends();
	 * declaredLineNum = -1;
	 * this.signature = signature;
	 * this.inheritedFromSig = inheritedFromSig;
	 * String[] _stubs = def(uc.stubs(), uc.s());
	 * stubs = new String[_stubs.length];
	 * for (int i = 0; i < _stubs.length; i++) {
	 * stubs[i] = _stubs[i];
	 * }
	 * String[] _vstubs = def(uc.stubs(), uc.s());
	 * addVStubs(_vstubs);
	 * defaultimpls = new String[uc.stubs().length];
	 * for (int i = 0; i < uc.defaultimpls().length; i++) {
	 * defaultimpls[i] = uc.defaultimpls()[i];
	 * }
	 * debug = new ISINExpression[uc.debug().length];
	 * expectexception = def(uc.expectexception(), uc.ee());
	 * }
	 */
	private ISINExpression[] getVStubVerifies() {

		if (vstubs != null && vstubs.length > 0) {
			ISINExpression[] vstubVerifies = new ISINExpression[vstubs.length];
			for (int i = 0; i < vstubs.length; i++) {
				if (vstubs[i].lastIndexOf("=") == -1) {
					setError("Syntax error in vstub definition \"" + vstubs[i] + "\".  Should be in format methodSignature=stubExpression " + //
							"but no \"=\" found.  Did you mean to use verify?");
					return new ISINExpression[0];
				}
				vstubVerifies[i] = SINExpressionFactory.get(vstubs[i].substring(0, vstubs[i].lastIndexOf("=")));
			}
			return vstubVerifies;
		}
		return new ISINExpression[0];
	}

	private String[] getVStubStubs() {

		if (vstubs != null && vstubs.length > 0) {
			return vstubs;
		}
		return new String[0];
	}

	// public UseCaseModel(IMemberValuePair[] params, Signature signature, int declaredLineNum,
	// Signature inheritedFromSig, boolean definedAsUseCase) {
	//
	// this(params, signature, declaredLineNum, inheritedFromSig, definedAsUseCase, null);
	// }

	/**
	 * Determines whether a given property has been specified (regardless of whether it was
	 * specified empty).
	 */
	public static class SpecifiedProps {

		public boolean name;
		public boolean description;
		public boolean instanceout;
		public boolean args;
		public boolean expect;
		public boolean verify;
		public boolean instance;
		public boolean constructor;
		public boolean before;
		public boolean after;
		public boolean depends;
		public boolean error;
		public boolean stubs;
		public boolean vstubs;
		public boolean stubObjs;
		public boolean defaultimpls;
		public boolean debug;
		public boolean expectexception;
		public boolean template;
	}

	public UseCaseModel(String name, String description, String instanceout, String[] args, ISINExpression[] expect, ISINExpression[] verify, ISINExpression instance, String constructor,
			ISINExpression[] before, ISINExpression[] after, String[] depends, int declaredLineNum, Signature signature, Signature inheritedFromSig, String error, String[] stubs, String[] vstubs,
			String[] defaultimpls, ISINExpression[] debug, String expectexception, boolean definedAsUseCase, SpecifiedProps specifiedProps) {

		String ucNamePrefix = signature != null && signature.getClassName() != null ? //
		(BasicUtils.getSimpleClassName(signature.getClassName()) + "/") : "";

		this.name = BasicUtils.appendUCNamePrefix(ucNamePrefix, name);
		this.description = description;
		this.instanceout = BasicUtils.appendUCNamePrefix(ucNamePrefix, instanceout);
		this.args = args;
		this.expect = expect;
		this.verify = verify;
		this.instance = instance;
		this.constructor = constructor;
		this.before = before;
		this.after = after;
		this.depends = depends;
		this.declaredLineNum = declaredLineNum;
		this.signature = signature;
		this.inheritedFromSig = inheritedFromSig;
		this.error = error;
		this.stubs = stubs;
		this.vstubs = vstubs;
		this.defaultimpls = defaultimpls;
		this.debug = debug;
		this.expectexception = expectexception;
		this.definedAsUseCase = definedAsUseCase;

		if (specifiedProps != null)
			this.specifiedProps = specifiedProps;

		PersistentDataFactory.getInstance().setWasLastExecUseCase(definedAsUseCase);

		if (name != null && name.contains(NamedInstanceTC.UC_REF_CHAR)) {
			setError("UseCases cannot be assigned names containing the " + NamedInstanceTC.UC_REF_CHAR + " character");
		}
	}

	public void setFromTemplates(String[] templates, String ucNamePrefix, NamedUseCaseModelFactoryI namedUCMFactory) {

		if (templates == null || namedUCMFactory == null)
			return;
		for (int i = templates.length - 1; i >= 0; i--) {
			String template = templates[i];
			UseCaseModel templateUCModel = namedUCMFactory.getUCModel(BasicUtils.appendUCNamePrefix(ucNamePrefix, template));
			// if (templateUCModel == null) {
			// // try without prefix
			// templateUCModel =
			// namedUCMFactory.getUCModel(BasicUtils.appendUCNamePrefix(ucNamePrefix, template));
			// }
			if (templateUCModel != null) {

				// NOTE: Only copy properties if they are not specified in this UseCase and are
				// specified in the other

				// name = templateUCModel.name;
				// description = templateUCModel.description;
				if (!specifiedProps.instanceout && templateUCModel.specifiedProps.instanceout)
					instanceout = templateUCModel.instanceout;
				if (!specifiedProps.args && templateUCModel.specifiedProps.args)
					args = templateUCModel.args;
				if (!specifiedProps.expect && templateUCModel.specifiedProps.expect)
					expect = templateUCModel.expect;
				if (!specifiedProps.verify && templateUCModel.specifiedProps.verify)
					verify = templateUCModel.verify;
				if (!specifiedProps.instance && templateUCModel.specifiedProps.instance)
					instance = templateUCModel.instance;
				if (!specifiedProps.constructor && templateUCModel.specifiedProps.constructor)
					constructor = templateUCModel.constructor;
				if (!specifiedProps.before && templateUCModel.specifiedProps.before)
					before = templateUCModel.before;
				if (!specifiedProps.after && templateUCModel.specifiedProps.after)
					after = templateUCModel.after;
				if (!specifiedProps.depends && templateUCModel.specifiedProps.depends)
					depends = templateUCModel.depends;
				// declaredLineNum = templateUCModel.declaredLineNum;
				// signature = templateUCModel.signature;
				// inheritedFromSig = templateUCModel.inheritedFromSig;
				if (!specifiedProps.error && templateUCModel.specifiedProps.error)
					error = "Error in template UseCase \"" + template + "\": " + templateUCModel.error;
				if (!specifiedProps.stubs && templateUCModel.specifiedProps.stubs)
					stubs = templateUCModel.stubs;
				if (!specifiedProps.vstubs && templateUCModel.specifiedProps.vstubs)
					vstubs = templateUCModel.vstubs;
				if (!specifiedProps.stubObjs && templateUCModel.specifiedProps.stubObjs)
					stubObjs = templateUCModel.stubObjs;
				if (!specifiedProps.defaultimpls && templateUCModel.specifiedProps.defaultimpls)
					defaultimpls = templateUCModel.defaultimpls;
				if (!specifiedProps.debug && templateUCModel.specifiedProps.debug)
					debug = templateUCModel.debug;
				if (!specifiedProps.expectexception && templateUCModel.specifiedProps.expectexception)
					expectexception = templateUCModel.expectexception;
				// definedAsUseCase = templateUCModel.definedAsUseCase;

				// NOTE: No need to copy template as template UC will already have copied its own
				// templated props if it has any

			} else {
				error = "Could not load template UseCase \"" + template + "\": " + //
						"No UseCase found in this source file with this name";
			}
		}
	}

	@Override
	public UseCaseModel clone() {

		return new UseCaseModel(name, description, instanceout, args, expect, verify, instance, DEFAULT_NOARGS_CONSTRUCTOR, before, after, //
				depends, declaredLineNum, signature, inheritedFromSig, error, stubs, vstubs, defaultimpls, debug, expectexception, definedAsUseCase, null);
	}

	public UseCaseModel clone(Signature newSignature) {

		return new UseCaseModel(name, description, instanceout, args, expect, verify, instance, DEFAULT_NOARGS_CONSTRUCTOR, before, after, //
				depends, declaredLineNum, newSignature, inheritedFromSig, error, stubs, vstubs, defaultimpls, debug, expectexception, definedAsUseCase, null);
	}

	public boolean isDefinedAsUseCase() {

		return definedAsUseCase;
	}

	public String getDescription() {

		return description;
	}

	public String getInstanceout() {

		return instanceout;
	}

	public String[] getArgs() {

		return args;
	}

	public SINType[] getArgSINTypes() {

		if (args == null)
			return new SINType[] {};
		SINType[] typedArgs = new SINType[args.length];
		for (int i = 0; i < args.length; i++) {
			try {
				typedArgs[i] = SINType.newFromRaw(args[i]);
			} catch (TypeConverterException e) {
				setError(e.getMessage());
				try {
					typedArgs[i] = SINType.newFromRaw("null");
				} catch (TypeConverterException e1) {
					// No possible
				}
			}
		}
		return typedArgs;
	}

	public void setArgSINType(int index, String argRaw) {

		args[index] = argRaw;
	}

	public Object[] getTypeConvertedArgs(ClassLoader classLoader, Class<?>[] paramTypes) throws TypeConverterException, NamedInstanceNotFoundException {

		if (args == null)
			return new Object[] {};
		Object[] typedArgs = new Object[args.length];
		SINType sinType;
		for (int i = 0; i < args.length; i++) {
			// Handle auto default class arg (*)
			if (args[i].equals("*") && paramTypes.length == args.length) {
				args[i] = "new *" + paramTypes[i].getSimpleName() + "()";
			}
			sinType = SINType.newFromRaw(args[i]);
			typedArgs[i] = TypeConverterFactory.instance.typeConvert(sinType, classLoader);
		}
		return typedArgs;
	}

	public SaUCStub[] getStubs() throws TypeConverterException {

		if (stubObjs != null)
			return stubObjs;

		String[] _stubs = (String[]) ArrayUtils.addAll(stubs, getVStubStubs());
		if (_stubs == null)
			return new SaUCStub[] {};
		stubObjs = new SaUCStub[_stubs.length];
		for (int i = 0; i < _stubs.length; i++) {

			stubObjs[i] = SaUCStub.newSaUCStub(_stubs[i]);
		}
		return stubObjs;
	}

	public void addStubs(String[] parentStubs) {

		if (parentStubs == null)
			return;
		if (stubs == null)
			stubs = new String[] {};
		stubs = (String[]) ArrayUtils.addAll(stubs, parentStubs);
	}

	public String[] getDefaultImpls() {

		return defaultimpls;
	}

	public ISINExpression[] getExpects() {

		return expect;
	}

	public ISINExpression[] getVerify() {

		if (verifyObjs != null)
			return verifyObjs;

		// NOTE: Need to do this to retain the original vstubs definition so it can be templated.
		verifyObjs = (ISINExpression[]) ArrayUtils.addAll(verify, getVStubVerifies());

		return verifyObjs;
	}

	public ISINExpression[] getDebug() {

		return debug;
	}

	public ISINExpression getInstance() {

		return instance;
	}

	public void setInstance(ISINExpression instance) {

		this.instance = instance;
	}

	public String getConstructor() {

		return constructor;
	}

	public void setConstructor(String constructor) {

		this.constructor = constructor;
	}

	public ISINExpression[] getBefore() {

		return before;
	}

	public ISINExpression[] getAfter() {

		return after;
	}

	public String[] getDepends() {

		return depends;
	}

	public String getName() {

		return name;
	}

	public void setName(String name) {

		this.name = name;
	}

	public void setInstanceout(String instanceout) {

		this.instanceout = instanceout;
	}

	public int getDeclaredLineNum() {

		return declaredLineNum;
	}

	public Signature getSignature() {

		return signature;
	}

	public void setError(String error) {

		if (this.error == null)
			this.error = error;
	}

	public String getError() {

		if (error == null && stubObjs != null) {
			for (SaUCStub stub : stubObjs) {
				if (stub.getError() != null)
					error = stub.getError();
			}
		}
		return error;
	}

	public boolean isValid() {

		return error == null;
	}

	public Signature getInheritedFromSignature() {

		return inheritedFromSig;
	}

	public String getExpectException() {

		return expectexception;
	}

	@Override
	public String toString() {

		return name + ": " + signature.toString();
	}

	public static interface NamedUseCaseModelFactoryI {

		public UseCaseModel getUCModel(String ucName);
	}

}
