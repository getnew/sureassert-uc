package com.sureassert.uc.runtime;

import com.sureassert.uc.runtime.ISINExpression.DefaultToType;
import com.sureassert.uc.runtime.exception.SARuntimeException;

/**
 * Matches SINExpressions containing method invocation filters / wildcards
 * 
 * @author Nathan Dolan
 * 
 */
public class SINExpressionMatcher {

	/**
	 * Attempts to match the given expression of the form class.method(conditionalExpr1,
	 * ...conditionalExprN)
	 * with the given method signature and method argument values.
	 * If the expression matches, an INFO ExecutorResult is returned with a message to this effect.
	 * Otherwise: <br>
	 * <li>If the signature matched but the argument expression did not, a WARN ExecutorResult is
	 * returned with details, or</li> <br>
	 * <li>if the signature did not match null is returned.
	 * 
	 * @param expr
	 * @param method
	 * @param invokedObj
	 * @param paramVals
	 * @param cl
	 * @return
	 */
	public ExecutorResult match(ISINExpression expr, Signature method, Object invokedObj, //
			Object[] paramVals, ClassLoader cl) {

		try {
			Object instance = expr.getInstance(cl);
			// Match method name
			if (instance != null && method.getMemberName() != null && method.getMemberName().equals(expr.getMethodName())) {

				// Match instance
				boolean matchedInstance = false;
				if (instance.getClass().equals(Class.class)) {
					// Declared class name - match on class name
					matchedInstance = cl.loadClass(method.getClassName()).isAssignableFrom((Class<?>) instance);
				} else {
					// Resolves to instance - match on invoked instance equality
					matchedInstance = instance == invokedObj;
				}
				if (matchedInstance) {

					// Match parameters
					String[] params = method.getParamClassNames();

					// check for no params
					boolean matchedParams = false;
					if (params == null || params.length == 0 || expr.getParams() == null || expr.getParams().isEmpty()) {
						if (expr.getParams() == null) {
							// no params part specified in expr expression so match any
							matchedParams = true;
						} else if ((params == null || params.length == 0) && expr.getParams().isEmpty()) {
							// expr method with no params matched
							matchedParams = true;
						}
					} else if (!matchedParams && expr.getParams() != null && !expr.getParams().isEmpty()) {

						// match each param
						matchedParams = paramVals.length == params.length && params.length == expr.getParams().size();
						try {
							NamedInstanceFactory.getInstance().addNamedInstance(//
									NamedInstanceFactory.VERIFY_OBJ_INSTANCE_NAME, invokedObj, null);
							for (int i = 0; i < params.length && matchedParams; i++) {
								// check for wildcard
								SINType param = expr.getParams().get(i);
								if (!param.toRawString().equals("*")) {

									try {
										NamedInstanceFactory.getInstance().addNamedInstance(//
												NamedInstanceFactory.VERIFY_ARG_INSTANCE_NAME, paramVals[i], null);
										// execute boolean expression
										ISINExpression paramExpr = SINExpressionFactory.get(param.toRawString());
										Object evalResult = paramExpr.invoke(paramExpr.getInstance(cl), cl, DefaultToType.ARG);

										if (evalResult == null) {
											// Didn't match
											matchedParams = false;
											/*
											 * executorResults.add(new
											 * ExecutorResult(BasicUtils
											 * .getCurrentUseCaseDisplayName(false) + //
											 * " invocation of " + expect.toString() + //
											 * " returned null", Type.ERROR, execResult));
											 */
										} else {

											boolean boolResult = ((Boolean) evalResult).booleanValue();
											if (boolResult) {
												// Matched
											} else {
												// Didn't match
												matchedParams = false;
												/*
												 * String associatedMessage =
												 * expect.getAssociatedMessage();
												 * if (associatedMessage == null) {
												 * associatedMessage =
												 * BasicUtils.getCurrentUseCaseDisplayName
												 * (false) + //
												 * " evaluation of " + expect.toString() +
												 * " returned false";
												 * }
												 * executorResults.add(new
												 * ExecutorResult(associatedMessage,
												 * Type.ERROR, evalResult));
												 */
											}
										}

									} finally {
										NamedInstanceFactory.getInstance().removeNamedInstance(//
												NamedInstanceFactory.VERIFY_ARG_INSTANCE_NAME);
									}
								}
							}
						} finally {

							NamedInstanceFactory.getInstance().removeNamedInstance(//
									NamedInstanceFactory.VERIFY_OBJ_INSTANCE_NAME);
						}
					}

					if (matchedParams) {
						return new ExecutorResult("Verified invocation", ExecutorResult.Type.INFO, null);
					}
				}
			}
			return null;
		} catch (Exception e) {
			throw new SARuntimeException(e);
		}
	}
}
