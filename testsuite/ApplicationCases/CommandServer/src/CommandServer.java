// Java GUI classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.PlainDocument;

class MainThread implements Runnable
{
    private String args[];
    public MainThread(String args[])
    {
        this.args = args;
    }
    public void run()
    {
        CommandServer app;
        app = new CommandServer(args);
    }
}

public class CommandServer
{
    private JFrame mainWindowWidget;
    private String lookAndFeel;
    private ButtonsPanel executorPanel;
    public VoiceCommandSet commandSet;
    public long lastTimeTyped;
    public JTextField jtf;
    private VitralCommandServer networkServer;
    public String currentCommand;

    public CommandServer(String args[])
    {
        jtf = null;
        lookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
        createModel();
        createGUI();
        networkServer = new VitralCommandServer(this);
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        commandSet = new VoiceCommandSet();
        // Order matters! entries listed at the beginning has priority over
        // entries listed at the end


        commandSet.addSpeech("IDC_FILE_QUIT", "salir de la aplicación");
        commandSet.addSpeech("IDC_CREATE_SPHERE", "crear esfera");
        commandSet.addSpeech("IDC_CREATE_SPHERE", "crear ese era");
        commandSet.addSpeech("IDC_CREATE_SPHERE", "crear este era");
        commandSet.addSpeech("IDC_CREATE_CONE", "crear cono");
        commandSet.addSpeech("IDC_CREATE_CONE", "crear con");
        commandSet.addSpeech("IDC_CREATE_CYLINDER", "crear cilindro");
        commandSet.addSpeech("IDC_CREATE_CUBE", "crear cubo");
        commandSet.addSpeech("IDC_CREATE_BOX", "crear caja");
        commandSet.addSpeech("IDC_CREATE_ARROW", "crear flecha");
        commandSet.addSpeech("IDC_CREATE_BREP", "crear sólido");
        commandSet.addSpeech("IDC_CREATE_BREP", "crear sólidos");
        commandSet.addSpeech("IDC_CREATE_PARAMETRICCUBICCURVE", "crear curva");
        commandSet.addSpeech("IDC_CREATE_PARAMETRICBICUBICPATCH", "crear parche");
        commandSet.addSpeech("IDC_IMPORT_OBJECTS_FROM_FILE", "leer archivo");
        commandSet.addSpeech("IDC_CREATE_OMNILIGHT", "crear luz");
        commandSet.addSpeech("IDC_CREATE_OMNILIGHT", "crearlos");
        commandSet.addSpeech("IDC_CREATE_OMNILIGHT", "crear los");
        commandSet.addSpeech("IDC_CREATE_OMNILIGHT", "crearluz");

        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_MOTIF", "apariencia unix");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_MOTIF", "apariencia clásica");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_MOTIF", "apariencia antigua");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_JAVA", "apariencia java");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_JAVA", "apariencia habrá");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_JAVA", "apariencia hava");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_JAVA", "apariencia haba");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_WINDOWS", "apariencia windows");
        commandSet.addSpeech("IDC_CUSTOMIZE_LANGUAGE_ENGLISH", "interfaz en inglés");
        commandSet.addSpeech("IDC_CUSTOMIZE_LANGUAGE_ENGLISH", "interfase en inglés");
        commandSet.addSpeech("IDC_CUSTOMIZE_LANGUAGE_ENGLISH", "idioma inglés");
        commandSet.addSpeech("IDC_CUSTOMIZE_LANGUAGE_SPANISH", "interfaz en español");
        commandSet.addSpeech("IDC_CUSTOMIZE_LANGUAGE_SPANISH", "interfase en español");
        commandSet.addSpeech("IDC_CUSTOMIZE_LANGUAGE_SPANISH", "idioma español");
        commandSet.addSpeech("IDC_OTHERS_CYCLE_BACKGROUND", "cambiar fondo");
        commandSet.addSpeech("IDC_OTHERS_CYCLE_BACKGROUND", "rotar fondo");
        commandSet.addSpeech("IDC_OTHERS_TOGGLE_GRID", "grilla");
        commandSet.addSpeech("IDC_TOOLS_CAMERA", "modo cámara");
        commandSet.addSpeech("IDC_TOOLS_CAMERA", "mondo cámara");
        commandSet.addSpeech("IDC_TOOLS_CAMERA", "cámara");
        commandSet.addSpeech("IDC_TOOLS_SELECT", "seleccionar");
        commandSet.addSpeech("IDC_TOOLS_SELECT", "modo selección");
        commandSet.addSpeech("IDC_TOOLS_SELECT", "modo de selección");
        commandSet.addSpeech("IDC_TOOLS_TRANSLATE", "modo translación");
        commandSet.addSpeech("IDC_TOOLS_TRANSLATE", "transladar");
        commandSet.addSpeech("IDC_TOOLS_TRANSLATE", "modo de translación");
        commandSet.addSpeech("IDC_TOOLS_ROTATE", "rotar");
        commandSet.addSpeech("IDC_TOOLS_ROTATE", "brotar");
        commandSet.addSpeech("IDC_TOOLS_ROTATE", "modo rotación");
        commandSet.addSpeech("IDC_TOOLS_ROTATE", "modo de rotación");
        commandSet.addSpeech("IDC_TOOLS_SCALE", "escalar");
        commandSet.addSpeech("IDC_TOOLS_SCALE", "modo de escalamiento");
        commandSet.addSpeech("IDC_TOOLS_RAY", "rayo");
