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
 * Created on Jan 13, 2004
 */
package edu.umd.cs.eclipse.courseProjectManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A property page dialog for configuring the AutoCVS nature for projects. This
 * code adapted from the example in Chapter 19 of <cite>Contributing to
 * Eclipse</cite> by Gamma and Beck.
 * 
 * @author David Hovemeyer
 */
public class AutoCVSPropertyPage extends PropertyPage {

	// Fields
	private Button autoCVSButton;
	
	private boolean origAutoCVS;
	
	public AutoCVSPropertyPage() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		boolean hasAutoCVSNature = false;
		try {
			hasAutoCVSNature = getProject().hasNature(
					AutoCVSPlugin.AUTO_CVS_NATURE);
			

			Debug.print("createContents: hasAutoCVSNature ==> "
					+ hasAutoCVSNature);
		} catch (CoreException e) {
			AutoCVSPlugin.getPlugin().getEventLog()
					.logError("Exception getting project nature", e);
		}
		Control control = addControl(parent, hasAutoCVSNature);

		origAutoCVS = hasAutoCVSNature;
		

		return control;
	}

	@Override
	public boolean performOk() {
		Debug.print("performOk() called");
		try {
			AutoCVSPlugin plugin = AutoCVSPlugin.getPlugin();
			boolean autoCVSEnabled = autoCVSButton.getSelection();
			
			Debug.print("\tAutoCVS ==> " + autoCVSEnabled);

			// IProjectNatureDescriptor[] natureArr =
			// getProject().getWorkspace().getNatureDescriptors();
			// for (int ii=0; ii < natureArr.length; ii++) {
			// Debug.print("Nature: " +natureArr[ii].getLabel()+ ", "
			// +natureArr[ii]);
			// }

			// Only reconfigure the project if the settings have
			// actually changed.
			if (autoCVSEnabled != origAutoCVS) {
				if (autoCVSEnabled) {
					

					plugin.addAutoCVSNature(getProject());
					
				} else {
					plugin.removeAutoCVSNature(getProject());
				}
			}
		} catch (CoreException e) {
			// IStatus s = e.getStatus();
			// if (!s.isOK()) {
			// Debug.print("Code: " +s.getCode());
			// Debug.print("Severity: " +s.getSeverity());
			// Debug.print("Message: " +s.getMessage());
			// IStatus[] statusArr = s.getChildren();
			// if (statusArr != null) {
			// Debug.print("MultiStatus messages: ");
			// for (int ii=0; ii<statusArr.length; ii++) {
			// Debug.print("\t" +statusArr[ii].getMessage());
			// }
			//
			// }
			// Throwable cause = s.getException();
			// if (cause != null)
			// Debug.print("Cause: ", cause);
			// }
			Debug.print("Core exception in performOK()", e);
			AutoCVSPlugin.getPlugin().getEventLog()
					.logError("Exception getting project nature", e);
		}
		return true;
	}

	/**
	 * Get the project we're configuring properties of.
	 * 
	 * @return the project
	 */
	private IProject getProject() {
		return (IProject) getElement();
	}

	/**
	 * Create the control used in the property page.
	 * 
	 * @param parent
	 *            the parent control
	 * @return the property page control
	 */
	private Control addControl(Composite parent, boolean hasAutoCVSNature) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);

		// This sets the GridData for the composite itself
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		Font font = parent.getFont();
		Label label = new Label(composite, SWT.NONE);
		
		boolean hasSubmitFile = AutoCVSPlugin.hasSubmitFile(getProject());
		label.setText("CourseProjectManager allows submission of projects");
		Label blank = new Label(composite, SWT.NONE);
		
		
		blank.setText("");

		
		if (hasSubmitFile) {
		autoCVSButton = new Button(composite, SWT.CHECK);
		autoCVSButton.setText("Enable Course Project Submission");
		autoCVSButton.setFont(font);
		autoCVSButton.setSelection(hasAutoCVSNature);
		} else {
		    Label msg = new Label(composite, SWT.NONE);
	        
	        msg.setText("A project needs to have a .submit file to enable course project submission.");
	        Label msg2 = new Label(composite, SWT.NONE);
	           msg2.setText("Download a .submit file for the project from the submit server.");

		}

		
		return composite;
	}

}
