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
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import java.applet.Applet;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidRenderer;

public class PolyhedralBoundedSolidExample extends Applet implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private PolyhedralBoundedSolid solid;
    private int faceIndex = 0;

    private RendererConfiguration quality;
    private CameraController cameraController;
    private GLCanvas canvas;

    public PolyhedralBoundedSolidExample() {
        camera = new Camera();
        camera.setPosition(new Vector3D(0.5, -1, 2));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-45), 0);
        camera.setRotation(R);

        quality = new RendererConfiguration();
        cameraController = new CameraControllerAquynza(camera);

        //- Cube building -------------------------------------------------
        solid = new PolyhedralBoundedSolid();
    solid.mvfs(new Vector3D(0.1, 0.1, 0.1), 1, 1);
        solid.mev(1, 1, 1, 1, 1, 4, new Vector3D(0.1, 1, 0.1));
        solid.mev(1, 1, 4, 1, 1, 3, new Vector3D(1, 1, 0.1));
        solid.mev(1, 1, 3, 4, 4, 2, new Vector3D(1, 0.1, 0.1));
        solid.mef(1, 1, 1, 4, 2, 3, /*NewFaceId*/ 2);
        solid.mev(1, 1, 1, 2, 2, 5, new Vector3D(0.1, 0.1, 1));
        solid.mev(1, 1, 2, 3, 3, 6, new Vector3D(1, 0.1, 1));
        solid.mef(1, 1, 5, 1, 6, 2, /*NewFaceId*/ 3);
        solid.mev(1, 1, 3, 4, 4, 7, new Vector3D(1, 1, 1));
        solid.mef(1, 1, 6, 2, 7, 3, /*NewFaceId*/ 4);
        solid.mev(1, 1, 4, 1, 1, 8, new Vector3D(0.1, 1, 1));
        solid.mef(1, 1, 7, 3, 8, 4, /*NewFaceId*/ 5);
        solid.mef(1, 1, 5, 6, 8, 4, /*NewFaceId*/ 6);

        //- Cube modification to holed box --------------------------------
        solid.mev(6, 6, 5, 6, 6, 9, new Vector3D(0.3, 0.3, 1));
        solid.kemr(6, 6, 5, 9, 9, 5);
        solid.mev(6, 6, 9, 9, 9, 10, new Vector3D(0.8, 0.3, 1));
        solid.mev(6, 6, 10, 9, 9, 11, new Vector3D(0.8, 0.8, 1));
        solid.mev(6, 6, 11, 10, 10, 12, new Vector3D(0.3, 0.8, 1));
        solid.mef(6, 6, 9, 10, 12, 11, /*NewFaceId*/ 7);

        //- Box extrusion -------------------------------------------------
        solid.mev(7, 7, 9, 10, 10, 13, new Vector3D(0.3, 0.3, 0.1));
        solid.mev(7, 7, 10, 11, 11, 14, new Vector3D(0.8, 0.3, 0.1));
        solid.mef(7, 7, 13, 9, 14, 10, /*NewFaceId*/ 8);
        solid.mev(7, 7, 11, 12, 12, 15, new Vector3D(0.8, 0.8, 0.1));
        solid.mef(7, 7, 14, 10, 15, 11, /*NewFaceId*/ 9);
        solid.mev(7, 7, 12, 9, 9, 16, new Vector3D(0.3, 0.8, 0.1));
        solid.mef(7, 7, 15, 11, 16, 12, /*NewFaceId*/ 10);
        solid.mef(7, 7, 13, 14, 16, 12, /*NewFaceId*/ 11);

        //- Topology joining from 0-genus to 1-genus ----------------------

        //-----------------------------------------------------------------
    }

    private GLCanvas createGUI()
    {
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        return canvas;
    }

    public static void main (String[] args) {
        JoglRenderer.verifyOpenGLAvailability();
        PolyhedralBoundedSolidExample instance = new PolyhedralBoundedSolidExample();
        JFrame frame = new JFrame("VITRAL concept test - Polyhedral bounded solid example");

        GLCanvas canvas = instance.createGUI();

        frame.add(canvas, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension size = new Dimension(640, 480);
        frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        canvas.requestFocusInWindow();
    }

    public void init()
    {
        setLayout(new BorderLayout());
        add("Center", createGUI());
    }
    
    private void drawObjectsGL(GL gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

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

        JoglPolyhedralBoundedSolidRenderer.draw(gl, solid, camera, quality, faceIndex);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(1, 1, 1, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);

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
      int unicode_id = e.getKeyChar();
      if ( unicode_id != e.CHAR_UNDEFINED ) {
          switch ( unicode_id ) {
            case '1': faceIndex --; break;
            case '2': faceIndex ++; break;
            case 'I': System.out.println(solid); break;
          }
          if ( faceIndex < 0 ) faceIndex = 0;
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
