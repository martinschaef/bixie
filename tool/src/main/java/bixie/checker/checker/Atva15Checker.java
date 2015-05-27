/**
 * 
 */
package bixie.checker.checker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import org.joogie.cfgPlugin.Util.Dag;

import util.Log;
import ap.parser.IFormula;
import bixie.checker.report.Report;
import bixie.checker.util.Statistics;
import bixie.checker.verificationcondition.Atva15TransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverResult;
import bixie.prover.princess.PrincessProver;
import bixie.transformation.CallUnwinding;
import bixie.transformation.SingleStaticAssignment;
import bixie.transformation.loopunwinding.AbstractLoopUnwinding;
import bixie.util.StopWatch;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgAxiom;
import boogie.controlflow.CfgProcedure;

/**
 * @author schaef
 * 
 */
public class Atva15Checker extends AbstractChecker {	
	
	protected HashSet<BasicBlock> dangerousBlocks;

	protected long overhead_time = 0L;
	private StopWatch overheadTimer;
	
	/**
	 * @param cff
	 * @param p
	 */
	public Atva15Checker(AbstractControlFlowFactory cff, CfgProcedure p) {
		super(cff, p);

		

		p.pruneUnreachableBlocks();

		CallUnwinding cunwind = new CallUnwinding();
		cunwind.unwindCalls(p);

		AbstractLoopUnwinding.unwindeLoops(p);
		p.pruneUnreachableBlocks();

		SingleStaticAssignment ssa = new SingleStaticAssignment();
		ssa.computeSSA(p);

		p.pruneUnreachableBlocks();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * bixie.checker.infeasiblecode.AbstractInfeasibleCodeDetection#checkSat(org
	 * .gravy.prover.Prover,
	 * bixie.checker.verificationcondition.CfgTransitionRelation)
	 */
	@Override
	public Report checkSat(Prover prover, AbstractControlFlowFactory cff, CfgProcedure p) {

		this.overhead_time = 0L;
		
		Atva15TransitionRelation tr = new Atva15TransitionRelation(procedure, cff, prover);

		// generate ineff flags; this map is also used to keep
		// track of the remaining uncovered blocks
		LinkedHashMap<ProverExpr, ProverExpr> ineffFlags = new LinkedHashMap<ProverExpr, ProverExpr>();

		Statistics.HACK_effectualSetSize = tr.getEffectualSet().size();

		for (BasicBlock block : tr.getEffectualSet()) {
			ProverExpr v = tr.getReachabilityVariables().get(block);
			ineffFlags.put(v, prover.mkVariable("" + v + "_flag",
					prover.getBooleanType()));
		}

		prover.push();
		this.pushTransitionRelation(prover, tr);

		HashSet<BasicBlock> blocks2cover = new HashSet<BasicBlock>(tr
				.getReachabilityVariables().keySet());

		prover.addAssertion(tr.getEnsures());
		// compute the feasible path cover under the given postcondition
		computePathCover2(prover, tr,
				new LinkedHashMap<ProverExpr, ProverExpr>(ineffFlags),
				blocks2cover);
		blocks2cover.removeAll(feasibleBlocks);

		// this set is empty for infeasible code detection.
		infeasibleBlocksUnderPost = new HashSet<BasicBlock>();

		infeasibleBlocks = new HashSet<BasicBlock>(tr
				.getReachabilityVariables().keySet());
		infeasibleBlocks.removeAll(feasibleBlocks);

		// !! THIS IS THE NEW ATVA STUFF
		HashSet<BasicBlock> unreachableBlocks = new HashSet<BasicBlock>(
				infeasibleBlocks);
		unreachableBlocks.removeAll(this.dangerousBlocks);

		StringBuilder sb = new StringBuilder();
		sb.append("We found in" + this.procedure.getProcedureName() + ":\n");
		sb.append("Dangerous   blocks:" + this.dangerousBlocks.size() + "\n");
		sb.append("Unreachable blocks:" + unreachableBlocks.size() + "\n");

		// pop the transition relation from the prover stack
		prover.pop();

		//group dangerousBlocks in subgraphs
		HashMap<BasicBlock, HashSet<BasicBlock>> sg = groupBlocks(this.dangerousBlocks);

		HashSet<BasicBlock> fwd_dangerous = new HashSet<BasicBlock>();
		HashSet<BasicBlock> bwd_dangerous = new HashSet<BasicBlock>();
		
		for (BasicBlock b : sg.keySet()) {
			if (forwardReachable(b, tr, ineffFlags)) {
				sb.append("\t" + b.getLabel() + " Forward\n");
				fwd_dangerous.addAll(sg.get(b));
			} else {
				sb.append("\t" + b.getLabel() + " Backward\n");
				bwd_dangerous.addAll(sg.get(b));
			}
		}
		sb.append("==================================");
//		System.err.println(sb.toString());

		// this is just for debugging purposes
//		if (Options.v().getChecker() == 4) {
//			return new InterpolationInfeasibleReport(this.cff, atr,
//					this.feasibleBlocks, this.infeasibleBlocks);
//		}
		
		Report report = new Report(tr);
		report.reportInconsistentCode(0, fwd_dangerous);
		report.reportInconsistentCode(1, bwd_dangerous);
		report.reportInconsistentCode(2, unreachableBlocks);
		
		return report;
	}

