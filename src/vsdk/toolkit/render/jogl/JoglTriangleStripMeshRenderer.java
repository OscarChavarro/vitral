//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 2 2007 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;

// VitralSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Vertex;
import vsdk.toolkit.environment.geometry.TriangleStripMesh;

public class JoglTriangleStripMeshRenderer extends JoglRenderer {

    private static void drawPoints(GL gl, TriangleStripMesh mesh) {
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(2.0f);

        gl.glBegin(gl.GL_POINTS);
        for ( int i = 0; i < mesh.getVertexes().length; i++ ) {
            gl.glVertex3d(mesh.getVertexAt(i).getPosition().x,
                          mesh.getVertexAt(i).getPosition().y,
                          mesh.getVertexAt(i).getPosition().z);
        }
        gl.glEnd();
    }

    private static void drawSurfacesWithoutTexture(GL gl, 
                                     TriangleStripMesh mesh, boolean flipNormals) {
        //-----------------------------------------------------------------
        // Warning: Shoult this be here? or not ...
        // Just in case using external texture...
        gl.glTexParameteri(gl.GL_TEXTURE_2D,
            gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
        gl.glTexParameteri(gl.GL_TEXTURE_2D,
            gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
        gl.glTexParameterf(gl.GL_TEXTURE_2D,
            gl.GL_TEXTURE_WRAP_S, gl.GL_REPEAT);
        gl.glTexParameterf(gl.GL_TEXTURE_2D,
            gl.GL_TEXTURE_WRAP_T, gl.GL_REPEAT);
        gl.glTexEnvf(gl.GL_TEXTURE_ENV,
            gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);

        //-----------------------------------------------------------------
        int strips[][] = mesh.getStrips();
        Vertex vertexes[] = mesh.getVertexes();
        Vertex v;
        int strip[];
        int i, j;

        for ( i = 0; i < strips.length; i++ ) {
            strip = strips[i];
            gl.glBegin(gl.GL_TRIANGLE_STRIP);
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

    @todo Handle PHONG and BUMPMAPPING cases, via vertex/program shaders
    */
    public static void
    draw(GL gl, TriangleStripMesh mesh, RendererConfiguration quality, boolean flip)
    {
        //-----------------------------------------------------------------
        gl.glEnable(gl.GL_NORMALIZE);

        if ( quality.isSurfacesSet() ) {
            JoglGeometryRenderer.prepareSurfaceQuality(gl, quality);
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
            gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(1.0f, 1.0f);

            drawSurfacesWithoutTexture(gl, mesh, flip);
        }
        if ( quality.isWiresSet() ) {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glDisable(gl.GL_CULL_FACE);
            gl.glShadeModel(gl.GL_FLAT);

            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
            gl.glDisable(gl.GL_POLYGON_OFFSET_LINE);
            gl.glLineWidth(1.0f);

            // Warning: Change with configured color for borders
            ColorRgb c = quality.getWireColor();
            gl.glColor3d(c.r, c.g, c.b);
            gl.glDisable(gl.GL_TEXTURE_2D);

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
