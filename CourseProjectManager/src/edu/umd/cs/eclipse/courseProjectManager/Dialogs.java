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
 * Created on Feb 2, 2004
 */
package edu.umd.cs.eclipse.courseProjectManager;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

/**
 * Convenience methods for error and success dialogs. In addition to displaying
 * a modal dialog, these methods log events to the AutoCVS console.
 * 
 * @author David Hovemeyer
 */
public class Dialogs {

	/**
	 * Show an error dialog caused by receiving an exception.
	 * 
	 * @param parent
	 *            parent Shell, null if none
	 * @param title
	 *            the dialog title
	 * @param message
	 *            description of the error, if one can't be found automatically
	 * @param e
	 *            the exception
	 */
	public static void errorDialog(Shell parent, String title, String message,
			Throwable e) {
		// Log the error.
		AutoCVSPlugin.getPlugin().getEventLog()
				.logError(title + ": " + message, e);

		// If e is an InvocationTargetException, then the wrapped
		// exception is much more likely to have useful information in it
		// than the outer exception.
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		Debug.print("Error dialog:");
		Debug.print("  title: " + title);
		Debug.print("  message: " + message);
		Debug.print("  exception:", e);
		// Create IStatus object describing the error.
		// Hopefully we're looking at a CoreException where the
		// status object is already present. If not, we just
		// use in a generic message to describe the error.
		IStatus status;
		if (e instanceof CoreException) {
			final IStatus exceptionStatus = ((CoreException) e).getStatus();
			// Reported status should have a severity less than errors
			if (exceptionStatus.getSeverity() != IStatus.ERROR) {
				status = exceptionStatus;
			} else {
				// Wrap the IStatus to turn it into a warning
				status = new IStatus() {
					public IStatus[] getChildren() {
						return exceptionStatus.getChildren();
					}

					public int getCode() {
						return exceptionStatus.getCode();
					}

					public Throwable getException() {
						return exceptionStatus.getException();
					}

					public String getMessage() {
						return exceptionStatus.getMessage();
					}

					public String getPlugin() {
						return exceptionStatus.getPlugin();
					}

					public int getSeverity() {
						return IStatus.WARNING;
					}

					public boolean isMultiStatus() {
						return exceptionStatus.isMultiStatus();
					}

					public boolean isOK() {
						return exceptionStatus.isOK();
					}

					public boolean matches(int severityMask) {
						return exceptionStatus.matches(severityMask);
					}
				};
			}
		} else {
			status = new Status(IStatus.ERROR, AutoCVSPlugin.getPlugin()
					.getBundle().getSymbolicName(), 0, // TODO: need a
																// code here?
					message, e);
		}
		AutoCVSPlugin.getPlugin().getLog().log(status);
		// Create a nice obnoxious dialog to inform the user.
		new ErrorDialog(parent, title, message, status, IStatus.CANCEL
				| IStatus.ERROR | IStatus.INFO | IStatus.WARNING).open();

	}

	/**
	 * Convenience method for displaying an error dialog.
	 * 
	 * @param parent
	 *            parent Shell, null if none
	 * @param title
	 *            dialog title
	 * @param message
	 *            message to be displayed
	 * @param reason
	 *            reason for the error
	 * @param severity
	 *            TODO
	 */
	public static void errorDialog(Shell parent, String title, String message,
			String reason, int severity) {
		// Log the error.
		AutoCVSPlugin.getPlugin().getEventLog()
				.logError(title + ": " + message + ":" + reason);
		Debug.print("Error dialog:");
		Debug.print("  title: " + title);
		Debug.print("  message: " + message);
		Debug.print("  reason:" + reason);
		IStatus status = new Status(severity,
				AutoCVSPlugin.getPlugin().getId(), 0, reason, null);
		AutoCVSPlugin.getPlugin().getLog().log(status);
		new ErrorDialog(parent, title, message, status, IStatus.CANCEL
				| IStatus.ERROR | IStatus.INFO | IStatus.WARNING).open();
	}

	/**
	 * Dialog to show the successful completion of a task.
	 * 
	 * @param parent
	 *            parent Shell, null if none
	 * @param title
	 *            dialog title
	 * @param message
	 *            message to be displayed
	 */
	public static void okDialog(Shell parent, String title, String message) {
		// Log the event
		AutoCVSPlugin.getPlugin().getEventLog()
				.logMessage(title + ": " + message);

		// Tagging appears to have succeeded.
		// Inform the user.
		new MessageDialog(parent, title, null, message, Window.OK,
				new String[] { "OK" }, 0).open();
	}
}
