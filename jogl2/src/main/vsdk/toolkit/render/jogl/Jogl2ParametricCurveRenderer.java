package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

// JOGL classes
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.surface.ParametricCurve;

public class Jogl2ParametricCurveRenderer extends Jogl2Renderer {
    private static GLU glu;
    private static _JoglPolygonTesselatorRoutines tesselatorProcessor;

    static {
        tesselatorProcessor = null;
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    \todo  Do not turn off lighting here, that's a wrongly supposed used.
    */
    static public void draw(GL2 gl, ParametricCurve curve, 
                            Camera c, RendererConfiguration q,
                            ColorRgb color) {
        int i, j;
        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);

        gl.glColor3d(color.r, color.g, color.b);
        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == ParametricCurve.BREAK ) {
                i++;
                continue;
            }

            // Build a polyline for approximating the [i] curve segment
            ArrayList<Vector3D> polyline = curve.calculatePoints(i, false);

            // Draw the polyline
            gl.glBegin(GL.GL_LINE_STRIP);
            for ( j = 0; j < polyline.size(); j++ ) {
                Vector3D vec = polyline.get(j);

                gl.glVertex3d(vec.x(), vec.y(), vec.z());
            }
            gl.glEnd();
        }

        gl.glPopAttrib();

        if ( q.isBoundingVolumeSet() ) {
            Jogl2GeometryRenderer.drawMinMaxBox(gl, curve, q);
        }
        if ( q.isSelectionCornersSet() ) {
            Jogl2GeometryRenderer.drawSelectionCorners(gl, curve, q);
        }

    }

    static public void draw(GL2 gl, ParametricCurve curve,
                            Camera c, RendererConfiguration q) {
        draw(gl, curve, c, q, q.getWireColor());
    }

    static public void drawControlPointsCurve(GL2 gl, 
                                              ParametricCurve curve) {
        ColorRgb colorLine = new ColorRgb(1, 1, 0);
        ColorRgb colorCenterPoint = new ColorRgb(1, 0, 0);
        ColorRgb colorTangPoint = new ColorRgb(0, 1, 0);
        drawControlPointsCurve(gl, curve, colorLine, colorCenterPoint,
                               colorTangPoint);
    }

    static public void drawControlPointsCurve(GL2 gl,
                                              ParametricCurve curve,
                                              ColorRgb colorLine,
                                              ColorRgb colorCenterPoint,
                                              ColorRgb colorTangPoint) {
        gl.glDisable(GL2.GL_LIGHTING);

        int typeseg = curve.types.get(0);

        if (typeseg == ParametricCurve.BEZIER ||
            typeseg == ParametricCurve.HERMITE) {
            drawTwoControlPoints(gl, curve.points.get(0), 2, colorLine,
                                 colorCenterPoint, colorTangPoint);
        }
        else {
            drawOneControlPoints(gl, curve.points.get(0)[0], colorCenterPoint);
        }
        drawFirstControlPoint(gl, curve.points.get(0)[0], colorCenterPoint);
            //-----------------------------------------------------------------

        for (int i = 1; i < curve.types.size(); i++) {
            typeseg = curve.types.get(i);

            if (typeseg == ParametricCurve.BEZIER ||
                typeseg == ParametricCurve.HERMITE) {
                if (i == curve.types.size() - 1) {
                    drawTwoControlPoints(gl, curve.points.get(i), 1, colorLine,
                                         colorCenterPoint, colorTangPoint);
                }
                else {
                    drawThreeControlPoints(gl, curve.points.get(i), colorLine,
                                           colorCenterPoint, colorTangPoint);
                }
            }
            else {
                drawOneControlPoints(gl, curve.points.get(i)[0], colorCenterPoint);
            }
        }

    }

    static public void drawPoints(GL2 gl, ArrayList<Vector3D[]> pts) {
        gl.glColor3d(1, 0, 0);
        gl.glLineWidth(1);
        for (int i = 0; i < pts.size(); i++) {
            gl.glColor3d(1, 0, 0);
            Vector3D vecarray[] = pts.get(i);
            Vector3D vec = vecarray[0];
            gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(vec.x() + 0.02, vec.y() - 0.02, vec.z());
            gl.glVertex3d(vec.x() - 0.02, vec.y() + 0.02, vec.z());
            gl.glVertex3d(vec.x() + 0.02, vec.y() + 0.02, vec.z());
            gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());

            gl.glEnd();
        }
        gl.glLineWidth(1);
    }

    static public void drawOneControlPoints(GL2 gl, Vector3D vec, ColorRgb color) {
        gl.glColor3d(color.r, color.g, color.b);
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x() + 0.02, vec.y() - 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() + 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());
        gl.glEnd();
    }

    static public void drawFirstControlPoint(GL2 gl, Vector3D vec, ColorRgb color) {
        gl.glColor3d(color.r, color.g, color.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());
        gl.glVertex3d(vec.x() + 0.02, vec.y() - 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() + 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());
        gl.glEnd();
    }

    static public void drawTwoControlPoints(GL2 gl, Vector3D[] ptsB,
                                            int tangPoint, ColorRgb colorLine,
                                            ColorRgb colorCenterPoint,
                                            ColorRgb colorTangPoint) {

        //p1
        gl.glColor3d(colorCenterPoint.r, colorCenterPoint.g, colorCenterPoint.b);
        Vector3D vec = ptsB[0];
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x() + 0.02, vec.y() - 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() + 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());
        gl.glEnd();

        //p2
        gl.glColor3d(colorTangPoint.r, colorTangPoint.g, colorTangPoint.b);
        Vector3D vec2 = ptsB[tangPoint];
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x() + 0.02, vec.y() - 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() + 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());
        gl.glEnd();

        gl.glColor3d(colorLine.r, colorLine.g, colorLine.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x(), vec.y(), vec.z());
        gl.glVertex3d(vec2.x(), vec2.y(), vec2.z());
        gl.glEnd();
    }

    static public void drawThreeControlPoints(GL2 gl, Vector3D[] ptsB,
                                              ColorRgb colorLine,
                                              ColorRgb colorCenterPoint,
                                              ColorRgb colorTangPoint) {

        gl.glColor3d(colorCenterPoint.r, colorCenterPoint.g, colorCenterPoint.b);
        // p1
        Vector3D vec = ptsB[0];
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x() + 0.02, vec.y() - 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() + 0.02, vec.y() + 0.02, vec.z());
        gl.glVertex3d(vec.x() - 0.02, vec.y() - 0.02, vec.z());
        gl.glEnd();

        // p2
        gl.glColor3d(colorTangPoint.r, colorTangPoint.g, colorTangPoint.b);
        Vector3D vec2 = ptsB[1];
        gl.glBegin(GL.GL_LINES);

        gl.glVertex3d(vec2.x() + 0.02, vec2.y() - 0.02, vec2.z());
        gl.glVertex3d(vec2.x() - 0.02, vec2.y() + 0.02, vec2.z());
        gl.glVertex3d(vec2.x() + 0.02, vec2.y() + 0.02, vec2.z());
        gl.glVertex3d(vec2.x() - 0.02, vec2.y() - 0.02, vec2.z());
        gl.glEnd();

        gl.glColor3d(colorLine.r, colorLine.g, colorLine.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x(), vec.y(), vec.z());
        gl.glVertex3d(vec2.x(), vec2.y(), vec2.z());
        gl.glEnd();

        gl.glColor3d(colorTangPoint.r, colorTangPoint.g, colorTangPoint.b);
        vec2 = ptsB[2];
        gl.glBegin(GL.GL_LINES);

        gl.glVertex3d(vec2.x() + 0.02, vec2.y() - 0.02, vec2.z());
        gl.glVertex3d(vec2.x() - 0.02, vec2.y() + 0.02, vec2.z());
        gl.glVertex3d(vec2.x() + 0.02, vec2.y() + 0.02, vec2.z());
        gl.glVertex3d(vec2.x() - 0.02, vec2.y() - 0.02, vec2.z());
        gl.glEnd();

        gl.glColor3d(colorLine.r, colorLine.g, colorLine.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x(), vec.y(), vec.z());
        gl.glVertex3d(vec2.x(), vec2.y(), vec2.z());
        gl.glEnd();
    }

    public static void
    drawTesselatedCurveInterior(GL2 gl, ParametricCurve curve)
    {
        if ( tesselatorProcessor == null ) {
            tesselatorProcessor = 
                new _JoglPolygonTesselatorRoutines(gl, glu);
        }

        GLUtessellator tesselator;
        int i, j;
        int totalNumberOfPoints;
        double list[][];
        Vector3D first;
        boolean beginning;
        int count;

        tesselator = GLU.gluNewTess();
        GLU.gluTessCallback(tesselator,
           GLU.GLU_TESS_VERTEX, tesselatorProcessor);
        GLU.gluTessCallback(tesselator,
           GLU.GLU_TESS_BEGIN, tesselatorProcessor);
        GLU.gluTessCallback(tesselator,
           GLU.GLU_TESS_END, tesselatorProcessor);
        GLU.gluTessCallback(tesselator,
           GLU.GLU_TESS_ERROR, tesselatorProcessor);

        GLU.gluTessBeginPolygon(tesselator, null);

        //-----------------------------------------------------------------
        totalNumberOfPoints = 0;

        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == ParametricCurve.BREAK ) {
                i++;
                continue;
            }
            ArrayList<Vector3D> polyline = curve.calculatePoints(i, false);
            totalNumberOfPoints += polyline.size();
        }

        list = new double[totalNumberOfPoints][3];

        //-----------------------------------------------------------------
        count = 0;

        GLU.gluTessBeginContour(tesselator);
        //gl.glBegin(GL.GL_LINE_LOOP);

        first = new Vector3D();
        beginning = true;
        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == ParametricCurve.BREAK ) {
                i++;
                //gl.glEnd();
                //gl.glBegin(GL.GL_LINE_LOOP);
                GLU.gluTessEndContour(tesselator);
                GLU.gluTessBeginContour(tesselator);
                beginning = true;
                continue;
            }

            // Build a polyline for approximating the [i] curve segment
            ArrayList<Vector3D> polyline = curve.calculatePoints(i, false);

            // Insert into current contour the polyline
            for ( j = 0; j < polyline.size(); j++ ) {
                Vector3D vec = polyline.get(j);
                if ( !beginning ) {
                    Vector3D prev = new Vector3D(list[count-1][0], 
                                                 list[count-1][1],
                                                 list[count-1][2]);
                    if ( VSDK.vectorDistance(vec,  prev) > VSDK.EPSILON &&
                         VSDK.vectorDistance(vec, first) > VSDK.EPSILON ) {
                        list[count][0] = vec.x();
                        list[count][1] = vec.y();
                        list[count][2] = vec.z();
                        GLU.gluTessVertex(tesselator, list[count], 0, list[count]);
                        //gl.glVertex3d(vec.x(), vec.y(), vec.z());
                        count++;
                    }
                  }
                  else {
                    beginning = false;
                    list[count][0] = vec.x();
                    list[count][1] = vec.y();
                    list[count][2] = vec.z();
                    GLU.gluTessVertex(tesselator, list[count], 0, list[count]);
                    //gl.glVertex3d(vec.x(), vec.y(), vec.z());
                    first = new Vector3D(vec.x(), vec.y(), vec.z());
                    count++;
                }
            }
        }
        //gl.glEnd();
        GLU.gluTessEndContour(tesselator);

        GLU.gluTessEndPolygon(tesselator);
        GLU.gluDeleteTess(tesselator);
    }

}
