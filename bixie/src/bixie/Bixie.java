/**
 * 
 */
package bixie;

import org.apache.log4j.LogManager;
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
		if (args.length!=1) {
			System.err.println("Pass path to java files or jar-filename as input.");
			return;
		}
		LogManager.resetConfiguration();
		String javaFileDir = args[0];
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
