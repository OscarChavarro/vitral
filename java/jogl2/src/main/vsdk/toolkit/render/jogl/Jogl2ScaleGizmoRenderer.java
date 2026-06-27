package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;

public class Jogl2ScaleGizmoRenderer extends Jogl2Renderer 
{

    public static void draw(GL2 gl, ScaleGizmo gizmo, Vector3Dd position)
    {
        Matrix4x4d R;

        R = new Matrix4x4d(gizmo.getTransformationMatrix());
        R = R.withTranslation(position);

        gl.glLineWidth(3);
        Jogl2MatrixRenderer.draw(gl, R);

        gl.glPushMatrix();
        Jogl2MatrixRenderer.activate(gl, R);
        gl.glColor3d(1, 1, 0);
        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(-0.2, -0.2, 0);
            gl.glVertex3d(0.2, 0.2, 0);
            gl.glVertex3d(-0.2, 0.2, 0);
            gl.glVertex3d(0.2, -0.2, 0);
        gl.glEnd();
        gl.glPopMatrix();
    }
}
