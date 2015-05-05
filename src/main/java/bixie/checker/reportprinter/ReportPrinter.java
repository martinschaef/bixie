/**
 * 
 */
package bixie.checker.reportprinter;

import bixie.checker.report.Report;

/**
 * @author schaef
 *
 */
public interface ReportPrinter {
	
	public void printReport(Report r);

	public String printSummary();
	
	public int countReports();
	
}