	/**
	 * takes a set of blocks in groups them into subgraphs that are directly connected.
	 * The key is the entry to that subgraph and the value is the set of all blocks
	 * in the graph.
	 * @param blocks
	 * @return
	 */
	private HashMap<BasicBlock, HashSet<BasicBlock>> groupBlocks(Set<BasicBlock> blocks) {
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
	
	private boolean forwardReachable(BasicBlock b, Atva15TransitionRelation tr,
			LinkedHashMap<ProverExpr, ProverExpr> ineffFlags) {
		
		if (tr.getProcedure().getRootNode()==b) return true;
		
		prover.push();
		// assert the basic prelude
		for (Entry<CfgAxiom, ProverExpr> entry : tr.getPreludeAxioms()
				.entrySet()) {
			prover.addAssertion(entry.getValue());
		}
		prover.addAssertion(tr.getRequires());
		// enable the assertions
		prover.addAssertion(prover.mkNot(tr.assertionFlag));
		// collect all blocks between b and root (including b)
		LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
		HashSet<BasicBlock> done = new HashSet<BasicBlock>();
		todo.addAll(b.getPredecessors());
		// todo.add(b.);
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pop();
			done.add(current);
			for (BasicBlock pre : current.getPredecessors()) {
				if (!todo.contains(pre) && !done.contains(pre)) {
					todo.add(pre);
				}
			}
		}
		// check if these blocks have a feasible path.
		for (Entry<BasicBlock, LinkedList<ProverExpr>> entry : tr
				.getProofObligations().entrySet()) {
			if (done.contains(entry.getKey())) {
				for (ProverExpr assertion : entry.getValue()) {
					prover.addAssertion(assertion);
				}
			} else {
				if (entry.getKey() != b && !b.getSuccessors().contains(entry.getKey())) {
					prover.addAssertion(prover.mkNot(tr
							.getReachabilityVariables().get(entry.getKey())));
				}
			}
		}
		// finally, add the assertions for b.
		for (ProverExpr assertion : tr.getProofObligations().get(b)) {
			prover.addAssertion(assertion);
		}

		ProverResult res = this.prover.checkSat(true);
		prover.pop();

		if (res == ProverResult.Sat) {
			return true;
		}
		return false;
	}


