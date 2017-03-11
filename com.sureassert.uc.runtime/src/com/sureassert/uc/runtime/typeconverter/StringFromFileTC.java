/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime.typeconverter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.SINType;
import com.sureassert.uc.runtime.TypeConverterException;
import com.sureassert.uc.runtime.exception.SARuntimeException;

public class StringFromFileTC extends AbstractTypeConverter<String> {

	public Class<String> getType() {

		return String.class;
	}

	public String getPrefixID() {

		return "sf";
	}

	@Override
	public String toRawSIN(Object value) throws TypeConverterException {

		return StringTC.PREFIX + ":'" + value.toString() + "'";
	}

	public String toInstance(SINType sinType, ClassLoader classLoader) {

		try {
			File file = new File(PersistentDataFactory.getInstance().getCurrentProjectFilePath(), //
					sinType.getEscaper().toRaw(sinType.getSINValue()));
			return FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e) {
			throw new SARuntimeException(e);
		}
	}

	@Override
	protected List<SINType> getChildSINs(SINType sinType) throws TypeConverterException {

		return Collections.emptyList();
	}
}
