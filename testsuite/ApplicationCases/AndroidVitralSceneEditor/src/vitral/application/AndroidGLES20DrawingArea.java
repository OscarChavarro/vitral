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

// Android classes: OpenGL ES 2.0
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
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
import vsdk.toolkit.render.androidgles20.AndroidGLES20BoxRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20CameraRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20ConeRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20LightRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20MaterialRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20RGBImageRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20RGBAImageRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SphereRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20TriangleMeshRenderer;

public class AndroidGLES20DrawingArea implements GLSurfaceView.Renderer {

    // Android application elements
    private Context androidApplicationContext;
    private AndroidGLES20Renderer vgl;

    // Vitral scene
    private RendererConfiguration quality;
    private Material material;
    private Camera camera;
    private ArrayList<Light> lights;
    private Sphere sphere;
    private Box box;
    private Cone cone;
    private TriangleMesh meshMug;
    private TriangleMesh mesh;
    private RGBAImage texture;
    
    // Display lists ids
    private int textureId;

    // Other
    private long baserr = SystemClock.uptimeMillis();
    private boolean withObjectRotation = false;
    private boolean withLightRotation = true;
    private boolean withReferenceSquare = false;
    private boolean highResSphere = false;
    
    public RendererConfiguration getRendererConfiguration()
    {
        return quality;
    }

