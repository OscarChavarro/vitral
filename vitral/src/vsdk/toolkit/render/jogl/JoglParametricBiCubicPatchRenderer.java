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

    public static void drawSurfaceGrid(GL gl, double[][][] points,
                                       float tilingU, float tilingV, 
                                       float inicialTextCoorU,
                                       float inicialTextCoorV) {

        gl.glBegin(gl.GL_QUADS);
        for (int i = 0; i < points.length - 1; i++) {
            float sizeDivTv = (float) (tilingV / (points.length-1));
            for (int j = 0; j < points[0].length - 1; j++) {
                float sizeDivTu = (float) (tilingU /( points[0].length-1));
                // Now we draw some lines
                Vector3D p1 = new Vector3D(points[i][j][0], points[i][j][1],
                                           points[i][j][2]);
                Vector3D p2 = new Vector3D(points[i + 1][j][0], points[i + 1][j][1],
                                           points[i + 1][j][2]);
                Vector3D p3 = new Vector3D(points[i + 1][j + 1][0],
                                           points[i + 1][j + 1][1],
                                           points[i + 1][j + 1][2]);

                Vector3D normal = p2.substract(p1).crossProduct(p3.substract(p1));
                normal = normal.multiply( -1);
                normal.normalize();
                gl.glTexCoord3f( (j * sizeDivTu) + inicialTextCoorU,
                                 (i * sizeDivTv) + inicialTextCoorV, 0);
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p1.x, p1.y, p1.z);
                gl.glTexCoord3f( (j * sizeDivTu) + inicialTextCoorU,
                                 ( (i + 1) * sizeDivTv) + inicialTextCoorV, 0);

                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p2.x, p2.y, p2.z);
                gl.glTexCoord3f( ( (j + 1) * sizeDivTu) + inicialTextCoorU,
                                 ( (i + 1) * sizeDivTv) + inicialTextCoorV, 0);
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(p3.x, p3.y, p3.z);
                gl.glTexCoord3f( ( (j + 1) * sizeDivTu) + inicialTextCoorU,
                                 (i * sizeDivTv) + inicialTextCoorV, 0);
                gl.glNormal3d(normal.x, normal.y, normal.z);
                gl.glVertex3d(points[i][j + 1][0], points[i][j + 1][1],
                              points[i][j + 1][2]);

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
        drawSurfaceGrid(gl, p.evaluateSurface(), 1, 1, 0, 0);
        if (q.isBoundingVolumeSet()) {
            JoglGeometryRenderer.drawMinMaxBox(gl, p, q);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, p, q);
        }
    }

    public static void draw(GL gl, ParametricBiCubicPatch p, Camera c,
                           RendererConfiguration q, int tilingU, int tilling_y,
                           float inicialTextCoorU,
                           float inicialTextCoorV) {

        drawSurfaceGrid(gl, p.evaluateSurface(), tilingU, tilling_y,
                        inicialTextCoorU, inicialTextCoorV);
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
