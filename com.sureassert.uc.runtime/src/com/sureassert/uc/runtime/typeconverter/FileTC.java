/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;

public class FileTC extends AbstractTypeConverter<File> {

	public Class<File> getType() {

		return File.class;
	}

	public String getPrefixID() {

		return "f";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return StringTC.PREFIX + ":'" + value.toString() + "'";
	}

	public File toInstance(SINType sinType, ClassLoader classLoader) {

		File file = new File(PersistentDataFactory.getInstance().getCurrentProjectFilePath(), sinType.getSINValue());
		return file;
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}

}
