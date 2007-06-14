//===========================================================================

// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity 
// involved. This will help him to dominate the involved libraries.

// Java base classes
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.nio.FloatBuffer;

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
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglSphereRenderer;

public class CgShaderExample implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
    private static GLCanvas canvas;
    private GLU glu = new GLU();

    private boolean NvidiaGpuEnabled = true;
    private CGprogram NvidiaGpuVertexProgram;
    private CGprogram NvidiaGpuFragmentProgram;

    private Camera camera;
    private CameraController cameraController;
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

        sphere = new Sphere(1.0);

        String imageFilename = "earth.gif";
        //String imageFilename = "../../../../../../aquynza/samples/bumpmaps/earth.png";
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
            LoadCgPrograms();
        }
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        //-----------------------------------------------------------------
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glClearColor(.25f, .25f, .25f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        //-----------------------------------------------------------------
        //JoglCameraRenderer.activate(gl, camera);
        
        Vector3D cameraPosition = camera.getPosition();
        Vector3D cameraFocus = camera.getFocusedPosition();
        Vector3D cameraUp = camera.getUp();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(camera.getFov(), camera.getViewportXSize() / camera.getViewportYSize(), 
                           camera.getNearPlaneDistance(), camera.getFarPlaneDistance());

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(
            cameraPosition.x, cameraPosition.y, cameraPosition.z,
            cameraFocus.x, cameraFocus.y, cameraFocus.z, 
            cameraUp.x, cameraUp.y, cameraUp.z);

        //-----------------------------------------------------------------
        gl.glRotated(xrotation, 1, 0, 0);
        gl.glRotated(yrotation, 0, 1, 0);
        gl.glRotated(zrotation, 0, 0, 1);

        if ( NvidiaGpuEnabled ) {
            // Bind uniform parameters to vertex shader
            CgGL.cgGLSetStateMatrixParameter(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "ModelViewProj"),
                CgGL.CG_GL_MODELVIEW_PROJECTION_MATRIX,
                CgGL.CG_GL_MATRIX_IDENTITY);
            CgGL.cgGLSetStateMatrixParameter(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "ModelView"),
                CgGL.CG_GL_MODELVIEW_MATRIX,
                CgGL.CG_GL_MATRIX_IDENTITY);
            CgGL.cgGLSetStateMatrixParameter(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "ModelViewIT"),
                CgGL.CG_GL_MODELVIEW_MATRIX,
                CgGL.CG_GL_MATRIX_INVERSE_TRANSPOSE);
            Vector3D cp = camera.getPosition();
            float campos[] = { (float)cp.x, (float)cp.y, (float)cp.z};
            CgGL.cgGLSetParameter3fv(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "PCamera1"), campos, 0);
            float lpos[] = { (float)lightPosition.x, (float)lightPosition.y, (float)lightPosition.z};
            CgGL.cgGLSetParameter3fv(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "PLight1"), lpos, 0);

            // We can also go ahead and bind varying parameters to vertex shader
            // that we just want to have the same value for all vertices.
            float Kd[] = { 1.0f, 1.0f, 1.0f }, Ks[] = { 0.8f, 0.8f, 0.8f };
            CgGL.cgGLSetParameter3fv(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "diffuse"), Kd, 0);
            CgGL.cgGLSetParameter3fv(
                CgGL.cgGetNamedParameter(NvidiaGpuVertexProgram, "specular"), Ks, 0);

            // Now bind uniform parameters to fragment shader
            float lightColor[] = { 1, 1, 1 };
            CgGL.cgGLSetParameter3fv(
                CgGL.cgGetNamedParameter(NvidiaGpuFragmentProgram, "lightColor"), 
                                     lightColor, 0);
            CgGL.cgGLSetParameter1f(
                CgGL.cgGetNamedParameter(NvidiaGpuFragmentProgram, "shininess"), 40);
            CgGL.cgGLBindProgram(NvidiaGpuVertexProgram);
            CgGL.cgGLBindProgram(NvidiaGpuFragmentProgram);

        }

        // And go ahead and draw the scene geometry
        enableTexture(gl);
        JoglSphereRenderer.draw(gl, sphere, new vsdk.toolkit.environment.Camera(), new vsdk.toolkit.common.RendererConfiguration());
    }

    void LoadCgPrograms() {
        try {
            NvidiaGpuVertexProgram = JoglRenderer.loadNvidiaGpuVertexShader(
                new FileInputStream("./etc/vertexShader.cg"));
            NvidiaGpuFragmentProgram = JoglRenderer.loadNvidiaGpuPixelShader(
                new FileInputStream("./etc/fragmentShader.cg"));
        }
        catch ( Exception e ) {
            System.out.println("Error loading shaders!");
            System.exit(1);
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
        frame.setSize(750, 750);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
