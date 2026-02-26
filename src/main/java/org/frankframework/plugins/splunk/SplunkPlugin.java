package org.frankframework.plugins.splunk;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.pf4j.Plugin;

public class SplunkPlugin extends Plugin {

	private static final String LOG4J2_CONFIGURATION_FILE_PROPERTY_NAME = "log4j.configurationFile";
	private static final String SPLUNK_CONFIG_FILE = "log4j2-to-splunk.xml";
	private boolean appendedConfigFile = false;

	public SplunkPlugin() {
		// Loaded as Bean, so it needs an empty constructor
	}

	@Override
	public void start() {
		String log4jConfigFile = PropertiesUtil.getProperties().getStringProperty(LOG4J2_CONFIGURATION_FILE_PROPERTY_NAME);
		if (log4jConfigFile == null) {
			log.warn("No log4j.configurationFile property found, cannot add sentry configuration");
			return;
		}

		if (!log4jConfigFile.contains(SPLUNK_CONFIG_FILE)) {
			log.info("Did not find Splunk plugin in: {}, adding: [,{}]", log4jConfigFile, SPLUNK_CONFIG_FILE);
			log4jConfigFile += "," + SPLUNK_CONFIG_FILE;
			System.setProperty(LOG4J2_CONFIGURATION_FILE_PROPERTY_NAME, log4jConfigFile);
			appendedConfigFile = true;
		}

		// Get the plugin's classloader for Log4j2 to find the config file
		ClassLoader pluginCL = this.getClass().getClassLoader();
		ClassLoader originalCL = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(pluginCL);

			// Clear Log4j2's plugin cache so it rescans
			PluginRegistry.getInstance().clear();

			// Reconfigure
			LoggerContext ctx = LoggerContext.getContext(false);
			ctx.reconfigure();

			log.info("Log4j2 reconfigured with Splunk appender");

		} finally {
			Thread.currentThread().setContextClassLoader(originalCL);
		}
	}

	@Override
	public void stop() {
		if (appendedConfigFile) {
			System.clearProperty(LOG4J2_CONFIGURATION_FILE_PROPERTY_NAME);
		}
	}
}
