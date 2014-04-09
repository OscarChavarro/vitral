//===========================================================================
package vitral.application;

// Java basic classes
import java.io.File;
import java.io.InputStream;
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
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.gui.AndroidSystem;
import vsdk.toolkit.io.geometry.ReaderPly;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.render.androidgles20.AndroidGLES20GeometryRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20MaterialRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20ImageRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SphereRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;

public class AndroidGLES20DrawingArea extends AndroidGLES20Renderer 
implements GLSurfaceView.Renderer {

    // Android application elements
    private final Context androidApplicationContext;

    // Vitral scene
    private Scene scene;
    private RendererConfiguration quality;
    private Material material;
    private Sphere sphere;
    private Box box;
    private Cone cone;
    private TriangleMesh meshMug;
    private TriangleMesh mesh;
    private RGBImage texture;
    private RGBImage raytracingImage;
    private RGBImage testImage;
    private IndexedColorImage bumpmap;
    private NormalMap normalMap;
    private boolean highResSphere = false;
    private boolean withReferenceSquare = false;
    private boolean doRaytrace = false;

    // Animation control
    private int frameCount;
    private boolean withObjectRotation = false;
    private boolean withLightRotation = false;

    // Hud & statistics
    private HashMap<String, TimeReport>timers;
    private HashMap<String, RGBAImage>characterSprites;

    public AndroidGLES20DrawingArea(Context context) {
        androidApplicationContext = context;

        createModel();
    }

    public RendererConfiguration getRendererConfiguration()
    {
        return quality;
    }

    public Scene getScene()
    {
        return scene;
    }

    public Camera getCamera()
    {
        return scene.camera;
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

    public void selectObject(int o)
    {
        sphere = null;
        mesh = null;
        box = null;
        cone = null;
        switch ( o ) {
          case 1: default:
            highResSphere = false;
            sphere = new Sphere(1.0);
            break;
          case 2:
            highResSphere = true;
            sphere = new Sphere(1.0);
            break;
          case 3:
            box = new Box(1.0, 1.0, 1.0);
            break;
          case 4:
            cone = new Cone(1.0, 1.0, 2.0);
            break;
          case 5:
            if ( meshMug == null ) {
                meshMug = loadPlyMesh("/storage/extSdCard/mug.ply");
            }
            mesh = meshMug;
            break;
        }

        resetTimers();
    }

    private TriangleMesh loadPlyMesh(String filename)
    {
        File meshFile;

        System.out.println("Loading mesh " + filename);
        meshFile = new File(filename);
        SimpleScene localScene;
        localScene = new SimpleScene();
        TriangleMesh m = null;
        try {
            ReaderPly.importEnvironment(meshFile, localScene);
            m = (TriangleMesh)(localScene.getSimpleBodies().get(0).getGeometry());
          }
          catch ( Exception e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, 
                "loadPlyMesh", "Error loading mesh " + filename, e);
        }
        return m;
    }

    public void prepareLights(int n)
    {
        Light l;
        double p[] = {0, -10, 0, 0.8, 0.8, 0.8,
                      1, -1, 1, 0, 1, 0,
                      -0.6, -2, 2, 0, 0, 1};

        ArrayList<Light> list = scene.scene.getLights();
        while ( !list.isEmpty() ) {
            list.remove(0);
        }

        if ( n > 3 ) {
            n = 3;
        }

        for ( int i = 0; i < n; i++ ) {
            l = new Light(Light.POINT, 
                new Vector3D(p[6*i+0], p[6*i+1], p[6*i+2]), 
                new ColorRgb(p[6*i+3], p[6*i+4], p[6*i+5]));
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
        
        meshMug = null;
        selectObject(2);
        createBitmapFontSprites();

        bumpmap = null;
        normalMap = null;
        
        //-----------------------------------------------------------------
        //testImage = new RGBImage();
        //testImage.init(128, 128);
        //testImage.createTestPattern();

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

    /**
    This method should be called after every call to a texture activation
    (glBindTexture).
    */
    private void setTextureParameters()
    {
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);
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

    public void onDrawFrame(GL10 glUnused) {
        
        if ( doRaytrace ) {
            raytrace();
            doRaytrace = false;
        }
        
        timers.get("02_GEOMETRY").start();

        if ( quality.isBumpMapSet() &&
             (bumpmap == null || normalMap == null) ) {
            loadBumpmap();
        }
        
        if ( errorsDetected ) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
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
        double r = 2.0;
        ArrayList<Light> l = scene.scene.getLights();
        if ( withLightRotation && l.size() > 0 ) {
            l.get(0).setPosition(new Vector3D(r, 0, 0));
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
        
        AndroidGLES20SceneRenderer.draw(scene, quality);

        AndroidGLES20MaterialRenderer.activate(material);
        glLoadIdentity();
        //glTranslated(-2, 0, 0);
        if ( withObjectRotation ) {
            glRotated(200*x, 0, 0, 1);
        }
        
        // Activate texture image.activate()
        if ( quality.isTextureSet() ) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            AndroidGLES20ImageRenderer.activate(texture);
            setTextureParameters();
        }

        if ( quality.isBumpMapSet() ) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            AndroidGLES20ImageRenderer.activate(testImage);
            setTextureParameters();
        }

        if ( sphere != null ) {
            sphere.setRadius(1.0);
            if ( highResSphere ) {
                AndroidGLES20SphereRenderer.setDefaultSlicesStacks(50, 25);
            }
            else {
                AndroidGLES20SphereRenderer.setDefaultSlicesStacks(20, 10);
            }
            AndroidGLES20GeometryRenderer.draw(sphere, scene.camera, quality);
        }

        if ( box != null ) {
            AndroidGLES20GeometryRenderer.draw(box, scene.camera, quality);
        }

        if ( cone != null ) {
            AndroidGLES20GeometryRenderer.draw(cone, scene.camera, quality);
        }
        
        if ( mesh != null ) {
            glScaled(15, 15, 15);
            glRotated(90, 1, 0, 0);
            AndroidGLES20GeometryRenderer.draw(mesh, scene.camera, quality);
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
            setTextureParameters();
            
            drawUnitSquare();
        }
        timers.get("02_GEOMETRY").stop();
        
        //- Draw HUD elements ---------------------------------------------
        drawHudElements();
        
        frameCount++;
    }

    private void drawHudElements() {
        timers.get("03_HUD").start();
        int y = 10;
        drawText("Frame: " + frameCount, getCamera(), 10, y);

        TimeReport tr;
        Set<String> s = timers.keySet();
        for ( String e : s ) {
            tr = timers.get(e);
            y += 40;
            drawText("" + tr, getCamera(), 10, y);
        }

        
        if ( raytracingImage != null ) {
            drawImage(raytracingImage, getCamera(), 10, y+50);
        }
        
        //AndroidGLES20ImageRenderer.unload(testImage);
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
    */
    private void drawImage(Image img, Camera c, int x, int y)
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
        setTextureParameters();
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

        scene.camera.updateViewportResize(width, height);
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig configUnused) {
        //- Setup shader parameters ---------------------------------------
        init(androidApplicationContext);

        if ( errorsDetected ) {
            return;
        }

        //- Set up textures -----------------------------------------------
        InputStream is;

        try {
            is = androidApplicationContext.getResources().openRawResource(
                R.raw.miniearth);
            texture = ImagePersistence.importRGB(is);
        }
        catch ( Exception e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR, 
                "createModel", "Can not load texture!", e);
        }
    }

    /**
    Creates a rendering from current scene using raytracer. 
    */
    private void raytrace()
    {
        raytracingImage = new RGBImage();
        raytracingImage.init(256, 256);
        scene.raytrace(raytracingImage, quality);
    }
    
    public void requestRaytracer()
    {
        doRaytrace = true;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
