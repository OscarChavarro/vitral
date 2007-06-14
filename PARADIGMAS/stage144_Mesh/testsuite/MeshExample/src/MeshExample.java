import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import util.filters.*;
import vitral.toolkits.common.*;
import vitral.toolkits.environment.*;
import vitral.toolkits.geometry.*;
import vitral.toolkits.gui.*;
import vitral.toolkits.util.loaders.*;
import vitral.toolkits.visual.jogl.*;

/**
 */
public class MeshExample
    extends JFrame implements GLEventListener, MouseListener,
    MouseMotionListener, MouseWheelListener, KeyListener {

  private Camera camera;
  private CameraController cameraController;
  private QualitySelection qualitySelection;
  private QualitySelectionController qualityController;
  private GLCanvas canvas;

  private MeshGroup meshGroup;

  public MeshExample() {
    super("VITRAL mesh test - JOGL");

    JFileChooser jfc = null;

    jfc = new JFileChooser( (new File("")).getAbsolutePath());

    jfc.removeChoosableFileFilter(jfc.getFileFilter());
    jfc.addChoosableFileFilter(new ObjectFilter("obj", "Obj Files"));
    int opc = jfc.showOpenDialog(new JPanel());
    if (opc == JFileChooser.APPROVE_OPTION) {
      try {
        File file = jfc.getSelectedFile();
        meshGroup = ReaderObj.read(file.getAbsolutePath());
        //System.out.println(meshGroup.toString());
      }
      catch (IOException ex) {
        System.out.println("Failed to read file");
        return;
      }

    }

    GLCapabilities capabilities = new GLCapabilities();
    canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);

    canvas.addGLEventListener(this);
    canvas.addMouseListener(this);
    canvas.addMouseMotionListener(this);
    canvas.addKeyListener(this);

    this.add(canvas, BorderLayout.CENTER);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    camera = new Camera();
    cameraController = new CameraControllerAquynza(camera);
    qualitySelection = new QualitySelection();
    qualityController = new QualitySelectionController(qualitySelection);
  }

  public Dimension getPreferredSize() {
    return new Dimension(640, 480);
  }

  public static void main(String[] args) {
    JFrame f = new MeshExample();
    f.pack();
    f.setVisible(true);
  }

  private void drawObjectsGL(GL gl) {
    gl.glLoadIdentity();
    JoglMeshGroupRenderer.draw(gl, meshGroup, qualitySelection);
    gl.glEnd();
  }

  /** Called by drawable to initiate drawing */
  public void display(GLAutoDrawable drawable) {
    GL gl = drawable.getGL();

    gl.glEnable(gl.GL_DEPTH_TEST);
    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    gl.glColor3d(1, 1, 1);
    
    JoglCameraRenderer.activateGL(gl, camera);
   
    initLight(gl);

    drawObjectsGL(gl);
  }

  private void initLight(GL gl) {
    gl.glEnable(gl.GL_LIGHTING);
    gl.glEnable(gl.GL_LIGHT0);
    float[] light_position = {
        (float) 1.5, (float) 0.0, (float) 1.0, (float) 0.0};
    float[] shadow_color = {
        (float) 0.1, (float) 0.1, (float) 0.1, 1};
    float[] brightness_color = {
        (float) 1.0, (float) 1.0, (float) 1.0, (float) 1.0};
    float[] light_color = {
        (float) 1.0, (float) 1.0, (float) 1.0, (float) 1.0};

    gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, light_position, 0);
    gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, shadow_color, 0);
    gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, light_color, 0);
    gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, brightness_color, 0);

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
  public void reshape(GLAutoDrawable drawable,
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
    if (qualityController.processKeyPressedEventAwt(e)) {
      canvas.repaint();
    }
  }

  public void keyReleased(KeyEvent e) {
    if (cameraController.processKeyReleasedEventAwt(e)) {
      canvas.repaint();
    }
    if (qualityController.processKeyReleasedEventAwt(e)) {
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
