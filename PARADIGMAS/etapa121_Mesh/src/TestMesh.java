import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import net.java.games.jogl.GL;
import net.java.games.jogl.GLU;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLCapabilities;
import net.java.games.jogl.GLDrawable;
import net.java.games.jogl.GLDrawableFactory;
import net.java.games.jogl.GLEventListener;
import vitral.toolkits.environment.Camera;
import vitral.framework.gui.CameraController;
import vitral.framework.gui.CameraControllerAquynza;
import vitral.toolkits.geometry.Mesh;
import vitral.toolkits.common.Triangle;
import vitral.toolkits.common.Vertex;
import vitral.toolkits.common.Vector3D;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class TestMesh
    extends Frame implements GLEventListener, MouseListener,
    MouseMotionListener, MouseWheelListener, KeyListener {

  private Camera camera;
  private CameraController cameraController;
  private GLCanvas canvas;
  private Mesh mesh;

  public TestMesh() {
    super("VITRAL mesh test - JOGL");
    mesh = new Mesh();
    mesh.addVertex(new Vertex(new Vector3D( -1.0, -1.0, -1.0)));
    mesh.addVertex(new Vertex(new Vector3D( -1.0, -1.0, 1.0)));
    mesh.addVertex(new Vertex(new Vector3D(1.0, -1.0, -1.0)));
    mesh.addVertex(new Vertex(new Vector3D(1.0, -1.0, 1.0)));

    mesh.addVertex(new Vertex(new Vector3D(1.0, 1.0, -1.0)));
    mesh.addVertex(new Vertex(new Vector3D(1.0, 1.0, 1.0)));
    mesh.addVertex(new Vertex(new Vector3D( -1.0, 1.0, -1.0)));
    mesh.addVertex(new Vertex(new Vector3D( -1.0, 1.0, 1.0)));

    mesh.addTriangle(new Triangle(4, 0, 6));
    mesh.addTriangle(new Triangle(0, 2, 4));
    mesh.addTriangle(new Triangle(4, 7, 6));
    mesh.addTriangle(new Triangle(7, 5, 4));
    mesh.addTriangle(new Triangle(2, 5, 3));
    mesh.addTriangle(new Triangle(2, 5, 4));
    mesh.addTriangle(new Triangle(1, 3, 5));
    mesh.addTriangle(new Triangle(1, 5, 7));
    mesh.addTriangle(new Triangle(0, 1, 6));
    mesh.addTriangle(new Triangle(1, 6, 7));
    mesh.addTriangle(new Triangle(0, 1, 2));
    mesh.addTriangle(new Triangle(1, 2, 3));


    System.out.println(mesh.toString());

    GLCapabilities capabilities = new GLCapabilities();
    canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);

    canvas.addGLEventListener(this);
    canvas.addMouseListener(this);
    canvas.addMouseMotionListener(this);
    canvas.addKeyListener(this);

    this.add(canvas, BorderLayout.CENTER);

    camera = new Camera();
    cameraController = new CameraControllerAquynza(camera);

  }

  public Dimension getPreferredSize() {
    return new Dimension(640, 480);
  }

  public static void main(String[] args) {
    Frame f = new TestMesh();
    f.pack();
    f.setVisible(true);
  }

  private void drawObjectsGL(GL gl) {
    gl.glLoadIdentity();
    mesh.draw(gl);
    gl.glEnd();
  }

  /** Called by drawable to initiate drawing */
  public void display(GLDrawable drawable) {
    GL gl = drawable.getGL();

    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    gl.glColor3d(1, 1, 1);

    camera.activateGL(gl);

    drawObjectsGL(gl);
  }

  /** Not used method, but needed to instanciate GLEventListener */
  public void init(GLDrawable drawable) {
    ;
  }

  /** Not used method, but needed to instanciate GLEventListener */
  public void displayChanged(GLDrawable drawable, boolean a, boolean b) {
    ;
  }

  /** Called to indicate the drawing surface has been moved and/or resized */
  public void reshape(GLDrawable drawable,
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
    if (cameraController.processMousePressedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void mouseReleased(MouseEvent e) {
    if (cameraController.processMouseReleasedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void mouseClicked(MouseEvent e) {
    if (cameraController.processMouseClickedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void mouseMoved(MouseEvent e) {
    if (cameraController.processMouseMovedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void mouseDragged(MouseEvent e) {
    if (cameraController.processMouseDraggedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    System.out.println(".");
    if (cameraController.processMouseWheelEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void keyPressed(KeyEvent e) {
    if (e.getKeyChar() == 'q') {
      System.exit(0);
    }

    if (cameraController.processKeyPressedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void keyReleased(KeyEvent e) {
    if (cameraController.processKeyReleasedEventAwt(e)) {
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
