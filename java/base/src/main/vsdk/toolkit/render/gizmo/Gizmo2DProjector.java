package vsdk.toolkit.render.gizmo;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;

/**
Geometric stage of the 2D gizmo pipeline [FOLE1992].6: takes the world space
primitives the gizmos build for the OpenGL renderers (triangle strips, fans,
segments, see `GizmoSolidTessellator`), transforms them with the projection
matrix of a camera, clips them in homogeneous coordinates against the six
planes `-w <= x, y, z <= w` (Sutherland-Hodgman for polygons) and maps the
result to the pixels of the viewport of the camera, adding them to a
`Gizmo2DDrawList`.

Pixels have the origin at the upper left corner of the viewport and grow
downwards; the viewport size is the one of the camera (see
`Camera.updateViewportResize`).
*/
public class Gizmo2DProjector
{
    private static final double[][] CLIP_PLANES = {
        { 1.0, 0.0, 0.0, 1.0 },
        { -1.0, 0.0, 0.0, 1.0 },
        { 0.0, 1.0, 0.0, 1.0 },
        { 0.0, -1.0, 0.0, 1.0 },
        { 0.0, 0.0, 1.0, 1.0 },
        { 0.0, 0.0, -1.0, 1.0 }
    };

    private final Matrix4x4d projection;
    private final double width;
    private final double height;
    private final Gizmo2DDrawList out;

    /**
    @param camera camera viewing the gizmo, with its viewport size updated
    @param out list receiving the projected primitives
    */
    public Gizmo2DProjector(Camera camera, Gizmo2DDrawList out)
    {
        this.projection = camera.calculateProjectionMatrix();
        this.width = camera.getViewportXSize();
        this.height = camera.getViewportYSize();
        this.out = out;
    }

    public Gizmo2DDrawList getDrawList()
    {
        return out;
    }

    //= Polygons ==========================================================

    /**
    Adds a simple polygon (convex or not), given by its vertices in order.
    */
    public void addPolygon(Vector3Dd[] vertices, ColorRgb color, double opacity)
    {
        List<Vector4Dd> polygon = new ArrayList<Vector4Dd>(vertices.length + 6);

        for ( Vector3Dd v : vertices ) {
            polygon.add(projection.multiply(new Vector4Dd(v)));
        }
        for ( double[] plane : CLIP_PLANES ) {
            polygon = clip(polygon, plane);
            if ( polygon.size() < 3 ) {
                return;
            }
        }

        int n = polygon.size();
        double[] x = new double[n];
        double[] y = new double[n];
        double depth = 0;

        for ( int i = 0; i < n; i++ ) {
            Vector4Dd ndc = polygon.get(i).dividedByW();
            x[i] = toPixelX(ndc.x());
            y[i] = toPixelY(ndc.y());
            depth += ndc.z();
        }
        out.addPolygon(x, y, depth / n, color, opacity);
    }

    /**
    Adds a triangle strip, as one quad for each pair of consecutive edges (a
    strip of 4 vertices is one quad), which are drawn with fewer seams than
    its triangles.
    */
    public void addStrip(Vector3Dd[] strip, ColorRgb color, double opacity)
    {
        for ( int i = 0; i + 3 < strip.length; i += 2 ) {
            addPolygon(new Vector3Dd[] {strip[i], strip[i + 1], strip[i + 3],
                strip[i + 2]}, color, opacity);
        }
        if ( strip.length % 2 == 1 && strip.length >= 3 ) {
            int last = strip.length - 1;
            addPolygon(new Vector3Dd[] {strip[last - 2], strip[last - 1],
                strip[last]}, color, opacity);
        }
    }

    /**
    Adds a triangle fan as its triangles (the first vertex is the center or
    apex of the fan).
    */
    public void addFanTriangles(Vector3Dd[] fan, ColorRgb color, double opacity)
    {
        for ( int i = 1; i + 1 < fan.length; i++ ) {
            addPolygon(new Vector3Dd[] {fan[0], fan[i], fan[i + 1]}, color, opacity);
        }
    }

