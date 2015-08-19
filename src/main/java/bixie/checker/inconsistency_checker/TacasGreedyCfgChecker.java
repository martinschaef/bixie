/**
 * 
 */
package bixie.checker.inconsistency_checker;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.Log;
import bixie.Options;
import bixie.checker.report.Report;
import bixie.checker.reportprinter.SourceLocation;
import bixie.checker.transition_relation.TransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverFactory;
import bixie.prover.ProverResult;
import bixie.prover.princess.PrincessProver;
import bixie.transformation.SingleStaticAssignment;
import bixie.transformation.TacasCallUnwinding;
import bixie.transformation.loopunwinding.AbstractLoopUnwinding;
import boogie.ast.Attribute;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.CfgVariable;
import boogie.controlflow.expression.CfgIdentifierExpression;
import boogie.controlflow.statement.CfgAssignStatement;
import boogie.controlflow.statement.CfgCallStatement;
import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 * 
 *         Inconsistent code detection algorithm based on greedy Cfg covering.
 *         It uses the Cfg-theory plugin of Princess.
 * 
 *         The algorithm is described in the papers: - Infeasible code detection
 *         (VSTTE'12) - A theory for control-flow graph exploration (ATVA'13)
 *
 */
public class TacasGreedyCfgChecker extends AbstractChecker {

	private TacasCallUnwinding cunwind;
	List<CfgAssignStatement> tacasCallAssignments;

	/**
	 * @param cff
	 * @param p
	 */
	public TacasGreedyCfgChecker(AbstractControlFlowFactory cff, CfgProcedure p) {
		super(cff, p);
	}

