
//===========================================================================

// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity 
// involved. This will help him to dominate the involved libraries.

// Java base classes
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

// Java GUI classes
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

// JOGL classes
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLEventListener;
import com.jogamp.opengl.util.Animator;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;

/**
This program is an extended variation of the program
CgComplexStandardShaderExample with one addition:
  - Cg shader management is managed by vitral toolkit material / Renderer
    configuration scheme
*/
public class CgAutomaticShaderExample
    implements GLEventListener, MouseListener, MouseMotionListener,
               MouseWheelListener, KeyListener
{
    //- GUI ----------------------------------------------------------------
    private static GLCanvas canvas;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;

    //- Animation & state control ------------------------------------------
    private boolean firstTimer = true;
    private int needPaint = 3;
    private boolean withRotationAnimation = false;
    private boolean withLightAnimation = false;
    private double lightAngle = 0;

    //- Scene elements -----------------------------------------------------
    private Camera camera;                   // 1. Camera
    private Light light;                     // 2. Light
    private Material material;               // 3. Surface properties
    private RGBImage textureMap;
    private NormalMap normalMap;
    private RendererConfiguration quality;
    private double xrotation;                // 4. Geometrical transformations
    private double yrotation;
    private double zrotation;
    private SimpleBody thing;                // 5. Geometry

    //----------------------------------------------------------------------

    public CgAutomaticShaderExample(boolean appletMode) {
        if ( !appletMode ) init();
    }

    public void init() {
        //- Print users' manual -------------------------------------------
        System.out.println(
        "Key controls:\n"+
        "  - Camera: <cursor arrows>, x/X, y/Y, z/Z, s/S, a/A, f/F, n/N, i\n"+
        "  - Rendering quality: g, <function keys>\n"+
        "  - Animation control: <space>, r\n"+
        "  - Light control: 0, 9, h, k, j, u\n"+
        "  - Rotation control: 1, 2, 3, 4, 5, 6\n"+
        "  - Exit: <escape>\n"+
        "Mouse controls:\n"+
        "  - Drag+button1: camera orientation\n"+
        "  - Drag+button2: camera panning\n"+
        "  - Drag+button3: camera advance & roll\n"
        );
        System.out.print("Initializing... ");

        //- Initialize scene elements--------------------------------------
        // 1: Camera
        camera = new Camera();
        camera.setPosition(new Vector3D(0, -4, 0));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(90.0), 0, 0);
        camera.setRotation(R);
        camera.setFov(30.0);

        // 2: Lights
        light = new Light(Light.POINT, new Vector3D(0, -4, 0), new ColorRgb(1, 1, 1));

        // 3.1. Object attribute -> material propierties
        material = new Material();
        material.setAmbient(new ColorRgb(0, 0, 0));
        material.setDiffuse(new ColorRgb(1, 1, 1));
        material.setSpecular(new ColorRgb(1, 1, 1));
        material.setPhongExponent(40);

        // 3.2. Object attribute -> texture map & bumpmap
        String imageFilename = null;
        try {
            //-------------------------------------------------------
            imageFilename = "../../../etc/textures/miniearth.png";
            textureMap = ImagePersistence.importRGB(new File(imageFilename));

            //-------------------------------------------------------
            //RGBImage convertedNormalMap = null;

            normalMap = new NormalMap();
            //imageFilename = "../../../etc/bumpmaps/blinn2.bw";
            imageFilename = "../../../etc/bumpmaps/earth.bw";
            IndexedColorImage source = ImagePersistence.importIndexedColor(new File(imageFilename));
            normalMap.importBumpMap(source, new Vector3D(1, 1, 0.2));
            //convertedNormalMap = normalMap.exportToRgbImage();
            //ImagePersistence.exportPPM(new File("./outputmap.ppm"), convertedNormalMap);
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }

        // 3.3. Object attribute -> how it will render
        quality = new RendererConfiguration();
        quality.setBumpMap(true);

        // 4. Object attribute -> geometrical transformations
        xrotation = 0;
        yrotation = 0;
        zrotation = 0;

        // 5. Object attribute -> geometry
        thing = new SimpleBody();
        thing.setGeometry(new Sphere(1.0));
        thing.setTexture(textureMap);
        thing.setNormalMap(normalMap);
        thing.setMaterial(material);
        thing.setPosition(new Vector3D(0, 0, 0));
        thing.setRotation(new Matrix4x4());

        //- Initialize GUI helpers ----------------------------------------
        cameraController = new CameraControllerAquynza(camera);
        qualityController = new RendererConfigurationController(quality);

        //-----------------------------------------------------------------
        System.gc();
        System.out.println("Ok!");
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

    public void init(GLAutoDrawable drawable) {
        // Not used in VitralSDK style applications... check 'firstTimer'
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        if ( withRotationAnimation ) {
            zrotation += 0.5*2;
            needPaint = 1;
        }

        if ( withLightAnimation ) {
            Vector3D lightPosition = new Vector3D(1, -3, 1);
            Vector3D axis = new Vector3D(0, -1, 0);
            Matrix4x4 R = new Matrix4x4();
            lightAngle += Math.toRadians(1.0*2);
            R.axisRotation(lightAngle, axis);
            lightPosition = R.multiply(lightPosition);
            light.setPosition(lightPosition);
            needPaint = 1;
        }

        if ( needPaint <= 0 ) {
            return;
        }
        needPaint--;

        //-----------------------------------------------------------------
        if ( firstTimer ) {
            firstTimer = false;
            JoglRenderer.createDefaultAutomaticNvidiaCgShaders();
        }

        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1.0f);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        //- Shader configuration from scene data --------------------------
        JoglCameraRenderer.activate(gl, camera);
        JoglLightRenderer.activate(gl, light);

        //- Body transforms -----------------------------------------------
        gl.glLoadIdentity();
        gl.glRotated(xrotation, 1, 0, 0);
        gl.glRotated(yrotation, 0, 1, 0);
        gl.glRotated(zrotation, 0, 0, 1);

        JoglSimpleBodyRenderer.draw(gl, thing, camera, quality);

        gl.glLoadIdentity();
        JoglLightRenderer.draw(gl, light);
    }

    public void
    displayChanged(GLAutoDrawable drawable,
        boolean modeChanged, boolean deviceChanged) {
        // nothing
    }

    public void reshape(GLAutoDrawable drawable,
        int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        
        gl.glViewport(x, y, width, height); 
        camera.updateViewportResize(width, height);
        needPaint = 1;
    }

    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
        //System.out.println("Mouse exited");
    }

    public void mousePressed(MouseEvent e) {
        if ( cameraController.processMousePressedEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    /**
    WARNING: It is not working... check pending
    */
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( cameraController.processKeyPressedEventAwt(e) ) {
            needPaint = 1;
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEventAwt(e) ) {
            System.out.println(quality);
            needPaint = 1;
            canvas.repaint();
        }

        int unicode_id = e.getKeyChar();
        if ( unicode_id != e.CHAR_UNDEFINED ) {
          Vector3D lightPosition = light.getPosition();
          switch ( unicode_id ) {
            case '1': xrotation -= 1.0; break;
            case '2': xrotation += 1.0; break;
            case '3': yrotation -= 1.0; break;
            case '4': yrotation += 1.0; break;
            case '5': zrotation -= 1.0; break;
            case '6': zrotation += 1.0; break;
            case 'h': lightPosition.x -= 0.1; break;
            case 'k': lightPosition.x += 0.1; break;
            case 'u': lightPosition.z += 0.1; break;
            case 'j': lightPosition.z -= 0.1; break;
            case '9': lightPosition.y -= 0.1; break;
            case '0': lightPosition.y += 0.1; break;
            case 'r': withRotationAnimation = !withRotationAnimation; break;
            case ' ': withLightAnimation = !withLightAnimation; break;
          }
          light.setPosition(lightPosition);
          needPaint = 1;
          canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if ( cameraController.processKeyReleasedEventAwt(e) ) {
            needPaint = 1;
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

    public static void main(String[] argv) {
        //-----------------------------------------------------------------
        JoglRenderer.verifyOpenGLAvailability();
        JoglRenderer.verifyNvidiaCgAvailability();

        //-----------------------------------------------------------------
        CgAutomaticShaderExample instance = new CgAutomaticShaderExample(false);

        JFrame frame = new JFrame("Vitral SDK / Nvidia Cg demo");
        canvas = new GLCanvas();
        canvas.addGLEventListener(instance);
        canvas.addMouseListener(instance);
        canvas.addMouseMotionListener(instance);
        canvas.addMouseWheelListener(instance);
        canvas.addKeyListener(instance);
        canvas.addGLEventListener(instance);
        Animator animator = new Animator(canvas);

        frame.add(canvas);
        frame.setSize(1150, 1150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        canvas.requestFocusInWindow();
        animator.start();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
