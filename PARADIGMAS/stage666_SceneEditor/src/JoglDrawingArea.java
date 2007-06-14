import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

import vitral.toolkits.environment.Camera;
import vitral.framework.gui.CameraController;
import vitral.framework.gui.CameraControllerAquynza;
import vitral.framework.gui.CameraControllerBlender;
import vitral.framework.gui.CameraControllerGravZero;


public class JoglDrawingArea implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
    KeyListener
{
    private SimpleCorridor corridor;
    private CameraController cameraController;
    private Camera activeCamera;
    private GLCanvas canvas;

    public JoglDrawingArea(Camera camera)
    {
        corridor = new SimpleCorridor();
        activeCamera = camera;
    //cameraController = new CameraControllerGravZero(camera);
    //cameraController = new CameraControllerBlender(camera);
        cameraController = new CameraControllerAquynza(camera);

        GLCapabilities capabilities = new GLCapabilities();
        canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);

        Dimension minimumSize = new Dimension(8, 8);
        canvas.setMinimumSize(minimumSize);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
    }

    public GLCanvas getCanvas()
    {
        return canvas;
    }

    private void drawObjectsGL(GL gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        corridor.drawGL(gl);

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
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        vitral.toolkits.visual.jogl.JoglCameraRenderer.activateGL(gl, activeCamera);

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

        activeCamera.updateViewportResize(width, height);
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

