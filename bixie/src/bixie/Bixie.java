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

		if (args.length == 0) {
			parser.printUsage(System.err);
			return;
		}

		try {
			parser.parseArgument(args);

			Bixie bixie = new Bixie();
			if (options.getBoogieFile() != null && options.getJarFile() != null) {
				org.gravy.util.Log
						.error("Can only take either Java or Boogie input. Not both");
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
			org.gravy.util.Log.error(e.toString());
			parser.printUsage(System.err);
		} catch (Throwable e) {
			org.gravy.util.Log.error(e.toString());
		}
	}

	public void run(String input, String output) {
		bixie.Options.v().setOutputFile(output);
		if (input != null && input.endsWith(".bpl")) {
			try {
				ProgramFactory pf = new ProgramFactory(input);
				InterpolatingJavaReportPrinter jp = runChecker(pf);
				report2File(jp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				org.gravy.util.Log.error(e.toString());
			}
		} else {
			org.gravy.util.Log.error("Not a valid Boogie file: " + input);
		}
	}

	protected void report2File(InterpolatingJavaReportPrinter jp) {
		try (PrintWriter out = new PrintWriter(bixie.Options.v()
				.getOutputFile());) {
			String str = jp.printAllReports();
			out.println(str);
			org.gravy.util.Log.info(str);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			org.gravy.util.Log.error(e.toString());
		}
	}

	public void translateAndRun(String input, String classpath, String output) {
		InterpolatingJavaReportPrinter jp = translateAndRun(input, classpath);
		bixie.Options.v().setOutputFile(output);
		report2File(jp);
	}

	public InterpolatingJavaReportPrinter translateAndRun(String input,
			String classpath) {
		org.gravy.util.Log.info("Translating");
		org.joogie.Dispatcher.setClassPath(classpath);
		ProgramFactory pf = org.joogie.Dispatcher.run(input);
		if (pf == null) {
			org.gravy.util.Log.error("Internal Error: Parsing failed");
			return null;
		}
		InterpolatingJavaReportPrinter jp = runChecker(pf);
		return jp;
	}

	public InterpolatingJavaReportPrinter runChecker(ProgramFactory pf) {
		org.gravy.util.Log.info("Checking");
		org.gravy.Options.v().setTimeOut(Options.v().getTimeout() * 1000);
		org.gravy.Options.v().setChecker(4);
		// Options.v().useLocationAttribute(true);
		org.gravy.Options.v().setLoopMode(1);
		if (bixie.Options.v().stopTime) {
			try {
				org.gravy.util.Statistics.v().setLogFilePrefix(
						bixie.Options.v().getOutputFile());
				org.gravy.Options.v().stopTime = true;
			} catch (Throwable e) {
				org.gravy.util.Log.error(e.toString());
				org.gravy.Options.v().stopTime = false;
			}
		}
		InterpolatingJavaReportPrinter jp = new InterpolatingJavaReportPrinter();
		try {
			ProgramAnalysis.runFullProgramAnalysis(pf, jp);
		} catch (Exception e) {
			org.gravy.util.Log.error(e.toString());
		}
		return jp;
	}

}
