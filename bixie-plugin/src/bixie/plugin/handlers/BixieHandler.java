package bixie.plugin.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import bixie.plugin.util.UI;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class BixieHandler extends AbstractBixieHandler {


	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			this.checkWithBixie(getCompilationUnit());
		} catch (Throwable e) {
			UI.log("Plugin Crashed: " + e.toString());
			thread = null;
		}
		return null;
	}



	/**
	 * Returns the selected compilation unit
	 * 
	 * @return Compilation unit
	 */
	protected ICompilationUnit getCompilationUnit() {
		IWorkbenchPart workbenchPart = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		IFile file = (IFile) workbenchPart.getSite().getPage()
				.getActiveEditor().getEditorInput().getAdapter(IFile.class);
		return JavaCore.createCompilationUnitFrom(file);
		// return null;
	}



}
