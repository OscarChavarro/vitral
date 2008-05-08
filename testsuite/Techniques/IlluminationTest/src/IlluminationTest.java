//===========================================================================

// Basic java classes
import java.io.File;
import java.io.IOException;

// AWT GUI java classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

// Swing GUI java classes
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.io.geometry.ReaderObj;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglTriangleMeshRenderer;

// Application classes
import util.filters.ObjectFilter;

/**
 */
public class IlluminationTest
    extends JFrame implements GLEventListener, MouseListener,
                   MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private Light light1;
    private Light light2;
    private Light light3;
    private CameraController cameraController;
    private RendererConfiguration qualitySelection1;
    private RendererConfiguration qualitySelection2;
    private RendererConfigurationController qualityController;
    private GLCanvas canvas;
    private Vector3D lightPosition;
    private boolean showVectors;
    private Vector3D N;
    private Vector3D H;
    private double phongExp;

    private TriangleMesh baseMesh;
    private TriangleMesh surfaceMesh;

    private int coord(int nx, int ny, int ix, int iy)
    {
        return ((nx+1)*iy) + ix;
    }

    private void updateH()
    {
        Vector3D L = new Vector3D(lightPosition);
        L.normalize();

        double mag = N.dotProduct(L) * 2;

        H = (N.multiply(mag)).substract(L);
    }

    private TriangleMesh createFloorMesh()
    {
        //-----------------------------------------------------------------
        // Parametric
        double dx = 0.2;  // Size of each tile in x direction
        double dy = 0.2;  // Size of each tile in y direction
        int nx = 50;      // Number of tiles in the x direction
        int ny = 50;      // Number of tiles in the y direction
        double iniz = 0.1; // Distance of the floor down of the z=0 plane

        // Temporary variable
        double x;
        double y;
        int ix;
        int iy;
        int index;

        TriangleMesh m = new TriangleMesh();

        //-----------------------------------------------------------------
        m.initVertexArrays((nx+1)*(ny+1));
        double vp[] = m.getVertexPositions();
        double vn[] = m.getVertexNormals();

        index = 0;
        for ( iy = 0, y = -((double)ny)/2*dy; iy <= ny; iy++, y += dy ) {
            for ( ix = 0, x = -((double)nx)/2*dx; ix <= nx; ix++, x += dx ) {
                vp[3*index] = x;
                vp[3*index+1] = y;
                vp[3*index+2] = 0;
                vn[3*index] = 0;
                vn[3*index+1] = 0;
                vn[3*index+2] = 1;
                index++;
            }
        }

        //-----------------------------------------------------------------
        m.initTriangleArrays(nx*ny*2);
        int t[] = m.getTriangleIndexes();

        index = 0;
        for ( iy = 0; iy < ny; iy++ ) {
            for ( ix = 0; ix < nx; ix++ ) {
                t[3*index] = coord(nx, ny, ix, iy);
                t[3*index+1] = coord(nx, ny, ix+1, iy);
                t[3*index+2] = coord(nx, ny, ix+1, iy+1);
                index++;

                t[3*index] = coord(nx, ny, ix, iy);
                t[3*index+1] = coord(nx, ny, ix+1, iy+1);
                t[3*index+2] = coord(nx, ny, ix, iy+1);
                index++;
            }
        }

        //-----------------------------------------------------------------
        m.calculateNormals();
        return m;
    }

    private TriangleMesh createSurfaceMesh()
    {
        //-----------------------------------------------------------------
        // Parametric
        int nx = 30;      // Number of tiles in the x direction
        int ny = 30;      // Number of tiles in the y direction
        double iniz = 0.1; // Distance of the floor down of the z=0 plane

        // Temporary variable
        double x;
        double y;
        int ix;
        int iy;
        int index;

        TriangleMesh m = new TriangleMesh();

        //- Prepare illumination model vectors ----------------------------
        Vector3D L = new Vector3D(lightPosition);
        L.normalize();

        //-----------------------------------------------------------------
        m.initVertexArrays((nx+1)*(ny+1));
        double v[] = m.getVertexPositions();
        double dtetha = 360.0 / ((double)ny);
        double dphi = 90.0 / ((double)nx);
        double tetha, phi, z;
        double r, x1, y1, z1, d, s;
        Vector3D E;

        index = 0;
        for ( iy = 0, tetha = 0; iy <= ny; iy++, tetha += dtetha ) {
            for ( ix = 0, phi = 0; ix <= nx; ix++, phi += dphi ) {

                // Constant (ambient)
                //r = 1;
                // Lambertian (diffuse) factor
                d = 1.5*Math.max(0.0, N.dotProduct(L));

                // Phong illumination model (specular) factor
                x1 = Math.cos(Math.toRadians(tetha)) *
                        Math.cos(Math.toRadians(phi));
                y1 = Math.sin(Math.toRadians(tetha)) *
                        Math.cos(Math.toRadians(phi));
                z1 = Math.cos(Math.toRadians(90-phi));
                E = new Vector3D(x1, y1, z1);
                E.normalize();
                s = Math.pow(H.dotProduct(E), phongExp);

                r = d + s;

                x = r * Math.cos(Math.toRadians(tetha)) *
                        Math.cos(Math.toRadians(phi));
                y = r * Math.sin(Math.toRadians(tetha)) *
                        Math.cos(Math.toRadians(phi));
                z = r * Math.cos(Math.toRadians(90-phi));

                v[3*index] = x;
                v[3*index+1] = y;
                v[3*index+2] = z;
                index++;
            }
        }

        //-----------------------------------------------------------------
        m.initTriangleArrays(nx*ny*2);
        int t[] = m.getTriangleIndexes();

        index = 0;
        for ( iy = 0; iy < ny; iy++ ) {
            for ( ix = 0; ix < nx; ix++ ) {
                if ( iy < ny - 1 ) {
                    t[3*index] = coord(nx, ny, ix, iy);
                    t[3*index+2] = coord(nx, ny, ix+1, iy);
                    t[3*index+1] = coord(nx, ny, ix+1, iy+1);
                    index++;

                    t[3*index] = coord(nx, ny, ix, iy);
                    t[3*index+2] = coord(nx, ny, ix+1, iy+1);
                    t[3*index+1] = coord(nx, ny, ix, iy+1);
                    index++;
                }
                else {
                    t[3*index] = coord(nx, ny, ix, iy);
                    t[3*index+2] = coord(nx, ny, ix+1, iy);
                    t[3*index+1] = coord(nx, ny, ix+1, 0);
                    index++;

                    t[3*index] = coord(nx, ny, ix, iy);
                    t[3*index+2] = coord(nx, ny, ix+1, 0);
                    t[3*index+1] = coord(nx, ny, ix, 0);
                    index++;
                }
            }
        }

        //-----------------------------------------------------------------
        m.calculateNormals();
        return m;
    }

    public IlluminationTest(String fileName) {
        super("VITRAL mesh test - JOGL");

        //-----------------------------------------------------------------
        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        qualitySelection1 = new RendererConfiguration();
        qualitySelection2 = new RendererConfiguration();
        qualityController = new RendererConfigurationController(qualitySelection1);

        lightPosition = new Vector3D(0, 0, 3);
        light1 = new Light(Light.POINT, lightPosition, 
                new ColorRgb(1, 1, 1));
        light1.setPosition(lightPosition);
        showVectors = false;



        light2 = new Light(Light.POINT, new Vector3D(30, -70, 50), 
                new ColorRgb(1, 1, 1));
        light3 = new Light(Light.POINT, new Vector3D(-40, 60, 30), 
                new ColorRgb(1, 1, 1));

        //-----------------------------------------------------------------
        N = new Vector3D(0, 0, 1); // For the floor!
        updateH();

        phongExp = 60.0;

        baseMesh = createFloorMesh();
        surfaceMesh = createSurfaceMesh();
    }

    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f;

        System.out.println("Press keys: 1, 2, 3, 4, 5, 6, 7, 8 and 0...");

        if ( args.length == 1 ) {
            f = new IlluminationTest(args[0]);
        }
        else {
            f = new IlluminationTest(null);
        }

        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL gl) {
        gl.glLoadIdentity();

/*
        // Draw reference frame
        gl.glLineWidth((float)3.0);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
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
*/
        // Draw mesh
        gl.glDisable(gl.GL_CULL_FACE);
        //gl.glCullFace(gl.GL_BACK);

        JoglLightRenderer.activate(gl, light1);
        JoglTriangleMeshRenderer.draw(gl, baseMesh, qualitySelection2, false);

        JoglLightRenderer.deactivate(gl, light1);
        JoglLightRenderer.activate(gl, light2);
        JoglLightRenderer.activate(gl, light3);
        JoglTriangleMeshRenderer.draw(gl, surfaceMesh, qualitySelection1, false);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        gl.glLineWidth(1.0f);
        
        JoglCameraRenderer.activate(gl, camera);

        JoglLightRenderer.turnOffAllLights(gl);

        drawObjectsGL(gl);

        gl.glDisable(gl.GL_LIGHTING);
        gl.glColor3d(1, 1, 0);
        JoglLightRenderer.draw(gl, light1);

        // red box around interest point over surface
        double z = 0.01;
        double l = 0.1;
        gl.glBegin(gl.GL_LINE_LOOP);
            gl.glColor3d(0.9, 0.5, 0.5);
            gl.glVertex3d(-l, -l, z);
            gl.glVertex3d(l, -l, z);
            gl.glVertex3d(l, l, z);
            gl.glVertex3d(-l, l, z);
        gl.glEnd();

        if ( showVectors == true ) {
            gl.glLineWidth(7.0f);

            gl.glBegin(gl.GL_LINES);
                gl.glColor3d(0.5, 0.5, 0.9);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(lightPosition.x, lightPosition.y, lightPosition.z);

                gl.glColor3d(0.9, 0.9, 0.5);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0, 0, 3);

                gl.glColor3d(0.5, 0.9, 0.5);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(3*H.x, 3*H.y, 3*H.z);

            gl.glEnd();
        }
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
        double deltalight = 0.1;

        //-----------------------------------------------------------------
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection1);
        }
        else if ( e.getKeyCode() == KeyEvent.VK_7 ) {
            phongExp += 5;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_8 ) {
            phongExp -= 5;
            if ( phongExp < 0.0 ) phongExp = 0.0;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_1 ) {
            lightPosition.x -= deltalight;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_2 ) {
            lightPosition.x += deltalight;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_3 ) {
            lightPosition.y -= deltalight;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_4 ) {
            lightPosition.y += deltalight;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_5 ) {
            lightPosition.z -= deltalight;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_6 ) {
            lightPosition.z += deltalight;
        }
        else if ( e.getKeyCode() == KeyEvent.VK_0 ) {
            if ( showVectors == true ) {
                showVectors = false;
            }
            else {
                showVectors = true;
            }
        }

        System.out.println("Phong exp: " + phongExp);
        System.out.println("Light: " + lightPosition);

        surfaceMesh = createSurfaceMesh();
        light1.setPosition(lightPosition);
        updateH();
        canvas.repaint();

        //-----------------------------------------------------------------
        if ( cameraController.processKeyPressedEventAwt(e) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEventAwt(e) ) {
            System.out.println(qualitySelection1);
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
