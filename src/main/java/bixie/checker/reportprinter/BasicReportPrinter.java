/**
 * 
 */
package bixie.checker.reportprinter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import util.Log;
import bixie.checker.report.Report;
import bixie.checker.report.Report.FaultExplanation;
import bixie.checker.util.SourceLine;

/**
 * @author schaef
 *
 */
public class BasicReportPrinter implements ReportPrinter {

	StringBuilder reports = new StringBuilder();
	int cirtical, errorhandling, unreachable;
	
	/**
	 * 
	 */
	public BasicReportPrinter() {
		// TODO Auto-generated constructor stub
		cirtical = 0;
		errorhandling = 0;
		unreachable = 0;
	}

	/* (non-Javadoc)
	 * @see bixie.checker.reportprinter.ReportPrinter#printReport(bixie.checker.report.Report)
	 */
	@Override
	public void printReport(Report r) {
			String s = this.buildBoogieErrorString(r);
			if (s!=null && s.length()>0) {				
				reports.append("===========\n");
				reports.append(s);
				Log.info(s);
			} 
	}
	
	public String printSummary() {
		this.reports.append("Summary: fwd=");
		this.reports.append(cirtical);
		this.reports.append("\tbwd=");
		this.reports.append(errorhandling);
		this.reports.append("\tunr=");
		this.reports.append(unreachable);
		this.reports.append("\n");
		return reports.toString();
	}

	private String buildBoogieErrorString(Report r) {
		
		Map<Integer, List<FaultExplanation>> reports = r.getReports();
		
		StringBuffer sb = new StringBuffer();
		
		if (reports.containsKey(0) && reports.get(0).size()>0) {
			cirtical++;
			sb.append("** CRITICAL (fwd reachable) **\n");
			appendReport(reports.get(0), sb);
		}
		
		if (reports.containsKey(1) && reports.get(1).size()>0) {
			errorhandling++;
			sb.append("** Bad Error Handling (bwd reachable) **\n");
			appendReport(reports.get(1), sb);
		}
		if (reports.containsKey(2) && reports.get(2).size()>0) {
			unreachable++;
			sb.append("** Unreachable (bwd reachable) **\n");
			appendReport(reports.get(2), sb);
		}		
		
//		sb.append("Total reports: "+reports.size()+ " -----------------\n");
		
		return sb.toString();
	}


	private void appendReport(List<FaultExplanation> reports, StringBuffer sb) {
		
		
		for (FaultExplanation fe : reports) {
			if (fe.locations.isEmpty()) continue;
			sb.append("  In file: " + fe.fileName+"\n");
			sb.append("\t lines: ");
			String comma = "";
			
			LinkedHashSet<Integer> lines = new LinkedHashSet<Integer>();
			for (SourceLine line : fe.locations) {
				lines.add(line.StartLine);
			}
			LinkedList<Integer> sortedLines = new LinkedList<Integer>(lines);
			Collections.sort(sortedLines);
			
			for (Integer i : sortedLines) {
				sb.append(comma);
				sb.append(i);
				comma = ", ";
			}
			sb.append("\n");
		}
			
	}

	@Override
	public int countReports() {		
		return this.cirtical + this.errorhandling + this.unreachable;
	}
	
	
}
