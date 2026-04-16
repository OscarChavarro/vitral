// Java basic classes
import java.util.ArrayList;

// Java Swing / Awt classes
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.render.HiddenLineRenderer;
import vsdk.toolkit.render.jogl.JoglRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidRenderer;
import vsdk.toolkit.processing.GeometricModeler;

@SuppressWarnings("removal")
public class PolyhedralBoundedSolidExample extends Applet implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private Material material;
    private Light light1;
    private Light light2;
    private PolyhedralBoundedSolid solid;
    private int faceIndex = -2;
    private int edgeIndex = -2;
    private boolean debugVertices = false;

    private RendererConfiguration quality;
    private RendererConfigurationController qualityController;
    private CameraController cameraController;
    private GLCanvas canvas;
    private int solidType = 23;
    private int csgOperation = 0;
    private int csgSample = 5;
    private boolean debugEdges = false;
    private boolean showCoordinateSystem = true;
    private boolean debugCsg = false;

    public PolyhedralBoundedSolidExample() {
        camera = new Camera();
        camera.setPosition(new Vector3D(2, -1, 2));
        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(135), Math.toRadians(-35), 0);
        camera.setRotation(R);

        quality = new RendererConfiguration();
        qualityController = new RendererConfigurationController(quality);
        cameraController = new CameraControllerAquynza(camera);

        material = defaultMaterial();
        light1 = new Light(Light.POINT, new Vector3D(3, -3, 2), new ColorRgb(1, 1, 1));
        light2 = new Light(Light.POINT, new Vector3D(-2, 5, -2), new ColorRgb(0.9, 0.5, 0.5));

        //- Solid building ------------------------------------------------
        solid = buildSolid(solidType);
    }

    private Material defaultMaterial()
    {
        Material m = new Material();

        m.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        m.setDiffuse(new ColorRgb(0.5, 0.5, 0.9));
        m.setSpecular(new ColorRgb(1, 1, 1));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }

    private PolyhedralBoundedSolid buildSolid(int type)
    {
        PolyhedralBoundedSolid mySolid;
        Matrix4x4 T, R, S, M;

        switch ( type % 24 ) {
          case 0:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(0.1, 0.1, 0.1), 1, 1);
            mySolid.smev(1, 1, 4, new Vector3D(0.1, 1, 0.1));
            mySolid.smev(1, 4, 3, new Vector3D(1, 1, 0.1));
            break;
          case 1:
            mySolid = PolyhedralBoundedSolidModelingTools.createBox(new Vector3D(0.9, 0.9, 0.9));
            break;
          case 3:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            GeometricModeler.addArc(
                mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 9);
            break;
          case 4:
            mySolid = GeometricModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 12
            );
            break;
          case 5:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            GeometricModeler.addArc(
                mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 18);

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));

            GeometricModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), M);

            break;
          case 6:
            mySolid = GeometricModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 24
            );

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));
            GeometricModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), M);

