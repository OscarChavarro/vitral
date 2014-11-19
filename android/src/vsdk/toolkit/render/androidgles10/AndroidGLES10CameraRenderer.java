//===========================================================================
package vsdk.toolkit.render.androidgles10;

// Android GLES 1.0 classes
import android.opengl.GLES10;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;

/**
*/
public class AndroidGLES10CameraRenderer extends AndroidGLES10Renderer {
    public static void activate(Camera c)
    {
        Matrix4x4 R;
        GLES10.glMatrixMode(GLES10.GL_PROJECTION);
        GLES10.glLoadIdentity();
        R = c.calculateProjectionMatrix();
        AndroidGLES10MatrixRenderer.activate(R);
        GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
