//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.gui.TranslateGizmo;
import javax.media.opengl.GL;

public class JoglTranslateGizmoRenderer
{

    public static void draw(GL gl, TranslateGizmo gizmo, Vector3D position)
    {
        Matrix4x4 R;

        R = new Matrix4x4(); //gizmo.getTransformationMatrix());

        R.M[0][3] = position.x;
        R.M[1][3] = position.y;
        R.M[2][3] = position.z;

        gl.glLineWidth(3);
        JoglMatrixRenderer.drawGL(gl, R);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
