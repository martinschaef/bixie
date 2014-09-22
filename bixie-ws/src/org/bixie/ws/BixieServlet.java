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

package org.bixie.ws;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bixie.ws.util.Examples;
import org.bixie.ws.util.BixieParserException;
import org.bixie.ws.util.Runner;

/**
 * @author arlt, schaef
 */
@SuppressWarnings("serial")
public class BixieServlet extends HttpServlet {

	String code = null;
	String example_idx = "0";
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			// load all examples
			req.setAttribute("examples", Examples.v().getExamples(req.getServletContext()));
			req.setAttribute("exampleIdx", example_idx);
			forward(req, resp);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			// run Bixie
			req.setAttribute("examples", Examples.v().getExamples(req.getServletContext()));
			code = req.getParameter("code");
			this.example_idx = req.getParameter("examplecounter");
			req.setAttribute("exampleIdx", example_idx);
			HashMap<Integer, String> lines = Runner.run(req.getServletContext(), code);
			req.setAttribute("report", lines);
		} catch (BixieParserException e) {
			req.setAttribute("parsererror", e.getErrorMessages());
		} catch (RuntimeException e) {
			e.printStackTrace();
			req.setAttribute("error", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			forward(req, resp);
		}
	}

	protected void forward(HttpServletRequest req, HttpServletResponse resp) {
		try {
			// forward request
			req.getRequestDispatcher("bixie.jsp").forward(req, resp);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
