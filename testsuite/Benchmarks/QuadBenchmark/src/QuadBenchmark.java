// This example is based on the original SPECViewperf benckmark. Currently
// supports just a very limited subset of SPECViewperf: quad data.

// Basic Java classes
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

// Awt / swing classes
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

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.Animator;

// VitralSDK classes
import vsdk.toolkit.environment.Camera;              // Model elements
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.QuadMesh;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;  // View elements
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.gui.CameraController;            // Controller elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.processing.StopWatch;
import vsdk.toolkit.io.geometry.ViewpointBinaryPersistence; // Persistence elements

/**
Note that this program is designed to work as a java application, or as a
java applet.  If current class does not extends from Applet, and `init` method
is deleted, this will continue working as a simple java application.

This is a simple program recommended for use as a template in the development
of VitralSDK programs by incremental modification.
*/
@SuppressWarnings("removal")
public class QuadBenchmark extends Applet implements 
    GLEventListener,                                                    // JOGL
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener // GUI
{

//= PROGRAM PART 1/5: ATTRIBUTES ============================================

    private boolean appletMode;
    private Camera camera;
    private SimpleScene scene;
    private CameraController cameraController;
    private GLCanvas canvas;
    private RendererConfiguration qualitySelection;
    private RendererConfigurationController qualityController;
    private Configuration options;
    private boolean withAutomaticAnimation;
    private double angle;
    private StopWatch clock;
    private int framesToGo;

//= PROGRAM PART 2/5: CONSTRUCTORS ==========================================

    /**
    When running this class inside a browser (in applet mode) there is no
    warranty of calling this method, or calling before init. It is recommended
    that real initialization be done in another `createModel` method, and
    that such method be called explicity from entry point function.
    */
    public QuadBenchmark() {
        // Empty! call `createModel` explicity from entry point function!
        ;
    }

    /**
    Real constructor
    */
    private void createModel()
    {
        qualitySelection = new RendererConfiguration();
        qualityController = new RendererConfigurationController(qualitySelection);
        camera = new Camera();

        //cameraController = new CameraControllerBlender(camera);
        cameraController = new CameraControllerAquynza(camera);
        //cameraController.setDeltaMovement(1.0);

        scene = new SimpleScene();
        try {
            ViewpointBinaryPersistence.importViewpointPolygonBinary(options.fisGeometry, options.fisColors, scene);
        }
        catch ( Exception e ) {
            System.out.println("Error loading file.");
            System.exit(0);
        }
    }

    private void createGUI()
    {
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
        withAutomaticAnimation = false;
        angle = 0.0;
        clock = null;
    }

//= PROGRAM PART 3/5: ENTRY POINTS ==========================================

    public static void main(String[] args) {

        // Common VitralSDK initialization
        JoglRenderer.verifyOpenGLAvailability();
        QuadBenchmark instance = new QuadBenchmark();
        instance.appletMode = false;
        instance.options = new Configuration();
        instance.options.process(args);
        instance.createModel();

        // Create application based GUI
        JFrame frame;
        Dimension size;

        instance.createGUI();
        frame = new JFrame("VITRAL concept test - Camera control example");
        frame.add(instance.canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        size = new Dimension(instance.options.windowXSize, instance.options.windowYSize);
        frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        instance.canvas.requestFocusInWindow();

        if ( instance.options.numberOfFrames >= 0 ) {
            Animator animator = new Animator(instance.canvas);
            instance.withAutomaticAnimation = true;
            animator.start();
            instance.framesToGo = instance.options.numberOfFrames;
        }
    }

    public void init()
    {
        appletMode = true;
        createModel();
        setLayout(new BorderLayout());
        createGUI();
        add("Center", canvas);
    }
    
//= PROGRAM PART 4/5: JOGL-OPENGL PROCEDURES ================================

    private void drawObjectsGL(GL2 gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        if ( withAutomaticAnimation ) {
            gl.glRotated(angle, 0, 0, 1);
            angle += 1.0;
            if ( angle > 360.0 ) {
                angle = 0.0;
            }
            framesToGo--;
            if ( framesToGo <= 0 ) {
                clock.stop();
                double t;
                double n;
                Geometry g;
                QuadMesh qm;
                long quads = 0;
                int qi[];

                t = clock.getElapsedRealTime();
                n = (double)options.numberOfFrames;
                int i;
                for ( i = 0; i < scene.getSimpleBodies().size(); i++ ) {
                    g = scene.getSimpleBodies().get(i).getGeometry();
                    if ( g instanceof QuadMesh ) {
                        qm = (QuadMesh)g;
                        qi = qm.getQuadIndices();
                        quads += qi.length/4;
                    }
                }


                System.out.println(options.numberOfFrames + " frames rendered on " + VSDK.formatDouble(t) + " seconds.");
                System.out.println("  - Frames per second: " + VSDK.formatDouble(n/t));
                System.out.println("  - Quads per second: " + VSDK.formatDouble((((double)quads)*n)/t));
                System.exit(1);
            }
        }

        gl.glLineWidth((float)3.0);
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

        int i;
        for ( i = 0;
              options.withVertexArrays && i < scene.getSimpleBodies().size();
              i++ ) {
            JoglSimpleBodyRenderer.drawWithVertexArrays(gl,
                scene.getSimpleBodies().get(i), camera, qualitySelection);
        }
        for ( i = 0;
              !options.withVertexArrays && i < scene.getSimpleBodies().size();
              i++ ) {
            JoglSimpleBodyRenderer.draw(gl, scene.getSimpleBodies().get(i),
                                        camera, qualitySelection);
        }

        if ( clock == null ) {
            // Placing this code at this point gets to not counting the time
            // of the first frame. This is to not account for display list
            // compile time, but to measure only sustained framerate.
            clock = new StopWatch();
            clock.start();
        }
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);        

        //-----------------------------------------------------------------
        if ( options.withDisplayList ) {
            JoglSimpleBodyRenderer.setAutomaticDisplayListManagement(true);
        }
        else {
            JoglSimpleBodyRenderer.setAutomaticDisplayListManagement(false);
        }
        drawObjectsGL(gl); 
   }
   
    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height); 

        camera.updateViewportResize(width, height);
    }   

//= PART 5/5: GUI PROCEDURES ================================================

    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
      //System.out.println("Mouse exited");
    }

    public void mousePressed(MouseEvent e) {
        if ( cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    /**
    WARNING: It is not working... check pending
    */
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            if ( !appletMode ) {
                System.exit(0);
            }
        }

        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if ( cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
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
