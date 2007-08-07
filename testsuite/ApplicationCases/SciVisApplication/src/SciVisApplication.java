//===========================================================================
//= Scientific Editor Application based on VSDK.                            =
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 20 2006 - Oscar Chavarro: Original base version                =
//===========================================================================

// Java basic classes
import java.io.File;
import java.io.FileReader;

// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory; 
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// VSDK classes
import vsdk.toolkit.media.Image;

// Internal classes
import vsdk.transition.gui.GuiCache;
import vsdk.transition.io.presentation.GuiCachePersistence;
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

// Application classes
import scivis.Study;
import scivis.TimeTake;

public class SciVisApplication
{
    // Application MODEL
    public Study study;

    // Application MODEL state (GUI interaction model)
    public int currentTimeTake;
    public int currentSlice;

    // Application GUI
    public GuiCache gui;
    private String lookAndFeel;
    public String languageGuiFile;
    private JFrame mainWindowWidget;
    public ButtonsPanel executorPanel;
    public JLabel statusMessage;
    private JPanel spaceControlSubDialog;
    private JPanel iconsAndWorkAreasPanel;
    public MyImagePanel drawingArea;
    public JSpinner slicesSelectSpinner;

    public SciVisApplication()
    {
        drawingArea = new MyImagePanel(this);
        study = null;

        gui = null;
        mainWindowWidget = null;
        lookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
        languageGuiFile = "./etc/english.gui";

        createGUI();
    }

    public void createModel()
    {
	System.out.println("Create model start");

	currentSlice = 0;
	currentTimeTake = 0;

        //- DEFAULT TEST HARD-CODED STUDY ---------------------------------
        String imageFilenameBase = "./etc/studies/frog/slices/s";
        int numSlices = 137;

        //-----------------------------------------------------------------
        study = new Study();
        study.init(1);

        int i;
        String imageFilename = null, ii;

        TimeTake timeTake = study.getTimeTake(0);
        for ( i = 0; i < numSlices; i++ ) {
            ii = String.format("%03d", i);
	    imageFilename = imageFilenameBase+ii+".png";
            timeTake.setSliceImageAt(imageFilename, i, 0, 0);
        }

        //-----------------------------------------------------------------
        Image img = null;

        img = study.getSliceImageAt(currentTimeTake, currentSlice);

        drawingArea.setImage(img);
        drawingArea.repaint();
	System.out.println("Create model end");
    }

    private JPanel
    createControlSpaceSubDialog(ButtonsPanel actionListener)
    {
        JPanel mainPanel, spacePanel, slicePanel;

        //-----------------------------------------------------------------
        TitledBorder border1;
        border1 = BorderFactory.createTitledBorder("Location movement: ");
        border1.setTitleJustification(TitledBorder.LEFT);
        spacePanel = new JPanel();
        spacePanel.setBorder(border1);

        //-----------------------------------------------------------------
        JSpinner spinner1;
        SpinnerNumberModel nm1;
        JLabel label1;

        nm1 = new SpinnerNumberModel(
	    0, /* Initial value */
	    0, /* Minimal value */
	    study.getNumTimeTakes()-1, /* Maximum value */
	    1  /* Step */
        );

        label1 = new JLabel("Current time take: ");
        spinner1 = new JSpinner(nm1);
        spinner1.setName("TIME_SPINNER");
        spinner1.addChangeListener(actionListener);
        spacePanel.add(label1);
        spacePanel.add(spinner1);

        //-----------------------------------------------------------------
        JSpinner spinner2;
        SpinnerNumberModel nm2;
        JLabel label2;

        TimeTake timeTake = study.getTimeTake(0);
        nm2 = new SpinnerNumberModel(
	    0, /* Initial value */
	    0, /* Minimal value */
	    timeTake.getNumSlices()-1, /* Maximum value */
	    1  /* Step */
        );

        label2 = new JLabel("Current slice: ");
        spinner2 = new JSpinner(nm2);
        spinner2.setName("SLICE_SPINNER");
        spinner2.addChangeListener(actionListener);
        spacePanel.add(label2);
        spacePanel.add(spinner2);
        slicesSelectSpinner = spinner2;

        //-----------------------------------------------------------------
        mainPanel = new JPanel();
        mainPanel.add(spacePanel);

        return mainPanel;
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
        mainWindowWidget = new JFrame("VITRAL Scientific Visualization Editor");
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

        //-----------------------------------------------------------------
        executorPanel = new ButtonsPanel(this, 101);

        //-----------------------------------------------------------------
        JMenuBar menubar;

        menubar = SwingGuiCacheRenderer.buildMenubar(gui, null, executorPanel);

        //-----------------------------------------------------------------
        JPanel statusBar = createStatusBar();
        JSplitPane splitPane;

        //drawingArea = new JoglDrawingArea(theScene, statusMessage, this);

	PanelManager panelManager = new PanelManager(this);

        //Component left = drawingArea.getCanvas();
        //Component left = drawingArea;
	Component left = panelManager;
        //Component right = createPanel();
        Component right = new JLabel("Panel");
        Dimension minleft = new Dimension(160, 120);
        Dimension minright = new Dimension(320, 120);

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

        spaceControlSubDialog = null;

        //-----------------------------------------------------------------
        int ancho_panel;

        if ( d.width < 640 ) ancho_panel = 320;
        ancho_panel = d.width - 320;

        //splitPane.setDividerLocation(ancho_panel);

        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        //drawingArea.getCanvas().requestFocusInWindow();

        //-----------------------------------------------------------------
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

    public void
    addSpaceControlSubDialog()
    {
        if ( spaceControlSubDialog != null ) {
            return;
        }
        spaceControlSubDialog = createControlSpaceSubDialog(executorPanel);
        iconsAndWorkAreasPanel.add(spaceControlSubDialog);
        mainWindowWidget.pack();
    }

    public void
    removeSpaceControlSubDialog()
    {
        iconsAndWorkAreasPanel.remove(spaceControlSubDialog);
        mainWindowWidget.pack();
        spaceControlSubDialog = null;
    }

    public static void main(String args[])
    {
        // Note that this is a thread-safe invocation of the GUI
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SciVisApplication();
            }
        });
    }
}

