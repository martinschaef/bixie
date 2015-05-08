package bixie.checker.verificationcondition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.CfgVariable;
import boogie.controlflow.expression.CfgExpression;
import boogie.controlflow.expression.CfgIdentifierExpression;
import boogie.controlflow.statement.CfgAssignStatement;
import boogie.controlflow.statement.CfgStatement;

/**
 * @author schaef
 * TODO: if we plan to do interprocedural analysis, we have
 * to change the way globals are handled here.
 */
public class JodTransitionRelation extends AbstractTransitionRelation {
	
	public HashMap<BasicBlock, ProverExpr> blockTransitionReleations = new HashMap<BasicBlock, ProverExpr>();	
	public HashMap<BasicBlock, ProverExpr> abstractTransitionReleations = new HashMap<BasicBlock, ProverExpr>();

	public JodTransitionRelation(CfgProcedure cfg, AbstractControlFlowFactory cff, Prover p) {
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

		//bfs through all blocks
		LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
		HashSet<BasicBlock> done = new HashSet<BasicBlock>();
		todo.add(cfg.getRootNode()); 
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pop();
			done.add(current);
			for (BasicBlock b : current.getSuccessors()) {
				if (!done.contains(b) && !todo.contains(b)) {
					todo.push(b);
				}
			}
			
			this.addBlock(current);			
		}
		finalizeAxioms();
	}	
	
	protected ProverExpr mkConjunction(Collection<ProverExpr> conjuncts) {
		if (conjuncts.size() == 0) {
			return prover.mkLiteral(true);
		}
		if (conjuncts.size() == 1) {
			return conjuncts.iterator().next();
		}
		return prover.mkAnd(conjuncts.toArray(new ProverExpr[conjuncts.size()]));
	}
	
	
	public void addBlock(BasicBlock b) {
		LinkedList<CfgStatement> bStatements = b.getStatements();
		
		// Add the concrete
		List<ProverExpr> concreteStmts = statements2proverExpression(bStatements);
     	this.blockTransitionReleations.put(b, mkConjunction(concreteStmts));
		
		// Add the abstract
		LinkedList<CfgStatement> bAbstractStatements = abstractStatements(bStatements);
		List<ProverExpr> abstractStmts = statements2proverExpression(bAbstractStatements);
		this.abstractTransitionReleations.put(b, mkConjunction(abstractStmts));
		
		// Add the variable
		this.reachabilityVariables.put(b, this.prover.mkVariable(b.getLabel() + "_fwd", this.prover.getBooleanType()));					
	}
	
	/**
	 * Isolate the abstraction of the statements (for example, just keep the frame statements).
	 */
	private LinkedList<CfgStatement> abstractStatements(LinkedList<CfgStatement> bStatements) {
		LinkedList<CfgStatement> abstractStatements = new LinkedList<CfgStatement>();
	
		for (CfgStatement stmnt : bStatements) {
			if (stmnt instanceof CfgAssignStatement) {
				CfgAssignStatement asgn = (CfgAssignStatement) stmnt;
				CfgIdentifierExpression[] left = asgn.getLeft();
				CfgExpression[] right = asgn.getRight();
				
				boolean ok = true;
				if (left.length == right.length) {
					for (int i = 0; ok && i < left.length; ++ i) {
						CfgIdentifierExpression left_i = left[i];
						CfgExpression right_i = right[i];
						if (right_i instanceof CfgIdentifierExpression) {
							CfgVariable left_var = left_i.getVariable();
							CfgVariable right_var = ((CfgIdentifierExpression)right_i).getVariable();
							if (left_var!=right_var) {
								ok = false;
							}
						} else {
							ok = false;
						}
					}
				} else {
					ok = false;
				}
				
				if (ok) {
					abstractStatements.add(stmnt);
				}
			}
		}
		
		return abstractStatements;
	}

	public void removeBlock(BasicBlock b) {
		this.blockTransitionReleations.remove(b);
		this.reachabilityVariables.remove(b);
	}
	
}
