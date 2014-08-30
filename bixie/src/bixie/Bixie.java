/**
 * 
 */
package bixie;

import java.io.PrintWriter;

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
		
//		String javaFileDir = args[0];
//		Bixie m = new Bixie();
//		m.run(javaFileDir, null);		
	}

	public void run(String input, String output) {
		if (input!=null && input.endsWith(".bpl")) {
			try (PrintWriter out = new PrintWriter(output);){
				ProgramFactory pf = new ProgramFactory(input);
				String str = runChecker(pf);
				out.println(str);
				System.out.println(str);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			//run(input, "");
		}
	}
	
//	public void run(String input, String classpath) {
//		System.out.println("Translating");
//		
//		redirectLoggers();
//			
//		org.joogie.Dispatcher.setClassPath(input + classpath);
//		ProgramFactory pf = org.joogie.Dispatcher.run(input);		
//		runChecker(pf);
//	}

	public String runChecker(ProgramFactory pf) {
		System.out.println("Checking");
		org.gravy.Options.v().setChecker(1);
		//Options.v().useLocationAttribute(true);
		org.gravy.Options.v().setLoopMode(1);
		JavaReportPrinter jp = new JavaReportPrinter();
		try {
			ProgramAnalysis.runFullProgramAnalysis(pf, jp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jp.printAllReports();		
	}
	
}
