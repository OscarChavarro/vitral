// VITRAL recomendation: Use explicit class imports (not .*) in hello world type programs
// so the user/programmer can be exposed to all the complexity involved. This will help him
// to dominate the involved libraries.

import javax.swing.*;
import javax.swing.event.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import java.awt.event.*;
import net.java.games.jogl.GL;
import net.java.games.jogl.GLU;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLCapabilities;
import net.java.games.jogl.GLDrawable;
import net.java.games.jogl.GLDrawableFactory;
import net.java.games.jogl.GLEventListener;
//import vitral.toolkits.common.Vector3D;
import vitral.toolkits.environment.Camera;
import vitral.framework.gui.CameraController;
import vitral.framework.gui.CameraControllerAquynza;
import vitral.framework.gui.CameraControllerBlender;

public class CameraExample extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    private ButtonsPanel controls;
    private JMenuBar menubar;

    public CameraExample() {
        super("VITRAL concept test - JOGL Hello World with Swing Menus");

        GLCapabilities capabilities = new GLCapabilities();
        canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);


        controls = new ButtonsPanel();

        menubar = this.buildMenu();

        this.add(canvas, BorderLayout.CENTER);
        this.add(controls, BorderLayout.SOUTH);
        this.setJMenuBar(menubar);

        camera = new Camera();
        cameraController = new CameraControllerBlender(camera);

        corridor = new SimpleCorridor();
    }

    public JMenuBar buildMenu()
    {
        JMenuBar menubar;
        JMenu popup;
        JMenuItem option;

        menubar = new JMenuBar();
        popup = new JMenu("File");
        menubar.add(popup);

        option = popup.add(new JMenuItem("Exit"));
        option.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }});

        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        return menubar;
    }
    
    public Dimension getPreferredSize() {
        return new Dimension (640, 480);
    }
    
    public static void main (String[] args) {
        JFrame f = new CameraExample();
        f.pack();
        f.setVisible(true);
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
    public void display(GLDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1); 

        camera.activateGL(gl);
        drawObjectsGL(gl);

    // Animation example
        /*
        Vector3D p = camera.getPosition();
        p.y += 0.1;
        camera.setPosition(p);

        canvas.repaint();
        */
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
    public void reshape (GLDrawable drawable,
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

  public void mouseWheelMoved(MouseWheelEvent e) {
      System.out.println(".");
      if ( cameraController.processMouseWheelEventAwt(e) ) {
          canvas.repaint();
      }
  }

  public void keyPressed(KeyEvent e) {
      if ( e.getKeyChar() == 'q' ) {
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


class ButtonsPanel extends Panel implements ActionListener
{
    public ButtonsPanel()
    {
        JButton b = null;

        b = new JButton("Exit");
        b.addActionListener(this);
        add(b);
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        if ( label == "Exit" ) {
            System.exit(1);
        }
    }
}
