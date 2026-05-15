//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.environment.geometry.elements.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector2Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
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

    /**
    §7.3.1.D preflight — detects the case where one solid is strictly
    contained in the other (A⊂B or B⊂A) without any real edge/face
    intersection and without partial coplanar overlap. When this holds,
    the regular intersect/classify/connect pipeline produces ∅ (no
    intersections found → empty boundary), even though set-theoretically
    the result must be one of the operands per Mäntylä Ch. 15.1.

    <p>Together with {@link #runTouchingOnlyPreflightCase}, this routes
    every "no real intersection" geometry to {@link #runSetOpNoIntersectionCase},
    which has the table-15.1 dispatch for all four cases (disjoint,
    touching, A⊂B, B⊂A).</p>
    */
    static boolean runContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int aInB = classifySolidAgainstSolid(inSolidA, inSolidB);
        int bInA = classifySolidAgainstSolid(inSolidB, inSolidA);
        int relation = classifyNoIntersectionRelation(aInB, bInA);

        if ( relation != NO_INT_RELATION_A_IN_B &&
             relation != NO_INT_RELATION_B_IN_A ) {
            return false;
        }

        // Note: we deliberately do NOT bail out on
        // hasPartialCoplanarFaceAreaOverlap here. The absorption identity
        // {@code A ∪ (A∩B)} produces a contained operand that can be
        // tangent to A on coplanar boundary faces (the second step of
        // §15.1 absorption holds whether or not the inner solid is
        // strictly interior). Proper edge/face intersections still
        // disqualify (those need the regular pipeline), but tangent
        // coplanar overlap is part of the degenerate dispatch.
        if ( hasProperEdgeFaceIntersection(inSolidA, inSolidB) ) {
            return false;
        }
        if ( hasProperEdgeFaceIntersection(inSolidB, inSolidA) ) {
            return false;
        }

        return true;
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

        if ( hasPartialCoplanarFaceAreaOverlap(inSolidA, inSolidB) ) {
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
            PolyhedralBoundedSolidTopologyEditing.compactIds(outRes);
            return outRes;
        }
        outRes.merge(inSolidA);
        return outRes;
    }

    static PolyhedralBoundedSolid runPartialCoplanarFaceAreaCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op)
    {
        ArrayList<ArrayList<Vector3Dd>> contactPolygons;
        int i;

        if ( op == UNION ) {
            return null;
        }

        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        if ( hasConfirmedInteriorOverlap(inSolidA, inSolidB) ||
             hasProperEdgeFaceIntersection(inSolidA, inSolidB) ||
             hasProperEdgeFaceIntersection(inSolidB, inSolidA) ) {
            return null;
        }

        contactPolygons = partialCoplanarFaceAreaOverlapPolygons(inSolidA,
            inSolidB);
        if ( contactPolygons.isEmpty() ) {
            return null;
        }

        if ( op == SUBTRACT ) {
            outRes.merge(inSolidA);
            return outRes;
        }

        for ( i = 0; i < contactPolygons.size(); i++ ) {
            PolyhedralBoundedSolid lamina =
                createLaminaFromPolygon(contactPolygons.get(i));
            if ( lamina.getPolygonsList().size() > 0 ) {
                outRes.merge(lamina);
            }
        }

        return outRes;
    }

    private static int compareToZero(double value)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static int pointInFace(_PolyhedralBoundedSolidFace face,
        Vector3Dd point)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .pointInFace(face, point);
    }

    private static int classifyPointAgainstSolid(PolyhedralBoundedSolid solid,
        Vector3Dd point)
    {
        int i;
        int j;
        _PolyhedralBoundedSolidFace face;
        double eps = numericContext.bigEpsilon();
        int insideVotes = 0;
        int outsideVotes = 0;

        if ( solid == null || solid.getPolygonsList().size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            face = solid.getPolygonsList().get(i);
            if ( face.getContainingPlane() == null ) {
                continue;
            }
            if ( Math.abs(face.getContainingPlane().pointDistance(point)) <= eps ) {
                if ( face.testPointInside(point, eps) != Geometry.OUTSIDE ) {
                    return Geometry.LIMIT;
                }
            }
        }

        Vector3Dd[] dirs = {
            new Vector3Dd(1.0, 0.371, 0.137),
            new Vector3Dd(0.193, 1.0, 0.417),
            new Vector3Dd(0.217, 0.173, 1.0)
        };

        for ( j = 0; j < dirs.length; j++ ) {
            int hits = 0;
            boolean ambiguous = false;
            ArrayList<Double> distances = new ArrayList<Double>();
            Ray ray = new Ray(point, dirs[j]);

            for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
                face = solid.getPolygonsList().get(i);
                if ( face.getContainingPlane() == null ) {
                    ambiguous = true;
                    break;
                }
                Ray rayHit = new Ray(ray);
                Ray hit = face.getContainingPlane().doIntersection(rayHit);
                if ( hit == null ) {
                    continue;
                }
                if ( hit.t() <= eps ) {
                    continue;
                }

                Vector3Dd pi = hit.origin().add(
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

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            double c = vertexCoordinate(solid.getVerticesList().get(i), axis);
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
                    Vector3Dd sample = new Vector3Dd(xs[i], ys[j], zs[k]);
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

        if ( solidA == null || solidA.getVerticesList().size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solidA.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solidA.getVerticesList().get(i);
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
        int i;
        int j;
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidFace face;
        _PolyhedralBoundedSolidVertex v1;
        _PolyhedralBoundedSolidVertex v2;
        double d1;
        double d2;
        double d3;
        double t;
        int s1;
        int s2;
        Vector3Dd p;

        if ( current == null || other == null ) {
            return false;
        }

        for ( i = 0; i < current.getEdgesList().size(); i++ ) {
            edge = current.getEdgesList().get(i);
            if ( edge == null || edge.rightHalf == null ||
                 edge.leftHalf == null ) {
                continue;
            }
            v1 = edge.rightHalf.startingVertex;
            v2 = edge.leftHalf.startingVertex;
            if ( v1 == null || v2 == null ) {
                continue;
            }

            for ( j = 0; j < other.getPolygonsList().size(); j++ ) {
                face = other.getPolygonsList().get(j);
                if ( face == null || face.getContainingPlane() == null ) {
                    continue;
                }

                d1 = face.getContainingPlane().pointDistance(v1.position);
                d2 = face.getContainingPlane().pointDistance(v2.position);
                s1 = compareToZero(d1);
                s2 = compareToZero(d2);

                if ( !((s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1)) ) {
                    continue;
                }

                t = d1 / (d1 - d2);
                p = v1.position.add(
                    v2.position.subtract(v1.position).multiply(t));
                d3 = face.getContainingPlane().pointDistance(p);
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

    private static boolean hasPartialCoplanarFaceAreaOverlap(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        int i;
        int j;

        if ( solidA == null || solidB == null ) {
            return false;
        }

        for ( i = 0; i < solidA.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace faceA = solidA.getPolygonsList().get(i);
            for ( j = 0; j < solidB.getPolygonsList().size(); j++ ) {
                _PolyhedralBoundedSolidFace faceB = solidB.getPolygonsList().get(j);
                if ( coplanarFaces(faceA, faceB) &&
                     partialCoplanarFaceAreaOverlap(faceA, faceB) ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ArrayList<ArrayList<Vector3Dd>>
    partialCoplanarFaceAreaOverlapPolygons(PolyhedralBoundedSolid solidA,
                                           PolyhedralBoundedSolid solidB)
    {
        ArrayList<ArrayList<Vector3Dd>> polygons;
        int i;
        int j;

        polygons = new ArrayList<ArrayList<Vector3Dd>>();
        if ( solidA == null || solidB == null ) {
            return polygons;
        }

        for ( i = 0; i < solidA.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace faceA = solidA.getPolygonsList().get(i);
            for ( j = 0; j < solidB.getPolygonsList().size(); j++ ) {
                _PolyhedralBoundedSolidFace faceB = solidB.getPolygonsList().get(j);
                if ( coplanarFaces(faceA, faceB) &&
                     partialCoplanarFaceAreaOverlap(faceA, faceB) ) {
                    ArrayList<Vector3Dd> polygon =
                        coplanarFaceIntersectionPolygon(faceA, faceB);
                    if ( polygon.size() >= 3 ) {
                        polygons.add(polygon);
                    }
                }
            }
        }

        return polygons;
    }

    private static boolean coplanarFaces(_PolyhedralBoundedSolidFace faceA,
                                         _PolyhedralBoundedSolidFace faceB)
    {
        if ( faceA == null || faceB == null ||
             faceA.getContainingPlane() == null ||
             faceB.getContainingPlane() == null ) {
            return false;
        }
        if ( !PolyhedralBoundedSolidNumericPolicy.unitVectorsParallel(
                 faceA.getContainingPlane().getNormal(),
                 faceB.getContainingPlane().getNormal(), numericContext) ) {
            return false;
        }
        if ( faceB.boundariesList.size() < 1 ||
             faceB.boundariesList.get(0).boundaryStartHalfEdge == null ) {
            return false;
        }

        return Math.abs(faceA.getContainingPlane().pointDistance(
            faceB.boundariesList.get(0).boundaryStartHalfEdge
                .startingVertex.position)) <= numericContext.bigEpsilon();
    }

    private static boolean partialCoplanarFaceAreaOverlap(
        _PolyhedralBoundedSolidFace faceA,
        _PolyhedralBoundedSolidFace faceB)
    {
        if ( faceHasInteriorVertexOrEdgeMidpoint(faceA, faceB) ||
             faceHasInteriorVertexOrEdgeMidpoint(faceB, faceA) ) {
            return true;
        }

        return faceBoundariesCrossProperly(faceA, faceB);
    }

    private static ArrayList<Vector3Dd> coplanarFaceIntersectionPolygon(
        _PolyhedralBoundedSolidFace faceA,
        _PolyhedralBoundedSolidFace faceB)
    {
        ArrayList<Vector3Dd> points;

        points = new ArrayList<Vector3Dd>();
        appendFaceVerticesInsideOther(points, faceA, faceB);
        appendFaceVerticesInsideOther(points, faceB, faceA);
        appendBoundaryIntersections(points, faceA, faceB);
        sortCoplanarPolygon(points, faceA.getContainingPlane().getNormal());

        if ( coplanarPolygonAreaMagnitude(points,
                 faceA.getContainingPlane().getNormal()) <=
             numericContext.bigEpsilon() * numericContext.bigEpsilon() ) {
            points.clear();
        }

        return points;
    }

    private static void appendFaceVerticesInsideOther(
        ArrayList<Vector3Dd> points,
        _PolyhedralBoundedSolidFace source,
        _PolyhedralBoundedSolidFace target)
    {
        int i;

        for ( i = 0; i < source.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = source.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start;
            _PolyhedralBoundedSolidHalfEdge he;

            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            start = loop.boundaryStartHalfEdge;
            he = start;
            do {
                if ( target.testPointInside(he.startingVertex.position,
                         numericContext.bigEpsilon()) != Geometry.OUTSIDE ) {
                    appendUniquePoint(points, he.startingVertex.position);
                }
                he = he.next();
            } while ( he != null && he != start );
        }
    }

    private static void appendBoundaryIntersections(
        ArrayList<Vector3Dd> points,
        _PolyhedralBoundedSolidFace faceA,
        _PolyhedralBoundedSolidFace faceB)
    {
        int i;
        int j;
        int dominantCoordinate = dominantCoordinateForFace(faceA);

        for ( i = 0; i < faceA.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loopA = faceA.boundariesList.get(i);
            if ( loopA == null ) {
                continue;
            }
            for ( j = 0; j < faceB.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loopB = faceB.boundariesList.get(j);
                if ( loopB != null ) {
                    appendLoopIntersections(points, loopA, loopB,
                        dominantCoordinate);
                }
            }
        }
    }

    private static void appendLoopIntersections(
        ArrayList<Vector3Dd> points,
        _PolyhedralBoundedSolidLoop loopA,
        _PolyhedralBoundedSolidLoop loopB,
        int dominantCoordinate)
    {
        int i;
        int j;

        for ( i = 0; i < loopA.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge heA = loopA.halfEdgesList.get(i);
            if ( heA == null || heA.next() == null ) {
                continue;
            }
            for ( j = 0; j < loopB.halfEdgesList.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge heB =
                    loopB.halfEdgesList.get(j);
                if ( heB == null || heB.next() == null ) {
                    continue;
                }
                appendSegmentIntersection(points, heA, heB,
                    dominantCoordinate);
            }
        }
    }

    private static void appendSegmentIntersection(
        ArrayList<Vector3Dd> points,
        _PolyhedralBoundedSolidHalfEdge heA,
        _PolyhedralBoundedSolidHalfEdge heB,
        int dominantCoordinate)
    {
        Vector2Dd a1 = projectPointTo2D(heA.startingVertex.position,
            dominantCoordinate);
        Vector2Dd a2 = projectPointTo2D(heA.next().startingVertex.position,
            dominantCoordinate);
        Vector2Dd b1 = projectPointTo2D(heB.startingVertex.position,
            dominantCoordinate);
        Vector2Dd b2 = projectPointTo2D(heB.next().startingVertex.position,
            dominantCoordinate);
        double den;
        double t;
        Vector2Dd da;
        Vector2Dd db;
        Vector2Dd ba;

        if ( !segmentsCrossProperly2D(a1, a2, b1, b2) ) {
            return;
        }

        da = new Vector2Dd(a2.x() - a1.x(), a2.y() - a1.y());
        db = new Vector2Dd(b2.x() - b1.x(), b2.y() - b1.y());
        ba = new Vector2Dd(b1.x() - a1.x(), b1.y() - a1.y());
        den = cross2D(da, db);
        if ( Math.abs(den) <= numericContext.bigEpsilon() ) {
            return;
        }

        t = cross2D(ba, db) / den;
        appendUniquePoint(points, heA.startingVertex.position.add(
            heA.next().startingVertex.position
                .subtract(heA.startingVertex.position).multiply(t)));
    }

    private static double cross2D(Vector2Dd a, Vector2Dd b)
    {
        return a.x()*b.y() - a.y()*b.x();
    }

    private static void appendUniquePoint(ArrayList<Vector3Dd> points,
                                          Vector3Dd point)
    {
        int i;

        for ( i = 0; i < points.size(); i++ ) {
            if ( PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                    points.get(i), point, numericContext) ) {
                return;
            }
        }
        points.add(new Vector3Dd(point));
    }

    private static void sortCoplanarPolygon(ArrayList<Vector3Dd> points,
                                            Vector3Dd normal)
    {
        Vector3Dd center;
        Vector3Dd u;
        Vector3Dd v;
        Vector3Dd n;
        int i;

        if ( points.size() < 3 ) {
            return;
        }

        center = new Vector3Dd();
        for ( i = 0; i < points.size(); i++ ) {
            center = center.add(points.get(i));
        }
        center = center.multiply(1.0 / points.size());

        n = new Vector3Dd(normal).normalized();
        u = points.get(0).subtract(center);
        if ( u.length() <= numericContext.bigEpsilon() ) {
            return;
        }
        u = u.normalized();
        v = n.crossProduct(u).normalized();

        final Vector3Dd sortCenter = center;
        final Vector3Dd sortU = u;
        final Vector3Dd sortV = v;
        Collections.sort(points, (p1, p2) -> {
            Vector3Dd d1 = p1.subtract(sortCenter);
            Vector3Dd d2 = p2.subtract(sortCenter);
            double a1 = Math.atan2(d1.dotProduct(sortV),
                d1.dotProduct(sortU));
            double a2 = Math.atan2(d2.dotProduct(sortV),
                d2.dotProduct(sortU));
            return Double.compare(a1, a2);
        });
    }

    private static double coplanarPolygonAreaMagnitude(
        ArrayList<Vector3Dd> points,
        Vector3Dd normal)
    {
        Vector3Dd accumulator;
        int i;

        if ( points.size() < 3 ) {
            return 0.0;
        }

        accumulator = new Vector3Dd();
        for ( i = 0; i < points.size(); i++ ) {
            Vector3Dd p = points.get(i);
            Vector3Dd q = points.get((i+1)%points.size());
            accumulator = accumulator.add(p.crossProduct(q));
        }

        return Math.abs(accumulator.dotProduct(normal.normalized())) * 0.5;
    }

    private static PolyhedralBoundedSolid createLaminaFromPolygon(
        ArrayList<Vector3Dd> points)
    {
        PolyhedralBoundedSolid solid;
        int i;

        solid = new PolyhedralBoundedSolid();
        if ( points.size() < 3 ) {
            return solid;
        }

        PolyhedralBoundedSolidEulerOperators.mvfs(solid, points.get(0), 1, 1);
        for ( i = 1; i < points.size(); i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i+1, points.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, points.size(), 1, 2);
        return solid;
    }

    private static boolean faceHasInteriorVertexOrEdgeMidpoint(
        _PolyhedralBoundedSolidFace source,
        _PolyhedralBoundedSolidFace target)
    {
        int i;

        for ( i = 0; i < source.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = source.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start;
            _PolyhedralBoundedSolidHalfEdge he;

            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }
            start = loop.boundaryStartHalfEdge;
            he = start;
            do {
                if ( target.testPointInside(he.startingVertex.position,
                         numericContext.bigEpsilon()) == Geometry.INSIDE ) {
                    return true;
                }
                if ( he.next() != null ) {
                    Vector3Dd midpoint = he.startingVertex.position.add(
                        he.next().startingVertex.position
                            .subtract(he.startingVertex.position)
                            .multiply(0.5));
                    if ( target.testPointInside(midpoint,
                             numericContext.bigEpsilon()) ==
                         Geometry.INSIDE ) {
                        return true;
                    }
                }
                he = he.next();
            } while ( he != null && he != start );
        }

        return false;
    }

    private static boolean faceBoundariesCrossProperly(
        _PolyhedralBoundedSolidFace faceA,
        _PolyhedralBoundedSolidFace faceB)
    {
        int i;
        int j;
        int dominantCoordinate = dominantCoordinateForFace(faceA);

        for ( i = 0; i < faceA.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loopA = faceA.boundariesList.get(i);
            if ( loopA == null ) {
                continue;
            }
            for ( j = 0; j < faceB.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loopB = faceB.boundariesList.get(j);
                if ( loopB != null &&
                     loopsCrossProperly(loopA, loopB, dominantCoordinate) ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean loopsCrossProperly(_PolyhedralBoundedSolidLoop loopA,
                                              _PolyhedralBoundedSolidLoop loopB,
                                              int dominantCoordinate)
    {
        int i;
        int j;

        for ( i = 0; i < loopA.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge heA = loopA.halfEdgesList.get(i);
            if ( heA == null || heA.next() == null ) {
                continue;
            }
            Vector2Dd a1 = projectPointTo2D(heA.startingVertex.position,
                dominantCoordinate);
            Vector2Dd a2 = projectPointTo2D(heA.next().startingVertex.position,
                dominantCoordinate);

            for ( j = 0; j < loopB.halfEdgesList.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge heB =
                    loopB.halfEdgesList.get(j);
                if ( heB == null || heB.next() == null ) {
                    continue;
                }
                Vector2Dd b1 = projectPointTo2D(heB.startingVertex.position,
                    dominantCoordinate);
                Vector2Dd b2 = projectPointTo2D(
                    heB.next().startingVertex.position, dominantCoordinate);
                if ( segmentsCrossProperly2D(a1, a2, b1, b2) ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int dominantCoordinateForFace(_PolyhedralBoundedSolidFace face)
    {
        Vector3Dd n = face.getContainingPlane().getNormal();

        if ( Math.abs(n.x()) >= Math.abs(n.y()) &&
             Math.abs(n.x()) >= Math.abs(n.z()) ) {
            return 1;
        }
        if ( Math.abs(n.y()) >= Math.abs(n.x()) &&
             Math.abs(n.y()) >= Math.abs(n.z()) ) {
            return 2;
        }
        return 3;
    }

    private static Vector2Dd projectPointTo2D(Vector3Dd in,
                                             int dominantCoordinate)
    {
        if ( dominantCoordinate == 1 ) {
            return new Vector2Dd(in.y(), in.z());
        }
        if ( dominantCoordinate == 2 ) {
            return new Vector2Dd(in.x(), in.z());
        }
        return new Vector2Dd(in.x(), in.y());
    }

    private static double orientation2D(Vector2Dd a, Vector2Dd b, Vector2Dd c)
    {
        return (b.x()-a.x())*(c.y()-a.y()) -
            (b.y()-a.y())*(c.x()-a.x());
    }

    private static boolean segmentsCrossProperly2D(Vector2Dd a1, Vector2Dd a2,
                                                   Vector2Dd b1, Vector2Dd b2)
    {
        double o1 = orientation2D(a1, a2, b1);
        double o2 = orientation2D(a1, a2, b2);
        double o3 = orientation2D(b1, b2, a1);
        double o4 = orientation2D(b1, b2, a2);
        double tolerance = PolyhedralBoundedSolidNumericPolicy
            .orientationTolerance2D(a1, a2, b1, numericContext);

        tolerance = Math.max(tolerance, PolyhedralBoundedSolidNumericPolicy
            .orientationTolerance2D(a1, a2, b2, numericContext));
        tolerance = Math.max(tolerance, PolyhedralBoundedSolidNumericPolicy
            .orientationTolerance2D(b1, b2, a1, numericContext));
        tolerance = Math.max(tolerance, PolyhedralBoundedSolidNumericPolicy
            .orientationTolerance2D(b1, b2, a2, numericContext));

        return (o1 > tolerance && o2 < -tolerance ||
                o1 < -tolerance && o2 > tolerance) &&
               (o3 > tolerance && o4 < -tolerance ||
                o3 < -tolerance && o4 > tolerance);
    }
}
