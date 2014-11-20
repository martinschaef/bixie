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

import util.Log;
import bixie.util.BixieReport;
import bixie.util.BixieReport.InfeasibleMessage;
import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 * 
 */
public class InterpolatingJavaReportPrinter implements ReportPrinter {

	protected HashMap<String, LinkedList<BixieReport>> sortedReports = new HashMap<String, LinkedList<BixieReport>>();
	
	public LinkedList<BixieReport> getBixieReport(String filename) {
		if (this.sortedReports.containsKey(filename)) {
			return sortedReports.get(filename);
		} else {
			if (this.sortedReports.size() == 1) {
				String s = this.sortedReports.keySet().iterator().next();
				return sortedReports.get(s);
			}
		}
		return null;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.gravy.reportprinter.ReportPrinter#printReport(org.gravy.report.Report)
	 */
	@Override
	public void printReport(Report r) {
		if (r!=null && r instanceof InterpolationInfeasibleReport) {
			InterpolationInfeasibleReport ir = (InterpolationInfeasibleReport)r;
			Log.debug(this.buildBoogieErrorString(ir));
//			if (s!=null && s.length()>0) Log.info(s);
		}
	}

	private String buildBoogieErrorString(InterpolationInfeasibleReport ir) {
		LinkedList<HashMap<CfgStatement, JavaSourceLocation>> reports = ir.getReports();
		if (reports==null || reports.size()==0) {
			return null;
		}
		BixieReport br = new BixieReport(ir);
		if (!this.sortedReports.containsKey(br.fileName)) {
			this.sortedReports.put(br.fileName, new LinkedList<BixieReport>()) ;
		}
		this.sortedReports.get(br.fileName).add(br);
		
		StringBuilder sb = new StringBuilder();
		sb.append("In file ");
		sb.append(br.fileName);
		sb.append("\n");	
		for (InfeasibleMessage m : br.messages) {
			sb.append("\t");
			for (Integer i : m.allLines) {
				sb.append(i + ", ");
			}
			sb.append("\n");
		}
			
		return sb.toString();
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
			if (entry.getValue().isEmpty()) continue;
			sb.append("File : "+entry.getKey());
			sb.append("\n");
			sb.append(" Inconsistency detected between the following lines:\n");
			Collections.sort(entry.getValue(), new BixieReportComparator());
			for (BixieReport br : entry.getValue()) {
				for (InfeasibleMessage im : br.messages) {
					HashMap<Integer, JavaSourceLocation> lines = new HashMap<Integer, JavaSourceLocation>();
					for (JavaSourceLocation jl : im.otherLines) {
						lines.put(jl.StartLine, jl);
					}
					for (JavaSourceLocation jl : im.infeasibleLines) {
						lines.put(jl.StartLine, jl);
					}
					LinkedList<Integer> orderedKeys = new LinkedList<Integer>();
					orderedKeys.addAll(lines.keySet());
					Collections.sort(orderedKeys);
					sb.append("\t");
					for (Integer i : orderedKeys) {
						sb.append(i);
						JavaSourceLocation jl = lines.get(i);
						if (jl.comment!=null) {
							if (jl.comment.equals("thenblock")) {
								sb.append( "(then-block)");
							} else if (jl.comment.equals("elseblock")) {
								sb.append( "(else-block)");
							}
						}
						if (i!=orderedKeys.getLast()) {
							sb.append(", ");
						}
					}
					
					sb.append("\n");
				}
			}
		}


		return sb.toString();
	}
	
}
