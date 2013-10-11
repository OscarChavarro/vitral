//===========================================================================

// Basic java classes
import java.io.File;
import java.util.ArrayList;

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
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;    // Model elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.geometry.EnvironmentPersistence; // Persistence elements
import vsdk.toolkit.render.jogl.JoglCameraRenderer; // View elements
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.gui.CameraController;           // Control elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;

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
    private RendererConfiguration qualitySelection;
    private RendererConfigurationController qualityController;
    public GLCanvas canvas;

    private SimpleScene scene;

    public double x;

    public MeshExample(String fileName) {
        super("VITRAL mesh test - JOGL");
        File file = null;

        x = 0;

        scene = new SimpleScene();

        //-----------------------------------------------------------------
        if ( fileName == null ) {
            JFileChooser jfc = null;

            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../../etc/geometry");

            jfc.removeChoosableFileFilter(jfc.getFileFilter());
            jfc.addChoosableFileFilter(new ObjectFilter("3ds", "3ds Files"));
            jfc.addChoosableFileFilter(new ObjectFilter("ase", "3ds Files (Ascii Scene Export)"));
            jfc.addChoosableFileFilter(new ObjectFilter("vtk", "kitware's VTK legacy binary file"));
            jfc.addChoosableFileFilter(new ObjectFilter("obj", "Obj Files"));
            int opc = jfc.showOpenDialog(new JPanel());

            if ( opc == JFileChooser.APPROVE_OPTION ) {
                file = jfc.getSelectedFile();
            }
        }
        else {
            file = new File(fileName);
        }

        //-----------------------------------------------------------------
        if ( file != null ) {
            try {
                EnvironmentPersistence.importEnvironment(file, scene);

// Trivial mesh creation (need to change TriangleMeshGroup by TriangleMesh):
/*
        Vertex v[] = new Vertex[3];
        v[0] = new Vertex(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1));
        v[1] = new Vertex(new Vector3D(1, 0, 0), new Vector3D(0, 0, 1));
        v[2] = new Vertex(new Vector3D(1, 1, 0), new Vector3D(0, 0, 1));

        Triangle t[] = new Triangle[1];
        t[0] = new Triangle(0, 1, 2);

        mesh = new TriangleMesh();
        mesh.setVertexes(v);
        mesh.setTriangles(t);
        mesh.calculateNormals();
*/
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

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        qualitySelection = new RendererConfiguration();
        qualityController = new RendererConfigurationController(qualitySelection);

        light = new Light(Light.POINT, new Vector3D(10, -20, 50), new ColorRgb(1, 1, 1));
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

        Animador h = new Animador((MeshExample)f);
        Thread x = new Thread(h);
        x.start();


        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL2 gl) {
        gl.glLoadIdentity();

        gl.glTranslated(x, 0, 0);

        // Draw reference frame
        gl.glLineWidth((float)3.0);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glBegin(gl.GL_LINES);
            gl.glColor3d(1, 0, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(1, 0, 0);

            gl.glColor3d(0, 1, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 1, 0);

            gl.glColor3d(0, 0, 1);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 1);
        gl.glEnd();

        // Draw mesh
        gl.glDisable(gl.GL_CULL_FACE);
        //gl.glCullFace(gl.GL_BACK);

        JoglSimpleBodyRenderer.setAutomaticDisplayListManagement(true);

        int i;
        for ( i = 0; i < scene.getSimpleBodies().size(); i++ ) {
            JoglSimpleBodyRenderer.drawWithVertexArrays(gl, scene.getSimpleBodies().get(i), camera, qualitySelection);
        }
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
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
        GL2 gl = drawable.getGL().getGL2();
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
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection);
        }
        if ( cameraController.processKeyPressedEventAwt(e) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEventAwt(e) ) {
            System.out.println(qualitySelection);
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

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
