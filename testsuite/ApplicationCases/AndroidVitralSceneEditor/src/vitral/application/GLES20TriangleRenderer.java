//===========================================================================

package vitral.application;

// Java basic classes
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// Android classes
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.SystemClock;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.render.androidgles20.AndroidGLES20Renderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20LightRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20CameraRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20MaterialRenderer;
import vsdk.toolkit.render.androidgles20.AndroidGLES20SphereRenderer;

public class GLES20TriangleRenderer implements GLSurfaceView.Renderer {

    // Android application elements
    private Context androidApplicationContext;
    private AndroidGLES20Renderer vgl;

    // Vitral scene
    private RendererConfiguration qualitySelection;
    public Camera camera;
    private Sphere sphere;
    private Material material;
    private Light light1;
    private Light light2;

    // Display lists ids
    private int textureId;

    // Other
    long baserr = SystemClock.uptimeMillis();

    public GLES20TriangleRenderer(Context context) {
        androidApplicationContext = context;

        //-----------------------------------------------------------------
        Vector3D p = new Vector3D(0, -5, 5);
        Matrix4x4 R = new Matrix4x4();

        R.eulerAnglesRotation(Math.toRadians(90.0), Math.toRadians(-45.0), 0);

        camera = new Camera();
        camera.setPosition(p);
        camera.setRotation(R);

        qualitySelection = new RendererConfiguration();
        qualitySelection.setPoints(false);
        qualitySelection.setWires(false);
        qualitySelection.setSurfaces(true);
        qualitySelection.setWireColor(new ColorRgb(1, 1, 1));

        material = new Material();
        material.setAmbient(new ColorRgb(0.2, 0.2, 0.2));
        material.setDiffuse(new ColorRgb(1.0, 1.0, 1.0));
        material.setSpecular(new ColorRgb(1.0, 1.0, 1.0));
        material.setPhongExponent(40.0);

        light1 = new Light(Light.POINT, 
            new Vector3D(0, -10, 0), new ColorRgb(1, 1, 1));
        light2 = new Light(Light.POINT, 
            new Vector3D(1, -1, 1), new ColorRgb(1, 0, 0));

        sphere = new Sphere(1.0);
    }

    public void onDrawFrame(GL10 glUnused) {
        if ( vgl.errorsDetected ) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }

        // Draw background
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Activate texture image.activate()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // camera.activate()
        AndroidGLES20CameraRenderer.activate(camera);

        // Tick updates transform
        long time = SystemClock.uptimeMillis() % 8000L;
        long rr = (SystemClock.uptimeMillis() - baserr) / 8000L;
        float x = 0.0005f * ((int) time);

        vgl.glMatrixMode(vgl.GL_MODELVIEW);
        vgl.glLoadIdentity();

        // Move light around center...
        double r = 1.2 + 0.2 * ((double)rr);
        light1.setPosition(new Vector3D(r, 0, 0));
        Matrix4x4 RL = new Matrix4x4();
        RL.axisRotation(Math.toRadians(-50.0*x), 0, 0, 1);
        Vector3D P, PR;
        P = light1.getPosition();
        PR = RL.multiply(P);
        light1.setPosition(PR);

        AndroidGLES20LightRenderer.draw(light1);
        AndroidGLES20LightRenderer.activate(light1);

        AndroidGLES20LightRenderer.draw(light2);
        AndroidGLES20LightRenderer.activate(light2);

        //vgl.glTranslated(-2, 0, 0);
        //vgl.glRotated(200*x, 0, 0, 1);

        //System.out.println("- OBJETO ESFERA SURFACES ----------- ");
        qualitySelection.setShadingType(
            RendererConfiguration.SHADING_TYPE_FLAT);
        qualitySelection.setWires(false);
        qualitySelection.setSurfaces(true);
        qualitySelection.setTexture(false);
        sphere.setRadius(1.0);
        AndroidGLES20MaterialRenderer.activate(material);
        AndroidGLES20SphereRenderer.draw(sphere, camera, qualitySelection);

        //System.out.println("- OBJETO PLANO ----------- ");
        vgl.glMatrixMode(vgl.GL_MODELVIEW);
        vgl.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        vgl.glLoadIdentity();
        vgl.glTranslated(x, 0, 0);
        vgl.glEnable(vgl.GL_TEXTURE_2D);
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
        int[] textures = new int[1];

        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        InputStream is;

        is = androidApplicationContext.getResources().openRawResource(
            R.raw.render);
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
          } 
          catch(IOException e) {
            ;
        }

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
