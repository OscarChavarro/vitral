// Java Awt/Swing classes
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

// JOGL classes
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.fixtures.Jogl4SimpleCorridorSample;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

@SuppressWarnings("removal")
public class CameraExample extends Applet implements
    GLEventListener,
    KeyListener,
    MouseListener,
    MouseMotionListener,
    MouseWheelListener
{
    private boolean appletMode;
    private boolean closing;
    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private JFrame frame;
    private Jogl4SimpleCorridorSample corridor;

    public CameraExample() {
    }

    private void createModel()
    {
        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);
        corridor = new Jogl4SimpleCorridorSample();
    }

    private void fillGUIWithCanvas()
    {
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
        canvas.setFocusable(true);
    }

    public static void main(String[] args)
    {
        if ( !Jogl4Renderer.verifyOpenGLAvailability() ) {
            System.out.println("Can not start OpenGL/JOGL.");
            return;
        }

        CameraExample instance = new CameraExample();
        instance.appletMode = false;
        instance.createModel();
        instance.fillGUIWithCanvas();

        instance.frame = new JFrame("VITRAL concept test - JOGL4 Camera control example");
        instance.frame.add(instance.canvas, BorderLayout.CENTER);
        instance.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        Dimension size = new Dimension(640, 480);
        instance.frame.setMinimumSize(size);
        instance.frame.setSize(size);
        instance.frame.addKeyListener(instance);
        instance.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                instance.requestClose();
            }
        });
        instance.frame.setVisible(true);
        instance.canvas.requestFocusInWindow();
    }

    @Override
    public void init()
    {
        appletMode = true;
        createModel();
        setLayout(new BorderLayout());
        fillGUIWithCanvas();
        add("Center", canvas);
    }

    private void drawObjectsGL(GL4 gl)
    {
        gl.glEnable(GL4.GL_DEPTH_TEST);

        Matrix4x4 projection = Jogl4CameraRenderer.activate(gl, camera);
        corridor.drawGL(gl, projection);
        Jogl4MatrixRenderer.draw(gl, projection, Matrix4x4.identityMatrix());
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        drawObjectsGL(gl);
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        if ( corridor != null ) {
            corridor.dispose(gl);
        }
        Jogl4CameraRenderer.dispose(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int xSize, int ySize)
    {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, xSize, ySize);
        camera.updateViewportResize(xSize, ySize);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if ( cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if ( cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if ( cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            if ( !appletMode ) {
                requestClose();
            }
            return;
        }

        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if ( cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    private void requestClose()
    {
        if ( closing ) {
            return;
        }
        closing = true;

        if ( canvas != null ) {
            canvas.destroy();
        }

        if ( frame != null ) {
            frame.dispose();
        }

        System.exit(0);
    }
}