/*
        commandSet.addSpeech("IDC_VOICECOMMAND_CLIENT", "");
        commandSet.addSpeech("IDC_NEW_VIEW", "");
        commandSet.addSpeech("IDC_DEL_VIEW", "");
        commandSet.addSpeech("IDC_CREATE_SPHERE_HARMONIC", "");
        commandSet.addSpeech("IDC_CREATE_PROJECTED_VIEWS", "");
        commandSet.addSpeech("IDC_CREATE_VOLUME", "");
        commandSet.addSpeech("Select palette for depthmap display", "");
        commandSet.addSpeech("IDC_RENDERING_OBTAINZBUFFERIMAGE", "");
        commandSet.addSpeech("IDC_RENDERING_OBTAINZBUFFERDEPTHMAP", "");
        commandSet.addSpeech("IDC_RENDERING_OBTAINCONTOURNS", "");
        commandSet.addSpeech("IDC_RENDERING_RAYTRACING", "");
        commandSet.addSpeech("IDC_CUSTOMIZE_LAF_GTK", "");
        commandSet.addSpeech("IDC_OTHERS_TOGGLE_TEST_CORRIDOR", "");
        commandSet.addSpeech("IDC_OTHERS_PRINT_SCENE_ON_CONSOLE", "");
*/
        currentCommand = null;

        //-----------------------------------------------------------------
        lastTimeTyped = System.currentTimeMillis();
        TextEraserRunnable son = new TextEraserRunnable(this);
        Thread tt = new Thread(son);
        tt.start();
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
        mainWindowWidget = new JFrame("VITRAL Command Server");
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension d;
        Toolkit tk = mainWindowWidget.getToolkit();
        d = tk.getScreenSize();
        d.width = 320;
        d.height -= 40;
/*
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
        JMenuBar menubar;

        menubar = SwingGuiCacheRenderer.buildMenubar(gui, null, executorPanel);

        //-----------------------------------------------------------------
        //mainWindowWidget.setJMenuBar(menubar);
        */

        //-----------------------------------------------------------------
        executorPanel = new ButtonsPanel(this);
        JPanel lowPanel = new JPanel();
        lowPanel.setLayout(new BoxLayout(lowPanel, BoxLayout.X_AXIS));

        jtf = new JTextField();
        lowPanel.add(jtf);
        jtf.addActionListener(executorPanel);
        jtf.getDocument().addDocumentListener(executorPanel);

        JButton b;
        b = new JButton("Clear");
        b.setName("Clear");
        lowPanel.add(b);
        b.addActionListener(executorPanel);

        b = new JButton("Send");
        b.setName("Send");
        lowPanel.add(b);
        b.addActionListener(executorPanel);

        //-----------------------------------------------------------------
        mainWindowWidget.add(executorPanel, BorderLayout.CENTER);
        mainWindowWidget.add(lowPanel, BorderLayout.SOUTH);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
        jtf.requestFocusInWindow();
        //drawingArea.getCanvas().requestFocusInWindow();

        //-----------------------------------------------------------------
    }

    public static void main(String args[])
    {
        // Note that this is a thread-safe invocation of the GUI
        MainThread mt = new MainThread(args);
        javax.swing.SwingUtilities.invokeLater(mt);
    }

    public void notifySpeech()
    {
        String label;
        VoiceCommand cmd;

        try {
            label = jtf.getDocument().getText(0, jtf.getDocument().getLength());
            cmd = commandSet.searchVoiceCommandFromSpeech(label);
            if ( cmd != null ) {
                System.out.println("Command [" + cmd.getName() + "] selected.");
                currentCommand = cmd.getName();
            }
            else {
                System.out.println("No command found for current speech");
                currentCommand = null;
            }
        }
        catch ( Exception e ) {
            System.out.println("Error in notifySpeech()!" + e);
        }
        jtf.setDocument(new PlainDocument());
        jtf.getDocument().addDocumentListener(executorPanel);
    }

    public synchronized void updateTime()
    {
        long newTyped;
        newTyped = System.currentTimeMillis();
        //System.out.println("Delta: " + (newTyped-lastTimeTyped));
        lastTimeTyped = newTyped;
    }
}
