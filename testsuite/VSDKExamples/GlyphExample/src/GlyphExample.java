// Basic JDK classes
import java.util.ArrayList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

// AWT classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;

// Swing classes
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.render.awt.AwtFontReader;
import vsdk.toolkit.render.jogl.JoglParametricCurveRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class GlyphExample extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private ArrayList<ParametricCurve> glyphs;
    private int steps;

    public GlyphExample(String fontFile) {
        super("VITRAL concept test - JOGL Hello World");

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        camera.setPosition(new Vector3D(0, 0, 5));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-90), 0);
        camera.setRotation(R);
        camera.setFov(20);

        cameraController = new CameraControllerAquynza(camera);

        steps = 7;

        //-----------------------------------------------------------------
        AwtFontReader fontReader = new AwtFontReader();
        //String msg = "\u00e1\u00d1\u3055\u3042\u307d"; // ennie: \u00d1
        String msg = "Hello!";
        //String msg = "\u7c8b";

        glyphs = new ArrayList<ParametricCurve>();

        for ( int i = 0; i < msg.length(); i++ ) {
            String character = msg.substring(i, i+1);
            ParametricCurve curve;
            curve = fontReader.extractGlyph(fontFile, character);
            curve.setApproximationSteps(steps);
            glyphs.add(curve);
        }
        //-----------------------------------------------------------------
    }

    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }

    public static void main (String[] args) {
        if ( args.length != 1 ) {
            System.err.println("Use java GlyphExample fontFile");
            return;
        }

        JFrame f = new GlyphExample(args[0]);
        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL2 gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        gl.glLineWidth((float)1.0);
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

        //-----------------------------------------------------------------
        RendererConfiguration q = new RendererConfiguration();
        q.setBoundingVolume(true);
        double minmax[] = new double[6];

        gl.glPushMatrix();
        for ( int i = 0; i < glyphs.size(); i++ ) {
            ParametricCurve c = glyphs.get(i);

            gl.glLineWidth((float)2.0);
            JoglParametricCurveRenderer.draw(gl, c, camera, 
                q, new ColorRgb(1, 1, 1));

            gl.glColor3d(0.5, 0.5, 0.9);
            JoglParametricCurveRenderer.drawTesselatedCurveInterior(gl, c);

            minmax = c.getMinMax();
            gl.glTranslated(minmax[3]-minmax[0]+0.1, 0, 0);

        }
        gl.glPopMatrix();
        //-----------------------------------------------------------------
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);

        drawObjectsGL(gl);
    }
   
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
      else if ( e.getKeyCode() == KeyEvent.VK_1 ) {
          steps--;
          if ( steps < 1 ) steps = 1;
          System.out.println("Steps: " + steps);

          for ( int i = 0; i < glyphs.size(); i++ ) {
              glyphs.get(i).setApproximationSteps(steps);
          }

          canvas.repaint();
      }
      else if ( e.getKeyCode() == KeyEvent.VK_2 ) {
          steps++;
          System.out.println("Steps: " + steps);

          for ( int i = 0; i < glyphs.size(); i++ ) {
              glyphs.get(i).setApproximationSteps(steps);
          }

          canvas.repaint();
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

}
