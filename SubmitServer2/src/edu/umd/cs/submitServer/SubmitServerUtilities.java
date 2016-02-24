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

package edu.umd.cs.submitServer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author jspacco
 * 
 */
public final class SubmitServerUtilities {
	public static String extractURL(HttpServletRequest request) {
		String parameters = request.getQueryString();
		if (parameters == null)
			parameters = "";
		else
			parameters = "?" + parameters;
		return request.getContextPath() + request.getRequestURI() + parameters;
	}

	/**
	 * Uses reflection to find the void (no-arg) constructor for a given class
	 * and invoke it to create a new instance of the object.
	 * 
	 * @param className
	 *            The class of the object to be instantiated.
	 * @return An fresh instance of an object of type className.
	 * @throws ServletException
	 *             There are 5 exceptions that can happen when trying to find
	 *             and invoke a constructor based on the name of the class; this
	 *             method wraps any of these exceptions with ServletException
	 *             and then throws the ServletException.
	 */
	public static Object createNewInstance(String className)
			throws ServletException {
		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(new Class[0]);
			return constructor.newInstance();
		} catch (ClassNotFoundException e) {
			throw new ServletException(e);
		} catch (NoSuchMethodException e) {
			throw new ServletException(e);
		} catch (IllegalAccessException e) {
			throw new ServletException(e);
		} catch (InvocationTargetException e) {
			throw new ServletException(e);
		} catch (InstantiationException e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Should this button of an HTML checkbox or radio button be checked?
	 * 
	 * @param currentVal
	 * @param selectVal
	 * @return "checked" if it should be checked, "" (empty string) otherwise
	 */
	public static <T> String  checked(T currentVal, T selectVal) {
		return checked(currentVal != null && currentVal.equals(selectVal));
	}
	/**
	 * Should this button of an HTML checkbox or radio button be checked?
	 * 
	 * @param currentVal
	 * @param selectVal
	 * @return "checked" if it should be checked, "" (empty string) otherwise
	 */
	public static <T> String checkedOrNull(String currentVal, String selectVal) {
		return checked(currentVal == null || currentVal.isEmpty() || currentVal.equals(selectVal));
	}


	/**
	 * Should this button of an HTML checkbox or radio button be checked?
	 * 
	 * @param currentVal
	 * @param selectVal
	 * @return "checked" if it should be checked, "" (empty string) otherwise
	 */
	public static String checked(boolean value) {
		return value ? "checked" : "";
	}

	/**
	 * Should this &lt;option&gt; of an HTML &lt;select&gt; pull-down menu be
	 * the default selected?
	 * 
	 * @param currentVal
	 * @param selectVal
	 * @return "selected" if it should be selected, "" (empty string) otherwise
	 */
	
	public static <T> String selected(@CheckForNull T currentVal, T selectVal) {
	    if (currentVal instanceof Enum<?> && selectVal instanceof String
	            && ((Enum<?>)currentVal).name().equalsIgnoreCase((String)selectVal))
	        return "selected";
	    if (currentVal instanceof String && selectVal instanceof String
	        && ((String)currentVal).equalsIgnoreCase((String) selectVal)) 
	      return "selected";
		return currentVal != null && currentVal.equals(selectVal) ? "selected" : "";
	}
	/**
	 * Should this &lt;option&gt; of an HTML &lt;select&gt; pull-down menu be
	 * the default selected?
	 * 
	 * @param currentVal
	 * @param selectVal
	 * @return "selected" if it should be selected, "" (empty string) otherwise
	 */
	public static String selectedOrNull(@CheckForNull String currentVal, String selectVal) {
		return currentVal == null || currentVal.isEmpty() || currentVal.equalsIgnoreCase(selectVal) ? "selected" : "";
	}
}
