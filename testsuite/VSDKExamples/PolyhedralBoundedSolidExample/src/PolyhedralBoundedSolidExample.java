//===========================================================================

// Java basic classes
import java.util.ArrayList;

// Java Swing / Awt classes
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.render.HiddenLineRenderer;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidRenderer;
import vsdk.toolkit.processing.GeometricModeler;

public class PolyhedralBoundedSolidExample extends Applet implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private Material material;
    private Light light1;
    private Light light2;
    private PolyhedralBoundedSolid solid;
    private int faceIndex = -2;
    private int edgeIndex = -2;

    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private CameraController cameraController;
    private GLCanvas canvas;
    private int solidType = 19;
    private int csgOperation = 0;
    private int csgSample = 5;
    private boolean debugEdges = false;
    private boolean showCoordinateSystem = true;

    public PolyhedralBoundedSolidExample() {
        camera = new Camera();
        camera.setPosition(new Vector3D(2, -1, 2));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(135), Math.toRadians(-35), 0);
        camera.setRotation(R);

        quality = new RendererConfiguration();
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerAquynza(camera);

        material = defaultMaterial();
        light1 = new Light(Light.POINT, new Vector3D(3, -3, 2), new ColorRgb(1, 1, 1));
        light2 = new Light(Light.POINT, new Vector3D(-2, 5, -2), new ColorRgb(0.9, 0.5, 0.5));

        //- Solid building ------------------------------------------------
        solid = buildSolid(solidType);
        //- Topology joining from 0-genus to 1-genus ----------------------

        //-----------------------------------------------------------------
    }

    private Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }

    private PolyhedralBoundedSolid buildSolid(int type)
    {
        PolyhedralBoundedSolid solid = null;
        Matrix4x4 T, R, S, M;

        switch ( type % 23 ) {
          case 0:
            solid = new PolyhedralBoundedSolid();
            solid.mvfs(new Vector3D(0.1, 0.1, 0.1), 1, 1);
            solid.smev(1, 1, 4, new Vector3D(0.1, 1, 0.1));
            solid.smev(1, 4, 3, new Vector3D(1, 1, 0.1));
            break;
          case 1:
            solid = PolyhedralBoundedSolidModelingTools.createBox(new Vector3D(0.9, 0.9, 0.9));
            break;
          case 3:
            solid = new PolyhedralBoundedSolid();
            solid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            GeometricModeler.addArc(
                solid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 9);
            break;
          case 4:
            solid = GeometricModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 12
            );
            break;
          case 5:
            solid = new PolyhedralBoundedSolid();
            solid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            GeometricModeler.addArc(
                solid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 18);

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));

            GeometricModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), M);

            break;
          case 6:
            solid = GeometricModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 24
            );

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));
            GeometricModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), M);