/*
            T = new Matrix4x4();
            T.translation(0.1, 0.1, 1.0);
            R = new Matrix4x4();
            //R.axisRotation(Math.toRadians(15), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.2, 0.2, 0.2);
            M = T.multiply(R.multiply(S));
            GeometricModeler.translationalSweepExtrudeFace(
                solid, solid.findFace(1), M);
*/


            break;

          case 7:
            mySolid = PolyhedralBoundedSolidModelingTools.createSphere(0.5);
            break;
          case 8:
            mySolid = PolyhedralBoundedSolidModelingTools.createCone(0.5, 0.0, 1.0);
            break;
          case 9:
            mySolid = PolyhedralBoundedSolidModelingTools.createArrow(0.7, 0.3, 0.05, 0.1);
            break;
          case 10:
            mySolid = PolyhedralBoundedSolidModelingTools.createLaminaWithTwoShells();
            break;
          case 11:
            mySolid = PolyhedralBoundedSolidModelingTools.createLaminaWithHole();
            break;
          case 12:
            mySolid = PolyhedralBoundedSolidModelingTools.createFontBlock("../../../../samples/fonts/microsoftArial.ttf", "\u7c8b\u00e1\u00d1\u3055\u3042\u307d");

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.1);

            GeometricModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), T);

            break;
          case 13:
            mySolid = PolyhedralBoundedSolidModelingTools.createGluedCilinders();
            break;
          case 14:
            mySolid = PolyhedralBoundedSolidModelingTools.eulerOperatorsTest();
            break;
          case 15:
            mySolid = PolyhedralBoundedSolidModelingTools.rotationalSweepTest();
            break;
          case 16:
            mySolid = PolyhedralBoundedSolidModelingTools.splitTest(1);
            break;
          case 17:
            mySolid = PolyhedralBoundedSolidModelingTools.splitTest(2);
            break;
          case 18:
            mySolid = PolyhedralBoundedSolidModelingTools.splitTest(3);
            break;
          case 19:
            mySolid = PolyhedralBoundedSolidModelingTools.csgTest(1, csgOperation, csgSample, debugCsg);
            debugCsg = false;
            break;
          case 20:
            mySolid = PolyhedralBoundedSolidModelingTools.csgTest(2, csgOperation, csgSample, debugCsg);
            debugCsg = false;
            break;
          case 21:
            mySolid = PolyhedralBoundedSolidModelingTools.csgTest(3, csgOperation, csgSample, debugCsg);
            debugCsg = false;
            break;
          case 22:
            mySolid = PolyhedralBoundedSolidModelingTools.featuredObject();
            break;
          case 23:
	    File fd = new File("/tmp/solid.bin");
	    if ( fd.exists() ) {
	        mySolid = importFromFile("/tmp/solid.bin");
	    }
	    else {
                mySolid = PolyhedralBoundedSolidModelingTools.featuredObject();
	    }
            break;
          case 2: default:
            mySolid = PolyhedralBoundedSolidModelingTools.createHoledBox();
            break;
        }

        return mySolid;
    }

    private GLCanvas createGUI()
    {
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        return canvas;
    }

    public static void main (String[] args) {
        JoglRenderer.verifyOpenGLAvailability();
        PolyhedralBoundedSolidExample instance = new PolyhedralBoundedSolidExample();
        JFrame frame = new JFrame("VITRAL concept test - Polyhedral bounded solid example");

        GLCanvas canvas = instance.createGUI();

        frame.add(canvas, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension size = new Dimension(1024, 768);
        //Dimension size = new Dimension(1366, 768);
        //frame.setMinimumSize(size);
        frame.setSize(size);
        frame.setVisible(true);
        canvas.requestFocusInWindow();
    }

    @Override
    public void init()
    {
        setLayout(new BorderLayout());
        add("Center", createGUI());
    }

    private void
    renderLinesResult(GL2 gl, ArrayList <Vector3D> contourLines,
                      ArrayList <Vector3D> visibleLines,
                      ArrayList <Vector3D> hiddenLines)
    {
        int i;
        Vector3D p;

        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        gl.glLineWidth(4.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(GL2.GL_LINES);
        for ( i = 0; i < contourLines.size(); i++ ) {
            p = contourLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();

        gl.glLineWidth(4.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(GL2.GL_LINES);
        for ( i = 0; i < visibleLines.size(); i++ ) {
            p = visibleLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();

/*
        gl.glLineWidth(1.0f);
        gl.glColor3d(0, 0, 0);
        gl.glBegin(gl.GL_LINES);
        for ( i = 0; i < hiddenLines.size(); i++ ) {
            p = hiddenLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();
*/
        //-----------------------------------------------------------------
/*
        gl.glPointSize(4.0f);
        gl.glColor3d(0.5, 0.5, 0.9);
        gl.glBegin(gl.GL_POINTS);
        for ( i = 0; i < contourLines.size(); i++ ) {
            p = contourLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        for ( i = 0; i < visibleLines.size(); i++ ) {
            p = visibleLines.get(i);
            gl.glVertex3d(p.x, p.y, p.z);
        }
        gl.glEnd();
*/
        //-----------------------------------------------------------------
        gl.glPopAttrib();
    }

    private void drawObjectsGL(GL2 gl)
    {
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth((float)3.0);

        if ( edgeIndex > -3 && showCoordinateSystem ) {
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

        //-----------------------------------------------------------------
        JoglMaterialRenderer.activate(gl, material);
        JoglLightRenderer.activate(gl, light1);
        JoglLightRenderer.draw(gl, light1);
        JoglLightRenderer.activate(gl, light2);
        JoglLightRenderer.draw(gl, light2);
        gl.glEnable(GL2.GL_LIGHTING);
        JoglPolyhedralBoundedSolidRenderer.draw(gl, solid, camera, quality);
        JoglPolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl, solid, faceIndex);
        JoglPolyhedralBoundedSolidRenderer.drawDebugFace(gl, solid, faceIndex);
        if ( debugVertices ) {
            JoglPolyhedralBoundedSolidRenderer.drawDebugVertices(gl, solid, camera);
        }

        //-----------------------------------------------------------------
        ArrayList <Vector3D> contourLines;
        ArrayList <Vector3D> visibleLines;
        ArrayList <Vector3D> hiddenLines;
        ArrayList <SimpleBody> bodyArray;
        SimpleBody body;

        if ( debugEdges && edgeIndex > -3 ) {
            JoglPolyhedralBoundedSolidRenderer.drawDebugEdges(gl, solid, camera, edgeIndex);
        }
        else if ( edgeIndex == -3 ) {
            contourLines = new ArrayList <Vector3D>();
            visibleLines = new ArrayList <Vector3D>();
            hiddenLines = new ArrayList <Vector3D>();
            bodyArray = new ArrayList <SimpleBody>();

            body = new SimpleBody();
            body.setGeometry(solid);
            body.setPosition(new Vector3D());
            body.setRotation(new Matrix4x4());
            body.setRotationInverse(new Matrix4x4());
            bodyArray.add(body);
            HiddenLineRenderer.executeAppelAlgorithm(bodyArray, camera,
                contourLines, visibleLines, hiddenLines);
            renderLinesResult(gl, contourLines, visibleLines, hiddenLines);
        }

        /*
        contourLines = null;
        visibleLines = null;
        hiddenLines = null;
        bodyArray = null;
        body = null;
        */
    }

    /** Called by drawable to initiate drawing
    @param drawable 
    */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        JoglCameraRenderer.activate(gl, camera);

        drawObjectsGL(gl);
    }
   
    /** Not used method, but needed to instanciate GLEventListener
    @param drawable 
    */
    @Override
    public void init(GLAutoDrawable drawable) {
        
    }

    /** Not used method, but needed to instanciate GLEventListener
    @param drawable 
    */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        
    }

    /** Not used method, but needed to instanciate GLEventListener
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
    @param e
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
        if ( qualityController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            System.out.println(quality);
            canvas.repaint();
        }

        int unicode_id = e.getKeyChar();
        if ( unicode_id != KeyEvent.CHAR_UNDEFINED ) {
            switch ( unicode_id ) {
            case '0':
                debugEdges = !debugEdges;
                break;
            case ' ':
                showCoordinateSystem = !showCoordinateSystem;
                break;
            case '1':
                faceIndex--;
                break;
            case '2':
                faceIndex++;
                break;
            case '8':
                edgeIndex--;
                break;
            case '9':
                edgeIndex++;
                break;
            case 'I':
                System.out.println(solid);
                if ( solid.validateModel() ) {
                    System.out.println("SOLID MODEL IS VALID!");
                }
                else {
                    System.out.println("SOLID MODEL IS INVALID!");
                }
                break;

            case '3':
                solidType--;
                if ( solidType < 0 ) {
                    solidType = 0;
                }
                solid = buildSolid(solidType);
                break;

            case '4':
                solidType++;
                solid = buildSolid(solidType);
                break;

            case '5':
                csgOperation++;
                if ( csgOperation > 3 ) {
                    csgOperation = 0;
                }
                solid = buildSolid(solidType);
                break;

            case '6':
                csgSample++;
                if ( csgSample > 7 ) {
                    csgSample = 0;
                }
                solid = buildSolid(solidType);
                break;

            case 'v':
                debugVertices = !debugVertices;
                break;

            case 'd':
                debugCsg = !debugCsg;
                solid = buildSolid(solidType);
                break;

            }
            if ( faceIndex < -2 ) {
                faceIndex = -2;
            }
            if ( edgeIndex < -3 ) {
                edgeIndex = -3;
            }
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
    will be invoked twice for
    @param e each key. Call it only from the `keyPressed` and
    `keyReleased` method
    */
    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    private PolyhedralBoundedSolid importFromFile(String filename) {
        PolyhedralBoundedSolid mysolid = null;
        
        try {
            File fd = new File(filename);
            FileInputStream fis;
            fis = new FileInputStream(fd);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mysolid = (PolyhedralBoundedSolid)ois.readObject();
            
            fis.close();
        }
        catch ( IOException e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, 
              "importFromFile", "Error reading solid from file " + filename, e);
        }
        catch ( ClassNotFoundException e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR,
              "importFromFile", "Error reading solid from file " + filename, e);
        }
        
        return mysolid;
    }

}
