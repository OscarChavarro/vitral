// Basic Java classes

// Awt / swing classes
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
//import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Random;
import javax.swing.JFrame;

// JDBC classes
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import javax.swing.JOptionPane;
import vsdk.toolkit.common.VSDK;

// VitralSDK classes
import vsdk.toolkit.environment.Camera;              // Model elements
import vsdk.toolkit.render.jogl.JoglCameraRenderer;  // View elements
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.gui.CameraController;            // Controller elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.visualAnalytics.PercentageWheelWidget;
import vsdk.toolkit.gui.visualAnalytics.VisualDoubleVariable;
import vsdk.toolkit.gui.visualAnalytics.VisualVariableSet;
import vsdk.toolkit.render.jogl.visualAnalytics.JoglPercentageWheelWidgetRenderer;
import vsdk.toolkit.gui.visualAnalytics.PercentageWheelWidgetController;

/**
Note that this program is designed to work as a java application, or as a
java applet.  If current class does not extends from Applet, and `init` method
is deleted, this will continue working as a simple java application.

This is a simple program recommended for use as a template in the development
of VitralSDK programs by incremental modification.
*/
public class PercentagesWheelExample extends Applet implements 
    GLEventListener,                                                    // JOGL
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener // GUI
{

//= PROGRAM PART 1/5: ATTRIBUTES ============================================

    private boolean appletMode;
    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private SimpleCorridor corridor;
    
    private VisualVariableSet variableSet;
    private PercentageWheelWidget visualAnalyticsWidget;
    private PercentageWheelWidgetController visualAnalyticsWidgetController;
    private boolean show2D;

    private Connection connection;
    
//= PROGRAM PART 2/5: CONSTRUCTORS ==========================================

    /**
    When running this class inside a browser (in applet mode) there is no
    warranty of calling this method, or calling before init. It is recommended
    that real initialization be done in another `createModel` method, and
    that such method be called explicity from entry point function.
    */
    public PercentagesWheelExample() {
        // Empty! call `createModel` explicity from entry point function!
        
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
        //----------------------------------------------------------------------
        //fillVariableSetWithHardcodedInformation();
        fillVariableSetWithDatabaseInformation();
        
        //----------------------------------------------------------------------
        visualAnalyticsWidget = 
            new PercentageWheelWidget(variableSet);
        show2D = true;
        visualAnalyticsWidgetController = new PercentageWheelWidgetController(
            visualAnalyticsWidget);
        connection = null;
    }

    protected void fillVariableSetWithHardcodedInformation() {
        variableSet = new VisualVariableSet();
        VisualDoubleVariable v;
        int N = 20;
        int i;
        Random r = new Random();

        for ( i = 0; i < N; i++ ) {
            v = new VisualDoubleVariable();
            v.setName("Variable " + (char)('A' + i));
            v.setDescription("Long description " + (char)('A' + i) + "\nadditional text\nthird line");
            v.setCurrentValue(r.nextDouble());
            variableSet.getDoubles().add(v);
        }
    }

    private Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String server = "jdbc:mysql://127.0.0.1:3306/wheel";
            String user = "jedilink";
            String password = "jed666";
            connection = DriverManager.getConnection(server, user, password);
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, ex, "Error1 en la Conexion con la BD " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            connection = null;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex, "Error2 en la Conexion con la BD " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            connection = null;
        }
        return connection;
    }

    private void fillVariableSetWithDatabaseInformation() {
        if ( connection == null ) {
            connection = getConnection();
        }
        
        String sqlquery = "select * from data";
        try {
            Statement sqlstatement;
            sqlstatement = connection.createStatement();
            sqlstatement.executeQuery(sqlquery);
            ResultSet result;
            result = sqlstatement.executeQuery(sqlquery);
            
            variableSet = new VisualVariableSet();
            VisualDoubleVariable v;

            while ( result.next() ) {
                v = new VisualDoubleVariable();
                v.setName(result.getString("variable_name"));
                v.setDescription(result.getString("variable_description"));
                v.setCurrentValue(Double.parseDouble(
                    result.getString("variable_value")) / 100.0);
                variableSet.getDoubles().add(v);            
            }
            result.close();
            sqlstatement.close();
        } catch (SQLException e) {
            VSDK.reportMessageWithException(null, VSDK.FATAL_ERROR, 
                    "fillArrayListSample", "Error connecting with database", e);

        }

    }

    
    private void createGUI()
    {
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
    }

