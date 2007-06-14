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

// JOGL clases
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import com.sun.opengl.cg.CgGL;
import com.sun.opengl.cg.CGcontext;
import com.sun.opengl.cg.CGprogram;

// VitralSDK classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglSphereRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;

public class CgShaderExample implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
    private static GLCanvas canvas;
    private GLU glu = new GLU();

    private boolean NvidiaGpuEnabled = true;
    private CGprogram NvidiaGpuVertexProgram;
    private CGprogram NvidiaGpuFragmentProgram;

    private Camera camera;
    private CameraController cameraController;
    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private RGBImage img;
    private Sphere sphere;

    private double xrotation;
    private double yrotation;
    private double zrotation;
    private Vector3D lightPosition;

    public CgShaderExample(boolean appletMode) {
        if ( !appletMode ) init();
    }

    public void init() {
        xrotation = 0;
        yrotation = 0;
        zrotation = 0;
        lightPosition = new Vector3D(0, -4, 0);
        camera = new Camera();
        camera.setPosition(new Vector3D(0, -4, 0));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(90.0), 0, 0);
        camera.setRotation(R);
        cameraController = new CameraControllerAquynza(camera);

        quality = new RendererConfiguration();
        qualityController = new RendererConfigurationController(quality);

        sphere = new Sphere(1.0);

        String imageFilename = "../../../etc/textures/earth.png";
        try {
            img = ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }

    }

    public void init(GLAutoDrawable drawable) {
        if ( NvidiaGpuEnabled ) {
            JoglRenderer.tryToEnableNvidiaCg();
            try {
                NvidiaGpuVertexProgram =
                    JoglRenderer.loadNvidiaGpuVertexShader(
                        new FileInputStream("./etc/vertexShader.cg"));
                NvidiaGpuFragmentProgram =
                    JoglRenderer.loadNvidiaGpuPixelShader(
                        new FileInputStream("./etc/fragmentShader.cg"));
            }
            catch ( Exception e ) {
                System.out.println("Error loading shaders!");
                System.exit(1);
            }
        }
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        //-----------------------------------------------------------------
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        //-----------------------------------------------------------------
        JoglCameraRenderer.activate(gl, camera);

        //-----------------------------------------------------------------
        gl.glLoadIdentity();
        gl.glRotated(xrotation, 1, 0, 0);
        gl.glRotated(yrotation, 0, 1, 0);
        gl.glRotated(zrotation, 0, 0, 1);

        if ( NvidiaGpuEnabled ) {
            //- Global per-frame shader activation ----------------------------
            JoglRenderer.enableNvidiaCgProfiles();
            CgGL.cgGLBindProgram(NvidiaGpuVertexProgram);
            CgGL.cgGLBindProgram(NvidiaGpuFragmentProgram);

            //- Shader configuration from camera data -------------------------
            // (This should be managed by JoglCameraRenderer)
            {
            Matrix4x4 MProjection;
            Matrix4x4 MModelviewGlobal;
            Vector3D cp = camera.getPosition();
            double matrixarray[];
            double vectorarray[] = {cp.x, cp.y, cp.z};

            MProjection = camera.calculateViewVolumeMatrix();
            MModelviewGlobal = camera.calculateTransformationMatrix();
            matrixarray = MModelviewGlobal.exportToArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram,
                    "modelViewGlobal"), matrixarray, 0);
            CgGL.cgGLSetParameter3dv(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram,
                    "cameraPositionGlobal"), vectorarray, 0);
            }

            //- Shader configuration from light data --------------------------
            // (This should be managed by JoglLightRenderer)
            {
            double lpos[] = {lightPosition.x, lightPosition.y, lightPosition.z};
            double lightColor[] = {1.0, 1.0, 1.0};

            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "lightPositionGlobal"), lpos, 0);
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                NvidiaGpuFragmentProgram, "lightColor"), lightColor, 0);
            }

            //- Shader configuration from material data -----------------------
            // (This should be managed by JoglMaterialRenderer)
            {
            double Ka[] = {0.1, 0.1, 0.1};
            double Kd[] = {1.0, 1.0, 1.0};
            double Ks[] = {0.8, 0.8, 0.8};
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "ambientColor"), Ka, 0);
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "diffuseColor"), Kd, 0);
            CgGL.cgGLSetParameter3dv(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "specularColor"), Ks, 0);
            CgGL.cgGLSetParameter1d(CgGL.cgGetNamedParameter(
                NvidiaGpuFragmentProgram, "phongExponent"), 40);
            }

            //- Shader configuration from current object ----------------------
            // (This should be managed by JoglGeometryRenderer)
            {
            Matrix4x4 MProjection;
            Matrix4x4 MModelviewGlobal;
            Matrix4x4 MModelviewLocal, MModelviewLocalIT, MCombined;
            double matrixarray[];

            MProjection = camera.calculateViewVolumeMatrix();
            MModelviewGlobal = camera.calculateTransformationMatrix();
            MModelviewLocal = MModelviewGlobal.multiply(
                JoglMatrixRenderer.importJOGL(gl, gl.GL_MODELVIEW_MATRIX));
            MCombined = MProjection.multiply(MModelviewLocal);
            MModelviewLocalIT = MModelviewLocal.inverse();
            MModelviewLocalIT.transpose();

            matrixarray = MCombined.exportToArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "modelViewProjectionLocal"),
                matrixarray, 0);

            matrixarray = MModelviewLocal.exportToArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "modelViewLocal"),
                matrixarray, 0);

            matrixarray = MModelviewLocalIT.exportToArrayRowOrder();
            CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
                NvidiaGpuVertexProgram, "modelViewLocalIT"),
                matrixarray, 0);
            }
        }

        // And go ahead and draw the scene geometry
        enableTexture(gl);
        JoglSphereRenderer.draw(gl, sphere, camera, quality);

        if ( NvidiaGpuEnabled ) {
            JoglRenderer.disableNvidiaCgProfiles();
        }

    }

    void enableTexture(GL gl) {
        int glList;

        // Basic OpenGL texture state setup
        gl.glTexParameteri(GL.GL_TEXTURE_2D,
           GL.GL_GENERATE_MIPMAP_SGIS, GL.GL_TRUE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,
           GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,
           GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,
           GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,
           GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        glList = JoglImageRenderer.activate(gl, img);
    }

    public void
    displayChanged(GLAutoDrawable drawable,
        boolean modeChanged, boolean deviceChanged) {
        // nothing
    }

    public void reshape(GLAutoDrawable drawable,
        int x, int y, int width, int height) {
        GL gl = drawable.getGL();
        
        gl.glViewport(x, y, width, height); 
        camera.updateViewportResize(width, height);
    }

    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
        //System.out.println("Mouse exited");
    }

    public void mousePressed(MouseEvent e) {
        if ( cameraController.processMousePressedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    /**
    WARNING: It is not working... check pending
    */
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( cameraController.processKeyPressedEventAwt(e) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEventAwt(e) ) {
            System.out.println(quality);
            canvas.repaint();
        }

        int unicode_id = e.getKeyChar();
        if ( unicode_id != e.CHAR_UNDEFINED ) {
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
          }
          canvas.repaint();
        }
    }

    public void keyReleased(KeyEvent e) {
        if ( cameraController.processKeyReleasedEventAwt(e) ) {
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
        CgShaderExample instance = new CgShaderExample(false);

        JFrame frame = new JFrame("Vitral SDK / Nvidia Cg demo");
        canvas = new GLCanvas();
        canvas.addGLEventListener(instance);
        canvas.addMouseListener(instance);
        canvas.addMouseMotionListener(instance);
        canvas.addKeyListener(instance);
        canvas.addGLEventListener(instance);

        frame.add(canvas);
        frame.setSize(1150, 1150);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
