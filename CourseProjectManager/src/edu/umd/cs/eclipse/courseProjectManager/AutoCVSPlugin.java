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

package edu.umd.cs.eclipse.courseProjectManager;

import java.util.Date;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */

public class AutoCVSPlugin extends AbstractUIPlugin implements IStartup {

	static final String ID = "edu.umd.cs.eclipse.courseProjectManager";

	static final String DISABLED = "disabled";
	static final String ENABLED = "enabled";
	/** Nature id for the AutoCVS nature. */
	public static final String AUTO_CVS_NATURE = ID + ".autoCVSNature";
	/**
	 * Set to true if any automatic CVS operation fails. Upon the failure of an
	 * AutoCVS operation, we inform the user and disable automatic execution for
	 * the remainder of the Eclipse session.
	 */
	private boolean failedOperation;
	static final String SUBMITIGNORE = ".submitIgnore";
	static final String SUBMITINCLUDE = ".submitInclude";
	static final String SUBMITUSER = ".submitUser";
	static final String SUBMITPROJECT = ".submit";

	private EventLog eventLog;
	private ResourceBundle messageBundle;
	/** The shared instance */
	private static AutoCVSPlugin plugin;

	
	static boolean hasSubmitFile(IProject project) {
		IResource submitProjectFile = project
				.findMember(AutoCVSPlugin.SUBMITPROJECT);
		return submitProjectFile != null;
	}

	/**
	 * @author jspacco
	 * 
	 *         Catches run events and can log them to a standard file that
	 *         should log all events.
	 */
	private static class LaunchLogger implements ILaunchListener {
		public void launchAdded(ILaunch launch) {
			String projectName;
			IProject project = null;
			ILaunchConfiguration launchConfiguration = launch
					.getLaunchConfiguration();
			if (launchConfiguration == null)
				return;
			try {
				projectName = launchConfiguration
						.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR",
								(String) null);
				if (projectName == null) {
					AutoCVSPlugin
							.getPlugin()
							.getEventLog()
							.logMessage(
									"Unable to determine project that was just executed: "
											+ launch.getLaunchMode());
					Debug.print("Unable to determine project that was just executed: "
							+ launch.getLaunchMode());
					return;

				}
				project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
				if (project == null) {
					Debug.print("Unable to find project " + projectName);
					return;
				}
				if (!hasSubmitFile(project)) {
					Debug.print("project doesn use cvs, can't log run event "
							+ projectName);
					return;

				}

			} catch (CoreException e) {
				Debug.print("Unable to retrieve name of project", e);
				return;
			}

			

			
		}

		public void launchRemoved(ILaunch launch) {
			// AutoCVSPlugin.getPlugin().getEventLog().logMessage("launch removed: "
			// +launch.getLaunchMode());
			// Debug.print(launch.getLaunchMode());
		}

