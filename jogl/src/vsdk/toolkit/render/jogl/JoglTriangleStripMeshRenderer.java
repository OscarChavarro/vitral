//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 2 2007 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.environment.geometry.TriangleStripMesh;

public class JoglTriangleStripMeshRenderer extends JoglRenderer {

    private static void drawPoints(GL2 gl, TriangleStripMesh mesh) {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(2.0f);

        gl.glBegin(GL.GL_POINTS);
        for ( int i = 0; i < mesh.getVertexes().length; i++ ) {
            gl.glVertex3d(mesh.getVertexAt(i).getPosition().x,
                          mesh.getVertexAt(i).getPosition().y,
                          mesh.getVertexAt(i).getPosition().z);
        }
        gl.glEnd();
    }

    private static void drawSurfacesWithoutTexture(GL2 gl, 
                                     TriangleStripMesh mesh, boolean flipNormals) {
        //-----------------------------------------------------------------
        // Note that glTexParameter and glTexEnv settings should be
        // configured before calling this method.

        //-----------------------------------------------------------------
        int strips[][] = mesh.getStrips();
        Vertex vertexes[] = mesh.getVertexes();
        Vertex v;
        int strip[];
        int i, j;

        for ( i = 0; i < strips.length; i++ ) {
            strip = strips[i];
            gl.glBegin(GL.GL_TRIANGLE_STRIP);
            for ( j = 0; j < strip.length; j++ ) {
                v = vertexes[strip[j]];
                gl.glNormal3d(v.normal.x, v.normal.y, v.normal.z);
                gl.glVertex3d(v.position.x, v.position.y, v.position.z);
            }
            gl.glEnd();
        }
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    \todo  Handle PHONG and BUMPMAPPING cases, via vertex/program shaders
    */
    public static void
    draw(GL2 gl, TriangleStripMesh mesh, RendererConfiguration quality, boolean flip)
    {
        //-----------------------------------------------------------------
        gl.glEnable(GL2.GL_NORMALIZE);

        if ( quality.isSurfacesSet() ) {
            JoglGeometryRenderer.prepareSurfaceQuality(gl, quality);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            //gl.glPolygonOffset(1.0f, 1.0f);

            drawSurfacesWithoutTexture(gl, mesh, flip);
        }
        if ( quality.isWiresSet() ) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glShadeModel(GL2.GL_FLAT);

            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glLineWidth(1.0f);

            // Warning: Change with configured color for borders
            ColorRgb c = quality.getWireColor();
            gl.glColor3d(c.r, c.g, c.b);
            gl.glDisable(GL.GL_TEXTURE_2D);

            // Warning: pending definition of this behavior...
            drawSurfacesWithoutTexture(gl, mesh, flip);
        }

        //- Drawing control of elements with no surface -------------------
        if ( quality.isPointsSet() ) {
            drawPoints(gl, mesh);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
