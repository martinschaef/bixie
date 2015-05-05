/**
 * 
 */
package bixie.checker.report;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import util.Log;
import bixie.checker.faultlocalization.FaultLocalizationThread;
import bixie.checker.util.SourceLine;
import bixie.checker.util.SourceLocation;
import bixie.checker.verificationcondition.AbstractTransitionRelation;
import boogie.ProgramFactory;
import boogie.ast.Attribute;
import boogie.ast.NamedAttribute;
import boogie.ast.statement.Statement;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 *
 */
public class Report {

	protected Map<Integer, Set<Set<BasicBlock>>> inconsistentBlocks = new LinkedHashMap<Integer, Set<Set<BasicBlock>>>(); 
	protected AbstractTransitionRelation tr;	
	
	protected Map<Integer, List<FaultExplanation>> faultExplanations = new LinkedHashMap<Integer, List<FaultExplanation>>();
	
	public Report(AbstractTransitionRelation tr) {
		this.tr = tr;
	}
	
	/**
	 * Report inconsistent code with severity level as suggested in the ATVA15 paper.
	 * Fault localization will be applied for each severity level individually. 
	 * @param severity Severity level of inconsistent blocks with 0 being the highest
	 * @param inconsistentBlocks Set of inconsistent codes to report
	 */
	public void reportInconsistentCode(Integer severity, Set<BasicBlock> inconsistentBlocks) {
		//Split the inconsistentBlocks into connected components.
		//Each connected component may be inconsistent for a separate
		//reason.
		Set<Set<BasicBlock>> inconsistentComponenets = findConnectedComponents(inconsistentBlocks);
		//remove the components that contain a noVerify tag.
		removeSkippedComponents(inconsistentComponenets);
		
		this.inconsistentBlocks.put(severity, inconsistentComponenets);
	}
	
	/**
	 * Returns the fault explanations generated by the fault localization per severity level.
	 * This map is empty if runFaultLocalization has not been called before.
	 * @return
	 */
	public Map<Integer, List<FaultExplanation>> getReports() {
		return this.faultExplanations;
	}
	
	/**
	 * Apply fault localization from the FSE13 paper to all reported 
	 * inconsistencies.
	 * @param tr 
	 */
	public void runFaultLocalization() {		
		for (Entry<Integer, Set<Set<BasicBlock>>> entry : this.inconsistentBlocks.entrySet()) {
			for (Set<BasicBlock> inconsistency : entry.getValue()) {
				try {
					runFaultLocalization(entry.getKey(), inconsistency);
				} catch (Throwable e) {
					return;
				}
			}
		}
	}
	
	private void runFaultLocalization(Integer severity, Set<BasicBlock> inconsistency) throws Throwable {
		if (inconsistency.isEmpty()) {
			return;
		}
		FaultLocalizationThread flt = new FaultLocalizationThread(this.tr, inconsistency);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future<?> future = executor.submit(flt);
		try {
			if (bixie.Options.v().getTimeout() > 0) {
				future.get(bixie.Options.v().getTimeout(), TimeUnit.SECONDS);
			} else {
				future.get();
			}
			List<Map<CfgStatement, SourceLocation>> reportedLines = flt.getReports();
			for (Map<CfgStatement, SourceLocation> lines : reportedLines) {
				if (lines.size()>0) {					
					if (!this.faultExplanations.containsKey(severity)) {
						this.faultExplanations.put(severity, new LinkedList<FaultExplanation>());
					}
					this.faultExplanations.get(severity).add(new FaultExplanation(lines));
				}
			}			
		} catch (TimeoutException e) {
			Log.error("fault localization timeout.");
			throw(e);
		} catch (OutOfMemoryError e) {
			throw(e);
		} catch (Throwable e) {
			e.printStackTrace();
			throw(e);
		} finally {
			if (flt != null) {
				flt.shutDownProver();
			}
			if (!future.isDone()) {
				future.cancel(true);
			}
			executor.shutdown();
		}	
		//TODO: if the fault localization failed,
		// we can still return the lines of the 
		// inconsistent blocks that we have.
	}
	

			
	protected LinkedList<Statement> collectStatements(AbstractControlFlowFactory cff, Set<BasicBlock> blocks) {
		LinkedList<Statement> astStatements = new LinkedList<Statement>();
		for (BasicBlock b : blocks) {
			for (CfgStatement s : b.getStatements()) {
				Statement ast_stmt = cff.findAstStatement(s);
				if (ast_stmt!=null && ast_stmt.getLocation()!=null) {
					astStatements.add(ast_stmt);
				} else {
					//for debugging only
				}
			}
		}
		return astStatements;
	}
	
