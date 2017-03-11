/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.exception;

public class SARuntimeException extends RuntimeException implements ExtendedMessageException {

	private static final long serialVersionUID = 1L;

	private String appendMessage;
	private String prefixMessage;

	public SARuntimeException() {

		super();
	}

	public SARuntimeException(String message, Throwable cause) {

		super(message, cause);
	}

	public SARuntimeException(String message) {

		super(message);
	}

	public SARuntimeException(Throwable cause) {

		super(cause);
	}

	public boolean displayExceptionName() {

		return false;
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
