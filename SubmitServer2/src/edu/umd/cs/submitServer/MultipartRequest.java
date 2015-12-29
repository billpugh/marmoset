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
 * Created on Jan 13, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.utilities.MarmosetPatterns;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;
import edu.umd.cs.marmoset.utilities.XSSScrubber;

/**
 * @author jspacco
 *
 *        
 */
@CheckReturnValue
public class MultipartRequest {
	private LinkedList<FileItem> fileItems = new LinkedList<FileItem>();
	private Map<String, Object> parameters = new HashMap<String, Object>();
	private final Logger logger;
	private final boolean strictChecking;

	public MultipartRequest(Logger logger, boolean strictChecking) {
		this.logger = logger;
		this.strictChecking = strictChecking;
	}

	public static MultipartRequest parseRequest(HttpServletRequest request,
			int maxSize, Logger logger, boolean strictChecking, ServletContext servletContext)
			throws IOException, ServletException {

		DiskFileItemFactory factory = getFactory(servletContext);
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setSizeMax(maxSize);
		MultipartRequest multipartRequest = new MultipartRequest(logger,
                strictChecking);
		try {
			// Parse the request
			List<FileItem> items = upload.parseRequest(request);

			
			for (FileItem item : items) {

				if (item.isFormField()) {
					multipartRequest.setParameter(item.getFieldName(),
							item.getString());
				} else {
					multipartRequest.addFileItem(item);
				}
			}
			return multipartRequest;
		} catch (FileUploadBase.SizeLimitExceededException e) {
			Debug.error("File upload is too big " +  e.getActualSize() + " > " + e.getPermittedSize());
			Debug.error("upload info: " + multipartRequest);
			throw new ServletException(e);
		} catch (FileUploadException e) {
			Debug.error("FileUploadException: " + e);
			throw new ServletException(e);
		}
	}

	/**
	 * @return
	 */
	private static DiskFileItemFactory getFactory(ServletContext servletContext) {
		DiskFileItemFactory factory =  new DiskFileItemFactory();
		
		FileCleaningTracker fileCleaningTracker
        = FileCleanerCleanup.getFileCleaningTracker(servletContext);
		factory.setFileCleaningTracker(fileCleaningTracker);
		return factory;
	}

	public void setParameter(String key, Object value) {
		parameters.put(key, value);
	}

	/**
	 * Special call to get the "password" parameter. This is split into a
	 * separate call to distinguish it from a call to getParameter(), which we
	 * try to avoid calling since it doesn't vet the parameter before returning.
	 * Presumably it's OK not to vet passwords!
	 *
	 * @return the value of the "password" parameter
	 * @throws InvalidRequiredParameterException
	 *             if parameter is not specified
	 */
	public String getPasswordParameter()
			throws InvalidRequiredParameterException {
		return getStringParameter("password");
	}

	/**
	 * Special call to get the "password" parameter, or null if the password
	 * parameter is not specified. This is split into a separate call to
	 * distinguish it from a call to getParameter(), which we try to avoid
	 * calling since it doesn't vet the parameter before returning. Presumably
	 * it's OK not to vet passwords!
	 *
	 * @return the value of the "password" parameter, or null if it's not
	 *         specified
	 */
	public String getOptionalPasswordParameter() {
		return getParameter("password");
	}

	/**
	 * Get the (String) value of the given parameter. Throw an exception if it
	 * wasn't specified.
	 *
	 * @return the value of the given parameter
	 * @throws InvalidRequiredParameterException
	 *             if parameter is not specified
	 */
	public String getStringParameter(String name)
			throws InvalidRequiredParameterException {
		String param = (String) parameters.get(name);
		if (param == null) 
		    throw new InvalidRequiredParameterException(name
                    + " is a required parameter and it was not present");
		if ( param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter and it was empty");
		}
		return param;
	}

	/**
	 * Get the (String) value of the given parameter or null if it isn't
	 * specified
	 *
	 * @return the value of the given parameter, or null if unspecified
	 */
	 public @CheckForNull String getOptionalStringParameter(String name) {
	   return getOptionalStringParameter(name, null);
	 }

	 public @CheckForNull String getOptionalStringParameter(String name, String defaultValue) {
		String param = (String) parameters.get(name);
		if (param == null || param.equals("")) {
			return defaultValue;
		}
		return param;
	}
	
