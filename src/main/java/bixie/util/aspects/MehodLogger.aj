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
		long startTime = System.currentTimeMillis();		
		Object result = proceed();
		long elapsed = System.currentTimeMillis() - startTime;
		String name = thisEnclosingJoinPointStaticPart.getSignature().getName();
		Log.info(name+" took: " + ((float) elapsed) / 1000f + "s");
		
		return result;
	}
}
