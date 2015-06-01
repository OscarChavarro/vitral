//===========================================================================

// Java AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
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
import javax.swing.border.EtchedBorder;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// Vitral classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class CohenSutherlandClipping3D implements 
GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, 
KeyListener
{
    // Application data model
    public Camera camera1;
    public Camera camera2;
    public Camera lastPerspectiveCamera;
    public Vector3D point0;
    public Vector3D point1;
    public Vector3D testVector;

    // Application GUI artifacts
    private ControlPanel controls;
    private JMenuBar menubar;
    public CameraController cameraController;
    public GLCanvas canvas;
    int cameraMode;

    public static final int TOPVIEW = 1;
    public static final int FRONTVIEW = 2;
    public static final int LEFTVIEW = 3;
    public static final int PERSPECTIVEVIEW = 4;

    public CohenSutherlandClipping3D() {
        testVector = new Vector3D();
        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        R.eulerAnglesRotation(Math.toRadians(110), Math.toRadians(-25), 0);
        camera1 = new Camera();
        camera1.setPosition(new Vector3D(1.5, -5, 2.5));
        camera1.setRotation(R);
        camera1.setFarPlaneDistance(300);

        camera2 = new Camera();
        camera2.setPosition(new Vector3D(0, 0, 1));
        camera2.setNearPlaneDistance(1);
        camera2.setFarPlaneDistance(3);
        camera2.setFov(90);
        R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-90), 0);
        camera2.setRotation(R);

        //-----------------------------------------------------------------

        cameraController = new CameraControllerAquynza(camera1);

        point0 = new Vector3D(0, 0, 0.6);
        point1 = new Vector3D(0, 0, -10);
        //point0 = new Vector3D(-3, 0, -2.5);
        //point1 = new Vector3D(0, 0, 0.5);
    }

    public void createGUI()
    {
        JFrame mainWindowWidget;
        mainWindowWidget = new JFrame("VITRAL concept test - Cohen Sutherland 3D line clipping");

        cameraMode = PERSPECTIVEVIEW;

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        controls = new ControlPanel(this);
        menubar = this.buildMenu();

        mainWindowWidget.setPreferredSize(new Dimension(800, 600));
        mainWindowWidget.add(canvas, BorderLayout.CENTER);
        mainWindowWidget.add(controls, BorderLayout.SOUTH);
        mainWindowWidget.setJMenuBar(menubar);
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainWindowWidget.pack();
        mainWindowWidget.setVisible(true);
    } 
    
    public void setPerspectiveView()
    {
        if ( cameraMode != PERSPECTIVEVIEW ) {
            camera1.setPosition(lastPerspectiveCamera.getPosition());
            camera1.setRotation(lastPerspectiveCamera.getRotation());
            camera1.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
            cameraMode = PERSPECTIVEVIEW;
        }
    }

    public void setTopView()
    {
        Matrix4x4 R = new Matrix4x4();
        if ( cameraMode == PERSPECTIVEVIEW ) {
            lastPerspectiveCamera = new Camera(camera1);
        }
        cameraMode = TOPVIEW;
        camera1.setPosition(new Vector3D(0, 0, 5));
        R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-90), 0);
        camera1.setRotation(R);
        camera1.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera1.setOrthogonalZoom(0.25);
    }

    public void setFrontView()
    {
        Matrix4x4 R = new Matrix4x4();
        if ( cameraMode == PERSPECTIVEVIEW ) {
            lastPerspectiveCamera = new Camera(camera1);
        }
        cameraMode = TOPVIEW;
        camera1.setPosition(new Vector3D(0, -10, 0));
        R.eulerAnglesRotation(Math.toRadians(90), 0, 0);
        camera1.setRotation(R);
        camera1.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera1.setOrthogonalZoom(0.25);
    }

    public void setLeftView()
    {
        Matrix4x4 R = new Matrix4x4();
        if ( cameraMode == PERSPECTIVEVIEW ) {
            lastPerspectiveCamera = new Camera(camera1);
        }
        cameraMode = TOPVIEW;
        camera1.setPosition(new Vector3D(-10, 0, 0));
        camera1.setRotation(R);
        camera1.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera1.setOrthogonalZoom(0.25);
    }
    
    public static void main (String[] args) {
        CohenSutherlandClipping3D instance = new CohenSutherlandClipping3D();
        instance.createGUI();
    }

    private void
    drawMark(GL2 gl, double delta)
    {
        gl.glBegin(GL2.GL_LINES);
            gl.glVertex3d(-delta/2, 0, 0);
            gl.glVertex3d(delta/2, 0, 0);
            gl.glVertex3d(0, -delta/2, 0);
            gl.glVertex3d(0, delta/2, 0);
            gl.glVertex3d(0, 0, -delta/2);
            gl.glVertex3d(0, 0, delta/2);
        gl.glEnd();
    }
    
    private void drawObjectsGL(GL2 gl)
    {
        gl.glEnable(GL2.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        gl.glLineWidth((float)2.0);
        gl.glBegin(GL2.GL_LINES);
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

        boolean linePasses;

        Vector3D clippedPoint0 = new Vector3D();
        Vector3D clippedPoint1 = new Vector3D();

        camera2.updateVectors();

        //linePasses = camera2.clipLineCohenSutherlandPlanes(
        //    point0, point1, clippedPoint0, clippedPoint1);

        linePasses = camera2.clipLineCohenSutherlandCanonicVolume(
            point0, point1, clippedPoint0, clippedPoint1);

        Matrix4x4 NT = camera2.getNormalizingTransformation().inverse();

        clippedPoint0 = NT.multiply(clippedPoint0);
        clippedPoint1 = NT.multiply(clippedPoint1);

        //-----------------------------------------------------------------
        double delta = 0.1;

        gl.glLoadIdentity();
        gl.glTranslated(point0.x, point0.y, point0.z);
        gl.glLineWidth((float)1.0);
        gl.glColor3d(1, 1, 1);
        drawMark(gl, delta);

        gl.glLoadIdentity();
        gl.glTranslated(point1.x, point1.y, point1.z);
        gl.glLineWidth((float)1.0);
        gl.glColor3d(1, 1, 0);
        drawMark(gl, delta);

        //-----------------------------------------------------------------
        gl.glLoadIdentity();
        gl.glLineWidth((float)1.0);
        gl.glColor3d(1, 0, 1);
        gl.glBegin(GL2.GL_LINES);
            gl.glVertex3d(point0.x, point0.y, point0.z);
            gl.glVertex3d(point1.x, point1.y, point1.z);
        gl.glEnd();
        if ( linePasses == true ) {
            gl.glLineWidth((float)5.0);
            gl.glBegin(GL2.GL_LINES);
                gl.glVertex3d(clippedPoint0.x, clippedPoint0.y,
                    clippedPoint0.z);
                gl.glVertex3d(clippedPoint1.x, clippedPoint1.y,
                    clippedPoint1.z);
            gl.glEnd();
        }
        //-----------------------------------------------------------------
    }

    /** Called by drawable to initiate drawing */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera1);

        drawObjectsGL(gl);

        gl.glLoadIdentity();
        JoglCameraRenderer.draw(gl, camera2);
    }
   
    /** Not used method, but needed to instanciate GLEventListener */
    @Override
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized */
    @Override
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height); 

        camera1.updateViewportResize(width, height);
    }   

    @Override
    public void mouseEntered(MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //System.out.println("Mouse exited");
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if ( cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    /**
       WARNING: It is not working... check pending
    */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }

        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
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
    @Override
    public void keyTyped(KeyEvent e) {
        ;
    }

    public JMenuBar buildMenu()
    {
        //------------------------------------------------------------
        JMenuBar newMenubar;
        JMenu popup;
        JMenuItem option;

        newMenubar = new JMenuBar();

        //------------------------------------------------------------
        popup = new JMenu("File");
        newMenubar.add(popup);
        option = popup.add(new JMenuItem("Exit"));
        option.addActionListener(new ActionListener() {
            @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }});
        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        //------------------------------------------------------------
        popup = new JMenu("Help");
        newMenubar.add(popup);
        option = popup.add(new JMenuItem("About"));
        MyActionListener mostrador_ayuda = new MyActionListener(newMenubar);
        option.addActionListener(mostrador_ayuda);
        popup.getPopupMenu().setLightWeightPopupEnabled(false);

        //------------------------------------------------------------

        return newMenubar;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
