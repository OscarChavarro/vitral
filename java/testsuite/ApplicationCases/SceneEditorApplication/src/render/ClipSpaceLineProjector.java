package render;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.linealAlgebra.Vector4Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.media.Calligraphic2DBuffer;

/**
Projects 3D segments to the 2D normalized device coordinates of a camera, as
the geometric stage of a graphics pipeline does before rasterization
[FOLE1992].6: points are transformed by the projection matrix of the camera
(view and view volume), segments are clipped in homogeneous coordinates
against the six planes `-w <= x, y, z <= w` and the perspective division gives
the 2D coordinates.

Clipping in homogeneous space (instead of the canonical perspective volume of
`WireframeRenderer`) works the same for perspective and orthogonal cameras,
and is the same scheme used by `HiddenLineRenderer` to project its lines, so
wireframes and hidden line drawings of a view match.
*/
public class ClipSpaceLineProjector
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

    /**
    @param camera camera whose view is projected; its viewport size must be
    already updated (it gives the aspect ratio)
    */
    public ClipSpaceLineProjector(Camera camera)
    {
        projection = camera.calculateProjectionMatrix();
    }

    /**
    Projects a segment given in world coordinates.
    @param out buffer receiving the projected segment, if any part of it is
    inside the view volume
    @param p0 first end point
    @param p1 second end point
    @return true if a segment was added
    */
    public boolean addSegment(Calligraphic2DBuffer out, Vector3Dd p0, Vector3Dd p1)
    {
        Vector4Dd start = projection.multiply(new Vector4Dd(p0));
        Vector4Dd end = projection.multiply(new Vector4Dd(p1));

        for ( double[] plane : CLIP_PLANES ) {
            double d0 = evaluate(plane, start);
            double d1 = evaluate(plane, end);

            if ( d0 < 0.0 && d1 < 0.0 ) {
                return false;
            }
            if ( d0 < 0.0 || d1 < 0.0 ) {
                double denominator = d0 - d1;
                if ( Math.abs(denominator) < VSDK.EPSILON ) {
                    return false;
                }
                double t = d0 / denominator;
                Vector4Dd intersection = start.multiply(1.0 - t).add(end.multiply(t));
                if ( d0 < 0.0 ) {
                    start = intersection;
                }
                else {
                    end = intersection;
                }
            }
        }

        Vector4Dd ndc0 = start.dividedByW();
        Vector4Dd ndc1 = end.dividedByW();
        out.add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
        return true;
    }

    /**
    Projects a segment given in the coordinates of an object.
    @param out buffer receiving the projected segment
    @param transform transformation from object to world coordinates
    @param p0 first end point
    @param p1 second end point
    @return true if a segment was added
    */
    public boolean addSegment(Calligraphic2DBuffer out, Matrix4x4d transform,
                              Vector3Dd p0, Vector3Dd p1)
    {
        return addSegment(out, transform.multiply(p0), transform.multiply(p1));
    }

    /**
    Projects a point, given in world coordinates, as a degenerate segment.
    @param out buffer receiving the projected point, if it is inside the view
    volume
    @param p the point
    @return true if the point was added
    */
    public boolean addPoint(Calligraphic2DBuffer out, Vector3Dd p)
    {
        Vector4Dd clip = projection.multiply(new Vector4Dd(p));

        for ( double[] plane : CLIP_PLANES ) {
            if ( evaluate(plane, clip) < 0.0 ) {
                return false;
            }
        }
        Vector4Dd ndc = clip.dividedByW();
        out.add2DLine(ndc.x(), ndc.y(), ndc.x(), ndc.y());
        return true;
    }

    private static double evaluate(double[] plane, Vector4Dd point)
    {
        return plane[0] * point.x() + plane[1] * point.y() +
            plane[2] * point.z() + plane[3] * point.w();
    }
}
