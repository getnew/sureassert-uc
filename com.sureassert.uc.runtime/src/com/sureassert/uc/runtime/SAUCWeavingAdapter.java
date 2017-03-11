package com.sureassert.uc.runtime;

import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;

public class SAUCWeavingAdapter extends ClassLoaderWeavingAdaptor {

	@Override
	protected void createMessageHandler() {

		super.createMessageHandler();
		setMessageHandler(new SAUCAspectJMessageHandler());
	}
}
