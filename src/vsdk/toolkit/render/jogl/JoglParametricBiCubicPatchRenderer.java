//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 20 2006 - Gina Chiquillo / David Camello: Original base version =
//= - April 28 2006 - Gina Chiquillo / Oscar Chavarro: quality check        =
//= - May 23 2006 - Gina Chiquillo: added generation of texture coords.     =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.render.jogl.JoglParametricCurveRenderer;
import vsdk.toolkit.render.jogl.JoglGeometryRenderer;
import vsdk.toolkit.render.jogl.JoglRGBAImageRenderer;

public class JoglParametricBiCubicPatchRenderer extends JoglRenderer {

    public static void drawControlPoints(GL gl, ParametricBiCubicPatch p)
    {
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for point
        gl.glColor3d(1, 0, 0);
        gl.glPointSize(6.0f);

        gl.glBegin(gl.GL_POINTS);

        int i; // Integer index in the U direction
        int j; // Integer index in the V direction
	double[][][] points = p.evaluateSurface();

        for ( i = 0; i < points.length; i += (points.length-1) ) {
            for ( j = 0; j < points[0].length; j += (points[0].length-1) ) {
                // Now we draw some lines
                Vector3D p1 = new Vector3D(points[i][j][0],
                                           points[i][j][1],
                                           points[i][j][2]);

                //- Generate GL primitives ----------------------------------------
                gl.glVertex3d(p1.x, p1.y, p1.z);
            }
        }

	gl.glEnd();
    }

    public static void drawNormals(GL gl, double[][][] points,
                                       double textureUSizeFactor,
                                       double textureVSizeFactor, 
                                       double textureURelaviteStart,
                                       double textureVRelativeStart) {
        int i; // Integer index in the U direction
        int j; // Integer index in the V direction
        double sizeDivTu; // Size relation between patch and texture coordinate in U direction
        double sizeDivTv; // Size relation between patch and texture coordinate in V direction

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for vertex normals
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);

