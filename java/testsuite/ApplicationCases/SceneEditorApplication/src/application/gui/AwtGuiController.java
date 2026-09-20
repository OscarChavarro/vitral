package application.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.FileInputStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import application.Jogl4ApplicationController;
import application.SceneEditorApplication;
import application.net.VitralCommandClient;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.io.gui.GuiPersistence;
import vsdk.toolkit.render.swing.SwingGuiRenderer;

public class AwtGuiController
{
    private final SceneEditorApplication parent;
    private final AwtApplicationModel model;
    private final Jogl4ApplicationController jogl4Controller;
    private VitralCommandClient networkCommandClient;
    private String currentNetworkCommandClientIp;
    private String currentNetworkCommandClientPort;

    public AwtGuiController(SceneEditorApplication parent,
                            AwtApplicationModel model,
                            Jogl4ApplicationController jogl4Controller)
    {
        this.parent = parent;
        this.model = model;
        this.jogl4Controller = jogl4Controller;
        currentNetworkCommandClientIp = "127.0.0.1";
        currentNetworkCommandClientPort = "1235";
    }

    public void setLookAndFeel(String lookAndFeel)
    {
        model.setLookAndFeel(lookAndFeel);
        destroyGUI();
        createGUI();
    }

    public void setGuiLanguage(String lang)
    {
        model.setLanguageGuiFile(lang);
        destroyGUI();
        createGUI();
    }

    private JPanel createStatusBar()
    {
        JLabel statusMessage = new JLabel(model.getGui().getMessage("IDM_INTRO_MESSAGE"));
        Border border = BorderFactory.createLoweredBevelBorder();
        statusMessage.setBorder(border);
        model.setStatusMessage(statusMessage);

        JPanel newStatusBarPanel = new JPanel();
        newStatusBarPanel.setLayout(new GridLayout());

        border = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        newStatusBarPanel.setBorder(border);
        newStatusBarPanel.add(statusMessage);

        return newStatusBarPanel;
    }

    private JTabbedPane createPanel()
    {
        JTabbedPane container = new JTabbedPane();
        JPanel panel;
        JButton button;
        JScrollPane sp;

        container.getModel().addChangeListener(new MyChangeListener(parent));

        panel = new ButtonsPanel(parent, 1, model.getExecutor());
        sp = new JScrollPane(panel);
        container.addTab(model.getGui().getMessage("IDM_CREATION_TAB"),
            null, sp, "Object creation operations");

        ModifyPanel modifyPanel = new ModifyPanel(parent);
        model.setModifyPanel(modifyPanel);
        sp = new JScrollPane(modifyPanel);
        container.addTab(model.getGui().getMessage("IDM_MODIFY_TAB"),
            null, sp, "Modify selected body");

        panel = new ButtonsPanel(parent, 2, model.getExecutor());
        sp = new JScrollPane(panel);
        container.addTab(model.getGui().getMessage("IDM_GUI_TAB"),
            null, sp, "GUI Control");

        panel = new ButtonsPanel(parent, 3, model.getExecutor());
        sp = new JScrollPane(panel);
        container.addTab(model.getGui().getMessage("IDM_OTHERS_TAB"),
            null, sp, "Control the scene components");

        panel = new ButtonsPanel(parent, 4, model.getExecutor());
        sp = new JScrollPane(panel);
        container.addTab(model.getGui().getMessage("IDM_RENDER_TAB"),
            null, sp, "Control the scene components");

        return container;
    }

    private void loadGuiDefinition()
    {
        try {
            model.setGui(GuiPersistence.importAquynzaGui(
                new FileInputStream(model.getLanguageGuiFile()), "."));
        }
        catch ( Exception e ) {
            System.err.println("Fatal error: can not open GUI file");
            System.exit(0);
        }
    }

    private void createGUIFullScreen()
    {
        JFrame mainWindowWidget = new JFrame("VITRAL Scene Editor");
        model.setMainWindowWidget(mainWindowWidget);
        mainWindowWidget.setUndecorated(true);

        Toolkit tk = mainWindowWidget.getToolkit();
        Dimension d = tk.getScreenSize();

        loadGuiDefinition();

        mainWindowWidget.add(
            jogl4Controller.getCanvas(model.getStatusMessage(), parent),
            BorderLayout.CENTER);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        jogl4Controller.requestFocusInWindow();

        model.setImageControlWindow(null);
        model.setSelectorDialog(null);
        model.setModifyPanelSelected(false);
    }

