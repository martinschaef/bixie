/*
 *
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

package bixie.ws.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.joogie.util.FileIO;

/**
 * @author arlt
 */
public class Examples {

	/**
	 * Examples path
	 */
	final static String PATH_EXAMPLES = "/exs";

	/**
	 * Singleton object
	 */
	private static Examples v;

	/**
	 * List of examples
	 */
	private List<String> examples = new ArrayList<String>();

	/**
	 * Singleton method
	 * 
	 * @return Singleton
	 */
	public static Examples v() {
		if (null == v) {
			v = new Examples();
		}
		return v;
	}

	/**
	 * Inits the examples
	 * 
	 * @param ctx
	 *            Context
	 */
	protected void initExamples(ServletContext ctx) {
		// example already initialized?
		if (!examples.isEmpty())
			return;

		// list all examples
		String path = ctx.getRealPath("/");
		File examplesDir = new File(path + PATH_EXAMPLES);
		File[] exampleFiles = examplesDir.listFiles();
		
		if (exampleFiles==null) return;
		
		// load examples
		for (File exampleFile : exampleFiles) {
			String example = FileIO.fromFile(exampleFile.getPath());
			example = example.replace("\r", "");
			example = example.replace("\n", "  \\n");
			example = example.replace("\"", "  \\\"");
			examples.add(example);
		}
	}

	/**
	 * Returns the examples
	 * 
	 * @param ctx
	 *            Context
	 * @return Examples
	 */
	public String[] getExamples(ServletContext ctx) {
		initExamples(ctx);
		return examples.toArray(new String[examples.size()]);
	}

	int currentExample = 0;
	
	/**
	 * Returns a random example
	 * 
	 * @param ctx
	 *            Context
	 * @return Example
	 */
	public String getNextExample(ServletContext ctx) {
		initExamples(ctx);
		
		currentExample++;
		
		if (currentExample>=examples.size()) {
			currentExample=0;
		}
		return examples.get(currentExample);
	}
	
}
