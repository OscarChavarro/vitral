// VITRAL recomendation: Use explicit class imports (not .*) in hello world type programs
// so the user/programmer can be exposed to all the complexity involved. This will help him
// to dominate the involved libraries.

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.Adjustable;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.Matrix4x4;
import vitral.toolkits.environment.Camera;
import vitral.toolkits.gui.CameraController;
import vitral.toolkits.gui.CameraControllerAquynza;
import vitral.toolkits.visual.jogl.JoglCameraRenderer;

public class CohenSutherlandClipping extends JFrame implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    public Camera camera1;
    public Camera camera2;
    public CameraController cameraController;
    public GLCanvas canvas;
    private ControlPanel controls;
    private JMenuBar menubar;
    public Vector3D point1;
    public Vector3D point2;

    public CohenSutherlandClipping() {
        super("VITRAL concept test - JOGL Hello World");

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        controls = new ControlPanel(this);
        menubar = this.buildMenu();

        this.add(canvas, BorderLayout.CENTER);
        this.add(controls, BorderLayout.SOUTH);
        this.setJMenuBar(menubar);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        R.eulerAnglesRotation(Math.toRadians(110), Math.toRadians(-25), 0);
        camera1 = new Camera();
        camera1.setPosition(new Vector3D(1.5, -5, 2.5));
        camera1.setRotation(R);

        camera2 = new Camera();
        camera2.setPosition(new Vector3D(0.2, 0.2, 0.2));

        //-----------------------------------------------------------------

        //cameraController = new CameraControllerGravZero(camera1);
        //cameraController = new CameraControllerBlender(camera1);
        cameraController = new CameraControllerAquynza(camera1);

        point1 = new Vector3D(0.5, 0.5, 0.5);
        point2 = new Vector3D(0, 0, 0);
    }

    public Dimension getPreferredSize() {
        return new Dimension (800, 600);
    }
    
    public static void main (String[] args) {
        JFrame f = new CohenSutherlandClipping();
        f.pack();
        f.setVisible(true);
    }
    
    private void drawObjectsGL(GL gl)
    {
        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();
        gl.glLineWidth((float)2.0);
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

        //-----------------------------------------------------------------
        double delta = 0.1;

        gl.glLoadIdentity();
        gl.glTranslated(point1.x, point1.y, point1.z);
        gl.glLineWidth((float)1.0);
        gl.glColor3d(1, 1, 1);
        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(-delta/2, 0, 0);
            gl.glVertex3d(delta/2, 0, 0);

            gl.glVertex3d(0, -delta/2, 0);
            gl.glVertex3d(0, delta/2, 0);

            gl.glVertex3d(0, 0, -delta/2);
            gl.glVertex3d(0, 0, delta/2);
        gl.glEnd();

        gl.glLoadIdentity();
        gl.glTranslated(point2.x, point2.y, point2.z);
        gl.glLineWidth((float)1.0);
        gl.glColor3d(1, 1, 0);
        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(-delta/2, 0, 0);
            gl.glVertex3d(delta/2, 0, 0);

            gl.glVertex3d(0, -delta/2, 0);
            gl.glVertex3d(0, delta/2, 0);

            gl.glVertex3d(0, 0, -delta/2);
            gl.glVertex3d(0, 0, delta/2);
        gl.glEnd();

        //-----------------------------------------------------------------
        gl.glLoadIdentity();
        gl.glLineWidth((float)5.0);
        gl.glColor3d(1, 0, 1);
        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(point1.x, point1.y, point1.z);
            gl.glVertex3d(point2.x, point2.y, point2.z);
        gl.glEnd();

        //-----------------------------------------------------------------
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activateGL(gl, camera1);

        drawObjectsGL(gl);

        gl.glLoadIdentity();
        JoglCameraRenderer.drawGL(gl, camera2);
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

        camera1.updateViewportResize(width, height);
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

    public JMenuBar buildMenu()
    {
        //------------------------------------------------------------
        JMenuBar menubar;
        JMenu popup;
        JMenuItem option;

        menubar = new JMenuBar();

        //------------------------------------------------------------
        popup = new JMenu("File");
        menubar.add(popup);
        option = popup.add(new JMenuItem("Exit"));
        option.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }});
        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        //------------------------------------------------------------
        popup = new JMenu("Help");
        menubar.add(popup);
        option = popup.add(new JMenuItem("About"));
        MyActionListener mostrador_ayuda = new MyActionListener(menubar);
        option.addActionListener(mostrador_ayuda);
        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        //------------------------------------------------------------

        return menubar;
    }

}

