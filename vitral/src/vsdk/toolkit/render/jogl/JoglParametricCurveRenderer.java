//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 20 2006 - Gina Chiquillo / David Camello: Original base version =
//= - April 28 2005 - Gina Chiquillo / Oscar Chavarro: quality check        =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallback;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.ParametricCurve;

class _CurveTesselatorProcessorRoutines extends JoglRenderer 
implements GLUtessellatorCallback
{
    private GL gl;
    private GLU glu;
    public _CurveTesselatorProcessorRoutines(GL gl, GLU glu) {
        this.gl = gl;
        this.glu = glu;
    }

    public void begin(int type) {
        gl.glBegin(type);
    }

    public void end() {
        gl.glEnd();
    }

    public void vertex(Object vertexData) {
        double[] pointer;
        if ( vertexData instanceof double[] ) {
            pointer = (double[]) vertexData;
            gl.glVertex3dv(pointer, 0);
        }

    }

    public void vertexData(Object vertexData, Object polygonData) {
    }

    /* combineCallback is used to create a new vertex when edges intersect.
    coordinate location is trivial to calculate, but weight[4] may be
    used to average color, normal, or texture coordinate data. In this
    program, color is weighted. */
    public void combine(double[] coords, Object[] data, 
                        float[] weight, Object[] outData) {
        double[] vertex = new double[6];
        int i;

        vertex[0] = coords[0];
        vertex[1] = coords[1];
        vertex[2] = coords[2];
        for (i = 3; i < 6/* 7OutOfBounds from C! */; i++)
            vertex[i] = weight[0]
                * ((double[]) data[0])[i] + weight[1]
                * ((double[]) data[1])[i] + weight[2]
                * ((double[]) data[2])[i] + weight[3]
                * ((double[]) data[3])[i];
        outData[0] = vertex;
    }

    public void combineData(double[] coords, Object[] data, //
                            float[] weight, Object[] outData, Object polygonData) {
    }

    public void error(int errnum) {
        String estring = null;

        try {
            estring = glu.gluErrorString(errnum);
        }
        catch ( Exception e ) {
        estring = "" + e;
        }

        System.err.println("Tessellation Error: " + estring);
        //System.exit(0);
    }

    public void beginData(int type, Object polygonData) {
    }

    public void endData(Object polygonData) {
    }

    public void edgeFlag(boolean boundaryEdge) {
    }

    public void edgeFlagData(boolean boundaryEdge, Object polygonData) {
    }

    public void errorData(int errnum, Object polygonData) {
    }
}

public class JoglParametricCurveRenderer extends JoglRenderer {
    private static GLU glu;
    private static _CurveTesselatorProcessorRoutines tesselatorProcessor;

    static {
        glu = null;
        tesselatorProcessor = null;
    }

    /**
    Generate OpenGL/JOGL primitives needed for the rendering of recieved
    Geometry object.

    @todo Do not turn off lighting here, that's a wrongly supposed used.
    */
    static public void draw(GL gl, ParametricCurve curve, 
                            Camera c, QualitySelection q,
                            ColorRgb color) {
        int i;
        gl.glPushAttrib(gl.GL_LIGHTING_BIT);
        gl.glDisable(GL.GL_LIGHTING);

        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == curve.BREAK ) {
                i++;
                continue;
            }

            // Build a polyline for approximating the [i] curve segment
            ArrayList polyline = curve.calculatePoints(i, false);

            gl.glColor3d(color.r, color.g, color.b);

