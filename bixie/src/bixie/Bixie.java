/**
 * 
 */
package bixie;

import java.io.File;
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
				String cp = options.getClasspath();
				if (cp!=null && !cp.contains(options.getJarFile())) {
					cp += File.pathSeparatorChar + options.getJarFile();
				}
				bixie.translateAndRun(options.getJarFile(), cp, options.getOutputFile());
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
				JavaReportPrinter jp = runChecker(pf);
				report2File(jp, output);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println("Not a valid Boogie file: "+input);
		}
	}

	protected void report2File(JavaReportPrinter jp, String output) {
		try (PrintWriter out = new PrintWriter(output);){			
			String str = jp.printAllReports();
			out.println(str);
			System.out.println(str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}
	
	public void translateAndRun(String input, String classpath, String output) {
		JavaReportPrinter jp = translateAndRun(input, classpath);
		report2File(jp, output);
	}
	
	
	public JavaReportPrinter translateAndRun(String input, String classpath) {
		try {
			System.out.println("Translating");
			org.joogie.Dispatcher.setClassPath(classpath);
			ProgramFactory pf = org.joogie.Dispatcher.run(input);		
			JavaReportPrinter jp = runChecker(pf);
			return jp;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		return null;
	}

	public JavaReportPrinter runChecker(ProgramFactory pf) {
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
		return jp;
	}
	
}
