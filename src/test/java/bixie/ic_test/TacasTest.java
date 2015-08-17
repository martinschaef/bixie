package bixie.ic_test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import bixie.Main;

@RunWith(Parameterized.class)
public class TacasTest extends AbstractIcTest {

	private File sourceFile;

	@Parameterized.Parameters(name = "{index}: check ({1})")
	public static Collection<Object[]> data() {
		List<Object[]> filenames = new LinkedList<Object[]>();
		final File source_dir = new File(testRoot + "tacas_test/");
		File[] directoryListing = source_dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.isFile() && child.getName().endsWith(".java")) {
					filenames.add(new Object[] { child, child.getName() });
				} else {
					// Ignore
				}
			}
		} else {
			// Handle the case where dir is not really a directory.
			// Checking dir.isDirectory() above would not be sufficient
			// to avoid race conditions with another process that deletes
			// directories.
			throw new RuntimeException("Test data not found!");
		}
		return filenames;
	}

	public TacasTest(File source, String name) {
		this.sourceFile = source;		
	}

	@Test
	public void testTacas() {
		System.out.println("Running test: "+sourceFile.getName());
		File classFileDir = null;
		File outFile = null;
		try {
			outFile = File.createTempFile("bixie_test", ".txt");
			classFileDir = compileJavaFile(this.sourceFile);
			if (classFileDir==null || !classFileDir.isDirectory()) {
				assertTrue(false);
			}
			Main bx = new Main();
			bixie.Options.v().setSelectedChecker(3);
			
			bx.translateAndRun(classFileDir.getAbsolutePath(),
					classFileDir.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		} finally {
			if (classFileDir != null) {
				try {
					delete(classFileDir);
					if (outFile != null && outFile.isFile()) {
						if (!outFile.delete()) {
							System.err.println("Failed to delete file");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}		
	}

}
