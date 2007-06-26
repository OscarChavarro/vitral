import java.applet.Applet;

public class SearchApplet extends Applet implements Search
{
    public void doSearch() {
	System.out.println
	    ("Error: should not call SearchApplet.do_search() directly.");
    }
    
    public void select_database(int i) {
	System.out.println
	    ("Error: should not call SearchApplet.select_database() directly.");
    }
}
