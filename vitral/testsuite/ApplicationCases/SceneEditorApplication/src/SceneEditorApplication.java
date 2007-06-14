//===========================================================================
//= This is the main class for a simple 3D editor application, and serves   =
//= as the main testbed integration for functionalities in the VSDK toolkit =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - Since august 2005 - Oscar Chavarro                                    =
//===========================================================================

// Java basic classes
import java.io.File;
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
import javax.swing.JFrame;
import javax.swing.JFileChooser;
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
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;

import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.io.XmlException;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;

// Internal classes
import vsdk.transition.gui.GuiCache;
import vsdk.transition.io.presentation.GuiCachePersistence;
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

abstract class SuffixAwareFilter
    extends javax.swing.filechooser.FileFilter {

  public String getSuffix(File f) {
    String s = f.getPath(), suffix = null;
    int i = s.lastIndexOf('.');

    if (i > 0 && i < s.length() - 1) {
      suffix = s.substring(i + 1).toLowerCase();
    }

    return suffix;
  }

  public boolean accept(File f) {
    return f.isDirectory();
  }

}

class MyFilter
    extends SuffixAwareFilter {
  private String suffix;
  private String description;

  public MyFilter(String suffix, String description) {
    this.suffix = suffix;
    this.description = description;
  }

  public boolean accept(File f) {
    boolean accept = super.accept(f);
    if (!accept) {
      String _suffix = getSuffix(f);
      if (suffix != null) {
        accept = suffix.equals(_suffix);
      }
    }
    return accept;
  }

  public String getDescription() {
    return description + " (*." + suffix + ")";
  }
}

public class SceneEditorApplication {
    // Application model
    public Scene theScene;
    public RGBImage raytracedImage;
    public RGBImage zbufferImage;
    public int raytracedImageWidth;
    public int raytracedImageHeight;
    public RGBColorPalette palette;

    // Application GUI
    public GuiCache gui;
    public JoglDrawingArea drawingArea;
    public JLabel statusMessage;
    public AwtImageControlWindow imageControlWindow;
    public ButtonsPanel executorPanel;
    private JFrame mainWindowWidget;
    private String lookAndFeel;
    public String languageGuiFile;

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

    /**
    Could be better: if the Swing GUI is not destroyed, but all labels are
    renamed... but ... what if language files are not exactly equal?
    */
    public void setGuiLanguage(String lang)
    {
        this.languageGuiFile = lang;
        destroyGUI();
        createGUI();
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        theScene = new Scene();

        raytracedImage = new RGBImage();
        raytracedImageWidth = 320;
        raytracedImageHeight = 240;

        palette = null;
        try {
            palette = RGBColorPalettePersistence.importGimpPalette(new java.io.FileReader("../../../etc/palettes/Cranes.gpl"));
        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }

    }

