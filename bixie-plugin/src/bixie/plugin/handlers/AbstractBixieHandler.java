/**
 * 
 */
package bixie.plugin.handlers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;

import bixie.Bixie;
import bixie.InterpolatingJavaReportPrinter;
import bixie.plugin.util.UI;
import bixie.util.BixieReport;
import bixie.util.SourceLine;
import bixie.util.BixieReport.InfeasibleMessage;

/**
 * @author schaef
 *
 */
public abstract class AbstractBixieHandler extends AbstractHandler {

	/**
	 * Soot thread
	 */
	protected Thread thread;
	
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
	 * checks a given compilation unit with Bixie.
	 * @param cu the compilation unit that should be checked
	 * @return true if a Bixie thread could be started, false otherwise.
	 * @throws ExecutionException
	 */
	protected boolean checkWithBixie(ICompilationUnit cu) throws ExecutionException {
		try {
			// return, if thread is running
			if (isThreadRunning())
				return false;

			// get compilation unit
			final ICompilationUnit compilationUnit = cu;
			if (null == compilationUnit)
				return false;

			// get class and source folder
			final String clazz = getClassName(compilationUnit);

			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			String uriString = "";
			try {
				uriString = root
						.getFile(
								compilationUnit.getJavaProject()
										.getOutputLocation()).getLocationURI()
						.toString();

			} catch (Throwable e) {
				UI.log("depressing");
			}
			final String sourceFolder = uriString;

			// create thread
			thread = new Thread() {
				public void run() {
					try {
						UI.log("Checking " + clazz);
						// create report
						Bixie bixie = new Bixie();
						InterpolatingJavaReportPrinter jp = bixie
								.translateAndRun(clazz, sourceFolder);
						if (jp == null) {
							throw new RuntimeException("analyzing " + clazz
									+ " failed.");
						}

						UI.log("Done.");
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
						if (jp.getBixieReport("") == null
								|| jp.getBixieReport("").isEmpty()) {
							UI.log("Bixie did not find inconsistencies!");
						} else {
							// create new markers
							UI.log(jp.printAllReports());
							for (BixieReport r : jp.getBixieReport(clazz)) {
								for (InfeasibleMessage im : r.messages) {
									Set<Integer> supportLines = new HashSet<Integer>();

									for (SourceLine loc : im.otherLines) {
										supportLines.add(loc.StartLine);
									}

									for (SourceLine loc : im.infeasibleLines) {
										supportLines.remove(loc.StartLine);
										StringBuffer comment = new StringBuffer();
										if (loc.comment != null) {
											if (loc.comment.equals("elseBlock")
													|| loc.comment
															.equals("elseblock")) {
												comment.append("The case where this conditional is false");
											} else if (loc.comment
													.equals("thenBlock")
													|| loc.comment
															.equals("thenblock")) {
												comment.append("The case where this conditional is true");
											} else {
												comment.append("This line");
											}
											if (supportLines.size() > 0) {
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
										marker.setAttribute(
												IMarker.LINE_NUMBER,
												loc.StartLine);
										marker.setAttribute("bixie", true);

									}
								}
							}
						}

					} catch (Throwable e) {
						UI.log("Bixie Failed: " + e.toString());
					}
				}
			};

			// start thread
			thread.start();

		} catch (Throwable e) {
			UI.log("Plugin Crashed: " + e.toString());
			thread = null;
			return false;
		}
		return true;
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
	
	
}
