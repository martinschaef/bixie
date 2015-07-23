/**
 * 
 */
package bixie.ic_test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import bixie.Bixie;
import bixie.checker.reportprinter.ReportPrinter;

/**
 * @author schaef
 *
 */
public class MainClassTest extends AbstractIcTest {

	@Test
	public void test() {
		final File source_file = new File(testRoot + "ic_java/false_positives/FalsePositives01.java");
		File classFileDir = null;
		try {
			classFileDir = compileJavaFile(source_file);
			Bixie.main(new String[]{"-j", classFileDir.getAbsolutePath()});
		} catch (IOException e) {		
			e.printStackTrace();
			fail("Not yet implemented");
		} finally {
			if (classFileDir != null) {
				try {
					delete(classFileDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}

}