        gl.glBegin(gl.GL_LINES);
        for ( i = 0; i < points.length - 1; i++ ) {
            sizeDivTv = (textureVSizeFactor / (points.length-1));
            for ( j = 0; j < points[0].length - 1; j++) {
                sizeDivTu = (textureUSizeFactor /( points[0].length-1));
                // Now we draw some lines
                Vector3D p1 = new Vector3D(points[i][j][0],
                                           points[i][j][1],
                                           points[i][j][2]);
                Vector3D p2 = new Vector3D(points[i + 1][j][0],
                                           points[i + 1][j][1],
                                           points[i + 1][j][2]);
                Vector3D p3 = new Vector3D(points[i + 1][j + 1][0],
                                           points[i + 1][j + 1][1],
                                           points[i + 1][j + 1][2]);
                Vector3D p4 = new Vector3D(points[i][j + 1][0],
                                           points[i][j + 1][1],
                                           points[i][j + 1][2]);

                Vector3D normal = p2.substract(p1).crossProduct(p3.substract(p1));
                normal.normalize();

                //- Generate GL primitives ----------------------------------------
                // First vertex
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p1.x, p1.y, p1.z);
                gl.glVertex3d(p1.x - normal.x/10,
                              p1.y - normal.y/10, 
                              p1.z - normal.z/10);
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
    public static void drawSurfaceGrid(GL gl, double[][][] points,
                                       double textureUSizeFactor,
                                       double textureVSizeFactor, 
                                       double textureURelaviteStart,
                                       double textureVRelativeStart) {
        int i; // Integer index in the U direction
        int j; // Integer index in the V direction
        double sizeDivTu; // Size relation between patch and texture coordinate in U direction
        double sizeDivTv; // Size relation between patch and texture coordinate in V direction

        gl.glBegin(gl.GL_QUADS);
        for ( i = 0; i < points.length - 1; i++ ) {
            sizeDivTv = (textureVSizeFactor / (points.length-1));
            for ( j = 0; j < points[0].length - 1; j++) {
                sizeDivTu = (textureUSizeFactor /( points[0].length-1));
                // Now we draw some lines
                Vector3D p1 = new Vector3D(points[i][j][0],
                                           points[i][j][1],
                                           points[i][j][2]);
                Vector3D p2 = new Vector3D(points[i + 1][j][0],
                                           points[i + 1][j][1],
                                           points[i + 1][j][2]);
                Vector3D p3 = new Vector3D(points[i + 1][j + 1][0],
                                           points[i + 1][j + 1][1],
                                           points[i + 1][j + 1][2]);
                Vector3D p4 = new Vector3D(points[i][j + 1][0],
                                           points[i][j + 1][1],
                                           points[i][j + 1][2]);

                Vector3D normal = p2.substract(p1).crossProduct(p3.substract(p1));
                normal.normalize();

                //- Generate GL primitives ----------------------------------------
                // First vertex
                gl.glTexCoord3d( (j * sizeDivTu) + textureURelaviteStart,
                                 (i * sizeDivTv) + textureVRelativeStart, 0);
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p1.x, p1.y, p1.z);

                // Second vertex
                gl.glTexCoord3d( (j * sizeDivTu) + textureURelaviteStart,
                                 ( (i + 1) * sizeDivTv) + textureVRelativeStart, 0);

                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p2.x, p2.y, p2.z);

                // Third vertex
                gl.glTexCoord3d( ( (j + 1) * sizeDivTu) + textureURelaviteStart,
                                 ( (i + 1) * sizeDivTv) + textureVRelativeStart, 0);
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p3.x, p3.y, p3.z);

                // Fourth vertex
                gl.glTexCoord3d( ( (j + 1) * sizeDivTu) + textureURelaviteStart,
                                 (i * sizeDivTv) + textureVRelativeStart, 0);
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p4.x, p4.y, p4.z);
            }
        }
        gl.glEnd();
    }

    public static void drawControlGrid(GL gl, ParametricBiCubicPatch patch,
                                       Camera c, RendererConfiguration q,
                                       ColorRgb color) {
        // Now we draw the points
        if (patch.type == ParametricCurve.BEZIER) {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glColor3d(color.r, color.g, color.b);
            gl.glLineWidth(1);
            for (int i = 0; i < 4; i++) {
                gl.glBegin(gl.GL_LINE_STRIP);
                for (int j = 0; j < 4; j++) {
                    gl.glVertex3d(patch.Gx_MATRIX.M[i][j], patch.Gy_MATRIX.M[i][j],
                                  patch.Gz_MATRIX.M[i][j]);
                }
                gl.glEnd();
            }
            for (int i = 0; i < 4; i++) {
                gl.glBegin(gl.GL_LINE_STRIP);
                for (int j = 0; j < 4; j++) {
                    gl.glVertex3d(patch.Gx_MATRIX.M[j][i], patch.Gy_MATRIX.M[j][i],
                                  patch.Gz_MATRIX.M[j][i]);
                }
                gl.glEnd();
            }
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

            JoglParametricCurveRenderer.drawPoints(gl, patch.contourCurve.points);

        }
        else if (patch.type == ParametricCurve.HERMITE) {
            // Now we draw the points
            ParametricCurve c1 = new ParametricCurve();
            c1.addPoint(patch.contourCurve.getPoint(0), c1.HERMITE);
            c1.addPoint(patch.contourCurve.getPoint(1), c1.HERMITE);
            c1.addPoint(patch.contourCurve.getPoint(3), c1.HERMITE);

            JoglParametricCurveRenderer.draw(gl, c1, c, q,
                                                  new ColorRgb(1, 0.4, 0.5));
            JoglParametricCurveRenderer.drawControlPointsCurve(gl, c1);

            c1 = new ParametricCurve();
            Vector3D[] v3 = new Vector3D[] {
                patch.contourCurve.getPoint(4)[0], patch.contourCurve.getPoint(4)[2],
                patch.contourCurve.getPoint(4)[1]};
            c1.addPoint(v3, c1.HERMITE);

            v3 = new Vector3D[] {
                patch.contourCurve.getPoint(2)[0], patch.contourCurve.getPoint(2)[2],
                patch.contourCurve.getPoint(2)[1]};
            c1.addPoint(v3, c1.HERMITE);
            v3 = new Vector3D[] {
                patch.contourCurve.getPoint(3)[0], patch.contourCurve.getPoint(3)[2],
                patch.contourCurve.getPoint(3)[1]};
            c1.addPoint(v3, c1.HERMITE);
            JoglParametricCurveRenderer.draw(gl, c1, c, q,
                                                  new ColorRgb(0.5, 0.5, 0.8));
            JoglParametricCurveRenderer.drawControlPointsCurve(gl, c1);

        }
        else {

            // Now we draw the points
            JoglParametricCurveRenderer.draw(gl, patch.contourCurve, c, q,
                                                  new ColorRgb(1, 1, 1));
            JoglParametricCurveRenderer.drawControlPointsCurve(gl,
                                                                    patch.contourCurve);
        }
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.
    */
    public static void draw(GL gl, ParametricBiCubicPatch p, Camera c,
                           RendererConfiguration q) {
        if ( q.isWiresSet() ) {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glDisable(gl.GL_CULL_FACE);
            gl.glShadeModel(gl.GL_FLAT);

            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
            gl.glEnable(gl.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(-0.5f, 0.0f);
            gl.glLineWidth(1.0f);
            drawSurfaceGrid(gl, p.evaluateSurface(), 1, 1, 0, 0);
	}
        if ( q.isSurfacesSet() ) {
            JoglGeometryRenderer.prepareSurfaceQuality(gl, q);
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
            gl.glPolygonOffset(0.0f, 0.0f);
            drawSurfaceGrid(gl, p.evaluateSurface(), 1, 1, 0, 0);
	}
        if ( q.isNormalsSet() ) {
            drawNormals(gl, p.evaluateSurface(), 1, 1, 0, 0);
	}
        if ( q.isPointsSet() ) {
            drawControlPoints(gl, p);
        }
        if (q.isBoundingVolumeSet()) {
            JoglGeometryRenderer.drawMinMaxBox(gl, p, q);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, p, q);
        }
    }

    public static void draw(GL gl, ParametricBiCubicPatch p, Camera c,
                           RendererConfiguration q,
                           int textureUSizeFactor, int tilling_y,
                           double textureURelaviteStart,
                           double textureVRelativeStart) {
        if ( q.isWiresSet() ) {
            gl.glDisable(gl.GL_LIGHTING);
            gl.glDisable(gl.GL_CULL_FACE);
            gl.glShadeModel(gl.GL_FLAT);

            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
            gl.glEnable(gl.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(-0.5f, 0.0f);
            gl.glLineWidth(1.0f);
            drawSurfaceGrid(gl, p.evaluateSurface(), textureUSizeFactor, tilling_y,
                        textureURelaviteStart, textureVRelativeStart);
	}
        if ( q.isSurfacesSet() ) {
            JoglGeometryRenderer.prepareSurfaceQuality(gl, q);
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
            gl.glPolygonOffset(0.0f, 0.0f);
            drawSurfaceGrid(gl, p.evaluateSurface(), textureUSizeFactor, tilling_y,
                        textureURelaviteStart, textureVRelativeStart);
	}
        if ( q.isNormalsSet() ) {
            drawNormals(gl, p.evaluateSurface(), textureUSizeFactor, tilling_y,
                        textureURelaviteStart, textureVRelativeStart);
	}
        if ( q.isPointsSet() ) {
            drawControlPoints(gl, p);
        }
        if (q.isBoundingVolumeSet()) {
            JoglGeometryRenderer.drawMinMaxBox(gl, p, q);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, p, q);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