	/**
	 * Computes a maximal feasible path cover for the BasicBlocks in
	 * blocks2cover and returns the set of all covered nodes.
	 * 
	 * @param prover
	 * @param tr
	 * @param ineffFlags
	 *            flags for the effectual blocks in blocks2cover
	 * @param blocks2cover
	 *            set of nodes for which the cover can be computed
	 * @return subset of nodes in block2cover that can be covered.
	 */
	protected void computePathCover2(Prover prover,
			Atva15TransitionRelation tr,
			LinkedHashMap<ProverExpr, ProverExpr> ineffFlags,
			HashSet<BasicBlock> blocks2cover) {

		Dag<IFormula> vcdag = tr.getProverDAG();

		HashSet<BasicBlock> coveredBlocks = new HashSet<BasicBlock>();

		// construct the inverted reachabilityVariables which is used later
		// to keep track of what has been covered so far.
		LinkedHashMap<ProverExpr, BasicBlock> uncoveredBlocks = new LinkedHashMap<ProverExpr, BasicBlock>();
		for (Entry<BasicBlock, ProverExpr> entry : tr
				.getReachabilityVariables().entrySet()) {
			if (blocks2cover.contains(entry.getKey())) {
				// ignore the blocks that we are not interested in
				uncoveredBlocks.put(entry.getValue(), entry.getKey());
			}
		}

		int phase = 0;
		while (phase < 2) {
			if (phase == 0) {
				// phase 0: enable all assertions.
				prover.push();
				// enable assertions.
				prover.addAssertion(prover.mkNot(tr.assertionFlag));
			} else if (phase == 1) {
				// everything covered so far is guarnateed to be feasible
				this.feasibleBlocks = new HashSet<BasicBlock>(coveredBlocks);
				// pop the assertion from the first phase.
				prover.pop();
				 this.overheadTimer = StopWatch.getInstanceAndStart();
			} else {
				throw new RuntimeException("no no no");
			}
			phase++;

			int threshold = ineffFlags.size();
			// hint for the greedy cover algorithm about
			// how many blocks could be covered in one query.
			if (threshold > 1)
				threshold = threshold / 2;

			while (threshold >= 1 && !ineffFlags.isEmpty()) {
				prover.push();

				// setup the CFG module
				LinkedList<ProverExpr> remainingBlockVars = new LinkedList<ProverExpr>();
				LinkedList<ProverExpr> remainingIneffFlags = new LinkedList<ProverExpr>();
				for (Entry<ProverExpr, ProverExpr> entry : ineffFlags
						.entrySet()) {
					remainingBlockVars.add(entry.getKey());
					remainingIneffFlags.add(entry.getValue());
				}

				((PrincessProver) prover).setupCFGPlugin(vcdag,
						remainingBlockVars, remainingIneffFlags, threshold);

				if (bixie.Options.v().getDebugMode()) {
					Log.info("Number of remaining blocks " + ineffFlags.size());
				}

				// Query the feasible paths for this setup
				ProverResult res = prover.checkSat(true);
				while (res == ProverResult.Sat) {

					if (bixie.Options.v().getDebugMode()) {
						Log.info("Prover returns " + res.toString());
					}

					LinkedList<ProverExpr> covered = new LinkedList<ProverExpr>();
					LinkedList<ProverExpr> flagsToAssert = new LinkedList<ProverExpr>();

					for (Entry<ProverExpr, BasicBlock> entry : uncoveredBlocks
							.entrySet()) {
						final ProverExpr pe = entry.getKey();
						if (prover.evaluate(pe).getBooleanLiteralValue()) {
							covered.add(pe);
							ProverExpr flag = ineffFlags.get(pe);
							if (flag != null)
								flagsToAssert.add(flag);
						}
					}

					for (ProverExpr e : covered) {
						ineffFlags.remove(e);
						coveredBlocks.add(uncoveredBlocks.get(e));
						uncoveredBlocks.remove(e);
					}

					prover.addAssertion(prover.mkAnd(flagsToAssert
							.toArray(new ProverExpr[flagsToAssert.size()])));

					if (bixie.Options.v().getDebugMode()) {
						Log.info("Number of remaining blocks "
								+ ineffFlags.size());
					}

					res = prover.checkSat(true);
//					prover.checkSat(false);
//					res = prover.getResult(TIME_LIMIT);
				}

				if (res == ProverResult.Running) {
//					 the coverage algorithm could not make progress within the
//					 given time limit. Falling back to the new algorithms.
//					 prover.stop();
//					 prover.pop();
//					 if (phase==0) prover.pop(); //pop again if we're still in phase 0
//					 coverHardToGetBlocks(tr, coveredBlocks, phase);
//					 return;
				}

				prover.pop();

				if (threshold == 1 || ineffFlags.isEmpty())
					break;

				do {
					threshold = (int) Math.ceil((double) threshold / 2.0);
				} while (threshold > ineffFlags.size());

			}
		}
		
		if (this.overheadTimer != null) {
			this.overhead_time = this.overheadTimer.stop();
		}
		
		// new remove all blocks that have been covered in the first round.
		coveredBlocks.removeAll(this.feasibleBlocks);
		this.dangerousBlocks = new HashSet<BasicBlock>(coveredBlocks);
		return;
	}


}