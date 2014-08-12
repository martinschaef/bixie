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

public class TestSnippets  {
	
	@Test
	public void test() {
		String dir = System.getProperty("user.dir")+"/regression/snippets";
		System.out.println("Test: " + dir);
		Bixie bx = new Bixie();
		bx.run(dir);
		org.junit.Assert.assertTrue(true);
		
	}

}
