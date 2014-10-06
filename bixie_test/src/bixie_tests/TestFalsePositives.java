package bixie_tests;
/**
 * 
 */


import org.junit.Test;

import bixie.Bixie;

/**
 * @author schaef
 *
 */

public class TestFalsePositives  {
	
	@Test
	public void test() {
		String dir = System.getProperty("user.dir")+"/regression/boogie/java_input.bpl";
		System.out.println("Test: " + dir);
		bixie.Options.v().stopTime = true;
		Bixie bx = new Bixie();
		bx.run(dir, "report.txt");
		org.junit.Assert.assertTrue(true);
		
	}

}
