//= This example shows the use of texturing in JOGL based VSDK applications =
//= applying Jogl2ImageRenderer class methods.                               =

import java.io.File;
import java.io.FileInputStream;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import javax.swing.JFrame;

// VSDK classes

import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.render.jogl.Jogl2ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;

public class ImageExample extends JFrame implements
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    private RGBImage img;
    private boolean closing;
    private boolean glResourcesReleased;

    public ImageExample() {
        super("VITRAL concept test - Image use example");

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
        canvas.addMouseWheelListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestClose();
            }
        });

        camera = new Camera();

        //cameraController = new CameraControllerBlender(camera);
        cameraController = new CameraControllerAquynza(camera);

        corridor = new SimpleCorridor();
        String imageFilename = "../../../etc/images/render.jpg";
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

    public Dimension getPreferredSize() {
        return new Dimension (1024, 768);
    }

    public static void main (String[] args) {
        ImageExample f = new ImageExample();

        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL2 gl)
    {
        // Preparation
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);

        gl.glLoadIdentity();

        // Draw test environment
        corridor.drawGL(gl);

        // Draw reference frame
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

        // Prepare to draw polygon with image.
        // On macOS, drawing back faces in line mode can make this textured
        // quad appear as wireframe depending on current view orientation.
        gl.glDisable(gl.GL_CULL_FACE);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glEnable(gl.GL_TEXTURE_2D);

        // First: activate texture, Second: set texture parameters
        Jogl2ImageRenderer.activate(gl, img);
        //gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
        //gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
        //gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP);
        //gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP);
        //gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_DECAL);

        // Draw textured geometry
        double dx = (double)img.getXSize()/(double)img.getYSize();

        gl.glBegin(gl.GL_QUADS);
            gl.glNormal3d(0, 0, 1);
            gl.glColor3d(1, 1, 1);

            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(0, 0, 0.01);

            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(dx, 0, 0.01);

            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(dx, 1, 0.01);

            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(0, 1, 0.01);
        gl.glEnd();

        // Draw image directly over screen
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        // WARNING: Texture must be disabled in order to maintain original 
        // version, and not be under the influence of glTexParameteri 
        // configurations
        gl.glDisable(gl.GL_TEXTURE_2D);
        Jogl2ImageRenderer.draw(gl, img);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        Jogl2CameraRenderer.activate(gl, camera);

        gl.glDisable(gl.GL_BLEND);
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        drawObjectsGL(gl);
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        if ( !glResourcesReleased ) {
            Jogl2ImageRenderer.unload(gl, img);
            glResourcesReleased = true;
        }

        if ( closing ) {
            System.exit(0);
        }
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
          requestClose();
          return;
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

  private void requestClose() {
      if ( closing ) {
          return;
      }
      closing = true;

      if ( canvas != null ) {
          canvas.destroy();
      }

      if ( !glResourcesReleased ) {
          // Fallback in case dispose was not called.
          System.exit(0);
      }
  }

}
