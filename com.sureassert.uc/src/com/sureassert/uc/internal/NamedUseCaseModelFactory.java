package com.sureassert.uc.internal;

import java.lang.reflect.AccessibleObject;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.MultiUseCase;
import org.sureassert.uc.annotation.UseCase;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.evaluator.model.ModelFactory;
import com.sureassert.uc.evaluator.model.MultiUseCaseModel;
import com.sureassert.uc.interceptor.SourceModel;
import com.sureassert.uc.runtime.Signature;
import com.sureassert.uc.runtime.SignatureTableFactory;
import com.sureassert.uc.runtime.exception.SARuntimeException;
import com.sureassert.uc.runtime.model.UseCaseModel;
import com.sureassert.uc.runtime.model.UseCaseModel.NamedUseCaseModelFactoryI;

public class NamedUseCaseModelFactory implements NamedUseCaseModelFactoryI {

	private final IType javaType;
	private final SourceModel sourceModel;
	private final ClassLoader classLoader;
	private final SourceFile sourceFile;

	// Cache ucName to UseCaseModel
	private Map<String, UseCaseModel> ucModelStore = null;

	/**
	 * Creates a NamedUseCaseModelFactory. This should be created prior to iterating methods in a
	 * type as it caches a map of UseCase names to models for better performance.
	 * Its scope should not be longer than the method iteration operation.
	 * 
	 * @param javaType
	 * @param sourceModel
	 * @param classLoader
	 * @param sourceFile
	 */
	public NamedUseCaseModelFactory(IType javaType, SourceModel sourceModel, ClassLoader classLoader, SourceFile sourceFile) {

		this.javaType = javaType;
		this.sourceModel = sourceModel;
		this.classLoader = classLoader;
		this.sourceFile = sourceFile;
	}

	public synchronized UseCaseModel getUCModel(String ucName) {

		if (ucModelStore == null) {
			// Find and store all named UseCases in this type
			initUCModelStore();
		}
		return ucModelStore.get(ucName);
	}

	private void initUCModelStore() {

		ucModelStore = new HashMap<String, UseCaseModel>();
		try {
			IMethod[] methods = javaType.getMethods();
			for (int methodIndex = 0; methodIndex < methods.length; methodIndex++) {

				IMethod iMethod = methods[methodIndex];
				Map<String, IAnnotation> annotations = EclipseUtils.getAnnotations(iMethod);

				AccessibleObject method = sourceModel.getMethod(iMethod, classLoader);
				Signature thisUCSig = SignatureTableFactory.instance.getSignature(method);

				// Try to get Exemplar/UseCase
				boolean definedAsUseCase = false;
				IAnnotation ucAnnotation = annotations.get(Exemplar.class.getSimpleName());
				if (ucAnnotation == null || !ucAnnotation.exists()) {
					ucAnnotation = annotations.get(UseCase.class.getSimpleName());
					if (ucAnnotation != null)
						definedAsUseCase = true;
				}
				if (ucAnnotation != null && ucAnnotation.exists()) {

					UseCaseModel ucModel = ModelFactory.newUseCaseModel(ucAnnotation.getMemberValuePairs(), //
							thisUCSig, sourceFile.getLineNum(ucAnnotation.getNameRange().getOffset()), //
							null, EclipseUtils.isUseCaseAn(ucAnnotation), this);
					if (ucModel.getName() != null && ucModel.getName().length() > 0)
						ucModelStore.put(ucModel.getName(), ucModel);
				} else {

					IAnnotation mucAnnotation = annotations.get(Exemplars.class.getSimpleName());
					if (mucAnnotation == null || !mucAnnotation.exists()) {
						mucAnnotation = annotations.get(MultiUseCase.class.getSimpleName());
						if (mucAnnotation != null)
							definedAsUseCase = true;
					}
					if (mucAnnotation != null && mucAnnotation.exists()) {
						int lineNum = sourceFile.getLineNum(mucAnnotation.getNameRange().getOffset());
						MultiUseCaseModel mucModel = new MultiUseCaseModel(mucAnnotation.getMemberValuePairs(), //
								thisUCSig, lineNum, sourceFile, null, definedAsUseCase, this);
						for (UseCaseModel ucModel : mucModel.getUseCases()) {
							if (ucModel.getName() != null && ucModel.getName().length() > 0)
								ucModelStore.put(ucModel.getName(), ucModel);
						}
					}
				}
			} // end method loop

		} catch (Exception e) {
			throw new SARuntimeException(e);
		}
	}
}
