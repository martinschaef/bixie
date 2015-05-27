/**
 * 
 */
package bixie.checker.checker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import org.joogie.cfgPlugin.Util.Dag;

import ap.parser.IFormula;
import bixie.checker.report.Report;
import bixie.checker.util.Statistics;
import bixie.checker.verificationcondition.Nfm15TransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverResult;
import bixie.prover.princess.PrincessProver;
import bixie.transformation.CallUnwinding;
import bixie.transformation.SingleStaticAssignment;
import bixie.transformation.loopunwinding.AbstractLoopUnwinding;
import bixie.util.Log;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgAxiom;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.util.HasseDiagram;
import boogie.controlflow.util.PartialBlockOrderNode;

/**
 * @author schaef
 * 
 */
public class JodChecker2 extends AbstractChecker {

	public JodChecker2(AbstractControlFlowFactory cff, CfgProcedure p,
			Prover prover) {
		super(cff, p);
		this.prover = prover;
	}

	/**
	 * @param cff
	 * @param p
	 */
	public JodChecker2(AbstractControlFlowFactory cff, CfgProcedure p) {
		super(cff, p);

		System.err.println("prune unreachable");

		// p.toDot("./"+p.getProcedureName()+".dot");

		p.pruneUnreachableBlocks();

		// p.toDot("./"+p.getProcedureName()+".dot");

		// System.err.println("remove calls");

		CallUnwinding cunwind = new CallUnwinding();
		cunwind.unwindCalls(p);

		// System.err.println("unwind loops");
		AbstractLoopUnwinding.unwindeLoops(p);
		p.pruneUnreachableBlocks();

		// System.err.println("ssa");
		// p.toFile("./"+p.getProcedureName()+".bpl");

		SingleStaticAssignment ssa = new SingleStaticAssignment();
		ssa.computeSSA(p);

		// System.err.println("prune again");
		p.pruneUnreachableBlocks();

		// System.err.println("done");

		// p.toFile("./"+p.getProcedureName()+".bpl");
		// p.toDot("./"+p.getProcedureName()+"_lf.dot");
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
	public Report checkSat(Prover prover, AbstractControlFlowFactory cff,
			CfgProcedure p) {
		Nfm15TransitionRelation tr = new Nfm15TransitionRelation(p, cff, prover);

		Statistics.HACK_effectualSetSize = tr.getEffectualSet().size();

		// now exclude all feasible paths that may violate the postcondition
		// compute the feasible path cover under the given postcondition
		try {
			feasibleBlocks = new HashSet<BasicBlock>(computeJodCover(prover,
					tr, new HashSet<BasicBlock>()));
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}

		// this set is empty for infeasible code detection.
		infeasibleBlocksUnderPost = new HashSet<BasicBlock>();

		infeasibleBlocks = new HashSet<BasicBlock>(tr
				.getReachabilityVariables().keySet());
		infeasibleBlocks.removeAll(feasibleBlocks);

		Report report = new Report(tr);
		report.reportInconsistentCode(0, this.infeasibleBlocks);
		return report;
	}

	Nfm15TransitionRelation transRel;

	public Collection<BasicBlock> computeJodCover(Prover prover,
			Nfm15TransitionRelation tr, Set<BasicBlock> alreadyCovered) {

		transRel = tr;
		HashSet<BasicBlock> coveredBlocks = new HashSet<BasicBlock>(
				alreadyCovered);

		// add the basic prelude stuff that is needed for every check.
		for (Entry<CfgAxiom, ProverExpr> entry : tr.getPreludeAxioms()
				.entrySet()) {
			prover.addAssertion(entry.getValue());
		}
		prover.addAssertion(tr.getRequires());
		prover.addAssertion(tr.getEnsures());

		PartialBlockOrderNode poRoot = tr.getHasseDiagram().getRoot();

		System.err.println("Searching for feasible paths...");
		coveredBlocks.addAll(findFeasibleBlocks2(prover, tr, poRoot,
				new HashSet<BasicBlock>(alreadyCovered)));

		return coveredBlocks;
	}

	HashSet<PartialBlockOrderNode> knownInfeasibleNodes = new HashSet<PartialBlockOrderNode>();

	// TODO: keep track of everything that has been proved infeasible
	// to make sure that we don't do the same work twice.
	Set<Set<BasicBlock>> infeasibleSubprograms = new HashSet<Set<BasicBlock>>();

	/**
	 * Check subprogram
	 * 
	 * @param prover
	 * @param tr
	 * @param node
	 * @return
	 */
	private HashSet<BasicBlock> findFeasibleBlocks2(Prover prover,
			Nfm15TransitionRelation tr, PartialBlockOrderNode node,
			Set<BasicBlock> alreadyCovered) {
		if (node.getSuccessors().size() > 0) {
			boolean allChildrenInfeasible = true;
			HashSet<BasicBlock> result = new HashSet<BasicBlock>();
			for (PartialBlockOrderNode child : node.getSuccessors()) {
				Set<BasicBlock> res = findFeasibleBlocks2(prover, tr, child,
						alreadyCovered);
				result.addAll(res);
				if (!res.isEmpty())
					allChildrenInfeasible = false;
				// check if we have proved this node to be infeasible
				if (knownInfeasibleNodes.contains(node))
					return new HashSet<BasicBlock>();
			}
			if (allChildrenInfeasible)
				knownInfeasibleNodes.add(node);
			return result;
		} else {
			HashSet<BasicBlock> result = new HashSet<BasicBlock>(alreadyCovered);
			if (alreadyCovered.containsAll(node.getElements()))
				return result;
			result.addAll(tryToFindConflictInPO(prover, tr, node, 0));
			return result;
		}
	}

	/**
	 * returns all nodes that occur on paths through b
	 * 
	 * @param b
	 * @return
	 */
	private Set<BasicBlock> getSubprogContaining(BasicBlock b) {
		Set<BasicBlock> knownInfeasibleBlocks = getKnownInfeasibleBlocks();

		LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
		HashSet<BasicBlock> done = new HashSet<BasicBlock>();

		todo.add(b);
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pop();
			done.add(current);
			for (BasicBlock x : current.getPredecessors()) {
				if (!todo.contains(x) && !done.contains(x)
						&& !knownInfeasibleBlocks.contains(x)) {
					todo.add(x);
				}
			}
		}
		// now the other direction
		todo.add(b);
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pop();
			done.add(current);
			for (BasicBlock x : current.getSuccessors()) {
				if (!todo.contains(x) && !done.contains(x)
						&& !knownInfeasibleBlocks.contains(x)) {
					todo.add(x);
				}
			}
		}
		return done;
	}

	// private Set<BasicBlock> getSubgraphContainingAll(Set<BasicBlock> nodes,
	// Set<BasicBlock> blocks) {
	// LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>(blocks);
	// HashSet<BasicBlock> done = new HashSet<BasicBlock>();
	// while (!todo.isEmpty()) {
	// BasicBlock current = todo.pop();
	// Set<BasicBlock> subprog = getSubgraphContaining(nodes, current);
	// done.addAll(subprog);
	// }
	// return done;
	// }

	private Set<BasicBlock> getKnownInfeasibleBlocks() {
		Set<BasicBlock> infeasibleBlocks = new HashSet<BasicBlock>();
		// for (PartialBlockOrderNode po : this.knownInfeasibleNodes) {
		// infeasibleBlocks.addAll(po.getElements());
		// }
		return infeasibleBlocks;
	}

	private ProverExpr mkDisjunction(Nfm15TransitionRelation tr,
			Collection<BasicBlock> blocks) {
		ProverExpr next;
		if (blocks.size() == 0) {
			next = prover.mkLiteral(true);
		} else if (blocks.size() == 1) {
			next = tr.getReachabilityVariables().get(blocks.iterator().next());
		} else {
			ProverExpr[] disj = new ProverExpr[blocks.size()];
			int i = 0;
			for (BasicBlock n : blocks) {
				disj[i++] = tr.getReachabilityVariables().get(n);
			}
			next = prover.mkOr(disj);
		}
		return next;
	}

	/**
	 * Get a complete and feasible path from the model produced by princes.
	 * 
	 * @param prover
	 * @param tr
	 * @param necessaryNodes
	 *            one of these nodes needs to be in the path
	 * @return
	 */
	private HashSet<BasicBlock> getPathFromModel(Prover prover,
			Nfm15TransitionRelation tr, Set<BasicBlock> allBlocks,
			Set<BasicBlock> necessaryNodes) {
		// Blocks selected by the model
		HashSet<BasicBlock> enabledBlocks = new HashSet<BasicBlock>();
		for (BasicBlock b : allBlocks) {
			final ProverExpr pe = tr.getReachabilityVariables().get(b);
			if (prover.evaluate(pe).getBooleanLiteralValue()) {
				enabledBlocks.add(b);
			}
		}

		for (BasicBlock block : necessaryNodes) {
			if (enabledBlocks.contains(block)) {
				// Get the path from block to the exit
				LinkedList<BasicBlock> blockToExit = new LinkedList<BasicBlock>();
				BasicBlock current = block;
				while (current != null) {
					blockToExit.add(current);
					BasicBlock _current = null;
					for (BasicBlock next : current.getSuccessors()) {
						if (enabledBlocks.contains(next)) {
							_current = next;
							break;
						}
					}
					current = _current;
				}

				if (blockToExit != null) {
					// Get the path from root to the block
					LinkedList<BasicBlock> rootToBlock = new LinkedList<BasicBlock>();
					current = block;
					while (current != null) {
						blockToExit.add(current);
						BasicBlock _current = null;
						for (BasicBlock next : current.getPredecessors()) {
							if (enabledBlocks.contains(next)) {
								_current = next;
								break;
							}
						}
						current = _current;
					}

					if (rootToBlock != null) {
						// We got a full path
						HashSet<BasicBlock> result = new HashSet<BasicBlock>();
						result.addAll(rootToBlock);
						result.addAll(blockToExit);
						return result;
					}
				}
			}
		}

		// Screwed
		toDot("path_error.dot", new HashSet<BasicBlock>(allBlocks),
				new HashSet<BasicBlock>(enabledBlocks),
				new HashSet<BasicBlock>(necessaryNodes));
		throw new RuntimeException("Could not find a path");
	}

	private void makeColors(PartialBlockOrderNode node, int startColor,
			int endColor, HashMap<PartialBlockOrderNode, Integer> node2color) {

		int range = (endColor - startColor) / 2;
		int midcolor = startColor + range;

		node2color.put(node, midcolor);

		int previous_color = startColor;
		int colordelta = (int) ((1.0) / ((double) node.getSuccessors().size()) * range);

		for (PartialBlockOrderNode child : node.getSuccessors()) {
			makeColors(child, previous_color, previous_color + colordelta,
					node2color);
			previous_color += colordelta;
		}

	}

	public void toDot(String filename, Nfm15TransitionRelation tr) {
		HasseDiagram hd = tr.getHasseDiagram();
		// HashSet<PartialBlockOrderNode> poNodes = getPoNodes(hd.getRoot());
		HashMap<PartialBlockOrderNode, Integer> node2color = new HashMap<PartialBlockOrderNode, Integer>();

		makeColors(hd.getRoot(), 0x101010, 0xffffff, node2color);
		// int i=1;
		// for (PartialBlockOrderNode node : poNodes) {
		// double color = ((double)(i++))/((double)poNodes.size()+1) *
		// ((double)0xffffff);
		// node2color.put(node, (int)color );
		// }

		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(filename), "UTF-8"))) {
			pw.println("digraph dot {");
			LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
			HashSet<BasicBlock> done = new HashSet<BasicBlock>();
			todo.add(tr.getProcedure().getRootNode());
			StringBuffer sb = new StringBuffer();
			while (!todo.isEmpty()) {
				BasicBlock current = todo.pop();
				done.add(current);
				// for (BasicBlock prev : current.getPredecessors()) {
				// pw.println(" \""+ current.getLabel()
				// +"\" -> \""+prev.getLabel()+"\" [style=dotted]");
				// if (!todo.contains(prev) && !done.contains(prev)) {
				// todo.add(prev);
				// }
				//
				// }
				for (BasicBlock next : current.getSuccessors()) {
					sb.append(" \"" + current.getLabel() + "\" -> \""
							+ next.getLabel() + "\" \n");
					if (!todo.contains(next) && !done.contains(next)) {
						todo.add(next);
					}
				}
			}

			for (BasicBlock b : done) {
				StringBuilder sb_ = new StringBuilder();
				sb_.append(Integer.toHexString(node2color.get(hd.findNode(b))));
				while (sb_.length() < 6) {
					sb_.insert(0, '0'); // pad with leading zero if needed
				}
				String colorHex = sb_.toString();
				pw.println("\"" + b.getLabel() + "\" " + "[label=\""
						+ b.getLabel() + "\",style=filled, fillcolor=\"#"
						+ colorHex + "\"];\n");
			}
			pw.println(sb.toString());

			pw.println("}");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void hasseToDot(String filename, Nfm15TransitionRelation tr) {
		HasseDiagram hd = tr.getHasseDiagram();

		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(filename), "UTF-8"))) {
			pw.println("digraph dot {");
			LinkedList<PartialBlockOrderNode> todo = new LinkedList<PartialBlockOrderNode>();
			HashSet<PartialBlockOrderNode> done = new HashSet<PartialBlockOrderNode>();
			todo.add(hd.getRoot());
			StringBuffer sb = new StringBuffer();
			while (!todo.isEmpty()) {
				PartialBlockOrderNode current = todo.pop();
				done.add(current);
				// for (BasicBlock prev : current.getPredecessors()) {
				// pw.println(" \""+ current.getLabel()
				// +"\" -> \""+prev.getLabel()+"\" [style=dotted]");
				// if (!todo.contains(prev) && !done.contains(prev)) {
				// todo.add(prev);
				// }
				//
				// }
				for (PartialBlockOrderNode next : current.getSuccessors()) {
					sb.append(" \"" + current.hashCode() + "\" -> \""
							+ next.hashCode() + "\" \n");
					if (!todo.contains(next) && !done.contains(next)) {
						todo.add(next);
					}
				}
			}

			for (PartialBlockOrderNode node : done) {
				StringBuilder _sb = new StringBuilder();
				for (BasicBlock b : node.getElements()) {
					_sb.append(b.getLabel() + "\n");
				}

				pw.println("\"" + node.hashCode() + "\" " + "[label=\""
						+ _sb.toString() + "\"];\n");
			}
			pw.println(sb.toString());

			pw.println("}");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * ---------------------------- Plan B --------------------------------
	 */

	private Set<BasicBlock> tryToFindConflictInPO(Prover prover,
			Nfm15TransitionRelation tr, PartialBlockOrderNode node, int timeout) {
		// pick any
		learnedConflicts.clear();
		BasicBlock current = node.getElements().iterator().next();
		try {
			Set<BasicBlock> res = null;
			// find the first one cheap.
			Set<BasicBlock> path = up(current, current,
					new HashSet<BasicBlock>());

			while (path != null) {
				if (checkPath(current, path)) {
					return path;
				}
				path = findNextPath(current);
			}

			// Set<BasicBlock> res = up(current, current, new
			// HashSet<BasicBlock>());
			if (path == null) {
				Log.debug("Cool ... learned stuff from conflict.");
				res = new HashSet<BasicBlock>();
			}
			return res;
		} catch (HackInfeasibleException e) {
			this.knownInfeasibleNodes.add(node);
			Log.debug("YEAH");
		}
		return new HashSet<BasicBlock>();
	}

	private Set<BasicBlock> up(BasicBlock b, BasicBlock source,
			Set<BasicBlock> path) throws HackInfeasibleException {
		Set<BasicBlock> path_ = new HashSet<BasicBlock>(path);
		path_.add(b);
		if (isInLearnedConflicts(path_))
			return null;
		if (b != this.procedure.getRootNode()) {
			for (BasicBlock x : b.getPredecessors()) {
				Set<BasicBlock> result = up(x, source, path_);
				if (result != null)
					return result;
			}
		} else {
			Set<BasicBlock> result = down(source, source, path_);
			if (result != null)
				return result;
		}
		return null;
	}

	private Set<BasicBlock> down(BasicBlock b, BasicBlock source,
			Set<BasicBlock> path) throws HackInfeasibleException {
		Set<BasicBlock> path_ = new HashSet<BasicBlock>(path);
		path_.add(b);
		if (isInLearnedConflicts(path_))
			return null;
		if (b != this.procedure.getExitNode()) {
			for (BasicBlock x : b.getSuccessors()) {
				Set<BasicBlock> result = down(x, source, path_);
				if (result != null)
					return result;
			}
		} else {
			// ignore paths that are already known conflicts.
			if (isInLearnedConflicts(path_))
				return null;

			// if (checkPath(source, path_)) {
			return path_;
			// }
		}
		return null;
	}

	private boolean isInLearnedConflicts(Set<BasicBlock> path) {
		for (Set<BasicBlock> conflict : learnedConflicts) {
			if (conflict.size() > 0 && path.size() > conflict.size()
					&& path.containsAll(conflict)) {
				// System.err.print("Skipping path with known conflict: ");
				// for (BasicBlock b : conflict) {
				// System.err.print(b.getLabel()+", ");
				// }
				// System.err.println();
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("serial")
	public class HackInfeasibleException extends Exception {

	}

	Set<Set<BasicBlock>> learnedConflicts = new HashSet<Set<BasicBlock>>();

	private boolean checkPath(BasicBlock source, Set<BasicBlock> path)
			throws HackInfeasibleException {
		Log.debug("checking path");

		prover.push();
		for (BasicBlock b : path) {
			prover.addAssertion(this.transRel.blockTransitionReleations.get(b));
		}
		ProverResult res = prover.checkSat(true);
		prover.pop();
		if (res == ProverResult.Sat) {
			Log.debug("\tSAT");
			return true;
		} else if (res == ProverResult.Unsat) {
			Log.debug("\tUNST");
			int oldsize = path.size();
			computePseudoUnsatCore(path);
			learnedConflicts.add(new HashSet<BasicBlock>(path));
			if (oldsize == path.size()) {
				Log.debug("nothing could be removed");
				return false;
			}
			Set<BasicBlock> inevitableBlocks = findNodeThatMustBePassed(this.transRel
					.getHasseDiagram().findNode(source));
			if (inevitableBlocks.containsAll(path)) {
				Log.debug("FOUND CONFLICT! DONE");
				markSmallestSubtreeInfeasible(path);
				throw new HackInfeasibleException();
			} else {
				Log.debug("nothing learned. Looking for next path.");
			}
		} else {
			throw new RuntimeException("PROVER FAILED");
		}
		return false;
	}

	private void computePseudoUnsatCore(Set<BasicBlock> path) {
		LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>(path);
		Log.debug("computing pseudo unsat core");
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pop();
			path.remove(current);
			prover.push();
			for (BasicBlock b : path) {
				prover.addAssertion(this.transRel.blockTransitionReleations
						.get(b));
			}
			ProverResult res = prover.checkSat(true);
			prover.pop();
			if (res == ProverResult.Sat) {
				path.add(current); // then we needed this one
			} else if (res == ProverResult.Unsat) {
				// otherwise, we can remove it.
			} else {
				throw new RuntimeException("PROVER FAILED");
			}
		}
	}

	private Set<BasicBlock> findNodeThatMustBePassed(PartialBlockOrderNode node) {
		if (node == null)
			return new HashSet<BasicBlock>();
		Set<BasicBlock> result = findNodeThatMustBePassed(node.getParent());
		result.addAll(node.getElements());
		return result;
	}

	private void markSmallestSubtreeInfeasible(Set<BasicBlock> unsatCore) {
		PartialBlockOrderNode lowest = this.transRel.getHasseDiagram()
				.getRoot();
		for (BasicBlock b : unsatCore) {
			PartialBlockOrderNode current = this.transRel.getHasseDiagram()
					.findNode(b);
			if (isAbove(current, lowest)) {
				lowest = current;
			}
		}
		LinkedList<PartialBlockOrderNode> todo = new LinkedList<PartialBlockOrderNode>();
		todo.add(lowest);
		while (!todo.isEmpty()) {
			PartialBlockOrderNode current = todo.pop();
			this.knownInfeasibleNodes.add(current);
			todo.addAll(current.getSuccessors());
		}

	}

	private boolean isAbove(PartialBlockOrderNode child,
			PartialBlockOrderNode parent) {
		PartialBlockOrderNode node = child;
		while (node != null) {
			if (node == parent)
				return true;
			node = node.getParent();
		}
		return false;
	}

	/*
	 * ================ stuff to find path with sat solver =====================
	 */

	private Set<BasicBlock> findNextPath(BasicBlock current) {
		Log.debug("Finding next path");
		Set<BasicBlock> blocks = this.getSubprogContaining(current);

		prover.push();
		// assert this subprogram.
		// assertAbstractaPath(blocks);

		assertAbstractaPathCfGTheory(blocks);

		prover.addAssertion(transRel.getReachabilityVariables().get(current));
		// block all learned conflicts
		Log.debug("Asserting " + this.learnedConflicts.size() + " conflicts");
		for (Set<BasicBlock> conflict : this.learnedConflicts) {
			ProverExpr[] conj = new ProverExpr[conflict.size()];
			int i = 0;
			for (BasicBlock b : conflict) {
				conj[i++] = this.transRel.getReachabilityVariables().get(b);
			}
			prover.addAssertion(prover.mkNot(prover.mkAnd(conj)));
		}
		Log.debug("Checking for path.");
		ProverResult res = prover.checkSat(true);
		if (res == ProverResult.Sat) {
			HashSet<BasicBlock> necessaryNodes = new HashSet<BasicBlock>();
			necessaryNodes.add(current);
			Set<BasicBlock> path = this.getPathFromModel(prover, transRel,
					blocks, necessaryNodes);
			prover.pop();
			Log.debug("Found one.");
			return path;
		} else if (res == ProverResult.Unsat) {
			prover.pop();
			// otherwise, we can remove it.
		} else {
			throw new RuntimeException("PROVER FAILED");
		}
		Log.debug("Found NONE.");
		return null;
	}

	private void assertAbstractaPathCfGTheory(Set<BasicBlock> blocks) {
		LinkedHashMap<ProverExpr, ProverExpr> ineffFlags = new LinkedHashMap<ProverExpr, ProverExpr>();
		for (BasicBlock block : blocks) {
			ProverExpr v = transRel.getReachabilityVariables().get(block);
			ineffFlags.put(v, prover.mkVariable("" + v + "_flag",
					prover.getBooleanType()));
		}
		Dag<IFormula> vcdag = transRel.getProverDAG();

		LinkedList<ProverExpr> remainingBlockVars = new LinkedList<ProverExpr>();
		LinkedList<ProverExpr> remainingIneffFlags = new LinkedList<ProverExpr>();
		for (Entry<ProverExpr, ProverExpr> entry : ineffFlags.entrySet()) {
			remainingBlockVars.add(entry.getKey());
			remainingIneffFlags.add(entry.getValue());
		}

		((PrincessProver) prover).setupCFGPlugin(vcdag, remainingBlockVars,
				remainingIneffFlags, 1);

		// Encode each block
		for (BasicBlock block : blocks) {

			// Get the successors of the block
			LinkedList<BasicBlock> successors = new LinkedList<BasicBlock>();
			for (BasicBlock succ : block.getSuccessors()) {
				if (blocks.contains(succ)) {
					successors.add(succ);
				}
			}

			// Construct the disjunction of the successors

			// Make the assertion
			ProverExpr assertion = prover.mkImplies(transRel
					.getReachabilityVariables().get(block),
					mkDisjunction(transRel, successors));

			// Assert it
			prover.addAssertion(assertion);

			// //now assert bwd
			// LinkedList<BasicBlock> predecessors = new
			// LinkedList<BasicBlock>();
			// for (BasicBlock pred : block.getPredecessors()) {
			// if (blocks.contains(pred)) {
			// predecessors.add(pred);
			// }
			// }
			//
			// assertion = prover.mkImplies(
			// transRel.getReachabilityVariables().get(block),
			// mkDisjunction(transRel, predecessors)
			// );
			//
			// // Assert it
			// prover.addAssertion(assertion);
		}

		prover.addAssertion(transRel.getReachabilityVariables().get(
				transRel.getProcedure().getRootNode()));
		prover.addAssertion(transRel.getReachabilityVariables().get(
				transRel.getProcedure().getExitNode()));
		// System.err.println("Entries "+count);
	}

}