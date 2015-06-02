/**
 * 
 */
package bixie.checker.inconsistency_checker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bixie.checker.report.Report;
import bixie.checker.transition_relation.Atva15TransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverResult;
import bixie.prover.princess.PrincessProver;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgAxiom;
import boogie.controlflow.CfgProcedure;

/**
 * @author schaef
 * 
 * Inconsistent code detection algorithm based on greedy Cfg covering.
 * It uses the Cfg-theory plugin of Princess.
 * 
 * The algorithm is described in the papers:
 * - Infeasible code detection (VSTTE'12)
 * - A theory for control-flow graph exploration (ATVA'13)
 *
 */
public class GreedyCfgChecker extends AbstractChecker {

	/**
	 * @param cff
	 * @param p
	 */
	public GreedyCfgChecker(AbstractControlFlowFactory cff, CfgProcedure p) {
		super(cff, p);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see bixie.checker.checker.AbstractChecker#checkSat(bixie.prover.Prover,
	 * boogie.controlflow.AbstractControlFlowFactory,
	 * boogie.controlflow.CfgProcedure)
	 */
	@Override
	public Report runAnalysis(Prover prover) {

		Atva15TransitionRelation tr = new Atva15TransitionRelation(this.procedure,
				this.cff, prover);

		// Statistics.HACK_effectualSetSize = tr.getEffectualSet().size();

		LinkedHashMap<ProverExpr, ProverExpr> ineffFlags = new LinkedHashMap<ProverExpr, ProverExpr>();

		for (BasicBlock block : tr.getEffectualSet()) {
			ProverExpr v = tr.getReachabilityVariables().get(block);
			ineffFlags.put(v, prover.mkVariable("" + v + "_flag",
					prover.getBooleanType()));
		}

		/* Assert the transition relation of the procedure.
		 */
		prover.push();
		this.pushTransitionRelation(prover, tr);
		prover.addAssertion(tr.getEnsures());	
		
				
		// construct the inverted reachabilityVariables which is used later
		// to keep track of what has been covered so far.
		Map<ProverExpr, BasicBlock> blocksToCover = createdInvertedReachabilityVariableMap(
				tr, new HashSet<BasicBlock>(tr.getReachabilityVariables()
						.keySet()));

		/* ===== main algorithm ====
		 * Two steps: 
		 * In the first step, push the assertion flag and check which blocks
		 * have feasible executions. Then pop the flag to disable all assertions
		 * and check what blocks can now be reached.
		 * 
		 * Step 1:
		 */
		prover.push();
		prover.addAssertion(prover.mkNot(tr.assertionFlag));
		HashSet<BasicBlock> coveredBlocks = new HashSet<BasicBlock>();
		coveredBlocks.addAll(coverBlocks(blocksToCover, tr, ineffFlags));
		
		// coverBlocks returns the set of all feasible blocks.
		this.feasibleBlocks = new HashSet<BasicBlock>(coveredBlocks);

		/* Step 2:
		 * Pop the tr.assertionFlag. An re-run coverBlocks to cover everything
		 * that has a feasible execution if assertions are ignored.
		 */
		prover.pop();
		coveredBlocks.addAll(coverBlocks(blocksToCover, tr, ineffFlags));
	
		/* Pop the transition relation. */
		prover.pop();
		
		/* ===== End of the main algorithm ====
		 * everything that was not covered in either of the iterations is clearly unreachable.
		 */
		HashSet<BasicBlock> unreachable = new HashSet<BasicBlock>(blocksToCover.values());
		
		/* All blocks that are covered in the second round - that is, the blocks 
		 * that are in coveredBlocks but not in feasibleBlocks - are potentially dangerous,
		 * because their inconsistency contains an assertion.  
		 */
		HashSet<BasicBlock> dangerous = new HashSet<BasicBlock>(coveredBlocks);		
		dangerous.removeAll(this.feasibleBlocks);
		
		/*
		 * TODO: Shall we remove this? Experiments show that bwd_dangerous is
		 * very rare. Further, its rather expensive to compute and doesn't add
		 * much value.
		 */
		HashMap<BasicBlock, HashSet<BasicBlock>> subGraphs = groupBlocks(dangerous);
		HashSet<BasicBlock> fwd_dangerous = new HashSet<BasicBlock>();
		HashSet<BasicBlock> bwd_dangerous = new HashSet<BasicBlock>();		
		for (Entry<BasicBlock, HashSet<BasicBlock>> entry : subGraphs.entrySet()) {
			BasicBlock b = entry.getKey();
			if (forwardReachable(b, tr, ineffFlags)) {				
				fwd_dangerous.addAll(subGraphs.get(b));
			} else {
				bwd_dangerous.addAll(subGraphs.get(b));
			}
		}		

		Report report = new Report(tr);
		report.reportInconsistentCode(0, fwd_dangerous);
		report.reportInconsistentCode(1, bwd_dangerous);
		report.reportInconsistentCode(2, unreachable);

		return report;
	}

