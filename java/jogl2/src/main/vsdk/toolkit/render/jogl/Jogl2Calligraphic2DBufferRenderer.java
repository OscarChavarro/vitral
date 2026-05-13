package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.Calligraphic2DBuffer;

public class Jogl2Calligraphic2DBufferRenderer extends Jogl2Renderer
{
    public static void draw(GL2 gl, Calligraphic2DBuffer vectors)
    {
        int i;
        Vector3D p0;
        Vector3D p1;

        gl.glBegin(GL.GL_LINES);
        for ( i = 0; i < vectors.getNumLines(); i++ ) {
            Vector3D[] segment = vectors.get2DLine(i);
            p0 = segment[0];
            p1 = segment[1];
            gl.glVertex3d(p0.x(), p0.y(), p0.z());
            gl.glVertex3d(p1.x(), p1.y(), p1.z());
        }
        gl.glEnd();
    }
}
