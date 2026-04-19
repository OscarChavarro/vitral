// Java classes
import java.io.File;

// Java AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;     // Model elements
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.Calligraphic2DBuffer;         // I/O artifacts
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.gui.CameraController;               // Interaction
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.io.geometry.EnvironmentPersistence; // Persistence elements
import vsdk.toolkit.render.WireframeRenderer;           // Processing elements
import vsdk.toolkit.render.jogl.JoglCameraRenderer;     // View elements
import vsdk.toolkit.render.jogl.JoglCalligraphic2DBufferRenderer;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer; 

/**
This example program is the most fundamental interactive computer graphics
example in VitralSDK, showing the independence and modularity from the
underlying rendering architectures. Using the keys '1', '2' and '3', end user
can select three diferent rendering strategies:
  - Fully accelerated 3D using JOGL/OpenGL
  - Software wireframe rendering, using JOGL/OpenGL just as a portable
    2D rasterizer
  - Fully software based wireframe rendering, using Bresenham 2D line
    rasterizer to send 3D projection to an image, still using JOGL/OpenGL
    canvas to show the image.

Current example is based on:
  - WireframeOfflineExample
  - CameraExample
*/
public class WireframeExample extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private SimpleScene scene;

    private CameraController cameraController;
    private GLCanvas canvas;

    private RendererConfiguration qualitySelection;
    private Calligraphic2DBuffer lineSet;
    private RGBImage img;

    private boolean drawJogl;
    private boolean drawWires;
    private boolean drawImage;

    public WireframeExample() {
        //-----------------------------------------------------------------
        super("VITRAL concept test - JOGL Hello World");

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //-----------------------------------------------------------------
        createModel();

        //-----------------------------------------------------------------
        cameraController = new CameraControllerAquynza(camera);
        qualitySelection = new RendererConfiguration();
        qualitySelection.setSurfaces(false);
        //qualitySelection.setShadingType(qualitySelection.SHADING_TYPE_NOLIGHT);
        qualitySelection.setWires(true);

        lineSet = new Calligraphic2DBuffer();
        drawJogl = false;
        drawWires = true;
        drawImage = false;

        img = new RGBImage();

    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        camera = new Camera();
        Matrix4x4 R = new Matrix4x4();

        camera.setPosition(new Vector3D(7, -4, 4));
        R = R.eulerAnglesRotation(Math.toRadians(140), Math.toRadians(-30), 0);
        camera.setNearPlaneDistance(0.001);
        camera.setFarPlaneDistance(100);
        camera.setRotation(R);

        //-----------------------------------------------------------------
        String sceneFile = "../../../etc/geometry/cow.obj";
        scene = new SimpleScene();

        try {
            EnvironmentPersistence.importEnvironment(new File(sceneFile), scene);
        }
        catch ( Exception ex ) {
            System.err.println("Failed to read file");
            ex.printStackTrace();
        }

        //-----------------------------------------------------------------
        SimpleBody b;
        Box box;

        b = new SimpleBody();
        box = new Box(1, 1, 1);
        b.setGeometry(box);
        b.setPosition(new Vector3D(1, 2, 3));
        scene.addBody(b);
    }

    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        JFrame f = new WireframeExample();
        f.pack();
        f.setVisible(true);
    }
    
    private void drawObjectsGL(GL2 gl)
    {
        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        gl.glDisable(gl.GL_TEXTURE_2D);
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

        gl.glColor3d(0.9, 0.5, 0.5);
        if ( drawJogl == true ) {
            int i;
            for ( i = 0; i < scene.getSimpleBodies().size(); i++ ) {
                JoglSimpleBodyRenderer.draw(gl, scene.getSimpleBodies().get(i),
                                            camera, qualitySelection);
            }
        }
        gl.glDisable(gl.GL_TEXTURE_2D);
        //-----------------------------------------------------------------
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        //-----------------------------------------------------------------
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        //-----------------------------------------------------------------
        JoglCameraRenderer.activate(gl, camera);
        drawObjectsGL(gl);

        //-----------------------------------------------------------------
        if ( drawWires || drawImage ) {
            //-----------------------------------------------------------------
            WireframeRenderer.execute(lineSet, scene.getSimpleBodies(), camera);

            //-----------------------------------------------------------------
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glLoadIdentity();

            //-----------------------------------------------------------------
            if ( drawImage ) {
                img.init(img.getXSize(), img.getYSize());
                lineSet.exportRgbImage(img);
                JoglRGBImageRenderer.draw(gl, img);
            }
            else {
                gl.glLineWidth(1.0f);
                gl.glColor3d(1, 1, 1);
                JoglCalligraphic2DBufferRenderer.draw(gl, lineSet);
            }

            lineSet.init();
        }
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
        img.init(width, height);
    }   

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
          System.exit(0);
      }
      if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
          canvas.repaint();
      }
      if ( e.getKeyCode() == KeyEvent.VK_1 ) {
          if ( drawJogl == true ) drawJogl = false; else drawJogl = true;
      }
      else if ( e.getKeyCode() == KeyEvent.VK_2 ) {
          if ( drawWires == true ) drawWires = false; else drawWires = true;
      }
      else if ( e.getKeyCode() == KeyEvent.VK_3 ) {
          if ( drawImage == true ) drawImage = false; else drawImage = true;
      }
      canvas.repaint();
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

//===========================================================================
