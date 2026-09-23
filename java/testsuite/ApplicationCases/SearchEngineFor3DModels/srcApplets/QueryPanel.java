import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.net.URLConnection;

public class QueryPanel extends Panel
{
    private static final int SEARCH_TIMEOUT = 40;
    Button searchButton;
    private static final String searchString = new String("   Search   ");
    private static final String searchingString = new String("  Searching  ");
    Choice databaseList;
    TextField keywordsText;
    TextField query;
    private static final int QUERY_LENGTH = 20;
    boolean searching = false;
    private Sketch parentApplet;
    
    QueryPanel(Sketch searchapplet) {
        parentApplet = searchapplet;
        searchButton = new Button(searchString);
        searchButton.setFont(Globals.searchButtonFont);
        searchButton
            .setBackground(Globals.searchButtonBgColour);
        searchButton
            .setForeground(Globals.searchButtonFgColour);
        searchButton
            .addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionevent) {
                System.out.println("search button pressed");
                parentApplet.doSearch();
            }
        });
        databaseList = new Choice();
        databaseList.addItem("All Models");
        databaseList.addItem("Free Web Models");
/*
        db_list.addItem("Viewpoint Models");
        db_list.addItem("De Espona Models");
        db_list.addItem("CacheForce Models");
        db_list.addItem("Protein Database");
*/
        databaseList.select(0);
        databaseList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemevent) {
                parentApplet.select_database(((QueryPanel) QueryPanel.this)
                                                  .databaseList.getSelectedIndex());
            }
        });
        keywordsText = new TextField("Keywords: ", 10);
        keywordsText.setFont(Globals.textFont);
        keywordsText.setEditable(false);
        keywordsText.setBackground(Color.white);
        query = new TextField(20);
        query.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionevent) {
                System.out.println("emter key pressed");
                parentApplet.doSearch();
            }
        });
        this.setBackground(Color.white);
        this.setLayout(new GridBagLayout());
        create_layout();
    }
    
    public StringBuffer construct_url() {
        StringBuffer stringbuffer
            = (new StringBuffer(parentApplet.getCodeBase() + "ServletConsole?"));
        stringbuffer.append("session=" + parentApplet.getSessionId());
        stringbuffer.append("&textquery=" + getQuery_string());
        stringbuffer.append("&dataset=");
        int i = getSelected_index();
        switch (i) {
        case 0:
            stringbuffer.append("all");
            break;
        case 1:
            stringbuffer.append("web");
            break;
        case 2:
            stringbuffer.append("viewpoint");
            break;
        case 3:
            stringbuffer.append("espona");
            break;
        case 4:
            stringbuffer.append("cacheforce");
            break;
        case 5:
            stringbuffer.append("pdb");
            break;
        }
        return stringbuffer;
    }
    
    void create_layout() {
        Layout.constrain(this, searchButton, 0, 0, 2, 1,
                         0, 10, 2.0, 0.75, 2, 2, 2, 2);
        Layout.constrain(this, databaseList, 2, 0, 2, 1, 0, 10,
                         2.0, 0.75, 4, 2, 0, 2);

/*
        Layout.constrain(this, keywords_text, 0, 1, 2, 1,
                         0, 10, 1.0, 0.75, 2, 2, 2, 2);
        Layout.constrain(this, query, 2, 1, 2, 1, 0, 10,
                         1.0, 0.75, 2, 2, 2, 2);
*/
    }
    
    public void disable_search() {
        searching = true;
        searchButton.setEnabled(false);
        searchButton.setLabel(searchingString);
    }
    
    public void enable_search() {
        System.out.println("enabling search button");
        searchButton.setLabel(searchString);
        searchButton.setEnabled(true);
        searching = false;
    }
    
    public String getQuery_string() {
        return query.getText();
    }
    
    public boolean getSearching() {
        return searching;
    }
    
    public int getSelected_index() {
        return databaseList.getSelectedIndex();
    }
    
    public void request_url(StringBuffer stringbuffer) {
        System.out.println("request_url(" + (Object) stringbuffer + ")");
        if ( !searching ) {
            disable_search();
        }
        try {
            URL url = new URL(stringbuffer.toString());
            URLConnection urlconnection = url.openConnection();
            urlconnection.setUseCaches(false);
            urlconnection.setDoInput(true);
            urlconnection.setDoOutput(true);
            urlconnection.setRequestProperty
                ("Content-Type", "application/x-www-form-urlencoded");
            parentApplet.getAppletContext().showDocument(url,
                                                    Globals.resultsFrameName);
            System.out.println("Sending results to frame [" + Globals.resultsFrameName + "]");
            System.out.println("done");
/*
            int i = 0;
            while ( searching ) {
                if (i >= 40) break;
                Thread.sleep(1000L);
                i++;
            }
*/
          }
          catch (Exception exception) {
            System.out.println(exception);
        }
        if (searching) {
            System.out.println("enabling after timeout");
            enable_search();
        }
    }
}
