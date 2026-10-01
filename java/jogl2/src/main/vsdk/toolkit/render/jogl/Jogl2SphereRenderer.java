//=   of GLUT/GLU utilities.                                                =

package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.common.statistics.RenderingStatistics;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.render.SpherePolyhedralCache;

public class Jogl2SphereRenderer extends Jogl2Renderer {

    /**
    The sphere is converted into a polyhedral bounded solid, shared with the
    renderers of the other technologies (see `SpherePolyhedralCache`), whose
    triangles carry the attributes of the parametric surface. `stacks` rings
    of vertices, poles included, are `stacks - 1` parallels of the solid.
    */
    private static SpherePolyhedralCache.Entry obtainTessellation(Sphere s,
        int slices, int stacks)
    {
        return SpherePolyhedralCache.obtain(s, Math.max(3, slices),
            Math.max(2, stacks - 1));
    }

    /**
    Warning: Change with configured color for vertex normals, tangents and
    binormals
    */
    private static void drawVertexNormals(GL2 gl, SpherePolyhedralCache.Entry entry) {
        float[] p = entry.getPositions();
        float[] n = entry.getNormals();
        float[] t = entry.getTangents();
        float[] b = entry.getBiNormals();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);

        gl.glBegin(GL.GL_LINES);
        for ( int i = 0; i < entry.getVertexCount(); i++ ) {
            int k = 3*i;

            gl.glColor3d(1, 1, 0);
            gl.glVertex3d(p[k], p[k+1], p[k+2]);
            gl.glVertex3d(p[k]+n[k]/10.0, p[k+1]+n[k+1]/10.0, p[k+2]+n[k+2]/10.0);

            gl.glColor3d(0.9, 0.5, 0.5);
            gl.glVertex3d(p[k], p[k+1], p[k+2]);
            gl.glVertex3d(p[k]+t[k]/20.0, p[k+1]+t[k+1]/20.0, p[k+2]+t[k+2]/20.0);

            gl.glColor3d(0.5, 0.9, 0.5);
            gl.glVertex3d(p[k], p[k+1], p[k+2]);
            gl.glVertex3d(p[k]+b[k]/20.0, p[k+1]+b[k+1]/20.0, p[k+2]+b[k+2]/20.0);
        }
        gl.glEnd();
    }

    private static void drawPoints(GL2 gl, SpherePolyhedralCache.Entry entry) {
        float[] p = entry.getPositions();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(6.0f);

        gl.glBegin(GL.GL_POINTS);
        for ( int i = 0; i < entry.getVertexCount(); i++ ) {
            gl.glVertex3d(p[3*i], p[3*i+1], p[3*i+2]);
        }
        gl.glEnd();
    }

    private static void
    drawSphereElements(GL2 gl, SpherePolyhedralCache.Entry entry)
    {
        float[] p = entry.getPositions();
        float[] n = entry.getNormals();
        float[] uv = entry.getUvs();
        int count = entry.getVertexCount();

        RenderingStatistics.accumulatePrimitiveCount(VSDK.TRIANGLE, count/3);

        gl.glBegin(GL.GL_TRIANGLES);
        for ( int i = 0; i < count; i++ ) {
            gl.glTexCoord2d(uv[2*i], uv[2*i+1]);
            gl.glNormal3d(n[3*i], n[3*i+1], n[3*i+2]);
            gl.glVertex3d(p[3*i], p[3*i+1], p[3*i+2]);
        }
        gl.glEnd();
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL2 gl, Sphere s, Camera c, RendererConfiguration q)
    {
        draw(gl, s, c, q, 20, 10);
    }

    public static void draw(GL2 gl, Sphere s, Camera c, RendererConfiguration q,
                            int slices, int stacks)
    {
        if ( q.isSurfacesSet() ) {
            Jogl2GeometryRenderer.prepareSurfaceQuality(gl, q);
            //gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            //gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            //gl.glPolygonOffset(1.0f, 1.0f);
            drawSphereElements(gl, obtainTessellation(s, slices, stacks));
        }
        if ( q.isWiresSet() ) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glShadeModel(GL2.GL_FLAT);

            //gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glLineWidth(1.0f);

            // Warning: Change with configured color for borders
            gl.glColor3d(1, 1, 1);
            gl.glDisable(GL.GL_TEXTURE_2D);

            drawSphereElements(gl, obtainTessellation(s, slices, stacks));
        }

        if ( q.isPointsSet() ) {
            drawPoints(gl, obtainTessellation(s, slices, stacks));
        }
        if ( q.isNormalsSet() ) {
            drawVertexNormals(gl, obtainTessellation(s, slices, stacks));
        }
        if ( q.isBoundingVolumeSet() ) {
            Jogl2GeometryRenderer.drawMinMaxBox(gl, s, q);
        }
        if ( q.isSelectionCornersSet() ) {
            Jogl2GeometryRenderer.drawSelectionCorners(gl, s, q);
        }
    }

}
