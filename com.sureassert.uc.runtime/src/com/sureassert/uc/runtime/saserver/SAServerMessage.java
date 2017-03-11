package com.sureassert.uc.runtime.saserver;

import java.io.Serializable;

public interface SAServerMessage<R extends Serializable> extends Serializable {

	/**
	 * Executes this message
	 * 
	 * @return A return value or null.
	 */
	public R execute();

	/**
	 * Gets the return type of this message.
	 * 
	 * @return
	 */
	public Class<R> getReturnType();
}
