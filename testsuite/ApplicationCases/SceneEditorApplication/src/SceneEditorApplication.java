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
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.ArrayList;

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
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.VoxelVolume;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.io.XmlException;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.ImageProcessing;

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
    public boolean withVisualDebugRay;
    public Ray visualDebugRay;
    public int visualDebugRayLevels;

    // Application GUI
    public GuiCache gui;
    public JoglDrawingArea drawingArea;
    public JLabel statusMessage;
    public JPanel statusBarPanel;
    public SwingImageControlWindow imageControlWindow;
    public SwingSelectorDialog selectorDialog;
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

        visualDebugRay = new Ray(new Vector3D(0, -3, 0), new Vector3D(0, 1, 0));
        visualDebugRayLevels = 2;
        withVisualDebugRay = false;

    }

    private JPanel createStatusBar()
    {
        JPanel statusBarPanel;

        statusMessage = new JLabel(gui.getMessage("IDM_INTRO_MESSAGE"));
        Border border = BorderFactory.createLoweredBevelBorder();
        statusMessage.setBorder(border);

        statusBarPanel = new JPanel();
        statusBarPanel.setLayout(new GridLayout());

        border = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        statusBarPanel.setBorder(border);
        statusBarPanel.add(statusMessage);

        return statusBarPanel;
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
        statusBarPanel = createStatusBar();

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
        mainWindowWidget.add(statusBarPanel, BorderLayout.SOUTH);
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
        selectorDialog = null;
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

    public void doRaytracedImage()
    {
        raytracedImage.init(raytracedImageWidth, raytracedImageHeight);
        if ( theScene.selectedBackground == 1 ) {
            ImageProcessing.resize(theScene.fixedBackground.getImage(), raytracedImage);
        }
        theScene.raytrace(raytracedImage);
    }

    public static void main(String[] args) {
        // Note that this is a thread-safe invocation of the GUI
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SceneEditorApplication();
            }
        });
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