		public void launchChanged(ILaunch launch) {
			// AutoCVSPlugin.getPlugin().getEventLog().logMessage("launch changed: "
			// +launch.getLaunchMode());
			// Debug.print(launch.getLaunchMode());
		}
	}

	public EventLog getEventLog() {
		return eventLog;
	}

	/**
	 * Get a message from the message bundle.
	 * 
	 * @param key
	 *            the key of the message to retrieve
	 * @return the message
	 */
	public static String getMessage(String key) {
		return getPlugin().messageBundle.getString(key);
	}

	public String getId() {
		return getBundle().getSymbolicName();
		// return getDescriptor().getUniqueIdentifier();
	}

	public String getVersion() {
		return (String) getBundle().getHeaders().get(
				org.osgi.framework.Constants.BUNDLE_VERSION);
		// new PluginVersionIdentifier(version);
	}

	/**
	 * Has any automatic CVS operation failed?
	 * 
	 * @return true if an update has failed, false if not
	 */
	public boolean hasFailedOperation() {
		return failedOperation;
	}

	/**
	 * The constructor.
	 */
	public AutoCVSPlugin() {
		plugin = this;
	}

	
	
	
	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		Debug.print("Starting up at " + new Date());

		this.eventLog = new EventLog();
		this.messageBundle = ResourceBundle
				.getBundle("edu.umd.cs.eclipse.courseProjectManager.Messages");
		// this.messageBundle =
		// ResourceBundle.getBundle("edu.umd.cs.courseProjectManager.Messages");

		
		// Install our launch listener for logging Eclipse launch events.
		DebugPlugin debugPlugin = DebugPlugin.getDefault();
		debugPlugin.getLaunchManager().addLaunchListener(new LaunchLogger());

		// Update projects in workspace
		try {
			autoUpdateWorkspaceProjects();
		} catch (CoreException e) {
			eventLog.logError("Exception updating workspace projects", e);
		}
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static AutoCVSPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Convenience method for getting the handle of the active workbench window.
	 * 
	 * @return the active workbench window, or null if we can't get it
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null) {
			IWorkbenchWindow wwin = workbench.getActiveWorkbenchWindow();
			if (wwin == null)
				Debug.print("Could not get handle of active workbench window");
			return wwin;
		}
		return null;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path.
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin
				.imageDescriptorFromPlugin("AutoCVSPlugin", path);
	}

	

	/**
	 * Add the AutoCVS nature to given project.
	 * 
	 * @param project
	 */
	public void addAutoCVSNature(IProject project) throws CoreException {
		addProjectNature(project, AUTO_CVS_NATURE);
	}

	

	/**
	 * Remove the AutoCVS nature from given project.
	 * 
	 * @param project
	 */
	void removeAutoCVSNature(IProject project) throws CoreException {
		removeProjectNature(project, AUTO_CVS_NATURE);
	}

	private void removeProjectNature(IProject project, String natureId)
			throws CoreException {
		if (!hasProjectNature(project, natureId))
			return;

		IProjectDescription projectDescription = project.getDescription();
		String[] ids = projectDescription.getNatureIds();
		String[] updateIds = new String[ids.length - 1];
		int count = 0;
		for (int i = 0; i < ids.length; ++i) {
			if (!ids[i].equals(natureId))
				updateIds[count++] = ids[i];
		}

		projectDescription.setNatureIds(updateIds);
		project.setDescription(projectDescription, null);
	}


	private void addProjectNature(IProject project, String natureId)
			throws CoreException {
		if (hasProjectNature(project, natureId))
			return;

		IProjectDescription projectDescription = project.getDescription();
		String[] ids = projectDescription.getNatureIds();
		String[] updateIds = new String[ids.length + 1];
		System.arraycopy(ids, 0, updateIds, 0, ids.length);
		updateIds[ids.length] = natureId;

		projectDescription.setNatureIds(updateIds);
		project.setDescription(projectDescription, null);
		// project.setDescription(projectDescription, IResource.FORCE, null);
	}

	private static boolean hasProjectNature(IProject project, String natureId) {
		try {
			return project.hasNature(natureId);
		} catch (CoreException e) {
			plugin.eventLog.logError("Exception getting project nature", e);
			return false;
		}
	}

	/**
	 * Attempt an automatic CVS update of given project
	 * 
	 * @param project
	 *            the project
	 */
	void attemptCVSUpdate(final IProject project, int syncMode)
			throws CoreException/*
								 * , InvocationTargetException,
								 * InterruptedException
								 */
	{
		
	}

	/**
	 * Set whether any automatic CVS operations have failed.
	 * 
	 * @param failedOperation
	 */
	public void setFailedOperation(boolean failedOperation) {
		this.failedOperation = failedOperation;
	}

	/**
	 * Automatically update all AutoCVS projects in the workspace.
	 */
	private void autoUpdateWorkspaceProjects() throws CoreException {
		// Attempt a CVS update on projects with the AutoCVS nature
		IProject[] projectList = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		Debug.print("Workspace contains " + projectList.length + " projects");

		
	}

	/**
	 * Return whether or not the given project has the AutoCVS nature.
	 * 
	 * @param project
	 * @return true if the project has the AutoCVS nature, false otherwise
	 */
	public static boolean hasAutoCVSNature(IProject project) {
		return hasProjectNature(project, AUTO_CVS_NATURE);
	}

	

	public void earlyStartup() {
		// This method intentionally left unimplemented
	}
}
