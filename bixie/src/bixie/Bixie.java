/**
 * 
 */
package bixie;

import org.gravy.ProgramAnalysis;

import boogie.ProgramFactory;


/**
 * @author schaef
 *
 */
public class Bixie {

	/**
	 * 
	 */
	public Bixie() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String javaFileDir = "./test";
		Bixie m = new Bixie();
		m.run(javaFileDir);		
	}

	public void run(String input) {
		org.joogie.Options.v().setClasspath(input);		
		ProgramFactory pf = org.joogie.Dispatcher.run(input);		
		
		org.gravy.Options.v().setChecker(1);
		//Options.v().useLocationAttribute(true);
		org.gravy.Options.v().setLoopMode(1);
			
		try {
			ProgramAnalysis.runFullProgramAnalysis(pf, new JavaReportPrinter());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	
}
