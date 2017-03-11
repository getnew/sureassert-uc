package com.sureassert.uc.builder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import com.sureassert.uc.runtime.Signature;

public class SerializableProjectProcessEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String javaProjectName;

	private final Map<String, Set<Signature>> affectedFileSigs;

	public SerializableProjectProcessEntity(ProjectProcessEntity ppe) {

		javaProjectName = ppe.getJavaProject().getProject().getName();

		if (ppe.getAffectedFileSigs() != null) {
			affectedFileSigs = new HashMap<String, Set<Signature>>();
			for (Entry<IPath, Set<Signature>> entry : ppe.getAffectedFileSigs().entrySet()) {
				affectedFileSigs.put(entry.getKey().toString(), entry.getValue());
			}
		} else {
			affectedFileSigs = null;
		}
	}

	public String getJavaProjectName() {

		return javaProjectName;
	}

	public Map<String, Set<Signature>> getAffectedFileSigs() {

		return affectedFileSigs;
	}

}