    private JPanel createStatusBar()
    {
        JPanel panel;

        statusMessage = new JLabel(gui.getMessage("IDM_INTRO_MESSAGE"));
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
        container.addTab(gui.getMessage("IDM_CREATION_TAB"), 
            null, sp, "Object creation operations");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 2);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_GUI_TAB"), 
            null, sp, "GUI Control");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 3);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_OTHERS_TAB"), 
            null, sp, "Control the scene components");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 4);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_RENDER_TAB"), 
            null, sp, "Control the scene components");
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
        try {
            gui = GuiCachePersistence.importAquynzaGui(
                                new FileReader(languageGuiFile)  );
        }
        catch (Exception e) {
            System.err.println("Fatal error: can not open GUI file");
            System.exit(0);
        }
        //System.out.println(gui);

        //-----------------------------------------------------------------
        executorPanel = new ButtonsPanel(this, 101);

        //-----------------------------------------------------------------
        JMenuBar menubar;

        menubar = SwingGuiCacheRenderer.buildMenubar(gui, null, executorPanel);

        //-----------------------------------------------------------------
        JSplitPane splitPane;
        JPanel statusBar = createStatusBar();

        drawingArea = new JoglDrawingArea(theScene, statusMessage, this);
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
        iconsAndWorkAreasPanel.add(executorPanel);
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

        //-----------------------------------------------------------------
        imageControlWindow = null;
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
        languageGuiFile = "./etc/english.gui";

        createModel();
        createGUI();
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
            // This is a button group inside right tab panels
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Border empty = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            this.setBorder(empty);
        }
        else {
            // This is a button group part of an icon bar
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border empty = BorderFactory.createEmptyBorder(0, 0, 0, 0);
            this.setBorder(empty);
        }

        //-------------------------------------------------------------------
        JPanel internal = null;

        switch ( group ) {
          case 1:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "CREATION", this);
            break;
          case 2:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "GUI", this);
            break;
          case 3:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "OTHER", this);
            break;
          case 4:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "RENDER", this);
            break;
          case 101:
            internal = 
            SwingGuiCacheRenderer.buildButtonGroup(parent.gui, "GLOBAL", this);
            break;
        }

        if ( internal != null ) {
            this.add(internal, BorderLayout.WEST);
        }

        //-------------------------------------------------------------------
    }

    private Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        return m;
    }

    private void addThing(Geometry g)
    {
        SimpleBody thing;

        thing = new SimpleBody();
        thing.setGeometry(g);
        thing.setPosition(new Vector3D());
        thing.setRotation(new Matrix4x4());
        thing.setRotationInverse(new Matrix4x4());
        thing.setMaterial(defaultMaterial());
        parent.theScene.things.add(thing);
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        // This makes event compatible with ButtonGroup scheme of event
        // handling
        if ( ev.getSource().getClass().getName().equals(
             "javax.swing.JButton") ) {
            JButton origin = (JButton)ev.getSource();
            label = origin.getName();
        }

        Light light;

        //- FILE ----------------------------------------------------------
        if ( label.equals("IDC_FILE_QUIT") ) {
            System.exit(0);
        }
        //- EDIT ----------------------------------------------------------
        //- CREATE --------------------------------------------------------
        else if ( label.equals("IDC_CREATE_SPHERE") ) {
            addThing(new Sphere(1));
        }
        else if ( label.equals("IDC_CREATE_CONE") ) {
            addThing(new Cone(1, 0, 2));
        }
        else if ( label.equals("IDC_CREATE_CYLINDER") ) {
            addThing(new Cone(1, 1, 2));
        }
        else if ( label.equals("IDC_CREATE_CUBE") ) {
            addThing(new Box(1, 1, 1));
        }
        else if ( label.equals("IDC_CREATE_BOX") ) {
            addThing(new Box(1, 2, 3));
        }
        else if ( label.equals("IDC_CREATE_ARROW") ) {
            addThing(new Arrow(0.7, 0.3, 0.05, 0.1));
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICCUBICCURVE") ) {
            ParametricCurve curve;

            // Case 1: curve hard-coded in source
            Vector3D pointParameters[];

            curve = new ParametricCurve();
            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0, 0, 0);
            pointParameters[1] = new Vector3D(0, 0, 0); // Not used
            pointParameters[2] = new Vector3D(0, 1, 0);
            curve.addPoint(pointParameters, curve.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(1, 1, 0);
            pointParameters[1] = new Vector3D(0, 1, 0);
            pointParameters[2] = new Vector3D(2, 1, 0);
            curve.addPoint(pointParameters, curve.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(2, 0, 1);
            pointParameters[1] = new Vector3D(2, 0, 0);
            pointParameters[2] = new Vector3D(0, 0, 0); // Not used
            curve.addPoint(pointParameters, curve.BEZIER);

            addThing(curve);

/*
            try {
                XmlManager.exportXml(curve, "curveTest.xml",
                                     "../../../etc/xml/vsdk.dtd");
            } catch (XmlException ex1) {
                System.out.println("EXPORT:XmlException:" +ex1);
            }
*/
/*
            // Case 2: curve read from a previous existing data file
            try {
                curve = (ParametricCurve)XmlManager.importXml(
                          "curveTest.xml");
                addThing(curve);
              }
              catch (XmlException ex1) {
                System.out.println("IMPORT:XmlException:" + ex1);
            }
*/
        }
        else if ( label.equals("IDC_CREATE_PARAMETRICBICUBICPATCH") ) {
            ParametricBiCubicPatch patch;

            // Case 1: Patch hard-coded in source
            ParametricCurve border = new ParametricCurve();
            Vector3D pointParameters[];
            pointParameters = new Vector3D[3];

            pointParameters[0] = new Vector3D(0, 0, 0);
            pointParameters[1] = new Vector3D(0.5, 0, 0);
            pointParameters[2] = new Vector3D(0, 0, 0.5);
            border.addPoint(pointParameters, border.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0.5, 0, 1);
            pointParameters[1] = new Vector3D(0, 0, 1);
            pointParameters[2] = new Vector3D(1, 0, 1);
            border.addPoint(pointParameters, border.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0.5, 1, 1);
            pointParameters[1] = new Vector3D(1, 1, 1);
            pointParameters[2] = new Vector3D(0, 1, 1);
            border.addPoint(pointParameters, border.BEZIER);

            pointParameters = new Vector3D[3];
            pointParameters[0] = new Vector3D(0, 1, 0);
            pointParameters[1] = new Vector3D(0, 1, 0.5);
            pointParameters[2] = new Vector3D(0.5, 1, 0);
            border.addPoint(pointParameters, border.BEZIER);

            border.addPoint(border.getPoint(0), border.BEZIER);
            patch = new ParametricBiCubicPatch(ParametricBiCubicPatch.QUAD,
                                               border);
            addThing(patch);
/*
            try {
                XmlManager.exportXml(patch, "patchTest.xml",
                                     "../../../etc/xml/vsdk.dtd");
              }
              catch (XmlException ex2) {
                 System.out.println("EXPORT:XmlException:" +ex2);
            }
*/
/*
            // Case 2: patch read from a previous existing data file
            try {
                patch = (ParametricBiCubicPatch) XmlManager.importXml(
                         "patchTest.xml");
                addThing(patch);
              }
              catch (XmlException ex1) {
                System.out.println("IMPORT:XmlException:" +ex1);
            }
*/
        }
        else if ( label.equals("IDC_IMPORT_OBJECTS_FROM_FILE") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../../etc/geometry");
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new MyFilter("obj", "obj Alias/Wavefront text mesh"));
            jfc.addChoosableFileFilter(new MyFilter("3ds", "3ds Kinetix/Discreet 3DStudio/3DStudioMax binary scene file"));
            int opc = jfc.showOpenDialog(new JPanel());
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();

                    EnvironmentPersistence.importEnvironment(file,
                        parent.theScene.things, null, null, null);

                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to read file");
                    return;
                }
            }
        }
        else if ( label.equals("IDC_CREATE_OMNILIGHT") ) {
            light = new Light(Light.POINT, new Vector3D(-10, -9, 8), new ColorRgb(1, 1, 1));
            parent.theScene.lights.add(light);
        }
        //- RENDERING -----------------------------------------------------
        else if ( label.equals("Select palette for depthmap display") ||
                  label.equals("IDC_RENDERING_SELECTPALETTEDEPTH") ) {
            JFileChooser jfc = null;
            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../../etc/palettes");
            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new MyFilter("gpl", "gpl Gimp Palettes"));
            int opc = jfc.showOpenDialog(new JPanel());
            if (opc == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = jfc.getSelectedFile();

                    parent.palette = 
                        RGBColorPalettePersistence.importGimpPalette(
                            new java.io.FileReader(file.getAbsolutePath()));

                    repaint();
                }
                catch (Exception ex) {
                    System.out.println("Failed to read file");
                    return;
                }
            }
        }
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERIMAGE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_PENDING_ZBUFFER_COLOR_IMAGE"));
            parent.drawingArea.wantToGetColor = true;
        }
        else if ( label.equals("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_PENDING_ZBUFFER_DEPTH"));
            parent.drawingArea.wantToGetDepth = true;
        }
        else if ( label.equals("IDC_RENDERING_RAYTRACING") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_COMPUTING_RAYTRACING"));
            parent.raytracedImage.init(parent.raytracedImageWidth, parent.raytracedImageHeight);
            parent.theScene.raytrace(parent.raytracedImage);
            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new AwtImageControlWindow(parent.raytracedImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.raytracedImage);
            }
            parent.imageControlWindow.redrawImage();
        }
        //- CUSTOMIZE -----------------------------------------------------
        else if ( label.equals("IDC_CUSTOMIZE_LAF_MOTIF") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_JAVA") ) {
            parent.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_GTK") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LAF_WINDOWS") ) {
            parent.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LANGUAGE_ENGLISH") ) {
            parent.setGuiLanguage("./etc/english.gui");
        }
        else if ( label.equals("IDC_CUSTOMIZE_LANGUAGE_SPANISH") ) {
            parent.setGuiLanguage("./etc/spanish.gui");
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_OTHERS_CYCLE_BACKGROUND") ) {
            parent.drawingArea.rotateBackground();
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_TEST_CORRIDOR") ) {
            if ( parent.theScene.showCorridor == true ) {
                parent.theScene.showCorridor = false;
            }
            else {
                parent.theScene.showCorridor = true;
            }
        }
        else if ( label.equals("IDC_OTHERS_TOGGLE_GRID") ) {
            if ( parent.theScene.showGrid == true ) {
                parent.theScene.showGrid = false;
            }
            else {
                parent.theScene.showGrid = true;
            }
        }
        else if ( label.equals("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE") ) {
            parent.theScene.print();
        }
        //-----------------------------------------------------------------
        else if ( label.equals("IDC_TOOLS_CAMERA") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_CAMERA_MODE"));
            parent.drawingArea.interactionMode = 
                parent.drawingArea.CAMERA_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_SELECT") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_SELECTION_MODE"));
            parent.drawingArea.interactionMode = 
            parent.drawingArea.SELECT_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_TRANSLATE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_TRANSLATION_MODE"));
            parent.drawingArea.interactionMode = 
            parent.drawingArea.TRANSLATE_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_ROTATE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_ROTATION_MODE"));
            parent.drawingArea.interactionMode = 
                parent.drawingArea.ROTATE_INTERACTION_MODE;
        }
        else if ( label.equals("IDC_TOOLS_SCALE") ) {
            parent.statusMessage.setText(
                parent.gui.getMessage("IDM_SCALE_MODE"));
            parent.drawingArea.interactionMode = 
                parent.drawingArea.SCALE_INTERACTION_MODE;
        }

        //-----------------------------------------------------------------
        parent.drawingArea.canvas.repaint();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
