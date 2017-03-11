package com.sureassert.uc.runtime;

import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.weaver.loadtime.DefaultMessageHandler;

public class SAUCAspectJMessageHandler extends DefaultMessageHandler {

	@Override
	public boolean handleMessage(IMessage message) throws AbortException {

		if (isIgnoring(message.getKind())) {
			return false;
		}

		if (message.getMessage().startsWith("can't throw checked exception 'java.lang.Exception' at this join point"))
			return false;
		else
			return SYSTEM_ERR.handleMessage(message);
	}

	@Override
	public boolean isIgnoring(Kind paramKind) {

		return super.isIgnoring(paramKind);
	}

	@Override
	public void dontIgnore(Kind paramKind) {

		super.dontIgnore(paramKind);
	}

	@Override
	public void ignore(Kind paramKind) {

		super.ignore(paramKind);
	}

}
