package com.sureassert.uc.license;

public class InvalidLicenseKeyException extends Exception {

	private static final long serialVersionUID = 1L;

	public enum ErrorType {
		NULL, INVALID_LENGTH, INVALID_CHAR, CHECK_BIT_FAILURE, INVALID_CREATED_ON, INVALID_VERSION, INVALID_EMAIL
	}

	private final ErrorType errorType;

	public InvalidLicenseKeyException(ErrorType type, String message) {

		super(message);
		errorType = type;
	}

	public ErrorType getErrorType() {

		return errorType;
	}
}
