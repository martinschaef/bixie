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

package bixie.plugin.util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import bixie.plugin.views.BixieConsoleView;

/**
 * UI
 * 
 * @author arlt
 * 
 */
public class UI {

	/**
	 * Logs a text to Joogie's console view
	 * 
	 * @param text
	 *            Text to be logged
	 */
	public static void log(String text) {
		final String logText = text;
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				// get workbench
				IWorkbenchWindow window = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow();
				if (null == window)
					return;

				// get workbench page
				IWorkbenchPage page = window.getActivePage();
				if (null == page)
					return;

				// find Bixies's console view
				BixieConsoleView view = (BixieConsoleView) page
						.findView(BixieConsoleView.ID);
				if (null == view) {
					//if its closed, open it.
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("bixie.plugin.views.BixieConsoleView");
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
					//and try to find it again.
					view = (BixieConsoleView) page.findView(BixieConsoleView.ID);
				}
				
				if (view == null) {
					return;
				}
				
				// write text
				view.writeText(logText);
			}
		});
	}

	/**
	 * Prints an info message
	 * 
	 * @param text
	 *            Text to be printed
	 */
	public static void printInfo(String text) {
		final String printText = text;
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getCurrent()
						.getActiveShell(), "Information", printText);
			}
		});
	}

	/**
	 * Prints an error message
	 * 
	 * @param text
	 *            Text to be printed
	 */
	public static void printError(String text) {
		final String printText = text;
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Error", printText);
			}
		});
	}

}
