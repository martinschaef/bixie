/**
 * 
 */
package bixie.checker.inconsistency_checker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bixie.Options;
import bixie.checker.report.Report;
import bixie.checker.transition_relation.AbstractTransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverFactory;
import bixie.transformation.CallUnwinding;
import bixie.transformation.SingleStaticAssignment;
import bixie.transformation.loopunwinding.AbstractLoopUnwinding;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgAxiom;
import boogie.controlflow.CfgProcedure;

/**
 * @author schaef
 *
 */
public abstract class AbstractChecker implements Runnable {

	protected AbstractControlFlowFactory cff;
	protected CfgProcedure procedure;

	private Report report = null;

	protected HashSet<BasicBlock> feasibleBlocks = new HashSet<BasicBlock>();

	protected Prover prover = null;

	/**
	 * 
	 */
	public AbstractChecker(AbstractControlFlowFactory cff, CfgProcedure p) {
		this.cff = cff;
		this.procedure = p;
	}

	public abstract Report runAnalysis(Prover prover);

	public Report getReport() {
		return this.report;
	}
	
	protected void transformIntoPassiveProgram() {
		this.procedure.pruneUnreachableBlocks();

		CallUnwinding cunwind = new CallUnwinding();
		cunwind.unwindCalls(this.procedure);

		AbstractLoopUnwinding.unwindeLoops(this.procedure);
		this.procedure.pruneUnreachableBlocks();

		SingleStaticAssignment ssa = new SingleStaticAssignment();
		ssa.computeSSA(this.procedure);
		this.procedure.pruneUnreachableBlocks();		
	}

	@Override
	public void run() {
		transformIntoPassiveProgram();
		ProverFactory pf = new bixie.prover.princess.PrincessProverFactory();
		try {			
			if (Options.v().getProverLogPrefix()!=null && !Options.v().getProverLogPrefix().isEmpty()) {
				this.prover = pf.spawnWithLog(Options.v().getProverLogPrefix());	
			} else {
				this.prover = pf.spawn();
			}
			this.report = runAnalysis(this.prover);
		} catch (Throwable e) {
			throw e;
		} finally {
			shutDownProver();
		}
	}

	public void shutDownProver() {
		if (null == this.prover)
			return;
		this.prover.shutdown();
		this.prover = null;
	}

	protected void pushTransitionRelation(Prover prover,
			AbstractTransitionRelation tr) {
		// now assert all proof obligations
		for (Entry<CfgAxiom, ProverExpr> entry : tr.getPreludeAxioms()
				.entrySet()) {
			prover.addAssertion(entry.getValue());
		}

		prover.addAssertion(tr.getRequires());

		for (Entry<BasicBlock, LinkedList<ProverExpr>> entry : tr
				.getProofObligations().entrySet()) {
			for (ProverExpr assertion : entry.getValue()) {
				prover.addAssertion(assertion);
			}
		}
	}
	
	/**
	 * Creates an inverted version of the tr.getReachabilityVariables map
	 * that only includes the subset of blocks in includedBlocks.
	 * @param tr the transition relation of a procedure.
	 * @param includedBlocks the blocks that are to be included in the result.
	 * @return A map from each block in includedBlocks to its reachability variable.
	 */
	protected Map<ProverExpr, BasicBlock> createdInvertedReachabilityVariableMap(AbstractTransitionRelation tr, Set<BasicBlock> includedBlocks) {
		LinkedHashMap<ProverExpr, BasicBlock> uncoveredBlocks = new LinkedHashMap<ProverExpr, BasicBlock>();
		for (Entry<BasicBlock, ProverExpr> entry : tr
				.getReachabilityVariables().entrySet()) {
			if (includedBlocks.contains(entry.getKey())) {
				// ignore the blocks that we are not interested in
				uncoveredBlocks.put(entry.getValue(), entry.getKey());
			}
		}
		return uncoveredBlocks;
	}	

	/**
	 * takes a set of blocks and groups them into subgraphs that are directly connected.
	 * The key is the entry to that subgraph and the value is the set of all blocks
	 * in the graph.
	 * @param blocks the set of blocks that should be grouped.
	 * @return map from a block A to the subgraph G where A is the entry. 
	 */
	protected HashMap<BasicBlock, HashSet<BasicBlock>> groupBlocks(Set<BasicBlock> blocks) {
		//find all blocks in 'blocks' that do not have a predecessor
		//in 'blocks'
		LinkedList<BasicBlock> entries = new LinkedList<BasicBlock>();
		for (BasicBlock b : blocks) {
			boolean has_pre = false;
			for (BasicBlock pre : b.getPredecessors()) {
				if (blocks.contains(pre)) {
					has_pre = true;
					break;
				} 
			}
			if (!has_pre) entries.add(b); 			
		}
		HashMap<BasicBlock, HashSet<BasicBlock>> res = new HashMap<BasicBlock, HashSet<BasicBlock>>();
		for (BasicBlock b : entries) {
			LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
			HashSet<BasicBlock> done = new HashSet<BasicBlock>();
			HashSet<BasicBlock> subgraph = new HashSet<BasicBlock>();
			todo.add(b);
			while (!todo.isEmpty()) {
				BasicBlock c = todo.pop();
				if (blocks.contains(c)) {
					subgraph.add(c);
				}
				for (BasicBlock suc : c.getSuccessors()) {
					if (blocks.contains(suc) && !todo.contains(suc) && !done.contains(suc)) {
						todo.add(suc);
					}
				}
			}
			res.put(b, subgraph);
		}
		return res;
	}
	
	protected void toDot(String filename, Collection<BasicBlock> allBlocks,
			Collection<BasicBlock> blueBlocks, Collection<BasicBlock> redBlocks) {
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(filename), "UTF-8"))) {

			pw.println("digraph dot {");

			for (BasicBlock block : blueBlocks) {
				// Special blocks
				if (redBlocks.contains(block)) {
					pw.println("\""
							+ block.getLabel()
							+ "\" [style=filled, color=red, fillcolor=blue, label=\""
							+ block.getLabel() + "\"]");
				} else {
					pw.println("\"" + block.getLabel()
							+ "\" [style=filled, fillcolor=blue, label=\""
							+ block.getLabel() + "\"]");
				}
			}

			for (BasicBlock block : redBlocks) {
				// Special blocks
				if (!blueBlocks.contains(block)) {
					pw.println("\"" + block.getLabel()
							+ "\" [color=red, label=\"" + block.getLabel()
							+ "\"]");
				}
			}

			for (BasicBlock block : allBlocks) {
				// Regular blocks
				if (!blueBlocks.contains(block) && !redBlocks.contains(block)) {
					pw.println("\"" + block.getLabel() + "\" [label=\""
							+ block.getLabel() + "\"]");
				}
			}

			for (BasicBlock block : allBlocks) {
				for (BasicBlock succ : block.getSuccessors()) {
					if (allBlocks.contains(succ)) {
						pw.println("\"" + block.getLabel() + "\"" + " -> "
								+ "\"" + succ.getLabel() + "\"");
					}
				}
			}

			pw.println("}");

			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
