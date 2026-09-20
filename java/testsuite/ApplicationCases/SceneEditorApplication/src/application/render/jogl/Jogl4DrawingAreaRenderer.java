package application.render.jogl;

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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JLabel;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.geometry.element.Triangle;
import vsdk.toolkit.environment.geometry.element.Vertex;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.IndexedColorImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.Jogl2BackgroundRenderer;
import vsdk.toolkit.render.jogl.Jogl2MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl2SimpleMaterialRenderer;
import vsdk.toolkit.render.jogl.Jogl2ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl2GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl2TranslateGizmoRenderer;
import vsdk.toolkit.render.jogl.Jogl2RotateGizmoRenderer;
import vsdk.toolkit.render.jogl.Jogl2ScaleGizmoRenderer;
import vsdk.toolkit.render.jogl.Jogl2RGBImageUncompressedRenderer;
import vsdk.toolkit.render.jogl.Jogl2ZBufferRenderer;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.processing.ImageProcessing;

// Application classes
import application.SceneEditorApplication;
import application.framework.Scene;
import application.gui.SwingImageControlWindow;
import application.gui.SwingSelectorDialog;
import application.model.ApplicationModel;
import framework.gui.ViewportInteractionTechniques;
import framework.gui.ViewportSetInteractionTechniques;
import framework.model.Viewport;
import framework.model.ViewportSet;
import framework.render.jogl4.Jogl4LabelImageProvider;
import framework.render.jogl4.Jogl4ViewportWindow;
import framework.render.jogl4.JoglViewportSetRenderer;
import java.awt.event.MouseEvent;

