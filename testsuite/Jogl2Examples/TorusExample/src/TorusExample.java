// Java basic classes

// Java Awt/Swing classes
import java.awt.BorderLayout;
import java.awt.Button;
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
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;              // Model elements
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.volume.Torus;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;  // View elements
import vsdk.toolkit.render.jogl.Jogl2Renderer;
import vsdk.toolkit.gui.CameraController;            // Controller elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.render.jogl.Jogl2TorusRenderer;
import vsdk.toolkit.render.jogl.Jogl2MaterialRenderer;
import vsdk.toolkit.render.jogl.Jogl2LightRenderer;
import vsdk.toolkit.environment.LightType;

/**
This is a simple program recommended for use as a template in the development
of VitralSDK programs by incremental modification.
*/
public class TorusExample implements 
    GLEventListener,                                                    // JOGL
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener // GUI
{

//= PROGRAM PART 1/5: ATTRIBUTES ============================================

    private Camera camera;
    private CameraController cameraController;
    private Light light;
    private Material material;
    private RendererConfiguration qualitySelection;
    private RendererConfigurationController qualityController;
    private GLCanvas canvas;  
    private Torus torus;
    private int N;
    private int n;

    
    // Attributes of Menu Bar
    static JButton jButton1 = new JButton();;
    static JButton jButton2 = new JButton();
    static JPanel jPanel1 = new JPanel();
    static JPanel jPanel2 = new JPanel();
    static JPanel jPanel3= new JPanel();
    static JLabel jLabel1 = new JLabel();
    static JLabel jLabel2 = new JLabel();
    static JLabel jLabel3 = new JLabel();
    static JLabel jLabel4 = new JLabel();
    static JSpinner jSpinner1 = new JSpinner();
    static JSpinner jSpinner2 = new JSpinner();
    static JSpinner jSpinner3 = new JSpinner();
    static JSpinner jSpinner4 = new JSpinner();

//= PROGRAM PART 2/5: CONSTRUCTORS ==========================================

    /**
    When running this class inside a browser (in applet mode) there is no
    warranty of calling this method, or calling before init. It is recommended
    that real initialization be done in another `createModel` method, and
    that such method be called explicity from entry point function.
    */
    public TorusExample() {
        //Parameters to draw de torus
        N=10;
        n=10;
        // Empty! call `createModel` explicity from entry point function!
        jButton1.addActionListener(new ActionListener () {
 
            @Override
            public void actionPerformed(ActionEvent  e)
            {
                //Execute when button is pressed
                Button a = new Button("click");
                KeyEvent ev = new KeyEvent(a, 1, 20, 1, 10, 'a');
                keyPressed( ev);
            }
        }); 
        
        jButton2.addActionListener(new ActionListener () {
 
            @Override
            public void actionPerformed(ActionEvent  e)
            {
                //Execute when button is pressed
                Button a = new Button("click");
                KeyEvent ev = new KeyEvent(a, 1, 20, 1, 10, 'A');
                keyPressed( ev);
            }
        }); 
        
        //Change the major radius of the torus
        jSpinner1.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                torus.setMajorRadius((double)jSpinner1.getValue());
                canvas.repaint();
                
            }

          
        });
        //Change the minor radius of the torus
        jSpinner2.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                torus.setMinorRadius((double)jSpinner2.getValue());
                canvas.repaint();
            }

          
        });
        //Change the number of xxxx of the torus
        jSpinner3.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                N=((int)jSpinner3.getValue());
                canvas.repaint();
            }

          
        });
        
        //Change the number of xxxx of the torus
        jSpinner4.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                n=((int)jSpinner4.getValue());
                canvas.repaint();
            }

          
        });
    }

    /**
    Real constructor
    */
    private void createModel()
    {
        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);
        
        qualitySelection = new RendererConfiguration();
        qualityController = 
            new RendererConfigurationController(qualitySelection);
        
        light = 
            new Light(
                LightType.POINT, new Vector3D(3, 3, 5), new ColorRgb(1, 1, 1));
        light.setId(0);
        material = new Material();
        material.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        material.setDiffuse(new ColorRgb(0.5, 0.9, 0.5));
        material.setSpecular(new ColorRgb(1, 1, 1));

        torus = new Torus(1.8, 0.2);
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
        Jogl2Renderer.verifyOpenGLAvailability();
        TorusExample instance = new TorusExample();
        instance.createModel();

           // Create application based GUI
        JFrame frame;
        Dimension size;

        instance.createGUI();
        frame = new JFrame("VITRAL concept test - Torus example");
        frame.add(instance.canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        size = new Dimension(640, 480);
        frame.setMinimumSize(size);
        frame.setSize(size);
        
      
             
  //      instance.canvas.requestFocusInWindow();
        frame.getContentPane().add(
            menu(), BorderLayout.AFTER_LINE_ENDS);
        
        frame.getContentPane().add(
            instance.canvas, BorderLayout.CENTER);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    
    static public JPanel menu()
    {
        jPanel2.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder()));

        jButton1.setIcon(new ImageIcon("./etc/mas.png")); // NOI18N
        jButton1.setContentAreaFilled(true);
       

        jButton2.setIcon(new ImageIcon("./etc/menos.png")); // NOI18N
        jButton2.setContentAreaFilled(true);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1, GroupLayout.PREFERRED_SIZE, 44, 
                    GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2, GroupLayout.PREFERRED_SIZE, 44, 
                    GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(
                    GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton2)
                    .addComponent(jButton1))
                .addGap(0, 9, Short.MAX_VALUE))
        );

        jPanel3.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder()));

        jSpinner1.setModel(
            new SpinnerNumberModel(1.8d, 0.0d, null, 0.1d));

        jLabel1.setText("R");

        jLabel2.setText("r");

        jSpinner2.setModel(
            new SpinnerNumberModel(0.2d, 0.0d, null, 0.1d));

        jLabel3.setText("N");

        jSpinner3.setModel(new SpinnerNumberModel(10, 0, null, 1));

        jLabel4.setText("n");

        jSpinner4.setModel(new SpinnerNumberModel(10, 0, null, 1));

        GroupLayout jPanel3Layout = 
            new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(
                GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(
                    GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, 
                    28, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(
                    GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSpinner1)
                    .addComponent(jSpinner2, 
                        GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                    .addComponent(jSpinner3)
                    .addComponent(jSpinner4))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(
                    GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner1, 
                        GroupLayout.PREFERRED_SIZE, 
                        GroupLayout.DEFAULT_SIZE, 
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(
                    GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner2, 
                        GroupLayout.PREFERRED_SIZE, 
                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(
                    GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner3, GroupLayout.PREFERRED_SIZE, 
                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(
                    GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner4, GroupLayout.PREFERRED_SIZE, 
                        GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap())
        );

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(
                    GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 0, 
                        Short.MAX_VALUE)
                    .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, 
                        GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, 
                jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, 
                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 
                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(245, Short.MAX_VALUE))
        );
       
               
        
        
        return jPanel1;
    }
    
//= PROGRAM PART 4/5: JOGL-OPENGL PROCEDURES ================================

    private void drawObjectsGL(GL2 gl)
    {
        //-----------------------------------------------------------------
        gl.glEnable(GL2.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        Jogl2LightRenderer.draw(gl, light);
        gl.glEnable(GL2.GL_LIGHTING);
        Jogl2LightRenderer.activate(gl, light);
        Jogl2MaterialRenderer.activate(gl, material);

        Jogl2TorusRenderer.draw(gl, torus, camera, qualitySelection, n, N);
        
        //-----------------------------------------------------------------
        gl.glDisable(GL2.GL_LIGHTING);
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

        Jogl2CameraRenderer.activate(gl, camera);

        drawObjectsGL(gl);
    }
   
    /** 
    Not used method, but needed to instance GLEventListener
    @param drawable 
    */
    @Override
    public void init(GLAutoDrawable drawable) {
        
    }

    /** 
    Not used method, but needed to instance GLEventListener
    @param drawable 
    */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        
    }

    /** 
    Called to indicate the drawing surface has been moved and/or resized
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
        if ( cameraController.processMousePressedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ( cameraController.processMouseReleasedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if ( cameraController.processMouseClickedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if ( cameraController.processMouseMovedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ( cameraController.processMouseDraggedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( cameraController.processMouseWheelEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection);
        }
        if ( cameraController.processKeyPressedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(
            AwtSystem.awt2vsdkEvent(e)) ) {
            System.out.println(qualitySelection);
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (cameraController.processKeyReleasedEvent(
            AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEvent(
            AwtSystem.awt2vsdkEvent(e))) {
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

}
