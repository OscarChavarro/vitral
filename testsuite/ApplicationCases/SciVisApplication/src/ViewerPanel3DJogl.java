
// AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

// JOGL classes
import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;

// Internal classes
import vsdk.transition.render.swing.SwingGuiCacheRenderer;

// Application classes
import scivis.Study;
import scivis.TimeTake;

public class ViewerPanel3DJogl extends ViewerPanel implements GLEventListener, ActionListener, KeyListener, MouseMotionListener
{
    private int viewportXpos;
    private int viewportYpos;
    private int viewportXsize;
    private int viewportYsize;
    private boolean viewportResizeNeeded;
    public GLJPanel canvas;

    private static final int MODE_2D_DIRECT = 1;
    private static final int MODE_2D_POLYGON = 2;
    private static final int MODE_3D = 3;
    private int operationMode;

    private Camera camera;
    private CameraController cameraController;

    private int selectedSlice;

    public ViewerPanel3DJogl(SciVisApplication parent, PanelManager container)
    {
        super(parent, container);
        this.parent = parent;
        this.viewportResizeNeeded = false;
        this.viewportXpos = 0;
        this.viewportYpos = 0;
        this.viewportXsize = 0;
        this.viewportYsize = 0;

        canvas = new GLJPanel();
        Dimension minimumSize = new Dimension(8, 8);
        canvas.setMinimumSize(minimumSize);
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        canvas.setComponentPopupMenu(
            SwingGuiCacheRenderer.buildPopupMenu(container.parentGui, 
                                                 "POPUP_PANEL_MANAGER_VIEW", this).getPopupMenu());

        setLayout(new BorderLayout());
        add(canvas);

        operationMode = MODE_3D;
        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(130), Math.toRadians(-30), 0);
        camera.setPosition(new Vector3D(1.25, -1.25, 1));
        camera.setRotation(R);

