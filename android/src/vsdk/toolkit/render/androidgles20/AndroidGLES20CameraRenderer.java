//===========================================================================

package vsdk.toolkit.render.androidgles20;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;

public class AndroidGLES20CameraRenderer extends AndroidGLES20Renderer
{
    public static void activate(Camera c)
    {
        Matrix4x4 MProjection = c.calculateProjectionMatrix();
        float array[] = MProjection.exportToFloatArrayColumnOrder();
        int i;
        for ( i = 0; i < 16; i++ ) {
	    projectionMatrix[i] = array[i];
	}
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
