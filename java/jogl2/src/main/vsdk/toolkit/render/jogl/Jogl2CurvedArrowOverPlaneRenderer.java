package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;

public class Jogl2CurvedArrowOverPlaneRenderer extends Jogl2Renderer
{
    private static final int SEGMENTS = 10;
    private static final double DEFAULT_CURVE_OFFSET_PERCENT = 0.1;

    private static double curveFactor(
        double axisDistance,
        double fullLength,
        double curveOffsetPercent)
    {
        double percent = axisDistance / fullLength;
        return curveOffsetPercent * fullLength * Math.sin(percent * Math.PI);
    }

    private static double curveSlope(
        double axisDistance,
        double fullLength,
        double curveOffsetPercent)
    {
        double percent = axisDistance / fullLength;
        return curveOffsetPercent * Math.PI * Math.cos(percent * Math.PI);
    }

    public static void draw(GL2 gl,
        Vector3Dd startPoint,
        Vector3Dd endPoint,
        InfinitePlane plane,
        double invert,
        double sizePercent)
    {
        draw(gl, startPoint, endPoint, plane, invert, sizePercent,
            DEFAULT_CURVE_OFFSET_PERCENT);
    }

    public static void draw(GL2 gl,
        Vector3Dd startPoint,
        Vector3Dd endPoint,
        InfinitePlane plane,
        double invert,
        double sizePercent,
        double curveOffsetPercent)
    {
        if ( gl == null || startPoint == null || endPoint == null || plane == null ) {
            return;
        }

        Vector3Dd planeNormal = plane.getNormal();
        if ( planeNormal == null ) {
            return;
        }

        Vector3Dd u;
        Vector3Dd v;
        double fullLength;
        double factor;
        double t;
        int i;
        Vector3Dd p;

        v = endPoint.subtract(startPoint);
        fullLength = v.length();
        if ( fullLength <= 1e-12 ) {
            return;
        }
        sizePercent = Math.min(1.0, Math.max(VSDK.EPSILON, sizePercent));
        curveOffsetPercent = Math.min(1.0, Math.max(0.0, curveOffsetPercent));
        factor = fullLength * sizePercent;
        v = v.normalized();
        u = v.crossProduct(planeNormal);
        double delta = factor / SEGMENTS;

        gl.glPushMatrix();

        gl.glBegin(GL.GL_LINES);
            for ( i = 0, t = 0; i < SEGMENTS; i++, t += delta ) {
                p = startPoint.add(v.multiply(t).add(u.multiply(invert *
                    curveFactor(t, fullLength, curveOffsetPercent))));
                gl.glVertex3d(p.x(), p.y(), p.z());
                p = startPoint.add(v.multiply(t + delta).add(u.multiply(invert *
                    curveFactor(t + delta, fullLength, curveOffsetPercent))));
                gl.glVertex3d(p.x(), p.y(), p.z());
            }
        gl.glEnd();

        Vector3Dd tip = startPoint.add(v.multiply(factor)).add(u.multiply(invert *
            curveFactor(factor, fullLength, curveOffsetPercent)));
        Vector3Dd tangent = v.add(u.multiply(invert *
            curveSlope(factor, fullLength, curveOffsetPercent)));
        if ( tangent.length() <= 1e-12 ) {
            tangent = v;
        }
        tangent = tangent.normalized();
        Vector3Dd headSide = tangent.crossProduct(planeNormal);
        if ( headSide.length() <= 1e-12 ) {
            headSide = u;
        }
        headSide = headSide.normalized();
        double headLength = factor * 0.1;
        Vector3Dd headBase = tip.subtract(tangent.multiply(headLength));
        double headHalfWidth = fullLength * curveOffsetPercent * 0.5;

        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(tip.x(), tip.y(), tip.z());
            p = headBase.add(headSide.multiply(headHalfWidth));
            gl.glVertex3d(p.x(), p.y(), p.z());

            gl.glVertex3d(tip.x(), tip.y(), tip.z());
            p = headBase.add(headSide.multiply(-headHalfWidth));
            gl.glVertex3d(p.x(), p.y(), p.z());
        gl.glEnd();

        gl.glPopMatrix();
    }
}
