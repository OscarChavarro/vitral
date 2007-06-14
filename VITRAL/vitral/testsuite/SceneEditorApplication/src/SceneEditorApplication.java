// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity
// involved. This will help him to dominate the involved libraries.

// Java basic classes
import java.io.FileReader;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.Border; 
import javax.swing.BorderFactory; 
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// JOGL Classes
import javax.media.opengl.GLCanvas;

// VSDK Classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.RayableObject;

// Internal classes
import gui.GuiCache;

public class SceneEditorApplication {
    // Application model
    public Scene theScene;

    // Application GUI
    public JoglDrawingArea drawingArea;
    public JLabel statusMessage;
    private JFrame mainWindowWidget;
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
        theScene = new Scene();
    }

    private JPanel createStatusBar()
    {
        JPanel panel;

        statusMessage = new JLabel("VSDK Editor example - copyright Pontificia Universidad Javeriana");
        Border border = BorderFactory.createLoweredBevelBorder();
        statusMessage.setBorder(border);

        panel = new JPanel();
        panel.setLayout(new GridLayout());

        border = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        panel.setBorder(border);

        panel.add(statusMessage);

        return panel;
    }

    private JTabbedPane createPanel()
    {
        JTabbedPane container;
        JPanel panel;
        JButton button;
        JScrollPane sp;

        container = new JTabbedPane();

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 1);
        sp = new JScrollPane(panel);
        container.addTab("Creation", null, sp, "Object creation operations");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 2);
        sp = new JScrollPane(panel);
        container.addTab("GUI Control", null, sp, "GUI Control");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 3);
        sp = new JScrollPane(panel);
        container.addTab("Scene components", null, sp, "Control the scene components");
        //-----------------------------------------------------------------

        return container;
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
        Toolkit tk = mainWindowWidget.getToolkit();
        Dimension d = tk.getScreenSize();

        //-----------------------------------------------------------------
        JMenuBar menubar;
        JSplitPane splitPane;

        menubar = buildMenu();
        drawingArea = new JoglDrawingArea(theScene);
        JPanel statusBar = createStatusBar();
        Component left = drawingArea.getCanvas();
        Component right = createPanel();
        Dimension minleft = new Dimension(160, 120);
        Dimension minright = new Dimension(320, 120);
        JPanel iconsAndWorkAreasPanel;

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                   left,
                                   right);
        left.setMinimumSize(minleft);
        right.setMinimumSize(minright);
        splitPane.setResizeWeight(1.0);

        Dimension dd = splitPane.getMaximumSize();

        dd.width = Short.MAX_VALUE;
        splitPane.setAlignmentX(0.5f);
        splitPane.setMaximumSize(dd);

        iconsAndWorkAreasPanel = new JPanel();
        iconsAndWorkAreasPanel.setLayout(
            new BoxLayout(iconsAndWorkAreasPanel, BoxLayout.Y_AXIS));
        iconsAndWorkAreasPanel.add(new ButtonsPanel(this, 101));
        iconsAndWorkAreasPanel.add(splitPane);

        mainWindowWidget.add(iconsAndWorkAreasPanel, BorderLayout.CENTER);
        mainWindowWidget.add(statusBar, BorderLayout.SOUTH);
        mainWindowWidget.setJMenuBar(menubar);

        //-----------------------------------------------------------------
        int ancho_panel;

        if ( d.width < 640 ) ancho_panel = 320;
        ancho_panel = d.width - 320;

        splitPane.setDividerLocation(ancho_panel);

        mainWindowWidget.setPreferredSize(d);
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

    public ButtonsPanel(SceneEditorApplication parent, int group)
    {
        //-------------------------------------------------------------------
        this.parent = parent;

        if ( group < 100 ) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border empty = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            this.setBorder(empty);
        }
        else {
            //this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        }

        //-------------------------------------------------------------------
        JButton b = null;

        switch ( group ) {
          case 1:
            b = new JButton("Create Sphere");
            configureButton(b);

            b = new JButton("Create Cone/Cylinder");
            configureButton(b);

            b = new JButton("Create Cube");
            configureButton(b);

            b = new JButton("Create Box");
            configureButton(b);

            b = new JButton("Create Arrow");
            configureButton(b);

            b = new JButton("Create Mesh");
            configureButton(b);

            break;

          case 2:
            b = new JButton("Motif");
            configureButton(b);

            b = new JButton("Java");
            configureButton(b);

            b = new JButton("Windows");
            configureButton(b);

            b = new JButton("GTK+");
            configureButton(b);

            break;

          case 3:
            b = new JButton("Cycle background");
            configureButton(b);

            b = new JButton("Toggle test corridor");
            configureButton(b);

            b = new JButton("Print scene report in console");
            configureButton(b);

            break;

          case 101:
            b = new JButton("New");
            configureButton2(b);

            b = new JButton("Load");
            configureButton2(b);

            b = new JButton("Save");
            configureButton2(b);

            b = new JButton("CameraMode");
            configureButton2(b);

            b = new JButton("SelectMode");
            configureButton2(b);

            b = new JButton("TranslateMode");
            configureButton2(b);

            b = new JButton("RotateMode");
            configureButton2(b);

            b = new JButton("ScaleMode");
            configureButton2(b);

            break;

        }
    }

    private void configureButton(JButton b)
    {
        Dimension d = b.getMaximumSize();

        d.width = Short.MAX_VALUE;
        b.setAlignmentX(0.5f);
        b.setMaximumSize(d);
        b.addActionListener(this);
        add(b);
    }

    private void configureButton2(JButton b)
    {
        b.setAlignmentX(0);
        b.setAlignmentY(0);
        b.addActionListener(this);
        add(b);
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        RayableObject thing;

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
        else if ( label == "Cycle background" ) {
            parent.drawingArea.rotateBackground();
        }
        else if ( label == "Create Sphere" ) {
            thing = new RayableObject();
            thing.setGeometry(new Sphere(1));
            thing.setPosition(new Vector3D());
            thing.setRotation(new Matrix4x4());
            thing.setRotationInverse(new Matrix4x4());
            thing.setMaterial(new Material());
            parent.theScene.things.add(thing);
        }
        else if ( label == "Toggle test corridor" ) {
            if ( parent.theScene.showCorridor == true ) {
                parent.theScene.showCorridor = false;
            }
            else {
                parent.theScene.showCorridor = true;
            }
        }
        else if ( label == "Print scene report in console" ) {
            parent.theScene.print();
        }
        else if ( label == "CameraMode" ) {
            parent.statusMessage.setText("Camera mode interaction - drag mouse with different buttons over the scene to change current camera.");
            parent.drawingArea.interactionMode = 
                parent.drawingArea.CAMERA_INTERACTION_MODE;
        }
        else if ( label == "SelectMode" ) {
            parent.statusMessage.setText("Selection mode interaction - click mouse to select objects.");
            parent.drawingArea.interactionMode = 
                parent.drawingArea.SELECT_INTERACTION_MODE;
        }

        parent.drawingArea.canvas.repaint();
    }
}
