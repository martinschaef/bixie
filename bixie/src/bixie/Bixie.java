/**
 * 
 */
package bixie;

import java.io.PrintWriter;

import org.gravy.ProgramAnalysis;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

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
		bixie.Options options = bixie.Options.v();
		CmdLineParser parser = new CmdLineParser(options);
		
		if (args.length==0) {
			parser.printUsage(System.err);
			return;
		}		
		
		try {
			parser.parseArgument(args);		

			Bixie bixie = new Bixie();
			if (options.getBoogieFile()!=null && options.getJarFile()!=null) {
				System.err.println("Can only take either Java or Boogie input. Not both");
				return;
			} else if (options.getBoogieFile()!=null) {
				bixie.run(options.getBoogieFile(), options.getOutputFile());
			} else {
				bixie.translateAndRun(options.getJarFile(), options.getClasspath(), options.getOutputFile());
			}
			
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void run(String input, String output) {
		if (input!=null && input.endsWith(".bpl")) {
			try {
				ProgramFactory pf = new ProgramFactory(input);
				runChecker(pf, output);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println("Not a valid Boogie file: "+input);
		}
	}
	
	public void translateAndRun(String input, String classpath, String output) {
		try {
			System.out.println("Translating");
			org.joogie.Dispatcher.setClassPath(classpath);
			ProgramFactory pf = org.joogie.Dispatcher.run(input);		
			runChecker(pf, output);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	}

	public void runChecker(ProgramFactory pf, String output) {
		System.out.println("Checking");
		try (PrintWriter out = new PrintWriter(output);){
		
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
			String str = jp.printAllReports();
			out.println(str);
			System.out.println(str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
