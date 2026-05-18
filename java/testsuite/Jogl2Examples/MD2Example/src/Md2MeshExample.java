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
import java.io.IOException;

// Swing GUI java classes
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;

// Application
import model.DebuggerModel;
import gui.KeyboardInteractionTechniques;
import gui.MouseInteractionTechniques;
import render.Jogl2DebuggerRenderer;
import animation.DebuggerAnimationController;
import io.DebuggerReader;

public class Md2MeshExample
    extends JFrame implements GLEventListener, MouseListener,
                   MouseMotionListener, MouseWheelListener, KeyListener {
    private static final String ASSETS_PATH = "../../../../";

    private CameraController cameraController;
    private RendererConfigurationController qualityController;
    private KeyboardInteractionTechniques keyboardInteractionTechniques;
    private MouseInteractionTechniques mouseInteractionTechniques;
    private Jogl2DebuggerRenderer renderer;
    public GLJPanel canvas;

    private final DebuggerModel model;
    private DebuggerReader reader;
    private DebuggerAnimationController animationController;

    public Md2MeshExample(String fileName) {
        super("VITRAL Quake MD2 mesh test - JOGL");
        model = new DebuggerModel();
        init();
    }

    private void init() throws GLException {
        reader = new DebuggerReader();

        try {
            reader.readMd2WithTexture(
                ASSETS_PATH + "etc/md2/samourai.md2",
                ASSETS_PATH + "etc/md2/samourai.jpg",
                model.md2Mesh
            );
        } catch ( IOException ex ) {
            Logger.reportMessageWithException(this, VSDK.FATAL_ERROR, "Md2MeshExample",
                "Input/Output error", ex);
            System.exit(0);
        }

        cameraController = new CameraControllerOrbiter(model.camera);
        cameraController.setDeltaMovement(10);
        qualityController = new RendererConfigurationController(model.qualitySelection);
        keyboardInteractionTechniques = new KeyboardInteractionTechniques(model);
        mouseInteractionTechniques = new MouseInteractionTechniques(model);
        renderer = new Jogl2DebuggerRenderer(model);
        animationController = new DebuggerAnimationController();

        initGui();
    }

    private void initGui() {
        GLCapabilities glCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        glCaps.setDepthBits(64);

        canvas = new GLJPanel(glCaps);
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f;

        if ( args.length == 1 ) {
            f = new Md2MeshExample(args[0]);
        }
        else {
            f = new Md2MeshExample(null);
        }

        f.pack();
        f.setVisible(true);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        renderer.display(drawable);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        renderer.init(drawable);
        animationController.start(model.md2Mesh, canvas);
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        renderer.reshape(drawable, width, height);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        mouseInteractionTechniques.processMouseEnteredEvent(mouseEvent);
        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        mouseInteractionTechniques.processMouseExitedEvent(mouseEvent);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        if (mouseInteractionTechniques.processMousePressedEvent(mouseEvent)) {
            canvas.repaint();
        }
        if (cameraController.processMousePressedEvent(mouseEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        if (mouseInteractionTechniques.processMouseReleasedEvent(mouseEvent)) {
            canvas.repaint();
        }
        if (cameraController.processMouseReleasedEvent(mouseEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        if (mouseInteractionTechniques.processMouseClickedEvent(mouseEvent)) {
            canvas.repaint();
        }
        if (cameraController.processMouseClickedEvent(mouseEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        if (mouseInteractionTechniques.processMouseMovedEvent(mouseEvent)) {
            canvas.repaint();
        }
        if (cameraController.processMouseMovedEvent(mouseEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        if (mouseInteractionTechniques.processMouseDraggedEvent(mouseEvent)) {
            canvas.repaint();
        }
        if (cameraController.processMouseDraggedEvent(mouseEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        vsdk.toolkit.gui.MouseEvent mouseEvent = AwtSystem.awt2vsdkEvent(e);
        if (mouseInteractionTechniques.processMouseWheelEvent(mouseEvent)) {
            canvas.repaint();
        }
        if (cameraController.processMouseWheelEvent(mouseEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        vsdk.toolkit.gui.KeyEvent keyEvent = AwtSystem.awt2vsdkEvent(e);

        if ( keyboardInteractionTechniques.processKeyPressedEvent(keyEvent) ) {
            canvas.repaint();
        }
        if ( cameraController.processKeyPressedEvent(keyEvent) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(keyEvent) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        vsdk.toolkit.gui.KeyEvent keyEvent = AwtSystem.awt2vsdkEvent(e);

        if (keyboardInteractionTechniques.processKeyReleasedEvent(keyEvent)) {
            canvas.repaint();
        }
        if (cameraController.processKeyReleasedEvent(keyEvent)) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEvent(keyEvent)) {
            canvas.repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        renderer.dispose(drawable);
    }
}
