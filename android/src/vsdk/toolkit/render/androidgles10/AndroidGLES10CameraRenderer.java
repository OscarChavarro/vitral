//===========================================================================
package vsdk.toolkit.render.androidgles10;

// Android GLES 1.0 classes
import javax.microedition.khronos.opengles.GL10;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;

/**
*/
public class AndroidGLES10CameraRenderer extends AndroidGLES10Renderer {
    public static void activate(GL10 gl, Camera c)
    {
        Matrix4x4 R = c.calculateProjectionMatrix();
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        R = c.calculateProjectionMatrix();
        AndroidGLES10MatrixRenderer.activate(gl, R);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
