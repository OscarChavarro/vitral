//===========================================================================

// Basic Java classes

// Awt / swing classes
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglRenderer;

/**
Note that this program is designed to work as a java application, or as a
java applet.  If current class does not extends from Applet, and `init` method
is deleted, this will continue working as a simple java application.

This is a simple programme recommended for use as a template in the development
of VitralSDK programs by incremental modification.
*/
public class CameraSwingExample extends Applet implements
    GLEventListener,
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{

//= PROGRAM PART 1/5: ATTRIBUTES ============================================

    public boolean appletMode;
    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    private ButtonsPanel controls;
    private JMenuBar menubar;

//= PROGRAM PART 2/5: CONSTRUCTORS ==========================================

    /**
    When running this class inside a browser (in applet mode) there is no
    warranty of calling this method, or calling before init. It is recommended
    that real initialization be done in another `createModel` method, and
    that such method be called explicity from entry point function.
    */
    public CameraSwingExample() {
        // Empty! call `createModel` explicity from entry point function!
        ;
    }

    /**
    Real constructor
    */
    private void createModel()
    {
        camera = new Camera();

        //cameraController = new CameraControllerBlender(camera);
        cameraController = new CameraControllerAquynza(camera);

        corridor = new SimpleCorridor();
    }

    private void createGUI()
    {
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        controls = new ButtonsPanel(this);
        menubar = buildMenu();
    }

//= PROGRAM PART 3/5: ENTRY POINTS ==========================================

    public static void main (String[] args) {
        // Common VitralSDK initialization
        JoglRenderer.verifyOpenGLAvailability();
        CameraSwingExample instance = new CameraSwingExample();
        instance.appletMode = false;
        instance.createModel();

        // Create application based GUI
        JFrame frame;
        Dimension size;

        instance.createGUI();
        frame = new JFrame("VITRAL concept test - Camera control example");
        frame.add(instance.canvas, BorderLayout.CENTER);
        frame.add(instance.controls, BorderLayout.SOUTH);
        frame.setJMenuBar(instance.menubar);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        size = new Dimension(640, 480);
        frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        instance.canvas.requestFocusInWindow();
    }

    public void init()
    {
        appletMode = true;
        createModel();
        setLayout(new BorderLayout());
        createGUI();
        add("North", menubar);
        add("Center", canvas);
        add("South", controls);
    }

//= PROGRAM PART 4/5: JOGL-OPENGL PROCEDURES ================================    
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
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glClearColor(0, 0, 0, 1);
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

//= PART 5/5: GUI PROCEDURES ================================================

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
            if ( !appletMode ) {
                System.exit(0);
            }
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
        JMenuBar menubar;
        JMenu popup;
        JMenuItem option;

        menubar = new JMenuBar();
        popup = new JMenu("File");
        menubar.add(popup);

        option = popup.add(new JMenuItem("Exit"));
        option.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !appletMode ) {
                    System.exit(0);
                }
            }});

        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        return menubar;
    }

}

class ButtonsPanel extends JPanel implements ActionListener
{
    private CameraSwingExample parent;

    public ButtonsPanel(CameraSwingExample parent)
    {
        this.parent = parent;

        JButton b = null;

        b = new JButton("Exit");
        b.addActionListener(this);
        add(b);
    }

    public void actionPerformed(ActionEvent ev) {
        String label = ev.getActionCommand();

        if ( label == "Exit" ) {
            if ( !parent.appletMode ) {
                System.exit(1);
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
