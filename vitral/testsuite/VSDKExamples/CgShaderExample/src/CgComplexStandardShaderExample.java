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
import com.sun.opengl.cg.CgGL;
import com.sun.opengl.cg.CGcontext;
import com.sun.opengl.cg.CGprogram;
import com.sun.opengl.cg.CGparameter;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglNormalMapRenderer;
import vsdk.toolkit.render.jogl.JoglSphereRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;

public class CgComplexStandardShaderExample implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
    //- GUI ----------------------------------------------------------------
    private static GLCanvas canvas;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;

    //- GPU control --------------------------------------------------------
    private boolean NvidiaGpuActive = true;
    private CGprogram NvidiaGpuVertexProgramTexture;
    private CGprogram NvidiaGpuPixelProgramTexture;
    private CGprogram NvidiaGpuVertexProgramTextureBump;
    private CGprogram NvidiaGpuPixelProgramTextureBump;
    private int normalMapList;
    private int textureMapList;

    //- Scene elements -----------------------------------------------------
    private Camera camera;
    private Material material;
    private Light light;
    private RGBImage textureMap;
    private NormalMap normalMap;
    private RendererConfiguration quality;
    private double xrotation;
    private double yrotation;
    private double zrotation;
    private Sphere sphere;

    RGBImage exported;

    //----------------------------------------------------------------------

    public CgComplexStandardShaderExample(boolean appletMode) {
        if ( !appletMode ) init();
    }

    public void init() {
        //- Initialize scene elements--------------------------------------
        // 1: Camera
        camera = new Camera();
        camera.setPosition(new Vector3D(0, -4, 0));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(90.0), 0, 0);
        camera.setRotation(R);

        // 2: Lights
        light = new Light(Light.POINT, new Vector3D(0, -4, 0), new ColorRgb(1, 1, 1));

        // 3.1. Object attribute -> texture map & bumpmap
        String imageFilename = "../../../etc/textures/earth.png";
        try {
            textureMap = ImagePersistence.importRGB(new File(imageFilename));

            normalMap = new NormalMap();
            imageFilename = "../../../etc/bumpmaps/blinn2.bw";
            //imageFilename = "../../../etc/bumpmaps/earth.bw";
            IndexedColorImage source = ImagePersistence.importIndexedColor(new File(imageFilename));
            normalMap.importBumpMap(source, new Vector3D(1, 1, 0.2));

            exported = normalMap.exportToRgbImage();
            ImagePersistence.exportPPM(new File("./outputmap.ppm"), exported);
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }

        // 3.2. Object attribute -> material propierties
        material = new Material();
        material.setAmbient(new ColorRgb(0, 0, 0));
        material.setDiffuse(new ColorRgb(1, 1, 1));
        material.setSpecular(new ColorRgb(1, 1, 1));
        material.setPhongExponent(40);

        // 3.3. Object attribute -> how it will render
        quality = new RendererConfiguration();
        quality.setBumpMap(true);

        // 3.4. Object attribute -> geometrical transformations
        xrotation = 0;
        yrotation = 0;
        zrotation = 0;

        // 3.5. Object attribute -> geometry
        sphere = new Sphere(1.0);

        //-----------------------------------------------------------------
        cameraController = new CameraControllerAquynza(camera);
        qualityController = new RendererConfigurationController(quality);

        //-----------------------------------------------------------------
    }

    public void init(GLAutoDrawable drawable) {
        JoglRenderer.tryToEnableNvidiaCg();
        try {
            NvidiaGpuVertexProgramTexture =
                JoglRenderer.loadNvidiaGpuVertexShader(
                    new FileInputStream("./etc/PhongTextureVertexShader.cg"));
            NvidiaGpuPixelProgramTexture =
                JoglRenderer.loadNvidiaGpuPixelShader(
                    new FileInputStream("./etc/PhongTexturePixelShader.cg"));
            NvidiaGpuVertexProgramTextureBump =
                JoglRenderer.loadNvidiaGpuVertexShader(
                    new FileInputStream("./etc/PhongTextureBumpVertexShader.cg"));
            NvidiaGpuPixelProgramTextureBump =
                JoglRenderer.loadNvidiaGpuPixelShader(
                    new FileInputStream("./etc/PhongTextureBumpPixelShader.cg"));
        }
        catch ( Exception e ) {
            System.err.println("Error loading shaders!");
            System.exit(1);
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

        CGprogram currentVertexProgram = null;
        CGprogram currentPixelProgram = null;
        CGparameter param = null;

        enableTexture(gl, NvidiaGpuActive && quality.isBumpMapSet());

        if ( NvidiaGpuActive ) {
            //- Global per-frame shader activation ----------------------------
            JoglRenderer.enableNvidiaCgProfiles();
            if ( quality.isBumpMapSet() ) {
                currentVertexProgram = NvidiaGpuVertexProgramTextureBump;
                currentPixelProgram = NvidiaGpuPixelProgramTextureBump;
              }
              else {
                currentVertexProgram = NvidiaGpuVertexProgramTexture;
                currentPixelProgram = NvidiaGpuPixelProgramTexture;
            }

            CgGL.cgGLBindProgram(currentVertexProgram);
            CgGL.cgGLBindProgram(currentPixelProgram);

            //- Shader configuration from scene data --------------------------
            JoglRenderer.activateNvidiaGpuParameters(gl, quality,
                currentVertexProgram, currentPixelProgram);
            JoglCameraRenderer.activateNvidiaGpuParameters(gl, camera,
                currentVertexProgram, currentPixelProgram);
            JoglLightRenderer.activateNvidiaGpuParameters(gl, light,
                currentVertexProgram, currentPixelProgram);
            JoglMaterialRenderer.activateNvidiaGpuParameters(gl, material,
                currentVertexProgram, currentPixelProgram);
            JoglGeometryRenderer.activateNvidiaGpuParameters(gl, sphere,
                camera, currentVertexProgram, currentPixelProgram);

            //- Multiple texture management for pixel shaders -----------------
            param = CgGL.cgGetNamedParameter(currentPixelProgram, "textureMap");
            CgGL.cgGLSetTextureParameter(param, textureMapList);
            CgGL.cgGLEnableTextureParameter(param);
            if ( quality.isBumpMapSet() ) {
                param = CgGL.cgGetNamedParameter(currentPixelProgram, "normalMap");
                CgGL.cgGLSetTextureParameter(param, normalMapList);
                CgGL.cgGLEnableTextureParameter(param);
            }  
        }
        else {
            JoglLightRenderer.activate(gl, light);
            JoglMaterialRenderer.activate(gl, material);
            if ( quality.isTextureSet() ) {
                gl.glEnable(gl.GL_TEXTURE_2D);
            }
            else {
                gl.glDisable(gl.GL_TEXTURE_2D);
            }
        }

        JoglSphereRenderer.draw(gl, sphere, camera, quality);

        if ( NvidiaGpuActive ) {
            // Disable textures
            param = CgGL.cgGetNamedParameter(currentPixelProgram, "textureMap");
            CgGL.cgGLDisableTextureParameter(param);
            if ( quality.isBumpMapSet() ) {
                param = CgGL.cgGetNamedParameter(currentPixelProgram, "normalMap");
                CgGL.cgGLDisableTextureParameter(param);
            }
            JoglRenderer.disableNvidiaCgProfiles();

            // When using multiple textures... without this fixed function OpenGL
            // gets unconfigured is texture state! More should be researched on
            // this topic!
            if ( quality.isBumpMapSet() ) {
                JoglRenderer.enableNvidiaCgProfiles();
                CgGL.cgGLBindProgram(NvidiaGpuPixelProgramTexture);
                param = CgGL.cgGetNamedParameter(NvidiaGpuPixelProgramTexture, "textureMap");
                CgGL.cgGLEnableTextureParameter(param);
                JoglRenderer.disableNvidiaCgProfiles();
            }
        }
        gl.glLoadIdentity();
        JoglLightRenderer.draw(gl, light);
    }

    void enableTexture(GL gl, boolean withMap) {
        //- Basic OpenGL texture state setup ------------------------------
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

        //-----------------------------------------------------------------
        textureMapList = JoglImageRenderer.activate(gl, textureMap);

        //-----------------------------------------------------------------
        if ( withMap ) {
            //normalMapList = JoglNormalMapRenderer.activate(gl, normalMap);
            normalMapList = JoglImageRenderer.activate(gl, exported);
        }
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
            case 'g': NvidiaGpuActive = !NvidiaGpuActive; break;
          }
          light.setPosition(lightPosition);
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
        CgComplexStandardShaderExample instance = new CgComplexStandardShaderExample(false);

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
