

/**
 * @author schaef
 *
 */
public abstract class InfeasibleCode02 {

	private final Object welcomeFilesLock = new Object();
    private String welcomeFiles[] = new String[0];
    
    public String[] findWelcomeFiles() {
    	if (welcomeFiles==null)
        synchronized (welcomeFilesLock) {
        	if (welcomeFiles==null)
        		return (welcomeFiles);
        }
    	return null;
    }
	
//	public void infeasible01()
//	{
//		String customFont = "";
//		if(customFont != null && customFont.length() > 0)
//		{
//			System.err.println("From Terpword, UnicodeDialog.java, line 287.");
//			return;
//		}
//		else
//		{
//			return;
//		}
//	}
//	
//
//	public void feasible01(String source) {
//		boolean hit = false;
//		String idString;
//		int counter = 0;
//		do
//		{
//			hit = false;
//			idString = "diesisteineidzumsuchenimsource" + counter;
//			if(source.indexOf(idString) > -1)
//			{
//				counter++;
//				hit = true;
//				if(counter > 10000)
//				{
//					return;
//				}
//			}
//		} while(hit);		
//	}
//	
//	
//	public void feasibe02(Object htmlTag) {
//		if (htmlTag.toString().compareTo("strike")==0)
//		{
//			String[] str = {"text-decoration","strike"};			
//		}
//		else if (htmlTag.toString().compareTo("sup")==0)
//		{
//			String[] str = {"vertical-align","sup"};			
//		}
//		else if (htmlTag.toString().compareTo("sub")==0)
//		{
//			String[] str = {"vertical-align","sub"};			
//		}
//	}

//	int x;
//	public void feasible01() {
//		boolean hit = false;
//		int counter = 0;
//		do
//		{
//			hit = false;			
//			if(x==counter)
//			{
//				counter++;
//				hit = true;
//				if(counter > 10000)
//				{
//					return;
//				}
//			}
//		} while(hit);		
//	}
//
//	
//	
//	public int infeasible0(int[] arr) {
//		int i = arr.length;
//		arr[3]=3;
//		return arr[i]; // INFEASIBLE
//	}
//
//	public int infeasible1(Object o) {
//		if (o!=null) {
//			return o.hashCode(); // INFEASIBLE
//		} 
//		System.err.println(o.toString() + " does not exist");
//		return 2;
//	}
//
//	public void infeasible2(int [] arr) {
//		for (int i=0; i<=arr.length;i++) {
//			arr[i]=i; // INFEASIBLE
//		}
//	}
//	
//	public void infeasible3(int a, int b) {
//		b=1; // ALL INFEASIBLE
//		if (a>0) b--;
//		b=1/b;
//		if (a<=0) b=1/(1-b);
//	}
//	
//	public boolean infeasible4(Object o) {
//		System.err.println(o.toString());
//		if (o==null) {
//			return false; // INFEASIBLE
//		}
//		return true;
//	}
//	
//	public void infeasible5() {
//		String test="too long";
//		if (test.length()==3) {
//			System.err.println("unreachable"); // INFEASIBLE
//		}
//	}
//	
//	public int infeasible6(int[] arr) {
//		return arr[-1] + arr[arr.length]; // INFEASIBLE
//	}	
//		
//	
//	public void infeasible07(char[] temp) {
//		int repos = -1;
//		int end = -1;
//		int j = end;
//		do {
//			j++;
//			if (temp[j]=='a') {
//				repos = j - end - 1;
//			}
//		} while (repos == -1 && j < temp.length);
//		if (repos == -1) {
//			repos=0; //unreachable
//		}
//	}
	
}



