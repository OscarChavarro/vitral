//===========================================================================
//= This is the main class for a simple 3D editor application, and serves   =
//= as the main testbed integration for functionalities in the VSDK toolkit =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - Since august 2005 - Oscar Chavarro                                    =
//===========================================================================

package application;

// Java basic classes
import java.io.FileInputStream;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import javax.swing.border.Border;
import javax.swing.BorderFactory; 
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

// VSDK Classes
import vsdk.toolkit.common.VSDK; 
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.processing.ImageProcessing;

// Internal classes
import vsdk.toolkit.gui.Gui;
import vsdk.toolkit.gui.variable.GuiVariable;
import vsdk.toolkit.io.gui.GuiPersistence;
import vsdk.toolkit.render.swing.SwingGuiRenderer;

// Application classes
import application.framework.Scene;
import application.gui.ModifyPanel;
import application.gui.ButtonsPanel;
import application.gui.GUIEventExecutor;
import application.gui.MyChangeListener;
import application.gui.SwingSelectorDialog;
import application.gui.SwingImageControlWindow;
import application.net.VitralEditorServer;
import application.net.VitralCommandClient;
import application.render.jogl.JoglDrawingArea;


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
    public Gui gui;
    public JoglDrawingArea drawingArea;
    public JLabel statusMessage;
    public JPanel statusBarPanel;
    public SwingImageControlWindow imageControlWindow;
    public SwingSelectorDialog selectorDialog;
    GUIEventExecutor executor;
    public ButtonsPanel executorPanel;
    public JFrame mainWindowWidget;
    private String lookAndFeel;
    public String languageGuiFile;
    public ModifyPanel modifyPanel;
    public boolean modifyPanelSelected;
    public boolean fullScreenGuiMode;
    private GUIEventExecutor guiEventExecutor;

    // Networking
    private VitralEditorServer networkServer;
    private VitralCommandClient networkCommandClient;
    private String currentNetworkCommandClientIp;
    private String currentNetworkCommandClientPort;

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
        catch ( Exception e ) {
            System.err.println(e);
            System.exit(0);
        }

        visualDebugRay = new Ray(new Vector3D(0, -3, 0), new Vector3D(0, 1, 0));
        visualDebugRayLevels = 2;
        withVisualDebugRay = false;

        networkServer = null;
        networkCommandClient = null;
        currentNetworkCommandClientIp = "127.0.0.1";
        currentNetworkCommandClientPort = "1235";
    }

    private JPanel createStatusBar()
    {
        JPanel newStatusBarPanel;

        statusMessage = new JLabel(gui.getMessage("IDM_INTRO_MESSAGE"));
        Border border = BorderFactory.createLoweredBevelBorder();
        statusMessage.setBorder(border);

        newStatusBarPanel = new JPanel();
        newStatusBarPanel.setLayout(new GridLayout());

        border = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        newStatusBarPanel.setBorder(border);
        newStatusBarPanel.add(statusMessage);

        return newStatusBarPanel;
    }

    private JTabbedPane createPanel()
    {
        JTabbedPane container;
        JPanel panel;
        JButton button;
        JScrollPane sp;

        container = new JTabbedPane();
        container.getModel().addChangeListener(new MyChangeListener(this));

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 1, executor);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_CREATION_TAB"), 
            null, sp, "Object creation operations");

        //-----------------------------------------------------------------
        modifyPanel = new ModifyPanel(this);
        sp = new JScrollPane(modifyPanel);
        container.addTab(gui.getMessage("IDM_MODIFY_TAB"), 
            null, sp, "Modify selected body");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 2, executor);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_GUI_TAB"), 
            null, sp, "GUI Control");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 3, executor);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_OTHERS_TAB"), 
            null, sp, "Control the scene components");

        //-----------------------------------------------------------------
        panel = new ButtonsPanel(this, 4, executor);
        sp = new JScrollPane(panel);
        container.addTab(gui.getMessage("IDM_RENDER_TAB"), 
            null, sp, "Control the scene components");
        //-----------------------------------------------------------------

        return container;
    }

    private void createGUIFullScreen()
    {
        mainWindowWidget = new JFrame("VITRAL Scene Editor");

        mainWindowWidget.setUndecorated(true);

        Toolkit tk = mainWindowWidget.getToolkit();
        Dimension d = tk.getScreenSize();

        //-----------------------------------------------------------------
        try {
            gui = GuiPersistence.importAquynzaGui(
		new FileInputStream(languageGuiFile), ".");
        }
        catch ( Exception e ) {
            System.err.println("Fatal error: can not open GUI file");
            System.exit(0);
        }

        if ( drawingArea == null ) {
            drawingArea = new JoglDrawingArea(theScene, statusMessage, this);
        }

        mainWindowWidget.add(drawingArea.getCanvas(), BorderLayout.CENTER);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        drawingArea.getCanvas().requestFocusInWindow();

        //-----------------------------------------------------------------
        imageControlWindow = null;
        selectorDialog = null;
        modifyPanelSelected = false;
    }

    private void createGUIWindowed()
    {
        //- Configure the application Look & feel -------------------------
        try {
            UIManager.setLookAndFeel(lookAndFeel);
          }
          catch ( ClassNotFoundException | InstantiationException | 
                  IllegalAccessException | UnsupportedLookAndFeelException e ) {
            VSDK.reportMessage(this, VSDK.WARNING, "createGUIWindowed", 
                "Warning: Can not set " + lookAndFeel + " look and feel\n" + e);
        }

        //- Configure this JFrame -----------------------------------------
        mainWindowWidget = new JFrame("VITRAL Scene Editor");
        mainWindowWidget.setUndecorated(false);
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Toolkit tk = mainWindowWidget.getToolkit();
        Dimension d = tk.getScreenSize();

        //-----------------------------------------------------------------
        try {
            gui = GuiPersistence.importAquynzaGui(
                new FileInputStream(languageGuiFile), ".");
        }
        catch ( Exception e ) {
            System.err.println("Fatal error: can not open GUI file");
            System.exit(0);
        }
        //System.out.println(gui);

        //-----------------------------------------------------------------
        executor = new GUIEventExecutor(this);
        executorPanel = new ButtonsPanel(this, 101, executor);

        //-----------------------------------------------------------------
        JMenuBar menubar;

        menubar = SwingGuiRenderer.buildMenubar(gui, null, executorPanel);

        //-----------------------------------------------------------------
        JSplitPane splitPane;
        statusBarPanel = createStatusBar();

        if ( drawingArea == null ) {
            drawingArea = new JoglDrawingArea(theScene, statusMessage, this);
        }

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

        //if ( d.width < 640 ) {
        //    ancho_panel = 320;
        //}
        ancho_panel = d.width - 320;

        splitPane.setDividerLocation(ancho_panel);

        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        drawingArea.getCanvas().requestFocusInWindow();

        //-----------------------------------------------------------------
        imageControlWindow = null;
        selectorDialog = null;
        modifyPanelSelected = false;
    }

    public final void createGUI()
    {
        if ( fullScreenGuiMode ) {
            createGUIFullScreen();
        }
        else {
            createGUIWindowed();
        }
    }

    public void destroyGUI()
    {
        mainWindowWidget.setVisible(false);
        mainWindowWidget.dispose();
        System.gc();
        mainWindowWidget = null;
        System.gc();
    }

    public SceneEditorApplication(String[] args) {
        //lookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
        lookAndFeel = "org.jvnet.substance.skin.SubstanceTwilightLookAndFeel";
        //lookAndFeel = "org.jvnet.substance.skin.SubstanceOfficeBlue2007LookAndFeel";
        languageGuiFile = "./etc/english.gui";

        fullScreenGuiMode = false;
        drawingArea = null;

        createModel();
        createGUI();

        int i;
        for ( i = 0; i < args.length; i++ ) {
            if ( args[i].equals("-s") ) {
                networkServer = new VitralEditorServer(this);
            }
        }
    }

    public void doRaytracedImage()
    {
        raytracedImage.init(raytracedImageWidth, raytracedImageHeight);
        if ( theScene.selectedBackground == 1 ) {
            ImageProcessing.resize(theScene.fixedBackground.getImage(), raytracedImage);
        }
        theScene.raytrace(raytracedImage);
    }

    public void switchVoiceCommandClient()
    {
        String ip, port;
        if ( networkCommandClient == null ) {
            ip = (String)JOptionPane.showInputDialog(
                mainWindowWidget, // Component parentComponent
                gui.getMessage("IDM_VOICECOMMAND_IPQUESTION"), // Object message
                gui.getMessage("IDM_VOICECOMMAND_TITLE"), // String title
                JOptionPane.QUESTION_MESSAGE, // int messageType
                null, // Icon icon
                null, // Object[] selectionValues
                currentNetworkCommandClientIp // Object initialSelectionValue
            );
            if ( ip == null ) {
                statusMessage.setText(
                    gui.getMessage("IDM_OPERATION_CANCELLED_BY_USER") + " - " +
                    gui.getMessage("IDM_VOICECOMMAND_DISABLED"));
                return;
            }
            currentNetworkCommandClientIp = ip;
            port = (String)JOptionPane.showInputDialog(
                mainWindowWidget, // Component parentComponent
                gui.getMessage("IDM_VOICECOMMAND_PORTQUESTION"), // Object message
                gui.getMessage("IDM_VOICECOMMAND_TITLE"), // String title
                JOptionPane.QUESTION_MESSAGE, // int messageType
                null, // Icon icon
                null, // Object[] selectionValues
                currentNetworkCommandClientPort // Object initialSelectionValue
            );
            if ( port == null ) {
                statusMessage.setText(
                    gui.getMessage("IDM_OPERATION_CANCELLED_BY_USER") + " - " +
                    gui.getMessage("IDM_VOICECOMMAND_DISABLED"));
                return;
            }
            currentNetworkCommandClientPort = port;
            networkCommandClient = new VitralCommandClient(this,
                currentNetworkCommandClientIp,
                Integer.parseInt(currentNetworkCommandClientPort));
            Thread ct = new Thread(networkCommandClient);
            ct.start();
            statusMessage.setText(
                gui.getMessage("IDM_VOICECOMMAND_ENABLED"));
        }
        else {
            networkCommandClient.running = false;
            statusMessage.setText(
                gui.getMessage("IDM_VOICECOMMAND_DISABLED"));
            networkCommandClient = null;
        }
    }

    public void closeApplication()
    {
        if ( networkCommandClient != null ) {
            networkCommandClient.end();
        }
        System.exit(0);
    }

    public void externalCommand(String label)
    {
        boolean b = guiEventExecutor.executeCommand(label);
    }

    public static void main(String[] args) {
        // Note that this is a thread-safe invocation of the GUI
        MainThread mt = new MainThread(args);
        javax.swing.SwingUtilities.invokeLater(mt);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
