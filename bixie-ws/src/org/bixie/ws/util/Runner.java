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

package org.bixie.ws.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.apache.tomcat.util.http.fileupload.FileUtils;

import bixie.Bixie;
import bixie.JavaReportPrinter;

/**
 * @author schaef
 */
public class Runner {

	/**
	 * Paths where the posts are stored
	 */
	final static String PATH_POSTS = "/tmp";

	final static String SUB_FOLDER = "/bixie";

	/**
	 * Paths where the libs are stored
	 */
	final static String PATH_LIBS = "/WEB-INF/lib";

	/**
	 * Max length of a program to be checked
	 */
	final static int MAX_LENGTH = 1024;

	public static String ErrorMessage;

	/**
	 * Runs GraVy
	 * 
	 * @param code
	 *            Code of the program to be checked
	 * 
	 * @param ctx
	 *            Context
	 * @return Report object
	 */
	public static HashMap<Integer, String> run(ServletContext ctx, String code)
			throws Exception {
		// pre-analyze code
		if (code.length() > MAX_LENGTH) {
			throw new RuntimeException(
					"Sorry, this program is too large for this web service.");
		}
		if (code.contains("package")) {
			throw new RuntimeException(
					"Sorry, the keyword 'package' is not allowed in this web service.");
		}

		// create UUID
		String uuid = String.format("bixie%s", UUID.randomUUID().toString()
				.replace("-", ""));
		String fileName = String.format("%s.java", uuid);

		// create a subfolder to make sure that there is only on file in that
		// folder. Just to be safe, delete the entire directory.
		String dirName = String.format("%s/%s", PATH_POSTS, String.format("bixie%s", UUID.randomUUID().toString()
				.replace("-", "")));
		File theDir = new File(dirName);
		if (theDir.exists()) {
			FileUtils.deleteDirectory(theDir);
		} 
		theDir.mkdir();
		
		String pathName = String.format("%s/%s", theDir.getAbsolutePath(), fileName);
		String clazz = String.format("public class %s {}", uuid);

		// create source file
		File sourceFile = new File(pathName);
		FileWriter fw = new FileWriter(sourceFile);
		fw.write(String.format("%s %s", code, clazz));
		fw.flush();
		fw.close();

		// run GraVy
		String libPath = ctx.getRealPath("/");
		libPath = new File(libPath, PATH_LIBS).getPath();
		JavaReportPrinter jp = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream baes = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		System.setErr(new PrintStream(baes));

		try {
			Bixie bixie = new Bixie();
			jp = bixie.translateAndRun(theDir.getAbsolutePath(), theDir.getAbsolutePath()
					+ File.pathSeparator + libPath);

		} catch (Throwable e) {
			// e.printStackTrace();
			jp = null;
		} finally {
			System.setOut(new PrintStream(new FileOutputStream(
					FileDescriptor.out)));
			System.setErr(new PrintStream(new FileOutputStream(
					FileDescriptor.err)));
			// delete source file
			sourceFile.delete();
			FileUtils.deleteDirectory(theDir);
		}

		if (jp == null
				|| baes.toString().contains("soot.CompilationDeathException")) {
			throw new BixieParserException(parseError(baos.toString()));
		} else {
			ErrorMessage = null;
		}

		// don't use the actual filename because tomcat adds some prefix to
		// it, so most likely, we're not going to find the actual
		// string anyway.
		return jp.getInfeasibleLines("");
	}

	private static HashMap<Integer, String> parseError(String error) {
		ErrorMessage = null;
		String[] lines = error.split("\n");
		HashMap<Integer, String> errorMessages = new HashMap<Integer, String>();
		if (lines == null)
			return errorMessages;
		boolean found = false;
		Integer lastLine = 0;

		for (String line : lines) {

			if (line.contains(".java")) {
				String substr = line.substring(line.indexOf(":") + 1);
				int firstComma = substr.indexOf(",");
				int firstCol = substr.indexOf(":");
				int idx = firstComma;
				if (firstComma == -1 || (firstCol > -1 && firstCol < idx)) {
					idx = firstCol;
				}
				if (idx < 0) {
					continue;
				}
				lastLine = Integer.parseInt(substr.substring(0, idx));
				found = true;
			} else {
				if (found) {
					String msg = "";
					if (errorMessages.containsKey(lastLine)) {
						msg = errorMessages.get(lastLine) + "\\n";
					}
					msg += line.substring(line.indexOf(":") + 1);
					errorMessages.put(lastLine, msg);
				}
				found = false;
			}

		}
		return errorMessages;
	}

}