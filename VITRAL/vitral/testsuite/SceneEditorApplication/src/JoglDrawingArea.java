import java.io.File;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
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
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.CameraControllerGravZero;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.io.image.RGBAImageBuilder;
import vsdk.toolkit.render.jogl.JoglSimpleBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCubemapBackgroundRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class JoglDrawingArea implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
    KeyListener
{
    public static final int CAMERA_INTERACTION_MODE = 1;
    public static final int SELECT_INTERACTION_MODE = 2;
    public static final int TRANSLATE_INTERACTION_MODE = 3;
    public static final int ROTATE_INTERACTION_MODE = 4;
    public static final int SCALE_INTERACTION_MODE = 5;

    public GLCanvas canvas;

    private CameraController cameraController;

    private Scene theScene;

    public int interactionMode;

    private Cursor camrotateCursor;
    private Cursor camtranslateCursor;
    private Cursor camadvanceCursor;
    private Cursor selectCursor;

    public JoglDrawingArea(Scene theScene)
    {
        this.theScene = theScene;

        interactionMode = CAMERA_INTERACTION_MODE;

    createCursors();

        //cameraController = new CameraControllerGravZero(theScene.camera);
        //cameraController = new CameraControllerBlender(theScene.camera);
        cameraController = new CameraControllerAquynza(theScene.camera);

        canvas = new GLCanvas();

        Dimension minimumSize = new Dimension(8, 8);
        canvas.setMinimumSize(minimumSize);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
    }

    private void createCursors()
    {
      Toolkit awtToolkit = Toolkit.getDefaultToolkit();
      Image i;

      i = awtToolkit.getImage("./etc/cursors/cursor_camrotate.gif");
      camrotateCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraRotation");

      i = awtToolkit.getImage("./etc/cursors/cursor_camtranslate.gif");
      camtranslateCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraTranslation");

      i = awtToolkit.getImage("./etc/cursors/cursor_camadvance.gif");
      camadvanceCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraAdvance");

      selectCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    }

    public void rotateBackground()
    {
        theScene.selectedBackground++;
        if ( theScene.selectedBackground > 1 ) {
            theScene.selectedBackground = 0;
        }
    }

    public GLCanvas getCanvas()
    {
        return canvas;
    }

    private void drawObjectsGL(GL gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        JoglSceneRenderer.draw(gl, theScene);

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

        switch ( theScene.selectedBackground ) {
          case 1:
            JoglCubemapBackgroundRenderer.draw(gl, theScene.cubemapBackground);
            break;
          case 0: default:
            JoglSimpleBackgroundRenderer.draw(gl, theScene.simpleBackground);
            break;
        }

        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activateGL(gl, theScene.activeCamera);

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

        theScene.activeCamera.updateViewportResize(width, height);
    }   

  public void mouseEntered(MouseEvent e) {
      canvas.requestFocusInWindow();

      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }
  }

  public void mouseExited(MouseEvent e) {
      //System.out.println("Mouse exited");
  }

  public void mousePressed(MouseEvent e) {
      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      int m = e.getModifiersEx();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           (m & e.BUTTON1_DOWN_MASK) != 0 ) {
          canvas.setCursor(camrotateCursor);
      }
      else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                (m & e.BUTTON2_DOWN_MASK) != 0 ) {
          canvas.setCursor(camtranslateCursor);
      }
      else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                (m & e.BUTTON3_DOWN_MASK) != 0 ) {
          canvas.setCursor(camadvanceCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMousePressedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void mouseReleased(MouseEvent e) {
      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseReleasedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void mouseClicked(MouseEvent e) {
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseClickedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void mouseMoved(MouseEvent e) {
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseMovedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void mouseDragged(MouseEvent e) {
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseDraggedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  /**
  WARNING: It is not working... check pending
  */
  public void mouseWheelMoved(MouseWheelEvent e) {
      System.out.println(".");
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseWheelEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void keyPressed(KeyEvent e) {
      if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
          System.exit(0);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processKeyPressedEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void keyReleased(KeyEvent e) {
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processKeyReleasedEventAwt(e) ) {
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

