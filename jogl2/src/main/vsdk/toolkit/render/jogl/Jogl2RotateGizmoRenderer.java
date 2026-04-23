package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.gui.RotateGizmo;

public class Jogl2RotateGizmoRenderer extends Jogl2Renderer 
{

    public static void draw(GL2 gl, RotateGizmo gizmo, Vector3D position)
    {
        Matrix4x4 R, giro;

        R = new Matrix4x4(gizmo.getTransformationMatrix());
        R = R.withTranslation(position);

        gl.glLineWidth(3);
        Jogl2MatrixRenderer.draw(gl, R);
        gl.glLineWidth(1);

        double a;
        double delta = Math.toRadians(15);
        Vector3D agujax = new Vector3D(0.5, 0, 0);
        Vector3D agujay = new Vector3D(0, 0.5, 0);
        Vector3D agujaz = new Vector3D(0, 0, 0.5);
        Vector3D p;
        giro = new Matrix4x4();

        gl.glPushMatrix();
        Jogl2MatrixRenderer.activate(gl, R);
        gl.glColor3d(1, 0, 0);
        gl.glBegin(GL.GL_LINE_LOOP);
        for ( a = delta; a < Math.toRadians(360-delta); a += delta ) {
            giro = giro.axisRotation(a, 1, 0, 0);
            p = giro.multiply(agujay);
            gl.glVertex3d(p.x(), p.y(), p.z());
        }
        gl.glEnd();
        gl.glPopMatrix();

        gl.glPushMatrix();
        Jogl2MatrixRenderer.activate(gl, R);
        gl.glColor3d(0, 1, 0);
        gl.glBegin(GL.GL_LINE_LOOP);
        for ( a = delta; a < Math.toRadians(360-delta); a += delta ) {
            giro = giro.axisRotation(a, 0, 1, 0);
            p = giro.multiply(agujax);
            gl.glVertex3d(p.x(), p.y(), p.z());
        }
        gl.glEnd();
        gl.glPopMatrix();

        gl.glPushMatrix();
        Jogl2MatrixRenderer.activate(gl, R);
        gl.glColor3d(0, 0, 1);
        gl.glBegin(GL.GL_LINE_LOOP);
        for ( a = delta; a < Math.toRadians(360-delta); a += delta ) {
            giro = giro.axisRotation(a, 0, 0, 1);
            p = giro.multiply(agujax);
            gl.glVertex3d(p.x(), p.y(), p.z());
        }
        gl.glEnd();
        gl.glPopMatrix();


    }
}
