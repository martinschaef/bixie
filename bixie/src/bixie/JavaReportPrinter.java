/**
 * 
 */
package bixie;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.gravy.GlobalsCache;
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
		System.err.println(buildJavaErrorString(ir));
	}

	
	
	private String buildJavaErrorString(InfeasibleReport ir) {
		StringBuilder sb = new StringBuilder();
		LinkedList<HashSet<Statement>> infeasibleSubProgs = ir.getInfeasibleSubPrograms();
		
//		Set<JavaSourceLocation> goodLocations = readJavaLocationTags(feasibleBlocks);
		
		boolean firstReport = true;
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
					startLine = -1;
					endLine = -1;
					filename="";					
					break;					
				} else if (this.containsNamedAttribute(s, GlobalsCache.cloneAttribute)) {
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
								if (startLine==-1 || jcl.StartLine<startLine) {
								startLine = jcl.StartLine;
								}
								if (endLine==-1 || jcl.EndLine>endLine) {
									endLine = jcl.EndLine;
								}	
							}
						}
					}		
				}
					
				
			}
			
			//if (ignoreSubProg) continue;
			
			if (filename=="" || startLine==-1 || endLine==-1) {
				//if there is no code location, then we have nothing to report.
				continue;
			}
			if (firstReport) {
				firstReport = false;
				sb.append("\nInfeasible Code:\n");
			}
			sb.append("in file: "+filename);
			sb.append("\tfrom "+startLine + " to " + endLine+ "\n");
		}
		if (!firstReport) sb.append("\n");
		
		return sb.toString();
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
					jcl.StartLine = Integer.parseInt(((IntegerLiteral)na.getValues()[1]).getValue());
					jcl.StartCol = Integer.parseInt(((IntegerLiteral)na.getValues()[2]).getValue());
					if (na.getValues().length>=5) {						
						jcl.EndLine = Integer.parseInt(((IntegerLiteral)na.getValues()[3]).getValue());
						jcl.EndCol = Integer.parseInt(((IntegerLiteral)na.getValues()[4]).getValue());										
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
