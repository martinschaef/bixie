/**
 * 
 */
package bixie;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.gravy.report.InterpolationInfeasibleReport;
import org.gravy.report.Report;
import org.gravy.reportprinter.ReportPrinter;
import org.gravy.util.JavaSourceLocation;

import bixie.util.BixieReport;
import bixie.util.BixieReport.InfeasibleMessage;
import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 * 
 */
public class InterpolatingJavaReportPrinter implements ReportPrinter {

	protected HashMap<String, LinkedList<BixieReport>> sortedReports = new HashMap<String, LinkedList<BixieReport>>();
	
	
	
	
	/* (non-Javadoc)
	 * @see org.gravy.reportprinter.ReportPrinter#printReport(org.gravy.report.Report)
	 */
	@Override
	public void printReport(Report r) {
		if (r!=null && r instanceof InterpolationInfeasibleReport) {
			InterpolationInfeasibleReport ir = (InterpolationInfeasibleReport)r;
			this.buildBoogieErrorString(ir);
//			if (s!=null && s.length()>0) Log.info(s);
		}
	}

	private String buildBoogieErrorString(InterpolationInfeasibleReport ir) {
		LinkedList<HashMap<CfgStatement, JavaSourceLocation>> reports = ir.getReports();
		if (reports.size()==0) {
			return null;
		}
		BixieReport br = new BixieReport(ir);
		if (!this.sortedReports.containsKey(br.fileName)) {
			this.sortedReports.put(br.fileName, new LinkedList<BixieReport>()) ;
		}
		this.sortedReports.get(br.fileName).add(br);
		
		return "";
	}

	public String printAllReports() {
		StringBuilder sb = new StringBuilder();

		class BixieReportComparator implements Comparator<BixieReport> {
		    @Override
		    public int compare(BixieReport o1, BixieReport o2) {
		        return o1.firstLine.compareTo(o2.firstLine);
		    }
		};

		
		for (Entry<String, LinkedList<BixieReport>> entry : this.sortedReports.entrySet()) {
			sb.append("File : "+entry.getKey());
			sb.append("\n");
			Collections.sort(entry.getValue(), new BixieReportComparator());
			for (BixieReport br : entry.getValue()) {
				for (InfeasibleMessage im : br.messages) {
					sb.append("\t");
					for (Integer i : im.allLines) {
						sb.append(i + ", ");
					}
					sb.append("\n");
				}
			}
		}


		return sb.toString();
	}
	
}
