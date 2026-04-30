//= This example shows the use of texturing in JOGL based VSDK applications =
//= applying Jogl2ImageRenderer class methods.                               =

import java.io.File;
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
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import javax.swing.JFrame;

// VSDK classes

import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBAImageCompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.render.jogl.Jogl2ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;

public class ImageExample extends JFrame implements
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private static final int DEPTH_BUFFER_BITS = 64;
    private static final float IMAGE_DEPTH_BIAS_FACTOR = -1.0f;
    private static final float IMAGE_DEPTH_BIAS_UNITS = -8.0f;

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    private Image renderImage;
    private Image earthImage;
    private boolean closing;
    private boolean glResourcesReleased;

    public ImageExample() {
        super("VITRAL concept test - Image use example");

        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(DEPTH_BUFFER_BITS);
        canvas = new GLCanvas(caps);

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

        cameraController = new CameraControllerAquynza(camera);

        corridor = new SimpleCorridor();
        renderImage = loadImage("../../../etc/images/render.jpg");
        earthImage = loadImage("../../../etc/textures/earth.dds");
    }

    private Image loadImage(String imageFilename)
    {
        try {
            return ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }
        return null;
    }

    public Dimension getPreferredSize() {
        return new Dimension (1024, 768);
    }

    public static void main (String[] args) {
        ImageExample f = new ImageExample();

        f.pack();
        f.setVisible(true);
    }

    private void drawReferenceFrame(GL2 gl)
    {
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
    }

    private void drawTexturedPolygon(
        GL2 gl,
        Image image,
        double x0,
        double y0,
        double width,
        double height)
    {
        int textureId = Jogl2ImageRenderer.activate(gl, image);
        if ( textureId <= 0 ) {
            return;
        }

        double x1 = x0 + width;
        double y1 = y0 + height;

        gl.glDisable(gl.GL_CULL_FACE);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
        gl.glShadeModel(gl.GL_FLAT);
        gl.glEnable(gl.GL_TEXTURE_2D);

        gl.glBegin(gl.GL_QUADS);
            gl.glNormal3d(0, 0, 1);
            gl.glColor3d(1, 1, 1);

            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(x0, y0, 0.01);

            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(x1, y0, 0.01);

            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(x1, y1, 0.01);

            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(x0, y1, 0.01);
        gl.glEnd();
    }

    private void drawWorldImages(GL2 gl)
    {
        double renderWidth = (double)renderImage.getXSize() / (double)renderImage.getYSize();

        drawTexturedPolygon(gl, renderImage, 0.0, 0.0, renderWidth, 1.0);
        drawTexturedPolygon(gl, earthImage, 0.0, -1.0, 1.0, 1.0);
    }

    private void drawWorldImagesDepthBiased(GL2 gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDepthFunc(gl.GL_LEQUAL);
        gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(IMAGE_DEPTH_BIAS_FACTOR, IMAGE_DEPTH_BIAS_UNITS);

        drawWorldImages(gl);

        gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
        gl.glDepthFunc(gl.GL_LESS);
    }

    private void drawHudImage(GL2 gl, Image image, boolean upperLeft)
    {
        int textureId = Jogl2ImageRenderer.activate(gl, image);
        if ( textureId <= 0 ) {
            return;
        }

        int[] viewport = new int[4];
        gl.glGetIntegerv(gl.GL_VIEWPORT, viewport, 0);
        int viewportWidth = Math.max(viewport[2], 1);
        int viewportHeight = Math.max(viewport[3], 1);

        double width = 2.0 * ((double)image.getXSize() / (double)viewportWidth);
        double height = 2.0 * ((double)image.getYSize() / (double)viewportHeight);

        double x0 = -1.0;
        double y0 = upperLeft ? 1.0 - height : -1.0;
        double x1 = x0 + width;
        double y1 = y0 + height;

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glDisable(gl.GL_CULL_FACE);
        gl.glEnable(gl.GL_TEXTURE_2D);
        if ( image instanceof RGBAImageUncompressed ||
             image instanceof RGBAImageCompressed ) {
            gl.glEnable(gl.GL_BLEND);
            gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
        }

        gl.glBegin(gl.GL_QUADS);
            gl.glColor3d(1, 1, 1);
            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(x0, y0, 0.0);
            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(x1, y0, 0.0);
            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(x1, y1, 0.0);
            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(x0, y1, 0.0);
        gl.glEnd();

        if ( image instanceof RGBAImageUncompressed ||
             image instanceof RGBAImageCompressed ) {
            gl.glDisable(gl.GL_BLEND);
        }
    }

    private void drawHud(GL2 gl)
    {
        gl.glDisable(gl.GL_DEPTH_TEST);
        drawHudImage(gl, renderImage, false);
        drawHudImage(gl, earthImage, true);
        gl.glEnable(gl.GL_DEPTH_TEST);
    }

    private void drawObjectsGL(GL2 gl)
    {
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
        gl.glLoadIdentity();

        corridor.drawGL(gl);
        drawReferenceFrame(gl);
        drawWorldImagesDepthBiased(gl);
        drawHud(gl);
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
            Jogl2ImageRenderer.unload(gl, renderImage);
            Jogl2ImageRenderer.unload(gl, earthImage);
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
