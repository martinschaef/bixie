/**
 * 
 */
package bixie.checker.checker;

import java.util.Collection;
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
import bixie.checker.verificationcondition.Nfm15TransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverFactory;
import bixie.prover.ProverResult;
import bixie.prover.princess.PrincessProver;
import bixie.transformation.CallUnwinding;
import bixie.transformation.SingleStaticAssignment;
import bixie.transformation.loopunwinding.AbstractLoopUnwinding;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgProcedure;

/**
 * @author schaef
 *
 */
public class Nfm15Checker extends
		AbstractChecker {

	/**
	 * @param cff
	 * @param p
	 */
	public Nfm15Checker(AbstractControlFlowFactory cff,
			CfgProcedure p) {
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


	/* (non-Javadoc)
	 * @see bixie.checker.infeasiblecode.AbstractInfeasibleCodeDetection#checkSat(bixie.checker.prover.Prover, bixie.checker.verificationcondition.CfgTransitionRelation)
	 */
	@Override
	public Report checkSat(Prover prover, AbstractControlFlowFactory cff,
			CfgProcedure p) {
		Nfm15TransitionRelation tr = new Nfm15TransitionRelation(
				p, cff, prover);
		
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

		HashSet<BasicBlock> blocks2cover = new HashSet<BasicBlock>(tr.getReachabilityVariables().keySet()); 
		
		prover.addAssertion(tr.getEnsures());
		//compute the feasible path cover under the given postcondition
		feasibleBlocks = new HashSet<BasicBlock>(computePathCover2(prover, tr, ineffFlags, blocks2cover));
		blocks2cover.removeAll(feasibleBlocks);
		
		//this set is empty for infeasible code detection.
		infeasibleBlocksUnderPost = new HashSet<BasicBlock>();
		
		infeasibleBlocks = new HashSet<BasicBlock>(tr.getReachabilityVariables().keySet());
		infeasibleBlocks.removeAll(feasibleBlocks);
		//this is just for debugging purposes
		Report report = new Report(tr);
		report.reportInconsistentCode(0, this.infeasibleBlocks);
		return report;
	}
		
	
	
	private int TIME_LIMIT = 8000; //millisecs
	/**
	 * Computes a maximal feasible path cover for the BasicBlocks 
	 * in blocks2cover and returns the set of all covered nodes.
	 * @param prover
	 * @param tr
	 * @param ineffFlags flags for the effectual blocks in blocks2cover
	 * @param blocks2cover set of nodes for which the cover can be computed
	 * @return subset of nodes in block2cover that can be covered.
	 */
	protected Collection<BasicBlock> computePathCover2(Prover prover, Nfm15TransitionRelation tr,
			LinkedHashMap<ProverExpr, ProverExpr> ineffFlags, HashSet<BasicBlock> blocks2cover) {

		Dag<IFormula> vcdag = tr.getProverDAG();

		HashSet<BasicBlock> coveredBlocks = new HashSet<BasicBlock>();
		
		// construct the inverted reachabilityVariables which is used later
		// to keep track of what has been covered so far.
		LinkedHashMap<ProverExpr, BasicBlock> uncoveredBlocks = new LinkedHashMap<ProverExpr, BasicBlock>();
		for (Entry<BasicBlock, ProverExpr> entry : tr
				.getReachabilityVariables().entrySet()) {
			if (blocks2cover.contains(entry.getKey())) {
				//ignore the blocks that we are not interested in
				uncoveredBlocks.put(entry.getValue(), entry.getKey());
			}	
		}

		
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
			for (Entry<ProverExpr, ProverExpr> entry : ineffFlags.entrySet()) {
				remainingBlockVars.add(entry.getKey());
				remainingIneffFlags.add(entry.getValue());
			}

			((PrincessProver) prover).setupCFGPlugin(vcdag, remainingBlockVars,
					remainingIneffFlags, threshold);

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
					coveredBlocks.add(uncoveredBlocks.get(e) );
					uncoveredBlocks.remove(e);
				}

				prover.addAssertion(prover.mkAnd(flagsToAssert
						.toArray(new ProverExpr[flagsToAssert.size()])));

				if (bixie.Options.v().getDebugMode()) {
					Log.info("Number of remaining blocks " + ineffFlags.size());				
				}

				prover.checkSat(false);			 
				res = prover.getResult(TIME_LIMIT);
			}

			if (res==ProverResult.Running) {
				//the coverage algorithm could not make progress within the 
				//given time limit. Falling back to the new algorithms.
				prover.stop();
				prover.pop();
				return coverHardToGetBlocks(tr, coveredBlocks);
			}
			
			prover.pop();

			if (threshold == 1 || ineffFlags.isEmpty())
				break;

			do {
				threshold = (int) Math.ceil((double) threshold / 2.0);
			} while (threshold > ineffFlags.size());

		}
		return coveredBlocks;
	}
	
	private Set<BasicBlock> coverHardToGetBlocks(Nfm15TransitionRelation tr, Set<BasicBlock> covered) {
		Set<BasicBlock> result = new HashSet<BasicBlock>(covered);
		//pop the old transition relation
		prover.shutdown();
		ProverFactory pf = new bixie.prover.princess.PrincessProverFactory();
		prover = pf.spawn();
		
		//get the set of blocks that still need to be covered.		
		Set<BasicBlock> notCovered = new HashSet<BasicBlock>(tr.getReachabilityVariables().keySet());
		notCovered.removeAll(covered);
		
		if (notCovered.isEmpty()) {
			throw new RuntimeException("this must not happen");
		}
		

		JodChecker2 jodcheck = new JodChecker2(cff, procedure, prover);
		Nfm15TransitionRelation jtr = new Nfm15TransitionRelation(procedure, cff, prover);
		
		result.addAll(jodcheck.computeJodCover(prover, jtr, covered));
		return result;
	}
	
	
}