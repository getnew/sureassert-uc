/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.exception;

public class BaseSAException extends Exception implements ExtendedMessageException {

	private static final long serialVersionUID = 1L;

	private String appendMessage;

	private String prefixMessage;

	public BaseSAException() {

		super();
	}

	public BaseSAException(String message, Throwable cause) {

		super(message, cause);
	}

	public BaseSAException(String message) {

		super(message);
	}

	public void setAppendMessage(String appendMessage) {

		this.appendMessage = appendMessage;
	}

	public String getAppendMessage() {

		return appendMessage;
	}

	public void setPrefixMessage(String prefixMessage) {

		this.prefixMessage = prefixMessage;
	}

	public String getPrefixMessage() {

		return prefixMessage;
	}
}
