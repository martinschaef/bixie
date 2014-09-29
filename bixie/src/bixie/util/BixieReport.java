/**
 * 
 */
package bixie.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.gravy.report.InterpolationInfeasibleReport;
import org.gravy.util.JavaSourceLocation;

import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 *
 */
public class BixieReport {

	public class CustomComparator implements Comparator<InfeasibleMessage> {
	    @Override
	    public int compare(InfeasibleMessage o1, InfeasibleMessage o2) {
	        return o1.firstLine.compareTo(o2.firstLine);
	    }
	}
	
	public class InfeasibleMessage {
		
		public Integer firstLine = -2;
		public String fileName = "";
		public LinkedList<JavaSourceLocation> locations = new LinkedList<JavaSourceLocation>();
		
		public HashSet<Integer> allLines = new HashSet<Integer>();
		public LinkedList<JavaSourceLocation> infeasibleLines = new LinkedList<JavaSourceLocation>();
		public LinkedList<JavaSourceLocation> otherLines = new LinkedList<JavaSourceLocation>();
		
		public InfeasibleMessage(HashMap<CfgStatement, JavaSourceLocation> report) {

			for (Entry<CfgStatement, JavaSourceLocation> line : report.entrySet()) {
				if (firstLine==-2) {
					fileName = line.getValue().FileName;
					firstLine = line.getValue().StartLine;
					
				} else if (line.getValue().StartLine<firstLine) {
					firstLine = line.getValue().StartLine;
				}
				allLines.add(line.getValue().StartLine);
				if (line.getValue().inInfeasibleBlock) {
					this.infeasibleLines.add(line.getValue());
				} else {
					this.otherLines.add(line.getValue());
				}
			}	
			locations.addAll(report.values());
			
		}
		
		public boolean includes(InfeasibleMessage other) {
			return this.allLines.containsAll(other.allLines);
		}
		
	}
	
	public LinkedList<InfeasibleMessage> messages = new LinkedList<BixieReport.InfeasibleMessage>();
	
	public Integer firstLine = -2;
	public String fileName="";
	
	public BixieReport(InterpolationInfeasibleReport ir) {
		LinkedList<HashMap<CfgStatement, JavaSourceLocation>> reports = ir.getReports();		
		
		for (HashMap<CfgStatement, JavaSourceLocation> lines : reports) {
			InfeasibleMessage im = new InfeasibleMessage(lines);
			boolean found = false;
			//make sure that we don't add a message twice
			//and that we only add the longest message
			//if they overlap.
			for (InfeasibleMessage m : new LinkedList<BixieReport.InfeasibleMessage>(this.messages)) {
				if (m.includes(im)) {
					found = true;
					break;
				}
				if (im.includes(m)) {
					this.messages.remove(m);					
				}
			}
			if (!found) {
				this.messages.add(im);
			}
		}
		
		Collections.sort(this.messages, new CustomComparator());
		
		if (this.messages.size()>0) {
			this.firstLine = this.messages.getFirst().firstLine;
			this.fileName = this.messages.getFirst().fileName;
			
		}
		
	}
	
	
	
}