	/**
	 * Tries to cover elements in blocks by covering all blocks in the 
	 * effectual set of the CFG using Princess' cfg-theory. 
	 * The map ineffFlags contains one helper variable for each block in the
	 * effectual set. 
	 * @param blocks Map from SMT variables to BasicBlocks.
	 * @param tr Transition relation of the analyzed procedure.
	 * @param ineffFlags Map from SMT variables in blocks to helper variables for Princess.
	 * @return The set of blocks that could be covered.
	 */
	protected Set<BasicBlock> coverBlocks(Map<ProverExpr, BasicBlock> blocks,
			Atva15TransitionRelation tr,
			LinkedHashMap<ProverExpr, ProverExpr> ineffFlags) {

		Set<BasicBlock> coveredBlocks = new HashSet<BasicBlock>();

		int threshold = ineffFlags.size();
		// hint for the greedy cover algorithm about
		// how many blocks could be covered in one query.
		if (threshold > 1)
			threshold = threshold / 2;

		while (threshold >= 1 && !ineffFlags.isEmpty()) {
			prover.push();

			coveredBlocks.addAll(coverBlocksWithThreshold(blocks, tr,
					ineffFlags, threshold));

			prover.pop();

			if (threshold == 1 || ineffFlags.isEmpty())
				break;

			do {
				threshold = (int) Math.ceil((double) threshold / 2.0);
			} while (threshold > ineffFlags.size());

		}

		return coveredBlocks;
	}

	/**
	 * Sub-step of coverBlocks. Finds all paths that contain at least
	 * 'threshold' previously uncovered blocks. Setting the threshold is
	 * more efficient looking for arbitrary new paths.
	 * @param blocks Map from SMT variables to BasicBlocks.
	 * @param tr Transition relation of the analyzed procedure.
	 * @param ineffFlags Map from SMT variables in blocks to helper variables for Princess.
	 * @param threshold lower bound for the number of new blocks that have to be
	 *                  covered per path. 
	 * @return The set of blocks that could be covered for the given threshold.
	 */
	protected Set<BasicBlock> coverBlocksWithThreshold(
			Map<ProverExpr, BasicBlock> blocks, Atva15TransitionRelation tr,
			LinkedHashMap<ProverExpr, ProverExpr> ineffFlags, int threshold) {

		// setup the CFG module
		LinkedList<ProverExpr> remainingBlockVars = new LinkedList<ProverExpr>();
		LinkedList<ProverExpr> remainingIneffFlags = new LinkedList<ProverExpr>();
		for (Entry<ProverExpr, ProverExpr> entry : ineffFlags.entrySet()) {
			remainingBlockVars.add(entry.getKey());
			remainingIneffFlags.add(entry.getValue());
		}

		((PrincessProver) prover).setupCFGPlugin(tr.getProverDAG(),
				remainingBlockVars, remainingIneffFlags, threshold);

		Set<BasicBlock> coveredBlocks = new HashSet<BasicBlock>();

		ProverResult res = prover.checkSat(true);

		while (res == ProverResult.Sat) {

			LinkedList<ProverExpr> trueInModel = new LinkedList<ProverExpr>();
			LinkedList<ProverExpr> flagsToAssert = new LinkedList<ProverExpr>();

			for (Entry<ProverExpr, BasicBlock> entry : blocks.entrySet()) {
				final ProverExpr pe = entry.getKey();
				if (prover.evaluate(pe).getBooleanLiteralValue()) {
					trueInModel.add(pe);
					ProverExpr flag = ineffFlags.get(pe);
					if (flag != null) {
						flagsToAssert.add(flag);
					}
					ineffFlags.remove(pe);
				}
			}

			for (ProverExpr e : trueInModel) {
				coveredBlocks.add(blocks.get(e));
				blocks.remove(e);
			}

			prover.addAssertion(prover.mkAnd(flagsToAssert
					.toArray(new ProverExpr[flagsToAssert.size()])));

			res = prover.checkSat(true);
			// prover.checkSat(false);
			// res = prover.getResult(TIME_LIMIT);

			if (res == ProverResult.Running) {
				// the coverage algorithm could not make progress within the
				// given time limit. Falling back to the new algorithms.
				// prover.stop();
				// prover.pop();
				// if (phase==0) prover.pop(); //pop again if we're still in
				// phase 0
				// coverHardToGetBlocks(tr, coveredBlocks, phase);
				// return;
			}

		}
		return coveredBlocks;
	}

	/** 
	 * Checks if b is reachable in tr. 
	 * @param b The block that should be reachable
	 * @param tr The procedure in which b should be reachable
	 * @param ineffFlags The flags for the solver.
	 * @return True, if b is reachable in tr. Otherwise False.
	 */
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
	
}
