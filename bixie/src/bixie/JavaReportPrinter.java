/**
 * 
 */
package bixie;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import org.gravy.report.InfeasibleReport;
import org.gravy.report.Report;
import org.gravy.reportprinter.ReportPrinter;

import boogie.ProgramFactory;
import boogie.ast.Attribute;
import boogie.ast.NamedAttribute;
import boogie.ast.expression.literal.IntegerLiteral;
import boogie.ast.expression.literal.StringLiteral;
import boogie.ast.statement.Statement;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 *
 */
public class JavaReportPrinter implements ReportPrinter {

	HashMap<String, LinkedHashSet<Integer>> sortedReports = new HashMap<String, LinkedHashSet<Integer>>();
	
	
	/**
	 * 
	 */
	public JavaReportPrinter() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.gravy.reportprinter.ReportPrinter#printReport(org.gravy.report.Report)
	 */
	@Override
	public void printReport(Report r) {
		if (!(r instanceof InfeasibleReport) ) {
			throw new RuntimeException("Bixie can only work with infeasible code reports!");
		}
		InfeasibleReport ir = (InfeasibleReport)r;
		buildJavaErrorString(ir);
		//if (s!=null && s.length()>0) System.err.println(s);
	}

	public String printAllReports() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, LinkedHashSet<Integer>> e :sortedReports.entrySet() ) {
			if (e.getValue().size()>0) {
				sb.append("In file: ");
				sb.append(e.getKey());
				sb.append("\n");
				LinkedList<Integer> reverse = new LinkedList<Integer>();
				for (Integer i : e.getValue()) {
					reverse.addFirst(i);
				}				
				Collections.sort(reverse);
				for (Integer i : reverse) {
					sb.append("\tline "+i+"\n");
				}
			}
		}
		return sb.toString();
	}
	
	private void buildJavaErrorString(InfeasibleReport ir) {
		
		LinkedList<HashSet<Statement>> infeasibleSubProgs = ir.getInfeasibleSubPrograms();
//		if (infeasibleSubProgs.size()>0) {
//			System.err.println("Found " + infeasibleSubProgs.size() + " candidates. Not all of them might be useful");
//		}
		
//		Set<JavaSourceLocation> goodLocations = readJavaLocationTags(feasibleBlocks);
		
		
//		int i=0;
		for (HashSet<Statement> subprog : infeasibleSubProgs) {
			
//			System.err.println("Subprog "+(i++));
//			for (BasicBlock b : subprog) System.err.println("\t"+b.getLabel());
			//find the first and last line of the infeasible
			//sub program for reporting
			int startLine = -1;
			int endLine = -1;
			String filename="";
			//boolean ignoreSubProg = false;
			
			for (Statement s : subprog) {

				if (this.containsNoVerifyAttribute(s)) {
					//Ignore this report
//					startLine = -1;
//					endLine = -1;
//					filename="";					
//					break;
					continue;
				} else if (this.containsNamedAttribute(s, ProgramFactory.Cloned)) {
					System.err.println("clone.");
					continue;
				}
				

				if (s.getAttributes()!=null) {
					for (Attribute attr : s.getAttributes()) {
						if (attr instanceof NamedAttribute) {
							
							JavaSourceLocation jcl = readSourceLocationFromAttrib(attr);
							if (jcl!=null) {
//									if (goodLocations.contains(jcl)) {
//										System.err.println("Halloooooo!");
//										continue;
//									}
						
								
								filename = jcl.FileName;
								if (filename == null || filename.length()==0) {
									throw new RuntimeException("Could not find debug information! Bixie cannot report anything with this.");
								}
								if (startLine==-1 || jcl.StartLine<startLine) {
								startLine = jcl.StartLine;
								}
								if (endLine==-1 || jcl.EndLine>endLine) {
									endLine = jcl.EndLine;
								}	
							} else {
								System.err.println("Error: mal-formated location tag.");
							}
						}
					}		
				}
					
				
			}
			
			//if (ignoreSubProg) continue;
			
			if (filename=="" || startLine==-1 ) {
				//if there is no code location, then we have nothing to report.
				continue;
			}

			//todo
			if (!sortedReports.containsKey(filename)) {
				sortedReports.put(filename, new LinkedHashSet<Integer>());
			}
			sortedReports.get(filename).add(startLine);
		}
		
		
		
	}
	
	protected class JavaSourceLocation {
		public String FileName = "";
		public int StartLine = -1;
		public int EndLine = -1;
		public int StartCol = -1;
		public int EndCol = -1;
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof JavaSourceLocation) {
				JavaSourceLocation o = (JavaSourceLocation)other;
				return o.FileName.equals(FileName) && o.StartLine==this.StartLine
						&& o.EndLine==this.EndLine && o.StartCol==this.StartCol
						&& o.EndCol==this.EndCol;
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return this.FileName.hashCode()*StartLine*EndLine*StartCol*EndCol;
		}
	}
	
	protected JavaSourceLocation readSourceLocationFromAttrib(Attribute attr) {
		if (attr instanceof NamedAttribute) {			
			NamedAttribute na = (NamedAttribute)attr;
			if (na.getName().equals(ProgramFactory.LocationTag)
					&& na.getValues().length>=3) {
				JavaSourceLocation jcl = null;
				try {
					jcl = new JavaSourceLocation();
					jcl.FileName = ((StringLiteral)na.getValues()[0]).getValue();
					if (na.getValues()[1] instanceof IntegerLiteral) { //else its -1
						jcl.StartLine = Integer.parseInt(((IntegerLiteral)na.getValues()[1]).getValue());
					}
					if (na.getValues()[2] instanceof IntegerLiteral) { //else its -1
						jcl.StartCol = Integer.parseInt(((IntegerLiteral)na.getValues()[2]).getValue());
					}
					if (na.getValues().length>=5) {
						if (na.getValues()[3] instanceof IntegerLiteral) { //else its -1
							jcl.EndLine = Integer.parseInt(((IntegerLiteral)na.getValues()[3]).getValue());
						}
						if (na.getValues()[4] instanceof IntegerLiteral) { //else its -1
							jcl.EndCol = Integer.parseInt(((IntegerLiteral)na.getValues()[4]).getValue());
						}
					}
					return jcl;
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				
			}
		}
		return null;
	}
	
	protected Set<JavaSourceLocation> readJavaLocationTags(Set<BasicBlock> blocks) {		
		HashSet<JavaSourceLocation> sourceLocations = new HashSet<JavaSourceLocation>();
		for (BasicBlock b : blocks) {				
			
			for (CfgStatement s : b.getStatements()) {
				if (s.getAttributes()!=null) {
					for (Attribute attr : s.getAttributes()) {
						JavaSourceLocation jcl = readSourceLocationFromAttrib(attr);
						if (jcl!=null) sourceLocations.add(jcl);
					}		
				}
			}			
		}		
		return sourceLocations;
	}
	
	protected boolean containsNoVerifyAttribute(Statement s) {
		return containsNamedAttribute(s, ProgramFactory.NoVerifyTag);
	}
	
	protected boolean containsNamedAttribute(Statement s, String name) {
		if (s.getAttributes()!=null) {
			for (Attribute attr : s.getAttributes()) {
				if (attr instanceof NamedAttribute) {
					NamedAttribute na = (NamedAttribute)attr;
					if (na.getName().equals(name)) {									
						return true;
					} 
				}
			}		
		}		
		return false;
	}
		
	
}
