//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 20 2006 - Gina Chiquillo / David Camello: Original base version =
//= - April 28 2005 - Gina Chiquillo / Oscar Chavarro: quality check        =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import java.util.ArrayList;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.environment.geometry.ParametricCubicCurve;

public class JoglParametricCubicCurveRenderer {

  static public void drawCurve(GL gl,
                               ParametricCubicCurve curve, ColorRgb color) {
    gl.glDisable(GL.GL_LIGHTING);
    gl.glLoadIdentity();
    for (int i = 1; i < curve.types.size(); i++) {

      /* Draw a polyline */
      ArrayList pol = curve.calculatePoints(i);
      gl.glColor3d(color.r, color.g, color.b);
      gl.glBegin(GL.GL_LINE_STRIP);
      for (int j = 0; j < pol.size(); j++) {
        Vector3D vec = (Vector3D) pol.get(j);
        gl.glVertex3d(vec.x, vec.y, vec.z);
      }
      gl.glEnd();
    }

  }

  static public void drawCurve(GL gl,
                               ParametricCubicCurve curve) {
    drawCurve(gl, curve, new ColorRgb(1, 1, 1));
  }

  static public void drawControlPointsCurve(GL gl, ParametricCubicCurve curve) {
    ColorRgb colorLine = new ColorRgb(1, 1, 0);
    ColorRgb colorCenterPoint = new ColorRgb(1, 0, 0);
    ColorRgb colorTangPoint = new ColorRgb(0, 1, 0);
    drawControlPointsCurve(gl, curve, colorLine, colorCenterPoint,
                           colorTangPoint);
  }

  static public void drawControlPointsCurve(GL gl,
                                            ParametricCubicCurve curve,
                                            ColorRgb colorLine,
                                            ColorRgb colorCenterPoint,
                                            ColorRgb colorTangPoint) {
    gl.glDisable(GL.GL_LIGHTING);
    gl.glLoadIdentity();

    int typeseg = curve.types.get(0);

    if (typeseg == ParametricCubicCurve.BEZIER ||
        typeseg == ParametricCubicCurve.HERMITE) {
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

      if (typeseg == ParametricCubicCurve.BEZIER ||
          typeseg == ParametricCubicCurve.HERMITE) {
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

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
