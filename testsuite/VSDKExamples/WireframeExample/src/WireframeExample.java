//===========================================================================
import java.util.Iterator;
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

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Vector4D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.media.Calligraphic2DBuffer;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.io.geometry.ReaderObj;
import vsdk.toolkit.render.jogl.JoglTriangleMeshGroupRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglCalligraphic2DBufferRenderer;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

public class WireframeExample extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;

    private TriangleMeshGroup meshGroup;

    private RendererConfiguration qualitySelection;
    private Calligraphic2DBuffer lineSet;
    private RGBImage img;

    private boolean drawJogl;
    private boolean drawWires;
    private boolean drawImage;

    public WireframeExample() {
        //-----------------------------------------------------------------
        super("VITRAL concept test - JOGL Hello World");

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        camera.setPosition(new Vector3D(7, -4, 4));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(140),
                              Math.toRadians(-30), 0);
        camera.setRotation(R);

        cameraController = new CameraControllerAquynza(camera);
        qualitySelection = new RendererConfiguration();
        qualitySelection.setSurfaces(false);
        //qualitySelection.setShadingType(qualitySelection.SHADING_TYPE_NOLIGHT);
        qualitySelection.setWires(true);

        lineSet = new Calligraphic2DBuffer();
        drawJogl = false;
        drawWires = true;
        drawImage = false;

        img = new RGBImage();

        //-----------------------------------------------------------------
        try {
            meshGroup = ReaderObj.read("../../../etc/geometry/cow.obj");
        }
        catch (Exception ex) {
            System.err.println("Failed to read file");
            System.exit(0);
            return;
        }

    }

    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        JFrame f = new WireframeExample();
        f.pack();
        f.setVisible(true);
    }
    
    private void drawObjectsGL(GL gl)
    {
        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        gl.glDisable(gl.GL_TEXTURE_2D);
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

        gl.glColor3d(0.9, 0.5, 0.5);
        if ( drawJogl == true ) {
          JoglTriangleMeshGroupRenderer.draw(gl, meshGroup, qualitySelection);
        }
        gl.glDisable(gl.GL_TEXTURE_2D);
        //-----------------------------------------------------------------
    }

    private void addLine(Calligraphic2DBuffer lineSet,
                         Vector3D cp0, Vector3D cp1, Matrix4x4 R)
    {
        Vector4D hp0, hp1; // Clipped points in homogeneous space
        Vector4D pp0, pp1; // Projected points

        hp0 = new Vector4D(cp0);
        hp1 = new Vector4D(cp1);
        pp0 = R.multiply(hp0);
        pp0.divideByW();
        pp1 = R.multiply(hp1);
        pp1.divideByW();
        lineSet.add2DLine(pp0.x, pp0.y, pp1.x, pp1.y);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        //-----------------------------------------------------------------
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);

        //-----------------------------------------------------------------
        drawObjectsGL(gl);

        //-----------------------------------------------------------------
        if ( drawWires || drawImage ) {
            TriangleMesh mesh = null;
            Iterator<TriangleMesh> i;
            Vertex[] arrVertexes;
            Triangle[] arrTriangles;
            int t;
            int p0, p1, p2;
            Vector3D mp0, mp1; // Mesh points
            Vector3D cp0, cp1; // Clipped points
            Matrix4x4 R;

            cp0 = new Vector3D();
            cp1 = new Vector3D();
            R = camera.calculateProjectionMatrix(camera.STEREO_MODE_CENTER);

            for ( i = meshGroup.getMeshes().iterator(); i.hasNext(); ) {
                mesh = (TriangleMesh)i.next();
                arrVertexes = mesh.getVertexes();
                arrTriangles = mesh.getTriangles();
                for ( t = 0; t < arrTriangles.length; t++ ) {
                    p0 = arrTriangles[t].p0;
                    p1 = arrTriangles[t].p1;
                    p2 = arrTriangles[t].p2;

                    mp0 = arrVertexes[p0].position;
                    mp1 = arrVertexes[p1].position;
                    if ( camera.clipLineCohenSutherland(mp0, mp1, cp0, cp1) ) {
                        addLine(lineSet, cp0, cp1, R);
                    }

                    mp0 = arrVertexes[p1].position;
                    mp1 = arrVertexes[p2].position;
                    if ( camera.clipLineCohenSutherland(mp0, mp1, cp0, cp1) ) {
                        addLine(lineSet, cp0, cp1, R);
                    }

                    mp0 = arrVertexes[p2].position;
                    mp1 = arrVertexes[p0].position;
                    if ( camera.clipLineCohenSutherland(mp0, mp1, cp0, cp1) ) {
                        addLine(lineSet, cp0, cp1, R);
                    }
                }
            }

            //-----------------------------------------------------------------
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glLoadIdentity();

            if ( drawImage ) {
                double xt = camera.getViewportXSize();
                double yt = camera.getViewportYSize();

                Vector3D e0 = new Vector3D();
                Vector3D e1 = new Vector3D();
                int x0, y0, x1, y1;
                RGBPixel pixel = new RGBPixel();

                img.init((int)xt, (int)yt);
                pixel.r = (byte)255;
                pixel.g = 0;
                pixel.b = 0;

                for ( int j = 0; j < lineSet.getNumLines(); j++ ) {
                    lineSet.get2DLine(j, e0, e1);
                    x0 = (int)((xt-1)*((e0.x+1)/2));
                    y0 = (int)((yt-1)*(1-((e0.y+1)/2)));
                    x1 = (int)((xt-1)*((e1.x+1)/2));
                    y1 = (int)((yt-1)*(1-((e1.y+1)/2)));
                    img.drawLine(x0, y0, x1, y1, pixel);
                }
                JoglRGBImageRenderer.draw(gl, img);
            }
            else {
                gl.glLineWidth(1.0f);
                gl.glColor3d(1, 1, 1);
                JoglCalligraphic2DBufferRenderer.draw(gl, lineSet);
            }

            lineSet.init();
        }
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
      if ( e.getKeyCode() == KeyEvent.VK_1 ) {
          if ( drawJogl == true ) drawJogl = false; else drawJogl = true;
      }
      else if ( e.getKeyCode() == KeyEvent.VK_2 ) {
          if ( drawWires == true ) drawWires = false; else drawWires = true;
      }
      else if ( e.getKeyCode() == KeyEvent.VK_3 ) {
          if ( drawImage == true ) drawImage = false; else drawImage = true;
      }
      canvas.repaint();
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
