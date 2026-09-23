import java.awt.Button;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Sketch extends SearchApplet
{
    QueryPanel queryPanel;
    SketchCanvas[] drawAreas;
    MyTimer timer = null;
    private int sketchCount = 0;
    private int activeId = 0;
    String[] filespecs = new String[3];
    Panel buttonPanel;
    TextArea[] text;
    Button[] undoButton;
    Button[] clearButton;
    Button clearAllButton;
    private GridBagLayout layout = new GridBagLayout();
    private long sessionId;

    public Sketch() {
        sessionId = 0;
    }

    public long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(long id)
    {
        sessionId = id;
        System.out.println("Session ID set to " + sessionId);
    }

    public boolean all_filled() {
        boolean bool = true;
        for (int i = 0; i < sketchCount; i++) {
            if (drawAreas[i].is_empty())
                bool = false;
        }
        if (!bool)
            System.out.println("not all drawing areas have pixels");
        return bool;
    }
    
    private void create_layout() {
        System.out.println("  doing layout");
        this.setLayout(layout);
        buttonPanel.setLayout(layout);
        for (int i = 0; i < sketchCount; i++) {
            Layout.constrain(this, drawAreas[i], 1, i * 3 + 2,
                             3, 3, 1, 10, 3.0, 1.0, 2, 2, 2, 2);
            Layout.constrain_field(buttonPanel,
                                   text[i], 0, i * 3);
            Layout.constrain_button(buttonPanel,
                                    undoButton[i], 0,
                                    i * 3 + 1);
            Layout.constrain_button(buttonPanel,
                                    clearButton[i], 0,
                                    i * 3 + 2);
        }
        Layout.constrain(this, queryPanel, 0, 0, 4, 2, 3, 10,
                         1.0, 1.0, 2, 2, 2, 2);
        Layout.constrain(this, buttonPanel, 0, 2, 1,
                         3 * sketchCount, 3, 10, 0.75, 1.0, 2, 2, 2, 2);
    }
    
    private void create_user_interface() {
        System.out.println("Sketch::create_user_interface, creating "
                           + sketchCount + " drawing areas");
        this.setBackground(Color.white);
        System.out.println("  creating panels");
        queryPanel = new QueryPanel(this);
        buttonPanel = new Panel();
        buttonPanel.setBackground(Color.white);
        buttonPanel.setForeground(Color.black);
        drawAreas = new SketchCanvas[sketchCount];
        for (int i = 0; i < sketchCount; i++)
            drawAreas[i] = new SketchCanvas(i, this);
        System.out.println("  creating buttons");
        undoButton = new Button[3];
        for (int i = 0; i < sketchCount; i++) {
            undoButton[i] = new Button(" Undo ");
            undoButton[i].setFont(Globals.buttonFont);
            undoButton[i]
                .setActionCommand(Integer.toString(i));
            undoButton[i]
                .setBackground(Globals.buttonBgColour);
            undoButton[i]
                .setForeground(Globals.buttonFgColour);
            undoButton[i]
                .addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionevent) {
                    int i_1_
                        = Integer.parseInt(actionevent.getActionCommand());
                    if (((Sketch) Sketch.this).drawAreas[i_1_].getStatus()
                        != 4)
                        ((Sketch) Sketch.this).drawAreas[i_1_].undo_segment();
                }
            });
        }
        clearButton = new Button[3];
        for (int i = 0; i < sketchCount; i++) {
            clearButton[i] = new Button(" Clear ");
            clearButton[i].setFont(Globals.buttonFont);
            clearButton[i]
                .setActionCommand(Integer.toString(i));
            clearButton[i]
                .setBackground(Globals.buttonBgColour);
            clearButton[i]
                .setForeground(Globals.buttonFgColour);
            clearButton[i]
                .addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionevent) {
                    int i_3_
                        = Integer.parseInt(actionevent.getActionCommand());
                    if (((Sketch) Sketch.this).drawAreas[i_3_].getStatus()
                        != 4)
                        ((Sketch) Sketch.this).drawAreas[i_3_].clear_points();
                }
            });
        }
        text = new TextArea[3];
        text[0] = new TextArea(" View\n     1", 2, 5, 3);
        text[1] = new TextArea("  View\n     2", 2, 5, 3);
        text[2] = new TextArea("  View\n     3", 2, 5, 3);
        for (int i = 0; i < sketchCount; i++) {
            text[i].setFont(Globals.textFont);
            text[i].setEditable(false);
            text[i].setBackground(Color.white);
            text[i].setForeground(Color.black);
        }
    }
    
    private void disableButtons() {
        for (int i = 0; i < sketchCount; i++) {
            undoButton[i].setEnabled(false);
            clearButton[i].setEnabled(false);
        }
    }
    
    public void disable_search() {
        queryPanel.disable_search();
    }
    
    public void doSearch() {
        int i;

        if ( !queryPanel.getSearching() ) {
            disable_search();
            Flag.set_loading();
            sendImages();
            StringBuffer stringbuffer
                = queryPanel.construct_url();
            stringbuffer.append("&input=text_2d&method=fourier");
            for ( i = 1; i <= 3; i++) {
                stringbuffer.append("&file" + i + "="
                                    + filespecs[i - 1]);
            }
            queryPanel.request_url(stringbuffer);
        }
    }
    
    private void enableButtons() {
        for (int i = 0; i < sketchCount; i++) {
            undoButton[i].setEnabled(true);
            clearButton[i].setEnabled(true);
        }
    }
    
    public void enable_search() {
        queryPanel.enable_search();
        if (timer != null)
            timer.start();
    }
    
    public MyTimer getTimer() {
        return timer;
    }
    
    public void init() {
        sketchCount = Integer.parseInt(this.getParameter("nr_sketches"));
        create_user_interface();
        create_layout();
        set_help_strings();
    }
    
    public void notify_canvas_active(int i) {
        if (timer == null)
            timer = new MyTimer("draw_timer");
        activeId = i;
        for (int i_4_ = 0; i_4_ < sketchCount; i_4_++) {
            if (i_4_ != i)
                drawAreas[i_4_].setStatus(0);
            else
                drawAreas[i_4_].setStatus(1);
            drawAreas[i_4_].paint();
        }
    }
    
    public void select_database(int i) {
        if (i == 5) {
            drawAreas[0]
                .set_help_string(0, "2D sketch is unavailable");
            drawAreas[0]
                .set_help_string(1, "for the Protein Database");
            drawAreas[0]
                .set_help_string(2, "Enter a PDB ID as a query,");
            drawAreas[0].set_help_string(3,
                                                          "or 'random' for");
            drawAreas[0]
                .set_help_string(4, "100 random proteins");
            setStatus(4);
        } else {
            for (int i_5_ = 0; i_5_ < 5; i_5_++)
                drawAreas[0].set_help_string(i_5_, "");
            setStatus(0);
        }
    }
    
    private void sendImages() {
        int i;

        System.out.println("Sketch::sendImages");
        if ( timer != null ) {
            timer.stop();
        }
        if ( drawAreas[0].getStatus() == 4 ) {
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
                submit.send(this, drawAreas, sketchCount);
                System.out.println("calling receive");
                setStatus(3);
                for ( i = 0; i < sketchCount; i++) {
                    filespecs[i] = new String("null");
                }
                submit.receive(this, filespecs);
                System.out
                    .println("receive called, nr_sketches is " + sketchCount);
                for ( i = 0; i < sketchCount; i++) {
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
        drawAreas[1]
            .set_help_string(2, "Left mouse button = draw");
        drawAreas[1]
            .set_help_string(3, "Right mouse button = erase");
    }
    
    public void set_inactive() {
        System.out.println("set_inactive()");
        setStatus(0);
    }
    
    private void setStatus(int i) {
        for (int i_6_ = 0; i_6_ < sketchCount; i_6_++) {
            drawAreas[i_6_].setStatus(i);
            drawAreas[i_6_].paint();
        }
    }
}
