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

package bixie.plugin.views;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/**
 * Joogie Console View
 * 
 * @author arlt
 * 
 */
public class BixieConsoleView extends ViewPart {

	/**
	 * The ID of the view
	 */
	public static final String ID = "bixie.plugin.views.BixieConsoleView";

	/**
	 * Text box of the view
	 */
	private Text textBox;

	@Override
	public void createPartControl(Composite parent) {
		textBox = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL
				| SWT.H_SCROLL);
		textBox.setFont(new Font(getDisplay(), "Courier New", 10, SWT.NORMAL));
		writeText("This is Bixie.");
	}

	@Override
	public void setFocus() {

	}

	/**
	 * Writes text to the text box
	 * 
	 * @param text
	 *            Text
	 */
	public void writeText(String text) {
		// get date
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String date = dateFormat.format(new Date());

		// append text
		String newText = String.format("%s %s\r\n", date, text);
		textBox.append(newText);
	}

	/**
	 * Returns the display instance
	 * 
	 * @return Display instance
	 */
	protected Display getDisplay() {
		// may be null if outside the UI thread
		Display display = Display.getCurrent();
		if (null == display)
			display = Display.getDefault();

		return display;
	}

}
