package vsdk.toolkit.render.jogl;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.surface.ParametricCurve;
import vsdk.toolkit.environment.geometry.surface.ParametricBiCubicPatch;

public class Jogl2ParametricBiCubicPatchRenderer extends Jogl2Renderer {

    public static void drawControlPoints(GL2 gl, ParametricBiCubicPatch p)
    {
        //-----------------------------------------------------------------
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(6.0f);

        //-----------------------------------------------------------------
        int i; // Integer index in the U direction
        int j; // Integer index in the V direction
        Vector3D pos = new Vector3D();
        int n = p.getApproximationSteps()+1;
        double s, t;
        double ds, dt;

        ds = 1 / ((double)n-1);
        dt = 1 / ((double)n-1);

        //-----------------------------------------------------------------
        gl.glBegin(GL.GL_POINTS);

        for ( i = 0; i < n; i += (n-1) ) {
            s = ((double)i)*ds;
            for ( j = 0; j < n; j += (n-1) ) {
                t = ((double)j)*dt;
                p.evaluate(pos, s, t);
                gl.glVertex3d(pos.x(), pos.y(), pos.z());
            }
        }

        gl.glEnd();
    }

    public static void drawNormals(GL2 gl, ParametricBiCubicPatch p,
                                   double textureUSizeFactor,
                                   double textureVSizeFactor, 
                                   double textureURelaviteStart,
                                   double textureVRelativeStart) {
        int i; // Integer index in the U direction
        int j; // Integer index in the V direction
        double s, t;
        double ds, dt;
        double sizeDivTu; // Size relation between patch and texture coordinate in U direction
        double sizeDivTv; // Size relation between patch and texture coordinate in V direction
        int n;

        n = p.getApproximationSteps()+1;
        ds = 1 / ((double)n-1);
        dt = 1 / ((double)n-1);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);

