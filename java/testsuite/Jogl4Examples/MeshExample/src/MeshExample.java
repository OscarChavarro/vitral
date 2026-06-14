// Basic java classes
import java.io.File;

// AWT GUI java classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

// Swing GUI java classes
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

// VSDK classes
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;

// Application classes
import awt.FileSelectorDialog;
import gui.MeshKeyboardInteractionTechniques;
import gui.MeshMouseInteractionTechniques;
import model.MeshModel;
import options.CommandLineOptions;
import render.Jogl4DebuggerRenderer;

public class MeshExample
    extends JFrame implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private MeshModel model;
    private CommandLineOptions commandLineOptions;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;
    private MeshMouseInteractionTechniques mouseInteractionTechniques;
    private MeshKeyboardInteractionTechniques keyboardInteractionTechniques;
    public GLCanvas canvas;
    private Jogl4DebuggerRenderer renderer;

    public MeshExample(String fileName) {
        super("VITRAL mesh test - JOGL");
        File file = null;

        model = new MeshModel();
        commandLineOptions = new CommandLineOptions(model);

        //-----------------------------------------------------------------
        if ( fileName == null ) {
            FileSelectorDialog selectorDialog = new FileSelectorDialog();
            file = selectorDialog.selectGeometryFile();
        }
        else {
            file = new File(fileName);
        }

        //-----------------------------------------------------------------
        if ( file != null ) {
            try {
                EnvironmentPersistence.importEnvironment(file, model.getScene());
                model.configureInitialViewAndLightToScene();
            }
            catch ( Exception ex ) {
                System.err.println("Failed to read file.");
                ex.printStackTrace();
                System.exit(0);
            }
        }
        else {
            System.err.println("File not specified");
            System.exit(0);
        }

        //-----------------------------------------------------------------
        GLCapabilities glCaps = new GLCapabilities(GLProfile.get(GLProfile.GL4));
        glCaps.setDepthBits(64);
        canvas = new GLCanvas(glCaps);

        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cameraController = new CameraControllerOrbiter(model.getCamera());
        qualityController = new RendererConfigurationController(model.getQualitySelection());
        mouseInteractionTechniques = new MeshMouseInteractionTechniques(cameraController);
        keyboardInteractionTechniques =
            new MeshKeyboardInteractionTechniques(model, cameraController, qualityController);

        renderer = new Jogl4DebuggerRenderer(model);
        canvas.addGLEventListener(renderer);
    }

    public void processCommandLineArguments(String[] args) {
        commandLineOptions.processArguments(args);
    }

    public MeshModel getModel() {
        return model;
    }

    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f;
        MeshExample app;

        app = new MeshExample(extractFileName(args));
        app.processCommandLineArguments(args);
        System.out.println("Searching tangible interface server on " + app.getModel().getTangibleServiceUrl());
        f = app;

        f.pack();
        f.setVisible(true);
    }

    private static String extractFileName(String[] args) {
        if ( args == null ) {
            return null;
        }

        for ( int i = 0; i < args.length; i++ ) {
            if ( "-tangibleServer".equals(args[i]) ) {
                i++;
                continue;
            }
            if ( !args[i].startsWith("-") ) {
                return args[i];
            }
        }

        return null;
    }

    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (mouseInteractionTechniques.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (mouseInteractionTechniques.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (mouseInteractionTechniques.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (mouseInteractionTechniques.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (mouseInteractionTechniques.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (mouseInteractionTechniques.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( keyboardInteractionTechniques.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if (keyboardInteractionTechniques.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void keyTyped(KeyEvent e) {
        ;
    }

}
