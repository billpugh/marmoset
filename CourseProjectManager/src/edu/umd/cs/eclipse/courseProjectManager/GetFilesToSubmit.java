package edu.umd.cs.eclipse.courseProjectManager;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class GetFilesToSubmit {

    public static ArrayList<IResource> getAllCFilesInProject(IProject project) {
        ArrayList<IResource> allCFiles = new ArrayList<IResource>();
        IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        IPath path = project.getLocation();

        recursiveFindCFiles(allCFiles, path, myWorkspaceRoot);
        return allCFiles;
    }

    private static void recursiveFindCFiles(ArrayList<IResource> filesToSubmit, IPath path,
            IWorkspaceRoot myWorkspaceRoot) {
        IContainer container = myWorkspaceRoot.getContainerForLocation(path);

        try {
            IResource[] iResources;
            iResources = container.members();
            for (IResource iR : iResources) {
                // for c files
                if ("c".equalsIgnoreCase(iR.getFileExtension()))
                    filesToSubmit.add(iR);
                if (iR.getType() == IResource.FOLDER) {
                    IPath tempPath = iR.getLocation();
                    recursiveFindCFiles(filesToSubmit, tempPath, myWorkspaceRoot);
                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static IProject getCurrentProject() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
            Object firstElement = selection.getFirstElement();
            if (firstElement instanceof IAdaptable) {
                IProject project = (IProject) ((IAdaptable) firstElement).getAdapter(IProject.class);
                return project;
            }
        }
        return null;
    }
}
