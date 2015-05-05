package bixie.checker.verificationcondition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.joogie.cfgPlugin.CFGPlugin;
import org.joogie.cfgPlugin.Util.Dag;

import util.Log;
import ap.parser.IFormula;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.princess.PrincessProver;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.expression.CfgExpression;

/**
 * @author schaef
 * TODO: if we plan to do interprocedural analysis, we have
 * to change the way globals are handled here.
 */
public class CfgTransitionRelation extends AbstractTransitionRelation {
	
	protected Dag<IFormula> proverDAG;	
	
//	protected ProverExpr expetionalReturnFlag = null;
	
	//TODO: this is a hack, like the creation
	//of this variable in the constructor
//	public ProverExpr getExpetionalReturnFlag() {
//		return expetionalReturnFlag;
//	}

	public Dag<IFormula> getProverDAG() {
		return proverDAG;
	}

	public CfgTransitionRelation(CfgProcedure cfg, AbstractControlFlowFactory cff, Prover p) {
		super(cfg, cff, p);
		makePrelude();
		
		//create the ProverExpr for the precondition 
		ProverExpr[] prec = new ProverExpr[cfg.getRequires().size()];
		int i=0;
		for (CfgExpression expr : cfg.getRequires()) {
			prec[i]=this.expression2proverExpression(expr);
			i++;
		}
		this.requires = this.prover.mkAnd(prec);

		//create the ProverExpr for the precondition 
		ProverExpr[] post = new ProverExpr[cfg.getEnsures().size()];
		i=0;
		for (CfgExpression expr : cfg.getEnsures()) {
			post[i]=this.expression2proverExpression(expr);
			i++;
		}
		this.ensures = this.prover.mkAnd(post);
				
		//encode the forward reachability
		ProverExpr firstok = block2transitionRelation(cfg.getRootNode(),
				this.reachabilityVariables, this.proofObligations);
		
		//the proof obligation for root als must contain that the block variable for root is true		
		this.proofObligations.get(cfg.getRootNode()).add(firstok);
		
		this.proverDAG = procToPrincessDag(cfg, this.reachabilityVariables );

		finalizeAxioms();		
	}	
	
	private ProverExpr block2transitionRelation(BasicBlock b,
			HashMap<BasicBlock, ProverExpr> blockvars,
			HashMap<BasicBlock, LinkedList<ProverExpr>> proofobligations) {
		if (blockvars.containsKey(b)) {
			return blockvars.get(b);
		}

		ProverExpr post;
		if (b.getSuccessors().size() == 0  ) {
			post = this.prover.mkLiteral(true);
		} else if (b.getSuccessors().size() == 1) {
			post = block2transitionRelation(
					b.getSuccessors().toArray(new BasicBlock[1])[0], blockvars,
					proofobligations);
		} else {
			/*
			 * compute \not (/\ (\not B_succ)) that is\/ ( B_succ)
			 */
			ProverExpr[] succs = new ProverExpr[b.getSuccessors().size()];
			int i = 0;
			for (BasicBlock next : b.getSuccessors()) {
				succs[i++] = block2transitionRelation(next, blockvars,
						proofobligations);
			}
			post = this.prover.mkOr(succs);
		}
		List<ProverExpr> stmts = statements2proverExpression(b.getStatements());
		stmts.add(post);
		ProverExpr[] conj = stmts.toArray(new ProverExpr[stmts.size()]);		

		ProverExpr blockvar = this.prover.mkVariable(b.getLabel() + "_fwd",
				this.prover.getBooleanType());
		blockvars.put(b, blockvar);
		LinkedList<ProverExpr> obligations = new LinkedList<ProverExpr>();
		obligations.add(this.prover.mkOr(this.prover.mkNot(blockvar),
				this.prover.mkAnd(conj)));
		proofobligations.put(b, obligations);
		return blockvar;
	}

	private Dag<IFormula> procToPrincessDag(CfgProcedure proc,
			HashMap<BasicBlock, ProverExpr> reachVars) {
		// First transform the CFG into a list and record
		// the index of each block
		// it is imporatant that the list starts with the
		// exitblock
		
		LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
		LinkedList<BasicBlock> done = new LinkedList<BasicBlock>();
		todo.add(proc.getRootNode());
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pollLast();
			boolean allDone = true;
			for (BasicBlock pre : current.getPredecessors()) {
				if (!done.contains(pre)) {
					allDone = false;
					continue;
				}
			}
			if (!allDone) {
				todo.addFirst(current);
				continue;
			}
			// store the position the block will have in the 'done' list.
			done.addLast(current);
			for (BasicBlock suc : current.getSuccessors()) {
				if (!todo.contains(suc) && !done.contains(suc)) {
					if (suc != current) {
						todo.addLast(suc);
					} else {
						// This has to be checked
						Log.error("The node has a self-loop! This is not supposed to happen.");
					}
				}
			}
		}

		Dag<IFormula> currentNode = CFGPlugin.mkDagEmpty();
		// TODO: assert that the first one in the list is actually the ExitBlock
		for (int j = done.size() - 1; j >= 0; j--) {
			BasicBlock b = done.get(j);
			List<Integer> succIndices = new LinkedList<Integer>();
			for (BasicBlock suc : b.getSuccessors()) {
				// TODO: @Philipp willst du die absolute position oder den
				// offset?
				int idx = done.indexOf(suc) - done.indexOf(b);
				succIndices.add(idx);
				// Log.error("\t " +idx+":"+suc.getName() );
			}
			// TODO: review. can be done better
			if (reachVars.get(b)==null) throw new RuntimeException("Cannot find var for "+b.getLabel());
			IFormula d = ((PrincessProver) this.prover)
					.proverExpToIFormula(reachVars.get(b));
			int[] succidx = new int[succIndices.size()];
			for (int i = 0; i < succIndices.size(); i++) {
				succidx[i] = succIndices.get(i);
			}
			currentNode = CFGPlugin.mkDagNode(d, succidx, currentNode);
		}
		// currentNode.prettyPrint();
		return currentNode;
	}
	
	
}
