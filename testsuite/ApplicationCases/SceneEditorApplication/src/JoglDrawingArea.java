//===========================================================================
//===========================================================================

// JDK Basic classes
import java.lang.reflect.Method;
import java.io.File;
import java.util.ArrayList;

// AWT/Swing classes
import java.awt.Cursor;
import java.awt.Dimension;
//import java.awt.Image; // Do not define! conflicts with VSDK's Image
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JLabel;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.Quaternion;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Triangle;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.SimpleBackground;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.jogl.JoglMaterialRenderer;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglTranslateGizmoRenderer;
import vsdk.toolkit.render.jogl.JoglRotateGizmoRenderer;
import vsdk.toolkit.render.jogl.JoglScaleGizmoRenderer;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl.JoglZBufferRenderer;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.CameraControllerBlender;
import vsdk.toolkit.gui.RendererConfigurationController;
import vsdk.toolkit.gui.TranslateGizmo;
import vsdk.toolkit.gui.RotateGizmo;
import vsdk.toolkit.gui.ScaleGizmo;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.ImageProcessing;
import vsdk.framework.shapeMatching.JoglProjectedViewRenderer;

public class JoglDrawingArea implements 
    GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener,
    KeyListener
{
    public static final int CAMERA_INTERACTION_MODE = 1;
    public static final int SELECT_INTERACTION_MODE = 2;
    public static final int TRANSLATE_INTERACTION_MODE = 3;
    public static final int ROTATE_INTERACTION_MODE = 4;
    public static final int SCALE_INTERACTION_MODE = 5;

    public GLCanvas canvas;

    private RendererConfiguration qualitySelection;
    private RendererConfiguration qualitySelectionVisualDebug;
    private CameraController cameraController;
    private RendererConfigurationController qualityController;
    private TranslateGizmo translationGizmo;
    private RotateGizmo rotateGizmo;
    private ScaleGizmo scaleGizmo;
    private Material visualDebugMaterial;

    private Scene theScene;
    private JLabel statusMessage;

    private ArrayList<JoglView> views;

    public int interactionMode;

    public boolean wantToGetColor;
    public boolean wantToGetDepth;
    public boolean wantToGetContourns;
    public boolean wantToDebugProjectedViews;

    private JoglProjectedViewRenderer projectedViewRenderer;

    private Cursor camrotateCursor;
    private Cursor camtranslateCursor;
    private Cursor camadvanceCursor;
    private Cursor selectCursor;

    //
    private static final int RENDER_MODE_ZBUFFER = 1;
    private static final int RENDER_MODE_RAYTRACING = 2;
    private int renderMode;
    private int globalViewportXSize;
    private int globalViewportYSize;
    private ViewOrganizer viewOrganizer;
    private int selectedView;
    private boolean fullViewport;
    private int viewOrderStyle;

    SceneEditorApplication parent;

    private boolean doDistanceField;
    private int distanceFieldSide;

    public JoglDrawingArea(Scene theScene, JLabel statusMessage, SceneEditorApplication parent)
    {
        this.parent = parent;
        this.theScene = theScene;
        this.statusMessage = statusMessage;
        this.globalViewportXSize = 0;
        this.globalViewportYSize = 0;
        this.renderMode = RENDER_MODE_ZBUFFER;

        interactionMode = CAMERA_INTERACTION_MODE;

        createCursors();

        //cameraController = new CameraControllerBlender(theScene.camera);
        cameraController = new CameraControllerAquynza(theScene.camera);
        translationGizmo = new TranslateGizmo(theScene.camera);

        qualitySelection = theScene.qualityTemplate;
        qualityController = new RendererConfigurationController(qualitySelection);
        qualitySelectionVisualDebug = new RendererConfiguration();
        qualitySelectionVisualDebug.setShadingType(
            qualitySelectionVisualDebug.SHADING_TYPE_GOURAUD);
        rotateGizmo = new RotateGizmo();
        scaleGizmo = new ScaleGizmo();

        visualDebugMaterial = parent.theScene.defaultMaterial();

        canvas = new GLCanvas();

        Dimension minimumSize = new Dimension(8, 8);
        canvas.setMinimumSize(minimumSize);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);

        wantToGetColor = false;
        wantToGetDepth = false;
        wantToGetContourns = false;
        wantToDebugProjectedViews = false;

        //-----------------------------------------------------------------
        doDistanceField = false;
        distanceFieldSide = 320;

        if ( doDistanceField ) {
            projectedViewRenderer = new JoglProjectedViewRenderer(distanceFieldSide, distanceFieldSide, true);
        }
        else {
            projectedViewRenderer = new JoglProjectedViewRenderer(640, 640, true);
        }

        //-----------------------------------------------------------------
        JoglView view;
        int i;

        views = new ArrayList<JoglView>();
        viewOrganizer = new ViewOrganizer();
        selectedView = 0;
        fullViewport = false;
        viewOrderStyle = 0;

        for ( i = 0; i < 2; i++ ) {
            view = new JoglView();
            views.add(view);
        }
        selectedView = viewOrganizer.doLayout(views, fullViewport?selectedView:-1, viewOrderStyle);
    }

    private void createCursors()
    {
      Toolkit awtToolkit = Toolkit.getDefaultToolkit();
      java.awt.Image i;

      i = awtToolkit.getImage("./etc/cursors/cursor_camrotate.gif");
      camrotateCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraRotation");

      i = awtToolkit.getImage("./etc/cursors/cursor_camtranslate.gif");
      camtranslateCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraTranslation");

      i = awtToolkit.getImage("./etc/cursors/cursor_camadvance.gif");
      camadvanceCursor = awtToolkit.createCustomCursor(i, new Point(16, 16), "CameraAdvance");

      selectCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    }

    public void rotateBackground()
    {
        theScene.selectedBackground++;
        if ( theScene.selectedBackground > 2 ) {
            theScene.selectedBackground = 0;
        }
    }

    public GLCanvas getCanvas()
    {
        return canvas;
    }

    private void drawGizmos(GL gl)
    {
        // Pending: Turn off scene light and turn on gizmo specific lighting

        translationGizmo.setCamera(theScene.activeCamera);

        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        int firstThingSelected = theScene.selectedThings.firstSelected();

        if ( interactionMode == TRANSLATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
              Vector3D position;
              SimpleBody gi;

              gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

              Matrix4x4 composed;

              position = gi.getPosition();
              composed = new Matrix4x4(gi.getRotation());
              composed.M[0][3] = position.x;
              composed.M[1][3] = position.y;
              composed.M[2][3] = position.z;
              translationGizmo.setTransformationMatrix(composed);

              JoglTranslateGizmoRenderer.draw(gl, translationGizmo);
            }
        }
        else if ( interactionMode == ROTATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
              Vector3D position;
              SimpleBody gi;

              gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

              position = gi.getPosition();
              rotateGizmo.setTransformationMatrix(gi.getRotation());
              JoglRotateGizmoRenderer.draw(gl, rotateGizmo, position);
            }
        }
        else if ( interactionMode == SCALE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
              Vector3D position;
              SimpleBody gi;

              gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

              position = gi.getPosition();
              scaleGizmo.setTransformationMatrix(gi.getRotation());
              JoglScaleGizmoRenderer.draw(gl, scaleGizmo, position);
            }
        }
        gl.glEnable(gl.GL_DEPTH_TEST);
    }

    private Image createProjectedView(GL gl, SimpleBodyGroup referenceBodies, int cam)
    {
        //- Will render a normalized body inside the unit cube ------------
        double minmax[];
        SimpleBodyGroup bodySet = new SimpleBodyGroup();
        int i;

        Vector3D p;
        {
            //-----------------------------------------------------------------
            minmax = referenceBodies.getMinMax();
            Vector3D min, max, s;
            min = new Vector3D(minmax[0], minmax[1], minmax[2]);
            max = new Vector3D(minmax[3], minmax[4], minmax[5]);
            s = new Vector3D(max.x - min.x, max.y - min.y, max.z - min.z);

            double maxsize = s.x;
            if ( s.y > maxsize ) maxsize = s.y;
            if ( s.z > maxsize ) maxsize = s.z;
            // The 95% scale factor is to allow a full render of the object to
            // fit inside the rendered view
            s.x = s.y = s.z = (2/maxsize) * 0.95;

            p = max.add(min);
            p = p.multiply(-1/maxsize);

            bodySet.setPosition(p);
            bodySet.setScale(s);
            //-----------------------------------------------------------------
            SimpleBody referenceBody;
            SimpleBody framedBody;

            for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
                referenceBody = referenceBodies.getBodies().get(i);
                framedBody = new SimpleBody();
                framedBody.setGeometry(referenceBody.getGeometry());
                framedBody.setPosition(referenceBody.getPosition());
                framedBody.setRotation(referenceBody.getRotation());
                framedBody.setRotationInverse(referenceBody.getRotationInverse());
                framedBody.setMaterial(theScene.defaultMaterial());
                bodySet.getBodies().add(framedBody);
            }
            //-----------------------------------------------------------------
            Matrix4x4 Mset = bodySet.getTransformationMatrix(), R, Ri, Mbody, S, M;
            SimpleBody copiedBody;
            Quaternion q;

            for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
                referenceBody = referenceBodies.getBodies().get(i);
                if ( cam == 1 ) {
                    copiedBody = theScene.addThing(referenceBody.getGeometry());
                    Mbody = referenceBody.getTransformationMatrix();
                    M = Mset.multiply(Mbody);
                    p = new Vector3D(M.M[0][3], M.M[1][3], M.M[2][3]);
                    M.M[0][3] = M.M[1][3] = M.M[2][3] = 0.0;
                    q = M.exportToQuaternion();
                    q.normalize();
                    R = new Matrix4x4();
                    R.importFromQuaternion(q);
                    Ri = R.inverse();
                    S = Ri.multiply(M);
                    s = new Vector3D(M.M[0][0], M.M[1][1], M.M[2][2]);

                    copiedBody.setPosition(p);
                    copiedBody.setScale(s);
                    copiedBody.setRotation(R);
                }
            }

            //-----------------------------------------------------------------
        }

        //- Render will proceed in a PBuffer ------------------------------
        IndexedColorImage distanceFieldIndexed;

        projectedViewRenderer.configureScene(bodySet, cam);
        projectedViewRenderer.draw(gl);
        //canvas.swapBuffers();

        //-----------------------------------------------------------------
        Image finalImage;
        if ( !doDistanceField ) {
            finalImage = projectedViewRenderer.image;
        }
        else {
            System.out.print("Processing maps for view " + cam + "... ");
            distanceFieldIndexed = new IndexedColorImage();
            distanceFieldIndexed.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(projectedViewRenderer.image, distanceFieldIndexed, 1);
            ImageProcessing.gammaCorrection(distanceFieldIndexed, 2.0);

            RGBAImage distanceFieldRgba;
            distanceFieldRgba = distanceFieldIndexed.exportToRgbaImage();
            int x, y;

            for ( x = 0; x < distanceFieldRgba.getXSize(); x++ ) {
                for ( y = 0; y < distanceFieldRgba.getYSize(); y++ ) {
                    if ( distanceFieldIndexed.getPixel(x, y) < 1 ) {
                        distanceFieldRgba.putPixel(x, y,
                            (byte)255, (byte)0, (byte)0, (byte)128);
                    }
                }
            }
            finalImage = distanceFieldRgba;
            System.out.println("Ok!");
        }

        //vsdk.toolkit.io.image.ImagePersistence.exportPPM(new java.io.File("./test" + cam + ".ppm"), finalImage);

        //- Obtain Pbuffer's rendered image -------------------------------
        return finalImage;
    }

    private SimpleBodyGroup
    addDebugProjectedView(GL gl, SimpleBodyGroup referenceBodies)
    {
        SimpleBody boxBody;
        Image texture;
        SimpleBodyGroup group;
        Vector3D position = new Vector3D(0, 0, 0);
        Vector3D scale = new Vector3D(1, 1, 1);
        Matrix4x4 R = new Matrix4x4();
        Matrix4x4 R1 = new Matrix4x4();
        Matrix4x4 R2 = new Matrix4x4();
        int i;
        double delta = 0.01/2.0;
        TriangleMesh mesh;
        Vertex[] vertexArray;
        Triangle[] triangleArray;
        Vector3D n;
        Image textureArray[];
        Material materialArray[];
        int materialRanges[][];
        int textureRanges[][];

        group = new SimpleBodyGroup();
        for ( i = 1; i <= 13; i++ ) {
            R = new Matrix4x4();
            switch ( i ) {
              case 1:
                position = new Vector3D(0, -2, 0);
                R.axisRotation(Math.toRadians(90), new Vector3D(1, 0, 0));
                break;
              case 2:
                position = new Vector3D(-2, 0, 0);
                R1.axisRotation(Math.toRadians(90), new Vector3D(0, 0, -1));
                R2.axisRotation(Math.toRadians(90), new Vector3D(0, -1, 0));
                R = R2.multiply(R1);
                break;
              case 3:
                position = new Vector3D(0, 0, -2);
                R.axisRotation(Math.toRadians(180), new Vector3D(0, 1, 0));
                break;
              case 4:
                position = new Vector3D(-1, -1, 1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(45), new Vector3D(0, 0, -1));
                R2.axisRotation(Math.toRadians(35), new Vector3D(1, -1, 0));
                R = R2.multiply(R1);
                break;
              case 5:
                position = new Vector3D(1, -1, 1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(45), new Vector3D(0, 0, 1));
                R2.axisRotation(Math.toRadians(35), new Vector3D(1, 1, 0));
                R = R2.multiply(R1);
                break;
              case 6:
                position = new Vector3D(1, 1, 1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(135), new Vector3D(0, 0, 1));
                R2.axisRotation(Math.toRadians(35), new Vector3D(-1, 1, 0));
                R = R2.multiply(R1);
                break;
              case 7:
                position = new Vector3D(-1, 1, 1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(135), new Vector3D(0, 0, -1));
                R2.axisRotation(Math.toRadians(35), new Vector3D(-1, -1, 0));
                R = R2.multiply(R1);
                break;
              case 8:
                position = new Vector3D(0, 1, -1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(180), new Vector3D(0, 0, 1));
                R2.axisRotation(Math.toRadians(135), new Vector3D(-1, 0, 0));
                R = R2.multiply(R1);
                break;
              case 9:
                position = new Vector3D(-1, 0, -1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(90), new Vector3D(0, 0, -1));
                R2.axisRotation(Math.toRadians(135), new Vector3D(0, -1, 0));
                R = R2.multiply(R1);
                break;
              case 10:
                position = new Vector3D(0, -1, -1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R.axisRotation(Math.toRadians(135), new Vector3D(1, 0, 0));
                break;
              case 11:
                position = new Vector3D(1, 0, -1);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(90), new Vector3D(0, 0, 1));
                R2.axisRotation(Math.toRadians(135), new Vector3D(0, 1, 0));
                R = R2.multiply(R1);
                break;
              case 12:
                position = new Vector3D(1, -1, 0);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(90), new Vector3D(1, 0, 0));
                R2.axisRotation(Math.toRadians(45), new Vector3D(0, 0, 1));
                R = R2.multiply(R1);
                break;
              case 13:
                position = new Vector3D(1, 1, 0);
                position.normalize();
                position = position.multiply(1.5);
                scale = new Vector3D(0.5, 0.5, 0.5);
                R1.axisRotation(Math.toRadians(90), new Vector3D(1, 0, 0));
                R2.axisRotation(Math.toRadians(135), new Vector3D(0, 0, 1));
                R = R2.multiply(R1);
                break;
            }

            //-----------------------------------------------------------------
            texture = createProjectedView(gl, referenceBodies, i);
            if ( texture == null ) {
                return null;
            }

            //-----------------------------------------------------------------
            n = new Vector3D(0, 0, 1);
            vertexArray = new Vertex[4];
            vertexArray[0] = new Vertex(new Vector3D(-1, -1, 0), n, 0.0, 0.0);
            vertexArray[1] = new Vertex(new Vector3D(1, -1, 0), n, 1.0, 0.0);
            vertexArray[2] = new Vertex(new Vector3D(1, 1, 0), n, 1.0, 1.0);
            vertexArray[3] = new Vertex(new Vector3D(-1, 1, 0), n, 0.0, 1.0);
            triangleArray = new Triangle[2];
            triangleArray[0] = new Triangle(0, 1, 2);
            triangleArray[1] = new Triangle(2, 3, 0);
            textureArray = new Image[1];
            textureArray[0] = texture;
            textureRanges = new int[1][2];
            textureRanges[0][0] = 2;
            textureRanges[0][1] = 1;
            materialArray = new Material[1];
            materialArray[0] = theScene.defaultMaterial();
            materialArray[0].setDoubleSided(true);
            materialArray[0].setAmbient(new ColorRgb(1, 1, 1));
            materialArray[0].setDiffuse(new ColorRgb(1, 1, 1));
            materialArray[0].setSpecular(new ColorRgb(1, 1, 1));
            materialRanges = new int[1][2];
            materialRanges[0][0] = 2;
            materialRanges[0][1] = 0;

            mesh = new TriangleMesh();
            mesh.setVertexes(vertexArray);
            mesh.setTriangles(triangleArray);
            mesh.setTextures(textureArray);
            mesh.setTextureRanges(textureRanges);
            mesh.setMaterials(materialArray);
            mesh.setMaterialRanges(materialRanges);

            //-----------------------------------------------------------------
            boxBody = new SimpleBody();
            boxBody.setGeometry(mesh);
            boxBody.setPosition(position);
            boxBody.setScale(scale);
            boxBody.setRotation(R);
            boxBody.setRotationInverse(R.inverse());
            boxBody.setMaterial(theScene.defaultMaterial());
            boxBody.getMaterial().setDoubleSided(true);
            boxBody.getMaterial().setAmbient(new ColorRgb(1, 1, 1));
            boxBody.getMaterial().setDiffuse(new ColorRgb(1, 1, 1));
            boxBody.getMaterial().setSpecular(new ColorRgb(1, 1, 1));
            boxBody.setName("Proyected view box");
            boxBody.setTexture(texture);
            //-----------------------------------------------------------------
            group.getBodies().add(boxBody);
        }
        return group;
    }

    private void debugProjectedViewsIfNeeded(GL glAppContext)
    {
        //-----------------------------------------------------------------
        if ( wantToDebugProjectedViews == false ) {
            return;
        }
        wantToDebugProjectedViews = false;

        //-----------------------------------------------------------------
        int selectedThing = theScene.selectedThings.firstSelected();
        SimpleBody referenceBody = null;
        int i;

        if ( selectedThing >= 0 ) {
            referenceBody = theScene.scene.getSimpleBodies().get(selectedThing);
        }

        if ( referenceBody == null ) {
            parent.statusMessage.setText("ERROR: An object must be selected for projected views debugging to be created");
        }
        else {
            SimpleBodyGroup group;
            SimpleBodyGroup bodySet;
            bodySet = new SimpleBodyGroup();

            for ( i = 0; i < theScene.selectedThings.size(); i++ ) {
                if ( theScene.selectedThings.isSelected(i) ) {
                    referenceBody = theScene.scene.getSimpleBodies().get(i);
                    bodySet.getBodies().add(referenceBody);
                }
            }

            group = addDebugProjectedView(glAppContext, bodySet);

            if ( group != null ) {
                theScene.debugThingGroups.add(group);
            }
            else {
                parent.statusMessage.setText("ERROR: cannot create Pbuffer, you need recent 3D hardware acceleration for this function");
            }
        }
    }

    private void copyColorBufferIfNeeded(GL gl)
    {
        if ( wantToGetColor ) {
            parent.zbufferImage = JoglRGBImageRenderer.getImageJOGL(gl);
            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.zbufferImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.zbufferImage);
            }
            parent.imageControlWindow.redrawImage();
            parent.statusMessage.setText("ZBuffer Color Image obtained!");
            wantToGetColor = false;
        }
    }
    private void copyZBufferIfNeeded(GL gl)
    {
        if ( wantToGetDepth ) {
            if ( wantToGetContourns ) {
                IndexedColorImage zbuffer;
                NormalMap nm;
                zbuffer = JoglZBufferRenderer.importJOGLZBuffer(gl).exportIndexedColorImage();
                nm = new NormalMap();
                nm.importBumpMap(zbuffer, new Vector3D(1, 1, 0.1));
                parent.zbufferImage = nm.exportToRgbImageGradient();
              }
              else {
                parent.zbufferImage =
                    JoglZBufferRenderer.importJOGLZBuffer(gl).exportRGBImage(
                        parent.palette);
            }

            if ( parent.imageControlWindow == null ) {
                parent.imageControlWindow = new SwingImageControlWindow(parent.zbufferImage, parent.gui, parent.executorPanel);
            }
            else {
                parent.imageControlWindow.setImage(parent.zbufferImage);
            }
            parent.imageControlWindow.redrawImage();
            parent.statusMessage.setText("ZBuffer depth map obtained!");
            wantToGetDepth = false;
            wantToGetContourns = false;
        }
    }

    private void displayView(GL gl, JoglView view)
    {
        if ( renderMode == RENDER_MODE_ZBUFFER ) {
            JoglSceneRenderer.draw(gl, theScene);
        }
        else {
            JoglSceneRenderer.drawBackground(gl, theScene);
            if ( views.get(0).useFullContainerViewportArea() ) {
                System.out.println("RAYTRACING COMPLETO");
                parent.raytracedImageWidth = globalViewportXSize;
                parent.raytracedImageHeight = globalViewportYSize;
            }
            else {
                System.out.println("RAYTRACING PARCIAL");

                parent.raytracedImageWidth = views.get(0).getViewportRequestedSizeXInPixels();
                parent.raytracedImageHeight = views.get(0).getViewportRequestedSizeYInPixels();
            }

            parent.doRaytracedImage();
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            JoglImageRenderer.draw(gl, parent.raytracedImage);
            gl.glPopMatrix();
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(gl.GL_MODELVIEW);
        }

        //-----------------------------------------------------------------
        drawVisualRayDebug(gl);

        //-----------------------------------------------------------------
        // Note that gizmo information will not be reported, as they damage
        // the zbuffer...
        copyZBufferIfNeeded(gl);

        // Must be the last to draw
        drawGizmos(gl);

        copyColorBufferIfNeeded(gl);
    }

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        debugProjectedViewsIfNeeded(gl);

        JoglView view;
        int i;

        //-----------------------------------------------------------------
        gl.glClearColor(0.77f, 0.77f, 0.77f, 1.0f);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glViewport(0, 0, globalViewportXSize, globalViewportYSize);
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        for ( i = 0; i < views.size(); i++ ) {
            view = views.get(i);
            view.drawBorderGL(gl, globalViewportXSize, globalViewportYSize);
        }

        //-----------------------------------------------------------------
        for ( i = 0; i < views.size(); i++ ) {
            view = views.get(i);

            if ( !view.isActive() ) {
                continue;
            }
            //
            view.activateViewportGL(gl, globalViewportXSize, globalViewportYSize);
            if ( view.isSelected() ) {
                cameraController.setCamera(view.getCamera());
            }
            theScene.activeCamera = view.getCamera();

            //
            displayView(gl, view);
        }
    }

    private void drawVisualRayDebugSegment(GL gl, Vector3D start, Vector3D end, boolean follow, double w, double tip)
    {
        double l;
        Vector3D diff = end.substract(start);
        l = diff.length();

        JoglMaterialRenderer.activate(gl, visualDebugMaterial);

        //-----------------------------------------------------------------
        Geometry a;

        if ( l > tip ) {
            a = new Arrow(l - tip, tip, w/2, w);
        }
        else {
            a = new Cone(w/2, w/2, l);
        }
        Matrix4x4 R = new Matrix4x4();
        double yaw, pitch;
        yaw = diff.obtainSphericalThetaAngle();
        pitch = diff.obtainSphericalPhiAngle();
        R.eulerAnglesRotation(Math.toRadians(180)+yaw, pitch, 0);

        gl.glPushMatrix();
        gl.glTranslated(start.x, start.y, start.z);
        JoglMatrixRenderer.activate(gl, R);
        JoglGeometryRenderer.draw(gl, a, theScene.camera, qualitySelectionVisualDebug);
        gl.glPopMatrix();

        //-----------------------------------------------------------------
        if ( follow ) {
            Sphere s = new Sphere(0.025);
            Vector3D p;
            double offset = 0.1;
            int i;
            diff.normalize();
            for ( i = 0; i < 3; i++, offset += 0.1 ) {
                p = end.add(diff.multiply(offset));
                gl.glPushMatrix();
                gl.glTranslated(p.x, p.y, p.z);
                JoglGeometryRenderer.draw(gl, s, theScene.camera, qualitySelectionVisualDebug);
                gl.glPopMatrix();
            }
        }
    }

    private void drawVisualRayDebug(GL gl, Ray ray, int level)
    {
        gl.glLoadIdentity();
        if ( level < 0 ) return;

        //-----------------------------------------------------------------
        Vector3D p;
        Vector3D d = new Vector3D(ray.direction);
        d.normalize();
        GeometryIntersectionInformation info;

        info = new GeometryIntersectionInformation();

        //-----------------------------------------------------------------
        visualDebugMaterial.setDiffuse(new ColorRgb(0.9, 0.5, 0.0));
        JoglMaterialRenderer.activate(gl, visualDebugMaterial);
        Sphere s = new Sphere(0.05);
        gl.glPushMatrix();
        gl.glTranslated(ray.origin.x, ray.origin.y, ray.origin.z);
        JoglGeometryRenderer.draw(gl, s, theScene.camera, qualitySelectionVisualDebug);
        gl.glPopMatrix();

        //-----------------------------------------------------------------
        if ( parent.theScene.doIntersection(ray, info) ) {
            d = d.multiply(ray.t);
            p = ray.origin.add(d);

            drawVisualRayDebugSegment(gl, ray.origin, p, false, 0.07, 0.4);
            if ( level >= 1 ) {
                // Draw normal
                visualDebugMaterial.setDiffuse(new ColorRgb(0.9, 0.9, 0.5));
                drawVisualRayDebugSegment(gl, p, p.add(info.n.multiply(0.5)), false, 0.05, 0.2);
            }
            // Reflection ray
            Vector3D dd = ray.direction.multiply(-1);
            dd.normalize();
            Vector3D h = info.n.multiply(dd.dotProduct(info.n)).substract(dd);
            Ray subray = new Ray(p, dd.add(h.multiply(2)));
            subray.origin = subray.origin.add(subray.direction.multiply(VSDK.EPSILON*10.0));
            drawVisualRayDebug(gl, subray, level-1);
        }
        else {
            d = d.multiply(1.4);
            p = ray.origin.add(d);
            drawVisualRayDebugSegment(gl, ray.origin, p, true, 0.07, 0.4);
        }
    }

    private void drawVisualRayDebug(GL gl)
    {
        //-----------------------------------------------------------------
        if ( !parent.withVisualDebugRay ) {
            return;
        }

        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_LIGHTING);
        drawVisualRayDebug(gl, parent.visualDebugRay, parent.visualDebugRayLevels);
        //-----------------------------------------------------------------
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
        this.globalViewportXSize = width;
        this.globalViewportYSize = height;
        views.get(0).updateViewportConfiguration(globalViewportXSize, globalViewportYSize);
    }   

  public void mouseEntered(MouseEvent e) {
      canvas.requestFocusInWindow();

      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }
  }

  public void mouseExited(MouseEvent e) {
      //System.out.println("Mouse exited");
  }

  public void mousePressed(MouseEvent e) {
      //-----------------------------------------------------------------
      int i;
      double xpercent;
      double ypercent;
      JoglView view;

      xpercent = ((double)e.getX()) / ((double)globalViewportXSize);
      ypercent = 1-((double)e.getY()) / ((double)globalViewportYSize);

      for ( i = 0; i < views.size(); i++ ) {
          view = views.get(i);
          if ( view.inside(xpercent, ypercent) ) {
              view.setSelected(true);
              selectedView = i;
          }
          else {
              view.setSelected(false);
          }
      }

      //-----------------------------------------------------------------
      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded
      int m = e.getModifiersEx();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           (m & e.BUTTON1_DOWN_MASK) != 0 ) {
          canvas.setCursor(camrotateCursor);
      }
      else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                (m & e.BUTTON2_DOWN_MASK) != 0 ) {
          canvas.setCursor(camtranslateCursor);
      }
      else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                (m & e.BUTTON3_DOWN_MASK) != 0 ) {
          canvas.setCursor(camadvanceCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMousePressedEventAwt(e) ) {
          ;
        }
        else if ( interactionMode == SELECT_INTERACTION_MODE ||
                  interactionMode == TRANSLATE_INTERACTION_MODE || 
                  interactionMode == ROTATE_INTERACTION_MODE || 
                  interactionMode == SCALE_INTERACTION_MODE 
                 ) {
          boolean composite = false;
          if ( ((e.getModifiersEx()) & e.CTRL_DOWN_MASK) != 0x0 ) {
              composite = true;
          }
          int f = theScene.selectedThings.firstSelected();
          view = views.get(selectedView);
          if ( view.useFullContainerViewportArea() ) {
              theScene.selectObjectWithMouse(e.getX()-view.getViewportStartX(),
                                             e.getY()-view.getViewportStartY(),
                                             composite);
          }

          if ( f >= 0 && theScene.selectedThings.firstSelected() < 0 &&
               interactionMode == TRANSLATE_INTERACTION_MODE &&
               translationGizmo.isActive() ) {
              theScene.selectedThings.select(f);
          }
          reportObjectSelection();
      }
      canvas.repaint();
  }

  public void mouseReleased(MouseEvent e) {
      // WARNING / TODO
      // There should be a cameraController.getFutureAction(e) that calculates
      // the proper icon for display ... here an Aquynza operation is
      // assumed and hard-coded

      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE ) {
          canvas.setCursor(camrotateCursor);
      }
      else {
          canvas.setCursor(selectCursor);
      }

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseReleasedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseReleasedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }

  }

  public void mouseClicked(MouseEvent e) {
      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseClickedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseClickedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }
  }

  public void mouseMoved(MouseEvent e) {
      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseMovedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseMovedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }
  }

  public void mouseDragged(MouseEvent e) {
      int firstThingSelected = theScene.selectedThings.firstSelected();

      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseDraggedEventAwt(e) ) {
          canvas.repaint();
      }
      else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                firstThingSelected >= 0 ) {
          Vector3D position;
          SimpleBody gi;

          gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

          Matrix4x4 composed;

          position = gi.getPosition();
          composed = new Matrix4x4(gi.getRotation());
          composed.M[0][3] = position.x;
          composed.M[1][3] = position.y;
          composed.M[2][3] = position.z;

          translationGizmo.setCamera(theScene.activeCamera);
          translationGizmo.setTransformationMatrix(composed);
          if ( translationGizmo.processMouseDraggedEventAwt(e) ) {
              composed = translationGizmo.getTransformationMatrix();
              position.x = composed.M[0][3];
              position.y = composed.M[1][3];
              position.z = composed.M[2][3];
              composed.M[0][3] = 0;
              composed.M[1][3] = 0;
              composed.M[2][3] = 0;
              applyTransformToSelectedObjects(position, composed);
              canvas.repaint();
          }
      }
  }

  /**
  WARNING: It is not working... check pending
  */
  public void mouseWheelMoved(MouseWheelEvent e) {
      System.out.println(".");
      if ( interactionMode == CAMERA_INTERACTION_MODE && 
           cameraController.processMouseWheelEventAwt(e) ) {
          canvas.repaint();
      }
  }

    public void keyPressed(KeyEvent e) {
        char unicode_id;
        int keycode;
        boolean skipKey = false;

        unicode_id = e.getKeyChar();
        keycode = e.getKeyCode();

        int firstThingSelected = theScene.selectedThings.firstSelected();

        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             cameraController.processKeyPressedEventAwt(e) ) {
            canvas.repaint();
        }
        else if ( interactionMode == SELECT_INTERACTION_MODE ) {
            if ( unicode_id == e.CHAR_UNDEFINED ) {
              switch ( keycode ) {
                case KeyEvent.VK_LEFT:
                  if ( theScene.selectedDebugThingGroups.numberOfSelections() < 1 ) {
                      theScene.selectedThings.selectPrevious();
                  }
                  if ( theScene.selectedThings.numberOfSelections() < 1 ) {
                      theScene.selectedDebugThingGroups.selectPrevious();
                  }
                  reportObjectSelection();
                  break;
                case KeyEvent.VK_RIGHT:
                  if ( theScene.selectedDebugThingGroups.numberOfSelections() < 1 ) {
                      theScene.selectedThings.selectNext();
                  }
                  if ( theScene.selectedThings.numberOfSelections() < 1 ) {
                      theScene.selectedDebugThingGroups.selectNext();
                  }
                  reportObjectSelection();
                  break;
              }
              canvas.repaint();
            }
        }
        else if ( interactionMode == TRANSLATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                Matrix4x4 composed;
                Vector3D position;
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                position = gi.getPosition();
                composed = new Matrix4x4(gi.getRotation());
                composed.M[0][3] = position.x;
                composed.M[1][3] = position.y;
                composed.M[2][3] = position.z;

                translationGizmo.setTransformationMatrix(composed);
                if ( translationGizmo.processKeyPressedEventAwt(e) ) {
                    composed = translationGizmo.getTransformationMatrix();
                    position.x = composed.M[0][3];
                    position.y = composed.M[1][3];
                    position.z = composed.M[2][3];
                    composed.M[0][3] = 0;
                    composed.M[1][3] = 0;
                    composed.M[2][3] = 0;
                    applyTransformToSelectedObjects(position, composed);
                    canvas.repaint();
                }
            }
        }
        else if ( interactionMode == ROTATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                Matrix4x4 R = gi.getRotation();

                rotateGizmo.setTransformationMatrix(R);

                if ( rotateGizmo.processKeyPressedEventAwt(e) ) {
                    R = rotateGizmo.getTransformationMatrix();
                    gi.setRotation(R);
                    Matrix4x4 Ri = new Matrix4x4(R);
                    Ri.invert();
                    gi.setRotationInverse(Ri);
                    canvas.repaint();
                }
            }
        }
        else if ( interactionMode == SCALE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                Vector3D s = gi.getScale();
                Matrix4x4 S = new Matrix4x4();
                S.M[0][0] = s.x;
                S.M[1][1] = s.y;
                S.M[2][2] = s.z;

                scaleGizmo.setTransformationMatrix(S);

                if ( scaleGizmo.processKeyPressedEventAwt(e) ) {
                    S = scaleGizmo.getTransformationMatrix();
                    s = new Vector3D(S.M[0][0], S.M[1][1], S.M[2][2]);
                    gi.setScale(s);
                    canvas.repaint();
                }
            }
        }

        // Global commands
        if ( keycode == KeyEvent.VK_ESCAPE ) System.exit(0);

        if ( qualityController.processKeyPressedEventAwt(e) ) {
            System.out.println(qualitySelection);
            canvas.repaint();
        }

        if ( keycode == KeyEvent.VK_DELETE ) {
            int  i;

            //-----------------------------------------------------------------
            for ( i = theScene.scene.getSimpleBodies().size()-1; i >= 0; i-- ) {
                if ( theScene.selectedThings.isSelected(i) ) {
                    theScene.scene.getSimpleBodies().remove(i);
                }
            }
            theScene.selectedThings.sync();
            //-----------------------------------------------------------------
            for ( i = theScene.debugThingGroups.size()-1; i >= 0; i-- ) {
                if ( theScene.selectedDebugThingGroups.isSelected(i) ) {
                    theScene.debugThingGroups.remove(i);
                }
            }
            theScene.selectedThings.sync();
            //-----------------------------------------------------------------
            canvas.repaint();
        }

        if ( keycode == KeyEvent.VK_F10 ) {
              parent.statusMessage.setText(
                  parent.gui.getMessage("IDM_COMPUTING_RAYTRACING"));
              parent.doRaytracedImage();
  
              if ( parent.imageControlWindow == null ) {
                  parent.imageControlWindow = new SwingImageControlWindow(parent.raytracedImage, parent.gui, parent.executorPanel);
              }
              else {
                  parent.imageControlWindow.setImage(parent.raytracedImage);
              }
              parent.imageControlWindow.redrawImage();
        }

        if ( keycode == KeyEvent.VK_0 ) {
            // Alphanumeric 0
            views.get(0).keyPressed(e);
            skipKey = true;
            canvas.repaint();
        }

        if ( keycode == KeyEvent.VK_9 ) {
            // Alphanumeric 0
            skipKey = true;
            switch ( renderMode ) {
              case RENDER_MODE_ZBUFFER:
                renderMode = RENDER_MODE_RAYTRACING;
                break;
              default:
                renderMode = RENDER_MODE_ZBUFFER;
                break;
            }
            canvas.repaint();
        }

        double theta = 0;
        double phi = Math.PI/2;

        if ( unicode_id != e.CHAR_UNDEFINED && !skipKey ) {
            switch ( unicode_id ) {
              //- Multiple views control -----------------------------------
              case '.':
                selectedView++;
                if ( selectedView >= views.size() ) {
                    selectedView = 0;
                }
                selectedView = viewOrganizer.doLayout(views, fullViewport?selectedView:-1, viewOrderStyle);
                canvas.repaint();
                break;
              case ',':
                viewOrderStyle++;
                selectedView = viewOrganizer.doLayout(views, fullViewport?selectedView:-1, viewOrderStyle);
                canvas.repaint();
                break;
              //- Visual debug ray control ---------------------------------
              case '4': // Numpad 4
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRay.origin.x -= 0.1;
                    canvas.repaint();
                }
                break;
              case '6': // Numpad 6
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRay.origin.x += 0.1;
                    canvas.repaint();
                }
                break;
              case '8': // Numpad 8
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRay.origin.y += 0.1;
                    canvas.repaint();
                }
                break;
              case '2': // Numpad 2
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRay.origin.y -= 0.1;
                    canvas.repaint();
                }
                break;
              case '1': // Numpad 1
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRay.origin.z -= 0.1;
                    canvas.repaint();
                }
                break;
              case '7': // Numpad 7
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRay.origin.z += 0.1;
                    canvas.repaint();
                }
                break;
              case '9': // Numpad 9
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRayLevels++;
                    canvas.repaint();
                }
                break;
              case '3': // Numpad 3
                if ( parent.withVisualDebugRay ) {
                    parent.visualDebugRayLevels--;
                    if ( parent.visualDebugRayLevels < 0 ) {
                        parent.visualDebugRayLevels = 0;
                  }
                    canvas.repaint();
                }
                break;
              case '5': // Numpad 5
                if ( parent.withVisualDebugRay ) {
                    parent.withVisualDebugRay = false;
                }
                else {
                    parent.withVisualDebugRay = true;
                }
                canvas.repaint();
                break;
              case '*': // Numpad *
                if ( parent.withVisualDebugRay ) {
                    theta =
                     parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                    phi =
                     parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                    theta -= Math.toRadians(5);
                    parent.visualDebugRay.direction.setSphericalCoordinates(
                     1, theta, phi);
                    canvas.repaint();
                }
                break;
              case '/': // Numpad /
                if ( parent.withVisualDebugRay ) {
                    theta =
                     parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                    phi =
                     parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                    theta += Math.toRadians(5);
                    parent.visualDebugRay.direction.setSphericalCoordinates(
                     1, theta, phi);
                    canvas.repaint();
                }
                break;
              case '+': // Numpad +
                if ( parent.withVisualDebugRay ) {
                    theta =
                     parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                    phi =
                     parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                    phi += Math.toRadians(5);
                    if ( phi > Math.PI ) phi = Math.PI;
                    parent.visualDebugRay.direction.setSphericalCoordinates(
                     1, theta, phi);
                    canvas.repaint();
                }
                break;
              case '-': // Numpad -
                if ( parent.withVisualDebugRay ) {
                    theta =
                     parent.visualDebugRay.direction.obtainSphericalThetaAngle();
                    phi =
                     parent.visualDebugRay.direction.obtainSphericalPhiAngle();
                    phi -= Math.toRadians(5);
                    if ( phi < 0 ) phi = 0;
                    parent.visualDebugRay.direction.setSphericalCoordinates(
                     1, theta, phi);
                    canvas.repaint();
                }
                break;
              //------------------------------------------------------------

              case 't':
                if ( firstThingSelected >= 0 ) {
                    SimpleBody gi;
                    Image texture;
                    gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                    texture = gi.getTexture();
                    if ( texture == null ) {
                        String imageFilename = "../../../etc/textures/miniearth.png";
                        try {
                            texture = 
                             ImagePersistence.importRGB(new File(imageFilename));
                        }
                        catch ( Exception ee ) {}
                        gi.setTexture(texture);
                    }
                    else {
                        gi.setTexture(null);
                    }
                }
                canvas.repaint();
                break;
              case 'b':
                if ( firstThingSelected >= 0 ) {
                    SimpleBody gi;
                    IndexedColorImage source = null;
                    NormalMap normalMap;
                    RGBImage exported;
                    gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                    normalMap = gi.getNormalMap();
                    if ( normalMap == null ) {
                        try {
                            normalMap = new NormalMap();
                            //String imageFilename = "../../../etc/bumpmaps/blinn2.bw";
                            String imageFilename = "../../../etc/bumpmaps/earth.bw";
                            source = ImagePersistence.importIndexedColor(new File(imageFilename));
                            normalMap.importBumpMap(source, new Vector3D(1, 1, 0.2));

                            exported = normalMap.exportToRgbImage();
                            //ImagePersistence.exportPPM(new File("./outputmap.ppm"), exported);
                        }
                        catch ( Exception ee ) {
                            System.err.println(ee);
                            ee.printStackTrace();
                        }
                        gi.setNormalMap(normalMap);
                      }
                      else {
                        gi.setNormalMap(null);
                    }
                }
                canvas.repaint();
                break;
              case 'h':
                //-------------------------------------------------------------
                if ( parent.selectorDialog == null ) {
                    parent.selectorDialog = new SwingSelectorDialog();
                }
                parent.selectorDialog.setVisible(true);
                parent.selectorDialog.repaint();
                //-------------------------------------------------------------


                SimpleBody o;
                int i;
                ArrayList generic = theScene.scene.getSimpleBodies();
                String msg = "";

                for ( i = 0; i < generic.size(); i++ ) {
                    System.out.println("Consultando cosa " + i + ":");
                    o = (SimpleBody)generic.get(i);
                    try {
                        Method m = o.getClass().getMethod("getName", (Class[])null);
                        if ( !(m.getReturnType().isInstance(msg)) ) {
                            throw new Exception("Wrong method signature");
                        }
                        msg = (String)m.invoke(o);
                    }
                    catch ( Exception ee ) {
                        msg = null;
                    }
                    if ( msg == null || msg.equals("") ) {
                        msg = "Not named object";
                    }
                    System.out.println("Object: " + msg);
                }
                break;

              case 'c':
                statusMessage.setText("Camera mode interaction - drag mouse with different buttons over the scene to change current camera.");
                interactionMode = CAMERA_INTERACTION_MODE;
                canvas.repaint();
                break;

              case 'q':
                statusMessage.setText("Selection mode interaction - click mouse to select objects, LEFT/RIGHT arrow keys to select sequencialy.");
                interactionMode = SELECT_INTERACTION_MODE;
                canvas.repaint();
                break;

              case 'w':
                if ( ((e.getModifiersEx()) & e.ALT_DOWN_MASK) != 0x0 ) {
                    if ( fullViewport ) {
                        fullViewport = false;
                    }
                    else {
                        fullViewport = true;
                    }
                    selectedView = viewOrganizer.doLayout(views, fullViewport?selectedView:-1, viewOrderStyle);
                }
                else {
                    statusMessage.setText("Translation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to move it.");
                    interactionMode = TRANSLATE_INTERACTION_MODE;
                }
                canvas.repaint();
                break;

              case 'e':
                statusMessage.setText("Rotation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to rotate it.");
                interactionMode = ROTATE_INTERACTION_MODE;
                canvas.repaint();
                break;

              case 'r':
                statusMessage.setText("Scale mode interaction - click mouse to select objects, X, Y, Z/ARROWS keys and gizmo to scale it.");
                interactionMode = SCALE_INTERACTION_MODE;
                canvas.repaint();
                break;

              case 'g':
                if ( theScene.showGrid == true ) {
                    theScene.showGrid = false;
                }
                else {
                    theScene.showGrid = true;
                }
                canvas.repaint();
                break;
            }
        }

        if ( interactionMode == CAMERA_INTERACTION_MODE ) {
            canvas.setCursor(camrotateCursor);
        }
        else {
            canvas.setCursor(selectCursor);
        }
    }

  private void applyTransformToSelectedObjects(Vector3D position,
                                               Matrix4x4 rotation)
  {
      SimpleBody gi;
      int firstThingSelected = theScene.selectedThings.firstSelected();
      int i;

      for ( i = 0; i < theScene.selectedThings.size(); i++ ) {
          if ( !theScene.selectedThings.isSelected(i) ) continue;
          gi = theScene.scene.getSimpleBodies().get(i);

          gi.setPosition(position);
          gi.setRotation(rotation);
          rotation = new Matrix4x4(rotation);
          rotation.invert();
          gi.setRotationInverse(rotation);
      }
  }

  public void newView()
  {
      views.add(new JoglView());
      selectedView = viewOrganizer.doLayout(views, fullViewport?selectedView:-1, viewOrderStyle);
  }

  public void delView()
  {
      if ( views.size() > 1 ) {
          views.remove(views.size()-1);
      }

      selectedView = viewOrganizer.doLayout(views, fullViewport?selectedView:-1, viewOrderStyle);
  }

  private void reportObjectSelection()
  {
      String msg = "";
      int n;

      //-----------------------------------------------------------------
      theScene.selectedThings.sync();
      n = theScene.selectedThings.numberOfSelections();
      if ( n == 0 ) {
          msg += "All things are UNSELECTED";
      }
      else if ( n == 1 ) {
          int f = theScene.selectedThings.firstSelected();
          msg = "Thing [" + f + "] selected, which is a [" + 
     ((SimpleBody)(theScene.scene.getSimpleBodies().get(f))).getGeometry().getClass().getName() 
          + "]";
      }
      else {
          msg += "" + n + " things selected";
      }

      //-----------------------------------------------------------------
      theScene.selectedDebugThingGroups.sync();
      n = theScene.selectedDebugThingGroups.numberOfSelections();
      if ( n == 0 ) {
          msg += "; All visual debug groups are UNSELECTED";
      }
      else if ( n == 1 ) {
          int f = theScene.selectedDebugThingGroups.firstSelected();
          msg += "; Debug group [" + f + "] selected.";
      }
      else {
          msg += "; " + n + " debug groups selected";
      }

      //-----------------------------------------------------------------
      statusMessage.setText(msg);
  }

    public void keyReleased(KeyEvent e) {
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
            cameraController.processKeyReleasedEventAwt(e) ) {
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
