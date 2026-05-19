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
import com.jogamp.opengl.awt.GLCanvas;

// VSDK classes
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;

// Application classes
import awt.FileSelectorDialog;
import model.MeshModel;
import render.Jogl4DebuggerRenderer;

public class MeshExample
    extends JFrame implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private MeshModel model;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;
    public GLCanvas canvas;
    private Jogl4DebuggerRenderer renderer;

    public MeshExample(String fileName) {
        super("VITRAL mesh test - JOGL");
        File file = null;

        model = new MeshModel();

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
        canvas = new GLCanvas();

        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cameraController = new CameraControllerOrbiter(model.getCamera());
        qualityController = new RendererConfigurationController(model.getQualitySelection());

        renderer = new Jogl4DebuggerRenderer(model);
        canvas.addGLEventListener(renderer);
    }

    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f;


        if ( args.length == 1 ) {
            f = new MeshExample(args[0]);
        }
        else {
            f = new MeshExample(null);
        }

        f.pack();
        f.setVisible(true);
    }

    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if (cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(model.getQualitySelection());
        }
        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            System.out.println(model.getQualitySelection());
            canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if (cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void keyTyped(KeyEvent e) {
        ;
    }

}
