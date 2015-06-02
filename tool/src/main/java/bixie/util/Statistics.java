/**
 * 
 */
package bixie.checker.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import bixie.checker.faultlocalization.FaultLocalizationThread;
import bixie.checker.report.Report;

/**
 * @author schaef
 *
 */
public class Statistics {
	
	public static Integer HACK_effectualSetSize = 0;

	private static Statistics instance = null;

	public static Statistics v() {
		if (instance == null) {
			instance = new Statistics();
		}
		return instance;
	}

	private Statistics() {

	}

	public static void resetInstance() {
		if (instance != null) {
			try {
				if (instance.checkerBuffer != null)
					instance.checkerBuffer.close();
				if (instance.faultLocBuffer != null)
					instance.faultLocBuffer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		instance = null;
	}

	// ---------------------

	private BufferedWriter checkerBuffer = null;
	private BufferedWriter faultLocBuffer = null;

	public void setLogFilePrefix(String s) throws Throwable {

		try (OutputStreamWriter cfw = new OutputStreamWriter(
				new FileOutputStream(s + "_checker.csv"), "UTF-8");
				OutputStreamWriter ffw = new OutputStreamWriter(
						new FileOutputStream(s + "_faultloc.csv"), "UTF-8");) {
			this.checkerBuffer = new BufferedWriter(cfw);
			this.faultLocBuffer = new BufferedWriter(ffw);

		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void writeCheckerStats(String procname, Integer lines,
			Double millisecs, Report report) {
		if (this.checkerBuffer != null) {
			try {

				this.checkerBuffer.write(procname);
				this.checkerBuffer.write(", ");
				this.checkerBuffer.write(lines.toString());
				this.checkerBuffer.write(", ");
				Double secs = (millisecs / 1000.0); // convert to seconds
				this.checkerBuffer.write(secs.toString());

				this.checkerBuffer.write(", -1");

				this.checkerBuffer.write(", ");
				this.checkerBuffer.write(Statistics.HACK_effectualSetSize
						.toString());
				this.checkerBuffer.write("\n");
				this.checkerBuffer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * hack
	 */
	public void writeFaultLocalizationStats(String procname, Double millisecs,
			Boolean timeout) {
		if (this.faultLocBuffer != null) {
			try {
				this.faultLocBuffer.write(procname);
				this.faultLocBuffer.write(", ");
				Double secs = (millisecs / 1000.0); // convert to seconds
				this.faultLocBuffer.write(secs.toString());
				this.faultLocBuffer.write(", ");
				this.faultLocBuffer
						.write(FaultLocalizationThread.DEBUG_ProofObligations
								.toString());
				this.faultLocBuffer.write(", ");
				this.faultLocBuffer
						.write(FaultLocalizationThread.DEBUG_AbstractTrace
								.toString());
				this.faultLocBuffer.write(", ");
				this.faultLocBuffer.write(timeout.toString());
				this.faultLocBuffer.write("\n");
				this.faultLocBuffer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