	public int getOptionalIntParameter(String name, int defaultValue) {
	  String param = (String) parameters.get(name);
    if (param == null || param.equals("")) {
      return defaultValue;
    }
    return Integer.parseInt(param);
	}

	/**
	 * Return the parameter's value if matched by the provided regex pattern. If
	 * the parameter is not specified or if it does not match the provided
	 * regex, null is returned.
	 *
	 * @param name
	 *            name of the parameter to retrieve
	 * @param p
	 *            pattern that must match a valid parameter
	 * @return the parameter if it specified and valid, null otherwise
	 */
	private @CheckForNull String getOptionalRegexParameter(String name, Pattern p) {
		String s = getParameter(name);
		if (s != null) {
			s = s.trim();
			if (p.matcher(s).matches()) {
				return s;
			}
			String scrubbed = XSSScrubber.scrubbedStr(s);
			logger.error("Param \"" + name + "\" value \"" + scrubbed
					+ "\" doesn't match regex filter " + p.toString());
			if (strictChecking) {
				throw new IllegalArgumentException(name
						+ " was malformed according to regular expression");
			} else {
				return scrubbed;
			}
		}
		return null;
	}

	/**
	 * Return the parameter's value if matched by the provided regex pattern. If
	 * the parameter is not specified or if it does not match the provided
	 * regex, null is returned.
	 *
	 * @param name
	 *            name of the parameter to retrieve
	 * @param p
	 *            pattern that must match a valid parameter
	 * @return the parameter if it specified and valid, null otherwise
	 */
	private String getRegexParameter(String name, Pattern p)
			throws InvalidRequiredParameterException {
		String s = getParameter(name);
		if (s != null) {
			if (p.matcher(s).matches()) {
				return s;
			}
			String scrubbed = XSSScrubber.scrubbedStr(s);
			logger.error("Param \"" + name + "\" value \"" + scrubbed
					+ "\" doesn't match regex filter " + p.toString());
			if (strictChecking) {
				throw new IllegalArgumentException(name
						+ " was malformed according to regular expression");
			} else {
				return scrubbed;
			}
		}
		throw new InvalidRequiredParameterException(name
				+ " is a required parameter but was not specified");
	}

	/**
	 * Get the string value of the given parameter, which has been passed
	 * through a regular-expression filter or, if no filter was defined for that
	 * parameter, has had its angle brackets escaped. Throws
	 * InvalidRequiredParameterException if the parameter is not defined.
	 *
	 * @param name
	 *            name of parameter to get
	 * @return the parameter
	 * @throws InvalidRequiredParameterException
	 */
	public String getCheckedParameter(String name)
			throws InvalidRequiredParameterException {
		Pattern p = MarmosetPatterns.getPattern(name);
		if (p != null) {
			return getRegexParameter(name, p);
		} else {
			return getScrubbedParameter(name);
		}
	}

	/**
	 * Get the string value of the given parameter, which has been passed
	 * through a regular-expression filter or, if no filter was defined for that
	 * parameter, has had its angle brackets escaped. Returns null if the
	 * parameter is not defined or is malformed according to the regular
	 * expression.
	 *
	 * @param name
	 *            name of parameter to get
	 * @return the filtered or scrubbed parameter, or null if it was undefined
	 *         or malformed
	 */
	public String getOptionalCheckedParameter(String name) {
		Pattern p = MarmosetPatterns.getPattern(name);
		if (p != null) {
			return getOptionalRegexParameter(name, p);
		} else {
			return getOptionalScrubbedParameter(name);
		}
	}

	/**
	 * Get the (String) value of the given parameter, scrubbed so that angle
	 * brackets are escaped to avoid an HTML script injection. Throw an
	 * exception if parameter isn't specified.
	 *
	 * @return the value of the given parameter with angle brackets escaped
	 * @throws InvalidRequiredParameterException
	 *             if parameter is not specified
	 */
	private String getScrubbedParameter(String name)
			throws InvalidRequiredParameterException {
		String param = (String) parameters.get(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter and it was " + param);
		}
		return XSSScrubber.scrubbedStr(param);
	}

	/**
	 * Get the (String) value of the given parameter, scrubbed so that angle
	 * brackets are escaped to avoid an HTML script injection. Return null if
	 * parameter isn't specified.
	 *
	 * @return the value of the given parameter with angle brackets escaped, or
	 *         null if parameter isn't specified
	 */
	private String getOptionalScrubbedParameter(String name) {
		String param = (String) parameters.get(name);
		if (param == null)
			return null;
		param = param.trim();
		if (param.equals("")) {
			return null;
		}
		return XSSScrubber.scrubbedStr(param);
	}

