import java.awt.Button;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Sketch extends SearchApplet
{
    QueryPanel query_panel;
    SketchCanvas[] draw_areas;
    MyTimer timer = null;
    private int nr_sketches = 0;
    private int active_id = 0;
    String[] filespecs = new String[3];
    Panel button_panel;
    TextArea[] text;
    Button[] undo_button;
    Button[] clear_button;
    Button clear_all_button;
    private GridBagLayout my_layout = new GridBagLayout();
    
    public boolean all_filled() {
        boolean bool = true;
        for (int i = 0; i < nr_sketches; i++) {
            if (draw_areas[i].is_empty())
                bool = false;
        }
        if (!bool)
            System.out.println("not all drawing areas have pixels");
        return bool;
    }
    
    private void create_layout() {
        System.out.println("  doing layout");
        this.setLayout(my_layout);
        button_panel.setLayout(my_layout);
        for (int i = 0; i < nr_sketches; i++) {
            Layout.constrain(this, draw_areas[i], 1, i * 3 + 2,
                             3, 3, 1, 10, 3.0, 1.0, 2, 2, 2, 2);
            Layout.constrain_field(button_panel,
                                   text[i], 0, i * 3);
            Layout.constrain_button(button_panel,
                                    undo_button[i], 0,
                                    i * 3 + 1);
            Layout.constrain_button(button_panel,
                                    clear_button[i], 0,
                                    i * 3 + 2);
        }
        Layout.constrain(this, query_panel, 0, 0, 4, 2, 3, 10,
                         1.0, 1.0, 2, 2, 2, 2);
        Layout.constrain(this, button_panel, 0, 2, 1,
                         3 * nr_sketches, 3, 10, 0.75, 1.0, 2, 2, 2, 2);
    }
    
    private void create_user_interface() {
        System.out.println("Sketch::create_user_interface, creating "
                           + nr_sketches + " drawing areas");
        this.setBackground(Color.white);
        System.out.println("  creating panels");
        query_panel = new QueryPanel(this);
        button_panel = new Panel();
        button_panel.setBackground(Color.white);
        button_panel.setForeground(Color.black);
        draw_areas = new SketchCanvas[nr_sketches];
        for (int i = 0; i < nr_sketches; i++)
            draw_areas[i] = new SketchCanvas(i, this);
        System.out.println("  creating buttons");
        undo_button = new Button[3];
        for (int i = 0; i < nr_sketches; i++) {
            undo_button[i] = new Button(" Undo ");
            undo_button[i].setFont(Globals.button_font);
            undo_button[i]
                .setActionCommand(Integer.toString(i));
            undo_button[i]
                .setBackground(Globals.button_bg_colour);
            undo_button[i]
                .setForeground(Globals.button_fg_colour);
            undo_button[i]
                .addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionevent) {
                    int i_1_
                        = Integer.parseInt(actionevent.getActionCommand());
                    if (((Sketch) Sketch.this).draw_areas[i_1_].get_status()
                        != 4)
                        ((Sketch) Sketch.this).draw_areas[i_1_].undo_segment();
                }
            });
        }
        clear_button = new Button[3];
        for (int i = 0; i < nr_sketches; i++) {
            clear_button[i] = new Button(" Clear ");
            clear_button[i].setFont(Globals.button_font);
            clear_button[i]
                .setActionCommand(Integer.toString(i));
            clear_button[i]
                .setBackground(Globals.button_bg_colour);
            clear_button[i]
                .setForeground(Globals.button_fg_colour);
            clear_button[i]
                .addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionevent) {
                    int i_3_
                        = Integer.parseInt(actionevent.getActionCommand());
                    if (((Sketch) Sketch.this).draw_areas[i_3_].get_status()
                        != 4)
                        ((Sketch) Sketch.this).draw_areas[i_3_].clear_points();
                }
            });
        }
        text = new TextArea[3];
        text[0] = new TextArea(" View\n     1", 2, 5, 3);
        text[1] = new TextArea("  View\n     2", 2, 5, 3);
        text[2] = new TextArea("  View\n     3", 2, 5, 3);
        for (int i = 0; i < nr_sketches; i++) {
            text[i].setFont(Globals.text_font);
            text[i].setEditable(false);
            text[i].setBackground(Color.white);
            text[i].setForeground(Color.black);
        }
    }
    
    private void disableButtons() {
        for (int i = 0; i < nr_sketches; i++) {
            undo_button[i].setEnabled(false);
            clear_button[i].setEnabled(false);
        }
    }
    
    public void disable_search() {
        query_panel.disable_search();
    }
    
    public void doSearch() {
	int i;

        if ( !query_panel.get_searching() ) {
            disable_search();
            Flag.set_loading();
            sendImages();
            StringBuffer stringbuffer
                = query_panel.construct_url();
            stringbuffer.append("&input=text_2d&method=fourier");
            for ( i = 1; i <= 3; i++) {
                stringbuffer.append("&file" + i + "="
                                    + filespecs[i - 1]);
	    }
            query_panel.request_url(stringbuffer);
        }
    }
    
    private void enableButtons() {
        for (int i = 0; i < nr_sketches; i++) {
            undo_button[i].setEnabled(true);
            clear_button[i].setEnabled(true);
        }
    }
    
    public void enable_search() {
        query_panel.enable_search();
        if (timer != null)
            timer.start();
    }
    
    public MyTimer get_timer() {
        return timer;
    }
    
    public void init() {
        nr_sketches = Integer.parseInt(this.getParameter("nr_sketches"));
        create_user_interface();
        create_layout();
        set_help_strings();
    }
    
    public void notify_canvas_active(int i) {
        if (timer == null)
            timer = new MyTimer("draw_timer");
        active_id = i;
        for (int i_4_ = 0; i_4_ < nr_sketches; i_4_++) {
            if (i_4_ != i)
                draw_areas[i_4_].setStatus(0);
            else
                draw_areas[i_4_].setStatus(1);
            draw_areas[i_4_].paint();
        }
    }
    
    public void select_database(int i) {
        if (i == 5) {
            draw_areas[0]
                .set_help_string(0, "2D sketch is unavailable");
            draw_areas[0]
                .set_help_string(1, "for the Protein Database");
            draw_areas[0]
                .set_help_string(2, "Enter a PDB ID as a query,");
            draw_areas[0].set_help_string(3,
                                                          "or 'random' for");
            draw_areas[0]
                .set_help_string(4, "100 random proteins");
            setStatus(4);
        } else {
            for (int i_5_ = 0; i_5_ < 5; i_5_++)
                draw_areas[0].set_help_string(i_5_, "");
            setStatus(0);
        }
    }
    
    private void sendImages() {
        int i;

        System.out.println("Sketch::sendImages");
        if ( timer != null ) {
            timer.stop();
        }
        if ( draw_areas[0].get_status() == 4 ) {
            System.out
                .println("  skipping because sketch canvases are disabled");
          }
          else {
            Submit submit = new Submit();
            boolean bool = submit.connect(this);
            if ( bool ) {
                System.out.println
                    ("Sketch::sendImages, connect says ok, calling send");
                disableButtons();
                setStatus(2);
                submit.send(draw_areas, nr_sketches);
                System.out.println("calling receive");
                setStatus(3);
                for ( i = 0; i < nr_sketches; i++) {
                    filespecs[i] = new String("null");
                }
                submit.receive(filespecs);
                System.out
                    .println("receive called, nr_sketches is " + nr_sketches);
                for ( i = 0; i < nr_sketches; i++) {
                    System.out.println("  filespec " + (i + 1) + ": "
                                       + filespecs[i]);
                    if ( filespecs[i] == null ) {
                        filespecs[i] = "null";
		    }
                }
                enableButtons();
                setStatus(0);
              }
              else {
                System.out.println("Error connecting to server");
            }
        }
    }
    
    private void set_help_strings() {
        draw_areas[1]
            .set_help_string(2, "Left mouse button = draw");
        draw_areas[1]
            .set_help_string(3, "Right mouse button = erase");
    }
    
    public void set_inactive() {
        System.out.println("set_inactive()");
        setStatus(0);
    }
    
    private void setStatus(int i) {
        for (int i_6_ = 0; i_6_ < nr_sketches; i_6_++) {
            draw_areas[i_6_].setStatus(i);
            draw_areas[i_6_].paint();
        }
    }
}
