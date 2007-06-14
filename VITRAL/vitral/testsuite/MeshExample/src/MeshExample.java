// Basic java classes
import java.io.File;
import java.io.IOException;

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
import javax.swing.JFileChooser;
import javax.swing.JPanel;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.QualitySelectionController;
import vsdk.toolkit.io.geometry.ReaderObj;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglTriangleMeshGroupRenderer;

// Application classes
import util.filters.ObjectFilter;

/**
 */
public class MeshExample
    extends JFrame implements GLEventListener, MouseListener,
                   MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private Light light;
    private CameraController cameraController;
    private QualitySelection qualitySelection;
    private QualitySelectionController qualityController;
    private GLCanvas canvas;

    private TriangleMeshGroup meshGroup;

    public MeshExample() {
        super("VITRAL mesh test - JOGL");

        JFileChooser jfc = null;

        jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../etc/geometry");

        jfc.removeChoosableFileFilter(jfc.getFileFilter());
        jfc.addChoosableFileFilter(new ObjectFilter("obj", "Obj Files"));
        int opc = jfc.showOpenDialog(new JPanel());
        if (opc == JFileChooser.APPROVE_OPTION) {
            try {
                File file = jfc.getSelectedFile();
                meshGroup = ReaderObj.read(file.getAbsolutePath());
                    //System.out.println(meshGroup.toString());
            }
            catch (IOException ex) {
                System.out.println("Failed to read file");
                return;
            }

        }

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        qualitySelection = new QualitySelection();
        qualityController = new QualitySelectionController(qualitySelection);

        light = new Light(Light.DIRECTIONAL, new Vector3D(10, 20, 50), new ColorRgb(1, 1, 1));
    }

    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f = new MeshExample();
        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL gl) {
        gl.glLoadIdentity();
        JoglTriangleMeshGroupRenderer.draw(gl, meshGroup, qualitySelection);
        gl.glEnd();
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        
        JoglCameraRenderer.activate(gl, camera);
        JoglLightRenderer.activate(gl, light);

        drawObjectsGL(gl);
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height) {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height);

        camera.updateViewportResize(width, height);
    }

    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
//System.out.println("Mouse exited");
    }

    public void mousePressed(MouseEvent e) {
        if (cameraController.processMousePressedEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (cameraController.processMouseReleasedEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (cameraController.processMouseClickedEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (cameraController.processMouseMovedEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (cameraController.processMouseDraggedEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if (cameraController.processMouseWheelEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }

        if (cameraController.processKeyPressedEventAwt(e)) {
            canvas.repaint();
        }
        if (qualityController.processKeyPressedEventAwt(e)) {
            canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if (cameraController.processKeyReleasedEventAwt(e)) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEventAwt(e)) {
            canvas.repaint();
        }
    }

    /**
       Do NOT call your controller from the `keyTyped` method, or the controller
       will be invoked twice for each key. Call it only from the `keyPressed` and
       `keyReleased` method
    */
    public void keyTyped(KeyEvent e) {
        ;
    }

}
