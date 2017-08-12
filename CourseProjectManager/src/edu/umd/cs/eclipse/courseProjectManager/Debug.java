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
 * Created on Jan 16, 2004
 */
package edu.umd.cs.eclipse.courseProjectManager;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * When enabled, prints debug statements for AutoCVS plugin.
 * 
 * @author David Hovemeyer
 */
public class Debug {

	private static final String OPTION_DEBUG = AutoCVSPlugin.ID + "/debug"; //$NON-NLS-1$

	/** Set to true when debugging is enabled for the plugin. */

	static final boolean DEBUG = Boolean
			.getBoolean("edu.umd.cs.eclipse.courseProjectManager.debug")
			|| AutoCVSPlugin.getPlugin().isDebugging()
			|| true;

	static final boolean DEBUG_TIMING = DEBUG;

	/**
	 * Print a debugging message. Does nothing if DEBUG flag is false.
	 * 
	 * @param message
	 *            the message
	 */
	public static void print(String msg) {
		if (!DEBUG)
			return;
		StringBuffer msgBuf = new StringBuffer(msg.length() + 40);
		if (DEBUG_TIMING) {
			DateFormat DEBUG_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS"); //$NON-NLS-1$
			DEBUG_FORMAT.format(new Date(), msgBuf, new FieldPosition(0));
			msgBuf.append('-');
		}
		msgBuf.append('[').append(Thread.currentThread()).append(']')
				.append(msg);
		System.out.println(msgBuf.toString());

	}

	/**
	 * Print a debugging message for an exception.
	 */
	public static void print(String message, Throwable e) {
		if (DEBUG) {
			print(message);
			if (e != null)
				e.printStackTrace(System.out);
		}
	}

	/**
	 * Prints the elements of an array to System.out (stdout).
	 * 
	 * @param name
	 *            string name of the array being printed
	 * @param arr
	 *            array to be printed
	 */
	public static void printArray(String name, Object[] arr) {
		printArray(name, arr, System.out);
	}

	/**
	 * Print the elements of the array to a given stream.
	 * 
	 * @param name
	 * @param arr
	 * @param stream
	 */
	public static void printArray(String name, Object[] arr, PrintStream stream) {
		stream.print(name + ": ");
		for (int ii = 0; ii < arr.length; ii++) {
			stream.println(name + "[" + ii + "]: " + arr[ii]);
		}
		stream.println();
	}
}
