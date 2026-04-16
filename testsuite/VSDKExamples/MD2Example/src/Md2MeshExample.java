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
import vsdk.toolkit.common.RendererConfiguration;    // Model elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.render.jogl.JoglCameraRenderer; // View elements
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.gui.CameraController;           // Control elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.environment.geometry.Md2Mesh;
import vsdk.toolkit.io.geometry.Md2Persistence;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.render.jogl.JoglMd2MeshRenderer;
import vsdk.toolkit.animation.Md2AnimationListener;
import vsdk.toolkit.common.VSDK;

// Application classes
//import util.filters.ObjectFilter;

//Polygon simplify.
import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.environment.geometry._Polygon2DContour;
import vsdk.toolkit.render.jogl.animation.JoglRepainterAnimationListener;

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
    //public GLCanvas canvas;
    public GLJPanel canvas;

    private SimpleScene scene;
    private Md2Persistence md2Pers;
    private Md2Mesh md2Mesh;
    private AnimationEventGenerator animator;
    //private JoglMd2MeshRenderer Md2MeshRend = new JoglMd2MeshRenderer();

    public double x;
    Vertex2D p0,p1,p2;
    private final _Polygon2DContour globP2DContour = new _Polygon2DContour();
    private final _Polygon2DContour globP2DContourSimp = new _Polygon2DContour();

    public Md2MeshExample(String fileName) {
        super("VITRAL mesh test - JOGL");
        
        init();
    }

    private void init() throws GLException {
        String fileName;
        File file = null;
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCaps = new GLCapabilities(glp);
        String texture;
        int i;
        
        scene = new SimpleScene();
        md2Mesh = new Md2Mesh();
        //-----------------------------------------------------------------
        //fileName = "C:\\Politecnica\\ModelosEdificios\\Biblioteca I. E. Municipio de San Agustín\\Biblioteca Laureano Gomez-San Agustín\\lorenzoGomesFinal_sinPiso.obj";
//        fileName = "C:\\Politecnica\\etc\\geometry\\Blade.md2";
//        texture  = "C:\\Politecnica\\etc\\geometry\\Texture\\Blade.jpg";
//        fileName = "C:\\Politecnica\\etc\\geometry\\Samourai.md2";
//        texture  = "C:\\Politecnica\\etc\\geometry\\Texture\\Samourai.jpg";
        fileName = "/Users/jedilink/usr/copiaSvn/vitral/etc/md2/samourai.md2";
        texture  = "/Users/jedilink/usr/copiaSvn/vitral/etc/md2/samourai.jpg";
        //fileName = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\Samourai\\Samourai.md2";
        //texture  = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\Samourai\\Samourai.jpg";
//        fileName = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\eje_01_DEFAULT_GenNormals.md2";
//        fileName = "E:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\eje_02_Segmento Respiracion.md2";
//        fileName = "E:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\eje_03_Declinacion_normales_VR2015.md2";
//        texture  = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\M_diffuse.jpg";
//        fileName = "D:\\Leonardo\\TrabajoPolitecnica\\__Modelos\\MD2\\Nina\\TEX 02_02.md2";
//        texture  = "D:\\Leonardo\\TrabajoPolitecnica\\__Modelos\\MD2\\Nina\\Avatar_Nina_Body_Dm_ajustes.jpg";
        try {
            md2Pers = new Md2Persistence();
            md2Pers.read(fileName,texture,md2Mesh);
            md2Mesh.setCurrentAnimationInd((short)0);
        } catch ( IOException ex ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, "Md2MeshExample",
                "Input/Output error", ex);
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
        cameraController.setDeltaMovement(10);
        camera.setPosition(new Vector3D(0, -100, 0));
        camera.setFarPlaneDistance(4000);
        qualitySelection = new RendererConfiguration();
        qualityController = new RendererConfigurationController(qualitySelection);
        light = new Light(Light.POINT, new Vector3D(10, -20, 50), new ColorRgb(1, 1, 1));
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

    private void drawObjectsGL(GL2 gl) {
        int i;
       
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
        
        
        JoglMd2MeshRenderer.draw(gl,md2Mesh); //LRR

    }
    private void polygonSimpTest(GL2 gl) {
        int i;
        
        gl.glBegin(GL2.GL_LINE_LOOP);
            gl.glColor3d(0.2, 0.8, 0.2);
            for(i=0;i<globP2DContour.vertices.size();++i){
                gl.glVertex3d(globP2DContour.vertices.get(i).x, 0, globP2DContour.vertices.get(i).y);
            }
        
        gl.glEnd();
        gl.glBegin(GL2.GL_LINE_LOOP);
            gl.glColor3d(0.9, 0.8, 0.2);
            for(i=0;i<globP2DContourSimp.vertices.size();++i){
                gl.glVertex3d(globP2DContourSimp.vertices.get(i).x, 0, globP2DContourSimp.vertices.get(i).y);
            }
        
        gl.glEnd();
    }
    /** Called by drawable to initiate drawing
     * @param drawable */
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

    /** Not used method, but needed to instantiate GLEventListener
     * @param drawable */
    @Override
    public void init(GLAutoDrawable drawable) {
        JoglMd2MeshRenderer.initGL(drawable.getGL().getGL2(), md2Mesh);
        createAnimator(md2Mesh, canvas);
    }
    
    public void createAnimator(Md2Mesh md2Mesh, GLJPanel gljp)
    {
        //- Set up animation control thread -----------------------------------
        animator = new AnimationEventGenerator();
        Md2AnimationListener md2AniListener = new Md2AnimationListener(md2Mesh);
        animator.addAnimationListener(md2AniListener);
        JoglRepainterAnimationListener repainterListener;
        repainterListener = new JoglRepainterAnimationListener(gljp);
        animator.addAnimationListener(repainterListener);
        Thread t  = new Thread(animator);
        t.start();
    }
    
    /** Not used method, but needed to instantiate GLEventListener
     * @param drawable
     * @param a
     * @param b */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        
    }

    /** Called to indicate the drawing surface has been moved and/or resized
     * @param drawable
     * @param x
     * @param y
     * @param width
     * @param height */
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
        if (cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if (cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        short[] animStartEnd = new short[2];
        
        if ( e.getKeyCode() == KeyEvent.VK_1 ) {
            md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
            if(md2Mesh.getCurrentAnimationInd() == animStartEnd[0])
                md2Mesh.setCurrentAnimationInd(md2Mesh.getMaxAnimationInd());
            else
                md2Mesh.setCurrentAnimationInd((short)(md2Mesh.getCurrentAnimationInd()-1));
        }
        if ( e.getKeyCode() == KeyEvent.VK_2 ) {
            md2Mesh.setCurrentAnimationInd((short)(md2Mesh.getCurrentAnimationInd()+1));
        }
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection);
        }
        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            System.out.println(qualitySelection);
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
     * @param e
    */
    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    /** Not used method, but needed to instantiate GLEventListener
     * @param drawable */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        
    }
}
