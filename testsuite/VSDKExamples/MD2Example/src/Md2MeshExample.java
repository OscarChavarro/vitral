//===========================================================================

// Basic java classes
import java.io.File;
import java.io.IOException;

// AWT/Swing GUI java classes
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
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;    // Model elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.environment.geometry.Md2Mesh;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.render.jogl.JoglCameraRenderer; // View elements
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMd2MeshRenderer;
import vsdk.toolkit.gui.CameraController;           // Control elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.io.geometry.Md2Persistence;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.animation.Md2AnimationListener;
import vsdk.toolkit.common.VSDK;

/**
 */
public class Md2MeshExample
    extends JFrame implements GLEventListener, MouseListener,
                   MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private Light light;
    private CameraController cameraController;
    private RendererConfiguration qualitySelection;
    private RendererConfigurationController qualityController;
    public GLJPanel canvas;

    private SimpleScene scene;
    private Md2Persistence md2Pers;
    private Md2Mesh md2Mesh;
    private AnimationEventGenerator animator;

    private double x;
    private Vertex2D p0;
    private Vertex2D p1;
    private Vertex2D p2;

    public Md2MeshExample(String fileName) {
        super("VITRAL MD2 animated mesh test - JOGL");
        File file = null;
        GLProfile glp;
        glp = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCaps = new GLCapabilities(glp);
        String texture;
        int i;

        scene = new SimpleScene();
        md2Mesh = new Md2Mesh();

        //-----------------------------------------------------------------
//        fileName = "C:\\Politecnica\\etc\\geometry\\samourai.md2";
//        texture = "C:\\Politecnica\\etc\\geometry\\Texture\\samourai.jpg";
        fileName = "../../../etc/md2/samourai.md2";
        texture = "../../../etc/md2/samourai.jpg";
        try {
            md2Pers = new Md2Persistence();
            md2Pers.read(fileName,texture,md2Mesh);
            md2Mesh.setCurrentAnimationInd((short)0);
        } catch ( IOException ex ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, 
                "Md2MeshExample", "Failed to read file " + fileName, ex);
            System.exit(0);
        }
       
        //-----------------------------------------------------------------
        canvas = new GLJPanel(glCaps);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        qualitySelection = new RendererConfiguration();
        qualityController = 
            new RendererConfigurationController(qualitySelection);

        light = new Light(Light.POINT, 
            new Vector3D(10, -20, 50), new ColorRgb(1, 1, 1));
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

//        Animador h = new Animador((Md2MeshExample)f);
//        Thread x = new Thread(h);
        //x.start();


        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL2 gl) {
        int i;
//        float[] LightPosition = new float[4];
//        float[] LightAmbient = new float[4];
//        float[] LightDiffuse = new float[4];
       
        gl.glLoadIdentity();

        gl.glTranslated(x, 0, 0);

        // Draw reference frame
        gl.glLineWidth((float)3.0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glBegin(GL2.GL_LINES);
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

        
        ///Temp polygon simplification  LRR
        //polygonSimpTest(gl);
        //Polygon simplification
        
        
        // Draw mesh
        gl.glDisable(GL2.GL_CULL_FACE);
        //gl.glCullFace(gl.GL_BACK);
        
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        
        float pos[] = {200f, 200f, 0f, 1.0f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, pos, 0);
        float dif[] = {1.0f,1.0f,1.0f,1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
        float amb[] = {0.7f,0.7f, 0.7f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);        
        
        
        JoglMd2MeshRenderer.draw(gl, md2Mesh);
    }
    /**
    Called by drawable to initiate drawing
    @param drawable
    */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        
        JoglCameraRenderer.activate(gl, camera);
        JoglLightRenderer.activate(gl, light);

        drawObjectsGL(gl);
    }

    /** 
    Not used method, but needed to instantiate GLEventListener
    @param drawable 
    */
    @Override
    public void init(GLAutoDrawable drawable) {
        JoglMd2MeshRenderer.initGL(drawable.getGL().getGL2(), md2Mesh);
        createAnimator(md2Mesh, canvas);
    }
    
    public void createAnimator(Md2Mesh md2Mesh, GLJPanel gljp)
    {
       //- Set up animation control thread -----------------------------------
       animator = new AnimationEventGenerator();
       Thread t  = new Thread(animator);
       t.start();

       // This is causing to repeat the canvas repaint!

       //streetSelectorListener = new StreetSelectorAnimationListener(c, m);
       //animator.addAnimationListener(streetSelectorListener);

       Md2AnimationListener testListener = 
           new Md2AnimationListener(gljp, md2Mesh);
       animator.addAnimationListener(testListener);
    }
    
    /** 
    Not used method, but needed to instantiate GLEventListener
    @param drawable
    @param a
    @param b 
    */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        
    }

    /** 
    Called to indicate the drawing surface has been moved and/or resized
    @param drawable
    @param x
    @param y
    @param width
    @param height 
    */
    @Override
    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);

        camera.updateViewportResize(width, height);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if ( cameraController.processMousePressedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( cameraController.processMouseWheelEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        short[] animStartEnd = new short[2];
        
        if ( e.getKeyCode() == KeyEvent.VK_1 ) {
            md2Mesh.returnStartEndAnim(
                md2Mesh.getCurrentAnimationInd(), animStartEnd);
            if(md2Mesh.getCurrentAnimationInd() == animStartEnd[0])
                md2Mesh.setCurrentAnimationInd(md2Mesh.getMaxAnimationInd());
            else
                md2Mesh.setCurrentAnimationInd((short)(
                    md2Mesh.getCurrentAnimationInd()-1));
        }
        if ( e.getKeyCode() == KeyEvent.VK_2 ) {
            md2Mesh.setCurrentAnimationInd((short)(
                md2Mesh.getCurrentAnimationInd()+1));
        }
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection);
        }
        if ( cameraController.processKeyPressedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            System.out.println(qualitySelection);
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (cameraController.processKeyReleasedEvent(
            AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEvent(
            AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
    @param e
    */
    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    /** 
    Not used method, but needed to instantiate GLEventListener
    @param drawable 
    */
    @Override
    public void dispose(GLAutoDrawable drawable) {
       
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
