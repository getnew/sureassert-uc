/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.propertyfile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sureassert.uc.runtime.TypeConverter;
import com.sureassert.uc.runtime.TypeConverterFactory;

public class PropertyFile {

	public static final String DEFAULT_PROPERTY_FILENAME = "sauc.properties";

	private final Properties props;

	private final List<String> failedClasses = new ArrayList<String>();

	public PropertyFile(File propFile) {

		props = new Properties();
		if (propFile.exists()) {
			InputStream in = null;
			try {
				in = new BufferedInputStream(new FileInputStream(propFile));
				props.load(in);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	public void loadExtensions(ClassLoader classLoader) {

		for (Object className : props.keySet()) {
			try {
				Class<?> clazz = classLoader.loadClass((String) className);
				if (TypeConverter.class.isAssignableFrom(clazz)) {
					TypeConverter<?> tc = (TypeConverter<?>) clazz.newInstance();
					TypeConverterFactory.instance.registerTypeConverter(tc);
				}

			} catch (Exception e) {
				failedClasses.add((String) className);
			}
		}
	}
}