class ButtonsPanel extends JPanel implements ActionListener, ChangeListener
{
    SciVisApplication parent;

    public ButtonsPanel(SciVisApplication parent, int group)
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

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        // This makes event compatible with ButtonGroup scheme of event
        // handling
        if ( ev.getSource().getClass().getName().equals(
             "javax.swing.JButton") ) {
            JButton origin = (JButton)ev.getSource();
            label = origin.getName();
        }

        //- FILE ----------------------------------------------------------
        if ( label.equals("IDC_FILE_QUIT") ) {
            System.exit(0);
        }

        //- TESTS ---------------------------------------------------------
        else if ( label.equals("IDC_TESTS_CYCLE_SLICES" ) ) {
            parent.drawingArea.setCyclePending(true);
        }

        //- SPACE CONTROL SUBDIALOG ---------------------------------------
        else if ( label.equals("IDC_FILE_CLOSE_STUDY") ) {
            parent.removeSpaceControlSubDialog();
            parent.study = null;
            parent.drawingArea.setImage(null);
            System.gc();
        }
        else if ( label.equals("IDC_FILE_NEW_STUDY") ) {
            parent.createModel();
            parent.addSpaceControlSubDialog();
	    parent.drawingArea.repaint();
        }

        //-----------------------------------------------------------------
        //parent.drawingArea.canvas.repaint();
    }

    public void stateChanged(ChangeEvent ev)
    {
        //-----------------------------------------------------------------
        JSpinner spinner = ((JSpinner)(ev.getSource()));
        String label = spinner.getName();
        int ival;
        SpinnerNumberModel nm;

        if ( label.equals("TIME_SPINNER") ) {
            ival = ((Integer)(spinner.getValue())).intValue();
	    parent.currentTimeTake = ival;
            TimeTake timeTake = parent.study.getTimeTake(0);
            nm = new SpinnerNumberModel(
                1, /* Initial value */
                0, /* Minimal value */
                timeTake.getNumSlices()-1, /* Maximum value */
                1  /* Step */
            );
            parent.slicesSelectSpinner.setModel(nm);
            parent.slicesSelectSpinner.setValue(new Integer(0));
        }
        if ( label.equals("SLICE_SPINNER") ) {
            ival = ((Integer)(spinner.getValue())).intValue();
	    parent.currentSlice = ival;
        }

        //-----------------------------------------------------------------
        Image img = null;

        img = parent.study.getSliceImageAt(parent.currentTimeTake, parent.currentSlice);

	System.out.println("Show take " + parent.currentTimeTake + ", slice " + parent.currentSlice);

        parent.drawingArea.setImage(img);
        parent.drawingArea.repaint();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
