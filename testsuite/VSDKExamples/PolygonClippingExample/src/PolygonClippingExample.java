// Basic java classes

import java.io.File;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

// Swing GUI java classes
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;    // Model elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.render.jogl.JoglCameraRenderer; // View elements
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.gui.CameraController;           // Control elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl._JoglPolygonTesselatorRoutines;
//Polygon simplify.
import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.environment.geometry.Polygon2D;
import vsdk.toolkit.environment.geometry._Polygon2DContour;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.WeilerAthertonPolygonClipper;
import vsdk.toolkit.processing._DoubleLinkedListNode;
import vsdk.toolkit.processing._Polygon2DContourWA;
import vsdk.toolkit.processing._Polygon2DWA;
import vsdk.toolkit.processing._VertexNode2D;

//geniousCity clases
//import geniousCity.toolkit.io.ESRIShapePersistence;
// Application classes
//import util.filters.ObjectFilter;
public class PolygonClippingExample
    extends JFrame implements GLEventListener, MouseListener,
    MouseMotionListener, MouseWheelListener, KeyListener {

    private final Camera camera;
    private final Light light;
    private final CameraController cameraController;
    private final RendererConfiguration qualitySelection;
    private final RendererConfigurationController qualityController;
    //public GLCanvas canvas;
    public GLJPanel canvas;

    private SimpleScene scene;
    private AnimationEventGenerator animator;
    //private JoglMd2MeshRenderer Md2MeshRend = new JoglMd2MeshRenderer();

    public double x;
    Vertex2D p0, p1, p2;
//    private _Polygon2DContour globP2DContour = new _Polygon2DContour();
//    private _Polygon2DContour globP2DContourSimp;
//    private Polygon2D globP2D, globP2DSimp;
//    float epsilon = 0.01f;
    private Polygon2D globP2DClip, globP2DSubject;
    private Polygon2D innerPoly;
    private Polygon2D outerPoly;
    private _Polygon2DWA clipPolyWA;
    private _Polygon2DWA subjectPolyWA;
    //Tessellation.
    private static GLU glu;
    private static _JoglPolygonTesselatorRoutines tesselatorProcessor;

    static {
        tesselatorProcessor = null;
    }
    ////Tessellation.
    byte testNumber = 0;
    int numSnapshot = 1;
    boolean takeSnapshot = false;

    @SuppressWarnings("LeakingThisInConstructor")
    public PolygonClippingExample(String fileName) {
        super("VITRAL mesh test - JOGL");
        File file = null;
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCaps = new GLCapabilities(glp);
        String texture;

        ///Clip poligons WA.
        testClipPolygons();
//        globP2DClip = new Polygon2D();
//        globP2DSubject = new Polygon2D();

//        globp2DWAClip.addVertex(3, 0);
//        globp2DWAClip.addVertex(6, 4);
//        globp2DWAClip.addVertex(2, 6);
//        globp2DWASubject.addVertex(3, 3);
//        globp2DWASubject.addVertex(8, 1);
//        globp2DWASubject.addVertex(9, 5);
//        globp2DWASubject.addVertex(7, 7);
//        globp2DWASubject.nextLoop(); //agujero.
//        globp2DWASubject.addVertex(4,3);
//        globp2DWASubject.addVertex(6,5);
//        globp2DWASubject.addVertex(8,3);
//        //globp2DWASubject.addVertex(7,2);
//        for(i=0;i<clipPolygonGlob.length/2;++i)
//            globP2DClip.addVertex(clipPolygonGlob[i*2], clipPolygonGlob[i*2+1]);
//        for(i=0;i<subjPolygonGlob.length/2;++i)
//            globP2DSubject.addVertex(subjPolygonGlob[i*2], subjPolygonGlob[i*2+1]);
        //Agujero.
//        globP2DSubject.nextLoop();
//        globP2DSubject.addVertex(2.5,2);
//        globP2DSubject.addVertex(2,3);
//        globP2DSubject.addVertex(3,2);
//        clipPolygons(globP2DClip,globP2DSubject);
        //Clip poligons WA.
        ///Temp polygon simplification  LRR
//        p0 = new Vertex2D(2,2);
//        p1 = new Vertex2D(7,2);
//        p2 = new Vertex2D(5,5);
//        x = 0;
//        for(i=0;i<polygon.length/2;++i){
////            globP2DContour.addVertex((polygon[i*2]-6.886384)*10, (polygon[i*2+1]-53.345787)*10);
//            globP2DContour.addVertex((polygon[i*2]-4.135277)*10, (polygon[i*2+1]-51.993984)*10);
//        }
//        globP2DContourSimp = polygon2DContourSimplify(globP2DContour,epsilon,false);
//        globP2D = ESRIShapePersistence.readPolygon("C:\\Politecnica\\netbeans_workspace\\11_ESRIShapeImporterTool\\polygon000.bin");
//        globP2DSimp = polygon2DSimplify(globP2D,epsilon,false);
        //Temp polygon simplification  LRR
//        scene = new SimpleScene();
//
//
//        
//        
//        if ( fileName == null ) {
//            JFileChooser jfc = null;
//
//            jfc = new JFileChooser( (new File("")).getAbsolutePath() + "/../../../etc/geometry");
//
//            jfc.removeChoosableFileFilter(jfc.getFileFilter());
//            jfc.addChoosableFileFilter(new ObjectFilter("obj", "Obj Files"));
//            jfc.addChoosableFileFilter(new ObjectFilter("3ds", "3ds Files"));
//            jfc.addChoosableFileFilter(new ObjectFilter("ply", "Ply Files"));
//            jfc.addChoosableFileFilter(new ObjectFilter("ase", "3ds Files (Ascii Scene Export)"));
//            jfc.addChoosableFileFilter(new ObjectFilter("vtk", "kitware's VTK legacy binary file"));
//            int opc = jfc.showOpenDialog(new JPanel());
//
//            if ( opc == JFileChooser.APPROVE_OPTION ) {
//                file = jfc.getSelectedFile();
//            }
//        }
//        else {
//            file = new File(fileName);
//        }
//
//        //-----------------------------------------------------------------
//        if ( file != null ) {
//            try {
//                EnvironmentPersistence.importEnvironment(file, scene);
//
//// Trivial mesh creation (need to change TriangleMeshGroup by TriangleMesh):
///*
//        Vertex v[] = new Vertex[3];
//        v[0] = new Vertex(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1));
//        v[1] = new Vertex(new Vector3D(1, 0, 0), new Vector3D(0, 0, 1));
//        v[2] = new Vertex(new Vector3D(1, 1, 0), new Vector3D(0, 0, 1));
//
//        Triangle t[] = new Triangle[1];
//        t[0] = new Triangle(0, 1, 2);
//
//        mesh = new TriangleMesh();
//        mesh.setVertexes(v);
//        mesh.setTriangles(t);
//        mesh.calculateNormals();
//*/
//            }
//            catch ( Exception ex ) {
//                System.err.println("Failed to read file.");
//                ex.printStackTrace();
//                System.exit(0);
//            }
//        }
//        else {
//            System.err.println("File not specified");
//            System.exit(0);
//        }
        //-----------------------------------------------------------------
        //canvas = new GLCanvas();
        canvas = new GLJPanel(glCaps);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        this.add(canvas, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        qualitySelection = new RendererConfiguration();
        qualityController = new RendererConfigurationController(qualitySelection);

        light = new Light(Light.POINT, new Vector3D(10, -20, 50), new ColorRgb(1, 1, 1));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f;

        if ( args.length == 1 ) {
            f = new PolygonClippingExample(args[0]);
        } else {
            f = new PolygonClippingExample(null);
        }

//        Animador h = new Animador((PolygonClippingExample)f);
//        Thread x = new Thread(h);
        //x.start();
        f.pack();
        f.setVisible(true);
    }

    private void drawObjectsGL(GL2 gl) {
        int i;
//        float[] LightPosition = new float[4];
//        float[] LightAmbient = new float[4];
//        float[] LightDiffuse = new float[4];

        gl.glLoadIdentity();

        gl.glTranslated(x, 0, 0);

        // Draw reference frame
        gl.glLineWidth((float) 3.0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
//        gl.glBegin(GL2.GL_LINES);
//            gl.glColor3d(1, 0, 0);
//            gl.glVertex3d(0, 0, 0);
//            gl.glVertex3d(1, 0, 0);
//
//            gl.glColor3d(0, 1, 0);
//            gl.glVertex3d(0, 0, 0);
//            gl.glVertex3d(0, 1, 0);
//
//            gl.glColor3d(0, 0, 1);
//            gl.glVertex3d(0, 0, 0);
//            gl.glVertex3d(0, 0, 1);
//        gl.glEnd();

        ///Temp polygon simplification  LRR
        gl.glDisable(GL2.GL_DEPTH_TEST);
        drawTest(gl);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        //Polygon simplification

        // Draw mesh
        gl.glDisable(GL2.GL_CULL_FACE);
        //gl.glCullFace(gl.GL_BACK);

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        //gl.glEnable(GL2.GL_LIGHT1);
//        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, LightAmbient);
//        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, LightDiffuse);
//        LightPosition[0] = 200;
//        LightPosition[1] = 200;
//        LightPosition[2] = 0;
//        LightPosition[3] = 0;
//        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, LightPosition,1);

        float pos[] = {200f, 200f, 0f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, pos, 0);
        float dif[] = {1.0f, 1.0f, 1.0f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
        float amb[] = {0.7f, 0.7f, 0.7f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);

        //JoglMd2MeshRenderer.draw(gl,md2Mesh); //LRR
//        JoglSimpleBodyRenderer.setAutomaticDisplayListManagement(true);
//        for ( i = 0; i < scene.getSimpleBodies().size(); i++ ) {
//            JoglSimpleBodyRenderer.drawWithVertexArrays(gl, scene.getSimpleBodies().get(i), camera, qualitySelection);
//        }
    }

    private void drawTest(GL2 gl) {
        int i,j;
        _DoubleLinkedListNode<_VertexNode2D> iterator, first;
        double minMaxClip[] = new double[6];
        double minMaxSubject[] = new double[6];

//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(-1,-1),new VertexNode2D(1,0),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(-1,-1),new VertexNode2D(-1,-2),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(-1,-1),new VertexNode2D(-2,1),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(1,-1),new VertexNode2D(1,1),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(1,-1),new VertexNode2D(1,-2),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(1,-1),new VertexNode2D(-1,-1),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(-1,-1),new VertexNode2D(-2,-2),new VertexNode2D(0,1));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(-1,-1),new VertexNode2D(-1,-2),new VertexNode2D(-2,-4));
//        i=isOrderedCounterclockwiseVectors2D(new VertexNode2D(0,5),new VertexNode2D(1,0),new VertexNode2D(0,1));
        //intersecLineLine2D();
        if ( clipPolyWA != null ) {
            gl.glColor3d(0.2, 0.8, 0.2);
//            for ( _Polygon2DContourWA p2DClip : clipPolyWA.loops ) {
            for ( i=0; i < clipPolyWA.loops.size(); ++i ) {
                _Polygon2DContourWA p2DClip = clipPolyWA.loops.get(i);
                gl.glBegin(GL2.GL_LINE_LOOP);
                first = p2DClip.vertices.getHead();
                iterator = first;
                do {
                    gl.glVertex3d(iterator.data.x, 0, iterator.data.y);
                    iterator = iterator.next;
                } while ( iterator != first );
                gl.glEnd();
            }
            gl.glPointSize(10);
            gl.glBegin(GL2.GL_POINTS);
//            for ( _Polygon2DContourWA p2DClip : clipPolyWA.loops ) {
            for ( i = 0; i < clipPolyWA.loops.size(); ++i ) {
                _Polygon2DContourWA p2DClip = clipPolyWA.loops.get(i);
                first = p2DClip.vertices.getHead();
                iterator = first;
                do {
                    if ( iterator.data.pairNode == null ) {
                        gl.glColor3d(0.7, 0.1, 0.4);
                    } else {
                        gl.glColor3d(0.2, 0.4, 0.2); //Is cut.
                    }
                    gl.glVertex3d(iterator.data.x, 0, iterator.data.y);
                    iterator = iterator.next;
                } while ( iterator != first );
            }
            gl.glEnd();
        }

        if ( subjectPolyWA != null ) {
            gl.glColor3d(0.7, 0.8, 0.2);
            for ( i = 0; i < subjectPolyWA.loops.size(); ++i ) {
                _Polygon2DContourWA p2DContour = subjectPolyWA.loops.get(i);
                gl.glBegin(GL2.GL_LINE_LOOP);
                first = p2DContour.vertices.getHead();
                iterator = first;
                do {
                    gl.glVertex3d(iterator.data.x, 0, iterator.data.y);
                    iterator = iterator.next;
                } while ( iterator != first );
                gl.glEnd();
            }
            gl.glPointSize(6);
            gl.glBegin(GL2.GL_POINTS);
//            for ( _Polygon2DContourWA p2DContour : subjectPolyWA.loops ) {
            for ( i = 0; i < subjectPolyWA.loops.size(); ++i ) {
                _Polygon2DContourWA p2DContour = subjectPolyWA.loops.get(i);
                first = p2DContour.vertices.getHead();
                iterator = first;
                do {
                    if ( iterator.data.pairNode == null ) {
                        gl.glColor3d(0.8, 0.6, 0.4);
                    } else {
                        gl.glColor3d(0.2, 1, 0.2); //Is cut.
                    }
                    gl.glVertex3d(iterator.data.x, 0, iterator.data.y);
                    iterator = iterator.next;
                } while ( iterator != first );
            }
            gl.glEnd();
        }
        //Cliped polygons.
        if ( globP2DClip != null ) {
            minMaxClip = globP2DClip.getMinMax();
        }
        if ( globP2DSubject != null ) {
            minMaxSubject = globP2DSubject.getMinMax();
        }
        double yMax = 0, yMin = 0, xMax = 0, xMin = 0;
        if ( globP2DClip == null ) {
            if ( globP2DSubject != null ) {
                yMax = minMaxSubject[4];
                yMin = minMaxSubject[1];
                xMax = minMaxSubject[3];
                xMin = minMaxSubject[0];
            }

        } else {
            if ( globP2DSubject != null ) {
                yMax = minMaxClip[4] > minMaxSubject[4] ? minMaxClip[4] : minMaxSubject[4];
                yMin = minMaxClip[1] < minMaxSubject[1] ? minMaxClip[1] : minMaxSubject[1];
                xMax = minMaxClip[3] > minMaxSubject[3] ? minMaxClip[3] : minMaxSubject[3];
                xMin = minMaxClip[0] < minMaxSubject[0] ? minMaxClip[0] : minMaxSubject[0];
            } else {
                yMax = minMaxClip[4];
                yMin = minMaxClip[1];
                xMax = minMaxClip[3];
                xMin = minMaxClip[0];
            }
        }

        gl.glTranslated(0, 0, -(yMax - yMin) * 1.2);
        if ( innerPoly != null ) {
            gl.glColor3d(0.6, 0.6, 0.6);
            drawWithTesselator(gl, innerPoly);
            gl.glColor3d(0.8, 0.6, 0.4);
//            for ( _Polygon2DContour p2DContour : innerPoly.loops ) {
            for ( i = 0; i < innerPoly.loops.size(); ++i ) {
                _Polygon2DContour p2DContour = innerPoly.loops.get(i);
                gl.glBegin(GL2.GL_LINE_LOOP);
//                for ( Vertex2D vertex : p2DContour.vertices ) {
                for ( j = 0; j < p2DContour.vertices.size(); ++j ) {
                    Vertex2D vertex = p2DContour.vertices.get(j);
                    gl.glVertex3d(vertex.x, 0, vertex.y);
                }
                gl.glEnd();
            }
        }
        if ( outerPoly != null ) {
            if ( globP2DClip != null ) {
                gl.glTranslated((xMax - xMin) * 1.1, 0, 0);
            } else {
                gl.glTranslated(globP2DSubject.getMinMax()[3] * 1.1, 0, 0);//Pending: subject and clip width.
            }
            gl.glColor3d(0.6, 0.7, 0.6);
            drawWithTesselator(gl, outerPoly);
            gl.glColor3d(0.2, 1, 0.2);
//            for ( _Polygon2DContour p2DContour : outerPoly.loops ) {
            for ( i = 0; i < outerPoly.loops.size(); ++i ) {
                _Polygon2DContour p2DContour = outerPoly.loops.get(i);
                gl.glBegin(GL2.GL_LINE_LOOP);
//                for ( Vertex2D vertex : p2DContour.vertices ) {
                for ( j = 0; j < p2DContour.vertices.size(); ++j ) {
                    Vertex2D vertex = p2DContour.vertices.get(j);
                    gl.glVertex3d(vertex.x, 0, vertex.y);
                }
                gl.glEnd();
            }
        }
//        gl.glBegin(GL2.GL_LINE_LOOP);
//            gl.glColor3d(0.2, 0.8, 0.2);
//            for(i=0;i<globP2DContour.vertices.size();++i){
//                gl.glVertex3d(globP2DContour.vertices.get(i).x, 0, globP2DContour.vertices.get(i).y);
//            }
//        
//        gl.glEnd();
//        gl.glBegin(GL2.GL_LINE_LOOP);
//            gl.glColor3d(0.9, 0.8, 0.2);
//            for(i=0;i<globP2DContourSimp.vertices.size();++i){
//                gl.glVertex3d(globP2DContourSimp.vertices.get(i).x, 0, globP2DContourSimp.vertices.get(i).y);
//            }
//        gl.glEnd();
//
//        gl.glBegin(GL2.GL_LINE_LOOP);
//            gl.glColor3d(0.2, 0.8, 0.2);
//            for(_Polygon2DContour p2DContour : globP2D.loops) {
//                for(i=0;i<p2DContour.vertices.size();++i){
//                    gl.glVertex3d(p2DContour.vertices.get(i).x + 4150, 0, p2DContour.vertices.get(i).y/1000);
//                }
//            }
//        gl.glEnd();
//        gl.glBegin(GL2.GL_LINE_LOOP);
//            gl.glColor3d(0.9, 0.8, 0.2);
//            for(_Polygon2DContour p2DContour : globP2DSimp.loops) {
//                for(i=0;i<p2DContour.vertices.size();++i){
//                    gl.glVertex3d(p2DContour.vertices.get(i).x + 4150, 0, p2DContour.vertices.get(i).y/1000);
//                }
//            }
//        gl.glEnd();

//        for(_Polygon2DContour p2DContour : globP2DClip.loops) {
//            gl.glBegin(GL2.GL_LINE_LOOP);
//                for(Vertex2D vertex : p2DContour.vertices) {
//                    gl.glVertex3d(vertex.x, 0, vertex.y);
//                }
//            gl.glEnd();
//        }
//        gl.glColor3d(0.8, 0.6, 0.4);
//        for(_Polygon2DContour p2DContour : globP2DSubject.loops) {
//            gl.glBegin(GL2.GL_LINE_LOOP);
//                for(Vertex2D vertex : p2DContour.vertices) {
//                    gl.glVertex3d(vertex.x, 0, vertex.y);
//                }
//            gl.glEnd();
//        }
    }

    private void drawWithTesselator(GL2 gl, Polygon2D poly) {
        // Tesselator preparation for current face
        GLUtessellator tesselator;
        double list[][]; // JOGL GLU Tesselator needs a vertex memory

        //- Prepare tesselator --------------------------------------------
        if ( tesselatorProcessor == null ) {
            glu = new GLU();
            tesselatorProcessor
                = new _JoglPolygonTesselatorRoutines(gl, glu);
        }

        tesselator = GLU.gluNewTess();
        GLU.gluTessCallback(tesselator,
            GLU.GLU_TESS_VERTEX, tesselatorProcessor);
        GLU.gluTessCallback(tesselator,
            GLU.GLU_TESS_BEGIN, tesselatorProcessor);
        GLU.gluTessCallback(tesselator,
            GLU.GLU_TESS_END, tesselatorProcessor);
        GLU.gluTessCallback(tesselator,
            GLU.GLU_TESS_ERROR, tesselatorProcessor);
        GLU.gluTessBeginPolygon(tesselator, null);

        _Polygon2DContour p2DContour;
        int i;
        for ( i = 0; i < poly.loops.size(); i++ ) {
            GLU.gluTessBeginContour(tesselator);
            p2DContour = poly.loops.get(i);
            list = new double[p2DContour.vertices.size()][3];
            Vertex2D vertex;
            for ( int j = 0; j < p2DContour.vertices.size(); j++ ) {
                vertex = p2DContour.vertices.get(j);
                list[j][0] = vertex.x;
                list[j][1] = 0;
                list[j][2] = vertex.y;
                GLU.gluTessVertex(tesselator, list[j], 0, list[j]);
            }

            GLU.gluTessEndContour(tesselator);
        }
        GLU.gluTessEndPolygon(tesselator);
        GLU.gluDeleteTess(tesselator);
    }

    /**
     * Called by drawable to initiate drawing
     *
     * @param drawable
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(1f, 1f, 1f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        JoglCameraRenderer.activate(gl, camera);
        JoglLightRenderer.activate(gl, light);

        drawObjectsGL(gl);
        if ( takeSnapshot ) {
            takeSnapshot = false;
            RGBImage snapshot;
            snapshot = JoglRGBImageRenderer.getImageJOGL(gl);
            ImagePersistence.exportPNG(new File("frame" + VSDK.formatNumberWithinZeroes(numSnapshot, 4) + ".png"), snapshot);
            ++numSnapshot;
        }
    }

    /**
     * Not used method, but needed to instantiate GLEventListener
     *
     * @param drawable
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        //JoglMd2MeshRenderer.initGL(drawable.getGL().getGL2(), md2Mesh);
        //createAnimator(md2Mesh, canvas);
    }

//    public void createAnimator(Md2Mesh md2Mesh, GLJPanel gljp)
//    {
//       //- Set up animation control thread -----------------------------------
//       animator = new AnimationEventGenerator();
//       Thread t  = new Thread(animator);
//       t.start();
//
//       // This is causing to repeat the canvas repaint!
//
//       //streetSelectorListener = new StreetSelectorAnimationListener(c, m);
//       //animator.addAnimationListener(streetSelectorListener);
//
//       Md2AnimationListener testListener = new Md2AnimationListener(gljp, md2Mesh);
//       animator.addAnimationListener(testListener);
//    }
    /**
     * Not used method, but needed to instantiate GLEventListener
     *
     * @param drawable
     * @param a
     * @param b
     */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {

    }

    /**
     * Called to indicate the drawing surface has been moved and/or resized
     *
     * @param drawable
     * @param x
     * @param y
     * @param width
     * @param height
     */
    @Override
    public void reshape(GLAutoDrawable drawable,
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

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int numTestPoly, temp;

        if ( e.getKeyCode() == KeyEvent.VK_1 ) {
            numTestPoly = clipPolygonsGlobTest.length;
            if ( testNumber == 0 ) {
                testNumber = (byte) (numTestPoly - 1);
            } else {
                --testNumber;
            }
            testClipPolygons();
            canvas.repaint();
        }
        if ( e.getKeyCode() == KeyEvent.VK_2 ) {
            numTestPoly = clipPolygonsGlobTest.length;
            if ( testNumber == numTestPoly - 1 ) {
                testNumber = 0;
            } else {
                ++testNumber;
            }
            testClipPolygons();
            canvas.repaint();
        }
        if ( e.getKeyCode() == KeyEvent.VK_H ) {
            takeSnapshot = true;
            canvas.repaint();
        }
//        if ( e.getKeyCode() == KeyEvent.VK_1 ) {
//            epsilon += 0.01f;
//            globP2DContourSimp = polygon2DContourSimplify(globP2DContour,epsilon,false);
//            canvas.repaint();
//        }
//        if ( e.getKeyCode() == KeyEvent.VK_2 ) {
//            epsilon -= 0.01f;
//            globP2DContourSimp = polygon2DContourSimplify(globP2DContour,epsilon,false);
//            canvas.repaint();
//        }
//        if ( e.getKeyCode() == KeyEvent.VK_3 ){
//            //Area code.
//            translate2D(p0,new Vertex2D(-0.1,0));
//            translate2D(p1,new Vertex2D(-0.1,0));
//            translate2D(p2,new Vertex2D(-0.1,0));
//        }
//        if ( e.getKeyCode() == KeyEvent.VK_4 ){
//            //Area code.
//            translate2D(p0,new Vertex2D(0.1,0));
//            translate2D(p1,new Vertex2D(0.1,0));
//            translate2D(p2,new Vertex2D(0.1,0));
//        }
//        if ( e.getKeyCode() == KeyEvent.VK_5 ){
//            //Area code.
//            translate2D(p0,new Vertex2D(0,-0.1));
//            translate2D(p1,new Vertex2D(0,-0.1));
//            translate2D(p2,new Vertex2D(0,-0.1));
//        }
//        if ( e.getKeyCode() == KeyEvent.VK_6 ){
//            //Area code.
//            translate2D(p0,new Vertex2D(0,0.1));
//            translate2D(p1,new Vertex2D(0,0.1));
//            translate2D(p2,new Vertex2D(0,0.1));
//        }
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            System.exit(0);
        }
        if ( e.getKeyCode() == KeyEvent.VK_I ) {
            System.out.println(qualitySelection);
        }
        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            System.out.println(qualitySelection);
            canvas.repaint();
        }
        //Area code.
        //System.out.println(calcArea());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if ( cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
        if ( qualityController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    /**
     * Do NOT call your controller from the `keyTyped` method, or the controller
     * will be invoked twice for each key. Call it only from the `keyPressed`
     * and `keyReleased` method
     *
     * @param e
     */
    @Override
    public void keyTyped(KeyEvent e) {

    }

    /**
     * Not used method, but needed to instantiate GLEventListener
     *
     * @param drawable
     */
    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    private void testClipPolygons() {
        int i, j;
        boolean leerArch = false;
//        Polygon2D[] innerPolyArr = new Polygon2D[1];
//        Polygon2D[] outerPolyArr = new Polygon2D[1];
        WeilerAthertonPolygonClipper WAPolygonClipper = new WeilerAthertonPolygonClipper();

        globP2DClip = null;
        for ( i = 0; i < clipPolygonsGlobTest[testNumber].length; ++i ) {
            if ( globP2DClip == null ) {
                globP2DClip = new Polygon2D();
            } else {
                globP2DClip.nextLoop();
            }
            for ( j = 0; j < clipPolygonsGlobTest[testNumber][i].length / 2; ++j ) {
                globP2DClip.addVertex(clipPolygonsGlobTest[testNumber][i][j * 2], clipPolygonsGlobTest[testNumber][i][j * 2 + 1] - 1);
            }
        }

        if ( leerArch ) {
            try {
                FileInputStream fileIn;
                fileIn = new FileInputStream("polygon000.bin");
                ObjectInputStream in;
                in = new ObjectInputStream(fileIn);
                globP2DSubject = (Polygon2D) in.readObject();
                scaleToFitPolygon(globP2DSubject, 8);
                globP2DSubject.invert(); //OJO, creo que esto es temporal.
                in.close();
                fileIn.close();
            } catch ( IOException exception ) {
                exception.printStackTrace();
                return;
            } catch ( ClassNotFoundException cnfException ) {
                System.out.println(globP2DSubject.getClass().getName() + " class not found");
                cnfException.printStackTrace();
                return;
            }
        } else {
            globP2DSubject = null;
            for ( i = 0; i < subjPolygonsGlobTest[testNumber].length; ++i ) {
                if ( globP2DSubject == null ) {
                    globP2DSubject = new Polygon2D();
                } else {
                    globP2DSubject.nextLoop();
                }
                for ( j = 0; j < subjPolygonsGlobTest[testNumber][i].length / 2; ++j ) {
                    globP2DSubject.addVertex(subjPolygonsGlobTest[testNumber][i][j * 2], subjPolygonsGlobTest[testNumber][i][j * 2 + 1]);
                }
            }
        }
        innerPoly = new Polygon2D();
        outerPoly = new Polygon2D();
        WAPolygonClipper.clipPolygons(globP2DClip, globP2DSubject, innerPoly, outerPoly);
//        innerPoly = innerPolyArr[0];
//        outerPoly = outerPolyArr[0];
        clipPolyWA = WAPolygonClipper.getClipPolyWA();
        subjectPolyWA = WAPolygonClipper.getSubjectPolyWA();
    }

    /**
     * This function scales 'polygon' in a manner that its width fits 'width'.
     * The center of scaling is the center of the 'polygon'.
     *
     * @param polygon
     * @param width
     */
    public void scaleToFitPolygon(Polygon2D polygon, double width) {
        Vertex2D center = new Vertex2D(0, 0);
        double scale, minMax[];
        int i,j;

        //minMax = new double[6];
        minMax = polygon.getMinMax();
        scale = width / (minMax[3] - minMax[0]);
        center.x = (minMax[3] + minMax[0]) / 2;
        center.y = (minMax[4] + minMax[1]) / 2;
//        for ( _Polygon2DContour p2DCont : polygon.loops ) {
        for ( i = 0; i < polygon.loops.size(); ++i ) {
            _Polygon2DContour p2DCont = polygon.loops.get(i);
//            for ( Vertex2D vertex : p2DCont.vertices ) {
            for ( j = 0; j < p2DCont.vertices.size(); ++j ) {
                Vertex2D vertex = p2DCont.vertices.get(j);
                vertex.x = (vertex.x - center.x) * scale + 2; //+ center.x;
                vertex.y = -(vertex.y - center.y) * scale + 2; // + center.y;
            }
        }
    }
//------------------------------------------------------------------------------    
    final static private double[][] clipPolygonGlobTest5 = {
        {1.88, 14.75, 17.88, 14.75, 17.63, 4.44, 1.69, 5.81}, {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16}, {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}
    };

    final static private double[][] subjPolygonGlobTest5 = {
        {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}
    };

    final static private double[][] clipPolygonGlobTest6 = {
        {1.88, 14.75, 17.88, 14.75, 17.63, 4.44, 1.69, 5.81}, {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16}, {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}
    };

    final static private double[][] subjPolygonGlobTest6 = {
        {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}
    };

    final static private double[][] clipPolygonGlobTest7 = {
        {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}
    };

    final static private double[][] subjPolygonGlobTest7 = {
        {1.88, 14.75, 17.88, 14.75, 17.63, 4.44, 1.69, 5.81}
    };
    final static private double[][] clipPolygonGlobTest8 = {
        {1.88, 14.75, 17.88, 14.75, 17.63, 4.44, 1.69, 5.81}, {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}
    };

    final static private double[][] subjPolygonGlobTest8 = {
        {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}
    };
    final static private double[][] clipPolygonGlobTest9 = {
        {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}
    };

    final static private double[][] subjPolygonGlobTest9 = {
        {1.88, 14.75, 17.88, 14.75, 17.63, 13.5, 1.69, 13.5}, {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16}, {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}
    };
    final static private double[][] clipPolygonGlobTest10 = {
        {2, 2, 10, 2, 10, 10, 2, 10}, {3, 3, 3, 9, 9, 9, 9, 3}, {4, 4, 8, 4, 8, 8, 4, 8}, {5, 5, 6, 7, 7, 5}
    };

    final static private double[][] subjPolygonGlobTest10 = {
        {0, 0, 12, 0, 12, 11, 0, 11}
    };
    final static private double[][] clipPolygonGlobTest11 = {
        {0, 0, 10, 0, 10, 11, 0, 11}, {1.23, 1.65, 1.19, 9.61, 9.42, 9.57, 9.23, 1.68}
    };

    final static private double[][] subjPolygonGlobTest11 = {
        {1.23, 1.65, 1.19, 9.61, 9.42, 9.57, 9.23, 1.68}
    };
//        globP2DSubject.addVertex(2.5,2);
//        globP2DSubject.addVertex(2,3);
//        globP2DSubject.addVertex(3,2);
    final static private double[][][] clipPolygonsGlobTest = {
              {{3, 0, 6, 4, 2, 6, 1, 3}}
            , {{3, 0, 6, 4, 2, 6, 1, 3}}
            , {{0, 1, 11, 8, 6, 11, -3, 5}}
            , {{0, 1, 11, 8, 6, 11, -3, 5}}
            , {{3, -1, 4, -1, 4, 8, 3, 8}}
            ,/**/ {{1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75}, {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16}, {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}}
            ,/**/ {{1.69, 5.81, 17.63, 4.44,  17.88, 14.75,1.88, 14.75}, {3.31, 11.97, 7.09, 11.97, 7.31, 7.88, 3.38, 8.16}, {11.91, 10.78, 15.09, 10.66, 14.78, 8.16, 12.00, 8.38}}
            , /**/{{2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34, 2.69, 13.28}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}}
            ,/**/ {{1.69, 5.81, 17.63, 4.44, 17.88, 14.75,1.88, 14.75}, {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}}
            ,/**/ {{2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,2.69, 13.28}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}}
            , {{2, 2, 10, 2, 10, 10, 2, 10}, {3, 3, 3, 9, 9, 9, 9, 3}, {4, 4, 8, 4, 8, 8, 4, 8}, {5, 5, 6, 7, 7, 5}}
            , {{0, 0, 12, 0, 12, 11, 0, 11}, {2.2, 6, 2.5, 7, 2.8, 6}, {3.2, 5, 3.5, 6, 3.8, 5}, {4.2, 6, 4.5, 7, 4.8, 6}, {5.5, 5.5, 6, 6.5, 6.5, 5.5}}
            , {{0, 0, 10, 0, 10, 11, 0, 11}, {1.23, 1.65, 1.19, 9.61, 9.42, 9.57, 9.23, 1.68}}
            , {{1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75}, {2.69, 13.28, 16.69, 13.34, 16.91, 6.09, 10.72, 5.97, 2.44, 7.09}}
            , {{-5, 5, 5, 0, 7, 10, -3, 15}}
            , {{-5, 0, 10, 0, 10, 10, -5, 10}}
            , {{0, 0, 10, 4, 10, 8, 0, 12}}
            , {{0, 0, 3, 0, 6, 4, 0, 4}}
            , {{3, 0, 6, 4, 3, 8, 0, 4}}
            , {{0, 0, 3, 0, 3, 3, 0, 3}}
            , {{3, 0, 5, 2, 4, 6, 0, 3}}
            , {{3, -3, 6, 0, 4, 2, 6, 4, 3, 7, 0, 2}}
            , {{3, -3, 6, 0, 4, 2, 6, 4, 3, 7, 0, 2}}
            , {{5, 2, 8, -1, 11, 3, 7, 6}}
            , {{4, 3, 7, 4, 5, 8, 1, 7}}
            , {{3, 0, 0, -3, 3, -6, 6, -3}}
            , {{0, 0, 6, 0, 6, 4, 0, 4}}
            , {{0, 0, 6, 0, 6, 4, 0, 4}}
            , {{0, 4, 1, 1, 5, 1, 5, 3}}
            , {{0, 0, 6, 0, 6, 4, 0, 4}}
            , {{0, 0, 6, 0, 6, 4, 0, 4}}
            , {{0, 0, 7, 0, 7, 4, 0, 4}}
            , {{0, 0, 3, -2, 3, 6, 0, 4}}
            , {{0, 0, 7, 0, 7, 4, 0, 4}}
            , {{0, 0, 3, -3, 3, 7, 0, 4, -4, 2}}
            , {{0, 0, 7, 0, 7, 4, 0, 4}}
            , {{0, 0, 3, -3, 3, 7, 0, 4, 2, 2}}
            , {{0, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 4, 0, 4}}
            , {{0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0, 3, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 7, 3}}
            , {{0, -1, 7, -1, 7, 2, 6.5, 2, 6, 1, 5, 0, 2, 0, 1, 1, 0.5, 2, 0, 2}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0.5, 2, 1, 1, 1.5, 0.5, 5.5, 0.5, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0.5, 2, 1, 1, 3, -1, 4, -1, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 8, 1, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            ,/**/ {{-1, 1, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
    };
    final static private double[][][] subjPolygonsGlobTest = {
              {{3, 3, 8, 1, 9, 5, 7, 7}}
            , {{3, 3, 8, 1, 9, 5, 7, 7}, {4, 3, 6, 5, 8, 3, 7, 2}}
            , {{4, 0, 5, 1, 1, 5, 6, 9, 9, 5, 6, 2, 3, 5, 6, 7, 7, 5, 6, 4, 5, 5, 6, 5, 6, 6, 4, 5, 6, 3, 8, 5, 6, 8, 2, 5, 6, 1, 11, 5, 6, 10, -1, 5}}
            , {{4, 0, 5, 1, 1, 5, 6, 9, 9, 5, 6, 2, 3, 5, 6, 7, 7, 5, 6, 4, 5, 5, 6, 5, 6, 6, 4, 5, 6, 3, 8, 5, 6, 8, 2, 5, 6, 1, 11, 5, 6, 10, -1, 5}, {2.5, 2, 2, 3, 3, 2}}
            , {{0, 0, 5, 0, 5, 1, 2, 1, 2, 2, 5, 2, 5, 3, 2, 3, 2, 4, 5, 4, 5, 5, 2, 5, 2, 6, 5, 6, 5, 7, 0, 7}}
            ,/**/ {{2.44, 7.09, 10.72, 5.97,  16.91, 6.09, 16.69, 13.34,2.69, 13.28}, {9.88, 11.38, 15.22, 12.03, 16.06, 7.97, 13.06, 6.91, 9.63, 8.88}}
            ,/**/ {{ 2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34,2.69, 13.28}}
            , /**/{{1.69, 5.81, 17.63, 4.44, 17.88, 14.75, 1.88, 14.75}}
            ,/**/ {{3.38, 8.16, 7.31, 7.88, 7.09, 11.97,3.31, 11.97}, {9.63, 8.88, 13.06, 6.91, 16.06, 7.97, 15.22, 12.03,9.88, 11.38}}
            ,/**/ {{1.69, 13.5, 17.63, 13.5, 17.88, 14.75,1.88, 14.75}, {3.38, 8.16, 7.31, 7.88, 7.09, 11.97,3.31, 11.97}, {12.00, 8.38, 14.78, 8.16, 15.09, 10.66,11.91, 10.78}}
            , {{0, 0, 12, 0, 12, 11, 0, 11}}
            , {{2, 2, 10, 2, 10, 10, 2, 10}, {3, 3, 3, 9, 9, 9, 9, 3}, {4, 4, 8, 4, 8, 8, 4, 8}, {5, 5, 6, 7, 7, 5}}
            , {{1.23, 1.65, 1.19, 9.61, 9.42, 9.57, 9.23, 1.68}}
            , {{2.44, 7.09, 10.72, 5.97, 16.91, 6.09, 16.69, 13.34, 2.69, 13.28}}
            , {{0, 0, 15, 0, 15, 10, 0, 10}}
            , {{0, 0, 15, 0, 15, 10, 0, 10}}
            , {{5, 0, 10, 0, 10, 12, 5, 12}}
            , {{3, 0, 6, 4, 3, 8, 0, 4}}
            , {{0, 0, 3, 0, 6, 4, 0, 4}}
            , {{-3, 1.5, 0, 0, 3, -3, 3, 0, 3, 3, 3, 6, 0, 3}}
            , {{0, 0, 3, 0, 3, 3, 0, 3}}
            , {{4, 2, 12, 3, 6, 4}}
            , {{4, 2, 12, 3, 8, 4}}
            , {{3, 0, 5, 2, 4, 6, 0, 3}}
            , {{4, 3, 2, 4, 4, 6, 1, 7, -1, 6, 0, 0}}
            , {{0, 0, 6, 0, 6, 4, 0, 4}}
            , {{0, 0, 3, 0, 3, 4, 0, 4}}
            , {{0, 4, 1, 1, 5, 1, 5, 3}}
            , {{0, 0, 6, 0, 6, 4, 0, 4}}
            , {{6, 0, 10, 0, 10, 4, 6, 4}}
            , {{3, 4, 6, 4, 6, 8, 3, 8}}
            , {{0, 0, 3, -2, 3, 6, 0, 4}}
            , {{0, 0, 7, 0, 7, 4, 0, 4}}
            , {{0, 0, 3, -3, 3, 7, 0, 4, -4, 2}}
            , {{0, 0, 7, 0, 7, 4, 0, 4}}
            , {{0, 0, 3, -3, 3, 7, 0, 4, 2, 2}}
            , {{0, 0, 7, 0, 7, 4, 0, 4}}
            , {{0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{0, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 4, 0, 4}}
            , {{0, 3, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 7, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0, -1, 7, -1, 7, 2, 6.5, 2, 6, 1, 5, 0, 2, 0, 1, 1, 0.5, 2, 0, 2}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0.5, 2, 1, 1, 1.5, 0.5, 5.5, 0.5, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0.5, 2, 1, 1, 3, -1, 4, -1, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
            , {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 8, 1, 6.5, 3, 0.5, 3}}
            ,/**/ {{-1, 2, 0.5, 2, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 8, 2, 8, 5, -1, 5}}
            , {{-1, 1, 1, 1, 2, 0, 5, 0, 6, 1, 6.5, 2, 6.5, 3, 0.5, 3}}
    };
    //final static private double[][][] prueba={{{3,4,5},{2,3,4}},{{3,4,5},{2,3,4}}};
//    final static private double[][] clipPolygonGlobTest11={
//        {1.88,14.75,17.88,14.75,17.63,4.44,1.69,5.81},{2.69,13.28,16.69,13.34,16.91,6.09,10.72,5.97,2.44,7.09}
//    };
//    final static private double[][] subjPolygonGlobTest11={
//        {2.69,13.28,16.69,13.34,16.91,6.09,10.72,5.97,2.44,7.09}
//    };
    final static private double[][] clipPolygonGlob = clipPolygonGlobTest5;
    final static private double[][] subjPolygonGlob = subjPolygonGlobTest5;
//    static private double[] clipPolygonGlob={
//         0,1
//        ,11,8
//        ,6,11
//        ,-3,5
//    };
//    static private double[] subjPolygonGlob={
//         4,0
//        ,5,1
//        ,1,5
//        ,6,9
//        ,9,5
//        ,6,2
//        ,3,5
//        ,6,7
//        ,7,5
//        ,6,4
//        ,5,5
//        ,6,5
//        ,6,6
//        ,4,5
//        ,6,3
//        ,8,5
//        ,6,8
//        ,2,5
//        ,6,1
//        ,11,5
//        ,6,10
//        ,-1,5
//    };

//    static private double[] clipPolygonGlob={
//         3,-1
//        ,4,-1
//        ,4,8
//        ,3,8
//    };
//    static private double[] subjPolygonGlob={
//         0,0
//        ,5,0
//        ,5,1
//        ,2,1
//        ,2,2
//        ,5,2
//        ,5,3
//        ,2,3
//        ,2,4
//        ,5,4
//        ,5,5
//        ,2,5
//        ,2,6
//        ,5,6
//        ,5,7
//        ,0,7
//    };
}