	/**
	 * Returns the set of connected components for a given
	 * set of blocks.
	 * @param blocks
	 * @return
	 */
	private Set<Set<BasicBlock>> findConnectedComponents(
			Set<BasicBlock> blocks) {
		Set<Set<BasicBlock>> components = new HashSet<Set<BasicBlock>>();
		LinkedList<BasicBlock> allblocks = new LinkedList<BasicBlock>();
		allblocks.addAll(blocks);
		while (!allblocks.isEmpty()) {
			HashSet<BasicBlock> subprog = new HashSet<BasicBlock>();
			LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
			todo.add(allblocks.pop());

			while (!todo.isEmpty()) {
				BasicBlock current = todo.pop();
				allblocks.remove(current);
				subprog.add(current);
				for (BasicBlock b : current.getPredecessors()) {
					if (!subprog.contains(b) && !todo.contains(b)
							&& allblocks.contains(b)) {
						todo.add(b);
					}
				}
				for (BasicBlock b : current.getSuccessors()) {
					if (!subprog.contains(b) && !todo.contains(b)
							&& allblocks.contains(b)) {
						todo.add(b);
					}
				}
			}
			if (subprog.size() > 0) {
				components.add(subprog);
			}
		}
		return components;
	}	
		
	/**
	 * Takes a set of connected components of blocks and removes
	 * those components that contain a noVerify tag, or where every
	 * block has a 'cloned' tag.
	 * @param components
	 * @return
	 */
	private void removeSkippedComponents(Set<Set<BasicBlock>> components) {
		for (Set<BasicBlock> component : new HashSet<Set<BasicBlock>>(components)) {
			boolean allSkipped = true;
			for (BasicBlock b : component) {
				if (containsNamedAttribute(b, ProgramFactory.NoVerifyTag)) {
					components.remove(component);
					break;
				}
				if (!containsNamedAttribute(b, ProgramFactory.Cloned)) {
					allSkipped = false;
				}
			}
			if (allSkipped) {
				components.remove(component);
			}
		}
	}
	
	protected boolean containsNoVerifyAttribute(BasicBlock b) {
		return containsNamedAttribute(b, ProgramFactory.NoVerifyTag);
	}
	
	protected boolean containsNamedAttribute(BasicBlock b, String name) {
		for (CfgStatement s : b.getStatements()) {
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
		}
		return false;
	}
	
	
	/**
	 * Class that holds the result of the fault localization.
	 * @author schaef
	 *
	 */
	public class FaultExplanation {		
		public Integer firstLine = -2;
		public String fileName = "";
		public LinkedList<SourceLine> locations = new LinkedList<SourceLine>();
		
		public HashSet<Integer> allLines = new HashSet<Integer>();
		public LinkedList<SourceLine> infeasibleLines = new LinkedList<SourceLine>();
		public LinkedList<SourceLine> otherLines = new LinkedList<SourceLine>();
		
		public FaultExplanation(Map<CfgStatement, SourceLocation> report) {

			for (Entry<CfgStatement, SourceLocation> line : report.entrySet()) {
				if (firstLine==-2) {
					fileName = line.getValue().FileName;
					firstLine = line.getValue().StartLine;
					
				} else if (line.getValue().StartLine<firstLine) {
					firstLine = line.getValue().StartLine;
				}
				allLines.add(line.getValue().StartLine);
				
				SourceLine sl = new SourceLine(line.getValue());
				
				if (line.getValue().inInfeasibleBlock) {
					this.infeasibleLines.add(sl);
				} else {
					this.otherLines.add(sl);
				}
				this.locations.add(sl);
				
			}				
		}
		
		public boolean includes(FaultExplanation other) {
			return this.allLines.containsAll(other.allLines);
		}
		
	}
	
}
