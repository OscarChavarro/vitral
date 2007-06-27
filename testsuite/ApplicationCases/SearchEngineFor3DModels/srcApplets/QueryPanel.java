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
    Button search_button;
    private static final String search_string = new String("   Search   ");
    private static final String searching_string = new String("  Searching  ");
    Choice db_list;
    TextField keywords_text;
    TextField query;
    private static final int QUERY_LENGTH = 20;
    boolean searching = false;
    private SearchApplet parent_applet;
    
    QueryPanel(SearchApplet searchapplet) {
        parent_applet = searchapplet;
        search_button = new Button(search_string);
        search_button.setFont(Globals.search_button_font);
        search_button
            .setBackground(Globals.search_button_bg_colour);
        search_button
            .setForeground(Globals.search_button_fg_colour);
        search_button
            .addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionevent) {
                System.out.println("search button pressed");
                parent_applet.doSearch();
            }
        });
        db_list = new Choice();
        db_list.addItem("All Models");
        db_list.addItem("Free Web Models");
/*
        db_list.addItem("Viewpoint Models");
        db_list.addItem("De Espona Models");
        db_list.addItem("CacheForce Models");
        db_list.addItem("Protein Database");
*/
        db_list.select(0);
        db_list.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent itemevent) {
                parent_applet.select_database(((QueryPanel) QueryPanel.this)
                                                  .db_list.getSelectedIndex());
            }
        });
        keywords_text = new TextField("Keywords: ", 10);
        keywords_text.setFont(Globals.text_font);
        keywords_text.setEditable(false);
        keywords_text.setBackground(Color.white);
        query = new TextField(20);
        query.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionevent) {
                System.out.println("emter key pressed");
                parent_applet.doSearch();
            }
        });
        this.setBackground(Color.white);
        this.setLayout(new GridBagLayout());
        create_layout();
    }
    
    public StringBuffer construct_url() {
        StringBuffer stringbuffer
            = (new StringBuffer(parent_applet.getCodeBase() + "ServletConsole?"));
        stringbuffer.append("textquery=" + get_query_string());
        stringbuffer.append("&dataset=");
        int i = get_selected_index();
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
        Layout.constrain(this, search_button, 0, 0, 2, 1,
                         0, 10, 2.0, 0.75, 2, 2, 2, 2);
        Layout.constrain(this, db_list, 2, 0, 2, 1, 0, 10,
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
        search_button.setEnabled(false);
        search_button.setLabel(searching_string);
    }
    
    public void enable_search() {
        System.out.println("enabling search button");
        search_button.setLabel(search_string);
        search_button.setEnabled(true);
        searching = false;
    }
    
    public String get_query_string() {
        return query.getText();
    }
    
    public boolean get_searching() {
        return searching;
    }
    
    public int get_selected_index() {
        return db_list.getSelectedIndex();
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
            parent_applet.getAppletContext().showDocument(url,
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
