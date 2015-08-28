package tacas_test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class TacasTest01 {

	public void bad01(File f) {		
		File parent = f.getParentFile();
		if (!parent.isDirectory()) {
			System.err.print("Hallo");
		}
	}
	
	public void good01(File f, boolean b) {		
		File parent = f.getParentFile();
		System.out.println(parent);
		System.out.println(f.getParent());
		f.listFiles();
	}

	protected void fp02(File file, List<File> jarFiles) {
		try {
			// open JAR file
			JarFile jarFile = new JarFile(file);

			// get manifest and their main attributes
			Manifest manifest = jarFile.getManifest();
			// empty class path?
			if (null == manifest) {
				System.out.println("No manifest found in jar "+jarFile.getName());
				jarFile.close();
				return;
			}

			Attributes mainAttributes = manifest.getMainAttributes();
			String classPath = mainAttributes
					.getValue(Attributes.Name.CLASS_PATH);

			// close JAR file
			jarFile.close();

			// empty class path?
			if (null == classPath)
				return;

			// look for dependent JARs
			String[] classPathItems = classPath.split(" ");
			for (String classPathItem : classPathItems) {
				if (classPathItem.endsWith(".jar")) {
					// add jar
					System.out.println("Adding " + classPathItem
							+ " to Soot's class path");
					jarFiles.add(new File(file.getParent(), classPathItem));
				}
			}

		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}
}