        double reductionFactor = 20;
        gl.glBegin(GL.GL_LINES);
        for ( i = 0; i < n; i++ ) {
            s = ((double)i)*ds;
            sizeDivTv = (textureVSizeFactor / (n-1));
            for ( j = 0; j < n; j++ ) {
                t = ((double)j)*dt;
                sizeDivTu = (textureUSizeFactor / (n-1));

                Vector3D p1 = new Vector3D();
                Vector3D normal;

                p.evaluate(p1, s, t);

                // Normal
                normal = p.evaluateNormal(s, t);

                gl.glColor3d(1, 1, 0);
                gl.glVertex3d(p1.x(), p1.y(), p1.z());
                gl.glVertex3d(p1.x() + normal.x()/reductionFactor,
                              p1.y() + normal.y()/reductionFactor, 
                              p1.z() + normal.z()/reductionFactor);

                // Tangent
                normal = p.evaluateTangent(s, t);

                gl.glColor3d(0.9, 0.5, 0.5);
                gl.glVertex3d(p1.x(), p1.y(), p1.z());
                gl.glVertex3d(p1.x() + normal.x()/(reductionFactor*2),
                              p1.y() + normal.y()/(reductionFactor*2), 
                              p1.z() + normal.z()/(reductionFactor*2));
                // Binormal
                normal = p.evaluateBinormal(s, t);

                gl.glColor3d(0.5, 0.9, 0.5);
                gl.glVertex3d(p1.x(), p1.y(), p1.z());
                gl.glVertex3d(p1.x() + normal.x()/(reductionFactor*2),
                              p1.y() + normal.y()/(reductionFactor*2), 
                              p1.z() + normal.z()/(reductionFactor*2));
            }
        }
        gl.glEnd();
    }

    /**
    This method generates the OpenGL/JOGL graphics primitives needed
    to render the given parametric bicubic patch's position points.

    The code asigns texture coordinates in the (u, v) space for the
    patch. Note that when textureUSizeFactor = 1, textureUSizeFactor = 1,
    textureURelaviteStart = 0 and textureVRelaviteStart = 0, the
    texture space matches the patch's parametric space, giving as a result
    a texture covering the whole patch. Changing those 4 parameters
    results in a different texture coverage.

    Note that the `points` matrix is of size points[NumberU][NumberV][3],
    and can be understood as a matrix of [NumberU][NumberV] points
    with coordinates (x, y, z).    
    */
    public static void drawSurfaceGrid(GL2 gl,
                                       ParametricBiCubicPatch p,
                                       double textureUSizeFactor,
                                       double textureVSizeFactor, 
                                       double textureURelaviteStart,
                                       double textureVRelativeStart,
                                       RendererConfiguration q) {
        int i; // Integer index in the U direction
        int j; // Integer index in the V direction
        double s, t;
        double ds, dt;
        double sizeDivTu; // Size relation between patch and texture coordinate in U direction
        double sizeDivTv; // Size relation between patch and texture coordinate in V direction
        int n = p.getApproximationSteps()+1;

        ds = 1 / ((double)n-1);
        dt = 1 / ((double)n-1);

        if ( q.getShadingType() == RendererConfiguration.SHADING_TYPE_FLAT ) {
            gl.glShadeModel(GL2.GL_FLAT);
        }
        else {
            gl.glShadeModel(GL2.GL_SMOOTH);
        }

        for ( i = 0; i < n - 1; i++ ) {
            s = ((double)i)*ds;
            sizeDivTv = (textureVSizeFactor / (n-1));
            gl.glBegin(GL2.GL_QUAD_STRIP);
            for ( j = 0; j < n; j++) {
                t = ((double)j)*dt;
                sizeDivTu = (textureUSizeFactor / (n-1));
                // Now we draw some lines
                Vector3D p1 = new Vector3D();
                Vector3D p2 = new Vector3D();

                p.evaluate(p1, s, t);
                p.evaluate(p2, s+ds, t);

                Vector3D n1 = p.evaluateNormal(s, t);
                Vector3D n2 = p.evaluateNormal(s+ds, t);

                //- Generate GL primitives ------------------------------------
                // First vertex
                gl.glTexCoord3d( (i * sizeDivTu) + textureURelaviteStart,
                                 (j * sizeDivTv) + textureVRelativeStart, 0);
                gl.glNormal3d(n1.x(), n1.y(), n1.z());
                gl.glVertex3d(p1.x(), p1.y(), p1.z());

                // Second vertex
                gl.glTexCoord3d( ((i+1) * sizeDivTu) + textureURelaviteStart,
                                   (j * sizeDivTv) + textureVRelativeStart, 0);
                gl.glNormal3d(n2.x(), n2.y(), n2.z());
                gl.glVertex3d(p2.x(), p2.y(), p2.z());
            }
            gl.glEnd();
        }
    }

    public static void drawControlGrid(GL2 gl, ParametricBiCubicPatch patch,
                                       Camera c, RendererConfiguration q,
                                       ColorRgb color) {
        // Now we draw the points
        if ( patch.type == ParametricCurve.BEZIER ) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3d(color.r, color.g, color.b);
            gl.glLineWidth(1);
            for (int i = 0; i < 4; i++) {
                gl.glBegin(GL.GL_LINE_STRIP);
                for (int j = 0; j < 4; j++) {
                    gl.glVertex3d(patch.Gx_MATRIX.get(i, j), patch.Gy_MATRIX.get(i, j),
                                  patch.Gz_MATRIX.get(i, j));
                }
                gl.glEnd();
            }
            for (int i = 0; i < 4; i++) {
                gl.glBegin(GL.GL_LINE_STRIP);
                for (int j = 0; j < 4; j++) {
                    gl.glVertex3d(patch.Gx_MATRIX.get(j, i), patch.Gy_MATRIX.get(j, i),
                                  patch.Gz_MATRIX.get(j, i));
                }
                gl.glEnd();
            }

            Jogl2ParametricCurveRenderer.drawPoints(gl, patch.contourCurve.points);

        }
        else if (patch.type == ParametricCurve.HERMITE) {
            // Now we draw the points
            ParametricCurve c1 = new ParametricCurve();
            c1.addPoint(patch.contourCurve.getPoint(0), ParametricCurve.HERMITE);
            c1.addPoint(patch.contourCurve.getPoint(1), ParametricCurve.HERMITE);
            c1.addPoint(patch.contourCurve.getPoint(3), ParametricCurve.HERMITE);

            Jogl2ParametricCurveRenderer.draw(gl, c1, c, q,
                                                  new ColorRgb(1, 0.4, 0.5));
            Jogl2ParametricCurveRenderer.drawControlPointsCurve(gl, c1);

            c1 = new ParametricCurve();
            Vector3D[] v3 = new Vector3D[] {
                patch.contourCurve.getPoint(4)[0], patch.contourCurve.getPoint(4)[2],
                patch.contourCurve.getPoint(4)[1]};
            c1.addPoint(v3, ParametricCurve.HERMITE);

            v3 = new Vector3D[] {
                patch.contourCurve.getPoint(2)[0], patch.contourCurve.getPoint(2)[2],
                patch.contourCurve.getPoint(2)[1]};
            c1.addPoint(v3, ParametricCurve.HERMITE);
            v3 = new Vector3D[] {
                patch.contourCurve.getPoint(3)[0], patch.contourCurve.getPoint(3)[2],
                patch.contourCurve.getPoint(3)[1]};
            c1.addPoint(v3, ParametricCurve.HERMITE);
            Jogl2ParametricCurveRenderer.draw(gl, c1, c, q,
                                                  new ColorRgb(0.5, 0.5, 0.8));
            Jogl2ParametricCurveRenderer.drawControlPointsCurve(gl, c1);

        }
        else {

            // Now we draw the points
            Jogl2ParametricCurveRenderer.draw(gl, patch.contourCurve, c, q,
                                                  new ColorRgb(1, 1, 1));
            Jogl2ParametricCurveRenderer.drawControlPointsCurve(gl,
                                                                    patch.contourCurve);
        }
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL2 gl, ParametricBiCubicPatch p, Camera c,
                           RendererConfiguration q) {
        if ( q.isSurfacesSet() ) {
            Jogl2GeometryRenderer.prepareSurfaceQuality(gl, q);

            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            // gl.glPolygonOffset(1.0f, 1.0f);

            if ( q.isTextureSet() ) {
                gl.glEnable(GL.GL_TEXTURE_2D);
            }
            else {
                gl.glDisable(GL.GL_TEXTURE_2D);
            }
            drawSurfaceGrid(gl, p, 1, 1, 0, 0, q);
        }
        if ( q.isWiresSet() ) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glShadeModel(GL2.GL_FLAT);

            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glLineWidth(1.0f);

            ColorRgb co = q.getWireColor();
            gl.glColor3d(co.r, co.g, co.b);
            gl.glDisable(GL.GL_TEXTURE_2D);

            drawSurfaceGrid(gl, p, 1, 1, 0, 0, q);
        }
        if ( q.isNormalsSet() ) {
            drawNormals(gl, p, 1, 1, 0, 0);
        }
        if ( q.isPointsSet() ) {
            drawControlPoints(gl, p);
        }
        if (q.isBoundingVolumeSet()) {
            Jogl2GeometryRenderer.drawMinMaxBox(gl, p, q);
        }
        if ( q.isSelectionCornersSet() ) {
            Jogl2GeometryRenderer.drawSelectionCorners(gl, p, q);
        }
    }

    public static void draw(GL2 gl, ParametricBiCubicPatch p, Camera c,
                           RendererConfiguration q,
                           int textureUSizeFactor, int tilling_y,
                           double textureURelaviteStart,
                           double textureVRelativeStart) {
        if ( q.isWiresSet() ) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glShadeModel(GL2.GL_FLAT);

            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glLineWidth(1.0f);
            drawSurfaceGrid(gl, p, textureUSizeFactor, tilling_y,
                            textureURelaviteStart, textureVRelativeStart, q);
        }
        if ( q.isSurfacesSet() ) {
            Jogl2GeometryRenderer.prepareSurfaceQuality(gl, q);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_FILL);
            //gl.glPolygonOffset(1.0f, 1.0f);
            drawSurfaceGrid(gl, p, textureUSizeFactor, tilling_y,
                            textureURelaviteStart, textureVRelativeStart, q);
        }
        if ( q.isNormalsSet() ) {
            drawNormals(gl, p, textureUSizeFactor, tilling_y,
                        textureURelaviteStart, textureVRelativeStart);
        }
        if ( q.isPointsSet() ) {
            drawControlPoints(gl, p);
        }
        if (q.isBoundingVolumeSet()) {
            Jogl2GeometryRenderer.drawMinMaxBox(gl, p, q);
        }
        if ( q.isSelectionCornersSet() ) {
            Jogl2GeometryRenderer.drawSelectionCorners(gl, p, q);
        }
    }
}
