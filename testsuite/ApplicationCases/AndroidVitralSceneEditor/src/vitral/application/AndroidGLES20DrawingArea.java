//===========================================================================
package vitral.application;

// Java basic classes
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

// Android classes: misc
import android.content.Context;
import android.os.SystemClock;
import android.graphics.Typeface;

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
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.io.geometry.ReaderPly;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.render.androidgles20.AndroidGLES20GeometryRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20MaterialRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20ImageRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SphereRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;

public class AndroidGLES20DrawingArea extends AndroidGLES20Renderer implements GLSurfaceView.Renderer {

    // Android application elements
    private Context androidApplicationContext;

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
    private RGBAImage textImage;

    // Other
    //private long baserr = SystemClock.uptimeMillis();
    private boolean withObjectRotation = false;
    private boolean withLightRotation = true;
    private boolean withReferenceSquare = false;
    private boolean highResSphere = false;
    
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
            mesh = meshMug;
            break;
        }
    }

    private TriangleMesh loadPlyMesh(String filename)
    {
        File meshFile;

        System.out.println("Loading mesh " + filename);
        meshFile = new File(filename);
        SimpleScene tscene = new SimpleScene();
        TriangleMesh m = null;
        try {
            ReaderPly.importEnvironment(meshFile, tscene);
            m = (TriangleMesh)(tscene.getSimpleBodies().get(0).getGeometry());
            System.out.println(mesh.toString());
          }
          catch ( Exception e ) {
            System.out.println("= ERROR LOADING MESH");
            e.printStackTrace();
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

    private void createModel()
    {
        //-----------------------------------------------------------------
        scene = new Scene();
        raytracingImage = null;
        quality = new RendererConfiguration();
        quality.setPoints(false);
        quality.setWires(false);
        quality.setWireColor(new ColorRgb(1.0, 1.0, 1.0));
        quality.setSurfaces(true);
        quality.setTexture(true);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);

        material = new Material();
        material.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        material.setDiffuse(new ColorRgb(1.0, 1.0, 1.0));
        material.setSpecular(new ColorRgb(1.0, 1.0, 1.0));
        material.setPhongExponent(40.0);

        prepareLights(1);
        
        meshMug = loadPlyMesh("/storage/extSdCard/mug.ply");
        selectObject(2);

        //-----------------------------------------------------------------
        Typeface tf = Typeface.MONOSPACE;

        textImage = new RGBAImage();
        textImage.init(120, 30);
        textImage.createTestPattern();
    }

    public AndroidGLES20DrawingArea(Context context) {
        androidApplicationContext = context;

        createModel();
    }

    /**
    This method should be called after every call to a texture activation
    (glBindTexture)
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

    public void onDrawFrame(GL10 glUnused) {
        if ( errorsDetected ) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }

        // Draw background
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Tick updates transform
        long time = SystemClock.uptimeMillis() % 8000L;
        //long rr = (SystemClock.uptimeMillis() - baserr) / 8000L;
        float x = 0.0005f * ((int) time);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Move light around center...
        double r = 2.0;
        ArrayList<Light> lights = scene.scene.getLights();
        if ( withLightRotation && lights.size() > 0 ) {
            lights.get(0).setPosition(new Vector3D(r, 0, 0));
            Matrix4x4 RL = new Matrix4x4();
            RL.axisRotation(Math.toRadians(-50.0*x), 0, 0, 1);
            Vector3D P, PR;
            P = lights.get(0).getPosition();
            PR = RL.multiply(P);
            lights.get(0).setPosition(PR);
        }

        AndroidGLES20SceneRenderer.draw(scene, quality);

        glLoadIdentity();
        //glTranslated(-2, 0, 0);
        if ( withObjectRotation ) {
            glRotated(200*x, 0, 0, 1);
        }

        //-----------------------------------------------------------------
        AndroidGLES20MaterialRenderer.activate(material);
        AndroidGLES20Renderer.setRendererConfiguration(quality);

        // Activate texture image.activate()
        if ( quality.isTextureSet() ) {
            AndroidGLES20ImageRenderer.activate(texture);
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
            glEnable(GL_TEXTURE_2D);
            setRendererConfiguration(quality);

            AndroidGLES20ImageRenderer.activate(textImage);
            setTextureParameters();
            drawUnitSquare();
        }

        //- Draw HUD elements ---------------------------------------------
        drawHUD(textImage, getCamera());
    }
 
    private void drawHUD(Image img, Camera c)
    {
	RendererConfiguration q;
        double fx, fy;

        fx = (((double)img.getXSize()) * 2.0) / 
             ((double)c.getViewportXSize());

        fy = (((double)img.getYSize()) * 2.0) / 
             ((double)c.getViewportYSize());

        q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslated(-1.0, 0, 0);
        glScaled(fx, fy, 1.0);
        glEnable(GL_TEXTURE_2D);
        setRendererConfiguration(q);
        AndroidGLES20ImageRenderer.activate(img);
        setTextureParameters();
        drawUnitSquare();
    }

    private void drawUnitSquare()
    {
        //-----------------------------------------------------------------
        // Geometry data
        int FLOAT_SIZE_IN_BYTES = 4;
        int VERTEX_SIZE_IN_BYTES = 8 * FLOAT_SIZE_IN_BYTES;
        float[] vertexDataArray = {
            // X, Y, Z, R, G, B, U, V
            -0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

        FloatBuffer verticesBufferedArray;

        verticesBufferedArray = ByteBuffer.allocateDirect(
            vertexDataArray.length * FLOAT_SIZE_IN_BYTES).order(
            ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBufferedArray.put(vertexDataArray);

        //-----------------------------------------------------------------
	drawVertices3Position3Color2Uv(
            verticesBufferedArray,
            GLES20.GL_TRIANGLE_STRIP,
            4,
            VERTEX_SIZE_IN_BYTES
        );
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
                R.raw.render);
            texture = ImagePersistence.importRGB(is);
        }
        catch ( Exception e ) {
        }
    }

    public void raytrace()
    {
        raytracingImage = new RGBImage();
        raytracingImage.init(128, 128);
        scene.raytrace(raytracingImage, quality);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
