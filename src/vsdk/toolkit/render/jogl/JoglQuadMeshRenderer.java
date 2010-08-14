//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 12 2008 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java classes
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// JOGL classes
import javax.media.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.geometry.QuadMesh;

public class JoglQuadMeshRenderer extends JoglRenderer
{

    private static void
    drawSurfaceBasic(GL2 gl, QuadMesh mesh)
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

    private static void
    drawSurfaceWithVertexArrays(GL2 gl, QuadMesh mesh)
    {
        FloatBuffer vertexPositionsBuffer;
        FloatBuffer vertexColorsBuffer;
        IntBuffer quadIndicesBuffer;
        double v[] = mesh.getVertexPositions();
        double n[] = mesh.getVertexNormals();
        double c[] = mesh.getVertexColors();
        int q[] = mesh.getQuadIndices();

        vertexPositionsBuffer = cloneDoubleArrayToFloatBuffer(v);
        vertexColorsBuffer = cloneDoubleArrayToFloatBuffer(c);
        quadIndicesBuffer = cloneIntArrayToIntBuffer(q);

        gl.glDisable(gl.GL_LIGHTING);

        //-----------------------------------------------------------------
        int info[] = new int[2];
        int i;
        int block;

        gl.glGetIntegerv(gl.GL_MAX_ELEMENTS_INDICES, info, 0);
        block = info[0];

        gl.glDisableClientState(gl.GL_EDGE_FLAG_ARRAY);
        gl.glDisableClientState(gl.GL_INDEX_ARRAY);
        gl.glDisableClientState(gl.GL_SECONDARY_COLOR_ARRAY);
        gl.glDisableClientState(gl.GL_FOG_COORD_ARRAY);
        gl.glDisableClientState(gl.GL_NORMAL_ARRAY);
        gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);

            gl.glVertexPointer(3, gl.GL_FLOAT, 0, vertexPositionsBuffer);
            gl.glColorPointer(3, gl.GL_FLOAT, 0, vertexColorsBuffer);

            for ( i = 0; i+block < q.length; i += block ) {
                quadIndicesBuffer.position(i);
                gl.glDrawElements(gl.GL_QUADS, block, gl.GL_UNSIGNED_INT, quadIndicesBuffer);
            }
            if ( i < q.length ) {
                quadIndicesBuffer.position(i);
                gl.glDrawElements(gl.GL_QUADS, q.length-i, gl.GL_UNSIGNED_INT, quadIndicesBuffer);
            }

        gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        //-----------------------------------------------------------------

        vertexPositionsBuffer = null;
        vertexColorsBuffer = null;
        quadIndicesBuffer = null;
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    draw(GL2 gl, QuadMesh mesh, RendererConfiguration quality, boolean flip) 
    {
        drawSurfaceBasic(gl, mesh);
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void
    drawWithVertexArrays(GL2 gl, QuadMesh mesh, RendererConfiguration quality, boolean flip) 
    {
        drawSurfaceWithVertexArrays(gl, mesh);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