public class Jogl4DrawingAreaRenderer implements 
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
    private final RendererConfiguration qualitySelectionVisualDebug;
    private final TranslateGizmo translationGizmo;
    private final RotateGizmo rotateGizmo;
    private final ScaleGizmo scaleGizmo;
    private final SimpleMaterial visualDebugMaterial;
    private final ViewportInteractionTechniques interactionTechniques;

    private final Scene theScene;
    private final ApplicationModel model;
    private final JLabel statusMessage;

    public int interactionMode;
    public int lastInteractionMode;
    private boolean translationGizmoDrawn;

    public boolean wantToGetColor;
    public boolean wantToGetDepth;
    public boolean wantToGetContourns;
    public boolean wantToDebugProjectedViews;

    private File pendingViewportExportFile;
    private boolean pendingViewportExportJpg;
    private File pendingWorkspaceExportFile;

    private final Jogl4ProjectedViewRenderer projectedViewRenderer;

    private Cursor camrotateCursor;
    private Cursor camtranslateCursor;
    private Cursor camadvanceCursor;
    private Cursor selectCursor;

    SceneEditorApplication parent;

    private final boolean doDistanceField;
    private final int distanceFieldSide;
    private int awtViewportWidth;
    private int awtViewportHeight;

    //=================================================================
    private final ViewportSet viewportSet;
    private final ViewportSetInteractionTechniques viewportSetTechniques;
    private final JoglViewportSetRenderer viewportSetRenderer;

    //=================================================================

    public Jogl4DrawingAreaRenderer(ApplicationModel model, JLabel statusMessage, SceneEditorApplication parent)
    {
        this.parent = parent;
        this.model = model;
        this.theScene = model.getScene();
        this.statusMessage = statusMessage;

        interactionMode = CAMERA_INTERACTION_MODE;
        lastInteractionMode = CAMERA_INTERACTION_MODE;
        translationGizmoDrawn = false;
        awtViewportWidth = 0;
        awtViewportHeight = 0;
        createCursors();

        qualitySelection = theScene.qualityTemplate;
        interactionTechniques = new ViewportInteractionTechniques(theScene.camera, qualitySelection);
        translationGizmo = interactionTechniques.getTranslationGizmo();
        qualitySelectionVisualDebug = new RendererConfiguration();
        qualitySelectionVisualDebug.setShadingType(
            RendererConfiguration.SHADING_TYPE_GOURAUD);
        rotateGizmo = interactionTechniques.getRotateGizmo();
        scaleGizmo = interactionTechniques.getScaleGizmo();

        visualDebugMaterial = theScene.defaultMaterial();

        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDepthBits(32);
        canvas = new GLCanvas(capabilities);

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
            projectedViewRenderer = new Jogl4ProjectedViewRenderer(distanceFieldSide, distanceFieldSide, true);
        }
        else {
            projectedViewRenderer = new Jogl4ProjectedViewRenderer(640, 640, true);
        }

        //-----------------------------------------------------------------
        // This drawing area presents the viewport set of the active display
        viewportSet = model.getActiveViewportSet();
        viewportSetTechniques = new ViewportSetInteractionTechniques(viewportSet);
        viewportSetRenderer = new JoglViewportSetRenderer(
            viewportSet,
            new Jogl4LabelImageProvider() {
                @Override
                public RGBAImageUncompressed createLabelImage(String text, ColorRgb color) {
                    return AwtSystem.calculateLabelImage(text, color);
                }
            },
            new JoglViewportSetRenderer.ViewRenderer() {
                @Override
                public void configureView(Jogl4ViewportWindow view) {
                    interactionTechniques.setCamera(view.getCamera());
                    interactionTechniques.setRendererConfiguration(view.getRendererConfiguration());
                    qualitySelection = view.getRendererConfiguration();
                }

                @Override
                public void drawView(GL2 gl, Jogl4ViewportWindow view) {
                    theScene.activeCamera = view.getCamera();
                    theScene.qualityTemplate = view.getRendererConfiguration();
                    Jogl4DrawingAreaRenderer.this.drawView(gl, view);
                }
            });
    }

    private void syncViewportStateFromSurface(int surfaceWidth, int surfaceHeight)
    {
        if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
            return;
        }

        if ( awtViewportWidth <= 0 || awtViewportHeight <= 0 ) {
            awtViewportWidth = canvas.getWidth();
            awtViewportHeight = canvas.getHeight();
        }

        if ( surfaceWidth == viewportSet.getSizeXInPixels() &&
             surfaceHeight == viewportSet.getSizeYInPixels() ) {
            return;
        }

        viewportSet.resize(surfaceWidth, surfaceHeight);
    }

    private int scaleXToSurface(int x)
    {
        if ( awtViewportWidth <= 0 || viewportSet.getSizeXInPixels() <= 0 ) {
            return x;
        }
        return (int)Math.round(((double)x * (double)viewportSet.getSizeXInPixels()) /
            (double)awtViewportWidth);
    }

    private int scaleYToSurface(int y)
    {
        if ( awtViewportHeight <= 0 || viewportSet.getSizeYInPixels() <= 0 ) {
            return y;
        }
        return (int)Math.round(((double)y * (double)viewportSet.getSizeYInPixels()) /
            (double)awtViewportHeight);
    }

    /**
    Converts an AWT mouse event to a vitral one, with coordinates expressed in
    pixels of the drawing surface (which can differ from canvas' ones in
    high density displays).
    */
    private vsdk.toolkit.gui.MouseEvent toSurfaceEvent(java.awt.event.MouseEvent e)
    {
        vsdk.toolkit.gui.MouseEvent surfaceEvent = AwtSystem.awt2vsdkEvent(e);

        surfaceEvent.setX(scaleXToSurface(surfaceEvent.getX()));
        surfaceEvent.setY(scaleYToSurface(surfaceEvent.getY()));
        return surfaceEvent;
    }

    /**
    Converts an AWT mouse event to a vitral one, with coordinates relative to
    the given viewport.
    */
    private vsdk.toolkit.gui.MouseEvent toViewportEvent(java.awt.event.MouseEvent e,
                                                        Jogl4ViewportWindow view)
    {
        return viewportSetTechniques.toViewportEvent(toSurfaceEvent(e), view.getViewport());
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

    private boolean shouldDrawTranslationGizmo()
    {
        return interactionMode == SELECT_INTERACTION_MODE ||
            interactionMode == TRANSLATE_INTERACTION_MODE ||
            (interactionMode == CAMERA_INTERACTION_MODE &&
             lastInteractionMode == TRANSLATE_INTERACTION_MODE);
    }

    private void drawGizmos(GL2 gl)
    {
        // Pending: Turn off scene light and turn on gizmo specific lighting
        translationGizmoDrawn = false;

        translationGizmo.setCamera(theScene.activeCamera);

        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);

        int firstThingSelected = theScene.selectedThings.firstSelected();

        if ( shouldDrawTranslationGizmo() ) {
            if ( firstThingSelected >= 0 ) {
                Vector3Dd position;
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                Matrix4x4d composed;

                position = gi.getPosition();
                //composed = new Matrix4x4d(gi.getRotation());
                composed = new Matrix4x4d();
                composed = composed.withVal(0, 3, position.x());
                composed = composed.withVal(1, 3, position.y());
                composed = composed.withVal(2, 3, position.z());
                translationGizmo.setTransformationMatrix(composed);

                Jogl2TranslateGizmoRenderer.draw(gl, translationGizmo);
                translationGizmoDrawn = true;
            }
        }
        else if ( interactionMode == ROTATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                Vector3Dd position;
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                position = gi.getPosition();
                rotateGizmo.setTransformationMatrix(gi.getRotation());
                Jogl2RotateGizmoRenderer.draw(gl, rotateGizmo, position);
            }
        }
        else if ( interactionMode == SCALE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                Vector3Dd position;
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                position = gi.getPosition();
                scaleGizmo.setTransformationMatrix(gi.getRotation());
                Jogl2ScaleGizmoRenderer.draw(gl, scaleGizmo, position);
            }
        }
        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    private Image createProjectedView(GL2 gl, SimpleBodyGroup referenceBodies, int cam)
    {
        //- Will render a normalized body inside the unit cube ------------
        double minmax[];
        SimpleBodyGroup bodySet = new SimpleBodyGroup();
        int i;

        Vector3Dd p;
        {
            //-----------------------------------------------------------------
            minmax = referenceBodies.getMinMax();
            Vector3Dd min, max, s;
            min = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
            max = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
            s = new Vector3Dd(max.x() - min.x(), max.y() - min.y(), max.z() - min.z());

            double maxsize = s.x();
            if ( s.y() > maxsize ) maxsize = s.y();
            if ( s.z() > maxsize ) maxsize = s.z();
            // The 95% scale factor is to allow a full render of the object to
            // fit inside the rendered view
            s = new Vector3Dd((2/maxsize) * 0.95, (2/maxsize) * 0.95,
                (2/maxsize) * 0.95);

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
            Matrix4x4d Mset = bodySet.getTransformationMatrix(), R, Ri, Mbody, S, M;
            SimpleBody copiedBody;
            Quaterniond q;

            for ( i = 0; i < referenceBodies.getBodies().size(); i++ ) {
                referenceBody = referenceBodies.getBodies().get(i);
                if ( cam == 1 ) {
                    copiedBody = theScene.addThing(referenceBody.getGeometry());
                    Mbody = referenceBody.getTransformationMatrix();
                    M = Mset.multiply(Mbody);
                    p = M.extractTranslation();
                    M = M.withVal(0, 3, 0.0);
                    M = M.withVal(1, 3, 0.0);
                    M = M.withVal(2, 3, 0.0);
                    q = M.exportToQuaternion().normalized();
                    R = new Matrix4x4d();
                    R = R.importFromQuaternion(q);
                    Ri = R.inverse();
                    S = Ri.multiply(M);
                    s = new Vector3Dd(M.get(0, 0), M.get(1, 1), M.get(2, 2));

                    copiedBody.setPosition(p);
                    copiedBody.setScale(s);
                    copiedBody.setRotation(R);
                }
            }

            //-----------------------------------------------------------------
        }

        //- Render will proceed in a PBuffer ------------------------------
        IndexedColorImageUncompressed distanceFieldIndexed;

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
            distanceFieldIndexed = new IndexedColorImageUncompressed();
            distanceFieldIndexed.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(projectedViewRenderer.image, distanceFieldIndexed, 1);
            ImageProcessing.gammaCorrection(distanceFieldIndexed, 2.0);

            RGBAImageUncompressed distanceFieldRgba;
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
    addDebugProjectedView(GL2 gl, SimpleBodyGroup referenceBodies)
    {
        SimpleBody boxBody;
        Image texture;
        SimpleBodyGroup group;
        Vector3Dd position = new Vector3Dd(0, 0, 0);
        Vector3Dd scale = new Vector3Dd(1, 1, 1);
        Matrix4x4d R;
        Matrix4x4d R1 = new Matrix4x4d();
        Matrix4x4d R2 = new Matrix4x4d();
        int i;
        double delta = 0.01/2.0;
        TriangleMesh mesh;
        Vertex[] vertexArray;
        Triangle[] triangleArray;
        Vector3Dd n;
        Image textureArray[];
        SimpleMaterial materialArray[];
        int materialRanges[][];
        int textureRanges[][];

        group = new SimpleBodyGroup();
        for ( i = 1; i <= 13; i++ ) {
            R = new Matrix4x4d();
            switch ( i ) {
              case 1:
                position = new Vector3Dd(0, -2, 0);
                R = R.axisRotation(Math.toRadians(90), new Vector3Dd(1, 0, 0));
                break;
              case 2:
                position = new Vector3Dd(-2, 0, 0);
                R1 = R1.axisRotation(Math.toRadians(90), new Vector3Dd(0, 0, -1));
                R2 = R2.axisRotation(Math.toRadians(90), new Vector3Dd(0, -1, 0));
                R = R2.multiply(R1);
                break;
              case 3:
                position = new Vector3Dd(0, 0, -2);
                R = R.axisRotation(Math.toRadians(180), new Vector3Dd(0, 1, 0));
                break;
              case 4:
                position = new Vector3Dd(-1, -1, 1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(45), new Vector3Dd(0, 0, -1));
                R2 = R2.axisRotation(Math.toRadians(35), new Vector3Dd(1, -1, 0));
                R = R2.multiply(R1);
                break;
              case 5:
                position = new Vector3Dd(1, -1, 1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(45), new Vector3Dd(0, 0, 1));
                R2 = R2.axisRotation(Math.toRadians(35), new Vector3Dd(1, 1, 0));
                R = R2.multiply(R1);
                break;
              case 6:
                position = new Vector3Dd(1, 1, 1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(135), new Vector3Dd(0, 0, 1));
                R2 = R2.axisRotation(Math.toRadians(35), new Vector3Dd(-1, 1, 0));
                R = R2.multiply(R1);
                break;
              case 7:
                position = new Vector3Dd(-1, 1, 1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(135), new Vector3Dd(0, 0, -1));
                R2 = R2.axisRotation(Math.toRadians(35), new Vector3Dd(-1, -1, 0));
                R = R2.multiply(R1);
                break;
              case 8:
                position = new Vector3Dd(0, 1, -1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(180), new Vector3Dd(0, 0, 1));
                R2 = R2.axisRotation(Math.toRadians(135), new Vector3Dd(-1, 0, 0));
                R = R2.multiply(R1);
                break;
              case 9:
                position = new Vector3Dd(-1, 0, -1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(90), new Vector3Dd(0, 0, -1));
                R2 = R2.axisRotation(Math.toRadians(135), new Vector3Dd(0, -1, 0));
                R = R2.multiply(R1);
                break;
              case 10:
                position = new Vector3Dd(0, -1, -1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R = R.axisRotation(Math.toRadians(135), new Vector3Dd(1, 0, 0));
                break;
              case 11:
                position = new Vector3Dd(1, 0, -1);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(90), new Vector3Dd(0, 0, 1));
                R2 = R2.axisRotation(Math.toRadians(135), new Vector3Dd(0, 1, 0));
                R = R2.multiply(R1);
                break;
              case 12:
                position = new Vector3Dd(1, -1, 0);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(90), new Vector3Dd(1, 0, 0));
                R2 = R2.axisRotation(Math.toRadians(45), new Vector3Dd(0, 0, 1));
                R = R2.multiply(R1);
                break;
              case 13:
                position = new Vector3Dd(1, 1, 0);
                position = position.normalized();
                position = position.multiply(1.5);
                scale = new Vector3Dd(0.5, 0.5, 0.5);
                R1 = R1.axisRotation(Math.toRadians(90), new Vector3Dd(1, 0, 0));
                R2 = R2.axisRotation(Math.toRadians(135), new Vector3Dd(0, 0, 1));
                R = R2.multiply(R1);
                break;
            }

            //-----------------------------------------------------------------
            texture = createProjectedView(gl, referenceBodies, i);
            if ( texture == null ) {
                return null;
            }

            //-----------------------------------------------------------------
            n = new Vector3Dd(0, 0, 1);
            vertexArray = new Vertex[4];
            vertexArray[0] = new Vertex(new Vector3Dd(-1, -1, 0), n, 0.0, 0.0);
            vertexArray[1] = new Vertex(new Vector3Dd(1, -1, 0), n, 1.0, 0.0);
            vertexArray[2] = new Vertex(new Vector3Dd(1, 1, 0), n, 1.0, 1.0);
            vertexArray[3] = new Vertex(new Vector3Dd(-1, 1, 0), n, 0.0, 1.0);
            triangleArray = new Triangle[2];
            triangleArray[0] = new Triangle(0, 1, 2);
            triangleArray[1] = new Triangle(2, 3, 0);
            textureArray = new Image[1];
            textureArray[0] = texture;
            textureRanges = new int[1][2];
            textureRanges[0][0] = 2;
            textureRanges[0][1] = 1;
            materialArray = new SimpleMaterial[1];
            materialArray[0] = theScene.defaultMaterial();
            materialArray[0] = materialArray[0].withDoubleSided(true);
            materialArray[0] = materialArray[0].withAmbient(new ColorRgb(1, 1, 1));
            materialArray[0] = materialArray[0].withDiffuse(new ColorRgb(1, 1, 1));
            materialArray[0] = materialArray[0].withSpecular(new ColorRgb(1, 1, 1));
            materialRanges = new int[1][2];
            materialRanges[0][0] = 2;
            materialRanges[0][1] = 0;

            mesh = new TriangleMesh();
            mesh.setVertexes(vertexArray, true, false, false, true);
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
            boxBody.setMaterial(boxBody.getMaterial().withDoubleSided(true));
            boxBody.setMaterial(boxBody.getMaterial().withAmbient(new ColorRgb(1, 1, 1)));
            boxBody.setMaterial(boxBody.getMaterial().withDiffuse(new ColorRgb(1, 1, 1)));
            boxBody.setMaterial(boxBody.getMaterial().withSpecular(new ColorRgb(1, 1, 1)));
            boxBody.setName("Proyected view box");
            boxBody.setTexture(texture);
            //-----------------------------------------------------------------
            group.getBodies().add(boxBody);
        }
        return group;
    }

    private void debugProjectedViewsIfNeeded(GL2 glAppContext)
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
            parent.getAwtModel().getStatusMessage().setText("ERROR: An object must be selected for projected views debugging to be created");
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
                parent.getAwtModel().getStatusMessage().setText("ERROR: cannot create Pbuffer, you need recent 3D hardware acceleration for this function");
            }
        }
    }

    private void copyColorBufferIfNeeded(GL2 gl, boolean selectedView)
    {
        if ( wantToGetColor && selectedView ) {
            captureColorBuffer(gl, true);
            parent.getAwtModel().getStatusMessage().setText("ZBuffer Color Image obtained!");
            wantToGetColor = false;
        }
    }

    private void captureColorBuffer(GL2 gl, boolean reportToImageWindow)
    {
        model.setZbufferImage(Jogl2RGBImageUncompressedRenderer.getImageJOGL(gl));
        if ( !reportToImageWindow ) {
            return;
        }
        if ( parent.getAwtModel().getImageControlWindow() == null ) {
            parent.getAwtModel().setImageControlWindow(new SwingImageControlWindow(
                model.getZbufferImage(),
                parent.getAwtModel().getGui(),
                parent.getAwtModel().getExecutorPanel()));
        }
        else {
            parent.getAwtModel().getImageControlWindow().setImage(model.getZbufferImage());
        }
        parent.getAwtModel().getImageControlWindow().redrawImage();
    }

    public void exportViewportPng(File file)
    {
        pendingViewportExportFile = file;
        pendingViewportExportJpg = false;
        canvas.display();
    }

    public void exportViewportJpg(File file)
    {
        pendingViewportExportFile = file;
        pendingViewportExportJpg = true;
        canvas.display();
    }

    public void exportWorkspaceJpg(File file)
    {
        pendingWorkspaceExportFile = file;
        canvas.display();
    }

    private RGBImageUncompressed cropImage(RGBImageUncompressed source,
                                           int startX, int startY,
                                           int width, int height)
    {
        int sourceWidth = source.getXSize();
        int sourceHeight = source.getYSize();
        int x0 = Math.max(0, startX);
        int y0 = Math.max(0, startY);
        int x1 = Math.min(sourceWidth, startX + width);
        int y1 = Math.min(sourceHeight, startY + height);
        RGBImageUncompressed result = new RGBImageUncompressed();
        result.init(Math.max(0, x1 - x0), Math.max(0, y1 - y0));
        for ( int y = y0; y < y1; y++ ) {
            for ( int x = x0; x < x1; x++ ) {
                RGBPixel pixel = source.getPixel(x, y);
                result.putPixel(x - x0, y - y0, pixel);
            }
        }
        return result;
    }

    private void exportPendingFrame(GL2 gl)
    {
        if ( pendingViewportExportFile == null && pendingWorkspaceExportFile == null ) {
            return;
        }

        int width = viewportSet.getSizeXInPixels();
        int height = viewportSet.getSizeYInPixels();
        gl.glViewport(0, 0, width, height);
        RGBImageUncompressed workspace = Jogl2RGBImageUncompressedRenderer.getImageJOGL(gl);

        if ( pendingViewportExportFile != null ) {
            Jogl4ViewportWindow selected = viewportSetRenderer.getSelectedWindow();
            RGBImageUncompressed viewport = cropImage(workspace,
                selected.getViewportStartX(), selected.getViewportStartY(),
                selected.getViewportSizeX(), selected.getViewportSizeY());
            if ( pendingViewportExportJpg ) {
                ImagePersistence.exportJPG(pendingViewportExportFile, viewport);
            }
            else {
                ImagePersistence.exportPNG(pendingViewportExportFile, viewport);
            }
            pendingViewportExportFile = null;
        }

        if ( pendingWorkspaceExportFile != null ) {
            ImagePersistence.exportJPG(pendingWorkspaceExportFile, workspace);
            pendingWorkspaceExportFile = null;
        }
    }

    private void copyZBufferIfNeeded(GL2 gl)
    {
        if ( wantToGetDepth ) {
            if ( wantToGetContourns ) {
                IndexedColorImageUncompressed zbuffer;
                NormalMap nm;
                zbuffer = Jogl2ZBufferRenderer.importJOGLZBuffer(gl).exportIndexedColorImage();
                nm = new NormalMap();
                nm.importBumpMap(zbuffer, new Vector3Dd(1, 1, 0.1));
                model.setZbufferImage(nm.exportToRgbImageGradient());
            }
            else {
                model.setZbufferImage(
                    Jogl2ZBufferRenderer.importJOGLZBuffer(gl).exportRGBImage(
                        model.getPalette()));
            }

            if ( parent.getAwtModel().getImageControlWindow() == null ) {
                parent.getAwtModel().setImageControlWindow(new SwingImageControlWindow(
                    model.getZbufferImage(),
                    parent.getAwtModel().getGui(),
                    parent.getAwtModel().getExecutorPanel()));
            }
            else {
                parent.getAwtModel().getImageControlWindow().setImage(model.getZbufferImage());
            }
            parent.getAwtModel().getImageControlWindow().redrawImage();
            parent.getAwtModel().getStatusMessage().setText("ZBuffer depth map obtained!");
            wantToGetDepth = false;
            wantToGetContourns = false;
        }
    }

    public void toggleGrid()
    {
        Viewport selected = viewportSet.getSelectedViewport();

        if ( selected != null ) {
            selected.toggleGrid();
        }
    }

    private void drawView(GL2 gl, Jogl4ViewportWindow view)
    {
        if ( !view.isActive() ) {
            return;
        }

        if ( view.getRenderMode() == Jogl4ViewportWindow.RENDER_MODE_ZBUFFER ) {
            Jogl4SceneRenderer.draw(gl, theScene, parent);
        }
        else {
            theScene.activateSelectedBackground();
            Jogl2BackgroundRenderer.draw(gl,
            theScene.scene.getBackgrounds().get(theScene.scene.getActiveBackgroundIndex()));
            model.setRaytracedImageWidth(view.getViewportSizeX());
            model.setRaytracedImageHeight(view.getViewportSizeY());
            parent.doRaytracedImage();
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            Jogl2ImageRenderer.draw(gl, model.getRaytracedImage());
            gl.glPopMatrix();
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(GL2.GL_MODELVIEW);
        }

        //-----------------------------------------------------------------
        drawVisualRayDebug(gl);

        // Nice debugging for Camera.projectPoint operation :)
/*
        gl.glPushAttrib(gl.GL_DEPTH_TEST);
        gl.glDisable(gl.GL_DEPTH_TEST);
        Vector3Dd projected = new Vector3Dd();
        if ( view.getCamera().projectPoint(
             model.getVisualDebugRay().origin, projected) ) {
            view.drawTextureString2D(gl,
                (int)projected.x()-3, (int)projected.y()+10, view.xLabelImage);
        }
        gl.glPopAttrib();
*/

        view.drawGrid(gl);

        //-----------------------------------------------------------------
        // Note that gizmo information will not be reported, as they damage
        // the zbuffer...
        copyZBufferIfNeeded(gl);

        // Must be the last to draw
        drawGizmos(gl);

        copyColorBufferIfNeeded(gl, view.isSelected());

        view.drawReferenceBase(gl);
        if ( translationGizmoDrawn ) {
            view.drawLabelsForTranslateGizmo(gl, translationGizmo);
        }
    }

    /** Called by drawable to initiate drawing
     * @param drawable */
    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        debugProjectedViewsIfNeeded(gl);
        syncViewportStateFromCanvas();
        syncViewportStateFromSurface(drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight());

        //-----------------------------------------------------------------
        gl.glViewport(0, 0, viewportSet.getSizeXInPixels(), viewportSet.getSizeYInPixels());
        gl.glClearColor(0.77f, 0.77f, 0.77f, 1.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        viewportSetRenderer.draw(gl, parent.getAwtModel().isFullScreenGuiMode());

        exportPendingFrame(gl);
    }

    private void drawVisualRayDebugSegment(GL2 gl, Vector3Dd start, Vector3Dd end, boolean follow, double w, double tip,
        SimpleMaterial segmentMaterial)
    {
        double l;
        Vector3Dd diff = end.subtract(start);
        l = diff.length();

        gl.glEnable(GL2.GL_LIGHTING);
        Jogl2SimpleMaterialRenderer.activate(gl, segmentMaterial);

        //-----------------------------------------------------------------
        Geometry a;

        if ( l > tip ) {
            a = new Arrow(l - tip, tip, w/2, w);
        }
        else {
            a = new Cone(w/2, w/2, l);
        }
        Matrix4x4d R = new Matrix4x4d();
        double yaw, pitch;
        yaw = diff.obtainSphericalThetaAngle();
        pitch = diff.obtainSphericalPhiAngle();
        R = R.eulerAnglesRotation(Math.toRadians(180)+yaw, pitch, 0);

        gl.glPushMatrix();
        gl.glTranslated(start.x(), start.y(), start.z());
        Jogl2MatrixRenderer.activate(gl, R);
        Jogl2GeometryRenderer.draw(gl, a, theScene.camera, qualitySelectionVisualDebug);
        gl.glPopMatrix();

        //-----------------------------------------------------------------
        if ( follow ) {
            Sphere s = new Sphere(0.025);
            Vector3Dd p;
            double offset = 0.1;
            int i;
            diff = diff.normalized();
            for ( i = 0; i < 3; i++, offset += 0.1 ) {
                p = end.add(diff.multiply(offset));
                gl.glPushMatrix();
                gl.glTranslated(p.x(), p.y(), p.z());
                Jogl2GeometryRenderer.draw(gl, s, theScene.camera, qualitySelectionVisualDebug);
                gl.glPopMatrix();
            }
        }
    }

    private void drawVisualRayDebug(GL2 gl, Ray ray, int level)
    {
        if ( level < 0 ) {
            return;
        }

        gl.glPushMatrix();
        gl.glLoadIdentity();

        //-----------------------------------------------------------------
        Vector3Dd p;
        Vector3Dd d = new Vector3Dd(ray.getDirection());
        d = d.normalized();
        RayHit info;

        info = new RayHit();

        //-----------------------------------------------------------------
        SimpleMaterial rayOriginMaterial = visualDebugMaterial.withDiffuse(new ColorRgb(0.9, 0.5, 0.0));
        Jogl2SimpleMaterialRenderer.activate(gl, rayOriginMaterial);
        Sphere s = new Sphere(0.05);
        gl.glPushMatrix();
        gl.glTranslated(ray.getOrigin().x(), ray.getOrigin().y(), ray.getOrigin().z());
        Jogl2GeometryRenderer.draw(gl, s, theScene.camera, qualitySelectionVisualDebug);
        gl.glPopMatrix();

        //-----------------------------------------------------------------
        if ( theScene.doIntersectionFirstHit(ray, info) ) {
            d = d.multiply(ray.getT());
            p = ray.getOrigin().add(d);

            drawVisualRayDebugSegment(gl, ray.getOrigin(), p, false, 0.07, 0.4, rayOriginMaterial);
            if ( level >= 1 ) {
                // Draw normal
                SimpleMaterial normalMaterial = visualDebugMaterial.withDiffuse(new ColorRgb(0.9, 0.9, 0.5));
                drawVisualRayDebugSegment(gl, p, p.add(info.n.multiply(0.5)), false, 0.05, 0.2, normalMaterial);
            }
            // Reflection ray
            Vector3Dd dd = ray.getDirection().multiply(-1);
            dd = dd.normalized();
            Vector3Dd h = info.n.multiply(dd.dotProduct(info.n)).subtract(dd);
            Ray subray = new Ray(p, dd.add(h.multiply(2)));
            subray = subray.withOrigin(
                subray.getOrigin().add(subray.getDirection().multiply(VSDK.EPSILON*10.0)));
            drawVisualRayDebug(gl, subray, level-1);
        }
        else {
            d = d.multiply(1.4);
            p = ray.getOrigin().add(d);
            drawVisualRayDebugSegment(gl, ray.getOrigin(), p, true, 0.07, 0.4, rayOriginMaterial);
        }
        gl.glPopMatrix();
    }

    private void drawVisualRayDebug(GL2 gl)
    {
        //-----------------------------------------------------------------
        if ( !model.isWithVisualDebugRay() ) {
            return;
        }

        //-----------------------------------------------------------------
        gl.glEnable(GL2.GL_LIGHTING);
        drawVisualRayDebug(gl, model.getVisualDebugRay(), model.getVisualDebugRayLevels());
        //-----------------------------------------------------------------
    }


    /** Not used method, but needed to instanciate GLEventListener
     * @param drawable */
    @Override
    public void init(GLAutoDrawable drawable)
    {
        
    }

    /** Not used method, but needed to instanciate GLEventListener
     * @param drawable */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        
    }

    /** Not used method, but needed to instanciate GLEventListener
     * @param drawable
     * @param a
     * @param b */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b)
    {
        
    }
    
    /** Called to indicate the drawing surface has been moved and/or resized
     * @param drawable
     * @param x
     * @param y
     * @param width
     * @param height */
    @Override
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height)
    {
        awtViewportWidth = canvas.getWidth();
        awtViewportHeight = canvas.getHeight();
        syncViewportStateFromSurface(width, height);
    }   

    @Override
    public void mouseEntered(java.awt.event.MouseEvent e)
    {
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

    @Override
    public void mouseExited(java.awt.event.MouseEvent e) 
    {
        //System.out.println("Mouse exited");
    }

    /**
    @return the window of the viewport under the pointer, or null if none
    */
    private Jogl4ViewportWindow getViewFromPointerPosition(java.awt.event.MouseEvent e)
    {
        syncViewportStateFromCanvas();
        Viewport viewport = viewportSetTechniques.findViewportAt(toSurfaceEvent(e));

        if ( viewport == null ) {
            return null;
        }
        return viewportSetRenderer.getWindow(viewport);
    }

    private void syncViewportStateFromCanvas()
    {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if ( width <= 0 || height <= 0 ) {
            return;
        }

        awtViewportWidth = width;
        awtViewportHeight = height;
    }

    /**
    Makes the camera controllers work over the camera and configuration of
    the given view. Viewport selection is processed by the viewport set
    interaction techniques.
    */
    private void activateInteractionView(Jogl4ViewportWindow view)
    {
        if ( view == null ) {
            return;
        }

        interactionTechniques.setCamera(view.getCamera());
        interactionTechniques.setRendererConfiguration(view.getRendererConfiguration());
        qualitySelection = view.getRendererConfiguration();
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent e)
    {
        Jogl4ViewportWindow mouseView = getViewFromPointerPosition(e);

        if ( mouseView == null ) {
            return;
        }
        viewportSetTechniques.processMousePressedEvent(toSurfaceEvent(e));
        activateInteractionView(mouseView);

        //-----------------------------------------------------------------
        // WARNING / TODO
        // There should be a cameraController.getFutureAction(e) that calculates
        // the proper icon for display ... here an Aquynza operation is
        // assumed and hard-coded
        int m = e.getModifiersEx();

        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             (m & MouseEvent.BUTTON1_DOWN_MASK) != 0 ) {
            canvas.setCursor(camrotateCursor);
        }
        else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                  (m & MouseEvent.BUTTON2_DOWN_MASK) != 0 ) {
            canvas.setCursor(camtranslateCursor);
        }
        else if ( interactionMode == CAMERA_INTERACTION_MODE &&
                  (m & MouseEvent.BUTTON3_DOWN_MASK) != 0 ) {
            canvas.setCursor(camadvanceCursor);
        }
        else {
            canvas.setCursor(selectCursor);
        }

        vsdk.toolkit.gui.MouseEvent vitralMouseEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraMousePressedEvent(vitralMouseEvent) ) {
            
        }
        else if ( interactionMode == SELECT_INTERACTION_MODE ||
                  interactionMode == TRANSLATE_INTERACTION_MODE || 
                  interactionMode == ROTATE_INTERACTION_MODE || 
                  interactionMode == SCALE_INTERACTION_MODE 
                  ) {
            boolean composite = false;
            if ( ((e.getModifiersEx()) & MouseEvent.CTRL_DOWN_MASK) != 0x0 ) {
                composite = true;
            }
            int oldThingSelected = theScene.selectedThings.firstSelected();
            vsdk.toolkit.gui.MouseEvent viewportEvent = toViewportEvent(e, mouseView);

            theScene.activeCamera = mouseView.getCamera();
            model.setVisualDebugRay(theScene.selectObjectWithMouse(
                viewportEvent.getX(), viewportEvent.getY(), composite, model.getVisualDebugRay()));

            int firstThingSelected = theScene.selectedThings.firstSelected();

            if ( oldThingSelected >= 0 && firstThingSelected < 0 &&
                 interactionMode == TRANSLATE_INTERACTION_MODE &&
                 translationGizmo.isActive() ) {
                theScene.selectedThings.select(oldThingSelected);
                firstThingSelected = theScene.selectedThings.firstSelected();
            }

            if ( firstThingSelected >= 0 ) {
                //------------------------------------------------------------
                Vector3Dd position;
                SimpleBody gi;
                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                Matrix4x4d composed;

                position = gi.getPosition();
                //composed = new Matrix4x4d(gi.getRotation());
                composed = new Matrix4x4d();
                composed = composed.withVal(0, 3, position.x());
                composed = composed.withVal(1, 3, position.y());
                composed = composed.withVal(2, 3, position.z());

                translationGizmo.setCamera(mouseView.getCamera());
                translationGizmo.setTransformationMatrix(composed);
                vitralMouseEvent = viewportEvent;
                interactionTechniques.processTranslationMousePressedEvent(vitralMouseEvent);
                //------------------------------------------------------------
            }

            reportObjectSelection();
        }
        canvas.repaint();
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e)
    {
        Jogl4ViewportWindow mouseView = getViewFromPointerPosition(e);

        if ( mouseView != null ) {
            viewportSetTechniques.processMouseReleasedEvent(toSurfaceEvent(e));
        }
        activateInteractionView(mouseView);

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

        vsdk.toolkit.gui.MouseEvent vitralMouseEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraMouseReleasedEvent(vitralMouseEvent) ) {
            canvas.repaint();
        }
        else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                  firstThingSelected >= 0 ) {
            Vector3Dd position;
            SimpleBody gi;

            gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

            Matrix4x4d composed;

            position = gi.getPosition();
            //composed = new Matrix4x4d(gi.getRotation());
            composed = new Matrix4x4d();
            composed = composed.withVal(0, 3, position.x());
            composed = composed.withVal(1, 3, position.y());
            composed = composed.withVal(2, 3, position.z());

            if ( mouseView == null ) {
                return;
            }
            translationGizmo.setCamera(mouseView.getCamera());
            translationGizmo.setTransformationMatrix(composed);
            vitralMouseEvent = toViewportEvent(e, mouseView);
            if ( interactionTechniques.processTranslationMouseReleasedEvent(vitralMouseEvent) ) {
                composed = translationGizmo.getTransformationMatrix();
                position = position.withX(composed.get(0, 3));
                position = position.withY(composed.get(1, 3));
                position = position.withZ(composed.get(2, 3));
                composed = composed.withVal(0, 3, 0);
                composed = composed.withVal(1, 3, 0);
                composed = composed.withVal(2, 3, 0);
                applyTranslationToSelectedObjects(position);
                canvas.repaint();
            }
        }

    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e)
    {
        Jogl4ViewportWindow mouseView = getViewFromPointerPosition(e);

        if ( mouseView != null ) {
            viewportSetTechniques.processMouseClickedEvent(toSurfaceEvent(e));
        }
        activateInteractionView(mouseView);

        int firstThingSelected = theScene.selectedThings.firstSelected();

        vsdk.toolkit.gui.MouseEvent vitralMouseEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraMouseClickedEvent(vitralMouseEvent) ) {
            canvas.repaint();
        }
        else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                  firstThingSelected >= 0 ) {
            Vector3Dd position;
            SimpleBody gi;

            gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

            Matrix4x4d composed;

            position = gi.getPosition();
            //composed = new Matrix4x4d(gi.getRotation());
            composed = new Matrix4x4d();
            composed = composed.withVal(0, 3, position.x());
            composed = composed.withVal(1, 3, position.y());
            composed = composed.withVal(2, 3, position.z());

            if ( mouseView == null ) {
                return;
            }
            translationGizmo.setCamera(mouseView.getCamera());
            translationGizmo.setTransformationMatrix(composed);
            vitralMouseEvent = toViewportEvent(e, mouseView);
            if ( interactionTechniques.processTranslationMouseClickedEvent(vitralMouseEvent) ) {
                composed = translationGizmo.getTransformationMatrix();
                position = position.withX(composed.get(0, 3));
                position = position.withY(composed.get(1, 3));
                position = position.withZ(composed.get(2, 3));
                composed = composed.withVal(0, 3, 0);
                composed = composed.withVal(1, 3, 0);
                composed = composed.withVal(2, 3, 0);
                applyTranslationToSelectedObjects(position);
                canvas.repaint();
            }
        }
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent e)
    {
        //-----------------------------------------------------------------
        Jogl4ViewportWindow mouseView = getViewFromPointerPosition(e);

        if ( mouseView != null ) {
            interactionTechniques.setCamera(mouseView.getCamera());
        }

        //-----------------------------------------------------------------
        int firstThingSelected = theScene.selectedThings.firstSelected();

        vsdk.toolkit.gui.MouseEvent vitralMouseEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraMouseMovedEvent(vitralMouseEvent) ) {
            canvas.repaint();
        }
        else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                  firstThingSelected >= 0 ) {
            Vector3Dd position;
            SimpleBody gi;

            gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

            Matrix4x4d composed;

            position = gi.getPosition();
            //composed = new Matrix4x4d(gi.getRotation());
            composed = new Matrix4x4d();
            composed = composed.withVal(0, 3, position.x());
            composed = composed.withVal(1, 3, position.y());
            composed = composed.withVal(2, 3, position.z());

            if ( mouseView == null ) {
                return;
            }
            translationGizmo.setCamera(mouseView.getCamera());
            translationGizmo.setTransformationMatrix(composed);
            vitralMouseEvent = toViewportEvent(e, mouseView);
            if ( interactionTechniques.processTranslationMouseMovedEvent(vitralMouseEvent) ) {
                composed = translationGizmo.getTransformationMatrix();
                position = position.withX(composed.get(0, 3));
                position = position.withY(composed.get(1, 3));
                position = position.withZ(composed.get(2, 3));
                composed = composed.withVal(0, 3, 0);
                composed = composed.withVal(1, 3, 0);
                composed = composed.withVal(2, 3, 0);
                applyTranslationToSelectedObjects(position);
                canvas.repaint();
            }
        }
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e)
    {
        Jogl4ViewportWindow mouseView = getViewFromPointerPosition(e);

        if ( mouseView != null ) {
            viewportSetTechniques.processMouseDraggedEvent(toSurfaceEvent(e));
        }
        activateInteractionView(mouseView);

        int firstThingSelected = theScene.selectedThings.firstSelected();

        vsdk.toolkit.gui.MouseEvent vitralMouseEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraMouseDraggedEvent(vitralMouseEvent) ) {
            canvas.repaint();
        }
        else if ( interactionMode == TRANSLATE_INTERACTION_MODE &&
                  firstThingSelected >= 0 ) {
            Vector3Dd position;
            SimpleBody gi;

            gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

            Matrix4x4d composed;

            position = gi.getPosition();
            //composed = new Matrix4x4d(gi.getRotation());
            composed = new Matrix4x4d();
            composed = composed.withVal(0, 3, position.x());
            composed = composed.withVal(1, 3, position.y());
            composed = composed.withVal(2, 3, position.z());

            if ( mouseView == null ) {
                return;
            }
            translationGizmo.setCamera(mouseView.getCamera());
            translationGizmo.setTransformationMatrix(composed);
            vitralMouseEvent = toViewportEvent(e, mouseView);
            if ( interactionTechniques.processTranslationMouseDraggedEvent(vitralMouseEvent) ) {
                composed = translationGizmo.getTransformationMatrix();
                position = position.withX(composed.get(0, 3));
                position = position.withY(composed.get(1, 3));
                position = position.withZ(composed.get(2, 3));
                composed = composed.withVal(0, 3, 0);
                composed = composed.withVal(1, 3, 0);
                composed = composed.withVal(2, 3, 0);
                applyTranslationToSelectedObjects(position);
                canvas.repaint();
            }
        }
    }

    /**
    WARNING: It is not working... check pending
     * @param e    */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        System.out.println(".");
        vsdk.toolkit.gui.MouseEvent vitralMouseEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraMouseWheelEvent(vitralMouseEvent) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        char unicode_id;
        int keycode;
        boolean skipKey = false;

        unicode_id = e.getKeyChar();
        keycode = e.getKeyCode();
        vsdk.toolkit.gui.KeyEvent vitralKeyEvent = AwtSystem.awt2vsdkEvent(e);

        int firstThingSelected = theScene.selectedThings.firstSelected();

        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraKeyPressedEvent(vitralKeyEvent) ) {
            
        }
        else if ( interactionMode == SELECT_INTERACTION_MODE ) {
            if ( unicode_id == KeyEvent.CHAR_UNDEFINED ) {
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
            }
        }
        else if ( interactionMode == TRANSLATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                Matrix4x4d composed;
                Vector3Dd position;
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);

                position = gi.getPosition();
                //composed = new Matrix4x4d(gi.getRotation());
                composed = new Matrix4x4d();
                composed = composed.withVal(0, 3, position.x());
                composed = composed.withVal(1, 3, position.y());
                composed = composed.withVal(2, 3, position.z());

                translationGizmo.setTransformationMatrix(composed);
                if ( interactionTechniques.processTranslationKeyPressedEvent(vitralKeyEvent) ) {
                    composed = translationGizmo.getTransformationMatrix();
                    position = position.withX(composed.get(0, 3));
                    position = position.withY(composed.get(1, 3));
                    position = position.withZ(composed.get(2, 3));
                    composed = composed.withVal(0, 3, 0);
                    composed = composed.withVal(1, 3, 0);
                    composed = composed.withVal(2, 3, 0);
                    applyTranslationToSelectedObjects(position);
                }
            }
        }
        else if ( interactionMode == ROTATE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                Matrix4x4d R = gi.getRotation();

                rotateGizmo.setTransformationMatrix(R);

                if ( interactionTechniques.processRotateKeyPressedEvent(vitralKeyEvent) ) {
                    R = rotateGizmo.getTransformationMatrix();
                    gi.setRotation(R);
                    Matrix4x4d Ri = new Matrix4x4d(R);
                    Ri = Ri.invert();
                    gi.setRotationInverse(Ri);
                }
            }
        }
        else if ( interactionMode == SCALE_INTERACTION_MODE ) {
            if ( firstThingSelected >= 0 ) {
                SimpleBody gi;

                gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                Vector3Dd s = gi.getScale();
                Matrix4x4d S = new Matrix4x4d();
                S = S.withVal(0, 0, s.x());
                S = S.withVal(1, 1, s.y());
                S = S.withVal(2, 2, s.z());

                scaleGizmo.setTransformationMatrix(S);

                if ( interactionTechniques.processScaleKeyPressedEvent(vitralKeyEvent) ) {
                    S = scaleGizmo.getTransformationMatrix();
                    s = new Vector3Dd(S.get(0, 0), S.get(1, 1), S.get(2, 2));
                    gi.setScale(s);
                }
            }
        }

        // Global commands
        int asp;
        switch ( keycode ) {
          case KeyEvent.VK_ESCAPE:
              parent.closeApplication();
            break;
          case KeyEvent.VK_EQUALS:
            // Alphanumeric =
            asp = translationGizmo.getAparentSizeInPixels();
            asp += 10;
            if ( asp > 300 ) asp = 300;
            translationGizmo.setAparentSizeInPixels(asp);
            break;
          case KeyEvent.VK_MINUS:
            // Alphanumeric -
            asp = translationGizmo.getAparentSizeInPixels();
            asp -= 20;
            if ( asp < 20 ) asp = 20;
            translationGizmo.setAparentSizeInPixels(asp);
            break;
        }

        if ( interactionTechniques.processQualityKeyPressedEvent(vitralKeyEvent) ) {
            System.out.println(qualitySelection);
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
        }

        if ( keycode == KeyEvent.VK_F10 ) {
            parent.getAwtModel().getStatusMessage().setText(
                parent.getAwtModel().getGui().getMessage("IDM_COMPUTING_RAYTRACING"));
            parent.doRaytracedImage();
  
            if ( parent.getAwtModel().getImageControlWindow() == null ) {
                parent.getAwtModel().setImageControlWindow(new SwingImageControlWindow(
                    model.getRaytracedImage(),
                    parent.getAwtModel().getGui(),
                    parent.getAwtModel().getExecutorPanel()));
            }
            else {
                parent.getAwtModel().getImageControlWindow().setImage(model.getRaytracedImage());
            }
            parent.getAwtModel().getImageControlWindow().redrawImage();
        }

        // Viewport set commands (selection, layout, per-viewport display)
        if ( !skipKey ) {
            viewportSetTechniques.processKeyPressedEvent(vitralKeyEvent);
        }

        double theta;
        double phi;

        if ( ((e.getModifiersEx()) & KeyEvent.SHIFT_DOWN_MASK) != 0x0 &&
             ((e.getModifiersEx()) & KeyEvent.CTRL_DOWN_MASK) != 0x0 ) {
            switch ( keycode ) {
              case KeyEvent.VK_F:
                parent.getAwtModel().toggleFullScreenGuiMode();
                parent.destroyGUI();
                parent.createGUI();
                break;
            }
        }

        if ( unicode_id != KeyEvent.CHAR_UNDEFINED && !skipKey ) {
            switch ( unicode_id ) {

                //- Visual debug ray control ---------------------------------
              case '4': // Numpad 4
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRay(model.getVisualDebugRay().withOrigin(
                        model.getVisualDebugRay().getOrigin().withX(model.getVisualDebugRay().getOrigin().x() - 0.1)));
                }
                break;
              case '6': // Numpad 6
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRay(model.getVisualDebugRay().withOrigin(
                        model.getVisualDebugRay().getOrigin().withX(model.getVisualDebugRay().getOrigin().x() + 0.1)));
                }
                break;
              case '8': // Numpad 8
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRay(model.getVisualDebugRay().withOrigin(
                        model.getVisualDebugRay().getOrigin().withY(model.getVisualDebugRay().getOrigin().y() + 0.1)));
                }
                break;
              case '2': // Numpad 2
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRay(model.getVisualDebugRay().withOrigin(
                        model.getVisualDebugRay().getOrigin().withY(model.getVisualDebugRay().getOrigin().y() - 0.1)));
                }
                break;
              case '1': // Numpad 1
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRay(model.getVisualDebugRay().withOrigin(
                        model.getVisualDebugRay().getOrigin().withZ(model.getVisualDebugRay().getOrigin().z() - 0.1)));
                }
                break;
              case '7': // Numpad 7
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRay(model.getVisualDebugRay().withOrigin(
                        model.getVisualDebugRay().getOrigin().withZ(model.getVisualDebugRay().getOrigin().z() + 0.1)));
                }
                break;
              case '9': // Numpad 9
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRayLevels(model.getVisualDebugRayLevels() + 1);
                }
                break;
              case '3': // Numpad 3
                if ( model.isWithVisualDebugRay() ) {
                    model.setVisualDebugRayLevels(model.getVisualDebugRayLevels() - 1);
                    if ( model.getVisualDebugRayLevels() < 0 ) {
                        model.setVisualDebugRayLevels(0);
                    }
                }
                break;
              case '5': // Numpad 5
                model.setWithVisualDebugRay(!model.isWithVisualDebugRay());
                break;
              case '*': // Numpad *
                if ( model.isWithVisualDebugRay() ) {
                    theta =
                        model.getVisualDebugRay().getDirection().obtainSphericalThetaAngle();
                    phi =
                        model.getVisualDebugRay().getDirection().obtainSphericalPhiAngle();
                    theta -= Math.toRadians(5);
                    model.setVisualDebugRay(model.getVisualDebugRay().withDirection(
                        Vector3Dd.fromSpherical(1, theta, phi)));
                }
                break;
              case '/': // Numpad /
                if ( model.isWithVisualDebugRay() ) {
                    theta =
                        model.getVisualDebugRay().getDirection().obtainSphericalThetaAngle();
                    phi =
                        model.getVisualDebugRay().getDirection().obtainSphericalPhiAngle();
                    theta += Math.toRadians(5);
                    model.setVisualDebugRay(model.getVisualDebugRay().withDirection(
                        Vector3Dd.fromSpherical(1, theta, phi)));
                }
                break;
              case '+': // Numpad +
                if ( model.isWithVisualDebugRay() ) {
                    theta =
                        model.getVisualDebugRay().getDirection().obtainSphericalThetaAngle();
                    phi =
                        model.getVisualDebugRay().getDirection().obtainSphericalPhiAngle();
                    phi += Math.toRadians(5);
                    if ( phi > Math.PI ) phi = Math.PI;
                    model.setVisualDebugRay(model.getVisualDebugRay().withDirection(
                        Vector3Dd.fromSpherical(1, theta, phi)));
                }
                break;
              case '-': // Numpad -
                if ( model.isWithVisualDebugRay() ) {
                    theta =
                        model.getVisualDebugRay().getDirection().obtainSphericalThetaAngle();
                    phi =
                        model.getVisualDebugRay().getDirection().obtainSphericalPhiAngle();
                    phi -= Math.toRadians(5);
                    if ( phi < 0 ) phi = 0;
                    model.setVisualDebugRay(model.getVisualDebugRay().withDirection(
                        Vector3Dd.fromSpherical(1, theta, phi)));
                }
                break;
                //------------------------------------------------------------

              case 'T':
                if ( firstThingSelected >= 0 ) {
                    SimpleBody gi;
                    Image texture;
                    gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                    texture = gi.getTexture();
                    if ( texture == null ) {
                        String imageFilename = "../../../../etc/textures/miniearth.png";
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
                break;
              case 'B':
                if ( firstThingSelected >= 0 ) {
                    SimpleBody gi;
                    IndexedColorImageUncompressed source;
                    NormalMap normalMap;
                    //RGBImageUncompressed exported;
                    gi = theScene.scene.getSimpleBodies().get(firstThingSelected);
                    normalMap = gi.getNormalMap();
                    if ( normalMap == null ) {
                        try {
                            normalMap = new NormalMap();
                                //String imageFilename = "../../../../etc/bumpmaps/blinn2.bw";
                            String imageFilename = "../../../../etc/bumpmaps/earth.bw";
                            source = ImagePersistence.importIndexedColor(new File(imageFilename));
                            normalMap.importBumpMap(source, new Vector3Dd(1, 1, 0.2));
                            //exported = normalMap.exportToRgbImage();
                            //ImagePersistence.exportPPM(new File("./outputmap.ppm"), exported);
                        }
                        catch ( Exception ee ) {
                            Logger.reportMessage(this, VSDK.WARNING, "keyPressed", "" + ee);
                        }
                        gi.setNormalMap(normalMap);
                    }
                    else {
                        gi.setNormalMap(null);
                    }
                }
                break;
              case 'h':
                //-------------------------------------------------------------
                if ( parent.getAwtModel().getSelectorDialog() == null ) {
                    parent.getAwtModel().setSelectorDialog(new SwingSelectorDialog());
                }
                parent.getAwtModel().getSelectorDialog().setVisible(true);
                parent.getAwtModel().getSelectorDialog().repaint();
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
                lastInteractionMode = interactionMode;
                interactionMode = CAMERA_INTERACTION_MODE;
                break;

              case 'q':
                statusMessage.setText("Selection mode interaction - click mouse to select objects, LEFT/RIGHT arrow keys to select sequencialy.");
                lastInteractionMode = interactionMode;
                interactionMode = SELECT_INTERACTION_MODE;
                break;

              case 'w':
                // Alt+w (maximize viewport) is a viewport set command
                if ( (vitralKeyEvent.modifierMask & vsdk.toolkit.gui.KeyEvent.MASK_ALT) == 0 ) {
                    statusMessage.setText("Translation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to move it.");
                    lastInteractionMode = interactionMode;
                    interactionMode = TRANSLATE_INTERACTION_MODE;
                }
                break;

              case 'e':
                statusMessage.setText("Rotation mode interaction - click mouse to select objects, X, Y, Z keys and gizmo to rotate it.");
                lastInteractionMode = interactionMode;
                interactionMode = ROTATE_INTERACTION_MODE;
                break;

              case 'r':
                statusMessage.setText("Scale mode interaction - click mouse to select objects, X, Y, Z/ARROWS keys and gizmo to scale it.");
                lastInteractionMode = interactionMode;
                interactionMode = SCALE_INTERACTION_MODE;
                break;
            }
        }

        if ( interactionMode == CAMERA_INTERACTION_MODE ) {
            canvas.setCursor(camrotateCursor);
        }
        else {
            canvas.setCursor(selectCursor);
        }
        canvas.repaint();
    }

    private void applyTransformToSelectedObjects(Vector3Dd position,
                                                 Matrix4x4d rotation)
    {
        SimpleBody gi;
        int firstThingSelected = theScene.selectedThings.firstSelected();
        int i;

        for ( i = 0; i < theScene.selectedThings.size(); i++ ) {
            if ( !theScene.selectedThings.isSelected(i) ) continue;
            gi = theScene.scene.getSimpleBodies().get(i);

            gi.setPosition(position);
            gi.setRotation(rotation);
            rotation = new Matrix4x4d(rotation);
            rotation = rotation.invert();
            gi.setRotationInverse(rotation);
        }
    }

    private void applyTranslationToSelectedObjects(Vector3Dd position)
    {
        SimpleBody gi;
        int firstThingSelected = theScene.selectedThings.firstSelected();
        int i;

        for ( i = 0; i < theScene.selectedThings.size(); i++ ) {
            if ( !theScene.selectedThings.isSelected(i) ) continue;
            gi = theScene.scene.getSimpleBodies().get(i);

            gi.setPosition(position);
        }
    }

    public void newView()
    {
        viewportSet.addViewport(new Viewport());
    }

    public void delView()
    {
        if ( viewportSet.getViewportCount() > 1 ) {
            viewportSet.removeViewport(viewportSet.getViewportCount() - 1);
        }
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

        //-----------------------------------------------------------------
        reportTargetToModifyPanel();
    }

    public void reportTargetToModifyPanel()
    {
        int firstThingSelected = theScene.selectedThings.firstSelected();
        if ( parent.getAwtModel().isModifyPanelSelected() && firstThingSelected >= 0 ) {
            parent.getAwtModel().getModifyPanel().notifyTargetBeginEdit(
                theScene.scene.getSimpleBodies().get(firstThingSelected)
            );
        }
        else {
            parent.getAwtModel().getModifyPanel().notifyTargetEndEdit();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) 
    {
        vsdk.toolkit.gui.KeyEvent vitralKeyEvent = AwtSystem.awt2vsdkEvent(e);
        if ( interactionMode == CAMERA_INTERACTION_MODE && 
             interactionTechniques.processCameraKeyReleasedEvent(vitralKeyEvent) ) {
            canvas.repaint();
        }
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
     * @param e
    */
    @Override
    public void keyTyped(KeyEvent e)
    {
        
    }
}
