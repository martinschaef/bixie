/**
 * 
 */
package bixie.util.aspects;

import util.Log;

/**
 * @author schaef
 *
 */
public aspect MehodLogger {

	Object around() : execution(* *(..)) && @annotation(Loggable) {
//		System.out.println("Before " + thisJoinPoint.getSignature());
		long startTime = System.currentTimeMillis();
		Object result = proceed();
		long elapsed = System.currentTimeMillis() - startTime;
		Log.info("Total time: " + ((float) elapsed) / 1000f + "s");
		return result;
	}
}
