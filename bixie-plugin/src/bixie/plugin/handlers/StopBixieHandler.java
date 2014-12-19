package bixie.plugin.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import bixie.plugin.util.UI;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class StopBixieHandler extends AbstractHandler {


	
	/**
	 * The constructor.
	 */
	public StopBixieHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {

			// get compilation unit
			final ICompilationUnit compilationUnit = getCompilationUnit();
			if (null == compilationUnit)
				return null;
			IResource resource = compilationUnit
					.getUnderlyingResource();

			// delete old markers
			IMarker[] markers = resource
					.findMarkers(IMarker.PROBLEM, true,
							IResource.DEPTH_INFINITE);
			for (IMarker marker : markers) {
				// delete old marker
				if (marker.getAttribute("bixie", false))
					marker.delete();
			}

		} catch (Exception e) {
			UI.printError(e.getMessage());
		}
		return null;
	}
	
	/**
	 * Returns the selected compilation unit
	 * 
	 * @return Compilation unit
	 */
	protected ICompilationUnit getCompilationUnit() {
		IWorkbenchPart workbenchPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart(); 
		IFile file = (IFile) workbenchPart.getSite().getPage().getActiveEditor().getEditorInput().getAdapter(IFile.class);
		return JavaCore.createCompilationUnitFrom(file);
//		return null;
	}

	/**
	 * Returns the fully-qualified class name of a compilation unit
	 * 
	 * @param compilationUnit
	 *            Compilation unit
	 * @return Class name
	 */
	protected String getClassName(ICompilationUnit compilationUnit) {
		// return class name
		return compilationUnit.findPrimaryType().getFullyQualifiedName();
	}

	/**
	 * Returns the source folder of a compilation unit
	 * 
	 * @param compilationUnit
	 *            Compilation unit
	 * @return Source folder
	 */
	protected String getSourceFolder(ICompilationUnit compilationUnit)
			throws JavaModelException {
		String sourceFolder = null;
		IClasspathEntry[] classPath = compilationUnit.getJavaProject()
				.getResolvedClasspath(true);
		for (IClasspathEntry entry : classPath) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				IFolder folder = root.getFolder(entry.getPath());
				File file = new File(folder.getLocationURI());
				sourceFolder = file.getPath();
				break;
			}
		}
		return sourceFolder;
	}

	
}
