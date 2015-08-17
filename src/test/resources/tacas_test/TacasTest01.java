package tacas_test;

import java.io.File;

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
	
}