class MyActionListener implements ActionListener {
    private Component parent;

    MyActionListener(Component parent)
    {
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(parent, 
"This program is a test for the Cohen-Sutherland 3D line clipping algorithm " +
"implemented in the Camera class of the VSDK. Move each line edge with the " +
"sliders and observe the line behavior respect the second camera view volume."
        );
    }
}

class ControlPanel extends JPanel implements AdjustmentListener, ActionListener
{

    private CohenSutherlandClipping parent;

    public ControlPanel(CohenSutherlandClipping parent)
    {
        //-----------------------------------------------------------------
        JScrollBar sb;
        JLabel jl;
        JPanel frame = null;
        JPanel innerframe = null;
        String names[] = new String[6];

        this.parent = parent;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        //-----------------------------------------------------------------
        JRadioButton rb;
        ButtonGroup bg = new ButtonGroup();

        frame = new JPanel();
        frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
        add(frame);
        frame.add(new JLabel("CAMERAS CONTROL -> "));

        innerframe = new JPanel();
        innerframe.setBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        innerframe.setLayout(new BoxLayout(innerframe, BoxLayout.X_AXIS));
        rb = new JRadioButton("Primary camera selected for control");
        rb.setSelected(true);
        rb.setActionCommand("ActivateCamera1");
        rb.addActionListener(this);
        bg.add(rb);
        innerframe.add(rb);
        rb = new JRadioButton("Secondary camera selected for control");
        rb.setSelected(false);
        rb.setActionCommand("ActivateCamera2");
        rb.addActionListener(this);
        bg.add(rb);
        innerframe.add(rb);
        frame.add(innerframe);

        //-----------------------------------------------------------------
        names[0] = "x1";
        names[1] = "y1";
        names[2] = "z1";
        names[3] = "x2";
        names[4] = "y2";
        names[5] = "z2";
        for ( int i = 0; i < 6; i++ ) {
            if ( i == 0 || i == 3 ) {
                frame = new JPanel();
                frame.setLayout(new BoxLayout(frame, BoxLayout.X_AXIS));
                add(frame);
                if ( i == 0 ) frame.add(new JLabel("     FIRST POINT -> "));
                else frame.add(new JLabel(" SECOND POINT -> "));

                innerframe = new JPanel();
                innerframe.setBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
                innerframe.setLayout(new BoxLayout(innerframe, BoxLayout.X_AXIS));
                frame.add(innerframe);
            }
            jl = new JLabel(names[i] + ": ");
            innerframe.add(jl);
            sb = new JScrollBar(JScrollBar.HORIZONTAL);
            sb.setName(names[i]);
            sb.addAdjustmentListener(this);
            //sb.setMinimum(0);
            sb.setMaximum(100);
            //sb.setValue(50);
            innerframe.add(sb);
        }
        //-----------------------------------------------------------------
    }

    public void adjustmentValueChanged(AdjustmentEvent ev) {
        double val = (((double)ev.getValue()) - 50.0) / 50.0;

        if ( ((JScrollBar)ev.getAdjustable()).getName().equals("x1") ) {
            parent.point1.x = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("y1") ) {
            parent.point1.y = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("z1") ) {
            parent.point1.z = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("x2") ) {
            parent.point2.x = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("y2") ) {
            parent.point2.y = val;
        }
        else if ( ((JScrollBar)ev.getAdjustable()).getName().equals("z2") ) {
            parent.point2.z = val;
        }

        parent.canvas.repaint();
    }

    public void actionPerformed(ActionEvent e)
    {
        if ( ((String)e.getActionCommand()).equals("ActivateCamera1") ) {
            parent.cameraController = 
                new CameraControllerAquynza(parent.camera1);
        }
        else {
            parent.cameraController = 
                new CameraControllerAquynza(parent.camera2);
        }
        parent.canvas.repaint();
    }
}

