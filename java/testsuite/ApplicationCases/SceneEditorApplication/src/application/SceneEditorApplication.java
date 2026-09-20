package application;

// Java basic classes
import java.io.FileInputStream;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import javax.swing.WindowConstants;
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
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.processing.ImageProcessing;
import vsdk.toolkit.gui.widget.Widget;
import vsdk.toolkit.io.gui.GuiPersistence;
import vsdk.toolkit.render.swing.SwingGuiRenderer;

// Application classes
import application.framework.Scene;
import application.model.ApplicationModel;
import application.gui.ModifyPanel;
import application.gui.ButtonsPanel;
import application.gui.GUIEventExecutor;
import application.gui.MyChangeListener;
import application.gui.SwingSelectorDialog;
import application.gui.SwingImageControlWindow;
import application.net.VitralEditorServer;
import application.net.VitralCommandClient;
import javax.swing.SwingUtilities;

public class SceneEditorApplication {
    // Application model
    private ApplicationModel applicationModel;

    // Application GUI
    public Widget gui;
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
    private Jogl4ApplicationController jogl4Controller;

    // Networking
    private VitralEditorServer networkServer;
    private VitralCommandClient networkCommandClient;
    private String currentNetworkCommandClientIp;
    private String currentNetworkCommandClientPort;

    public void setLookAndFeel(String lookAndFeel)
    {
        this.lookAndFeel = lookAndFeel;

        destroyGUI();
        createGUI();
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
        applicationModel = new ApplicationModel();
        applicationModel.setScene(new Scene());

        applicationModel.setRaytracedImage(new RGBImageUncompressed());
        applicationModel.setRaytracedImageWidth(320);
        applicationModel.setRaytracedImageHeight(240);

        applicationModel.setPalette(null);
        try {
            applicationModel.setPalette(
                RGBColorPalettePersistence.importGimpPalette(
                    new java.io.FileReader("../../../../etc/palettes/Cranes.gpl")));
        }
        catch ( Exception e ) {
            System.err.println(e);
            System.exit(0);
        }

        applicationModel.setVisualDebugRay(new Ray(new Vector3Dd(0, -3, 0), new Vector3Dd(0, 1, 0)));
        applicationModel.setVisualDebugRayLevels(2);
        applicationModel.setWithVisualDebugRay(false);
        jogl4Controller = new Jogl4ApplicationController(applicationModel);

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

        mainWindowWidget.add(
            jogl4Controller.getCanvas(statusMessage, this),
            BorderLayout.CENTER);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        jogl4Controller.requestFocusInWindow();

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
            Logger.reportMessage(this, VSDK.WARNING, "createGUIWindowed", 
                "Warning: Can not set " + lookAndFeel + " look and feel\n" + e);
        }

        //- Configure this JFrame -----------------------------------------
        mainWindowWidget = new JFrame("VITRAL Scene Editor");
        mainWindowWidget.setUndecorated(false);
        mainWindowWidget.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

        //-----------------------------------------------------------------
        executor = new GUIEventExecutor(this);
        guiEventExecutor = executor;
        executorPanel = new ButtonsPanel(this, 101, executor);

        //-----------------------------------------------------------------
        JMenuBar menuBar = SwingGuiRenderer.buildMenubar(gui, null, executorPanel);

        //-----------------------------------------------------------------
        JSplitPane splitPane;
        statusBarPanel = createStatusBar();

        Component left = jogl4Controller.getCanvas(statusMessage, this);
        Component right = createPanel();
        Dimension minLeft = new Dimension(160, 120);
        Dimension minRight = new Dimension(320, 120);
        JPanel iconsAndWorkAreasPanel;

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                   left,
                                   right);
        left.setMinimumSize(minLeft);
        right.setMinimumSize(minRight);
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
        mainWindowWidget.setJMenuBar(menuBar);

        //-----------------------------------------------------------------
        int panelWidth = d.width - 320;

        splitPane.setDividerLocation(panelWidth);

        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        jogl4Controller.requestFocusInWindow();

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
        lookAndFeel = "org.jvnet.substance.skin.SubstanceTwilightLookAndFeel";
        languageGuiFile = "./etc/english.json";

        fullScreenGuiMode = false;

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
        applicationModel.getRaytracedImage().init(
            applicationModel.getRaytracedImageWidth(),
            applicationModel.getRaytracedImageHeight());
        if ( applicationModel.getScene().selectedBackground == 1 ) {
            ImageProcessing.resize(
                applicationModel.getScene().fixedBackground.getImage(),
                applicationModel.getRaytracedImage());
        }
        applicationModel.getScene().raytrace(applicationModel.getRaytracedImage());
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

    public ApplicationModel getApplicationModel()
    {
        return applicationModel;
    }

    public Jogl4ApplicationController getJogl4Controller()
    {
        return jogl4Controller;
    }

    public static void main(String[] args) {
        MainThread mt = new MainThread(args);
        SwingUtilities.invokeLater(mt);
    }
}
