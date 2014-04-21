//===========================================================================
package vitral.application;

// Java basic classes
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

// Android classes: misc
import android.content.Context;
import android.os.SystemClock;

// Android classes: OpenGL ES 2.0
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.AndroidSystem;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.io.geometry.EnvironmentPersistence;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.render.androidgles20.AndroidGLES20GeometryRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20MaterialRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20ImageRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SphereRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20TriangleMeshGroupRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20TriangleMeshRenderer;

/**
The Drawing Area is the main Vitral application element responsible for managing
the view on the model-view-controller design pattern. Each rendering technology
used to implement a drawing area must define a class, in this case, this class
is used on Android devices, using OpenGL ES 2.0.

This class is supposed to receive the main OpenGL ES 2.0 drawing events such as
draw and resize-reshape, but should act only as a facade structural design 
pattern, and delegate most of the application functionality to other application 
classes, using the chain of responsibility behavioral design pattern.
*/
public class AndroidGLES20DrawingArea extends AndroidGLES20Renderer 
implements GLSurfaceView.Renderer, View.OnTouchListener {

    // Android application elements
    private final Context androidApplicationContext;

    // Vitral scene
    private Scene scene;
    private RendererConfiguration quality;
    private Material material;
    private Box box;
    private Cone cone;
    private SimpleScene preloadedCow;
    private SimpleScene preloadedMug;

    private RGBImage texture;
    private RGBImage raytracingImage;
    private RGBImage testImage;
    private IndexedColorImage bumpmap;
    private NormalMap normalMap;
    private boolean highResSphere = false;
    private boolean withReferenceSquare = false;
    private boolean doRaytrace = false;
    private boolean withHudReport = true;
    private double firstLightRadius = 2.0;
    private int numberOfAffectedObjectsOnScene = 1;

    // Animation control
    private int frameCount;
    private boolean withObjectRotation = false;
    private boolean withLightRotation = false;
    private boolean firstTimer = true;
    private boolean displayListsCompiled = false;

    // Hud & statistics
    private HashMap<String, TimeReport>timers;
    private HashMap<String, RGBAImage>characterSprites;

    // Interaction
    private int interaction;
    private int mouseMovementsFromLastDown;
    private CameraControllerAquynza cameraController;
    
    public AndroidGLES20DrawingArea(Context context) {
        androidApplicationContext = context;

        createModel();
    }

    public RendererConfiguration getRendererConfiguration()
    {
        return quality;
    }

    public Camera getCamera()
    {
        return getScene().camera;
    } 

    public void toggleObjectRotation()
    {
        withObjectRotation = !withObjectRotation;
    }

    public void toggleLightRotation()
    {
        withLightRotation = !withLightRotation;
    }

    public void toggleReferenceSquare()
    {
        withReferenceSquare = !withReferenceSquare;
    }

    /**
    Add a thing in the current Vitral scene with given geometry and scale factor
    with identity rotation and position at the origin.
    @param g
    @param scale
    */
    public void addThing(Geometry g, Vector3D scale)
    {
        SimpleBody b;
        
        b = new SimpleBody();
        b.setPosition(new Vector3D(0, 0, 0));
        b.setRotation(new Matrix4x4());
        b.setMaterial(material);
        b.setScale(scale);
        b.setGeometry(g);
        b.setTexture(texture);
        
        scene.scene.getSimpleBodies().add(0, b);
    }
    
    public void selectObject(int o)
    {
        frameCount = 0;
        
        int i;

        for ( i = 0; 
              i < numberOfAffectedObjectsOnScene &&
              scene.scene.getSimpleBodies().size() > 0;
              i++ ) {
            scene.scene.getSimpleBodies().remove(0);
        }
        
        box = null;
        cone = null;
        Sphere sphere;
        numberOfAffectedObjectsOnScene = 1;
        Matrix4x4 R;

        switch ( o ) {
          case 1: default:
            highResSphere = false;
            sphere = new Sphere(1.0);
            addThing(sphere, new Vector3D(1, 1, 1));
            break;
          case 2:
            highResSphere = true;
            sphere = new Sphere(1.0);
            addThing(sphere, new Vector3D(1, 1, 1));
            break;
          case 3:
            box = new Box(1.0, 1.0, 1.0);
            break;
          case 4:
            cone = new Cone(1.0, 1.0, 2.0);
            break;
          case 5:
            if ( preloadedMug == null ) {
                preloadedMug = 
                        loadExternalSceneFile("/storage/extSdCard/mug.ply");
            }
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(90), 1, 0, 0);
            activateSubScene(preloadedMug, R, new Vector3D(20, 20, 20));
            break;
          case 6:
            if ( preloadedCow == null ) {
                preloadedCow = 
                        loadExternalSceneFile("/storage/extSdCard/cow.obj");
            }
            R = new Matrix4x4();
            activateSubScene(preloadedCow, R, new Vector3D(0.3, 0.3, 0.3));
            break;
        }

        resetTimers();
    }

    private void activateSubScene(SimpleScene source, Matrix4x4 R, Vector3D scale) {
        int i;
        for ( i = 0; i < source.getSimpleBodies().size();
                i++ ) {
            SimpleBody b = source.getSimpleBodies().get(i);
            b.setScale(scale);
            //Matrix4x4 orig = b.getRotation();
            //Matrix4x4 modified = orig.multiply(R);
            b.setRotation(R);
            scene.scene.getSimpleBodies().add(b);
        }
        numberOfAffectedObjectsOnScene =
                source.getSimpleBodies().size();
    }

    private SimpleScene loadExternalSceneFile(String filename)
    {
        File meshFile;

        meshFile = new File(filename);
        SimpleScene localScene;
        localScene = new SimpleScene();
        
        try {
            EnvironmentPersistence.importEnvironment(meshFile, localScene);
          }
          catch ( Exception e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, 
                "loadExternalSceneFile", "Error loading file " + filename, e);
        }
        return localScene;
    }

    public void prepareLights(int n)
    {
        Light l;
        double p[] = {
            0, -firstLightRadius, 0, 0.8, 0.8, 0.8,
            1, -1, 1, 0, 1, 0,
            -0.6, -2, 2, 0, 0, 1};

        ArrayList<Light> list = getScene().scene.getLights();
        while ( !list.isEmpty() ) {
            list.remove(0);
        }

        if ( n > 3 ) {
            n = 3;
        }

        int i;
        for ( i = 0; i < n; i++ ) {
            l = new Light(Light.POINT, 
                new Vector3D(p[6*(i%3)+0], p[6*(i%3)+1], p[6*(i%3)+2]), 
                new ColorRgb(p[6*(i%3)+3], p[6*(i%3)+4], p[6*(i%3)+5]));
            list.add(l);
        }
    }

    private HashMap<String, TimeReport> createTimers()
    {
        timers = new HashMap<String, TimeReport>();
        //timers.put("TOTAL_FRAME", new TimeReport("TOTAL_FRAME"));
        timers.put("03_HUD", new TimeReport("03_HUD"));
        timers.put("02_GEOMETRY", new TimeReport("02_GEOMETRY"));
        timers.put("01_STARTUP", new TimeReport("01_STARTUP"));
        return timers;
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        timers = createTimers();
        timers.get("01_STARTUP").start();

        //-----------------------------------------------------------------
        scene = new Scene();
        raytracingImage = null;
        quality = new RendererConfiguration();
        quality.setPoints(false);
        quality.setWires(false);
        quality.setWireColor(new ColorRgb(1.0, 1.0, 0.0));
        quality.setSurfaces(true);
        quality.setTexture(false);
        quality.setBumpMap(false);
        quality.setUseVertexColors(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);

        material = new Material();
        material.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        material.setDiffuse(new ColorRgb(1.0, 1.0, 1.0));
        material.setSpecular(new ColorRgb(1.0, 1.0, 1.0));
        material.setPhongExponent(20.0);

        prepareLights(1);

        selectObject(5);
        createBitmapFontSprites();

        bumpmap = null;
        normalMap = null;

        preloadedCow = null;
        preloadedMug = null;
        
        //-----------------------------------------------------------------
        //testImage = new RGBImage();
        //testImage.init(128, 128);
        //testImage.createTestPattern();

        //-----------------------------------------------------------------
        setInteraction(1);
        mouseMovementsFromLastDown = 0;
        cameraController = new CameraControllerAquynza(getCamera());

        //-----------------------------------------------------------------
        frameCount = 0;
        timers.get("01_STARTUP").stop();
    }

    private void createBitmapFontSprites() {
        //-----------------------------------------------------------------
        characterSprites = new HashMap<String, RGBAImage>();
        char c;
        String s;
        RGBAImage img;
        int fontSize = 16;

        for ( c = 'a'; c <= 'z'; c++ ) {
            s = "" + c;
            img = AndroidSystem.calculateLabelImage(
                    s, new ColorRgb(1.0, 1.0, 1.0), fontSize);
            characterSprites.put(s, img);
        }

        for ( c = 'A'; c <= 'Z'; c++ ) {
            s = "" + c;
            img = AndroidSystem.calculateLabelImage(
                    s, new ColorRgb(1.0, 1.0, 1.0), fontSize);
            characterSprites.put(s, img);
        }

        for ( c = '0'; c <= '9'; c++ ) {
            s = "" + c;
            img = AndroidSystem.calculateLabelImage(
                    s, new ColorRgb(1.0, 1.0, 1.0), fontSize);
            characterSprites.put(s, img);
        }

        img = AndroidSystem.calculateLabelImage(
                ".", new ColorRgb(1.0, 1.0, 1.0), fontSize);
        characterSprites.put(".", img);

        img = AndroidSystem.calculateLabelImage(
                ",", new ColorRgb(1.0, 1.0, 1.0), fontSize);
        characterSprites.put(",", img);

        img = AndroidSystem.calculateLabelImage(
                " ", new ColorRgb(1.0, 1.0, 1.0), fontSize);
        characterSprites.put(" ", img);

        img = AndroidSystem.calculateLabelImage(
                "_", new ColorRgb(1.0, 1.0, 1.0), fontSize);
        characterSprites.put("_", img);

        img = AndroidSystem.calculateLabelImage(
                ":", new ColorRgb(1.0, 1.0, 1.0), fontSize);
        characterSprites.put(":", img);

        img = AndroidSystem.calculateLabelImage(
                "/", new ColorRgb(1.0, 1.0, 1.0), fontSize);
        characterSprites.put("/", img);
    }

    private void loadBumpmap() {
        //-----------------------------------------------------------------
        try {
            bumpmap =
                    ImagePersistence.importIndexedColor(
                            new File("/storage/extSdCard/earth.bw"));
            normalMap = new NormalMap();
            normalMap.importBumpMap(bumpmap, new Vector3D(1, 1, 0.2));
            
            testImage = normalMap.exportToRgbImage();
        }
        catch ( Exception e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR,
                    "createModel", "Can not load bumpmap!", e);
        }
        
        System.out.println("XXX: Bumpmap of size " +
                bumpmap.getXSize() + " x " + bumpmap.getYSize());
    }

    public void resetTimers()
    {
        TimeReport tr;
        Set<String> s = timers.keySet();
        for ( String e : s ) {
            tr = timers.get(e);
            tr.reset();
        }
    }

    /**
    Vitral framework for scene render control takes into account several cases:
    error checking, startup screen and actual application screen. Note that
    this method is supposed to be kept simple.
    @param glUnused
    */
    public void onDrawFrame(GL10 glUnused) {
        if ( firstTimer ) {
            firstTimer = false;

            createSecondaryGLES20RenderingThread();
        }

        if ( errorsDetected ) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }
        
        if ( isDisplayListsCompiled() ) {
            drawCurrent3DScene();
        }
        else {
            drawStartupScreen();
        }
        
        frameCount++;
    }

    /**
    This method is called each time a frame is needed and model is not loaded
    from secondary storage to main memory or graphic assets are not yet
    sent to GPU. This method should not depend on complex loading operations
    as is supposed to be shown rapidly to the used, as soon as application is
    loaded. This method is supposed to show an splash screen for application
    startup, with some kind of "wait for application loading" message to
    end user.
    */
    private void drawStartupScreen() {
        Camera c = getCamera();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        int y;
        
        //- Set up textures -----------------------------------------------
        /*
        if ( startupTexture == null ) {
            InputStream is;

            try {
                is = androidApplicationContext.getResources().openRawResource(
                    R.raw.startuph);
                startupTexture = ImagePersistence.importRGB(is);
            }
            catch ( Exception e ) {
                VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, 
                    "createModel", "Can not load texture!", e);
            }
        }
        */
        
        //---------------------------------------------------------------------
        RendererConfiguration q;
        double fx, fy;
        double dx, dy;

        fx = 2.0;
        fy = 2.0;
        dx = 0.0;
        dy = 0.0;
        
        q = new RendererConfiguration();
        q.setSurfaces(true);
        //q.setTexture(true);
        q.setUseVertexColors(true);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslated(dx, dy, 0);
        glScaled(fx, fy, 1.0);
        setRendererConfiguration(q);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //AndroidGLES20ImageRenderer.activate(startupTexture);
        //setTextureParameters();
        //drawUnitSquare();

        //---------------------------------------------------------------------
        y = 10;
        drawText("Frame: " + frameCount, getCamera(), 10, y);
    }

    /**
    This method controls the rendering of 3d scene on main application
    functionality, as all model elements are loaded from secondary storage to
    main RAM and all graphics assets such as display lists and textures are 
    ready sent to GPU.
    */
    private void drawCurrent3DScene() {
        if ( doRaytrace ) {
            raytrace();
            doRaytrace = false;
        }
        
        if ( frameCount > 1 ) timers.get("02_GEOMETRY").start();

        if ( quality.isBumpMapSet() &&
                (bumpmap == null || normalMap == null) ) {
            loadBumpmap();
        }
        
        // Draw background
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Tick updates transform
        long time = SystemClock.uptimeMillis() % 8000L;
        float x = 0.0005f * ((int) time);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Move light around center...
        ArrayList<Light> l = getScene().scene.getLights();
        if ( withLightRotation && l.size() > 0 ) {
            l.get(0).setPosition(new Vector3D(firstLightRadius, 0, 0));
            Matrix4x4 RL = new Matrix4x4();
            RL.axisRotation(Math.toRadians(-50.0*x), 0, 0, 1);
            Vector3D P, PR;
            P = l.get(0).getPosition();
            PR = RL.multiply(P);
            l.get(0).setPosition(PR);
        }

        //-----------------------------------------------------------------
        AndroidGLES20Renderer.setRendererConfiguration(quality);

        //-----------------------------------------------------------------
        glLoadIdentity();
        
        if ( withObjectRotation ) {
            int i;
            
            for ( i = 0; scene.scene.getSimpleBodies().size() >= 1 &&
                    i < numberOfAffectedObjectsOnScene; i++ ) {
                SimpleBody b = scene.scene.getSimpleBodies().get(i);
                Matrix4x4 original = b.getRotation();
                Matrix4x4 delta = new Matrix4x4();
                delta.axisRotation(Math.toRadians(1.0), new Vector3D(0, 0, 1));
                b.setRotation(delta.multiply(original));
            }
        }
        AndroidGLES20SceneRenderer.draw(getScene(), quality);

        //-----------------------------------------------------------------
        AndroidGLES20MaterialRenderer.activate(material);
        glLoadIdentity();
        //glTranslated(-2, 0, 0);
        if ( withObjectRotation ) {
            glRotated(200*x, 0, 0, 1);
        }
        
        // Activate texture image.activate()
        if ( quality.isTextureSet() ) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            AndroidGLES20ImageRenderer.activate(getTexture());
            activateDefaultTextureParameters();
        }

        if ( quality.isBumpMapSet() ) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            AndroidGLES20ImageRenderer.activate(testImage);
            activateDefaultTextureParameters();
        }

        if ( highResSphere ) {
            AndroidGLES20SphereRenderer.setDefaultSlicesStacks(50, 25);
        }
        else {
            AndroidGLES20SphereRenderer.setDefaultSlicesStacks(20, 10);
        }

        if ( box != null ) {
            AndroidGLES20GeometryRenderer.draw(box, getScene().camera, quality);
        }

        if ( cone != null ) {
            AndroidGLES20GeometryRenderer.draw(cone, getScene().camera, quality);
        }

        //-----------------------------------------------------------------
        if ( withReferenceSquare ) {
            //if ( raytracingImage != null ) {
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            glTranslated(x, 0, 0);
            setRendererConfiguration(quality);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            AndroidGLES20ImageRenderer.activate(testImage);
            activateDefaultTextureParameters();
            
            drawUnitSquare();
        }
        
        if ( frameCount > 1 ) timers.get("02_GEOMETRY").stop();
        
        //- Draw HUD elements ---------------------------------------------
        drawHudElements();
    }

    /**
    Creates a secondary OpenGL ES 2.0 rendering thread, with a context shared
    with current thread's OpenGL ES 2.0 context, in an offline surface
    (pbuffer), supposed to fetch rendering assets from model main memory to
    GPU memory. Current operation is non-blocking with respect to current
    thread.
    */
    private void createSecondaryGLES20RenderingThread() {
        AndroidGLES20AssetLoader assetLoaderRunnable;
        Thread assetLoaderThread;
        
        assetLoaderRunnable = new AndroidGLES20AssetLoader(this);
        assetLoaderThread = new Thread(assetLoaderRunnable);
        assetLoaderThread.setName("OpenGLES20AssetLoader");
        assetLoaderThread.start();
    }

    private void drawHudElements() {
        timers.get("03_HUD").start();
        
        if ( withHudReport ) {
            int y = 10;
            drawText("Frame: " + frameCount, getCamera(), 10, y);

            TimeReport tr;
            Set<String> s = timers.keySet();
            for (String e : s) {
                tr = timers.get(e);
                y += 40;
                drawText("" + tr, getCamera(), 10, y);
            }

            if (raytracingImage != null) {
                drawImage(raytracingImage, getCamera(), 10, y + 50);
            }
            
            y += 40;
            drawText("Viewport: (" + 
                    getCamera().getViewportXSize() + ", " + 
                    getCamera().getViewportYSize() + ")", 
                    getCamera(), 10, y);
            
            //AndroidGLES20ImageRenderer.unload(testImage);
        }
        timers.get("03_HUD").stop();
    }

    private void
    drawText(String msg, Camera c, int x0, int y0)
    {
        int x = x0, y = y0;
        int i;
        String key;

        for ( i = 0; i < msg.length(); i++ ) {
            key = "" + msg.charAt(i);
            if ( characterSprites.containsKey(key) ) {
                Image img = characterSprites.get(key);
                drawImage(img, c, x, y);
                x += img.getXSize()*2;
            }
        }
    }
 
    /**
    Draws an image at integer screen coordinates (x, y) in pixels from
    upper left corner. Takes into account current configured camera (viewpoint)
    @param img
    @param c
    @param x
    @param y
    */
    public void drawImage(Image img, Camera c, int x, int y)
    {
        RendererConfiguration q;
        double fx, fy;
        double dx, dy;

        fx = (((double)img.getXSize()) * 2.0) / 
             ((double)c.getViewportXSize());

        fy = (((double)img.getYSize()) * 2.0) / 
             ((double)c.getViewportYSize());

        dx = ((double)img.getXSize() + x) / 
            ((double)c.getViewportXSize());

        dy = ((double)img.getYSize() + y) / 
            ((double)c.getViewportYSize());

        q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setUseVertexColors(true);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        setRendererConfiguration(q);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslated(-1.0 + dx, 1.0 - dy, 0);
        glScaled(fx, fy, 1.0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        AndroidGLES20ImageRenderer.activate(img);
        activateDefaultTextureParameters();
        drawUnitSquare();
    }

    private void drawUnitSquare()
    {
        //-----------------------------------------------------------------
        // Geometry data
        float[] vertexDataArray = {
            // X, Y, Z, R, G, B, NX, NY, NZ, U, V
            -0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f };

        FloatBuffer verticesBufferedArray;

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);

        //-----------------------------------------------------------------
        drawVertices3Position3Color3Normal2Uv(
            verticesBufferedArray,
            GLES20.GL_TRIANGLE_STRIP, 4);
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        if ( errorsDetected ) {
            return;
        }

        getScene().camera.updateViewportResize(width, height);
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig configUnused) {
        //- Setup shader parameters ---------------------------------------
        init(getAndroidApplicationContext());
    }

    /**
    Creates a rendering from current scene using raytracer. 
    */
    private void raytrace()
    {
        raytracingImage = new RGBImage();
        raytracingImage.init(256, 256);
        getScene().raytrace(raytracingImage, quality);
    }
    
    public void requestRaytracer()
    {
        doRaytrace = true;
    }

    /**
     * @return the withHudReport
     */
    public boolean isWithHudReport() {
        return withHudReport;
    }

    /**
     * @param withHudReport the withHudReport to set
     */
    public void setWithHudReport(boolean withHudReport) {
        this.withHudReport = withHudReport;
    }

    public void toggleHudReport() {
        withHudReport = !withHudReport;
    }

    /**
     * @return the displayListsCompiled
     */
    public boolean isDisplayListsCompiled() {
        return displayListsCompiled;
    }

    /**
     * @param displayListsCompiled the displayListsCompiled to set
     */
    public void setDisplayListsCompiled(boolean displayListsCompiled) {
        this.displayListsCompiled = displayListsCompiled;
    }

    /**
     * @return the androidApplicationContext
     */
    public Context getAndroidApplicationContext() {
        return androidApplicationContext;
    }

    /**
     * @return the texture
     */
    public RGBImage getTexture() {
        return texture;
    }

    /**
     * @param texture the texture to set
     */
    public void setTexture(RGBImage texture) {
        this.texture = texture;
    }

    /**
    @return the scene
    */
    public Scene getScene() {
        return scene;
    }
    
    public void clearSceneFromObjects()
    {
        scene.scene.getSimpleBodies().clear();
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        
        switch ( e.getAction() ) {
          case MotionEvent.ACTION_DOWN:            // one touch: drag
            //System.out.println("one down");
            mouseMovementsFromLastDown = 0;
            break;
          case MotionEvent.ACTION_POINTER_DOWN:    // two touches: zoom
            //System.out.println("two down");
            break;
          case MotionEvent.ACTION_UP:              // no mode
            mouseMovementsFromLastDown++;
            //System.out.println("up");
            break;
          case MotionEvent.ACTION_POINTER_UP:      // no mode
            //System.out.println("upup");
            break;
          case MotionEvent.ACTION_MOVE:            // rotation
            //System.out.println("move");
            mouseMovementsFromLastDown += 10;
            break;
        }

        MouseEvent evsdk = AndroidSystem.android2vsdkEvent(e);

        switch ( interaction ) {
          case 1:
            if ( mouseMovementsFromLastDown > 1 ) { // Drag
                cameraController.processMouseDraggedEvent(evsdk);
            }
            else if ( mouseMovementsFromLastDown == 1 ) { // Click
                getScene().selectObjectWithMouse(evsdk.getX(), evsdk.getY());
            }
            break;

          case 2:
            if ( mouseMovementsFromLastDown > 1 ) { // Drag
                cameraController.processMouseDraggedEvent(evsdk);
            }
            else if ( mouseMovementsFromLastDown == 1 ) { // Click
                getScene().insertSphereWithMouse(evsdk.getX(), evsdk.getY());
            }
            break;

          default:
            return false;
        }

        return true;
    }

    /**
    @param interaction the interaction to set
    */
    public void setInteraction(int interaction) {
        this.interaction = interaction;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
