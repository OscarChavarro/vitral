//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 22 2005 - David Diaz: Original base version                    =
//= - November 15 2005 - Oscar Chavarro: Migrated to JOGL Beta Version      =
//= - November 28 2005 - Oscar Chavarro: Added activateGLCenter method      =
//===========================================================================

package vitral.toolkits.visual.jogl;

import vitral.toolkits.common.Matrix4x4;
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.environment.Camera;
import javax.media.opengl.GL;

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

    public static void activateGLCenter(GL gl, Camera cam)
    {
        Matrix4x4 R;
        Camera camera2 = new Camera(cam);
        Vector3D eye, center, neweye, newcenter;

        eye = camera2.getPosition();
        center = camera2.getFocusedPosition();
        neweye = new Vector3D(0, 0, 0);
        newcenter = center.substract(eye);
        camera2.setPosition(neweye);
        camera2.setFocusedPositionDirect(newcenter);
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        R = camera2.calculateProjectionMatrix(Camera.STEREO_MODE_CENTER);
        JoglMatrixRenderer.activateGL(gl, R);
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
