/**
 * Marmoset: a student project snapshot, submission, testing and code review
 * system developed by the Univ. of Maryland, College Park
 * 
 * Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 * by William Pugh. See http://marmoset.cs.umd.edu/
 * 
 * Copyright 2005 - 2011, Univ. of Maryland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/*
 * Created on Sep 4, 2004
 */
package edu.umd.cs.buildServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.WillClose;

/**
 * Object to load, store, and retrieve configuration properties. Note that
 * properties not found through the usual file-based mechanism will be searched
 * for in the Java system properties.
 * 
 * @author David Hovemeyer
 */
public class Configuration {
	private static final boolean DEBUG = Boolean
			.getBoolean("buildServer.config.debug");
	private static final Pattern SUBST_PATTERN = Pattern
			.compile("\\$\\{[A-Za-z.]+\\}");

	private Properties properties;

	/**
	 * Constructor. Configuration will be empty.
	 */
	public Configuration() {
		this.properties = new Properties();
	}

	public @Override String toString() {
		return properties.toString();
	}
	

	
	/**
	 * Set a configuration property
	 * 
	 * @param key
	 *            the name of the property
	 * @param value
	 *            the value of the property
	 */
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	/**
	 * Check if this configuration has a certain property set.
	 * 
	 * @param key
	 *            The key of the property.
	 * @return True if there is a property with the given key; false otherwise.
	 */
	public boolean hasProperty(String key) {
		return getOptionalProperty(key) != null;
	}

	/**
	 * Get a string-valued required configuration property.
	 * 
	 * @param key
	 *            name of the property
	 * @return value of the property
	 * @throws MissingConfigurationPropertyException
	 *             if the property is not defined
	 */
	public String getRequiredProperty(String key)
			throws MissingConfigurationPropertyException {
		String value = getOptionalProperty(key);
		if (value == null) {
			if (key.startsWith(ConfigurationKeys.DEBUG_PFX))
				// Debug properties need not be specified, and they default to
				// false
				return "false";
			else
				throw new MissingConfigurationPropertyException(
						"Missing configuration property: " + key);
		}
		return value;
	}

	/**
	 * Get a string-valued configuration property.
	 * 
	 * @param key
	 *            Name of the property.
	 * @param defaultValue
	 *            Default value for the property if key is unbound.
	 * @return The value bound to the given key; the defaultValue if the key is
	 *         unbound.
	 */
	public String getStringProperty(String key, String defaultValue) {
		String value = getOptionalProperty(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * Get an optional string-valued configuration property. Note that if the
	 * property was not defined in the configuration properties file, the Java
	 * system properties are searched.
	 * 
	 * @param key
	 *            name of the property
	 * @return value of the property, or null if the property is not defined
	 */
	public @CheckForNull
	String getOptionalProperty(String key) {
		String value = getProperty(properties, key);
		if (value == null)
			return null;

		// Expand referenced properties
		final int MAX_ITERS = 20;
		for (int i = 0; i < MAX_ITERS; ++i) {
			Matcher m = SUBST_PATTERN.matcher(value);

			int numFound = 0;
			StringBuffer buf = new StringBuffer();
			while (m.find()) {
				++numFound;
				String prop = m.group(0);
				prop = prop.substring(2);
				prop = prop.substring(0, prop.length() - 1);
				String replacement = expandPropertyOnce(properties, prop);
				m.appendReplacement(buf, replacement.replace("$", "\\$"));
			}
			m.appendTail(buf);

			value = buf.toString();

			if (numFound == 0)
				break;
		}

		return value;
	}

	/**
	 * Get a boolean configuration property.
	 * 
	 * @param key
	 *            name of the property
	 * @return boolean value of the property
	 * @throws MissingConfigurationPropertyException
	 */
	public boolean getBooleanProperty(String key)
			throws MissingConfigurationPropertyException {
		return Boolean.valueOf(getRequiredProperty(key)).booleanValue();
	}

	/**
	 * Get optional boolean property. If not defined, returns false.
	 * 
	 * @param key
	 *            name of the property
	 * @return the value of the property, or false if the property is not
	 *         defined
	 */
	public boolean getOptionalBooleanProperty(String key) {
		String value = getOptionalProperty(key);
		if (value == null)
			return false;
		return Boolean.valueOf(value).booleanValue();
	}

	/**
	 * Get optional integer property.
	 * 
	 * @param key
	 *            name of the property
	 * @param defaultValue
	 *            value to return if property is not defined
	 * @return the value of the property (or the default value if the property
	 *         is not defined)
	 */
	public int getOptionalIntegerProperty(String key, int defaultValue) {
		int result;
		String value = getOptionalProperty(key);
		if (value == null) {
			result = defaultValue;
		} else {
			try {
				result = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				result = defaultValue;
			}
		}
		return result;
	}

	/**
	 * Get the value of a debug boolean property. The names of debug properties
	 * must start with "debug.".
	 * 
	 * @param key
	 *            name of the debug property
	 * @return boolean value of the debug property
	 */
	public boolean getDebugProperty(String key) {
		if (!key.startsWith("debug."))
			throw new IllegalArgumentException("Key " + key
					+ " is not a debug property");

		return getOptionalBooleanProperty(key);
	}

	/**
	 * Load configuration properties from an input stream.
	 * 
	 * @param in
	 *            the InputStream
	 * @throws IOException
	 */
	public void load(@WillClose InputStream in) throws IOException {
		properties.load(in);
		in.close();
	}

	/**
	 * Get a property from the given Properties object, or system properties if
	 * it is not defined in the Properties object.
	 * 
	 * @param properties
	 *            the Properties object
	 * @param prop
	 *            name of property
	 * @return value of the property, or null if it is defined in neither the
	 *         Properties object nor the system properties
	 */
	private static String getProperty(Properties properties, String prop) {
		// First, use existing property value
		String value = properties.getProperty(prop);

		// Next, try a system property
		if (value == null)
			value = System.getProperty(prop);

		return value;
	}

	private static String expandPropertyOnce(Properties properties, String prop) {
		if (DEBUG)
			System.out.println("Expanding " + prop);

		String replacement = getProperty(properties, prop);

		// If undefined, just expand to an empty string
		if (replacement == null)
			replacement = "";

		if (DEBUG)
			System.out.println("\t==> " + replacement);

		return replacement;
	}

	/**
	 * Save configuration properties to an output stream.
	 * 
	 * @param out
	 *            the OutputStream
	 * @throws IOException
	 */
	public void store(OutputStream out) throws IOException {
		properties.store(out, "");
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + Configuration.class.getName()
					+ ": <config filename>");
			System.exit(1);
		}

		String filename = args[0];

		Configuration config = new Configuration();
		config.load(new FileInputStream(filename));

		for (Iterator<?> i = config.properties.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			System.out.println(key + "=" + config.getRequiredProperty(key));
		}
	}
}
