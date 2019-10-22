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
 * Created on Aug 12, 2004
 *
 */
package edu.umd.cs.eclipse.courseProjectManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

/**
 * @author jspacco
 * 
 */
@SuppressWarnings("restriction")
public class TurninProjectAction implements IObjectActionDelegate {
	static {
		Protocol easyhttps = new Protocol("https",
				new EasySSLProtocolSocketFactory(), 443);
		Protocol.registerProtocol("easyhttps", easyhttps);
	}

	static class RequestCanceledException extends Exception {
	}
	}
	// Fields
	private ISelection selection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.
	 * action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * Get all resources in given project.
	 * 
	 * @param project
	 *            the project
	 * @return Set containing all resources in the project
	 */
	private Set<IResource> getProjectResources(IProject project)
			throws CoreException {
		final Set<IResource> projectResources = new HashSet<IResource>();
		project.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) {
				projectResources.add(resource);
				return true;
			}
		});
		return projectResources;
	}

	/**
	 * Get all dirty editors which are editing resources belonging to a project.
	 * 
	 * @param workbench
	 *            the workbench
	 * @param project
	 *            the project
	 * @return the list of dirty editors for the project
	 */
	private List<IEditorPart> getDirtyEditorsForProject(IWorkbench workbench,
			IProject project) throws CoreException {
		Set<IResource> projectResources = getProjectResources(project);
		List<IEditorPart> dirtyEditors = new LinkedList<IEditorPart>();

		// This code is based on Workbench.saveAllEditors().
		// You'd think there would be a much easier way
		// to find dirty editors.
		IWorkbenchWindow[] wwinList = workbench.getWorkbenchWindows();
		for (int i = 0; i < wwinList.length; i++) {
			IWorkbenchWindow wwin = wwinList[i];
			IWorkbenchPage[] pageList = wwin.getPages();
			for (int j = 0; j < pageList.length; j++) {
				IWorkbenchPage page = pageList[j];
				IEditorReference[] editorReferenceList = page
						.getEditorReferences();
				for (int k = 0; k < editorReferenceList.length; k++) {
					IEditorReference editorRef = editorReferenceList[k];
					IEditorPart editor = editorRef.getEditor(true);
					if (editor != null && editor.isDirty()) {
						IEditorInput input = editor.getEditorInput();
						IResource resource = (IResource) input
								.getAdapter(IResource.class);
						if (resource != null) {
							Debug.print("Got a resource from a dirty editor: "
									+ resource.getName());
							if (projectResources.contains(resource))
								dirtyEditors.add(editor);
						}
					}
				}
			}
		}

		return dirtyEditors;
	}

	/**
	 * Save all dirty editors in given project.
	 * 
	 * @param project
	 *            the project
	 * @param workbench
	 *            the workbench
	 * @return true if no editors are dirty, or if all dirty editors were
	 *         successfully saved
	 * @throws CoreException
	 */
	private boolean saveDirtyEditors(IProject project, IWorkbench workbench)
			throws CoreException {

		List<IEditorPart> dirtyEditors = getDirtyEditorsForProject(workbench,
				project);
		boolean noDirt;
		if (dirtyEditors.isEmpty()) {
			noDirt = true;
		} else {
			if (!workbench.saveAllEditors(true))
				noDirt = false;
			else
				noDirt = getDirtyEditorsForProject(workbench, project)
						.isEmpty();
		}
		if (noDirt)
			return true;

		AutoCVSPlugin
				.getPlugin()
				.getEventLog()
				.logMessage(
						"Submission of project " + project.getName()
								+ " cancelled");
		return false;
	}

	static Properties getUserProperties(IResource submitUserResource)
			throws IOException {
		Properties userProperties = new Properties();
		if (submitUserResource != null) {
			// load .submitUser properties (classAccount and oneTimePassword)
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(submitUserResource
						.getRawLocation().toString());
				userProperties.load(fileInputStream);
			} catch (FileNotFoundException e) {
			} finally {
				if (fileInputStream != null)
					fileInputStream.close();
			}
		}
		return userProperties;
	}

	private static Properties getSubmitUserProperties(IProject project)
			throws IOException {
		// Somehow, the resource might exist but the file doesn't exist?
		IResource submitUserResource = project
				.findMember(AutoCVSPlugin.SUBMITUSER);
		return getUserProperties(submitUserResource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// TODO Refactor: Should the places where we raise a dialog and return
		// could throw an exception instead?
		String timeOfSubmission = "t" + System.currentTimeMillis();

		// Make sure we can get the workbench...
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) {
			Dialogs.errorDialog(null, "Warning: project submission failed",
					"Could not submit project",
					"Internal error: Can't get workbench", IStatus.ERROR);
			return;
		}
		// ...and the workbenchWindow
		IWorkbenchWindow wwin = workbench.getActiveWorkbenchWindow();
		if (wwin == null) {
			Dialogs.errorDialog(null, "Error submitting project",
					"Could not submit project",
					"Internal error: Can't get workbench window", IStatus.ERROR);
			return;
		}

		// Shell to use as parent of dialogs.
		Shell parent = wwin.getShell();
		// Sanity check.
		if (!(selection instanceof IStructuredSelection)) {
			Dialogs.errorDialog(
					parent,
					"Warning: Selection is Invalid",
					"Invalid turnin action: You have selected an object that is not a Project. Please select a Project and try again.",
					"Object selected is not a Project", IStatus.WARNING);
			return;
		}
		IStructuredSelection structured = (IStructuredSelection) selection;
		Object obj = structured.getFirstElement();
		Debug.print("Selection object is a " + obj.getClass().getName() + " @"
				+ System.identityHashCode(obj));
		IProject project;
		if (obj instanceof IProject) {
			project = (IProject) obj;
		} else if (obj instanceof IProjectNature) {
			project = ((IProjectNature) obj).getProject();
		} else {
			Dialogs.errorDialog(
					null,
					"Warning: Selection is Invalid",
					"Invalid turnin action: You have selected an object that is not a Project. Please select a Project and try again.",
					"Object selected is not a Project", IStatus.WARNING);
			return;
		}
		Debug.print("Got the IProject for the turnin action @"
				+ System.identityHashCode(project));

		// ================================= save dirty editors
		// ========================================
		// save dirty editors
		try {
			if (!saveDirtyEditors(project, workbench)) {
				Dialogs.errorDialog(
						parent,
						"Submit not performed",
						"Projects cannot be submitted unless all open files are saved",
						"Unsaved files prevent submission", IStatus.WARNING);
				return;
			}
		} catch (CoreException e) {
			Dialogs.errorDialog(parent, "Submit not performed",
					"Could not turn on cvs management for all project files", e);
			return;
		}

		// ========================= Add all non-ignored files in the project
		// =========================
		IResource[] files;
		try {
			Set<IResource> resourceSet = getProjectResources(project);
			ArrayList<IFile> addedFiles = new ArrayList<IFile>();
			for (Iterator<IResource> iter = resourceSet.iterator(); iter
					.hasNext();) {
				IResource resource = iter.next();
				if (resource instanceof IFile) {
					IFile file = (IFile) resource;
					if (!AutoCVSPlugin.isCVSIgnored(file)
							&& !AutoCVSPlugin.isCVSManaged(file)) {
						addedFiles.add(file);
					}
				}
			}
			files = (IResource[]) addedFiles.toArray(new IResource[addedFiles
					.size()]);
		} catch (CoreException e) {
			Dialogs.errorDialog(
					parent,
					"Submit not performed",
					"Could not perform submit; unable to find non-ignored resources",
					e);
			return;
			// TODO what to do here?
		}

		// ================================= perform CVS commit
		// ========================================
		// TODO Somehow move this into the previous try block
		// This forces add/commit operations when AutoSync was shut off
		// Would it just be easier to enable autoSync and then trigger
		// a resource changed delta, since this method appears to enable
		// autoSync anyway?
		//
		String cvsStatus = "Not performed";
		try {
			cvsStatus = forceCommit(project, files);
		} catch (Exception e) {
			Dialogs.errorDialog(
					parent,
					"CVS commit not performed as part of submission due to unexpected exception",
					e.getClass().getName() + " " + e.getMessage(), e);
		}

		// ================================= perform CVS tag
		// ========================================
		try {
			CVSOperations.tagProject(project, timeOfSubmission,
					CVSOperations.SYNC);
		} catch (Exception e) {
			AutoCVSPlugin
					.getPlugin()
					.getEventLog()
					.logError(
							"Error tagging submission; submission via the web unlikely to work",
							e);
		}

		// ================================= find properties
		// ========================================
		// find the .submitProject file
		IResource submitProjectFile = project
				.findMember(AutoCVSPlugin.SUBMITPROJECT);
		if (submitProjectFile == null) {
			Dialogs.errorDialog(parent,
					"Warning: Project submission not enabled",
					"Submission is not enabled", "There is no "
							+ AutoCVSPlugin.SUBMITPROJECT
							+ " file for the project", IStatus.ERROR);
			return;
		}
		// Get the properties from the .submit file, and the .submitUser file,
		// if it exists
		// or can be fetched from the server
		Properties allSubmissionProps = null;
		try {
			allSubmissionProps = getAllProperties(timeOfSubmission, parent,
					project, submitProjectFile);
	  } catch (RequestCanceledException e) {
	    return;
	  } catch (IOException e) {
			String message = "IOException finding "
					+ AutoCVSPlugin.SUBMITPROJECT + " and "
					+ AutoCVSPlugin.SUBMITUSER + " files; " + cvsStatus;
			AutoCVSPlugin.getPlugin().getEventLog().logError(message, e);
			Dialogs.errorDialog(parent, "Submission failed", message,
					e.getMessage(), IStatus.ERROR);
			Debug.print("IOException: " + e);
			return;
		} catch (CoreException e) {
			String message = "IOException finding "
					+ AutoCVSPlugin.SUBMITPROJECT + " and "
					+ AutoCVSPlugin.SUBMITUSER + " files; " + cvsStatus;
			AutoCVSPlugin.getPlugin().getEventLog().logError(message, e);
			Dialogs.errorDialog(parent, "Submission failed", message,
					e.getMessage(), IStatus.ERROR);
			Debug.print("CoreException: " + e);
			return;
		}

		//
		// THE ACTUAL SUBMIT HAPPENS HERE
		//
		try {
			// ============================== find files to submit
			// ====================================
			Collection<IFile> cvsFiles = findFilesForSubmission(project);

			// ========================== assemble zip file in byte array
			// ==============================

			ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
			ZipOutputStream zipfile = new ZipOutputStream(bytes);
			zipfile.setComment("zipfile for submission created by CourseProjectManager version "
					+ AutoCVSPlugin.getPlugin().getVersion());

			try {
				byte[] buf = new byte[4096];
				for (IFile file : cvsFiles) {
					if (!file.exists()) {
						Debug.print("Resource " + file.getName()
								+ " being ignored because it doesn't exist");
						continue;
					}

					ZipEntry entry = new ZipEntry(file.getProjectRelativePath()
							.toString());
					entry.setTime(file.getModificationStamp());

					zipfile.putNextEntry(entry);
					// Copy file data to zip file
					InputStream in = file.getContents();

					try {
						while (true) {
							int n = in.read(buf);
							if (n < 0)
								break;
							zipfile.write(buf, 0, n);
						}
					} finally {
						in.close();
					}
					zipfile.closeEntry();
				}
			} catch (IOException e1) {
				Dialogs.errorDialog(parent,
						"Warning: Project submission failed",
						"Unable to zip files for submission\n" + cvsStatus, e1);
				return;
			} finally {
				if (zipfile != null)
					zipfile.close();
			}

			// ============================== Post to submit server
			// ====================================
			String version = System.getProperties().getProperty(
					"java.runtime.version");
			boolean useEasyHttps = version.startsWith("1.3")
					|| version.startsWith("1.2") || version.startsWith("1.4.0")
					|| version.startsWith("1.4.1")
					|| version.startsWith("1.4.2_0") && version.charAt(7) < '5';
			if (useEasyHttps) {
				String submitURL = allSubmissionProps.getProperty("submitURL");
				if (submitURL.startsWith("https"))
					submitURL = "easy" + submitURL;
				allSubmissionProps.setProperty("submitURL", submitURL);
			}
			// prepare multipart post method
			MultipartPostMethod filePost = new MultipartPostMethod(
					allSubmissionProps.getProperty("submitURL"));

			// add properties
			addAllPropertiesButSubmitURL(allSubmissionProps, filePost);

			// add filepart
			byte[] allInput = bytes.toByteArray();
			filePost.addPart(new FilePart("submittedFiles",
					new ByteArrayPartSource("submit.zip", allInput)));

			// prepare httpclient
			HttpClient client = new HttpClient();
			client.setConnectionTimeout(5000);
			int status = client.executeMethod(filePost);

			// Piggy-back uploading the launch events onto submitting.
			EclipseLaunchEventLog.postEventLogToServer(project);

			if (status == HttpStatus.SC_OK) {
				Dialogs.okDialog(
						parent,
						"Project submission successful",
						"Project "
								+ allSubmissionProps
										.getProperty("projectNumber")
								+ " was submitted successfully\n"
								+ filePost.getResponseBodyAsString());

			} else {
				Dialogs.errorDialog(parent,
						"Warning: Project submission failed",
						"Project submission failed", filePost.getStatusText()
								+ "\n " + cvsStatus, IStatus.CANCEL);
				AutoCVSPlugin.getPlugin().getEventLog()
						.logMessage(filePost.getResponseBodyAsString());
			}

		} catch (CoreException e) {
			Dialogs.errorDialog(parent, "Warning: Project submission failed",
					"Project submissions via https failed\n" + cvsStatus, e);
		} catch (HttpConnection.ConnectionTimeoutException e) {
			Dialogs.errorDialog(parent, "Warning: Project submission failed",
					"Project submissions failed",
					"Connection timeout while trying to connect to submit server\n "
							+ cvsStatus, IStatus.ERROR);
		} catch (IOException e) {
			Dialogs.errorDialog(parent, "Warning: Project submission failed",
					"Project submissions failed\n " + cvsStatus, e);
		}
	}

	/**
	 * @param allSubmissionProps
	 * @param filePost
	 */
	static void addAllPropertiesButSubmitURL(Properties allSubmissionProps,
			MultipartPostMethod filePost) {
		for (Map.Entry<?, ?> e : allSubmissionProps.entrySet()) {
			String key = (String) e.getKey();
			String value = (String) e.getValue();
			if (!key.equals("submitURL"))
				filePost.addParameter(key, value);
		}
	}

	/**
	 * @param project
	 * @param submitProjectFile
	 * @return list of files that should be submitted
	 * @throws TeamException
	 */
	static Collection<IFile> findFilesForSubmission(IProject project)
			throws TeamException {
		// TODO Do I really need to use ICVSFile? Can't I just go ahead and use
		// a regular
		// visitor for IResources?

		// look for .submitIgnore file
		// .submitIgnore functions like a .cvsignore file and allows
		// fine-grained filtering of what gets submitted
		SubmitIgnoreFilter ignoreFilter = null;
		IResource submitignoreFile = project
				.findMember(AutoCVSPlugin.SUBMITIGNORE);
		if (submitignoreFile != null) {
			String filename = submitignoreFile.getRawLocation().toString();
			try {
				ignoreFilter = SubmitIgnoreFilter
						.createSubmitIgnoreFilterFromFile(filename);
			} catch (IOException ignore) {
				Debug.print("Unable to create new ignore SubmitFilter: "
						+ filename);
				// ignore and use default null value
			}
		}
		List<ICVSFile> cvsFiles = new LinkedList<ICVSFile>();
		SubmitResourceVisitor visitor = new SubmitResourceVisitor(ignoreFilter,
				cvsFiles);

		// Guaranteed to have a valid .submit file.
		IResource submitProjectFile = project
				.findMember(AutoCVSPlugin.SUBMITPROJECT);
		if (submitProjectFile != null) {
			CVSTeamProvider provider = CVSOperations
					.getProvider(submitProjectFile);
			CVSWorkspaceRoot root = provider.getCVSWorkspaceRoot();
			ICVSFolder rootFolder = root.getLocalRoot();
			rootFolder.acceptChildren(visitor);
		}
		Collection<IFile> result = new LinkedHashSet<IFile>();
		for (ICVSFile cvsFile : cvsFiles) {
			IResource r = cvsFile.getIResource();
			if (r instanceof IFile)
				result.add((IFile) r);
			else
				Debug.print("Not a file: " + r);

		}
		IResource submitIncludeFile = project
				.findMember(AutoCVSPlugin.SUBMITINCLUDE);
		if (submitIncludeFile instanceof IFile) {
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								((IFile) submitIncludeFile).getContents()));
				while (true) {
					String filePath = reader.readLine();
					if (filePath == null)
						break;
					IResource fileResource = project.findMember(filePath);
					if (fileResource instanceof IFile)
						result.add((IFile) fileResource);

				}

			} catch (IOException e) {
				Debug.print("Error handling " + AutoCVSPlugin.SUBMITINCLUDE
						+ " file", e);

			} catch (CoreException e) {
				Debug.print("Error handling " + AutoCVSPlugin.SUBMITINCLUDE
						+ " file", e);

			}
		}

		return result;
	}

	/**
	 * @param timeOfSubmission
	 * @param parent
	 * @param project
	 * @param submitProjectFile
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws HttpException
	 * @throws CoreException
	 */
	private Properties getAllProperties(String timeOfSubmission, Shell parent,
			IProject project, IResource submitProjectFile) throws IOException,
			FileNotFoundException, HttpException, CoreException, RequestCanceledException {
		Properties allSubmissionProps;
		//
		// Load all properties contained in .submitproject file
		// TODO validate .submitproject file
		allSubmissionProps = new Properties();
		FileInputStream fileInputStream = new FileInputStream(submitProjectFile
				.getRawLocation().toString());
		allSubmissionProps.load(fileInputStream);
		fileInputStream.close();
		// p.list(System.out);
		allSubmissionProps.setProperty("cvstagTimestamp", timeOfSubmission);
		allSubmissionProps.setProperty("submitClientTool", "EclipsePlugin");
		allSubmissionProps.setProperty("submitClientVersion", AutoCVSPlugin
				.getPlugin().getVersion());
		allSubmissionProps.setProperty("hasFailedCVSOperation", Boolean
				.toString(AutoCVSPlugin.getPlugin().hasFailedOperation()));
		allSubmissionProps.setProperty("isAutoCVSSync", Boolean
				.toString(AutoCVSPlugin.getPlugin().getAutoSync(project)));

		Properties userProperties = getSubmitUserProperties(project);

		String authentication = allSubmissionProps.getProperty("authentication.type");
		Debug.print("properties: " + userProperties);

		// classAccount will be null when we don't have a .submitUser file and
		// need
		// to fetch one from the server
		if (invalidSubmitUser(userProperties)) {
		    InputStream submitUser = null;
			if (!authentication.equals("ldap") ){
			   try {
			    submitUser = getSubmitUserForOpenId(parent, allSubmissionProps);
			   } catch (IOException | RuntimeException e) {
			     if (!authentication.equals("cas")) 
			       throw e;
			     
			   }
			}
			if (submitUser == null) {
			PasswordDialog passwordDialog = new PasswordDialog(parent);
			int passwordStatus = passwordDialog.open();
			if (passwordStatus != PasswordDialog.OK) {
				throw new RequestCanceledException();
			}
			String username = passwordDialog.getUsername();
			String password = passwordDialog.getPassword();
			
			submitUser = TurninProjectAction
					.getSubmitUserFileFromServer(username, password,
					        allSubmissionProps);
			}
			Debug.print("I have input stream from the server");

			// create .submituser file
			IFile submitUserFile = project.getFile(AutoCVSPlugin.SUBMITUSER);
			IResource submitUserResource = project
					.findMember(AutoCVSPlugin.SUBMITUSER);
			Debug.print("\nsubmitUserResource = " + submitUserResource + "\n");
			if (submitUserResource == null)
				submitUserFile.create(submitUser, true, null);
			else
				submitUserFile.setContents(submitUser, true, false, null);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			
			Debug.print("created .submituser file");
			userProperties = getSubmitUserProperties(project);
		}

		// open .submituser file, load properties, and add those properties
		// the current set of properties
		if (invalidSubmitUser(userProperties)) {
			throw new IOException(
					"Cannot find classAccount in user properties even after negotiating with the SubmitServer for a one-time password for this project");
		}

		// combine the two sets of properties
		addPropertiesNotAlreadyDefined(allSubmissionProps, userProperties);
		if (invalidSubmitUser(allSubmissionProps)) {
			throw new IOException(
					"Cannot find classAccount in all properties even after negotiating with the SubmitServer for a one-time password for this project");
		}
		return allSubmissionProps;
	}

	private boolean invalidSubmitUser(Properties userProperties) {
		return userProperties.getProperty("cvsAccount") == null
				&& userProperties.getProperty("classAccount") == null;
	}

	public static boolean openURL(String u) {
	       try {
	           URL url = new URL(u);

	           if (false) {
	               IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser("courseProjectManager");
	               browser.openURL(url);
	           } else {
	               PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
	           }
	           return true;
        } catch (PartInitException e) {
            return false;
        } catch (MalformedURLException e) {
           return false;
        }


	}
    public static InputStream getSubmitUserForOpenId(Shell parent, Properties properties) 
            throws IOException {
        String courseKey = properties.getProperty("courseKey");
        String projectNumber = properties.getProperty("projectNumber");
        String baseURL = properties.getProperty("baseURL");
        
        String encodedProjectNumber = URLEncoder.encode(projectNumber, "UTF-8");
        String u = baseURL + "/view/submitStatus.jsp?courseKey=" + courseKey + "&projectNumber="
                + encodedProjectNumber;
        SubmitUserDialog submitUserDialog = new SubmitUserDialog(parent);
        System.out.println(u);
        openURL(u);
        int status = submitUserDialog.open();

        if (status != SubmitUserDialog.OK) {
           throw new RequestCanceledException();
        }
    
         String classAccount = submitUserDialog.getClassAccount();
         String oneTimePassword = submitUserDialog.getOneTimePassString();
         String results = String.format("classAccount=%s%noneTimePassword=%s%n",
                 classAccount, oneTimePassword);
         return new ByteArrayInputStream(results.getBytes());
        
    }

	private static String forceCommit(IProject project, IResource[] addedFiles) {
		String cvsStatus = "(already submitted backup copy via CVS)";
		try {
			// Special case: if AutoSync has been disabled because of a failed
			// cvs update or because it was explicitly disabled,
			// but the user chooses to submit anyway, we need
			// to add/commit all non-ignored resources in order to bring the
			// project in sync with the repository. If we didn't, unmanaged
			// files
			// wouldn't get tagged, and some tags would correspond to stale
			// file versions.
			if (addedFiles.length > 0
					|| AutoCVSPlugin.getPlugin().hasFailedOperation()
					|| !AutoCVSPlugin.getPlugin().getAutoSync(project)) {
				if (AutoCVSPlugin.getPlugin().hasFailedOperation()
						|| !AutoCVSPlugin.getPlugin().getAutoSync(project)) {
					AutoCVSPlugin
							.getPlugin()
							.getEventLog()
							.logMessage(
									"Syncing project for disconnected submit operation for project "
											+ project.getName());
				}
				cvsStatus = "(CVS submission unsucessful as well)";
				// Completion callback to record the success or failure of the
				// add and submit actions.
				CVSOperations.SimpleContext completion = new CVSOperations.SimpleContext();

				// XXX It's not clear that SYNC mode actually works
				// CVSOperations.cvsCommand(files, CVSOperations.SYNC,
				// completion, Command.ADD);
				if (addedFiles.length > 0)
					CVSOperations.add(addedFiles, CVSOperations.SYNC,
							completion);
				if (addedFiles.length == 0 || completion.success) {

					// Commit the project to ensure all files are in sync with
					// the repository.
					// CVSOperations.cvsCommand(new IResource[]{project},
					// CVSOperations.SYNC, completion, Command.COMMIT);
					CVSOperations.commit(new IResource[] { project },
							CVSOperations.SYNC, completion);
					if (completion.success)
						cvsStatus = "(However, CVS submission successful)";

				}
			}
		} catch (CoreException e) {
			AutoCVSPlugin
					.getPlugin()
					.getEventLog()
					.logError("Error performing CVS commit during submission",
							e);
		}
		return cvsStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public static void addPropertiesNotAlreadyDefined(Properties dst,
			Properties src) {
		for (Map.Entry<?, ?> entry : src.entrySet()) {
			if (!dst.containsKey(entry.getKey()))
				dst.setProperty((String) entry.getKey(),
						(String) entry.getValue());
		}
	}

	private static class SubmitResourceVisitor implements ICVSResourceVisitor {
		private SubmitIgnoreFilter filter;

		private List<ICVSFile> cvsFiles;

		/**
		 *
		 */
		public SubmitResourceVisitor(SubmitIgnoreFilter filter,
				List<ICVSFile> cvsFiles) {
			this.filter = filter;
			this.cvsFiles = cvsFiles;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFile
		 * (org.eclipse.team.internal.ccvs.core.ICVSFile)
		 */
		public void visitFile(ICVSFile file) throws CVSException {
			// TODO More elegant way to skip these files than hardcoding things
			// here?
			// Maybe make a default SubmitIgnore object that always ignores
			// these?
			if (file.getName().equals(EclipseLaunchEventLog.LOG_NAME)) {
				Debug.print("\n\nSkipping " + file.getName() + "\n\n");
				return;
			} else if (file.getName().equals(".submitIgnore")) {
				Debug.print("Skipping submitIgnore file = " + file.getName());
				return;
			}
			// Debug.print("file: " + file.getName());
			// Debug.print("filter: " + filter);
			if (filter != null)
				Debug.print("filtering these patterns: " + filter.toString()
						+ " against this file: " + file.getName());
			if (filter == null || !filter.matches(file.getName())) {
				// only add the file if the filter exists and doesn't match this
				// string
				cvsFiles.add(file);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFolder
		 * (org.eclipse.team.internal.ccvs.core.ICVSFolder)
		 */
		public void visitFolder(ICVSFolder folder) throws CVSException {
			if (filter == null || !filter.matches(folder.getName())) {
				// only traverse this directory if the .submitignore file
				// doesn't match this directory
				Debug.print("vising folder: " + folder.getName());
				SubmitIgnoreFilter newFilter = null;
				ICVSFile submitIgnoreFile = folder
						.getFile(AutoCVSPlugin.SUBMITIGNORE);
				if (submitIgnoreFile != null && submitIgnoreFile.exists()) {
					Debug.print("valid submitIgnoreFile that exists on the file system: "
							+ submitIgnoreFile);

					String filename = getRawFilesystemLocation(submitIgnoreFile);
					try {
						Debug.print("Trying to open submitignore file: "
								+ filename);
						newFilter = SubmitIgnoreFilter
								.createSubmitIgnoreFilterFromFile(filename);

					} catch (IOException e) {
						// keep default null value, there's no useful way to
						// recover from this error
						Debug.print("Unable to create submitignore filter from file: "
								+ filename);
					}

				}
				SubmitResourceVisitor newVisitor = new SubmitResourceVisitor(
						newFilter, cvsFiles);
				folder.acceptChildren(newVisitor);
			}
		}

		/**
		 * @param iCVSFile
		 * @return
		 * @throws CVSException
		 */
		private String getRawFilesystemLocation(ICVSFile iCVSFile)
				throws CVSException {
			String filename = iCVSFile.getIResource().getRawLocation()
					.toString();
			return filename;
		}
	}

	private static class SubmitIgnoreFilter {
		private HashSet<String> filters = new HashSet<String>();

		/**
		 * Adds a new filter represented by the given string.
		 * 
		 * @param filterString
		 *            the String representing the types of files to filter
		 */
		void addFilter(String filterString) {
			filterString = "^" + filterString;
			// using replaceAll() since replace() is only available in Java 1.5
			// filterString = filterString.replaceAll("\\$", "\\\\\\$");
			filterString = filterString.replaceAll("\\.", "\\\\.");
			filterString = filterString.replaceAll("\\*", "\\.\\*");
			filters.add(filterString);
		}

		/**
		 * Checks if a filename should be filtered out because it matches one of
		 * the rules in the SubmitIgnore object (which was created from a
		 * .submitignore file).
		 * 
		 * @param filename
		 *            the filename to try to match
		 * @return true if the file should be filtered, false otherwise
		 */
		boolean matches(String filename) {
			for (String regexp : filters) {
				if (filename.matches(regexp))
					return true;
			}
			return false;
		}

		public String toString() {
			StringBuffer result = new StringBuffer();
			for (Iterator<String> ii = filters.iterator(); ii.hasNext();) {
				result.append(ii.next() + "\n");
			}
			return result.toString();
		}

		/**
		 * Gets an iterator over the filter strings.
		 * 
		 * @return an iterator over the filter strings
		 */
		Iterator<String> iterator() {
			return filters.iterator();
		}

		/**
		 * Static factory method that creates a SubmitIgnoreFilter from a file.
		 * 
		 * @param filename
		 *            the name of the file to use to create the
		 *            SubmitIgnoreFilter.
		 * @return a submitIgnoreFilter
		 * @throws IOException
		 *             if there is an error reading the file
		 */
		static SubmitIgnoreFilter createSubmitIgnoreFilterFromFile(
				String filename) throws IOException {
			SubmitIgnoreFilter submitIgnoreFilter = new SubmitIgnoreFilter();

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(filename));

				String filterString;
				while ((filterString = readLine(reader)) != null) {
					submitIgnoreFilter.addFilter(filterString);
				}
				return submitIgnoreFilter;
			} finally {
				if (reader != null)
					reader.close();
			}
		}
	}

	/**
	 * Utility method that reads the next non-comment, non-empty line from a
	 * BufferedReader. Treats the data coming out of the BufferedReader as if it
	 * were a unix-style configuration file, using '#' to denote a comment.
	 * 
	 * @param reader
	 *            the BufferedReader
	 * @return the next non-comment, non-empty line, or null if we're at EOF
	 * @throws IOException
	 */
	public static String readLine(BufferedReader reader) throws IOException {
		String line;
		do {
			line = reader.readLine();
			// return null if the BufferedReader returns null (meaning we'er at
			// EOF)
			if (line == null)
				return null;

			// System.out.println("line before: " + line);

			// else try to strip out comments
			int startComment = line.indexOf('#');
			// System.out.println("startComment: " +startComment);
			if (startComment != -1) {
				line = line.substring(0, startComment);
			}
			// System.out.println("line after: " +line);

			// replace all leading whitespace
			line = line.replaceAll("\\s+", "");
		} while (line.equals(""));

		// at this point we know that we have a valid non-empty string
		return line;
	}

	public static void main(String[] args) throws Exception {
		String HOME = System.getenv("HOME");
		TurninProjectAction.SubmitIgnoreFilter submitIgnoreFilter = TurninProjectAction.SubmitIgnoreFilter
				.createSubmitIgnoreFilterFromFile(HOME + "/submitignore");

		for (Iterator<String> ii = submitIgnoreFilter.iterator(); ii.hasNext();) {
			System.out.println(ii.next());
		}

	}

	private static InputStream getSubmitUserFileFromServer(String loginName,
			String password, Properties allProperties) throws IOException,
			HttpException {
		String url = allProperties.getProperty("baseURL");
		url += "/eclipse/NegotiateOneTimePassword";
		// System.out.println(url);
		// Debug.print("url: " +url);
		PostMethod post = new PostMethod(url);
		post.addParameter("loginName", loginName);
		post.addParameter("password", password);
		
		addParameter(post, "courseKey", allProperties);
		addParameter(post, "projectNumber", allProperties);
		post.addParameter("submitClientVersion", AutoCVSPlugin
				.getPlugin().getVersion());

		HttpClient client = new HttpClient();
		client.setConnectionTimeout(5000);

		// System.out.println("Preparing to execute method");
		int status = client.executeMethod(post);
		// System.out.println("Post finished with status: " +status);

		if (status != HttpStatus.SC_OK) {
			throw new HttpException(
					"Unable to negotiate one-time password with the server: "
							+ post.getResponseBodyAsString());
		}

		return post.getResponseBodyAsStream();
	}
	
	static void addParameter(PostMethod post, String name, Properties properties) {
	    String property = properties.getProperty(name);
		if (property != null)
			post.addParameter(name, property);
	}
	
}