        selectedSlice = -1;
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
        this.viewportXpos = 0;
        this.viewportYpos = 0;
        this.viewportXsize = width;
        this.viewportYsize = height;
        this.viewportResizeNeeded = true;
    }   

    private void display3DSlices(GL2 gl)
    {
        JoglCameraRenderer.activate(gl, camera);

        gl.glDisable(gl.GL_LIGHTING);

        TimeTake timeTake;

        Image reference = null;

        timeTake = parent.study.getTimeTake(parent.currentTimeTake);
        if ( timeTake != null ) {
            reference = parent.study.getSliceImageAt(parent.currentTimeTake, 0);
        }

        if ( timeTake == null && reference != null ) {
            gl.glColor3d(1.0, 0.0, 0.0);
            gl.glBegin(gl.GL_LINES);
                gl.glVertex3d(-1, -1, 0);
                gl.glVertex3d(1, 1, 0);
                gl.glVertex3d(1, -1, 0);
                gl.glVertex3d(-1, 1, 0);
            gl.glEnd();
            return;
        }

        //-----------------------------------------------------------------
        double xsize = 1.0;
        double ysize = 1.0;
        double zsize = 1.0;
        double pixelFactor = 1.0;
        int xs = reference.getXSize();
        int ys = reference.getYSize();

        if ( xs > ys ) {
            xsize = ((double)xs) / ((double) ys);
            pixelFactor = 2.0 / ((double) ys);
        }
        else {
            ysize = ((double)ys) / ((double) xs);
            pixelFactor = 2.0 / ((double) xs);
        }

        Vector3D s = parent.study.getVoxelSizeFactor();
        double z = -pixelFactor*((double)timeTake.getNumSlices()/2.0);

        //-----------------------------------------------------------------
        gl.glLoadIdentity();
        gl.glScaled(s.x, s.y, s.z);

        int i;
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glBegin(gl.GL_QUADS);
        for ( i = 0; i < timeTake.getNumSlices(); i++ ) {
            if ( i == selectedSlice ) {
                gl.glLineWidth(4.0f);
                gl.glColor3d(1.0, 0.0, 0.0);
            }
            else {
                gl.glLineWidth(1.0f);
                gl.glColor3d(1.0, 1.0, 1.0);
            }
            gl.glVertex3d(-xsize/2, -ysize/2, z);
            gl.glVertex3d(xsize/2, -ysize/2, z);
            gl.glVertex3d(xsize/2, ysize/2, z);
            gl.glVertex3d(-xsize/2, ysize/2, z);
            z += pixelFactor;
        }
        gl.glEnd();

        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
        gl.glColor3d(1.0, 1.0, 1.0);
        z = -pixelFactor*((double)timeTake.getNumSlices()/2.0);
        for ( i = 0; i < timeTake.getNumSlices(); i++ ) {
            if ( i == selectedSlice ) {
                reference = parent.study.getSliceImageAt(parent.currentTimeTake, i);
                if ( reference != null ) {
                    gl.glEnable(gl.GL_TEXTURE_2D);
                    JoglImageRenderer.activate(gl, reference);
                }
                else {
                    gl.glDisable(gl.GL_TEXTURE_2D);
                }
                gl.glBegin(gl.GL_QUADS);
                  gl.glTexCoord2d(0, 0);
                  gl.glVertex3d(-xsize/2, -ysize/2, z);
                  gl.glTexCoord2d(1, 0);
                  gl.glVertex3d(xsize/2, -ysize/2, z);
                  gl.glTexCoord2d(1, 1);
                  gl.glVertex3d(xsize/2, ysize/2, z);
                  gl.glTexCoord2d(0, 1);
                  gl.glVertex3d(-xsize/2, ysize/2, z);
                gl.glEnd();
                break;
            }
            z += pixelFactor;
        }

        //-----------------------------------------------------------------

    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        //-----------------------------------------------------------------
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
        if ( viewportResizeNeeded ) {
            gl.glViewport(viewportXpos, viewportYpos,
                          viewportXsize, viewportYsize); 
            camera.updateViewportResize(viewportXsize, viewportYsize);
        }
        gl.glEnable(gl.GL_DEPTH_TEST);
        //-----------------------------------------------------------------
        if ( operationMode == MODE_3D ) {
            display3DSlices(gl);
        }
        else {
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glLoadIdentity();

            gl.glBegin(gl.GL_LINE_LOOP);
                gl.glVertex3d(-0.9, -0.9, 0);
                gl.glVertex3d(0.9, -0.9, 0);
                gl.glVertex3d(0.9, 0.9, 0);
                gl.glVertex3d(-0.9, 0.9, 0);
            gl.glEnd();
        }
    }

    public void actionPerformed(ActionEvent ev) {
        container.actionPerformed(ev);
    }

    public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        canvas.requestFocusInWindow();
    }

    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if ( cameraController.processMousePressedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        if ( cameraController.processMouseReleasedEventAwt(e) ) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
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

    public void keyPressed(KeyEvent e) {
        char unicode_id;
        int keycode;
        boolean skipKey = false;

        unicode_id = e.getKeyChar();
        keycode = e.getKeyCode();

        if ( cameraController.processKeyPressedEventAwt(e) ) {
            canvas.repaint();
            skipKey = true;
        }

        if ( unicode_id != e.CHAR_UNDEFINED && !skipKey ) {
            switch ( unicode_id ) {
              case '1':
                selectedSlice--;
                if ( selectedSlice < -1 ) {
                    selectedSlice = -1;
                }
                break;
              case '2':
                TimeTake timeTake;
                timeTake = parent.study.getTimeTake(parent.currentTimeTake);
                if ( timeTake != null ) {
                    selectedSlice++;
                    if ( selectedSlice >= timeTake.getNumSlices() ) {
                        selectedSlice = timeTake.getNumSlices()-1;
                    }
                }
                break;
              case ' ': // SPACE
                System.out.println("HERE, WE ARE SUPPOSED TO ROTATE OPERATION MODE...");
                break;
            }
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
