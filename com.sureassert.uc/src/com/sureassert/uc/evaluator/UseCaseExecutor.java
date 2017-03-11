/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.evaluator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.BasicUtils;
import com.sureassert.uc.runtime.ExecutorResult;
import com.sureassert.uc.runtime.ExecutorResult.Type;
import com.sureassert.uc.runtime.ISINExpression;
import com.sureassert.uc.runtime.ISINExpression.DefaultToType;
import com.sureassert.uc.runtime.NamedInstanceException;
import com.sureassert.uc.runtime.NamedInstanceFactory;
import com.sureassert.uc.runtime.NamedInstanceNotFoundException;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.PersistentDataLoadException;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.Timer;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.TypeConverterFactory;
import com.sureassert.uc.runtime.exception.EvaluatorException;
import com.sureassert.uc.runtime.exception.MethodArgumentException;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.exception.UseCaseException;
import com.sureassert.uc.runtime.model.UseCaseModel;

public class UseCaseExecutor {

	// private final DateFormat df;

	UseCaseModel ucModel;

	public UseCaseExecutor(UseCaseModel ucModel) throws TypeConverterException, NamedInstanceNotFoundException {

		this.ucModel = ucModel;
		// this.df = DateFormat.getTimeInstance(DateFormat.SHORT);
	}

	public UseCaseModel getModel() {

		return ucModel;
	}

	private void readPersistentData(ObjectInputStream in) throws IOException, ClassNotFoundException, PersistentDataLoadException {

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		BasicUtils.debug("Getting NamedInstanceFactory");
		NamedInstanceFactory.setInstance((NamedInstanceFactory) in.readObject());
		BasicUtils.debug("Getting number of PersistentData");
		int numPD = (Integer) in.readObject();
		for (int i = 0; i < numPD; i++) {
			BasicUtils.debug("Getting PersistentData[" + i + "]");
			pdf.load(in);
		}
	}

	private void writePersistentData(ObjectOutputStream out) throws IOException, ClassNotFoundException, PersistentDataLoadException {

		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		BasicUtils.debug("Writing NamedInstanceFactory");
		out.writeObject(NamedInstanceFactory.getInstance());

		pdf.writePersistentData(out);
	}

