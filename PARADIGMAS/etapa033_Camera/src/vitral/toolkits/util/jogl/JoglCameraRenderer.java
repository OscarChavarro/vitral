//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 22 2005 - David Diaz: Original base version                    =
//===========================================================================

package vitral.toolkits.util.jogl;

import vitral.toolkits.common.Matrix4x4;
import vitral.toolkits.environment.Camera;
import net.java.games.jogl.GL;

/**
 *
 * @author Oscar Chavarro, David Diaz
 */
public class JoglCameraRenderer 
{
    /**
    stereoMode must have one of the STEREO_MODE values
    */
    public static void activateGL(GL gl, int stereoMode, Camera cam)
    {
        Matrix4x4 R;

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        R = cam.calculateProjectionMatrix(stereoMode);
        JoglMatrixRenderer.activateGL(gl, R);
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }

    public static void activateGL(GL gl, Camera cam)
    {
        activateGL(gl, Camera.STEREO_MODE_CENTER, cam);
    }
    
}
