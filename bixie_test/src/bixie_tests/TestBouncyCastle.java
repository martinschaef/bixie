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
		String dir = System.getProperty("user.dir")+"/regression/bouncycastle/java";
		System.out.println("Test: " + dir);
		Bixie bx = new Bixie();
		bx.run(dir);
		org.junit.Assert.assertTrue(true);		
	}

}
