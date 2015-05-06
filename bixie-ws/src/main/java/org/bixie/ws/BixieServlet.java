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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bixie.ws.util.BixieParserException;
import org.bixie.ws.util.Examples;
import org.bixie.ws.util.Runner;


import bixie.util.BixieReport;
import bixie.util.BixieReport.InfeasibleMessage;

/**
 * @author arlt, schaef
 */
@SuppressWarnings("serial")
public class BixieServlet extends HttpServlet {

	String code = null;
	String example_idx = "0";
	
	protected Set<Integer> supportLines = new HashSet<Integer>();
	protected Map<Integer, String> infeasibleLines = new HashMap<Integer, String>(); 
	
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
			
			this.supportLines.clear();
			this.infeasibleLines.clear();
			LinkedList<BixieReport> reports = Runner.run(req.getServletContext(), code);
			for (BixieReport report : reports) {
				for (InfeasibleMessage im : report.messages) {
					for (org.gravy.util.JavaSourceLocation loc : im.otherLines) {
						this.supportLines.add(loc.StartLine);
					}
					for (org.gravy.util.JavaSourceLocation loc : im.infeasibleLines) {
						this.supportLines.remove(loc.StartLine);
						String comment = null;
						if (loc.comment!=null) {
							if (loc.comment.equals("elseBlock") || loc.comment.equals("elseblock")) {
								comment="The case where this conditional evaluates to false";
							} else if (loc.comment.equals("thenBlock") || loc.comment.equals("thenblock")) {
								comment="The case where this conditional evaluates to true";
							} else {
								System.err.println(loc.comment);
								comment="This line";
							}
							if (this.supportLines.size()>0) {
								comment += " conflicts with the other marked lines";
							} else {
								comment += " can never be executed";
							}
						}
						this.infeasibleLines.put(loc.StartLine, comment);
					}					
				}
			}
 
			
			req.setAttribute("inflines", this.infeasibleLines);
			req.setAttribute("suplines", this.supportLines);
			
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