	/**
	 * Same as in AbstractChecker but with a different CallUnwinding
	 */
	@Override
	public void run() {
		this.procedure.pruneUnreachableBlocks();

		cunwind = new TacasCallUnwinding();
		cunwind.unwindCalls(this.procedure);
		this.tacasCallAssignments = cunwind.getCallAssigns();

		AbstractLoopUnwinding.unwindeLoops(this.procedure);
		this.procedure.pruneUnreachableBlocks();

		SingleStaticAssignment ssa = new SingleStaticAssignment();
		ssa.computeSSA(this.procedure);
		this.procedure.pruneUnreachableBlocks();

		ProverFactory pf = new bixie.prover.princess.PrincessProverFactory();
		try {
			if (Options.v().getProverLogPrefix() != null
					&& !Options.v().getProverLogPrefix().isEmpty()) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see bixie.checker.checker.AbstractChecker#checkSat(bixie.prover.Prover,
	 * boogie.controlflow.AbstractControlFlowFactory,
	 * boogie.controlflow.CfgProcedure)
	 */
	@Override
	public Report runAnalysis(Prover prover) {

		TransitionRelation tr = new TransitionRelation(this.procedure,
				this.cff, prover);


		LinkedHashMap<ProverExpr, ProverExpr> ineffFlags = new LinkedHashMap<ProverExpr, ProverExpr>();

		for (BasicBlock block : tr.getEffectualSet()) {
			ProverExpr v = tr.getReachabilityVariables().get(block);
			ineffFlags.put(v, prover.mkVariable("" + v + "_flag",
					prover.getBooleanType()));
		}

		//TACAS Hack
//		Map<ProverExpr, ProverExpr> ineffFlags_copy = new LinkedHashMap<ProverExpr, ProverExpr>(ineffFlags);
		
		/*
		 * Assert the transition relation of the procedure.
		 */
		prover.push();
		this.pushTransitionRelation(prover, tr);
		prover.addAssertion(tr.getEnsures());

		// construct the inverted reachabilityVariables which is used later
		// to keep track of what has been covered so far.
		Map<ProverExpr, BasicBlock> blocksToCover = createdInvertedReachabilityVariableMap(
				tr, new HashSet<BasicBlock>(tr.getReachabilityVariables()
						.keySet()));

		/*
		 * ===== main algorithm ==== Two steps: In the first step, push the
		 * assertion flag and check which blocks have feasible executions. Then
		 * pop the flag to disable all assertions and check what blocks can now
		 * be reached.
		 * 
		 * Step 1:
		 */
		prover.push();
		prover.addAssertion(prover.mkNot(tr.assertionFlag));
		HashSet<BasicBlock> coveredBlocks = new HashSet<BasicBlock>();
		tacas_enabled = true;
		coveredBlocks.addAll(coverBlocks(blocksToCover, tr, ineffFlags));
		tacas_enabled = false;

		// coverBlocks returns the set of all feasible blocks.
		this.feasibleBlocks = new HashSet<BasicBlock>(coveredBlocks);

		/*
		 * Step 2: Pop the tr.assertionFlag. An re-run coverBlocks to cover
		 * everything that has a feasible execution if assertions are ignored.
		 */
		prover.pop();
		coveredBlocks.addAll(coverBlocks(blocksToCover, tr, ineffFlags));

		/* Pop the transition relation. */
		prover.pop();

		/*
		 * ===== End of the main algorithm ==== everything that was not covered
		 * in either of the iterations is clearly unreachable.
		 */
		HashSet<BasicBlock> unreachable = new HashSet<BasicBlock>(
				blocksToCover.values());

		/*
		 * All blocks that are covered in the second round - that is, the blocks
		 * that are in coveredBlocks but not in feasibleBlocks - are potentially
		 * dangerous, because their inconsistency contains an assertion.
		 */
		HashSet<BasicBlock> dangerous = new HashSet<BasicBlock>(coveredBlocks);
		dangerous.removeAll(this.feasibleBlocks);

		Report report = new Report(tr);
		report.reportInconsistentCode(0, dangerous);
		report.reportInconsistentCode(1, unreachable);

		return report;
	}

	/**
	 * Tries to cover elements in blocks by covering all blocks in the effectual
	 * set of the CFG using Princess' cfg-theory. The map ineffFlags contains
	 * one helper variable for each block in the effectual set.
	 * 
	 * @param blocks
	 *            Map from SMT variables to BasicBlocks.
	 * @param tr
	 *            Transition relation of the analyzed procedure.
	 * @param ineffFlags
	 *            Map from SMT variables in blocks to helper variables for
	 *            Princess.
	 * @return The set of blocks that could be covered.
	 */
	protected Set<BasicBlock> coverBlocks(Map<ProverExpr, BasicBlock> blocks,
			TransitionRelation tr,
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
	 * 'threshold' previously uncovered blocks. Setting the threshold is more
	 * efficient looking for arbitrary new paths.
	 * 
	 * @param blocks
	 *            Map from SMT variables to BasicBlocks.
	 * @param tr
	 *            Transition relation of the analyzed procedure.
	 * @param ineffFlags
	 *            Map from SMT variables in blocks to helper variables for
	 *            Princess.
	 * @param threshold
	 *            lower bound for the number of new blocks that have to be
	 *            covered per path.
	 * @param timeLimit
	 *            the time limit for the prover. If the limit is reached, the
	 *            analysis stops and returns the current set of covered blocks.
	 *            If timeLimit is 0, the solver is not timed out.
	 * @return The set of blocks that could be covered for the given threshold.
	 */
	protected Set<BasicBlock> coverBlocksWithThreshold(
			Map<ProverExpr, BasicBlock> blocks, TransitionRelation tr,
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
			
			List<TacasData> backlog = new LinkedList<TacasData>();
			
			System.err.println("One");
			for (Entry<ProverExpr, BasicBlock> entry : blocks.entrySet()) {
				final ProverExpr pe = entry.getKey();
				if (prover.evaluate(pe).getBooleanLiteralValue()) {
					trueInModel.add(pe);

					/*
					 * TODO: this is where we check if there was a procedure
					 * call on the feasible path that may return null. If the
					 * call does not return null in this model, we add it to the
					 * backlog of things that we want to check later.
					 */
					if (tacas_enabled ) { //check if we haven't covered that block
						for (CfgStatement st : entry.getValue().getStatements()) {
							if (tacasCallAssignments.contains(st)) {
								CfgAssignStatement asgn = (CfgAssignStatement) st;
								CfgIdentifierExpression v = (CfgIdentifierExpression) asgn
										.getRight()[0];
								ProverExpr retvar = tr.getProverExpr(
										v.getVariable(), 0);
								BigInteger i = prover.evaluate(retvar)
										.getIntLiteralValue();
								ProverExpr nullvar = getNullVariable(tr);
								BigInteger null_value = prover
										.evaluate(nullvar).getIntLiteralValue();
								if (!i.equals(null_value)) {
									TacasData td = new TacasData();
									td.blockWithCall = entry.getValue();
									td.blockVar = pe;
									td.returnVariable = retvar;
									td.cfgVariable = v.getVariable();
									td.nullVar = nullvar;
									td.retAssingStmt = st;
									backlog.add(td);
									
									System.err.println(td.blockWithCall.getLabel() + " " + st);
									
									/* Now check if there is a feasible path through this 
									 * if the procedure returned null.
									 */
								} else {
									//TODO: make sure that we never add this to the backlog.
									//because we already found and blocked a path where null
									//return is feasible.
									System.err.println("Null works for " + entry.getValue().getLabel() + " " + st);
								}
							}
						}
					}
				}
			}

			for (TacasData td : backlog) {				
				prover.push();
				//temporarily set the threshold to 1 to find the path
				//and clone remainingBlockVars and remainingIneffFlags
				((PrincessProver) prover).setupCFGPlugin(tr.getProverDAG(),
						new LinkedList<ProverExpr>(remainingBlockVars), new LinkedList<ProverExpr>(remainingIneffFlags), 1);
				//assert that the return value is null.
				prover.addAssertion(prover.mkEq(td.returnVariable, td.nullVar));
				//assert that we go through the same block.
				prover.addAssertion(td.blockVar);				
				if (prover.checkSat(true)!=ProverResult.Sat) {
					CfgCallStatement call = this.cunwind.findCallStatement(td.cfgVariable);
					StringBuilder sb = new StringBuilder();
					sb.append("TACAS: ");
					SourceLocation loc = null;
					SourceLocation lastloc = null;
					for (CfgStatement st : td.blockWithCall.getStatements()) {
						lastloc = praseLocationTags(st.getAttributes());	
						if (lastloc!=null) {
							loc = lastloc;
						}
						if (st==td.retAssingStmt) {
							break;
						}
					}					
					if (loc!=null) {
						sb.append(" in ");
						sb.append(loc.FileName);
						sb.append(" line ");
						sb.append(loc.StartLine);
						sb.append(": ");
					}
					sb.append(call.toString());
					sb.append(" breaks stuff when it returns Null.\n");
					Log.info(sb.toString());
//					TacasReport rep = new TacasReport();
//					rep.call = call;
//					rep.loc = loc;
//					rep.name = tr.getProcedure().getProcedureName();
//					callReports.add(rep);
				} else {
					System.err.println("boring");
				}
				prover.pop();
				//reset the threshold
				((PrincessProver) prover).setupCFGPlugin(tr.getProverDAG(),
						remainingBlockVars, remainingIneffFlags, threshold);				
			}
			backlog.clear();
			
			for (ProverExpr pe : trueInModel) {
				//Now continue with business as usual.
				ProverExpr flag = ineffFlags.get(pe);
				if (flag != null) {
					flagsToAssert.add(flag);
				}
				ineffFlags.remove(pe);									
			}
			
			for (ProverExpr e : trueInModel) {
				coveredBlocks.add(blocks.get(e));
				blocks.remove(e);
			}

			prover.addAssertion(prover.mkAnd(flagsToAssert
					.toArray(new ProverExpr[flagsToAssert.size()])));

			res = prover.checkSat(true);

		}
		return coveredBlocks;
	}

	/*
	 * What follows is stuff that is specific to the tacas paper.
	 */
	public static class TacasData {
		public CfgStatement retAssingStmt;
		public CfgVariable cfgVariable;
		public BasicBlock blockWithCall;
		public ProverExpr returnVariable;
		public ProverExpr blockVar;
		public ProverExpr nullVar;
	}
	
//	public static class TacasReport {
//		public String name;
//		public SourceLocation loc;
//		public CfgCallStatement call;
//	}
//	
//	private List<TacasReport> callReports = new LinkedList<TacasReport>(); 
	
	private boolean tacas_enabled = false;

	private ProverExpr getNullVariable(TransitionRelation tr) {
		CfgVariable v = tr.findVariableByName("$null");
		return tr.getProverExpr(v, 0);
	}

	private SourceLocation praseLocationTags(Attribute[] attributes) {
		if (attributes == null)
			return null;
		return SourceLocation.readSourceLocationFromAttributes(attributes);
	}
	
}
