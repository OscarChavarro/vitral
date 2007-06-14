// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity
// involved. This will help him to dominate the involved libraries.

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.media.opengl.GLCanvas;

import vitral.toolkits.environment.Camera;

public class SceneEditorApplication {
    // Application model
    private Camera camera;

    // Application GUI
    private JFrame mainWindowWidget;
    private ButtonsPanel controls;
    private JMenuBar menubar;
    JoglDrawingArea drawingArea;
    private JSplitPane splitPane;
    private String lookAndFeel;

    public void setLookAndFeel(String lookAndFeel)
    {
        this.lookAndFeel = lookAndFeel;

        // Method 1
        destroyGUI();
        createGUI();

        // Method 2: It doesn't work well when the window decoration style
        // has to change in the main JFrame
        /*
        try {
            UIManager.setLookAndFeel(lookAndFeel);
          }
          catch (Exception e) {
            System.err.println("Warning: Can not set " +
              lookAndFeel + "look and feel");
        }
        SwingUtilities.updateComponentTreeUI(mainWindowWidget);
        mainWindowWidget.pack();
        */
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        camera = new Camera();
    }

    private void createGUI()
    {
        //- Configure the application Look & feel -------------------------
        try {
            UIManager.setLookAndFeel(lookAndFeel);
          }
          catch (Exception e) {
            System.err.println("Warning: Can not set " +
              lookAndFeel + "look and feel");
        }
        JFrame.setDefaultLookAndFeelDecorated(true);

        //- Configure this JFrame -----------------------------------------
        mainWindowWidget = new JFrame("VITRAL Scene Editor");
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //-----------------------------------------------------------------
        menubar = buildMenu();
        drawingArea = new JoglDrawingArea(camera);
        controls = new ButtonsPanel(this);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                   drawingArea.getCanvas(),
                                   new JButton("test"));
        splitPane.setOneTouchExpandable(true);

        mainWindowWidget.add(splitPane, BorderLayout.CENTER);
        mainWindowWidget.add(controls, BorderLayout.SOUTH);
        mainWindowWidget.setJMenuBar(menubar);

        //-----------------------------------------------------------------
        splitPane.setDividerLocation(640/3*2);
        mainWindowWidget.setPreferredSize(new Dimension (1024, 768));

        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        drawingArea.getCanvas().requestFocusInWindow();
    }

    private void destroyGUI()
    {
        mainWindowWidget.setVisible(false);
        mainWindowWidget.dispose();
        System.gc();
        mainWindowWidget = null;
        System.gc();
    }

    public SceneEditorApplication() {
        lookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";

        createModel();
        createGUI();
    }

    public JMenuBar buildMenu()
    {
        JMenuBar menubar;
        GuiCache guiReader = null;

        try {
            guiReader = new GuiCache(new FileReader("./etc/spanish.gui"));
        }
        catch (Exception e) {
            System.err.println("Fatal error: can not open GUI file");
            System.exit(0);
        }

        menubar = guiReader.exportSwingMenubar();

        return menubar;
    }

    public static void main (String[] args) {
        // Note that this is a thread-safe invocation of the GUI
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SceneEditorApplication();
            }
        });
    }

}

class ButtonsPanel extends JPanel implements ActionListener
{
    SceneEditorApplication parent;

    public ButtonsPanel(SceneEditorApplication parent)
    {
        JButton b = null;

        b = new JButton("Motif");
        b.addActionListener(this);
        add(b);

        b = new JButton("Java");
        b.addActionListener(this);
        add(b);

        b = new JButton("Windows");
        b.addActionListener(this);
        add(b);

        b = new JButton("GTK+");
        b.addActionListener(this);
        add(b);

        b = new JButton("Rotar fondo");
        b.addActionListener(this);
        add(b);

        this.parent = parent;
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        if ( label == "Motif" ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        }
        else if ( label == "Java" ) {
            parent.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        }
        else if ( label == "GTK+" ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        }
        else if ( label == "Windows" ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        else if ( label == "Rotar fondo" ) {
            parent.drawingArea.rotateBackground();
            parent.drawingArea.canvas.repaint();
        }
    }
}