    private void createGUIWindowed()
    {
        try {
            UIManager.setLookAndFeel(model.getLookAndFeel());
        }
        catch ( ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException e ) {
            Logger.reportMessage(this, VSDK.WARNING, "createGUIWindowed",
                "Warning: Can not set " + model.getLookAndFeel() + " look and feel\n" + e);
        }

        JFrame mainWindowWidget = new JFrame("VITRAL Scene Editor");
        model.setMainWindowWidget(mainWindowWidget);
        mainWindowWidget.setUndecorated(false);
        mainWindowWidget.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Toolkit tk = mainWindowWidget.getToolkit();
        Dimension d = tk.getScreenSize();

        loadGuiDefinition();

        GUIEventExecutor executor = new GUIEventExecutor(parent);
        model.setExecutor(executor);
        model.setGuiEventExecutor(executor);
        model.setExecutorPanel(new ButtonsPanel(parent, 101, executor));

        JMenuBar menuBar = SwingGuiRenderer.buildMenubar(
            model.getGui(), null, model.getExecutorPanel());

        JSplitPane splitPane;
        model.setStatusBarPanel(createStatusBar());

        Component left = jogl4Controller.getCanvas(model.getStatusMessage(), parent);
        Component right = createPanel();
        Dimension minLeft = new Dimension(160, 120);
        Dimension minRight = new Dimension(320, 120);
        JPanel iconsAndWorkAreasPanel;

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
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
        iconsAndWorkAreasPanel.add(model.getExecutorPanel());
        iconsAndWorkAreasPanel.add(splitPane);

        mainWindowWidget.add(iconsAndWorkAreasPanel, BorderLayout.CENTER);
        mainWindowWidget.add(model.getStatusBarPanel(), BorderLayout.SOUTH);
        mainWindowWidget.setJMenuBar(menuBar);

        int panelWidth = d.width - 320;
        splitPane.setDividerLocation(panelWidth);

        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        jogl4Controller.requestFocusInWindow();

        model.setImageControlWindow(null);
        model.setSelectorDialog(null);
        model.setModifyPanelSelected(false);
    }

    public final void createGUI()
    {
        if ( model.isFullScreenGuiMode() ) {
            createGUIFullScreen();
        }
        else {
            createGUIWindowed();
        }
    }

    public void destroyGUI()
    {
        JFrame mainWindowWidget = model.getMainWindowWidget();
        if ( mainWindowWidget != null ) {
            mainWindowWidget.setVisible(false);
            mainWindowWidget.dispose();
        }
        System.gc();
        model.setMainWindowWidget(null);
        System.gc();
    }

    public void switchVoiceCommandClient()
    {
        String ip, port;
        if ( networkCommandClient == null ) {
            ip = (String)JOptionPane.showInputDialog(
                model.getMainWindowWidget(),
                model.getGui().getMessage("IDM_VOICECOMMAND_IPQUESTION"),
                model.getGui().getMessage("IDM_VOICECOMMAND_TITLE"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                currentNetworkCommandClientIp);
            if ( ip == null ) {
                model.getStatusMessage().setText(
                    model.getGui().getMessage("IDM_OPERATION_CANCELLED_BY_USER") + " - " +
                    model.getGui().getMessage("IDM_VOICECOMMAND_DISABLED"));
                return;
            }
            currentNetworkCommandClientIp = ip;
            port = (String)JOptionPane.showInputDialog(
                model.getMainWindowWidget(),
                model.getGui().getMessage("IDM_VOICECOMMAND_PORTQUESTION"),
                model.getGui().getMessage("IDM_VOICECOMMAND_TITLE"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                currentNetworkCommandClientPort);
            if ( port == null ) {
                model.getStatusMessage().setText(
                    model.getGui().getMessage("IDM_OPERATION_CANCELLED_BY_USER") + " - " +
                    model.getGui().getMessage("IDM_VOICECOMMAND_DISABLED"));
                return;
            }
            currentNetworkCommandClientPort = port;
            networkCommandClient = new VitralCommandClient(parent,
                currentNetworkCommandClientIp,
                Integer.parseInt(currentNetworkCommandClientPort));
            Thread ct = new Thread(networkCommandClient);
            ct.start();
            model.getStatusMessage().setText(
                model.getGui().getMessage("IDM_VOICECOMMAND_ENABLED"));
        }
        else {
            networkCommandClient.running = false;
            model.getStatusMessage().setText(
                model.getGui().getMessage("IDM_VOICECOMMAND_DISABLED"));
            networkCommandClient = null;
        }
    }

    public void closeApplication()
    {
        if ( networkCommandClient != null ) {
            networkCommandClient.end();
        }
    }
}
