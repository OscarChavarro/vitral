//===========================================================================

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
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.AffineTransform;

import javax.swing.JFrame;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.CameraControllerGravZero;
import vsdk.toolkit.render.jogl.JoglParametricCurveRenderer;

public class GlyphExample extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private ParametricCurve curve;
    private Font fuente;

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

        //-----------------------------------------------------------------
        try {
            fuente = Font.createFont(Font.TRUETYPE_FONT, 
                                     new File(fontFile));
        }
        catch ( Exception e ) {
            System.err.println("Error loading font file " + fontFile);
            return;
        }

        System.out.println("---- Fuente con " + fuente.getNumGlyphs() + " letras ----");

        //-----------------------------------------------------------------
        Vector3D pointParameters[];

        curve = new ParametricCurve();

        //- Analisis de glyphs ---------------------------------------
        AffineTransform a = new AffineTransform();

        FontRenderContext frc = new FontRenderContext(a, true, true);
        GlyphVector gv = fuente.createGlyphVector(frc, "k");

        GeneralPath p = (GeneralPath)gv.getGlyphOutline(0);

        boolean endIt = false;
        int code = 0;

        for ( PathIterator pi = p.getPathIterator(a);
              !pi.isDone(); pi.next() ) {
            double coords[] = new double[6];
            int type = pi.currentSegment(coords);
            String msg = "";

            code = 0;
            switch ( type ) {
              case PathIterator.SEG_CUBICTO:
                msg = msg + "SEG_CUBICTO";
                break;
              case PathIterator.SEG_LINETO:
                msg = msg + "SEG_LINETO";
                code = 1;
                break;
              case PathIterator.SEG_MOVETO:
                msg = msg + "SEG_MOVETO";
                code = 0;
                break;
              case PathIterator.SEG_QUADTO:
                msg = msg + "SEG_QUADTO";
                code = 2;
                break;
              case PathIterator.SEG_CLOSE:
                msg = msg + "SEG_CLOSE";
                code = 3;
                break;
              default: System.out.print("UNKNOWN"); break;
            }

            if ( !endIt ) {
                switch ( code ) {
          case 0:
                    curve.addPoint(null, curve.BREAK);

                    pointParameters = new Vector3D[1];
                    pointParameters[0] = 
                        new Vector3D(coords[0], -coords[1], 0);
                    curve.addPoint(pointParameters, curve.CORNER);
/*
                    System.out.println(msg + "(POINT) <" + coords[0] +
                               ", " + coords[1] + ">");
*/
            break;
          case 1:
                    pointParameters = new Vector3D[1];
                    pointParameters[0] = new Vector3D(coords[0], -coords[1], 0);
                    curve.addPoint(pointParameters, curve.CORNER);
/*
                    System.out.println(msg + "(POINT) <" + coords[0] +
                               ", " + coords[1] + ">");
*/
            break;
          case 2:
                    pointParameters = new Vector3D[2];
                    // Note the inverse order of awt with respect to VSDK!
                    pointParameters[0] = new Vector3D(coords[2], -coords[3], 0);
                    pointParameters[1] = new Vector3D(coords[0], -coords[1], 0);
                    curve.addPoint(pointParameters, curve.QUAD);
/*
                    System.out.println(msg + "(QUAD) <" + coords[0] +
                               ", " + coords[1] + "> / <" + coords[2] +
                               ", " + coords[3] + ">");
*/
            break;
          case 3:
                    //endIt = true;
            break;
          default:
/*
                    System.out.println(msg + " <" + coords[0] +
                               ", " + coords[1] + "> / <" + coords[2] +
                               ", " + coords[3] + "> / <" + coords[4] +
                               ", " + coords[5] + ">");
*/
                break;
        }
        }

        }

        System.out.println("Curve with " + curve.getNumPieces() + " pieces");

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

    private void drawObjectsGL(GL gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        gl.glLineWidth((float)1.0);

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

        gl.glLineWidth((float)2.0);
        JoglParametricCurveRenderer.draw(gl, curve, camera, new QualitySelection(), new ColorRgb(1, 1, 1));

    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        vsdk.toolkit.render.jogl.JoglCameraRenderer.activate(gl, camera);

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
