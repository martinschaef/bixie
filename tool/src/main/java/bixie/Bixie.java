/**
 * 
 */
package bixie;

import java.io.File;
import java.io.PrintWriter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import bixie.checker.ProgramAnalysis;
import bixie.checker.reportprinter.BasicReportPrinter;
import bixie.checker.reportprinter.ReportPrinter;
import bixie.util.Log;
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

		if (args.length == 0) {
			parser.printUsage(System.err);
			return;
		}

		try {
			parser.parseArgument(args);

			Bixie bixie = new Bixie();
			if (options.getBoogieFile() != null && options.getJarFile() != null) {
				Log.error("Can only take either Java or Boogie input. Not both");
				return;
			} else if (options.getBoogieFile() != null) {
				bixie.run(options.getBoogieFile(), options.getOutputFile());
			} else {
				String cp = options.getClasspath();
				if (cp != null && !cp.contains(options.getJarFile())) {
					cp += File.pathSeparatorChar + options.getJarFile();
				}
				bixie.translateAndRun(options.getJarFile(), cp,
						options.getOutputFile());
			}
		} catch (CmdLineException e) {
			bixie.util.Log.error(e.toString());
			parser.printUsage(System.err);
		} catch (Throwable e) {
			bixie.util.Log.error(e.toString());
		}
	}

	public void run(String input, String output) {
		bixie.Options.v().setOutputFile(output);
		if (input != null && input.endsWith(".bpl")) {
			try {
				ProgramFactory pf = new ProgramFactory(input);
				ReportPrinter jp = runChecker(pf);
				report2File(jp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				bixie.util.Log.error(e.toString());
			}
		} else {
			bixie.util.Log.error("Not a valid Boogie file: " + input);
		}
	}

	protected void report2File(ReportPrinter reportPrinter) {
		try (PrintWriter out = new PrintWriter(bixie.Options.v()
				.getOutputFile());) {
			String str = reportPrinter.printSummary();
			out.println(str);
			bixie.util.Log.info(str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			bixie.util.Log.error(e.toString());
		}
	}

	public void translateAndRun(String input, String classpath, String output) {
		ReportPrinter reportPrinter = translateAndRun(input, classpath);
		bixie.Options.v().setOutputFile(output);
		report2File(reportPrinter);
	}

	public ReportPrinter translateAndRun(String input,
			String classpath) {
		bixie.util.Log.info("Translating");
		org.joogie.Dispatcher.setClassPath(classpath);		
		ProgramFactory pf = org.joogie.Dispatcher.run(input);
		if (pf == null) {
			bixie.util.Log.error("Internal Error: Parsing failed");
			return null;
		}
		ReportPrinter jp = runChecker(pf);
		return jp;
	}

	public ReportPrinter runChecker(ProgramFactory pf) {
		bixie.util.Log.info("Checking");
		
		bixie.checker.ProgramAnalysis.Checker = 1;
		// Options.v().useLocationAttribute(true);
		
		if (bixie.Options.v().stopTime) {
			try {
				bixie.checker.util.Statistics.v().setLogFilePrefix(
						bixie.Options.v().getOutputFile());				
			} catch (Throwable e) {
				bixie.util.Log.error(e.toString());				
			}
		}
		ReportPrinter jp = new BasicReportPrinter();
		try {
			ProgramAnalysis.runFullProgramAnalysis(pf, jp);
		} catch (Exception e) {
			bixie.util.Log.error(e.toString());
		}
		
		return jp;
	}

}