//= PROGRAM PART 3/5: ENTRY POINTS ==========================================

    public static void main (String[] args) {
        // Common VitralSDK initialization
        JoglRenderer.verifyOpenGLAvailability();
        PercentagesWheelExample instance = new PercentagesWheelExample();
        instance.appletMode = false;
        instance.createModel();

        // Create application based GUI
        JFrame frame;
        Dimension size;

        instance.createGUI();
        frame = new JFrame("VITRAL concept test - Visual Analytics / Wheel Percentage example");
        frame.add(instance.canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        size = new Dimension(512, 512);
        frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        //frame.setBounds(1400, 100, size.width, size.height);
        instance.canvas.requestFocusInWindow();
    }

    @Override
    public void init()
    {
        appletMode = true;
        createModel();
        setLayout(new BorderLayout());
        createGUI();
        add("Center", canvas);
    }
    
//= PROGRAM PART 4/5: JOGL-OPENGL PROCEDURES ================================

    private void drawObjectsGL(GL2 gl)
    {
        gl.glEnable(GL2.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        corridor.drawGL(gl);

        gl.glLineWidth((float)3.0);
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

        gl.glLoadIdentity();
        JoglPercentageWheelWidgetRenderer.draw(
            gl, visualAnalyticsWidget);
        
        if ( show2D ) {
            configureForHUD(gl, camera);
            JoglPercentageWheelWidgetRenderer.draw(
                gl, visualAnalyticsWidget);
        }
    }

    /** 
    Called by drawable to initiate drawing 
    @param drawable
    */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);

        drawObjectsGL(gl);
    }
   
    /**
    Not used method, but needed to instanciate GLEventListener 
    @param drawable
    */
    @Override
    public void init(GLAutoDrawable drawable) {
        
    }

    /** 
    Not used method, but needed to instanciate GLEventListener 
    @param drawable
    */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        
    }

    /** 
    Not used method, but needed to instanciate GLEventListener 
    @param drawable
    @param a
    @param b
    */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized
    @param drawable 
    @param x 
    @param y 
    @param width 
    @param height 
    */
    @Override
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height); 

        camera.updateViewportResize(width, height);
    }   

//= PART 5/5: GUI PROCEDURES ================================================

    /**
    @param e
    */
    @Override
    public void mouseEntered(java.awt.event.MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    /**
    @param e
    */
    @Override
    public void mouseExited(java.awt.event.MouseEvent e) {
      //System.out.println("Mouse exited");
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent awte) {
        MouseEvent e = AwtSystem.awt2vsdkEvent(awte);
        if ( visualAnalyticsWidgetController.processMousePressedEvent(e, camera) ) {
            canvas.repaint();
        }
        else if ( cameraController.processMousePressedEvent(e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent awte) {
        MouseEvent e = AwtSystem.awt2vsdkEvent(awte);

        if ( visualAnalyticsWidgetController.processMouseMovedEvent(e, camera) ) {
            canvas.repaint();
        }
        
        if ( cameraController.processMouseMovedEvent(e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(
                 AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            if ( !appletMode ) {
                System.exit(0);
            }
        }

        if ( e.getKeyCode() == KeyEvent.VK_SPACE ) {
            show2D = !show2D;
            canvas.repaint();
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
    @param e
    */
    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    private void configureForHUD(GL2 gl, Camera camera) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        double dx = camera.getViewportXSize();
        double dy = camera.getViewportYSize();
        
        // Warning: Should not scale z to -1, but 
        // JoglPercentageWheelWidgetRenderer is behaving in a rare form...
        if ( dy > dx ) {
            gl.glScaled(1, dx / dy, -1);
        }
        else {
            gl.glScaled(dy / dx, 1, -1);
        }

    }

}
