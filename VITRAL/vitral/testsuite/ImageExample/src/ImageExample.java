// VITRAL recomendation: Use explicit class imports (not .*) in hello world type programs
// so the user/programmer can be exposed to all the complexity involved. This will help him
// to dominate the involved libraries.

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
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.RGBAImageBuilder;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.CameraControllerGravZero;
import vsdk.toolkit.render.jogl.JoglRGBAImageRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class ImageExample extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    private RGBAImage img;

    private int machete = 0;

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

        //cameraController = new CameraControllerGravZero(camera);
        //cameraController = new CameraControllerBlender(camera);
        cameraController = new CameraControllerAquynza(camera);

        corridor = new SimpleCorridor();
        try {
            img = RGBAImageBuilder.buildImage(new File("./etc/render.jpg"));
        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(0);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension (1024, 768);
    }
    
    public static void main (String[] args) {
        JFrame f = new ImageExample();
        f.pack();
        f.setVisible(true);
    }
    
    private void drawObjectsGL(GL gl)
    {
        // Preparation
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glLoadIdentity();

        // Draw test environment
        corridor.drawGL(gl);

        // Draw reference frame
        gl.glLineWidth((float)3.0);
        gl.glBegin(GL.GL_LINES);
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

        // Draw polygon with image
        gl.glEnable(gl.GL_TEXTURE_2D);
        JoglRGBAImageRenderer.activateGL(gl, img);

        // WARNING: It is not supposed to be, but this code requires to call this in THIS place
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);

        double dx = (double)img.getXSize()/(double)img.getYSize();

        gl.glBegin(GL.GL_QUADS);
            gl.glNormal3d(0, 0, 1);
            gl.glColor3d(1, 1, 1);

            gl.glTexCoord2f(0, 0);
            gl.glVertex3d(0, 0, 0);

            gl.glTexCoord2f(1, 0);
            gl.glVertex3d(dx, 0, 0);

            gl.glTexCoord2f(1, 1);
            gl.glVertex3d(dx, 1, 0);

            gl.glTexCoord2f(0, 1);
            gl.glVertex3d(0, 1, 0);            
        gl.glEnd();

        // Draw image directly over screen
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glDisable(gl.GL_DEPTH_TEST);

        // WARNING: Texture must be disabled in order to maintain original version, and not be
        // under the influence of glTexParameteri configurations
        gl.glDisable(gl.GL_TEXTURE_2D);
        JoglRGBAImageRenderer.draw(gl, img);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activateGL(gl, camera);

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
