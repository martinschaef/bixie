package bixie.checker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import typechecker.TypeChecker;
import bixie.Options;
import bixie.checker.inconsistency_checker.AbstractChecker;
import bixie.checker.inconsistency_checker.CdcChecker;
import bixie.checker.inconsistency_checker.GreedyCfgChecker;
import bixie.checker.report.Report;
import bixie.checker.reportprinter.ReportPrinter;
import bixie.util.Log;
import bixie.util.Statistics;
import boogie.ProgramFactory;
import boogie.controlflow.AbstractControlFlowFactory;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.DefaultControlFlowFactory;

/**
 * @author schaef
 * 
 */
public class ProgramAnalysis {

	public static long timeouts = 0;
	public static long totalTime = 0;
	public static long overheadTime = 0;

	public static void runFullProgramAnalysis(ProgramFactory pf,
			ReportPrinter rp) {

		GlobalsCache.v().setProgramFactory(pf);
		TypeChecker tc = new TypeChecker(pf.getASTRoot());
		// build the control-flow graphs
		DefaultControlFlowFactory cff = new DefaultControlFlowFactory(
				pf.getASTRoot(), tc);

//		String fname = bixie.checker.Options.v().getFunctionName();

		Long checkTime = 0L;
		overheadTime = 0L;

		for (CfgProcedure p : cff.getProcedureCFGs()) {
			// if option is set to analyze only one procedure,
			// continue if this is not the right proc.
//			if (fname != null && !p.getProcedureName().equals(fname)) {
//				continue;
//			}
			// continue if the procedure has no body.
			if (p.getRootNode() == null)
				continue;

			try {
				// StopWatch sw = StopWatch.getInstanceAndStart();

				Report report = analyzeProcedure(p, cff);

				// sw.stop();
				// Long t = sw.getTime();
				// checkTime += t;
				// overheadTime += Atva15Checker.overhead_time;

				// if (Options.v().stopTime) {
				// Statistics.v().writeCheckerStats(p.getProcedureName(),
				// countStatementsForStatistics(p), t.doubleValue(),
				// report);
				// }

				if (report != null ) {
					// sw = null;
					// if (Options.v().stopTime) {
					// sw = StopWatch.getInstanceAndStart();
					// }
					report.runFaultLocalization(); // do the interpolation based fault
										// localization here to avoid timeouts.
										// if (Options.v().stopTime) {
					// t = sw.getTime();
					// sw.stop();
					// boolean timeout = (report instanceof
					// InterpolationInfeasibleReport) ?
					// ((InterpolationInfeasibleReport) report).timeout
					// : false;
					// Statistics.v().writeFaultLocalizationStats(
					// p.getProcedureName(), t.doubleValue(), timeout);
					// }
					rp.printReport(report);
				}

			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}

		totalTime += checkTime;
		Log.info("Total time: " + ((float) checkTime) / 1000f + "s");
		Log.info("Overhead time: " + ((float) overheadTime) / 1000f + "s");
		Log.info("Total Timeouts after " + bixie.Options.v().getTimeout() + "sec: "
				+ timeouts);

		GlobalsCache.resetInstance();		
		Statistics.resetInstance();
	}
	
	private static AbstractChecker getChecker(AbstractControlFlowFactory cff,
			CfgProcedure p) {
		AbstractChecker checker = null;

		switch (Options.v().getSelectedChecker()) {
		case 1: {
//			checker = new CdcChecker(cff, p);
			checker = new GreedyCfgChecker(cff, p);
			break;
		}
		case 2: {
			checker = new CdcChecker(cff, p);
			break;
		}
		default: {
			checker = new GreedyCfgChecker(cff, p);
			break;
		}
		}
		return checker;
	}

	private static Report analyzeProcedure(CfgProcedure p,
			AbstractControlFlowFactory cff) {
		if (bixie.Options.v().getDebugMode()) {
			Log.info("Checking: " + p.getProcedureName());
		}
		// create an executor to kill the verification with a timeout if
		// necessary
		ExecutorService executor = Executors.newSingleThreadExecutor();

		AbstractChecker checkerThread = getChecker(cff, p);

		final Future<?> future = executor.submit(checkerThread);

		boolean exception = false;

		try {
			// start thread and wait xx seconds. If timeout is set to 0, wait
			// until it terminates.
			if (bixie.Options.v().getTimeout() > 0) {
				future.get(bixie.Options.v().getTimeout(), TimeUnit.SECONDS);
			} else {
				future.get();
			}
			Log.debug("Finished method " + p.getProcedureName());
		} catch (TimeoutException e) {
			// set timeout to method info
			// methodInfo.setTimeout(true);
			timeouts++;
			Log.info("Timeout reached for method " + p.getProcedureName());
			exception = true;
		} catch (OutOfMemoryError e) {
			Log.info("Out of memory for " + p.getProcedureName());
			exception = true;
		} catch (Exception e) {
			e.printStackTrace();
			exception = true;
		} finally {
			// cancel thread if not done
			if (!future.isDone()) {
				future.cancel(true);
			}

			// shutdown prover
			checkerThread.shutDownProver();

			// shutdown executor
			executor.shutdown();

		}

		Report report = checkerThread.getReport();
		if (exception)
			return null;
		return report;
	}

}
