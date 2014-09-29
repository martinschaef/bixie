/**
 * 
 */
package bixie;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.gravy.report.InterpolationInfeasibleReport;
import org.gravy.report.Report;
import org.gravy.reportprinter.ReportPrinter;
import org.gravy.util.JavaSourceLocation;

import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 * 
 */
public class InterpolatingJavaReportPrinter implements ReportPrinter {

	protected HashMap<String, LinkedHashSet<Integer[]>> sortedReports = new HashMap<String, LinkedHashSet<Integer[]>>();
	
	
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

		StringBuffer sb = new StringBuffer();
		
		sb.append("Total reports: "+reports.size()+ " -----------------\n");
		int count =0;
		for (HashMap<CfgStatement, JavaSourceLocation> lines : reports) {
			sb.append("  Reports: "+(++count));
			boolean first = true;
			
			LinkedList<JavaSourceLocation> locations = new LinkedList<JavaSourceLocation>();
			
			for (Entry<CfgStatement, JavaSourceLocation> line : lines.entrySet()) {
				if (first) {
					sb.append("  In file: " + line.getValue().FileName+"\n");
					first = false;
				}
				JavaSourceLocation lineInfo = line.getValue();
				if (lineInfo==null) {
					System.err.println("Error: line info is broken");
					break;
				}
				
				locations.add(lineInfo);
				
				sb.append("\t line: " + lineInfo.StartLine);
				sb.append(" cloned:" + lineInfo.isCloned);
				sb.append(" noverify:" + lineInfo.isNoVerify);
				sb.append(" infeasible:" + lineInfo.inInfeasibleBlock);
				sb.append("\n");
//				sb.append("\t line: " + line.getValue().StartLine+":"+line.getValue().StartCol+"  "+line.getValue().inInfeasibleBlock+"\n");
				
			}
			
			if (locations.size()>0) {
				String fn = locations.getFirst().FileName;
				Integer[] lineNumbers = new Integer[locations.size()];
				for (int i=0; i<locations.size(); i++) {
					lineNumbers[i]=locations.get(i).StartLine;
				}
				if (!this.sortedReports.containsKey(fn)) {
					this.sortedReports.put(fn, new LinkedHashSet<Integer[]>());
				}
				this.sortedReports.get(fn).add(lineNumbers);
			}
			
		}
		
		return sb.toString();
	}

	public String printAllReports() {
		StringBuilder sb = new StringBuilder();
		
		for (Entry<String, LinkedHashSet<Integer[]>> e : sortedReports.entrySet()) {
			if (e.getValue().size() > 0) {
				sb.append("In file: ");
				sb.append(e.getKey());
				sb.append("\n");
				for (Integer[] list : e.getValue()) {
					sb.append("\t");
					for (Integer i : list) {
						sb.append(i);
						sb.append(" ");
					}
					sb.append("\n");
				}
			}
		}
		
		return sb.toString();
	}
	
}
