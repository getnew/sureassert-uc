/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;


public interface ErrorContainingModel {

	Signature getSignature();

	void setError(String error);
}
