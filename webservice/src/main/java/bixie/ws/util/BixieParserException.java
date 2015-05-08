/**
 * 
 */
package bixie.ws.util;

import java.util.HashMap;

/**
 * @author schaef
 *
 */
public class BixieParserException extends Exception {

	private static final long serialVersionUID = 666961965503740297L;

	private HashMap<Integer, String> errorMessages;
	
	public BixieParserException(HashMap<Integer, String> errors) {
		this.errorMessages = errors;
	}
	
	public HashMap<Integer, String> getErrorMessages() {
		return this.errorMessages;
	}
}
