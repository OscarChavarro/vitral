//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

public class Jogl2PolyhedralBoundedSolidVertexRenderer extends Jogl2Renderer
{
    public static void
    drawPoints(GL2 gl, PolyhedralBoundedSolid solid)
    {
        ColorRgb c;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(2.0f);

        int i;
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solid.getVerticesList().get(i);
            Vector3D p = v.position;

            c = v.debugColor;
            if ( c.r >= 1 - VSDK.EPSILON &&
                 c.g <= VSDK.EPSILON &&
                 c.b <= VSDK.EPSILON ) {
                gl.glPointSize(5.0f);
            }
            else {
                gl.glPointSize(15.0f);
            }
            gl.glColor3d(c.r, c.g, c.b);

            gl.glBegin(GL.GL_POINTS);
                gl.glVertex3d(p.x(), p.y(), p.z());
            gl.glEnd();
        }
    }

    public static void
    drawDebugVertices(GL2 gl,
                      PolyhedralBoundedSolid solid,
                      Camera camera,
                      vsdk.toolkit.common.RendererConfiguration quality)
    {
        // Vertex-id labels are painted in the example HUD, not here.
    }

    public static void
    drawVertexNormals(GL2 gl, PolyhedralBoundedSolid solid)
    {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        int i;
        int j;

        //-----------------------------------------------------------------
        Vector3D p0 = null;
        Vertex vertex = new Vertex(p0);

        gl.glBegin(GL.GL_LINES);
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            // Logic
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( face.getContainingPlane() != null ) {
                vertex.normal = face.getContainingPlane().getNormal();
            }
            else {
                continue;
            }

            // Face polygon processing via JOGL GLU tesselator
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he;
                _PolyhedralBoundedSolidHalfEdge heStart;

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
                }
                heStart = he;
                do {
                    // Logic
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        break;
                    }

                    // Draw polygon parts
                    vertex.position = he.startingVertex.position;
                    Jogl2GeometryRenderer.drawVertexNormal(gl, vertex);
                } while( he != heStart );
            }
        }
        gl.glEnd();
    }

}