	public boolean getOptionalBooleanParameter(String name) {
		String param = (String) parameters.get(name);
		if (param == null)
			return false;
		return MarmosetUtilities.isTrue(param);
	}

	/**
	 * @return Returns the fileItem.
	 */
	public Collection<FileItem> getFileItems() {
		return fileItems;
	}
	
	public FileItem getFileItem() {
		if (fileItems.size() != 1)
			throw new IllegalStateException("Have " + fileItems.size() + " file uploads");
		return fileItems.element();
		
	}

	/**
	 * @param fileItem
	 *            The fileItem to set.
	 */
	public void addFileItem(FileItem fileItem) {
		this.fileItems.add(fileItem);
	}

	/**
	 * Finds the value mapped to by the given key. Can return null or the empty
	 * string.
	 *
	 * @param key
	 *            the key
	 * @return the value mapped to by the given key. Will return null if the key
	 *         is unmapped.
	 */
	public String getParameter(String key) {
		return (String) parameters.get(key);
	}

	public String getParameter(String key, String defaultValue) {
		if (parameters.containsKey(key))
			return (String) parameters.get(key);
		return defaultValue;
	}

	/**
	 * Returns the value mapped to by the given key as a boolean. boolean true
	 * is represented by 'yes' or 'true' (case-insensitively) while false is
	 * anything else. The value string cannot be null or empty.
	 *
	 * @param key
	 *            the key
	 * @return true if the key maps to a true value (where true is 'yes' or
	 *         'true' case-insensitively); false otherwise
	 */
	public boolean getBooleanParameter(String key)
			throws InvalidRequiredParameterException {
		String value = getStringParameter(key);
		value = value.toUpperCase();
		if (value.equals("YES") || value.equals("TRUE"))
			return true;
		return false;
	}

	/**
	 * @param string
	 *            name of the parameter
	 * @return
	 */
	public int getIntParameter(String key)
			throws InvalidRequiredParameterException {
		try {
			String value = getStringParameter(key);
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new InvalidRequiredParameterException(e.getMessage());
		}
	}

	/**
	 * @param string
	 *            name of the parameter
	 * @return
	 */
	public int getIntParameter(String key, int def) {
		if (!parameters.containsKey(key)) {
			return def;
		}
		try {
			String value = (String) parameters.get(key);
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * Return the value of the parameter parsed as an Integer, or return the
	 * given default value if the parameter isn't specified or cannot be parsed.
	 *
	 * @param name
	 *            name of parameter to get
	 * @param def
	 *            default to return if parameter is either unspecified or
	 *            doesn't parse
	 * @return Integer value of the parameter when it's specified and parseable,
	 *         or the default 'def' if the parameter is unspecified or
	 *         unparseable
	 */
	public Integer getIntegerParameter(String name, Integer def) {
		String param = (String) parameters.get(name);
		if (param == null || param.equals("")) {
			return def;
		}
		try {
			return MarmosetUtilities.toIntegerOrNull(param);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * @param string
	 * @return
	 */
	public long getLongParameter(String key)
			throws InvalidRequiredParameterException {
		try {
			String value = getStringParameter(key);
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new InvalidRequiredParameterException(e.getMessage());
		}
	}

	public double getDoubleParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getStringParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter");
		}
		double d;
		try {
			d = Double.parseDouble(param);
			return d;
		} catch (IllegalArgumentException e) {
			throw new InvalidRequiredParameterException(
					"name was of an invalid form: " + e.toString());
		}
	}

	public boolean hasKey(String key) {
		return getOptionalStringParameter(key) != null;
	}

	@Override
	public String toString() {
		return "parameters: " + parameters + "\nfileitem: " + fileItems;
	}

	public Timestamp getTimestampParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getStringParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter");
		}
		Timestamp timestamp = null;
		try {
			timestamp = Timestamp.valueOf(param);
		} catch (IllegalArgumentException e) {
			throw new InvalidRequiredParameterException(
					"name was of an invalid form: " + e.toString());
		}
		return timestamp;
	}
	
	public Set<String> getParameterNames() {
	    return  parameters.keySet();
	}
}
