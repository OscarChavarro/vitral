package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

public class _PolyhedralBoundedSolidFaceValidator
{
    private _PolyhedralBoundedSolidFaceValidator()
    {
    }

    public static boolean isSurfaceDegenerate(
        _PolyhedralBoundedSolidFace face)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forFace(face);
        ArrayList<Vector3Dd> points =
            PolyhedralBoundedSolidGeometricValidator.extractPointsFromFace(face);
        if ( points == null ||
             !PolyhedralBoundedSolidGeometricValidator
                 .validateFacePointsAreCoplanar(points, numericContext) ) {
            return true;
        }
        if ( faceArea(face) <= numericContext.bigEpsilon() *
             numericContext.bigEpsilon() ) {
            return true;
        }
        return hasCloseNonAdjacentEdges(face, numericContext);
    }

    public static double faceArea(_PolyhedralBoundedSolidFace face)
    {
        double area = 0.0;
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge start = he;
            Vector3Dd vectorArea = new Vector3Dd();
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                vectorArea = vectorArea.add(
                    he.startingVertex.position.crossProduct(
                        next.startingVertex.position));
                he = next;
            } while ( he != start );
            area += 0.5 * vectorArea.length();
        }
        return area;
    }

    public static boolean hasCloseNonAdjacentEdges(
        _PolyhedralBoundedSolidFace face,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        ArrayList<FaceSegment> segments = collectFaceSegments(face);
        double tolerance = Math.max(numericContext.bigEpsilon() * 10.0,
            numericContext.modelScale() * 1.0e-5);
        for ( int i = 0; i < segments.size(); i++ ) {
            for ( int j = i + 1; j < segments.size(); j++ ) {
                FaceSegment a = segments.get(i);
                FaceSegment b = segments.get(j);
                if ( a.sharesEndpointWith(b, numericContext.bigEpsilon()) ) {
                    continue;
                }
                if ( segmentDistance(a.start, a.end, b.start, b.end) <=
                     tolerance ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<FaceSegment> collectFaceSegments(
        _PolyhedralBoundedSolidFace face)
    {
        ArrayList<FaceSegment> segments = new ArrayList<FaceSegment>();
        for ( int i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge start = he;
            do {
                _PolyhedralBoundedSolidHalfEdge next = he.next();
                if ( next == null ) {
                    break;
                }
                segments.add(new FaceSegment(he.startingVertex.position,
                    next.startingVertex.position));
                he = next;
            } while ( he != start );
        }
        return segments;
    }

    private static double segmentDistance(Vector3Dd p1, Vector3Dd q1,
                                          Vector3Dd p2, Vector3Dd q2)
    {
        Vector3Dd d1 = q1.subtract(p1);
        Vector3Dd d2 = q2.subtract(p2);
        Vector3Dd r = p1.subtract(p2);
        double a = d1.dotProduct(d1);
        double e = d2.dotProduct(d2);
        double f = d2.dotProduct(r);
        double s;
        double t;

        if ( a <= VSDK.EPSILON && e <= VSDK.EPSILON ) {
            return p1.subtract(p2).length();
        }
        if ( a <= VSDK.EPSILON ) {
            s = 0.0;
            t = clamp(f / e, 0.0, 1.0);
        }
        else {
            double c = d1.dotProduct(r);
            if ( e <= VSDK.EPSILON ) {
                t = 0.0;
                s = clamp(-c / a, 0.0, 1.0);
            }
            else {
                double b = d1.dotProduct(d2);
                double denom = a * e - b * b;
                if ( denom != 0.0 ) {
                    s = clamp((b * f - c * e) / denom, 0.0, 1.0);
                }
                else {
                    s = 0.0;
                }
                t = (b * s + f) / e;
                if ( t < 0.0 ) {
                    t = 0.0;
                    s = clamp(-c / a, 0.0, 1.0);
                }
                else if ( t > 1.0 ) {
                    t = 1.0;
                    s = clamp((b - c) / a, 0.0, 1.0);
                }
            }
        }

        return p1.add(d1.multiply(s)).subtract(p2.add(d2.multiply(t))).length();
    }

    private static double clamp(double value, double min, double max)
    {
        if ( value < min ) {
            return min;
        }
        if ( value > max ) {
            return max;
        }
        return value;
    }

    private static final class FaceSegment
    {
        private final Vector3Dd start;
        private final Vector3Dd end;

        private FaceSegment(Vector3Dd start, Vector3Dd end)
        {
            this.start = start;
            this.end = end;
        }

        private boolean sharesEndpointWith(FaceSegment other, double tolerance)
        {
            return start.subtract(other.start).length() <= tolerance ||
                start.subtract(other.end).length() <= tolerance ||
                end.subtract(other.start).length() <= tolerance ||
                end.subtract(other.end).length() <= tolerance;
        }
    }
}