	/**
	 * Executes the given UseCase on the given method of the given object.
	 * 
	 * @param method
	 * @param targetObj
	 * @param classLoader
	 * @return A list of error descriptions; empty if no errors were encountered.
	 */
	public List<ExecutorResult> execute(AccessibleObject method, Object targetObj, ClassLoader classLoader, //
			boolean doEvaluate) throws NamedInstanceNotFoundException {

		// boolean b = true;
		// if (!b) {
		// Socket runServerSoc = null;
		// ObjectOutputStream out = null;
		// ObjectInputStream in = null;
		//
		// try {
		// runServerSoc = new Socket((String) null, 6871);
		// BasicUtils.debug("Got socket");
		// in = new ObjectInputStream(runServerSoc.getInputStream());
		// BasicUtils.debug("Got InputStream");
		//
		// writePersistentData(out);
		// readPersistentData(in);
		//
		// } catch (Exception e) {
		// throw new RuntimeException(e);
		//
		// } finally {
		// try {
		// if (in != null)
		// in.close();
		// if (out != null)
		// out.close();
		// if (runServerSoc != null)
		// runServerSoc.close();
		//
		// } catch (Exception e) {
		// throw new RuntimeException(e);
		// }
		// }
		// }

		try {
			ExecutePhaseTracker phaseTracker = new ExecutePhaseTracker();
			try {
				return _execute(method, targetObj, classLoader, doEvaluate, phaseTracker);
			} catch (NamedInstanceNotFoundException e) {
				// Add sub-UC postfix to any exception message if this UC is inherited
				// This is crucial as the postfix is used to determine which markers relate to which
				// concrete class
				e.setPrefixMessage(phaseTracker.phase.msg);
				if (ucModel.getInheritedFromSignature() != null)
					e.setAppendMessage(getSubUCMarkerPostfix(ucModel));
				throw e;
			} catch (SARuntimeException e) {
				e.setPrefixMessage(phaseTracker.phase.msg);
				if (ucModel.getInheritedFromSignature() != null)
					e.setAppendMessage(getSubUCMarkerPostfix(ucModel));
				throw e;

			} finally {
				// Execute afters
				phaseTracker.phase = ExecutePhase.AFTER;
				try {
					execute(ucModel.getAfter(), classLoader);
				} catch (NamedInstanceNotFoundException ninfe) {
					ninfe.setPrefixMessage(phaseTracker.phase.msg);
					if (ucModel.getInheritedFromSignature() != null)
						ninfe.setAppendMessage(getSubUCMarkerPostfix(ucModel));
					throw ninfe;
				} catch (UseCaseException uce) {
					uce.setPrefixMessage(phaseTracker.phase.msg);
					if (ucModel.getInheritedFromSignature() != null)
						uce.setAppendMessage(getSubUCMarkerPostfix(ucModel));
					throw new SARuntimeException(uce);
				} catch (SARuntimeException e) {
					e.setPrefixMessage(phaseTracker.phase.msg);
					if (ucModel.getInheritedFromSignature() != null)
						e.setAppendMessage(getSubUCMarkerPostfix(ucModel));
					throw e;
				} catch (Throwable e) {
					SARuntimeException newE = new SARuntimeException(e);
					newE.setPrefixMessage(phaseTracker.phase.msg);
					if (ucModel.getInheritedFromSignature() != null)
						newE.setAppendMessage(getSubUCMarkerPostfix(ucModel));
					throw newE;
				}
			}
		} finally {

			NamedInstanceFactory niFactory = NamedInstanceFactory.getInstance();
			niFactory.removeNamedInstance(NamedInstanceFactory.RETVAL_INSTANCE_NAME);
			niFactory.removeNamedInstance(NamedInstanceFactory.THIS_INSTANCE_NAME);
			int argNum = 1;
			while (niFactory.namedInstanceExists(NamedInstanceFactory.ARG_PREFIX + argNum)) {
				niFactory.removeNamedInstance(NamedInstanceFactory.ARG_PREFIX + argNum);
				argNum++;
			}
		}
	}

