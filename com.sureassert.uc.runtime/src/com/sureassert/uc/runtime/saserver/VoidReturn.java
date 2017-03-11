package com.sureassert.uc.runtime.saserver;

import java.io.Serializable;

/**
 * Special return type for SAServerMessage indicating a message has no return value
 * and that the caller does not need to block.
 * 
 * @author Nathan Dolan
 * 
 */
public class VoidReturn implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final VoidReturn INSTANCE = new VoidReturn();
}
