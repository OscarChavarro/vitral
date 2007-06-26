public class Flag
{
    private static boolean loading = false;
    
    private Flag() {
	;
    }
    
    public static synchronized void set_done() {
	System.out.println("Flag.set_done");
	if (!loading)
	    System.out.println("flag was false, should have been true");
	loading = false;
    }
    
    public static synchronized void set_loading() {
	System.out.println("Flag.set_loading");
	loading = true;
    }
    
    public static synchronized boolean still_loading() {
	System.out.println("Flag.still_loading");
	return loading;
    }
}
