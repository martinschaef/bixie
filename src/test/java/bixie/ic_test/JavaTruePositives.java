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

import bixie.Bixie;

@RunWith(Parameterized.class)
public class JavaTruePositives extends AbstractIcTest {

	private File sourceFile, goldenFile;

	@Parameterized.Parameters(name = "{index}: check ({1})")
	public static Collection<Object[]> data() {
		List<Object[]> filenames = new LinkedList<Object[]>();
		final File source_dir = new File(testRoot + "ic_java/true_positives/");
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

	public JavaTruePositives(File source, String name) {
		this.sourceFile = source;
		this.goldenFile = new File(source.getAbsolutePath().replace(".java", ".gold"));
	}

	@Test
	public void test() {
		System.out.println("Running test: "+sourceFile.getName());
		File classFileDir = null;
		File outFile = null;
		try {
			outFile = File.createTempFile("bixie_test", ".txt");
			classFileDir = compileJavaFile(this.sourceFile);
			if (classFileDir==null || !classFileDir.isDirectory()) {
				assertTrue(false);
			}
			Bixie bx = new Bixie();
			String outFilePath = outFile.getAbsolutePath();
			// if no golden output has been generated for this test,
			// generate one using the current result.
			boolean firstRun = false;
			if (!this.goldenFile.isFile()) {
				firstRun = true;
				outFilePath = this.goldenFile.getAbsolutePath();
			}
			
			bx.translateAndRun(classFileDir.getAbsolutePath(),
					classFileDir.getAbsolutePath(), outFilePath);
			if (!firstRun) {
				assertTrue(this.compareFiles(outFile, this.goldenFile));
			} else {
				assertTrue(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		} finally {
			if (classFileDir != null) {
				try {
					delete(classFileDir);
					if (outFile != null && outFile.isFile()) {
						outFile.delete();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}		
	}

}
