//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.gui.ScaleGizmo;
import javax.media.opengl.GL;

public class JoglScaleGizmoRenderer extends JoglRenderer 
{

    public static void draw(GL gl, ScaleGizmo gizmo, Vector3D position)
    {
        Matrix4x4 R;

        R = new Matrix4x4(gizmo.getTransformationMatrix());

        R.M[0][3] = position.x;
        R.M[1][3] = position.y;
        R.M[2][3] = position.z;

        gl.glLineWidth(3);
        JoglMatrixRenderer.draw(gl, R);

        gl.glPushMatrix();
        JoglMatrixRenderer.activate(gl, R);
        gl.glColor3d(1, 1, 0);
        gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(-0.2, -0.2, 0);
            gl.glVertex3d(0.2, 0.2, 0);
            gl.glVertex3d(-0.2, 0.2, 0);
            gl.glVertex3d(0.2, -0.2, 0);
        gl.glEnd();
        gl.glPopMatrix();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
