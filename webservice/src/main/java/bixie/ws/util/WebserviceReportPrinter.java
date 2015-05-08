/**
 * 
 */
package bixie.ws.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import bixie.checker.report.Report;
import bixie.checker.report.Report.FaultExplanation;
import bixie.checker.reportprinter.ReportPrinter;

/**
 * @author schaef
 *
 */
public class WebserviceReportPrinter implements ReportPrinter {

	protected Map<Integer, List<FaultExplanation>> explanations = new HashMap<Integer, List<FaultExplanation>>();
	
	public Map<Integer, List<FaultExplanation>> getFaultExplanations() {
		return this.explanations;
	}
	
	/* (non-Javadoc)
	 * @see bixie.checker.reportprinter.ReportPrinter#countReports()
	 */
	@Override
	public int countReports() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see bixie.checker.reportprinter.ReportPrinter#getSortedReports()
	 */
	@Override
	public Map<Integer, Map<String, List<List<Integer>>>> getSortedReports() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see bixie.checker.reportprinter.ReportPrinter#printReport(bixie.checker.report.Report)
	 */
	@Override
	public void printReport(Report report) {
		//don't print, just store them.
		for ( Entry<Integer,List<FaultExplanation>> entry : report.getReports().entrySet()) {
			if (!this.explanations.containsKey(entry.getKey())) {
				this.explanations.put(entry.getKey(), new LinkedList<FaultExplanation>());
			}
			this.explanations.get(entry.getKey()).addAll(entry.getValue());
		}		
	}

	/* (non-Javadoc)
	 * @see bixie.checker.reportprinter.ReportPrinter#printSummary()
	 */
	@Override
	public String printSummary() {
		// TODO Auto-generated method stub
		return null;
	}

}