	public List<ExecutorResult> _execute(AccessibleObject method, Object targetObj, ClassLoader classLoader, //
			boolean doEvaluate, ExecutePhaseTracker phaseTracker) throws NamedInstanceNotFoundException {

		// Check for invalid model
		if (!ucModel.isValid()) {
			return Collections.singletonList(new ExecutorResult(//
					ucModel.getError(), Type.ERROR, null));
		}

		// Execute
		Object result = null;
		Class<?> targetClass = (targetObj == null && method instanceof Constructor<?>) ? ((Constructor<?>) method).getDeclaringClass() : targetObj.getClass();
		if (targetClass.getName().equals(Class.class.getName()) && targetObj != null && targetObj instanceof Class) {
			// static method
			targetClass = (Class<?>) targetObj;
		}
		// ClassLoader classLoader = targetClass.getClassLoader().
		Timer timer = null;
		try {
			NamedInstanceFactory.getInstance().addNamedInstance(NamedInstanceFactory.THIS_INSTANCE_NAME, targetObj, null);
			// Execute befores
			execute(ucModel.getBefore(), classLoader);
			phaseTracker.phase = ExecutePhase.EXEC;

			method.setAccessible(true);
			String methodName = "";
			if (method instanceof Method) {
				methodName = ((Method) method).getName();
				String ucName = ucModel.getName() == null ? "on " + targetClass.getName() + "." + methodName : ucModel.getName();
				BasicUtils.debug("Executing UseCase " + ucName);
				timer = new Timer("Execution of UC " + ucName);
				Object[] args = ucModel.getTypeConvertedArgs(classLoader, ((Method) method).getParameterTypes());
				checkArgs(args, (Method) method);
				result = ((Method) method).invoke(targetObj, args);
			} else if (method instanceof Constructor<?>) {
				methodName = ((Constructor<?>) method).getName();
				String ucName = ucModel.getName() == null ? " on " + targetClass.getName() + ".<init>" : ucModel.getName();
				BasicUtils.debug("Executing UseCase " + ucName);
				timer = new Timer("Execution of UC " + ucName);
				Object[] args = ucModel.getTypeConvertedArgs(classLoader, ((Constructor<?>) method).getParameterTypes());
				checkArgs(args, (Constructor<?>) method);
				result = ((Constructor<?>) method).newInstance(args);
			} else if (method instanceof Field) {
				methodName = ((Field) method).getName();
				String ucName = ucModel.getName() == null ? " on " + targetClass.getName() + "." + methodName : ucModel.getName();
				BasicUtils.debug("Executing UseCase " + ucName);
				timer = new Timer("Execution of UC " + ucName);
				result = ((Field) method).get(targetObj);
			}
			NamedInstanceFactory.getInstance().addNamedInstance(//
					NamedInstanceFactory.RETVAL_INSTANCE_NAME, result, null);
			timer.printExpiredTime();
			if (getModel().getName() != null) {
				NamedInstanceFactory.getInstance().addNamedInstance(getModel().getName(), result, //
						ucModel.getSignature());
			}
			if (getModel().getInstanceout() != null) {
				NamedInstanceFactory.getInstance().addNamedInstance(getModel().getInstanceout(), targetObj, //
						ucModel.getSignature());
			}
			phaseTracker.phase = ExecutePhase.EVAL;
			return postProcessResults(evaluate(result, ucModel.getExpects(), ucModel.getVerify(), ucModel.getDebug(), method, //
					classLoader, false, doEvaluate));

		} catch (InvocationTargetException e) {
			// InvocationTargetException wraps the exception thrown by the method invoked
			Throwable cause = e.getCause() == null ? e : e.getCause();
			if (cause != null && ucModel.getExpectException() != null && //
					(cause.getClass().getSimpleName().equals(ucModel.getExpectException()) || //
					cause.getClass().getName().equals(ucModel.getExpectException().replace('/', '.')))) {
				// Expected this exception
				try {
					// Result/retval is the exception
					result = cause;
					NamedInstanceFactory.getInstance().addNamedInstance(//
							NamedInstanceFactory.RETVAL_INSTANCE_NAME, result, null);
					timer.printExpiredTime();
					if (getModel().getName() != null) {
						NamedInstanceFactory.getInstance().addNamedInstance(getModel().getName(), result, //
								ucModel.getSignature());
					}
					if (getModel().getInstanceout() != null) {
						NamedInstanceFactory.getInstance().addNamedInstance(getModel().getInstanceout(), targetObj, //
								ucModel.getSignature());
					}
					phaseTracker.phase = ExecutePhase.EVAL;
					return postProcessResults(evaluate(result, ucModel.getExpects(), ucModel.getVerify(), ucModel.getDebug(), //
							method, classLoader, true, doEvaluate));
				} catch (NamedInstanceNotFoundException ninfe) {
					throw ninfe;
				} catch (UseCaseException uce) {
					throw new SARuntimeException(uce);
				} catch (Throwable e2) {
					String methodName = method == null ? "null" : ((Member) method).getName();
					ExecutorResult failedResult = new ExecutorResult(//
							BasicUtils.getCurrentUseCaseDisplayName(false) + " failure: " + //
									BasicUtils.toDisplayStr(e2), Type.ERROR, result, //
							BasicUtils.getLineNum(e2, methodName, targetClass), targetClass.getName());
					return postProcessResults(BasicUtils.<ExecutorResult> newList(failedResult, //
							getDefaultFailedExecResult(method, failedResult, targetClass)));
				}
			}
			ExecutorResult failedResult = new ExecutorResult(//
					BasicUtils.getCurrentUseCaseDisplayName(false) + " failure: " + //
							BasicUtils.toDisplayStr(cause), Type.ERROR, result, //
					BasicUtils.getLineNum(cause, ((Member) method).getName(), targetClass), targetClass.getName());
			return postProcessResults(BasicUtils.<ExecutorResult> newList(failedResult, //
					getDefaultFailedExecResult(method, failedResult, targetClass)));
		} catch (NamedInstanceNotFoundException ninfe) {
			throw ninfe;
		} catch (UseCaseException uce) {
			throw new SARuntimeException(uce);
		} catch (Throwable e) {
			String methodName = method == null ? "null" : ((Member) method).getName();
			ExecutorResult failedResult = new ExecutorResult(//
					BasicUtils.getCurrentUseCaseDisplayName(false) + " failure: " + //
							BasicUtils.toDisplayStr(e), Type.ERROR, result, //
					BasicUtils.getLineNum(e, methodName, targetClass), targetClass.getName());
			return postProcessResults(BasicUtils.<ExecutorResult> newList(failedResult, //
					getDefaultFailedExecResult(method, failedResult, targetClass)));
		}
	}

