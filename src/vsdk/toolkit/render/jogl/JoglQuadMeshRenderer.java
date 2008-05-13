//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 12 2008 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.QuadMesh;

public class JoglQuadMeshRenderer extends JoglRenderer
{

    private static void
    drawSurfaceBasic(GL gl, QuadMesh mesh)
    {
        int i, p0, p1, p2, p3;
        double v[] = mesh.getVertexPositions();
        double n[] = mesh.getVertexNormals();
        double c[] = mesh.getVertexColors();
        int q[] = mesh.getQuadIndices();

        gl.glDisable(gl.GL_LIGHTING);
        gl.glBegin(gl.GL_QUADS);
        for ( i = 0; i < mesh.getNumQuads(); i++ ) {
            p0 = q[4*i];
            p1 = q[4*i+1];
            p2 = q[4*i+2];
            p3 = q[4*i+3];
            gl.glColor3d(c[3*p0], c[3*p0+1], c[3*p0+2]);
            gl.glVertex3d(v[3*p0], v[3*p0+1], v[3*p0+2]);
            gl.glColor3d(c[3*p1], c[3*p1+1], c[3*p1+2]);
            gl.glVertex3d(v[3*p1], v[3*p1+1], v[3*p1+2]);
            gl.glColor3d(c[3*p2], c[3*p2+1], c[3*p2+2]);
            gl.glVertex3d(v[3*p2], v[3*p2+1], v[3*p2+2]);
            gl.glColor3d(c[3*p3], c[3*p3+1], c[3*p3+2]);
            gl.glVertex3d(v[3*p3], v[3*p3+1], v[3*p3+2]);
        }
        gl.glEnd();
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL gl, QuadMesh mesh, RendererConfiguration quality, boolean flip) 
    {
        drawSurfaceBasic(gl, mesh);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
