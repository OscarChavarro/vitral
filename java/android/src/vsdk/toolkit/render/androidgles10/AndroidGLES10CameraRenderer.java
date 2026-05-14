package vsdk.toolkit.render.androidgles10;

// Android GLES 1.0 classes
import android.opengl.GLES10;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.Camera;

public class AndroidGLES10CameraRenderer extends AndroidGLES10Renderer {
    public static void activate(Camera c)
    {
        Matrix4x4d R;
        GLES10.glMatrixMode(GLES10.GL_PROJECTION);
        GLES10.glLoadIdentity();
        R = c.calculateProjectionMatrix();
        AndroidGLES10MatrixRenderer.activate(R);
        GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
    }

    public static void activateCenter(final Camera inCamera) {
        Matrix4x4d R;
        Camera camera2 = new Camera(inCamera);
        Vector3Dd eye, center, neweye, newcenter;

        eye = camera2.getPosition();
        center = camera2.getFocusedPosition();
        neweye = new Vector3Dd(0, 0, 0);
        newcenter = center.substract(eye);
        camera2.setPosition(neweye);
        camera2.setFocusedPositionDirect(newcenter);
        camera2.setNearPlaneDistance(0.1);
        camera2.setFarPlaneDistance(10.0);
        
        GLES10.glMatrixMode(GLES10.GL_PROJECTION);
        GLES10.glLoadIdentity();
        R = camera2.calculateProjectionMatrix();
        AndroidGLES10MatrixRenderer.activate(R);
        GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
    }
}
