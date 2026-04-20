//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Encapsulates preflight classification and no-intersection resolution for
Boolean set operations. This keeps containment/touching policy separate from
the main intersection/splitting pipeline.
*/
final class _PolyhedralBoundedSolidSetNonIntersectingClassifier
    extends _PolyhedralBoundedSolidOperator
{
    private static final int NO_INT_RELATION_DISJOINT = 0;
    private static final int NO_INT_RELATION_TOUCHING = 1;
    private static final int NO_INT_RELATION_A_IN_B = 2;
    private static final int NO_INT_RELATION_B_IN_A = 3;

    private _PolyhedralBoundedSolidSetNonIntersectingClassifier()
    {
    }

    static boolean runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int aInB = classifySolidAgainstSolid(inSolidA, inSolidB);
        int bInA = classifySolidAgainstSolid(inSolidB, inSolidA);
        int relation = classifyNoIntersectionRelation(aInB, bInA);

        if ( relation != NO_INT_RELATION_TOUCHING ) {
            return false;
        }

        if ( hasConfirmedInteriorOverlap(inSolidA, inSolidB) ) {
            return false;
        }

        if ( hasProperEdgeFaceIntersection(inSolidA, inSolidB) ) {
            return false;
        }
        if ( hasProperEdgeFaceIntersection(inSolidB, inSolidA) ) {
            return false;
        }

        return true;
    }

    static PolyhedralBoundedSolid runSetOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int aInB = classifySolidAgainstSolid(inSolidA, inSolidB);
        int bInA = classifySolidAgainstSolid(inSolidB, inSolidA);
        int relation = classifyNoIntersectionRelation(aInB, bInA);

        if ( op == INTERSECTION ) {
            if ( relation == NO_INT_RELATION_A_IN_B ) {
                outRes.merge(inSolidA);
            }
            else if ( relation == NO_INT_RELATION_B_IN_A ) {
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if ( op == UNION ) {
            if ( relation == NO_INT_RELATION_A_IN_B ) {
                outRes.merge(inSolidB);
            }
            else if ( relation == NO_INT_RELATION_B_IN_A ) {
                outRes.merge(inSolidA);
            }
            else {
                outRes.merge(inSolidA);
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if ( relation == NO_INT_RELATION_A_IN_B ) {
            return outRes;
        }
        if ( relation == NO_INT_RELATION_B_IN_A ) {
            outRes.merge(inSolidA);
            inSolidB.revert();
            outRes.merge(inSolidB);
            outRes.compactIds();
            return outRes;
        }
        outRes.merge(inSolidA);
        return outRes;
    }

    private static int compareToZero(double value)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static int pointInFace(_PolyhedralBoundedSolidFace face,
        Vector3D point)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .pointInFace(face, point);
    }

    private static int classifyPointAgainstSolid(PolyhedralBoundedSolid solid,
        Vector3D point)
    {
        int i, j;
        _PolyhedralBoundedSolidFace face;
        double eps = numericContext.bigEpsilon();
        int insideVotes = 0;
        int outsideVotes = 0;

        if ( solid == null || solid.polygonsList.size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            face = solid.polygonsList.get(i);
            if ( face.containingPlane == null ) {
                continue;
            }
            if ( Math.abs(face.containingPlane.pointDistance(point)) <= eps ) {
                if ( face.testPointInside(point, eps) != Geometry.OUTSIDE ) {
                    return Geometry.LIMIT;
                }
            }
        }

        Vector3D[] dirs = {
            new Vector3D(1.0, 0.371, 0.137),
            new Vector3D(0.193, 1.0, 0.417),
            new Vector3D(0.217, 0.173, 1.0)
        };

        for ( j = 0; j < dirs.length; j++ ) {
            int hits = 0;
            boolean ambiguous = false;
            ArrayList<Double> distances = new ArrayList<Double>();
            Ray ray = new Ray(point, dirs[j]);

            for ( i = 0; i < solid.polygonsList.size(); i++ ) {
                face = solid.polygonsList.get(i);
                if ( face.containingPlane == null ) {
                    ambiguous = true;
                    break;
                }
                Ray rayHit = new Ray(ray);
                Ray hit = face.containingPlane.doIntersection(rayHit);
                if ( hit == null ) {
                    continue;
                }
                if ( hit.t() <= eps ) {
                    continue;
                }

                Vector3D pi = hit.origin().add(
                    hit.direction().multiply(hit.t()));
                int status = face.testPointInside(pi, eps);
                if ( status == Geometry.LIMIT ) {
                    ambiguous = true;
                    break;
                }
                if ( status == Geometry.INSIDE ) {
                    boolean duplicated = false;
                    int k;
                    for ( k = 0; k < distances.size(); k++ ) {
                        if ( Math.abs(distances.get(k).doubleValue() - hit.t())
                             <= eps ) {
                            duplicated = true;
                            break;
                        }
                    }
                    if ( !duplicated ) {
                        distances.add(Double.valueOf(hit.t()));
                        hits++;
                    }
                }
            }

            if ( !ambiguous ) {
                if ( (hits % 2) == 1 ) {
                    insideVotes++;
                }
                else {
                    outsideVotes++;
                }
            }
        }

        if ( insideVotes > outsideVotes ) {
            return Geometry.INSIDE;
        }
        if ( outsideVotes > insideVotes ) {
            return Geometry.OUTSIDE;
        }
        return Geometry.LIMIT;
    }

    private static double[] overlappingBounds(PolyhedralBoundedSolid solidA,
                                              PolyhedralBoundedSolid solidB)
    {
        double[] a = solidA.getMinMax();
        double[] b = solidB.getMinMax();

        return new double[] {
            Math.max(a[0], b[0]),
            Math.max(a[1], b[1]),
            Math.max(a[2], b[2]),
            Math.min(a[3], b[3]),
            Math.min(a[4], b[4]),
            Math.min(a[5], b[5])
        };
    }

    private static boolean hasPositiveOverlapVolume(double[] bounds)
    {
        double eps = numericContext.bigEpsilon();

        return bounds[3] - bounds[0] > eps &&
            bounds[4] - bounds[1] > eps &&
            bounds[5] - bounds[2] > eps;
    }

    private static double vertexCoordinate(
        _PolyhedralBoundedSolidVertex vertex,
        int axis)
    {
        if ( axis == 0 ) {
            return vertex.position.x();
        }
        if ( axis == 1 ) {
            return vertex.position.y();
        }
        return vertex.position.z();
    }

    private static void appendInteriorVertexCoordinates(
        ArrayList<Double> coords,
        PolyhedralBoundedSolid solid,
        int axis,
        double min,
        double max,
        double eps)
    {
        int i;

        if ( solid == null ) {
            return;
        }

        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            double c = vertexCoordinate(solid.verticesList.get(i), axis);
            if ( c > min + eps && c < max - eps ) {
                coords.add(Double.valueOf(c));
            }
        }
    }

    private static void appendUniqueInteriorSample(
        ArrayList<Double> samples,
        double value,
        double min,
        double max,
        double eps)
    {
        int i;

        if ( value <= min + eps || value >= max - eps ) {
            return;
        }

        for ( i = 0; i < samples.size(); i++ ) {
            if ( Math.abs(samples.get(i).doubleValue() - value) <= eps ) {
                return;
            }
        }
        samples.add(Double.valueOf(value));
    }

    private static double[] sampleCoordinates(double min, double max,
                                              PolyhedralBoundedSolid solidA,
                                              PolyhedralBoundedSolid solidB,
                                              int axis)
    {
        ArrayList<Double> coords;
        ArrayList<Double> samples;
        ArrayList<Double> uniqueCoords;
        double eps = numericContext.bigEpsilon();
        double center = (min + max) / 2.0;
        double quarter = min + (max - min) / 4.0;
        double threeQuarters = max - (max - min) / 4.0;
        int i;

        coords = new ArrayList<Double>();
        coords.add(Double.valueOf(min));
        coords.add(Double.valueOf(max));
        appendInteriorVertexCoordinates(coords, solidA, axis, min, max, eps);
        appendInteriorVertexCoordinates(coords, solidB, axis, min, max, eps);
        Collections.sort(coords);

        uniqueCoords = new ArrayList<Double>();
        for ( i = 0; i < coords.size(); i++ ) {
            double c = coords.get(i).doubleValue();
            if ( uniqueCoords.isEmpty() ||
                 Math.abs(uniqueCoords.get(uniqueCoords.size()-1).doubleValue() - c) > eps ) {
                uniqueCoords.add(Double.valueOf(c));
            }
        }

        samples = new ArrayList<Double>();
        appendUniqueInteriorSample(samples, quarter, min, max, eps);
        appendUniqueInteriorSample(samples, center, min, max, eps);
        appendUniqueInteriorSample(samples, threeQuarters, min, max, eps);

        for ( i = 0; i < uniqueCoords.size() - 1; i++ ) {
            double left = uniqueCoords.get(i).doubleValue();
            double right = uniqueCoords.get(i+1).doubleValue();
            if ( right - left > eps ) {
                appendUniqueInteriorSample(samples, (left + right) / 2.0,
                    min, max, eps);
            }
        }

        Collections.sort(samples);
        if ( samples.isEmpty() ) {
            return new double[] { center };
        }

        double[] result = new double[samples.size()];
        for ( i = 0; i < samples.size(); i++ ) {
            result[i] = samples.get(i).doubleValue();
        }
        return result;
    }

    private static boolean hasConfirmedInteriorOverlap(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        double[] bounds;
        double[] xs;
        double[] ys;
        double[] zs;
        int i;
        int j;
        int k;

        if ( solidA == null || solidB == null ) {
            return false;
        }

        bounds = overlappingBounds(solidA, solidB);
        if ( !hasPositiveOverlapVolume(bounds) ) {
            return false;
        }

        xs = sampleCoordinates(bounds[0], bounds[3], solidA, solidB, 0);
        ys = sampleCoordinates(bounds[1], bounds[4], solidA, solidB, 1);
        zs = sampleCoordinates(bounds[2], bounds[5], solidA, solidB, 2);

        for ( i = 0; i < xs.length; i++ ) {
            for ( j = 0; j < ys.length; j++ ) {
                for ( k = 0; k < zs.length; k++ ) {
                    Vector3D sample = new Vector3D(xs[i], ys[j], zs[k]);
                    if ( classifyPointAgainstSolid(solidA, sample) ==
                         Geometry.INSIDE &&
                         classifyPointAgainstSolid(solidB, sample) ==
                         Geometry.INSIDE ) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static int classifySolidAgainstSolid(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        int i;
        boolean sawLimit = false;
        boolean sawOutside = false;

        if ( solidA == null || solidA.verticesList.size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solidA.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solidA.verticesList.get(i);
            int status = classifyPointAgainstSolid(solidB, v.position);
            if ( status == Geometry.INSIDE ) {
                return Geometry.INSIDE;
            }
            if ( status == Geometry.LIMIT ) {
                sawLimit = true;
            }
            else {
                sawOutside = true;
            }
        }

        if ( sawLimit ) {
            return Geometry.LIMIT;
        }
        if ( sawOutside ) {
            return Geometry.OUTSIDE;
        }
        return Geometry.OUTSIDE;
    }

    private static int classifyNoIntersectionRelation(int aInB, int bInA)
    {
        if ( aInB == Geometry.INSIDE ) {
            return NO_INT_RELATION_A_IN_B;
        }
        if ( bInA == Geometry.INSIDE ) {
            return NO_INT_RELATION_B_IN_A;
        }
        if ( aInB == Geometry.LIMIT || bInA == Geometry.LIMIT ) {
            return NO_INT_RELATION_TOUCHING;
        }
        return NO_INT_RELATION_DISJOINT;
    }

    private static boolean hasProperEdgeFaceIntersection(
        PolyhedralBoundedSolid current,
        PolyhedralBoundedSolid other)
    {
        int i, j;
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidFace face;
        _PolyhedralBoundedSolidVertex v1, v2;
        double d1, d2, d3, t;
        int s1, s2;
        Vector3D p;

        if ( current == null || other == null ) {
            return false;
        }

        for ( i = 0; i < current.edgesList.size(); i++ ) {
            edge = current.edgesList.get(i);
            if ( edge == null || edge.rightHalf == null ||
                 edge.leftHalf == null ) {
                continue;
            }
            v1 = edge.rightHalf.startingVertex;
            v2 = edge.leftHalf.startingVertex;
            if ( v1 == null || v2 == null ) {
                continue;
            }

            for ( j = 0; j < other.polygonsList.size(); j++ ) {
                face = other.polygonsList.get(j);
                if ( face == null || face.containingPlane == null ) {
                    continue;
                }

                d1 = face.containingPlane.pointDistance(v1.position);
                d2 = face.containingPlane.pointDistance(v2.position);
                s1 = compareToZero(d1);
                s2 = compareToZero(d2);

                if ( !((s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1)) ) {
                    continue;
                }

                t = d1 / (d1 - d2);
                p = v1.position.add(
                    v2.position.subtract(v1.position).multiply(t));
                d3 = face.containingPlane.pointDistance(p);
                if ( compareToZero(d3) != 0 ) {
                    continue;
                }

                if ( pointInFace(face, p) == Geometry.INSIDE ) {
                    return true;
                }
            }
        }
        return false;
    }
}
