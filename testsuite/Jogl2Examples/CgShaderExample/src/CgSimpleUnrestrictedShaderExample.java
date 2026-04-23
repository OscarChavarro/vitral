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
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.cg.CgGL;
import com.jogamp.opengl.cg.CGcontext;
import com.jogamp.opengl.cg.CGprogram;
import com.jogamp.opengl.util.Animator;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.render.jogl.Jogl2GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;
import vsdk.toolkit.render.jogl.Jogl2MatrixRenderer;
import vsdk.toolkit.render.joglcg.JoglCgCameraRenderer;
import vsdk.toolkit.render.joglcg.JoglCgMaterialRenderer;
import vsdk.toolkit.render.joglcg.JoglCgRenderer;
import vsdk.toolkit.render.joglcg.JoglCgImageRenderer;

/**
This program constitutes a template for VitralSDK based applications which
make use of Nvidia Cg shaders. Note that current code does not use standard
VitralSDK material handling, but all shader management is standalone and
contained here.  Use this program as a base for custom developed programs
and shaders.
*/
public class CgSimpleUnrestrictedShaderExample
    implements GLEventListener, MouseListener, MouseMotionListener,
               MouseWheelListener, KeyListener
{
    //- GUI ----------------------------------------------------------------
    private static GLCanvas canvas;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;

    //- GPU control --------------------------------------------------------
    private boolean NvidiaGpuActive = true;
    private boolean NvidiaGpuAvailable = true;
    private CGprogram NvidiaGpuVertexProgramTexture;
    private CGprogram NvidiaGpuPixelProgramTexture;

    //- Animation & state control ------------------------------------------
    private boolean firstTimer = true;
    private int needPaint = 3;
    private boolean retextureNeeded = false;
    private boolean withRotationAnimation = false;
    private boolean withLightAnimation = false;
    private double lightAngle = 0;

    //- Scene elements -----------------------------------------------------
    private Camera camera;                   // 1. Camera
    private Light light;                     // 2. Light
    private Material material;               // 3. Surface properties
    private RGBImage textureMap;
    private RendererConfiguration quality;
    private double xrotation;                // 4. Geometrical transformations
    private double yrotation;
    private double zrotation;
    private Geometry geometry;                   // 5. Geometry

    //----------------------------------------------------------------------

    public CgSimpleUnrestrictedShaderExample(boolean appletMode) {
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
        R = R.eulerAnglesRotation(Math.toRadians(90.0), 0, 0);
        camera.setRotation(R);
        camera.setFov(30.0);

        // 2: Lights
        light = new Light(vsdk.toolkit.environment.LightType.POINT, new Vector3D(0, -4, 0), new ColorRgb(1, 1, 1));
        light.setId(0);

        // 3.1. Object attribute -> material propierties
        material = new Material();
        material.setAmbient(new ColorRgb(0, 0, 0));
        material.setDiffuse(new ColorRgb(1, 1, 1));
        material.setSpecular(new ColorRgb(1, 1, 1));
        material.setPhongExponent(40);

        // 3.2. Object attribute -> texture map
        String imageFilename = null;
        try {
            //-------------------------------------------------------
            imageFilename = "../../../etc/textures/miniearth.png";
            textureMap = ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }

        // 3.3. Object attribute -> how it will render
        quality = new RendererConfiguration();

        // 4. Object attribute -> geometrical transformations
        xrotation = 0;
        yrotation = 0;
        zrotation = 0;

        // 5. Object attribute -> geometry
        geometry = new Sphere(1.0);

        //- Initialize GUI helpers ----------------------------------------
        cameraController = new CameraControllerAquynza(camera);
        qualityController = new RendererConfigurationController(quality);

        //-----------------------------------------------------------------
        System.gc();
        System.out.println("Ok!");
    }

    public void createCgElements() {
        if ( !JoglCgRenderer.tryToEnableNvidiaCg() ) {
            System.out.println("Nvidia Cg not available. Turning off GPU support!");
            NvidiaGpuActive = false;
            NvidiaGpuAvailable = false;
            return;
        }
        try {
            //-----------------------------------------------------------------
            NvidiaGpuVertexProgramTexture =
              JoglCgRenderer.loadNvidiaGpuVertexShader(
                new FileInputStream("../../../etc/cgShaders/PhongTextureVertexShader.cg"));
            NvidiaGpuPixelProgramTexture =
              JoglCgRenderer.loadNvidiaGpuPixelShader(
                new FileInputStream("../../../etc/cgShaders/PhongTexturePixelShader.cg"));
        }
        catch ( Exception e ) {
            System.err.println("Error loading shaders!");
            System.exit(1);
        }
    }

    public void init(GLAutoDrawable drawable) {
        // Not used in VitralSDK style applications... check 'firstTimer'
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
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
            R = R.axisRotation(lightAngle, axis);
            lightPosition = R.multiply(lightPosition);
            light.setPosition(lightPosition);
            needPaint = 1;
        }

        if ( needPaint <= 0 ) {
            return;
        }
        needPaint--;

        if ( firstTimer ) {
            firstTimer = false;
            createCgElements();
        }

        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1.0f);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        //-----------------------------------------------------------------
        JoglCgCameraRenderer.activate(gl, camera);

        //-----------------------------------------------------------------
        gl.glLoadIdentity();
        gl.glRotated(xrotation, 1, 0, 0);
        gl.glRotated(yrotation, 0, 1, 0);
        gl.glRotated(zrotation, 0, 0, 1);

        if ( NvidiaGpuActive ) {
            //- Global per-frame shader activation ----------------------------
            JoglCgRenderer.enableNvidiaCgProfiles();
            CGprogram currentVertexProgram; // Use this variables to choose
            CGprogram currentPixelProgram;  // between various shaders...
            currentVertexProgram = NvidiaGpuVertexProgramTexture;
            currentPixelProgram = NvidiaGpuPixelProgramTexture;

            CgGL.cgGLBindProgram(currentVertexProgram);
            CgGL.cgGLBindProgram(currentPixelProgram);

            //- Shader configuration for special features ---------------------
            // (This should be managed by JoglCgRenderer, usually with the help
            // of RendererConfiguration)
            {
            double withTexture = 0.0;
            if ( quality.isTextureSet() ) withTexture = 1.0;
            CgGL.cgGLSetParameter1d(CgGL.cgGetNamedParameter(
                currentPixelProgram, "withTexture"), withTexture);
            }

            //- Shader configuration from camera data -------------------------
            // (This should be managed by Jogl2CameraRenderer)
            {
            Matrix4x4 MProjection;
            Matrix4x4 MModelviewGlobal;
            Vector3D cp = camera.getPosition();
            double matrixarray[];
            double vectorarray[] = {cp.x, cp.y, cp.z};

            MProjection = camera.calculateViewVolumeMatrix();
            MModelviewGlobal = camera.calculateTransformationMatrix();
            matrixarray = MModelviewGlobal.exportToDoubleArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(
                CgGL.cgGetNamedParameter(currentVertexProgram,
                    "modelViewGlobal"), matrixarray, 0);
            CgGL.cgGLSetParameter3dv(
                CgGL.cgGetNamedParameter(currentVertexProgram,
                    "cameraPositionGlobal"), vectorarray, 0);
            }

            //- Shader configuration from light data --------------------------
            // (This should be managed by Jogl2LightRenderer)
            {
            Vector3D lp = light.getPosition();
            double lpos[] = {lp.x, lp.y, lp.z};
            double lightColor[] = {1.0, 1.0, 1.0};

            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                currentVertexProgram, "lightPositionGlobal"), lpos, 0);
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                currentPixelProgram, "lightColor"), lightColor, 0);
            }

            //- Shader configuration from material data -----------------------
            // (This should be managed by JoglCgMaterialRenderer)
            {
            double Ka[] = material.getAmbient().exportToDoubleArrayVect();
            double Kd[] = material.getDiffuse().exportToDoubleArrayVect();
            double Ks[] = material.getSpecular().exportToDoubleArrayVect();
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                currentVertexProgram, "ambientColor"), Ka, 0);
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                currentVertexProgram, "diffuseColor"), Kd, 0);
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                currentVertexProgram, "specularColor"), Ks, 0);
            CgGL.cgGLSetParameter1d(CgGL.cgGetNamedParameter(
                currentPixelProgram, "phongExponent"), material.getPhongExponent());
            }

            //- Shader configuration from current object ----------------------
            // (This should be managed by Jogl2GeometryRenderer)
            {
            Matrix4x4 MProjection;
            Matrix4x4 MModelviewGlobal;
            Matrix4x4 MModelviewLocal, MModelviewLocalIT, MCombined;
            double matrixarray[];

            MProjection = camera.calculateViewVolumeMatrix();
            MModelviewGlobal = camera.calculateTransformationMatrix();
            MModelviewLocal = MModelviewGlobal.multiply(
                Jogl2MatrixRenderer.importJOGL(gl, gl.GL_MODELVIEW_MATRIX));
            MCombined = MProjection.multiply(MModelviewLocal);
            MModelviewLocalIT = MModelviewLocal.inverse();
            MModelviewLocalIT = MModelviewLocalIT.transpose();

            matrixarray = MCombined.exportToDoubleArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
                currentVertexProgram, "modelViewProjectionLocal"),
                matrixarray, 0);

            matrixarray = MModelviewLocal.exportToDoubleArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
                currentVertexProgram, "modelViewLocal"),
                matrixarray, 0);

            matrixarray = MModelviewLocalIT.exportToDoubleArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
                currentVertexProgram, "modelViewLocalIT"),
                matrixarray, 0);
            }
        }
        else {
            Jogl2LightRenderer.activate(gl, light);
            JoglCgMaterialRenderer.activate(gl, material);
            if ( quality.isTextureSet() ) {
                gl.glEnable(gl.GL_TEXTURE_2D);
            }
            else {
                gl.glDisable(gl.GL_TEXTURE_2D);
            }
        }

        // And go ahead and draw the scene geometry
        enableTexture(gl);
        Jogl2GeometryRenderer.draw(gl, geometry, camera, quality);

        if ( NvidiaGpuActive ) {
            JoglCgRenderer.disableNvidiaCgProfiles();
        }
        gl.glLoadIdentity();
        Jogl2LightRenderer.draw(gl, light);
    }

    void enableTexture(GL2 gl) {
        int glList;

        //- Basic OpenGL texture state setup ------------------------------
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_GENERATE_MIPMAP,
            gl.GL_TRUE);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER,
            gl.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER,
            gl.GL_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S,
            gl.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T,
            gl.GL_CLAMP_TO_EDGE);

        glList = JoglCgImageRenderer.activate(gl, textureMap);
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
        if ( cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    /**
    WARNING: It is not working... check pending
    */
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            needPaint = 1;
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
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
            case 'g':
              if ( NvidiaGpuAvailable ) {
                  NvidiaGpuActive = !NvidiaGpuActive;
              }
              else {
                  NvidiaGpuActive = false;
                  System.out.println("Nvidia Cg not available. Turning on GPU support not available!");
              }
              break;
            case 'r': withRotationAnimation = !withRotationAnimation; break;
            case ' ': withLightAnimation = !withLightAnimation; break;
          }
          light.setPosition(lightPosition);
          needPaint = 1;
          canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if ( cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
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
        JoglCgRenderer.verifyOpenGLAvailability();
        JoglCgRenderer.verifyNvidiaCgAvailability();
        JoglCgRenderer.setNvidiaCgAutomaticMode(false);

        //-----------------------------------------------------------------
        CgSimpleUnrestrictedShaderExample instance = new CgSimpleUnrestrictedShaderExample(false);

        JFrame frame = new JFrame("Vitral SDK / Nvidia Cg demo");
        canvas = new GLCanvas();
        canvas.addMouseListener(instance);
        canvas.addMouseMotionListener(instance);
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