    public Camera getCamera()
    {
        return camera;
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
        ReaderPly reader;

        System.out.println("Loading mesh " + filename);
        meshFile = new File(filename);
        reader = new ReaderPly();
        SimpleScene scene = new SimpleScene();
        TriangleMesh m = null;
        try {
            reader.importEnvironment(meshFile, scene);
            m = (TriangleMesh)(scene.getSimpleBodies().get(0).getGeometry());
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

        lights = new ArrayList<Light>();

        if ( n > 3 ) {
            n = 3;
	}

        for ( int i = 0; i < n; i++ ) {
            l = new Light(Light.POINT, 
		new Vector3D(p[6*i+0], p[6*i+1], p[6*i+2]), 
                new ColorRgb(p[6*i+3], p[6*i+4], p[6*i+5]));
            lights.add(l);
	}
    }

    private void createModel()
    {
        //-----------------------------------------------------------------
        Vector3D p = new Vector3D(0, -5, 5);
        Matrix4x4 R = new Matrix4x4();

        R.eulerAnglesRotation(Math.toRadians(90.0), Math.toRadians(-45.0), 0);

        camera = new Camera();
        camera.setPosition(p);
        camera.setRotation(R);

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
        selectObject(1);
    }

    public AndroidGLES20DrawingArea(Context context) {
        androidApplicationContext = context;

        createModel();
    }

    public void onDrawFrame(GL10 glUnused) {
        if ( vgl.errorsDetected ) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }

        // Draw background
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // camera.activate()
        AndroidGLES20CameraRenderer.activate(camera);

        // Tick updates transform
        long time = SystemClock.uptimeMillis() % 8000L;
        long rr = (SystemClock.uptimeMillis() - baserr) / 8000L;
        float x = 0.0005f * ((int) time);

        vgl.glMatrixMode(vgl.GL_MODELVIEW);
        vgl.glLoadIdentity();

        // Move light around center...
        double r = 2.0;
        if ( withLightRotation ) {
            lights.get(0).setPosition(new Vector3D(r, 0, 0));
            Matrix4x4 RL = new Matrix4x4();
            RL.axisRotation(Math.toRadians(-50.0*x), 0, 0, 1);
            Vector3D P, PR;
            P = lights.get(0).getPosition();
            PR = RL.multiply(P);
            lights.get(0).setPosition(PR);
	}

        for ( int i = 0; i < lights.size(); i++ ) {
            AndroidGLES20LightRenderer.draw(lights.get(i));
            AndroidGLES20LightRenderer.activate(lights.get(i));
	}

        //vgl.glTranslated(-2, 0, 0);
        if ( withObjectRotation ) {
            vgl.glRotated(200*x, 0, 0, 1);
        }

        //-----------------------------------------------------------------
        AndroidGLES20MaterialRenderer.activate(material);
        AndroidGLES20Renderer.setRendererConfiguration(quality);

        // Activate texture image.activate()
        if ( quality.isTextureSet() ) {
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            AndroidGLES20RGBAImageRenderer.activate(texture, textureId);
        }

        if ( sphere != null ) {
            sphere.setRadius(1.0);
            if ( highResSphere ) {
                AndroidGLES20SphereRenderer.draw(sphere, camera, quality, 50, 25);
	    }
	    else {
                AndroidGLES20SphereRenderer.draw(sphere, camera, quality, 20, 10);
	    }
        }

        if ( box != null ) {
            AndroidGLES20BoxRenderer.draw(box, camera, quality);
        }

        if ( cone != null ) {
            AndroidGLES20ConeRenderer.draw(cone, camera, quality);
        }
        
        if ( mesh != null ) {
            vgl.glScaled(15, 15, 15);
            vgl.glRotated(90, 1, 0, 0);
            AndroidGLES20TriangleMeshRenderer.draw(mesh, camera, quality);
        }
        
        //-----------------------------------------------------------------
        if ( withReferenceSquare ) {
            vgl.glMatrixMode(vgl.GL_MODELVIEW);
            vgl.glLoadIdentity();
            vgl.glTranslated(x, 0, 0);
            vgl.glEnable(vgl.GL_TEXTURE_2D);
            vgl.setRendererConfiguration(quality);
            drawUnitSquare();
        }
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
        // Prepare geometry
        // glVertex3d
        verticesBufferedArray.position(0);
        GLES20.glVertexAttribPointer(vgl.PObjectParam, 3, GLES20.GL_FLOAT, 
            false, VERTEX_SIZE_IN_BYTES, verticesBufferedArray);
        vgl.checkGlError("glVertexAttribPointer PObject");
        GLES20.glEnableVertexAttribArray(vgl.PObjectParam);
        vgl.checkGlError(
            "glEnableVertexAttribArray PObjectParam");

        // glColor3d
        verticesBufferedArray.position(3);
        GLES20.glEnableVertexAttribArray(vgl.emissionColorParam);
        vgl.checkGlError("glEnableVertexAttribArray emissionColorParam");
        GLES20.glVertexAttribPointer(vgl.emissionColorParam, 3, GLES20.GL_FLOAT,
            false, VERTEX_SIZE_IN_BYTES, verticesBufferedArray);
        vgl.checkGlError("glVertexAttribPointer emissionColorParam");

        // glTexCoord2d
        verticesBufferedArray.position(6);
        GLES20.glVertexAttribPointer(vgl.uvVertexTextureCoordinateParam, 2, 
            GLES20.GL_FLOAT, false, VERTEX_SIZE_IN_BYTES, 
            verticesBufferedArray);
        vgl.checkGlError(
            "glVertexAttribPointer uvVertexTextureCoordinateParam");
        GLES20.glEnableVertexAttribArray(vgl.uvVertexTextureCoordinateParam);
        vgl.checkGlError(
            "glEnableVertexAttribArray uvVertexTextureCoordinateParam");

        //-----------------------------------------------------------------
        // Draw geometry
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        vgl.checkGlError("glDrawArrays");
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        if ( vgl.errorsDetected ) {
            return;
        }

        camera.updateViewportResize(width, height);
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig configUnused) {
        //- Setup shader parameters ---------------------------------------
        vgl = new AndroidGLES20Renderer();
        vgl.init(androidApplicationContext);

        if ( vgl.errorsDetected ) {
            return;
        }
 
        //- Set up textures -----------------------------------------------
        InputStream is;

        try {
            is = androidApplicationContext.getResources().openRawResource(
                R.raw.render);
            texture = ImagePersistence.importRGBA(is);
	}
	catch ( Exception e ) {
	}
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
