//===========================================================================

// Basic Java classes

// Awt / swing classes
import java.applet.Applet;
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
import java.io.File;
import javax.swing.JFrame;

// JOGL classes
import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import vsdk.toolkit.common.RendererConfiguration;

// VitralSDK classes
import vsdk.toolkit.environment.Camera;              // Model elements
import vsdk.toolkit.render.jogl.JoglCameraRenderer;  // View elements
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.gui.CameraController;            // Controller elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglImageRenderer;


/**
 *
 * @author Leidy Alexandra Lozano Jácome
 */

/**
Note that this program is designed to work as a java application, or as a
java applet.  If current class does not extends from Applet, and `init` method
is deleted, this will continue working as a simple java application.

This is a simple program recommended for use as a template in the development
of VitralSDK programs by incremental modification.
*/
public class TorusExample extends Applet implements 
    GLEventListener,                                                    // JOGL
    KeyListener, MouseListener, MouseMotionListener, MouseWheelListener // GUI
{

//= PROGRAM PART 1/5: ATTRIBUTES ============================================

    private boolean appletMode;
    private Camera camera;
    private CameraController cameraController;
    private RendererConfiguration qualitySelection;
    private RendererConfigurationController qualityController;
    private GLCanvas canvas;
    private RGBImage img;
    private RGBAImage img1;
  
    private Torus torus;
    private JoglTorusRender rTorus;
    
    private int N;
    private int n;

    
    // Attributes of Menu Bar
    static JButton jButton1 = new javax.swing.JButton();;
    static JButton jButton2 = new javax.swing.JButton();
    static JPanel jPanel1 = new javax.swing.JPanel();
    static JPanel jPanel2 = new javax.swing.JPanel();
    static JPanel jPanel3= new javax.swing.JPanel();
    static JLabel jLabel1 = new javax.swing.JLabel();
    static JLabel jLabel2 = new javax.swing.JLabel();
    static JLabel jLabel3 = new javax.swing.JLabel();
    static JLabel jLabel4 = new javax.swing.JLabel();
    static JSpinner jSpinner1 = new javax.swing.JSpinner();
    static JSpinner jSpinner2 = new javax.swing.JSpinner();
    static JSpinner jSpinner3 = new javax.swing.JSpinner();
    static JSpinner jSpinner4 = new javax.swing.JSpinner();

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
            @SuppressWarnings("empty-statement")
            public void actionPerformed(ActionEvent  e)
            {
                //Execute when button is pressed
                 Button a = new Button("click");
                KeyEvent ev = new KeyEvent(a, 1, 20, 1, 10, 'a');;
                keyPressed( ev);
            }
        }); 
        
        jButton2.addActionListener(new ActionListener () {
 
            @Override
            @SuppressWarnings("empty-statement")
            public void actionPerformed(ActionEvent  e)
            {
                //Execute when button is pressed
                 Button a = new Button("click");
                KeyEvent ev = new KeyEvent(a, 1, 20, 1, 10, 'A');;
                keyPressed( ev);
            }
        }); 
        
        //Change the major radius of the torus
        jSpinner1.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                torus.setrMajor((double)jSpinner1.getValue());
                canvas.repaint();
                
            }

          
        });
        //Change the minor radius of the torus
        jSpinner2.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent e) {
                torus.setrMinor((double)jSpinner2.getValue());
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
        
        
        
       String imageFilename = "./etc/control.png";
        try {
         
            img1 = ImagePersistence.importRGBA(new File(imageFilename));
            
        }
        catch (Exception e) {
            System.err.println("Error: could not read the image file \"" + imageFilename + "\".");
            System.err.println("Check you have access to that file from current working directory.");
            System.err.println(e);
            System.exit(0);
        }
    }

    /**
    Real constructor
    */
    private void createModel()
    {
        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);
        
        qualitySelection = new RendererConfiguration();
        qualityController = new RendererConfigurationController(qualitySelection);
        
        torus=new Torus(1.8, 0.2);
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
        TorusExample instance = new TorusExample();
        instance.appletMode = false;
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
        frame.getContentPane().add(menu(), java.awt.BorderLayout.AFTER_LINE_ENDS);
        
        frame.getContentPane().add(instance.canvas, java.awt.BorderLayout.CENTER);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void init()
    {
       appletMode = true;
        createModel();
        setLayout(new BorderLayout());
        createGUI();
        add("Center", canvas);
    }
    
static public JPanel menu()
    {
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder()));

        jButton1.setIcon(new javax.swing.ImageIcon("./etc/mas.png")); // NOI18N
        jButton1.setContentAreaFilled(true);
       

        jButton2.setIcon(new javax.swing.ImageIcon("./etc/menos.png")); // NOI18N
        jButton2.setContentAreaFilled(true);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton2)
                    .addComponent(jButton1))
                .addGap(0, 9, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder()));

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(1.8d), Double.valueOf(0.0d), null, Double.valueOf(0.1d)));

        jLabel1.setText("R");

        jLabel2.setText("r");

        jSpinner2.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(0.2d), Double.valueOf(0.0d), null, Double.valueOf(0.1d)));

        jLabel3.setText("N");

        jSpinner3.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(10), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel4.setText("n");

        jSpinner4.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(10), Integer.valueOf(0), null, Integer.valueOf(1)));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 28, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSpinner1)
                    .addComponent(jSpinner2, javax.swing.GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE)
                    .addComponent(jSpinner3)
                    .addComponent(jSpinner4))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(245, Short.MAX_VALUE))
        );
       
               
        
        
        return jPanel1;
    }
    
//= PROGRAM PART 4/5: JOGL-OPENGL PROCEDURES ================================

    private void drawObjectsGL(GL2 gl)
    {
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glLoadIdentity();

        
        rTorus=new JoglTorusRender();
        rTorus.draw(gl,torus,camera,qualitySelection,n,N);
        
      

        gl.glLineWidth((float)3.0);
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
        
        
        // Draw image directly over screen
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
         gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
  
      
        JoglImageRenderer.draw(gl, img1);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);

        drawObjectsGL(gl);
    }
   
    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
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
        GL2 gl = drawable.getGL().getGL2();
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

    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( cameraController.processMouseWheelEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection);
        }
        if ( cameraController.processKeyPressedEventAwt(e) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEventAwt(e) ) {
            System.out.println(qualitySelection);
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
