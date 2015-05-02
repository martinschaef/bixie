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

public class TestBouncyCastle  {
	
	@Test
	public void test() {
		String dir = System.getProperty("user.dir")+"/regression/bouncycastle/bouncycastle/org";
		System.out.println("Test: " + dir);
		Bixie bx = new Bixie();
		String jar = dir;
		String cp = null ;
		bx.translateAndRun(jar, cp , "./report.txt");
		org.junit.Assert.assertTrue(true);		
	}

}
