/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.wearefrank;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;

import nl.nn.adapterframework.util.StringResolver;

@Plugin(name = "ff", category = StrLookup.CATEGORY)
public class FrankPropertyLookupProvider extends AbstractLookup {
	private static final String LOG4J_PROPS_FILE = "log4j4ibis.properties";
	private static final String DS_PROPERTIES_FILE = "DeploymentSpecifics.properties";

	private final Properties properties;

	public FrankPropertyLookupProvider() throws IOException {
		properties = getProperties();
	}

	@Override
	public String lookup(LogEvent ignored, String key) { // Always ignore the event
		String value = properties.getProperty(key);

		if(StringUtils.isEmpty(value)) {
			return "";
		}

		if(StringResolver.needsResolution(value)) {
			value = StringResolver.substVars(value, properties);
		}
		return value;
	}

	@Nonnull
	private Properties getProperties() throws IOException {
		Properties log4jProperties = getParseProperties(LOG4J_PROPS_FILE);
		if(log4jProperties == null) {
			log4jProperties = new Properties();
		}

		Properties dsProperties = getParseProperties(DS_PROPERTIES_FILE);
		if (dsProperties != null) {
			log4jProperties.putAll(dsProperties);
		}

		log4jProperties.putAll(System.getProperties()); //Set these after reading DeploymentSpecifics as we want to override the properties
		log4jProperties.putAll(System.getenv()); // let environment properties override system properties and appConstants
		setInstanceNameLc(log4jProperties); //Set instance.name.lc for log file names

		return log4jProperties;
	}

	private @Nullable Properties getParseProperties(String filename) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> urls = cl.getResources(filename);
		URL url = null;
		while (urls.hasMoreElements()) {
			url = urls.nextElement();
		}

		if(url != null) {
			Properties properties = new Properties();
			try(InputStream is = url.openStream(); Reader reader = getCharsetDetectingInputStreamReader(is)) {
				properties.load(reader);
			}
			return properties;
		}
		return null;
	}

	/* May be duplicate code, but the LogFactory may not depend on any class that uses a logger. */
	private static Reader getCharsetDetectingInputStreamReader(InputStream inputStream) throws IOException {
		BOMInputStream bOMInputStream = BOMInputStream.builder()
				.setInputStream(inputStream)
				.setByteOrderMarks(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE)
				.get();

		ByteOrderMark bom = bOMInputStream.getBOM();
		String charsetName = bom == null ? StandardCharsets.UTF_8.displayName() : bom.getCharsetName();

		return new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);
	}

	private static void setInstanceNameLc(Properties log4jProperties) {
		String instanceNameLowerCase = log4jProperties.getProperty("instance.name");
		if (instanceNameLowerCase != null) {
			instanceNameLowerCase = instanceNameLowerCase.toLowerCase();
		} else {
			instanceNameLowerCase = "ibis";
		}
		log4jProperties.setProperty("instance.name.lc", instanceNameLowerCase);
	}
}
