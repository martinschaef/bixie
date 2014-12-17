/*
 * Joogie translates Java bytecode to the Boogie intermediate verification language
 * Copyright (C) 2011 Martin Schaef and Stephan Arlt
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package bixie.plugin.popup.actions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import bixie.Bixie;
import bixie.InterpolatingJavaReportPrinter;
import bixie.plugin.util.UI;
import bixie.util.BixieReport;
import bixie.util.BixieReport.InfeasibleMessage;

/**
 * Run Joogie Action
 * 
 * @author arlt
 */
public class RunBixieAction implements IObjectActionDelegate {

	/**
	 * Soot thread
	 */
	private Thread thread;

	/**
	 * Boogie file
	 */
	private String boogieFile;

	/**
	 * C-tor
	 */
	public RunBixieAction() {
		super();
		boogieFile = addMissingSeparator(System.getProperty("java.io.tmpdir"))
				+ "joogie.bpl";
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}

	@Override
	public void run(IAction action) {
		try {
			// return, if thread is running
			if (isThreadRunning())
				return;

			// get compilation unit
			final ICompilationUnit compilationUnit = getCompilationUnit();
			if (null == compilationUnit)
				return;

			// get class and source folder
			final String clazz = getClassName(compilationUnit);
			final String sourceFolder = getSourceFolder(compilationUnit);
			
			// create thread
			thread = new Thread() {
				public void run() {
					try {
						UI.log("Checking "+clazz);
						// create report
						Bixie bixie = new Bixie();
						InterpolatingJavaReportPrinter jp = bixie.translateAndRun(clazz, sourceFolder);
						// get resource
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
						
						// no infeasible code?
						if (jp.getBixieReport("")==null || jp.getBixieReport("").isEmpty()) {
							UI.printInfo("Bixie did not find inconsistencies!");
						} else {
							// create new markers
							UI.log(jp.printAllReports());
							for (BixieReport r : jp.getBixieReport(clazz)) {								
								for (InfeasibleMessage im : r.messages) {
									Set<Integer> supportLines = new HashSet<Integer>();
									
									for (org.gravy.util.JavaSourceLocation loc : im.otherLines) {
										supportLines.add(loc.StartLine);
									}									
									
									for (org.gravy.util.JavaSourceLocation loc : im.infeasibleLines) {
										supportLines.remove(loc.StartLine);
										StringBuffer comment = new StringBuffer();
										if (loc.comment!=null) {
											if (loc.comment.equals("elseBlock") || loc.comment.equals("elseblock")) {
												comment.append("The case where this conditional is false");
											} else if (loc.comment.equals("thenBlock") || loc.comment.equals("thenblock")) {
												comment.append("The case where this conditional is true");
											} else {
												System.err.println(loc.comment);
												comment.append("This line");
											}
											if (supportLines.size()>0) {
												comment.append(" is inconsistent with lines ");
												boolean first = true;
												for (Integer i : supportLines) {
													if (first) {
														first = false;
													} else {
														comment.append(", ");
													}
													comment.append(i.toString());
												}
											} else {
												comment.append(" can never be executed");
											}
										}
										IMarker marker = resource
												.createMarker(IMarker.PROBLEM);
										marker.setAttribute(IMarker.MESSAGE,
												comment.toString());
										marker.setAttribute(IMarker.SEVERITY,
												IMarker.SEVERITY_ERROR);
										marker.setAttribute(IMarker.LINE_NUMBER,
												loc.StartLine);
										marker.setAttribute("bixie", true);
									}										
								}
							}
						}

					} catch (Exception e) {
						UI.printError(e.getMessage());
					} finally {
						deleteTempFiles();
					}
				}
			};

			// start thread
			thread.start();

		} catch (Exception e) {
			UI.printError(e.getMessage());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * Determines if the thread is running or not
	 * 
	 * @return true = thread is running
	 */
	protected boolean isThreadRunning() {
		if (null != thread && thread.isAlive()) {
			UI.printError("Bixie is running!");
			return true;
		}
		return false;
	}

	/**
	 * Returns the selected compilation unit
	 * 
	 * @return Compilation unit
	 */
	protected ICompilationUnit getCompilationUnit() {
		// get workbench window
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (null == window)
			return null;

		// get selection
		ISelection selection = window.getSelectionService().getSelection();
		if (null == selection)
			return null;

		// return compilation unit
		TreeSelection treeSelection = (TreeSelection) selection;
		return (ICompilationUnit) treeSelection.getFirstElement();
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

	/**
	 * Deletes the temporary files
	 */
	protected void deleteTempFiles() {
		try {
			new File(boogieFile).delete();

		} catch (Exception e) {
			UI.printError(e.getMessage());
		}
	}

	/**
	 * Adds a missing separator to a path
	 * 
	 * @param path
	 *            Path
	 * @return Path without missing separator
	 */
	protected String addMissingSeparator(String path) {
		if (path.length() > 0
				&& File.separatorChar != path.charAt(path.length() - 1)) {
			path += File.separatorChar;
		}
		return path;
	}

}