/*
            T = new Matrix4x4();
            T.translation(0.1, 0.1, 1.0);
            R = new Matrix4x4();
            //R.axisRotation(Math.toRadians(15), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.2, 0.2, 0.2);
            M = T.multiply(R.multiply(S));
            GeometricModeler.translationalSweepExtrudeFace(
                solid, solid.findFace(1), M);
*/


            break;

          case 7:
            solid = PolyhedralBoundedSolidModelingTools.createSphere(0.5);
            break;
          case 8:
            solid = PolyhedralBoundedSolidModelingTools.createCone(0.5, 0.0, 1.0);
            break;
          case 9:
            solid = PolyhedralBoundedSolidModelingTools.createArrow(0.7, 0.3, 0.05, 0.1);
            break;
          case 10:
            solid = PolyhedralBoundedSolidModelingTools.createLaminaWithTwoShells();
            break;
          case 11:
            solid = PolyhedralBoundedSolidModelingTools.createLaminaWithHole();
            break;
          case 12:
            solid = PolyhedralBoundedSolidModelingTools.createFontBlock("../../../../samples/fonts/microsoftArial.ttf", "\u7c8b\u00e1\u00d1\u3055\u3042\u307d");

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.1);

            GeometricModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), T);

            break;
          case 13:
            solid = PolyhedralBoundedSolidModelingTools.createGluedCilinders();
            break;
          case 14:
            solid = PolyhedralBoundedSolidModelingTools.eulerOperatorsTest();
            break;
          case 15:
            solid = PolyhedralBoundedSolidModelingTools.rotationalSweepTest();
            break;
          case 16:
            solid = PolyhedralBoundedSolidModelingTools.splitTest(1);
            break;
          case 17:
            solid = PolyhedralBoundedSolidModelingTools.splitTest(2);
            break;
          case 18:
            solid = PolyhedralBoundedSolidModelingTools.splitTest(3);
            break;
          case 19:
            solid = PolyhedralBoundedSolidModelingTools.csgTest(1, csgOperation, csgSample);
            break;
          case 20:
            solid = PolyhedralBoundedSolidModelingTools.csgTest(2, csgOperation, csgSample);
            break;
          case 21:
            solid = PolyhedralBoundedSolidModelingTools.csgTest(3, csgOperation, csgSample);
            break;
          case 22:
            solid = PolyhedralBoundedSolidModelingTools.featuredObject();
            break;
          case 2: default:
            solid = PolyhedralBoundedSolidModelingTools.createHoledBox();
            break;
        }

        return solid;
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
        //frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        canvas.requestFocusInWindow();
    }

    public void init()
    {
        setLayout(new BorderLayout());
        add("Center", createGUI());
    }
    
    private void
    renderLinesResult(GL gl, ArrayList <Vector3D> contourLines,
                      ArrayList <Vector3D> visibleLines,
                      ArrayList <Vector3D> hiddenLines)
    {
        int i;
        Vector3D p;

        gl.glPushAttrib(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        gl.glLineWidth(4.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(gl.GL_LINES);
        for ( i = 0; i < contourLines.size(); i++ ) {
            p = contourLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();

        gl.glLineWidth(4.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(gl.GL_LINES);
        for ( i = 0; i < visibleLines.size(); i++ ) {
            p = visibleLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();

/*
        gl.glLineWidth(1.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(gl.GL_LINES);
        for ( i = 0; i < hiddenLines.size(); i++ ) {
            p = hiddenLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();
*/
        //-----------------------------------------------------------------
/*
        gl.glPointSize(4.0f);
        gl.glColor3d(0.5, 0.5, 0.9);
        gl.glBegin(gl.GL_POINTS);
        for ( i = 0; i < contourLines.size(); i++ ) {
            p = contourLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        for ( i = 0; i < visibleLines.size(); i++ ) {
            p = visibleLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();
*/
        //-----------------------------------------------------------------
        gl.glPopAttrib();
    }

    private void drawObjectsGL(GL gl)
    {
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        gl.glDisable(gl.GL_LIGHTING);
        gl.glLineWidth((float)3.0);

        if ( edgeIndex > -3 && showCoordinateSystem ) {
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

        //-----------------------------------------------------------------
        JoglMaterialRenderer.activate(gl, material);
        JoglLightRenderer.activate(gl, light1);
        JoglLightRenderer.draw(gl, light1);
        JoglLightRenderer.activate(gl, light2);
        JoglLightRenderer.draw(gl, light2);
        gl.glEnable(gl.GL_LIGHTING);
        JoglPolyhedralBoundedSolidRenderer.draw(gl, solid, camera, quality);
        JoglPolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl, solid, faceIndex);
        JoglPolyhedralBoundedSolidRenderer.drawDebugFace(gl, solid, faceIndex);

        //-----------------------------------------------------------------
        ArrayList <Vector3D> contourLines;
        ArrayList <Vector3D> visibleLines;
        ArrayList <Vector3D> hiddenLines;
        ArrayList <SimpleBody> bodyArray;
        SimpleBody body;

        if ( debugEdges && edgeIndex > -3 ) {
            JoglPolyhedralBoundedSolidRenderer.drawDebugEdges(gl, solid, camera, edgeIndex);
        }
        else if ( edgeIndex == -3 ) {
            contourLines = new ArrayList <Vector3D>();
            visibleLines = new ArrayList <Vector3D>();
            hiddenLines = new ArrayList <Vector3D>();
            bodyArray = new ArrayList <SimpleBody>();

            body = new SimpleBody();
            body.setGeometry(solid);
            body.setPosition(new Vector3D());
            body.setRotation(new Matrix4x4());
            body.setRotationInverse(new Matrix4x4());
            bodyArray.add(body);
            HiddenLineRenderer.executeAppelAlgorithm(bodyArray, camera,
                contourLines, visibleLines, hiddenLines);
            renderLinesResult(gl, contourLines, visibleLines, hiddenLines);
        }

        contourLines = null;
        visibleLines = null;
        hiddenLines = null;
        bodyArray = null;
        body = null;
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        gl.glEnable(gl.GL_DEPTH_TEST);

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
      if ( qualityController.processKeyPressedEventAwt(e) ) {
          System.out.println(quality);
          canvas.repaint();
      }

      int unicode_id = e.getKeyChar();
      if ( unicode_id != e.CHAR_UNDEFINED ) {
          switch ( unicode_id ) {
            case '0':
                if ( debugEdges ) {
                    debugEdges = false;
                }
                else {
                    debugEdges = true;
                }
                break;
            case ' ':
                if ( showCoordinateSystem ) {
                    showCoordinateSystem = false;
                }
                else {
                    showCoordinateSystem = true;
                }
                break;
            case '1': faceIndex --; break;
            case '2': faceIndex ++; break;
            case '8': edgeIndex --; break;
            case '9': edgeIndex ++; break;
            case 'I':
                System.out.println(solid);
                if ( solid.validateModel() ) {
                    System.out.println("SOLID MODEL IS VALID!");
                }
                else {
                    System.out.println("SOLID MODEL IS INVALID!");
                }
                break;

            case '3':
              solidType--;
              if ( solidType < 0 ) solidType = 0;
              solid = buildSolid(solidType);
              break;

            case '4':
              solidType++;
              solid = buildSolid(solidType);
              break;

            case '5':
              csgOperation++;
              if ( csgOperation > 3 ) csgOperation = 0;
              solid = buildSolid(solidType);
              break;

            case '6':
              csgSample++;
              if ( csgSample > 5 ) csgSample = 0;
              solid = buildSolid(solidType);
              break;

          }
          if ( faceIndex < -2 ) faceIndex = -2;
          if ( edgeIndex < -3 ) edgeIndex = -3;
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