	private void execute(ISINExpression[] exprs, ClassLoader classLoader) throws EvaluatorException, TypeConverterException, NamedInstanceNotFoundException, NamedInstanceException, SecurityException,
			IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

		if (exprs != null) {
			for (ISINExpression expr : exprs) {
				if (expr.getRawSINExpression() == null || expr.getRawSINExpression().length() > 0)
					expr.invoke(expr.getInstance(classLoader), classLoader, DefaultToType.NONE);
			}
		}
	}

	private List<ExecutorResult> postProcessResults(List<ExecutorResult> results) {

		Signature inheritSig = ucModel.getInheritedFromSignature();
		if (inheritSig != null) {
			for (ExecutorResult result : results) {
				result.setDescription(result.getDescription() + getSubUCMarkerPostfix(ucModel));
			}
		}
		return results;
	}

	public static String getSubUCMarkerPostfix(UseCaseModel ucModel) {

		return " (from " + //
				BasicUtils.getSimpleClassName(ucModel.getSignature().getClassName()) + ")";
	}

	private List<ExecutorResult> evaluate(Object execResult, ISINExpression[] expects, ISINExpression[] verifies, ISINExpression[] debug, AccessibleObject method, ClassLoader classLoader, //
			boolean execThrewException, boolean doEvaluate) throws SecurityException, IllegalArgumentException, NoSuchMethodException, EvaluatorException, IllegalAccessException,
			InvocationTargetException, TypeConverterException, NamedInstanceNotFoundException, InstantiationException, NamedInstanceException {

		List<ExecutorResult> executorResults = new ArrayList<ExecutorResult>();
		if (!doEvaluate)
			return getDefaultExecResults(execResult, method, doEvaluate);
		String[] ignoreSINTypeNames = getIgnoreSINTypeNames();
		if (ucModel.getExpectException() != null && !execThrewException) {
			executorResults.add(new ExecutorResult(//
					BasicUtils.getCurrentUseCaseDisplayName(false) + " expected an exception " + //
							" but no exception was thrown", Type.ERROR, execResult));
		}
		if (expects != null) {
			for (ISINExpression expect : expects) {

				Object evalResult;
				if (expect.getRawSINExpression() != null && expect.getRawSINExpression().trim().equals("")) {
					evalResult = true;
				} else
					evalResult = expect.invoke(expect.getInstance(classLoader), classLoader, DefaultToType.RETVAL);

				if (evalResult == null) {
					executorResults.add(new ExecutorResult(BasicUtils.getCurrentUseCaseDisplayName(false) + //
							" invocation of " + expect.toString() + //
							" returned null", Type.ERROR, execResult));
				} else {

					boolean boolResult = ((Boolean) evalResult).booleanValue();
					if (boolResult) {
						if (!expect.toString().equals("")) {
							executorResults.add(new ExecutorResult(BasicUtils.getCurrentUseCaseDisplayName(false) + //
									" evaluation of " + expect.toString() + //
									" returned true", Type.INFO, evalResult));
						}
					} else {
						String associatedMessage = expect.getAssociatedMessage();
						if (associatedMessage == null) {
							associatedMessage = BasicUtils.getCurrentUseCaseDisplayName(false) + //
									" evaluation of " + expect.toString() + " returned false";
						}
						executorResults.add(new ExecutorResult(associatedMessage, Type.ERROR, evalResult));
					}
				}
			}
		}
		if (verifies != null) {
			PersistentDataFactory pdf = PersistentDataFactory.getInstance();
			ExecutorResult[] verifyResults = pdf.getCurrentUCRuntimeStore().getVerifyResults();

			for (int i = 0; i < verifyResults.length; i++) {
				ExecutorResult verifyResult = verifyResults[i];
				if (verifyResult == null) {
					executorResults.add(new ExecutorResult("No invocation matched the verify expression " + //
							verifies[i].getRawSINExpression(), Type.ERROR, null));
				}
			}
		}
		if (debug != null) {
			for (ISINExpression debugExp : debug) {
				Object evalResult = debugExp.invoke(debugExp.getInstance(classLoader), classLoader, DefaultToType.NONE);
				String val;
				if (evalResult == null) {
					val = "null";
				} else {
					try {
						val = TypeConverterFactory.instance.toSINType(evalResult, ignoreSINTypeNames).getRawSINType();
					} catch (TypeConverterException tce) {
						val = evalResult.toString();
					}
				}
				String message = BasicUtils.getCurrentUseCaseDisplayName(false) + //
						" invocation of debug expression " + debugExp.toString() + " returned " + val;
				executorResults.add(new ExecutorResult(message, Type.INFO, evalResult));
			}
		}
		executorResults.addAll(getDefaultExecResults(execResult, method, doEvaluate));
		return executorResults;
	}

	private Class<?> getReturnType(AccessibleObject method) {

		if (method instanceof Method)
			return ((Method) method).getReturnType();
		else if (method instanceof Field)
			return ((Field) method).getType();
		else
			return ((Constructor<?>) method).getDeclaringClass();
	}

	private String[] getIgnoreSINTypeNames() {

		List<String> ignoreSINTypeNamesL = new ArrayList<String>();
		if (ucModel.getName() != null)
			ignoreSINTypeNamesL.add(ucModel.getName());
		if (ucModel.getInstanceout() != null)
			ignoreSINTypeNamesL.add(ucModel.getInstanceout());
		return ignoreSINTypeNamesL.toArray(new String[0]);
	}

	private List<ExecutorResult> getDefaultExecResults(Object execResult, AccessibleObject method, boolean doEvalute) throws TypeConverterException {

		String message;
		if (doEvalute) {
			message = BasicUtils.getCurrentUseCaseDisplayName(false) + //
					": " + BasicUtils.getSimpleClassName(((Member) method).getName()) + " ran successfully";
		} else {
			message = BasicUtils.getCurrentUseCaseDisplayName(false) + //
					": " + BasicUtils.getSimpleClassName(((Member) method).getName()) + " re-executed";
		}
		if (!getReturnType(method).equals(Void.TYPE)) {
			message += " and returned " + (execResult == null ? "null" : //
			TypeConverterFactory.instance.toSINType(execResult, getIgnoreSINTypeNames()).getRawSINType());
		}
		List<ExecutorResult> executorResults = new ArrayList<ExecutorResult>();
		executorResults.add(new ExecutorResult(message, Type.INFO, execResult));
		return executorResults;
	}

	private ExecutorResult getDefaultFailedExecResult(AccessibleObject method, ExecutorResult failedResult, Class<?> failureClass) {

		String classMsg = failureClass.getName().equals(failedResult.getClassName()) ? "" : //
		"in " + failedResult.getClassName();
		StringBuilder message = new StringBuilder(BasicUtils.getCurrentUseCaseDisplayName(false)).append(//
				": execution of ").append(((Member) method).getName()).append(" failed");
		if (failedResult.getErrorLineNum() > -1) {
			message.append(". See error ").append(classMsg).append(" at line ").append(failedResult.getErrorLineNum());
		}
		return new ExecutorResult(message.toString(), Type.WARNING, null);
	}

	private void checkArgs(Object[] args, Method method) throws MethodArgumentException, NamedInstanceException {

		Class<?>[] expectTypes = method.getParameterTypes();
		if (args.length != expectTypes.length) {
			throw new MethodArgumentException(BasicUtils.getCurrentUseCaseDisplayName(false) + //
					": Method \"" + method.toString() + "\" expects " + //
					withNum(expectTypes.length, "arguments") + " but " + withIsOrAre(args.length) + //
					" given in the UseCase");
		}
		for (int i = 0; i < args.length; i++) {
			if (expectTypes[i].isPrimitive() && args[i] == null) {
				throw new MethodArgumentException(BasicUtils.getCurrentUseCaseDisplayName(false) + //
						": Cannot use null for primitive argument " + (i + 1) + //
						" of method \"" + method.toString() + "\"");
			} else if (args[i] != null && !BasicUtils.toNonPrimitiveType(expectTypes[i]).isAssignableFrom(BasicUtils.toNonPrimitiveType(args[i].getClass()))) {
				throw new MethodArgumentException(BasicUtils.getCurrentUseCaseDisplayName(false) + //
						": Given argument type " + args[i].getClass().getName() + //
						"  is not assignable to argument " + (i + 1) + " of method \"" + method.toString() + "\" ");
			} else {
				// Set $arg named instances
				NamedInstanceFactory.getInstance().addNamedInstance(NamedInstanceFactory.ARG_PREFIX + (i + 1), //
						args[i], null);
			}
		}
	}

	private void checkArgs(Object[] args, Constructor<?> constructor) throws MethodArgumentException {

		Class<?>[] expectTypes = constructor.getParameterTypes();
		if (args.length != expectTypes.length) {

			String innerClassWarn = "";
			if (BasicUtils.isNonStaticInnerClass(constructor.getDeclaringClass())) {
				innerClassWarn = //
				".  Note UseCases defined on non-static inner class constructors must specify the enclosing " + //
						"class instance as the first arg, e.g. @UseCase(args={\"namedInstanceFromEnclosingClassUseCase\", ...}";
			}
			throw new MethodArgumentException(BasicUtils.getCurrentUseCaseDisplayName(false) + //
					": Constructor \"" + constructor.toString() + "\" expects " + //
					withNum(expectTypes.length, "arguments") + " but " + withIsOrAre(args.length) + //
					" given in the UseCase" + innerClassWarn);
		}
		for (int i = 0; i < args.length; i++) {
			if (expectTypes[i].isPrimitive() && args[i] == null) {
				throw new MethodArgumentException(BasicUtils.getCurrentUseCaseDisplayName(false) + //
						": Cannot use null for primitive argument " + (i + 1) + //
						" of contructor \"" + constructor.toString() + "\"");
			}
			if (args[i] != null && !BasicUtils.toNonPrimitiveType(expectTypes[i]).isAssignableFrom(BasicUtils.toNonPrimitiveType(args[i].getClass()))) {
				throw new MethodArgumentException(BasicUtils.getCurrentUseCaseDisplayName(false) + //
						": Given argument type " + args[i].getClass().getName() + //
						" is not assignable to argument " + (i + 1) + " of constructor \"" + constructor.toString() + "\"");
			}
		}
	}

	private String withNum(int num, String str) {

		return num + " " + (num == 1 ? str.substring(0, str.length() - 1) : str);
	}

	private String withIsOrAre(int num) {

		if (num == 0)
			return "none are";
		else if (num == 1)
			return "1 is";
		else
			return num + " are";
	}

	private enum ExecutePhase {

		BEFORE("[before] "), EXEC(""), EVAL("[expect] "), AFTER("[after] ");

		private final String msg;

		private ExecutePhase(String msg) {

			this.msg = msg;
		}

		public String getMessage() {

			return msg;
		}
	}

	private class ExecutePhaseTracker {

		private ExecutePhase phase = ExecutePhase.BEFORE;
	}
}
