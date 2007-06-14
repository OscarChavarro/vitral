//===========================================================================
//= This example shows the use of texturing in JOGL based VSDK applications =
//= applying JoglImageRenderer class methods.                               =
//===========================================================================

import java.nio.ByteBuffer;
import com.sun.opengl.util.BufferUtil;

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
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

// test classes
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

// VSDK classes

import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class ImageExample extends JFrame implements
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    private RGBImage img;

    private static int checkImageWidth = 128;
    private static int checkImageHeight = 128;
    private static final int color = 3;
    private ByteBuffer checkImageBuf = 
    BufferUtil.newByteBuffer(checkImageHeight * checkImageWidth * color);

    public ImageExample() {
        super("VITRAL concept test - Image use example");

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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

    private void drawObjectsGL(GL gl)
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

        // Prepare to draw polygon with image
        gl.glPolygonMode(gl.GL_FRONT, gl.GL_FILL);
        gl.glPolygonMode(gl.GL_BACK, gl.GL_LINE);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glEnable(gl.GL_TEXTURE_2D);

        // First: activate texture, Second: set texture parameters
        JoglImageRenderer.activate(gl, img);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP);
        gl.glTexParameterf(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP);
        gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_DECAL);

        // Draw textured geometry
        double dx = (double)img.getXSize()/(double)img.getYSize();

        gl.glBegin(gl.GL_QUADS);
            gl.glNormal3d(0, 0, 1);
            gl.glColor3d(1, 1, 1);

            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(0, 0, 0);

            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(dx, 0, 0);

            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(dx, 1, 0);

            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(0, 1, 0);
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
        JoglImageRenderer.draw(gl, img);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        JoglCameraRenderer.activate(gl, camera);

        gl.glDisable(gl.GL_BLEND);
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDepthFunc(gl.GL_LESS);
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
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL gl = drawable.getGL();
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

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
