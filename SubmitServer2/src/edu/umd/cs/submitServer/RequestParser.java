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

/**
 * Created on Jan 9, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer;

import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;

import edu.umd.cs.marmoset.modelClasses.BrowserEditing;
import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.utilities.LenientDateParser;
import edu.umd.cs.marmoset.utilities.MarmosetPatterns;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;
import edu.umd.cs.marmoset.utilities.XSSScrubber;

/**
 * @author jspacco
 *
 */
public class RequestParser {
	private HttpServletRequest request;
	private final Logger logger;
	private final boolean strictChecking;

	public RequestParser(HttpServletRequest request, Logger logger,
			boolean strictChecking) {
		this.request = request;
		this.logger = logger;
		this.strictChecking = strictChecking;
	}

	public String getStringParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter and it was " + param);
		}
		return param;
	}

	public <T extends Enum<T>> T getEnumParameter(String name, Class<T> enumClass) throws InvalidRequiredParameterException {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter");
		}
		return Enum.valueOf(enumClass, param);
	}
	public <T extends Enum<T>> T getOptionalEnumParameter(String name, Class<T> enumClass, T defaultValue) {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			return defaultValue;
		}
		try {
			return Enum.valueOf(enumClass, param);
		} catch (RuntimeException e) {
			return defaultValue;
		}
	}

	public Timestamp getTimestampParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
		    String paramDate = getParameter(name+"-date");
		    if (paramDate == null || paramDate.isEmpty()) 
		        throw new InvalidRequiredParameterException(name
	                    + " is a required parameter");
		    String paramTime= getParameter(name+"-time");
		    if (paramTime == null || paramTime.isEmpty())
		        paramTime = "23:59:59";
		    param = paramDate + " " + paramTime;
			
		}
		Timestamp timestamp = null;
		try {
			timestamp = Timestamp.valueOf(param);
		} catch (IllegalArgumentException e) {
			java.util.Date d = LenientDateParser.parse(param);
			return new Timestamp(d.getTime());
		}
		return timestamp;
	}
	
	public Timestamp getOptionalTimestampParameter(String name, Timestamp defaultValue)
			throws InvalidRequiredParameterException {
	    try {
	        return getTimestampParameter(name);
	    }catch (InvalidRequiredParameterException e) {
	        return defaultValue;
	    }
	}


	public double getDoubleParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getParameter(name);
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

	public boolean getOptionalBooleanParameter(String name) {
		String param = getParameter(name);
		if (param == null)
			return false;
		return MarmosetUtilities.isTrue(param);
	}
	public boolean getOptionalBooleanParameter(String name, boolean defaultValue) {
		String param = getParameter(name);
		if (param == null)
			return defaultValue;
		return MarmosetUtilities.isTrue(param);
	}


	public boolean getBooleanParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter");
		}
		return MarmosetUtilities.isTrue(param);
	}

	/**
	 * Return the value of the given parameter parsed as a boolean. Returns the
	 * given default if the parameter is not specified. If the parameter is
	 * specified but does not encode a boolean, false is returned.
	 *
	 * @param name
	 *            name of the boolean parameter to retrieve
	 * @param def
	 *            default to return if parameter is unspecified
	 * @return parameter parsed as a boolean, or default if parameter is
	 *         unspecified
	 */
	public boolean getBooleanParameter(String name, boolean def) {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			return def;
		}
		return MarmosetUtilities.isTrue(param);
	}

	/**
	 * Return the value of the parameter parsed as an Integer, or return null if
	 * the parameter isn't specified or cannot be parsed.
	 *
	 * @param name
	 *            name of parameter to get
	 * @return Integer value of the parameter when it's specified and parseable,
	 *         or null if the parameter is unspecified or cannot be parsed
	 */
	public @CheckForNull
	Integer getIntegerParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter");
		}
		try {
			return MarmosetUtilities.toIntegerOrNull(param);
		} catch (NumberFormatException e) {
			throw new InvalidRequiredParameterException("Parameter " + name
					+ " must be a valid integer: " + param + "\n"
					+ e.toString());
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
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			return def;
		}
		try {
			return MarmosetUtilities.toIntegerOrNull(param);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public int getIntParameter(String name)
			throws InvalidRequiredParameterException {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter");
		}
		int i;
		try {
			i = Integer.parseInt(param);
		} catch (IllegalArgumentException e) {
			throw new InvalidRequiredParameterException(
					"name was of an invalid form: " + e.toString());
		}
		return i;
	}

	public int getIntParameter(String name, int def) {
		String param = getParameter(name);
		if (param == null || param.equals("")) {
			return def;
		}
		int i;
		try {
			i = Integer.parseInt(param);
		} catch (IllegalArgumentException e) {
			return def;
		}
		return i;
	}

	public Course getCourse() throws InvalidRequiredParameterException {
		Course course = new Course();
		// these two CANNOT be null
		course.setSemester(getCheckedParameter("semester"));
		course.setCourseName(getCheckedParameter("courseName"));
		
		// these can be null
		String sectionsCsv = request.getParameter("sections");
		if (!Strings.isNullOrEmpty(sectionsCsv)) {
			course.setSections(sectionsCsv.split(","));
		}
		course.setDescription(getOptionalCheckedParameter("description"));
		course.setUrl(getOptionalCheckedParameter("url"));
		course.setBrowserEditing(BrowserEditing.valueOfAnyCase(
                getCheckedParameter("browserEditing")));
		course.setAllowsBaselineDownload(getCheckbox("download"));
		course.setAllowsHelpRequests(getCheckbox("helpRequests"));
		course.setUseDefaultBuildservers(getCheckbox("useDefaultBuildservers"));
		return course;
	}

	public Project getProject() throws InvalidRequiredParameterException {
		Project project = new Project();
		if (hasParameter("projectPK"))
			project.setProjectPK(
			        Project.asPK(getIntParameter("projectPK")));
		updateProject(project);
		return project;
	}


	public void updateProject(Project project) throws InvalidRequiredParameterException {
		boolean visibleToStudents = getOptionalBooleanParameter("visibleToStudents");
		project.setVisibleToStudents(visibleToStudents);
		
		@Project.PK int projectPK = Project.asPK(getIntParameter("projectPK", 0));
        if (hasParameter("projectPK") && project.getProjectPK() != projectPK)
			throw new IllegalArgumentException("Can't change projectPK");

		project.setTestSetupPK(getIntParameter("testSetupPK", 0));
		project.setCoursePK(Course.asPK(getIntParameter("coursePK", 0)));
		project.setProjectNumber(getCheckedParameter("projectNumber"));
		Timestamp ontime = getTimestampParameter("ontime");
		project.setOntime(ontime);
		Timestamp late = getOptionalTimestampParameter("late", ontime);
		if (ontime.compareTo(late) > 0)
		  throw new IllegalArgumentException("late deadline is before ontime deadline");
    project.setLate(late);
		String title = getCheckedParameter("title");
    project.setTitle(title);
	    project.setUrl(getOptionalCheckedParameter("url"));
	    project.setDescription(getOptionalCheckedParameter("description"));
	    Integer diffAgainst = Project.asPK(getOptionalInteger("diffAgainst"));
	    if (diffAgainst != null && diffAgainst.intValue() != 0 && diffAgainst.intValue() == projectPK)
	        throw new IllegalArgumentException("Can't diff a project against itself");
        project.setDiffAgainst(diffAgainst);
        project.setBrowserEditing(BrowserEditing.valueOfAnyCase(
                getCheckedParameter("browserEditing")));

		project.setPair(getCheckbox("pair"));

		boolean isTested = getCheckbox("tested");
		project.setIsTested(isTested);
		if (isTested) {
            project.setPostDeadlineOutcomeVisibility(getCheckedParameter("postDeadlineOutcomeVisibility"));
            String bestSubmissionPolicy = getCheckedParameter("bestSubmissionPolicy");
            project.setBestSubmissionPolicy(bestSubmissionPolicy.equals("") ? null
                    : bestSubmissionPolicy);
            project.setReleasePolicy(getCheckedParameter("releasePolicy"));
            project.setNumReleaseTestsRevealed(getIntParameter("numReleaseTestsRevealed"));
            project.setStackTracePolicy(getCheckedParameter("stackTracePolicy"));
            project.setReleaseTokens(getIntParameter("releaseTokens"));
            project.setRegenerationTime(getIntParameter("regenerationTime"));
            project.setKindOfLatePenalty(getCheckedParameter("kindOfLatePenalty"));
            // ensure that lateMultiplier has at least a default value since it
            // can't be null
            String lateMultiplier = getCheckedParameter("lateMultiplier");
            if (lateMultiplier == null || lateMultiplier.equals(""))
                project.setLateMultiplier(0.0);
            else
                project.setLateMultiplier(getDoubleParameter("lateMultiplier"));
            // ensure that lateConstant has at least a default value since it can't
            // be null
            Integer lateConstant = getIntegerParameter("lateConstant", 0);
            project.setLateConstant(lateConstant);

		}
		project.setCanonicalStudentRegistrationPK(StudentRegistration.asPK(getIntParameter("canonicalStudentRegistrationPK")));
		// these could be null
		project.setArchivePK(getIntegerParameter("archivePK", null));
		project.setUrl(getOptionalCheckedParameter("url"));
		project.setDescription(getOptionalCheckedParameter("description"));
	}

	/**
	 * Looks for a request parameter with the given key. If the given key does
	 * not map to a value, returns the default value.
	 *
	 * @param key
	 *            the request parameter key
	 * @param defaultValue
	 *            the default value to return if the given key has no value
	 * @return the value mapped to by this key, or the defaultValue if no such
	 *         value exists.
	 */
	private String getParameter(String key, String defaultValue) {
		String value = getParameter(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * Returns the parameter regardless of whether it's null or empty
	 *
	 * @param name
	 * @return
	 */
	public String getParameter(String name) {
		String value = request.getParameter(name);
		if (value == null)
			return value;
		return value.trim();
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
	 * Special call to get the "password" parameter. This is split into a
	 * separate call to distinguish it from a call to getParameter(), which we
	 * try to avoid calling since it doesn't vet the parameter before returning.
	 * Presumably it's OK not to vet passwords!
	 *
	 * @return the value of the "password" parameter
	 * @throws InvalidRequiredParameterException
	 *             if parameter is not specified
	 */
	public String getOptionalPasswordParameter() {
		return getParameter("password");
	}

	/**
	 * Special call to get a password parameter (not necessarily the "password"
	 * parameter). This is a separate call to distinguish it from a call to
	 * getParameter(), which we try to avoid calling since it doesn't vet the
	 * parameter before returning. Presumably it's OK not to vet passwords!
	 *
	 * @return the value of the given password parameter
	 * @throws InvalidRequiredParameterException
	 *             if parameter is not specified
	 */
	public String getOptionalPasswordParameter(String name) {
		return getParameter(name);
	}

	/**
	 * Special call to get a password parameter (not necessarily the "password"
	 * parameter). This is a separate call to distinguish it from a call to
	 * getParameter(), which we try to avoid calling since it doesn't vet the
	 * parameter before returning. Presumably it's OK not to vet passwords!
	 *
	 * @return the value of the given password parameter
	 * @throws InvalidRequiredParameterException
	 *             if parameter is not specified
	 */
	public String getPasswordParameter(String name)
			throws InvalidRequiredParameterException {
		return getStringParameter(name);
	}

	/**
	 * Get the (String) value of the given parameter, scrubbed so that angle
	 * brackets are escaped to avoid an HTML script injection. Return null if
	 * parameter isn't specified.
	 *
	 * @return the value of the given parameter with angle brackets escaped, or
	 *         null if parameter isn't specified
	 */
	private @CheckForNull
	String getOptionalScrubbedParameter(String name) {
		String s = getParameter(name);
		if (s == null || s.equals("")) {
			return null;
		}
		return XSSScrubber.scrubbedStr(s);
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
		String s = getParameter(name);
		if (s == null || s.equals("")) {
			throw new InvalidRequiredParameterException(name
					+ " is a required parameter and it was " + s);
		}
		return XSSScrubber.scrubbedStr(s);
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
	private @CheckForNull
	String getOptionalRegexParameter(String name, Pattern p) {
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

	public boolean hasParameter(String name) {
		return request.getParameter(name) != null;

	}
	public boolean getCheckbox(String name) {
		String s = getParameter(name);
		return "on".equals(s) || "true".equals(s) || "yes".equals(s);
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
	public @CheckForNull
	String getOptionalCheckedParameter(String name) {
		Pattern p = MarmosetPatterns.getPattern(name);
		if (p != null) {
			return getOptionalRegexParameter(name, p);
		} else {
			return getOptionalScrubbedParameter(name);
		}
	}

  public @Nonnull
  String getOptionalCheckedParameter(String name, @Nonnull String defaultValue) {
    try {
      if (!hasParameter(name))
        return defaultValue;
      Pattern p = MarmosetPatterns.getPattern(name);

      if (p != null) {
        return getRegexParameter(name, p);
      } else {
        return getScrubbedParameter(name);
      }
    } catch (InvalidRequiredParameterException e) {
      return defaultValue;
    }
  }

	/**
	 * Returns the parameter bound to key as an Integer. Returns null if no such
	 * request parameter exists.
	 *
	 * @param key
	 * @return the parameter as an Integer, or null if no such parameter exists.
	 */
	public @CheckForNull
	Integer getOptionalInteger(String key) {
		String value = getParameter(key);
		if (value == null || value.equals(""))
			return null;
		return new Integer(value);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getParameterNames() {
		return request.getParameterNames();
	}
}
