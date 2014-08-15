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
		if (args.length!=1) {
			System.err.println("Pass path to java files or jar-filename as input.");
			return;
		}
		
		String javaFileDir = args[0];
		Bixie m = new Bixie();
		m.run(javaFileDir, null);		
	}

	public void run(String input) {
		run(input, "");
	}
	
	public void run(String input, String classpath) {
		System.out.println("Translating");
		
		redirectLoggers();
			
		org.joogie.Dispatcher.setClassPath(input + classpath);
		ProgramFactory pf = org.joogie.Dispatcher.run(input);		
		runChecker(pf);
	}

	public void runChecker(ProgramFactory pf) {
		pf.toFile("./tmp.bpl");
		System.out.println("Checking");
		org.gravy.Options.v().setChecker(1);
		//Options.v().useLocationAttribute(true);
		org.gravy.Options.v().setLoopMode(1);
		try {
			ProgramAnalysis.runFullProgramAnalysis(pf, new JavaReportPrinter());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done");
	}
	
	private void redirectLoggers() {
		//SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
//		ch.qos.logback.classic.Logger log;
//		log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("soot.PackManager");
//		log.setLevel(Level.ERROR);
//		log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("soot.SootResolver");
//		log.setLevel(Level.ERROR);
	}
	
	/*
#log4j.rootLogger=INFO, stdout, stderr
log4j.rootLogger=INFO, stdout, stderr, file

# configure stdout
log4j.appender.stdout = org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Threshold = INFO
log4j.appender.stdout.Target = System.out
log4j.appender.stdout.layout = org.apache.log4j.SimpleLayout
log4j.appender.stdout.filter.filter1 = org.apache.log4j.varia.LevelRangeFilter
log4j.appender.stdout.filter.filter1.levelMin = DEBUG
log4j.appender.stdout.filter.filter1.levelMax = INFO

# configure stderr
log4j.appender.stderr = org.apache.log4j.ConsoleAppender
log4j.appender.stderr.Threshold = ERROR
log4j.appender.stderr.Target = System.err
log4j.appender.stderr.layout = org.apache.log4j.SimpleLayout

# configure file
log4j.appender.file = org.apache.log4j.RollingFileAppender
log4j.appender.file.Append = false
log4j.appender.file.file = ./gravy.log
log4j.appender.file.file.threshold = INFO
log4j.appender.file.layout = org.apache.log4j.SimpleLayout

	 */
	
	
}
