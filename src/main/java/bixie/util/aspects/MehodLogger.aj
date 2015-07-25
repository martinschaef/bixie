/**
 * 
 */

import java.util.Set;

import bixie.checker.faultlocalization.FaultLocalizationThread;
import bixie.checker.transition_relation.AbstractTransitionRelation;
import bixie.util.Log;
import bixie.util.aspects.Loggable;
import boogie.controlflow.CfgProcedure;

/**
 * @author schaef
 *
 */
public aspect MehodLogger {

	
	Object around() : execution(* *(..)) && @annotation(Loggable) {				
		//Execute the actual procedure
		long startTime = System.currentTimeMillis();
		Object result = proceed();
		long elapsed = System.currentTimeMillis() - startTime;

		if (thisJoinPoint.getThis() instanceof FaultLocalizationThread 
				&& thisJoinPoint.getSignature().toLongString().contains("bixie.checker.faultlocalization.FaultLocalizationThread.localizeFaults")) {			
			logLocalizeFaults((FaultLocalizationThread)thisJoinPoint.getThis(),
					(AbstractTransitionRelation)thisJoinPoint.getArgs()[0],
					(Set<?>)thisJoinPoint.getArgs()[1]);			
		} else if (thisJoinPoint.getSignature().toLongString().contains("bixie.checker.ProgramAnalysis.analyzeProcedure")) {
			logProgramAnalysis(((CfgProcedure)thisJoinPoint.getArgs()[0]).getProcedureName(), result, elapsed);
		} 
		
		return result;
	}

	private void logLocalizeFaults(FaultLocalizationThread flt, AbstractTransitionRelation tr, Set<?> infeasibleBlocks) {
		Log.info("Running fault localization for "+infeasibleBlocks.size()+" blocks.");
	}
	
	private void logProgramAnalysis(String name, Object result, long elapsed) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		if (result==null) {
			sb.append(" failed in ");
		} else {
			sb.append(" completed in ");
		}
		sb.append(((float) elapsed) / 1000f);
		sb.append("seconds.");
		Log.info(sb.toString());
	}
	
}