    /**
    Adds a planar triangle fan as one polygon (its center followed by its
    perimeter), so a translucent fan is not darker along its inner edges.
    */
    public void addFanAsPolygon(Vector3Dd[] fan, ColorRgb color, double opacity)
    {
        if ( fan.length >= 3 ) {
            addPolygon(fan, color, opacity);
        }
    }

    /**
    Adds a list of independent triangles (3 vertices each).
    */
    public void addTriangles(Vector3Dd[] triangles, ColorRgb color, double opacity)
    {
        for ( int i = 0; i + 2 < triangles.length; i += 3 ) {
            addPolygon(new Vector3Dd[] {triangles[i], triangles[i + 1],
                triangles[i + 2]}, color, opacity);
        }
    }

    //= Lines and points ==================================================

    /**
    Adds a segment.
    @param depthBias added to its depth (negative is towards the viewer)
    */
    public void addSegment(Vector3Dd p0, Vector3Dd p1, ColorRgb color,
                           double lineWidth, double depthBias)
    {
        Vector4Dd start = projection.multiply(new Vector4Dd(p0));
        Vector4Dd end = projection.multiply(new Vector4Dd(p1));

        for ( double[] plane : CLIP_PLANES ) {
            double d0 = evaluate(plane, start);
            double d1 = evaluate(plane, end);

            if ( d0 < 0.0 && d1 < 0.0 ) {
                return;
            }
            if ( d0 < 0.0 || d1 < 0.0 ) {
                double denominator = d0 - d1;
                if ( Math.abs(denominator) < VSDK.EPSILON ) {
                    return;
                }
                Vector4Dd intersection = interpolate(start, end, d0 / denominator);
                if ( d0 < 0.0 ) {
                    start = intersection;
                }
                else {
                    end = intersection;
                }
            }
        }
        Vector4Dd a = start.dividedByW();
        Vector4Dd b = end.dividedByW();
        out.addLine(toPixelX(a.x()), toPixelY(a.y()), toPixelX(b.x()),
            toPixelY(b.y()), (a.z() + b.z()) / 2 + depthBias, color, lineWidth);
    }

    /**
    @param p point in world space
    @return its pixel {x, y} in the viewport, or null if it is behind the
    camera or outside of the depth range of the view volume (it may be
    outside the sides of the viewport)
    */
    public double[] projectPoint(Vector3Dd p)
    {
        Vector4Dd clip = projection.multiply(new Vector4Dd(p));

        if ( clip.w() <= VSDK.EPSILON || clip.z() < -clip.w() || clip.z() > clip.w() ) {
            return null;
        }
        Vector4Dd ndc = clip.dividedByW();
        return new double[] {toPixelX(ndc.x()), toPixelY(ndc.y())};
    }

    //=====================================================================

    private double toPixelX(double ndcX)
    {
        return (ndcX + 1.0) / 2.0 * width;
    }

    private double toPixelY(double ndcY)
    {
        return (1.0 - ndcY) / 2.0 * height;
    }

    /**
    Sutherland-Hodgman clipping of a polygon against one plane of the
    homogeneous view volume.
    */
    private static List<Vector4Dd> clip(List<Vector4Dd> polygon, double[] plane)
    {
        List<Vector4Dd> result = new ArrayList<Vector4Dd>(polygon.size() + 2);
        int n = polygon.size();

        for ( int i = 0; i < n; i++ ) {
            Vector4Dd current = polygon.get(i);
            Vector4Dd next = polygon.get((i + 1) % n);
            double dc = evaluate(plane, current);
            double dn = evaluate(plane, next);

            if ( dc >= 0.0 ) {
                result.add(current);
            }
            if ( (dc >= 0.0) != (dn >= 0.0) ) {
                result.add(interpolate(current, next, dc / (dc - dn)));
            }
        }
        return result;
    }

    private static Vector4Dd interpolate(Vector4Dd a, Vector4Dd b, double t)
    {
        return a.multiply(1.0 - t).add(b.multiply(t));
    }

    private static double evaluate(double[] plane, Vector4Dd point)
    {
        return plane[0] * point.x() + plane[1] * point.y() +
            plane[2] * point.z() + plane[3] * point.w();
    }
}
