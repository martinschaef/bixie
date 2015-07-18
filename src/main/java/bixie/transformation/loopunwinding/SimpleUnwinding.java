/**
 * 
 */
package bixie.transformation.loopunwinding;

import java.util.List;

import util.Log;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.util.LoopDetection;
import boogie.controlflow.util.LoopInfo;

/**
 * @author schaef
 *
 */
public class SimpleUnwinding extends AbstractLoopUnwinding {

	public SimpleUnwinding(CfgProcedure proc, int unwindings) {
		super(proc);
		this.proc = proc;
		this.maxUnwinding = unwindings;
	}

	
	/* (non-Javadoc)
	 * @see bixie.checker.loopunwinding.AbstractLoopUnwinding#unwind()
	 */
	@Override
	public void unwind() {
		BasicBlock root = proc.getRootNode();
		LoopDetection detection = new LoopDetection();
		List<LoopInfo> loops = detection.computeLoops(root);

		for (LoopInfo loop : loops) {
			Log.debug(loop);
			unwind(loop, this.maxUnwinding);
		}

	}


}
