/**
 * 
 */
package bixie.checker.checker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import bixie.checker.report.Report;
import bixie.checker.verificationcondition.AbstractTransitionRelation;
import bixie.prover.Prover;
import bixie.prover.ProverExpr;
import bixie.prover.ProverFactory;
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
	protected HashSet<BasicBlock> infeasibleBlocks = new HashSet<BasicBlock>();
	protected HashSet<BasicBlock> infeasibleBlocksUnderPost = new HashSet<BasicBlock>();

	protected Prover prover = null;

	/**
	 * 
	 */
	public AbstractChecker(AbstractControlFlowFactory cff, CfgProcedure p) {
		this.cff = cff;
		this.procedure = p;
	}

	public abstract Report checkSat(Prover prover,
			AbstractControlFlowFactory cff, CfgProcedure p);

	public Report getReport() {
		return this.report;
	}

	@Override
	public void run() {
		ProverFactory pf = new bixie.prover.princess.PrincessProverFactory();
		// Prover prover = pf.spawnWithLog("lala");
		try {
			this.prover = pf.spawn();
			this.report = checkSat(prover, this.cff, this.procedure);
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
