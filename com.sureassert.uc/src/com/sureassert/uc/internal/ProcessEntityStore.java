/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

import com.sureassert.uc.runtime.BasicUtils;

public class ProcessEntityStore {

	Map<IJavaProject, Set<ProcessEntity>> peByProject = new HashMap<IJavaProject, Set<ProcessEntity>>();
	Map<String, ProcessEntity> peByClassName = new HashMap<String, ProcessEntity>();

	public ProcessEntity addProcessEntity(IJavaProject javaProject, ProcessEntity processEntity) {

		ProcessEntity storedPE = peByClassName.get(processEntity.getJavaType().getFullyQualifiedName());
		if (storedPE != null) {
			// Add the given process entity sigs to the existing process entity
			storedPE.addProcessSigs(processEntity);
			return storedPE;
		} else {
			BasicUtils.mapSetAdd(peByProject, javaProject, processEntity);
			peByClassName.put(processEntity.getJavaType().getFullyQualifiedName(), processEntity);
			return processEntity;
		}
	}
}