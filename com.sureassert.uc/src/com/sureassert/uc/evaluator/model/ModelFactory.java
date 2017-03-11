package com.sureassert.uc.evaluator.model;

import org.eclipse.jdt.core.IMemberValuePair;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.SINExpressionFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.model.UseCaseModel;
import com.sureassert.uc.runtime.model.UseCaseModel.NamedUseCaseModelFactoryI;
import com.sureassert.uc.runtime.model.UseCaseModel.SpecifiedProps;

public class ModelFactory {

	public static UseCaseModel newUseCaseModel(IMemberValuePair[] params, Signature signature, int declaredLineNum, //
			Signature inheritedFromSig, boolean definedAsUseCase, NamedUseCaseModelFactoryI namedUCMFactory) {

		SpecifiedProps specifiedProps = new SpecifiedProps();
		String _name = null;
		String _description = null;
		String _instanceout = null;
		String _expectexception = null;
		ISINExpression _instance = null;
		String _constructor = null;
		String[] _args = null;
		ISINExpression[] _expect = null;
		ISINExpression[] _verify = null;
		ISINExpression[] _debug = null;
		ISINExpression[] _before = null;
		ISINExpression[] _after = null;
		String[] _depends = null;
		String[] _stubs = null;
		String[] _vstubs = null;
		String[] _defaultimpls = null;
		String[] _template = null;

		String ucNamePrefix = signature != null && signature.getClassName() != null ? //
		(BasicUtils.getSimpleClassName(signature.getClassName()) + "/") : "";

		for (IMemberValuePair param : params) {

			// NOTE: Must set properties to null in UseCaseModel if user sets them to empty string
			// to facilitate template overriding

			if (param.getMemberName().equals("name") || param.getMemberName().equals("n")) {
				specifiedProps.name = true;
				if (((String) param.getValue()).length() > 0)
					_name = BasicUtils.appendUCNamePrefix(ucNamePrefix, (String) param.getValue());
			}
			if (param.getMemberName().equals("description") || param.getMemberName().equals("d")) {
				specifiedProps.description = true;
				if (((String) param.getValue()).length() > 0)
					_description = (String) param.getValue();
			}
			if (param.getMemberName().equals("instanceout") || param.getMemberName().equals("io")) {
				specifiedProps.instanceout = true;
				if (((String) param.getValue()).length() > 0)
					_instanceout = BasicUtils.appendUCNamePrefix(ucNamePrefix, (String) param.getValue());
			}
			if (param.getMemberName().equals("expectexception") || param.getMemberName().equals("ee")) {
				specifiedProps.expectexception = true;
				if (((String) param.getValue()).length() > 0)
					_expectexception = (String) param.getValue();
			}
			if (param.getMemberName().equals("args") || param.getMemberName().equals("a")) {
				specifiedProps.args = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_args = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_args[i] = (String) value[i];
				}
				if (_args.length == 1 && _args[0].length() == 0)
					_args = null;
			}
			if (param.getMemberName().equals("expect") || param.getMemberName().equals("e")) {
				specifiedProps.expect = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_expect = new ISINExpression[value.length];
				for (int i = 0; i < value.length; i++) {
					_expect[i] = SINExpressionFactory.get((String) value[i]);
				}
				if (_expect.length == 1 && _expect[0].isEmpty())
					_expect = null;
			}
			if (param.getMemberName().equals("verify") || param.getMemberName().equals("v")) {
				specifiedProps.verify = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_verify = new ISINExpression[value.length];
				for (int i = 0; i < value.length; i++) {

					try {
						SINType type = SINType.newFromRaw((String) value[i]);
						type.toString();
					} catch (TypeConverterException e) {
						e.printStackTrace();
					}
					_verify[i] = SINExpressionFactory.get((String) value[i]);
				}
				if (_verify.length == 1 && _verify[0].isEmpty())
					_verify = null;
			}
			if (param.getMemberName().equals("debug")) {
				specifiedProps.debug = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_debug = new ISINExpression[value.length];
				for (int i = 0; i < value.length; i++) {
					_debug[i] = SINExpressionFactory.get((String) value[i]);
				}
				if (_debug.length == 1 && _debug[0].isEmpty())
					_debug = null;
			}
			if (param.getMemberName().equals("instance") || param.getMemberName().equals("i")) {
				specifiedProps.instance = true;
				if (((String) param.getValue()).length() > 0)
					_instance = SINExpressionFactory.get((String) param.getValue());
			}
			if (param.getMemberName().equals("constructor")) {
				specifiedProps.constructor = true;
				if (((String) param.getValue()).length() > 0)
					_constructor = (String) param.getValue();
			}
			if (param.getMemberName().equals("before")) {
				specifiedProps.before = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_before = new ISINExpression[value.length];
				for (int i = 0; i < value.length; i++) {
					_before[i] = SINExpressionFactory.get((String) value[i]);
				}
				if (_before.length == 1 && _before[0].isEmpty())
					_before = null;
			}
			if (param.getMemberName().equals("after")) {
				specifiedProps.after = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_after = new ISINExpression[value.length];
				for (int i = 0; i < value.length; i++) {
					_after[i] = SINExpressionFactory.get((String) value[i]);
				}
				if (_after.length == 1 && _after[0].isEmpty())
					_after = null;
			}
			if (param.getMemberName().equals("depends")) {
				specifiedProps.depends = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_depends = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_depends[i] = (String) value[i];
				}
				if (_depends.length == 1 && _depends[0].length() == 0)
					_depends = null;
			}
			if (param.getMemberName().equals("stubs") || param.getMemberName().equals("s")) {
				specifiedProps.stubs = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_stubs = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_stubs[i] = (String) value[i];
				}
				if (_stubs.length == 1 && _stubs[0].length() == 0)
					_stubs = null;
			}
			if (param.getMemberName().equals("vstubs") || param.getMemberName().equals("vs")) {
				specifiedProps.vstubs = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_vstubs = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_vstubs[i] = (String) value[i];
				}
				if (_vstubs.length == 1 && _vstubs[0].length() == 0)
					_vstubs = null;
			}
			if (param.getMemberName().equals("defaultimpls")) {
				specifiedProps.defaultimpls = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_defaultimpls = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_defaultimpls[i] = (String) value[i];
				}
				if (_defaultimpls.length == 1 && _defaultimpls[0].length() == 0)
					_defaultimpls = null;
			}
			if (param.getMemberName().equals("template") || param.getMemberName().equals("t")) {
				specifiedProps.template = true;
				Object[] value = (param.getValue() instanceof Object[]) ? //
				(Object[]) param.getValue() : new Object[] { param.getValue() };
				_template = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					_template[i] = (String) value[i];
				}
				if (_template.length == 1 && _template[0].length() == 0)
					_template = null;
			}
		}

		UseCaseModel model = new UseCaseModel(_name, _description, _instanceout, _args, _expect, _verify, _instance, _constructor, _before, _after, _depends, declaredLineNum, signature,
				inheritedFromSig, null, _stubs, _vstubs, _defaultimpls, _debug, _expectexception, definedAsUseCase, specifiedProps);

		model.setFromTemplates(_template, ucNamePrefix, namedUCMFactory); // Must be last assignment

		return model;
	}
}
