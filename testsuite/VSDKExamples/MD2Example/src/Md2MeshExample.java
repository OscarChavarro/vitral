//===========================================================================

// Basic java classes
import java.io.File;
import java.util.ArrayList;

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
import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

// Swing GUI java classes
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

// JOGL classes
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;    // Model elements
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.geometry.EnvironmentPersistence; // Persistence elements
import vsdk.toolkit.render.jogl.JoglCameraRenderer; // View elements
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.gui.CameraController;           // Control elements
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.environment.geometry.Md2Mesh;
import vsdk.toolkit.io.geometry.Md2Persistence;
import vsdk.toolkit.animation.AnimationEventGenerator;
import vsdk.toolkit.render.jogl.JoglMd2MeshRenderer;
import vsdk.toolkit.animation.Md2AnimationListener;

// Application classes
//import util.filters.ObjectFilter;

//Polygon simplify.
import vsdk.toolkit.common.Vertex2D;
import vsdk.toolkit.environment.geometry.Polygon2D;
import vsdk.toolkit.environment.geometry._Polygon2DContour;
import vsdk.toolkit.render.jogl.animation.JoglRepainterAnimationListener;

/**
 */
public class Md2MeshExample
    extends JFrame implements GLEventListener, MouseListener,
                   MouseMotionListener, MouseWheelListener, KeyListener {

    private Camera camera;
    private Light light;
    private CameraController cameraController;
    private RendererConfiguration qualitySelection;
    private RendererConfigurationController qualityController;
    //public GLCanvas canvas;
    public GLJPanel canvas;

    private SimpleScene scene;
    private Md2Persistence md2Pers;
    private Md2Mesh md2Mesh;
    private AnimationEventGenerator animator;
    //private JoglMd2MeshRenderer Md2MeshRend = new JoglMd2MeshRenderer();

    public double x;
    Vertex2D p0,p1,p2;
    private _Polygon2DContour globP2DContour = new _Polygon2DContour();
    private _Polygon2DContour globP2DContourSimp = new _Polygon2DContour();

    public Md2MeshExample(String fileName) {
        super("VITRAL mesh test - JOGL");
        File file = null;
        GLProfile glp = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCaps = new GLCapabilities(glp);
        String texture;
        int i;

        ///Temp polygon simplification  LRR
//        p0 = new Vertex2D(2,2);
//        p1 = new Vertex2D(7,2);
//        p2 = new Vertex2D(5,5);
//        x = 0;
//        for(i=0;i<polygon.length/2;++i){
//            globP2DContour.addVertex((polygon[i*2]-6.886384)*10, (polygon[i*2+1]-53.345787)*10);
//        }
//        globP2DContourSimp = polygon2DContourSimplify(globP2DContour,75,true);
        //Temp polygon simplification  LRR
        
        scene = new SimpleScene();
        md2Mesh = new Md2Mesh();

        //-----------------------------------------------------------------
        //fileName = "C:\\Politecnica\\ModelosEdificios\\Biblioteca I. E. Municipio de San Agustín\\Biblioteca Laureano Gomez-San Agustín\\lorenzoGomesFinal_sinPiso.obj";
//        fileName = "C:\\Politecnica\\etc\\geometry\\Blade.md2";
//        texture  = "C:\\Politecnica\\etc\\geometry\\Texture\\Blade.jpg";
//        fileName = "C:\\Politecnica\\etc\\geometry\\Samourai.md2";
//        texture  = "C:\\Politecnica\\etc\\geometry\\Texture\\Samourai.jpg";
        fileName = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\Samourai\\Samourai.md2";
        texture  = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\Samourai\\Samourai.jpg";
//        fileName = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\eje_01_DEFAULT_GenNormals.md2";
//        fileName = "E:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\eje_02_Segmento Respiracion.md2";
//        fileName = "E:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\eje_03_Declinacion_normales_VR2015.md2";
//        texture  = "D:\\Leonardo\\TrabajoPolitecnica\\003Md2-quake2\\Modelos\\DeWilson\\M_diffuse.jpg";
//        fileName = "D:\\Leonardo\\TrabajoPolitecnica\\__Modelos\\MD2\\Nina\\TEX 02_02.md2";
//        texture  = "D:\\Leonardo\\TrabajoPolitecnica\\__Modelos\\MD2\\Nina\\Avatar_Nina_Body_Dm_ajustes.jpg";
        try {
            md2Pers = new Md2Persistence();
            md2Pers.read(fileName,texture,md2Mesh);
            md2Mesh.setCurrentAnimationInd((short)0);
        } catch ( IOException ex ) {
            System.err.println("Failed to read file.");
            ex.printStackTrace();
            System.exit(0);
        }
        
        
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

    public Dimension getPreferredSize() {
        return new Dimension(640, 480);
    }

    public static void main(String[] args) {
        JFrame f;


        if ( args.length == 1 ) {
            f = new Md2MeshExample(args[0]);
        }
        else {
            f = new Md2MeshExample(null);
        }

//        Animador h = new Animador((Md2MeshExample)f);
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
        gl.glLineWidth((float)3.0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
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

        
        ///Temp polygon simplification  LRR
        //polygonSimpTest(gl);
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
        
        
        float pos[] = {200f, 200f, 0f, 1.0f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, pos, 0);
        float dif[] = {1.0f,1.0f,1.0f,1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dif, 0);
        float amb[] = {0.7f,0.7f, 0.7f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, amb, 0);        
        
        
        JoglMd2MeshRenderer.draw(gl,md2Mesh); //LRR

        
//        JoglSimpleBodyRenderer.setAutomaticDisplayListManagement(true);
//
//        for ( i = 0; i < scene.getSimpleBodies().size(); i++ ) {
//            JoglSimpleBodyRenderer.drawWithVertexArrays(gl, scene.getSimpleBodies().get(i), camera, qualitySelection);
//        }
    }
    private void polygonSimpTest(GL2 gl) {
        int i;
        
        gl.glBegin(GL2.GL_LINE_LOOP);
            gl.glColor3d(0.2, 0.8, 0.2);
            for(i=0;i<globP2DContour.vertices.size();++i){
                gl.glVertex3d(globP2DContour.vertices.get(i).x, 0, globP2DContour.vertices.get(i).y);
            }
        
        gl.glEnd();
        gl.glBegin(GL2.GL_LINE_LOOP);
            gl.glColor3d(0.9, 0.8, 0.2);
            for(i=0;i<globP2DContourSimp.vertices.size();++i){
                gl.glVertex3d(globP2DContourSimp.vertices.get(i).x, 0, globP2DContourSimp.vertices.get(i).y);
            }
        
        gl.glEnd();
    }
    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        
        JoglCameraRenderer.activate(gl, camera);
        JoglLightRenderer.activate(gl, light);

        drawObjectsGL(gl);
    }

    /** Not used method, but needed to instantiate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        JoglMd2MeshRenderer.initGL(drawable.getGL().getGL2(), md2Mesh);
        createAnimator(md2Mesh, canvas);
    }
    
    public void createAnimator(Md2Mesh md2Mesh, GLJPanel gljp)
    {
        //- Set up animation control thread -----------------------------------
        animator = new AnimationEventGenerator();
        Md2AnimationListener md2AniListener = new Md2AnimationListener(md2Mesh);
        animator.addAnimationListener(md2AniListener);
        JoglRepainterAnimationListener repainterListener;
        repainterListener = new JoglRepainterAnimationListener(gljp);
        animator.addAnimationListener(repainterListener);
        Thread t  = new Thread(animator);
        t.start();

       // This is causing to repeat the canvas repaint!

       //streetSelectorListener = new StreetSelectorAnimationListener(c, m);
       //animator.addAnimationListener(streetSelectorListener);
    }
    
    /** Not used method, but needed to instantiate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height) {
        GL2 gl = drawable.getGL().getGL2();
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
        if (cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(".");
        if (cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
    }

    public void keyPressed(KeyEvent e) {
        short[] animStartEnd = new short[2];
        
        if ( e.getKeyCode() == KeyEvent.VK_1 ) {
            md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
            if(md2Mesh.getCurrentAnimationInd() == animStartEnd[0])
                md2Mesh.setCurrentAnimationInd(md2Mesh.getMaxAnimationInd());
            else
                md2Mesh.setCurrentAnimationInd((short)(md2Mesh.getCurrentAnimationInd()-1));
//            //Area code.
//            rotate2D(p0,0.06f);
//            rotate2D(p1,0.06f);
//            rotate2D(p2,0.06f);
        }
        if ( e.getKeyCode() == KeyEvent.VK_2 ) {
            md2Mesh.setCurrentAnimationInd((short)(md2Mesh.getCurrentAnimationInd()+1));
//            //Area code.
//            rotate2D(p0,-0.06f);
//            rotate2D(p1,-0.06f);
//            rotate2D(p2,-0.06f);
        }
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
//        //Area code.
//        System.out.println(calcArea());
    }

    public void keyReleased(KeyEvent e) {
        if (cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
            canvas.repaint();
        }
        if (qualityController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e))) {
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

    /** Not used method, but needed to instantiate GLEventListener */
    public void dispose(GLAutoDrawable drawable) {
        ;
    }
//    
//    /**
//     * Given a Polygon2D object, return a simplified version of that set of polygons.
//     * @param pol2DIn array of polygons.
//     * @param percent Percent of the total points to be preserved(This is not an exact number).
//     * @param copy the points of the contours are copied or referenced?
//     * @return array of simplified polygons.
//     */
//    public Polygon2D polygon2DSimplify(Polygon2D pol2DIn, float percent, boolean copy){
//        Polygon2D pol2DSimp = new Polygon2D();
//            
//        for(_Polygon2DContour p2DContour : pol2DIn.loops) {
//            pol2DSimp.loops.add(polygon2DContourSimplify(p2DContour,percent,true));
//        }
//        return pol2DSimp;
//    }
//    
//    /**
//     * Given a polygon2DContour object, return a simplified version of that polygon.
//     * @param p2DContour   Single polygon.
//     * @param percent   Percent of the total points to be preserved(This is not an exact number).
//     * @param copy the points of the contour are copied or referenced?
//     */
//    private _Polygon2DContour  polygon2DContourSimplify(_Polygon2DContour p2DContour, float percent, boolean copy){
//        _Polygon2DContour p2DContourSimp = new _Polygon2DContour();
//        Vertex2D point;
//        /** In missingNodes we put two vertex at a time */
//        Stack<Integer> indsStkMissingNodes = new Stack();
//        int ind0,ind1,indFar;
//        float epsilon = 0.01f;///
//        float[] dist = new float[1];
//        //LinkedList<Vertex2D> P2DContourSimp = new LinkedList();
//        /** The default initialization in java of indContourSimp is used
//          * (filled with zeros)*/
//        int[] ContourSimpFlags;
//        int numVertex,i;
//        
//        
//        numVertex = p2DContour.vertices.size();
//        if(numVertex < 3) { //One line or one point.
//            if(copy)
//                for(i=0;i<numVertex;++i) {
//                        point = p2DContour.vertices.get(i);
//                        point = new Vertex2D(point.x, point.y, point.color.r, point.color.g, point.color.b);
//                        p2DContourSimp.vertices.add(point);
//                }
//            else
//                for(i=0;i<numVertex;++i) {
//                        point = p2DContour.vertices.get(i);
//                        p2DContourSimp.vertices.add(point);
//                }
//            return p2DContourSimp;
//        }
//        ContourSimpFlags = new int[numVertex];
//        indsStkMissingNodes.push(numVertex-1); //The last one.
//        indsStkMissingNodes.push(0); //The fist one.
//        while(!indsStkMissingNodes.isEmpty()) {
//            ind0 = indsStkMissingNodes.pop();
//            ind1 = indsStkMissingNodes.pop();
//            ContourSimpFlags[ind0] = 1;
//            ContourSimpFlags[ind1] = 1;
//            if((ind1-ind0)>1) {
//                indFar = getFarthestNodeToLine(p2DContour,ind0,ind1,dist);
//                if(dist[0]>epsilon) {
//                    indsStkMissingNodes.push(indFar);
//                    indsStkMissingNodes.push(ind0);
//                    indsStkMissingNodes.push(ind1);
//                    indsStkMissingNodes.push(indFar);
//                }
//            }
//        }
//        if(copy)
//            for(i=0;i<numVertex;++i) {
//                if(ContourSimpFlags[i]==1) {
//                    point = p2DContour.vertices.get(i);
//                    point = new Vertex2D(point.x, point.y, point.color.r, point.color.g, point.color.b);
//                    p2DContourSimp.vertices.add(point);
//                }
//            }
//        else
//            for(i=0;i<numVertex;++i) {
//                if(ContourSimpFlags[i]==1) {
//                    point = p2DContour.vertices.get(i);
//                    p2DContourSimp.vertices.add(point);
//                }
//            }
//        return p2DContourSimp;
//    }
//    /**
//     * Return the index of the farthest point to the line formed by the point at index ind0
//     * and the point at index ind1 in the p2DContour; the distance is also returned in outDist[0].
//     * @param p2DContour   Polygon.
//     * @param ind0 Index 0 of the line.
//     * @param ind1 Index 1 of the line.
//     * @param outDist Array of one element: distance to the farthest point.
//     */
//    private int getFarthestNodeToLine(_Polygon2DContour p2DContour, int ind0, int ind1, float[] outDist) {
//        int i;
//        float xInd0,yInd0,nx,ny,m,temp;
//        float[] v = new float[2];
//        float dist, maxDist;
//        int indFar;
//        
//        //Find the unitary normal vector to the line.
//        xInd0 = (float)p2DContour.vertices.get(ind0).x;
//        yInd0 = (float)p2DContour.vertices.get(ind0).y;
//        temp = (float)(p2DContour.vertices.get(ind1).y - yInd0);
//        if(temp<0.00001 && temp>-0.00001) {
//            nx=0;
//            ny=1;
//        }
//        else {
//            //m = Slope of the normal to the line.
//            m =  -(float)((p2DContour.vertices.get(ind1).x - xInd0)
//                         /temp);
//            temp=(float)Math.sqrt(1+m*m);
//            nx=1/temp;
//            ny=m/temp;
//        }
//        //Find the farthest point and its distance.
//        maxDist=0;
//        indFar=ind0+1;
//        for(i=ind0+1; i<ind1; ++i) {
//            v[0] = (float)(p2DContour.vertices.get(i).x - xInd0);
//            v[1] = (float)(p2DContour.vertices.get(i).y - yInd0);
//            //Dot product.
//            dist = Math.abs(v[0]*nx + v[1]*ny);
//            if(dist>maxDist) {
//                maxDist=dist;
//                indFar=i;
//            }
//        }
//        outDist[0]=maxDist;
//        return indFar;
//    }
//    
//    //temp
//    private double calcArea(){
//        Vertex2D pA,pB,pC;
//        //double area;
//        
////        p0 = new Vertex2D(0,0);
////        p1 = new Vertex2D(50,0);
////        p2 = new Vertex2D(30,20);
//        //First: find the two extreme points with respect to the x axis.
//        if(p0.x<p1.x)
//            if(p0.x<p2.x)
//                pA=p0;
//            else
//                pA=p2;
//        else
//            if(p1.x<p2.x)
//                pA=p1;
//            else
//                pA=p2;            
//        if(p0.x>p1.x)
//            if(p0.x>p2.x)
//                pB=p0;
//            else
//                pB=p2;
//        else
//            if(p1.x>p2.x)
//                pB=p1;
//            else
//                pB=p2;
//        //Next, the other point(pC).
//        pC=p0;//Only to avoid error.
//        if(pA==p0)
//            if(pB==p1)
//                pC=p2;
//            else
//                pC=p1;
//        if(pA==p1)
//            if(pB==p0)
//                pC=p2;
//            else
//                pC=p0;
//        if(pA==p2)
//            if(pB==p0)
//                pC=p1;
//            else
//                pC=p0;
//pA=p0;
//pB=p1;
//pC=p2;
//        //Now, the area.
//        return Math.abs( ((pB.x-pC.x)*pC.y + (pB.y-pC.y)*(pB.x-pC.x)/2)
//                        +((pC.x-pA.x)*pA.y + (pC.y-pA.y)*(pC.x-pA.x)/2)
//                        -((pB.x-pA.x)*pA.y + (pB.y-pA.y)*(pB.x-pA.x)/2));
//        
//    }
//    //temp
//    private void rotate2D(Vertex2D p, float ang) {
//        double temp;
//        
//        temp = p.x*Math.cos(ang) - p.y*Math.sin(ang);
//        p.y = p.x*Math.sin(ang) + p.y*Math.cos(ang);
//        p.x = temp;
//    }
//    //temp
//    private void translate2D(Vertex2D p, Vertex2D trans){
//        p.x += trans.x;
//        p.y += trans.y;
//    }
//    static private double[] polygon={
//        6.886384,53.345787,6.886259,53.342194,6.883163,53.339536,6.88282,53.329654,6.882864,53.329472,6.884088,53.324432,6.884133,53.324245,6.883884,53.317058,6.885228,53.312547,6.887962,53.31084,6.888165,53.310713,6.887759,53.299034,6.884729,53.298173,6.860643,53.295775,6.860704,53.297571,6.851707,53.297681,6.846995,53.291447,6.838244,53.291553,6.837999,53.291556,6.829032,53.292563,6.826243,53.298888,6.817156,53.2963,6.814745,53.298562,6.814246,53.299031,6.808484,53.306289,6.811514,53.307152,6.804361,53.317091,6.804339,53.317123,6.804384,53.318511,6.804486,53.321615,6.806105,53.325191,6.807662,53.326967,6.807665,53.32697,6.810696,53.327833,6.815198,53.32778,6.824232,53.328571,6.832665,53.32847,6.833236,53.328463,6.834736,53.328445,6.836438,53.329095,6.839299,53.330187,6.83936,53.331984,6.835068,53.338327,6.833718,53.342837,6.83528,53.344616,6.836841,53.346395,6.842847,53.346322,6.850353,53.346231,6.860801,53.344306,6.863772,53.343371,6.866775,53.343334,6.874311,53.34414,6.875968,53.348613,6.885989,53.345894,6.886384,53.345787
//    };
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