            // Draw the polyline
            gl.glBegin(GL.GL_LINE_STRIP);
            for ( int j = 0; j < polyline.size(); j++ ) {
                Vector3D vec = (Vector3D) polyline.get(j);
                gl.glVertex3d(vec.x, vec.y, vec.z);
            }
            gl.glEnd();
        }

        gl.glPopAttrib();

        if ( q.isBoundingVolumeSet() ) {
            JoglGeometryRenderer.drawMinMaxBox(gl, curve, q);
        }
        if ( q.isSelectionCornersSet() ) {
            JoglGeometryRenderer.drawSelectionCorners(gl, curve, q);
        }

    }

    static public void draw(GL gl, ParametricCurve curve,
                            Camera c, QualitySelection q) {
        draw(gl, curve, c, q, new ColorRgb(1, 1, 1));
    }

    static public void drawControlPointsCurve(GL gl, 
                                              ParametricCurve curve) {
        ColorRgb colorLine = new ColorRgb(1, 1, 0);
        ColorRgb colorCenterPoint = new ColorRgb(1, 0, 0);
        ColorRgb colorTangPoint = new ColorRgb(0, 1, 0);
        drawControlPointsCurve(gl, curve, colorLine, colorCenterPoint,
                               colorTangPoint);
    }

    static public void drawControlPointsCurve(GL gl,
                                              ParametricCurve curve,
                                              ColorRgb colorLine,
                                              ColorRgb colorCenterPoint,
                                              ColorRgb colorTangPoint) {
        gl.glDisable(GL.GL_LIGHTING);

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

    static public void drawPoints(GL gl, ArrayList pts) {
        gl.glColor3d(1, 0, 0);
        gl.glLineWidth(1);
        for (int i = 0; i < pts.size(); i++) {
            gl.glColor3d(1, 0, 0);
            Vector3D vec = ( (Vector3D[]) pts.get(i))[0];
            gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(vec.x + 0.02, vec.y - 0.02, vec.z);
            gl.glVertex3d(vec.x - 0.02, vec.y + 0.02, vec.z);
            gl.glVertex3d(vec.x + 0.02, vec.y + 0.02, vec.z);
            gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);

            gl.glEnd();
        }
        gl.glLineWidth(1);
    }

    static public void drawOneControlPoints(GL gl, Vector3D vec, ColorRgb color) {
        gl.glColor3d(color.r, color.g, color.b);
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x + 0.02, vec.y - 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x + 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);
        gl.glEnd();
    }

    static public void drawFirstControlPoint(GL gl, Vector3D vec, ColorRgb color) {
        gl.glColor3d(color.r, color.g, color.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);
        gl.glVertex3d(vec.x + 0.02, vec.y - 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x + 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);
        gl.glEnd();
    }

    static public void drawTwoControlPoints(GL gl, Vector3D[] ptsB,
                                            int tangPoint, ColorRgb colorLine,
                                            ColorRgb colorCenterPoint,
                                            ColorRgb colorTangPoint) {

        //p1
        gl.glColor3d(colorCenterPoint.r, colorCenterPoint.g, colorCenterPoint.b);
        Vector3D vec = ptsB[0];
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x + 0.02, vec.y - 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x + 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);
        gl.glEnd();

        //p2
        gl.glColor3d(colorTangPoint.r, colorTangPoint.g, colorTangPoint.b);
        Vector3D vec2 = ptsB[tangPoint];
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x + 0.02, vec.y - 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x + 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);
        gl.glEnd();

        gl.glColor3d(colorLine.r, colorLine.g, colorLine.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x, vec.y, vec.z);
        gl.glVertex3d(vec2.x, vec2.y, vec2.z);
        gl.glEnd();
    }

    static public void drawThreeControlPoints(GL gl, Vector3D[] ptsB,
                                              ColorRgb colorLine,
                                              ColorRgb colorCenterPoint,
                                              ColorRgb colorTangPoint) {

        gl.glColor3d(colorCenterPoint.r, colorCenterPoint.g, colorCenterPoint.b);
        // p1
        Vector3D vec = ptsB[0];
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(vec.x + 0.02, vec.y - 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x + 0.02, vec.y + 0.02, vec.z);
        gl.glVertex3d(vec.x - 0.02, vec.y - 0.02, vec.z);
        gl.glEnd();

        // p2
        gl.glColor3d(colorTangPoint.r, colorTangPoint.g, colorTangPoint.b);
        Vector3D vec2 = ptsB[1];
        gl.glBegin(GL.GL_LINES);

        gl.glVertex3d(vec2.x + 0.02, vec2.y - 0.02, vec2.z);
        gl.glVertex3d(vec2.x - 0.02, vec2.y + 0.02, vec2.z);
        gl.glVertex3d(vec2.x + 0.02, vec2.y + 0.02, vec2.z);
        gl.glVertex3d(vec2.x - 0.02, vec2.y - 0.02, vec2.z);
        gl.glEnd();

        gl.glColor3d(colorLine.r, colorLine.g, colorLine.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x, vec.y, vec.z);
        gl.glVertex3d(vec2.x, vec2.y, vec2.z);
        gl.glEnd();

        gl.glColor3d(colorTangPoint.r, colorTangPoint.g, colorTangPoint.b);
        vec2 = ptsB[2];
        gl.glBegin(GL.GL_LINES);

        gl.glVertex3d(vec2.x + 0.02, vec2.y - 0.02, vec2.z);
        gl.glVertex3d(vec2.x - 0.02, vec2.y + 0.02, vec2.z);
        gl.glVertex3d(vec2.x + 0.02, vec2.y + 0.02, vec2.z);
        gl.glVertex3d(vec2.x - 0.02, vec2.y - 0.02, vec2.z);
        gl.glEnd();

        gl.glColor3d(colorLine.r, colorLine.g, colorLine.b);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(vec.x, vec.y, vec.z);
        gl.glVertex3d(vec2.x, vec2.y, vec2.z);
        gl.glEnd();
    }

    public static void
    drawTesselatedCurveInterior(GL gl, ParametricCurve curve)
    {
        if ( tesselatorProcessor == null ) {
            glu = new GLU();
            tesselatorProcessor = 
                new _CurveTesselatorProcessorRoutines(gl, glu);
        }

        GLUtessellator tesselator;

        tesselator = glu.gluNewTess();
        glu.gluTessCallback(tesselator, glu.GLU_TESS_VERTEX, tesselatorProcessor);
        glu.gluTessCallback(tesselator, glu.GLU_TESS_BEGIN, tesselatorProcessor);
        glu.gluTessCallback(tesselator, glu.GLU_TESS_END, tesselatorProcessor);
        glu.gluTessCallback(tesselator, glu.GLU_TESS_ERROR, tesselatorProcessor);

        glu.gluTessBeginPolygon(tesselator, null);

        int i;

        //-----------------------------------------------------------------
        int totalNumberOfPoints = 0;
        double list[][];

        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == curve.BREAK ) {
                i++;
                continue;
            }
            ArrayList polyline = curve.calculatePoints(i, false);
            totalNumberOfPoints += polyline.size();
        }

        list = new double[totalNumberOfPoints][3];

        //-----------------------------------------------------------------
        int count = 0;

        glu.gluTessBeginContour(tesselator);
        //gl.glBegin(gl.GL_LINE_LOOP);

        Vector3D first = new Vector3D();
        boolean beginning = true;
        for ( i = 1; i < curve.types.size(); i++ ) {
            if ( curve.types.get(i).intValue() == curve.BREAK ) {
                i++;
                //gl.glEnd();
                //gl.glBegin(gl.GL_LINE_LOOP);
                glu.gluTessEndContour(tesselator);
                glu.gluTessBeginContour(tesselator);
                beginning = true;
                continue;
            }

            // Build a polyline for approximating the [i] curve segment
            ArrayList polyline = curve.calculatePoints(i, false);

            // Insert into current contour the polyline
            for ( int j = 0; j < polyline.size(); j++ ) {
                Vector3D vec = (Vector3D) polyline.get(j);
                if ( !beginning ) {
                    Vector3D prev = new Vector3D(list[count-1][0], 
                                                 list[count-1][1],
                                                 list[count-1][2]);
                    if ( VSDK.vectorDistance(vec,  prev) > VSDK.EPSILON &&
                         VSDK.vectorDistance(vec, first) > VSDK.EPSILON ) {
                        list[count][0] = vec.x;
                        list[count][1] = vec.y;
                        list[count][2] = vec.z;
                        glu.gluTessVertex(tesselator, list[count], 0, list[count]);
                        //gl.glVertex3d(vec.x, vec.y, vec.z);
                        count++;
                    }
                  }
                  else {
                    beginning = false;
                    list[count][0] = vec.x;
                    list[count][1] = vec.y;
                    list[count][2] = vec.z;
                    glu.gluTessVertex(tesselator, list[count], 0, list[count]);
                    //gl.glVertex3d(vec.x, vec.y, vec.z);
                    first = new Vector3D(vec.x, vec.y, vec.z);
                    count++;
                }
            }
        }
        //gl.glEnd();
        glu.gluTessEndContour(tesselator);

        glu.gluTessEndPolygon(tesselator);
        glu.gluDeleteTess(tesselator);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
