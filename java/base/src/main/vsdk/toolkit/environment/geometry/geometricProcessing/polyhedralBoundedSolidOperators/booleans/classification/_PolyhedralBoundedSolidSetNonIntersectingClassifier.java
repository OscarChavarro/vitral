//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.common.linealAlgebra.Vector2Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidFaceGeometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
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
    Per-{@code setOp} memo for the expensive, order-independent preflight
    predicates. Within a single Boolean operation the operand solids are not
    mutated until the Mäntylä generate stage runs (after every preflight has
    either fired or declined), so each predicate is a pure function of
    {@code (a, b)} and may be computed once and reused across
    {@link #runTouchingOnlyPreflightCase},
    {@link #runContainmentOnlyPreflightCase} and {@link #runSetOpNoIntersectionCase}.

    <p>The cache only memoizes the heavy primitives (two
    {@code classifySolidAgainstSolid} scans, the interior-overlap test and
    the two edge/face intersection scans). It does not change any decision: a
    request that hits the cache returns exactly the value the uncached path
    would have recomputed.</p>
    */
    static final class _PreflightCache
    {
        private static final int UNSET = Integer.MIN_VALUE;

        private final PolyhedralBoundedSolid a;
        private final PolyhedralBoundedSolid b;
        private int aInB = UNSET;
        private int bInA = UNSET;
        private byte interiorOverlap = -1;
        private byte edgeFaceAB = -1;
        private byte edgeFaceBA = -1;

        private _PreflightCache(PolyhedralBoundedSolid a,
            PolyhedralBoundedSolid b)
        {
            this.a = a;
            this.b = b;
        }

        int aInB()
        {
            if ( aInB == UNSET ) {
                aInB = classifySolidAgainstSolid(a, b);
            }
            return aInB;
        }

        int bInA()
        {
            if ( bInA == UNSET ) {
                bInA = classifySolidAgainstSolid(b, a);
            }
            return bInA;
        }

        boolean hasInteriorOverlap()
        {
            if ( interiorOverlap < 0 ) {
                interiorOverlap = (byte)(hasConfirmedInteriorOverlap(a, b)
                    ? 1 : 0);
            }
            return interiorOverlap == 1;
        }

        boolean hasEdgeFaceIntersectionAB()
        {
            if ( edgeFaceAB < 0 ) {
                edgeFaceAB = (byte)(hasProperEdgeFaceIntersection(a, b)
                    ? 1 : 0);
            }
            return edgeFaceAB == 1;
        }

        boolean hasEdgeFaceIntersectionBA()
        {
            if ( edgeFaceBA < 0 ) {
                edgeFaceBA = (byte)(hasProperEdgeFaceIntersection(b, a)
                    ? 1 : 0);
            }
            return edgeFaceBA == 1;
        }
    }

    /**
    Builds a fresh per-{@code setOp} preflight memo for the operand pair. The
    caller is responsible for using it only while {@code inSolidA}/{@code inSolidB}
    remain unmutated (i.e. across the preflight block, before the generate
    stage).
    */
    static _PreflightCache newPreflightCache(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return new _PreflightCache(inSolidA, inSolidB);
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
        return runContainmentOnlyPreflightCase(inSolidA, inSolidB,
            newPreflightCache(inSolidA, inSolidB));
    }

    static boolean runContainmentOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        _PreflightCache cache)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int relation = classifyNoIntersectionRelation(cache.aInB(),
            cache.bInA());

        // Restricted to strict containment only. An earlier attempt to
        // extend this preflight to "tangent containment" (one solid
        // sitting inside the other with all its boundary vertices on
        // the other's surface) regressed legitimate cases — those need
        // the regular pipeline because their result expects the inner
        // surface preserved as a hole/cut, not a plain merge. The two
        // absorption-step-2 fixtures (MANT1988_15_2_LIMIT,
        // MANT1988_6_13) that motivate this preflight are tracked in
        // plan §7.3.1.D-cont as remaining drift.
        if ( relation != NO_INT_RELATION_A_IN_B &&
             relation != NO_INT_RELATION_B_IN_A ) {
            return false;
        }

        if ( cache.hasEdgeFaceIntersectionAB() ) {
            return false;
        }
        if ( cache.hasEdgeFaceIntersectionBA() ) {
            return false;
        }

        return true;
    }

    static boolean runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return runTouchingOnlyPreflightCase(inSolidA, inSolidB,
            newPreflightCache(inSolidA, inSolidB));
    }

    static boolean runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        _PreflightCache cache)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int relation = classifyNoIntersectionRelation(cache.aInB(),
            cache.bInA());

        if ( relation != NO_INT_RELATION_TOUCHING ) {
            return false;
        }

        if ( cache.hasEdgeFaceIntersectionAB() ) {
            return false;
        }
        if ( cache.hasEdgeFaceIntersectionBA() ) {
            return false;
        }

        // Solids that share part of a face and nothing else are a contact
        // too (STACKED_BLOCKS): the interiors do not overlap. A union along
        // such a face is glued by the pipeline; the caller keeps it out.
        if ( cache.hasInteriorOverlap() ) {
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
        return runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op,
            newPreflightCache(inSolidA, inSolidB));
    }

    static PolyhedralBoundedSolid runSetOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op,
        _PreflightCache cache)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));

        int relation = classifyNoIntersectionRelation(cache.aInB(),
            cache.bInA());

        // Tangent containment (B-02 b): one solid inside the other with
        // faces on the other's boundary. The vertex test cannot see it
        // (every vertex is "on"), so it reports touching. Only the answers
        // that need no cavity use it; B \ A-style cavities need the
        // pipeline.
        if ( relation == NO_INT_RELATION_TOUCHING ) {
            if ( isTangentlyContained(inSolidA, inSolidB) ) {
                relation = NO_INT_RELATION_A_IN_B;
            }
            else if ( op != SUBTRACT &&
                      isTangentlyContained(inSolidB, inSolidA) ) {
                relation = NO_INT_RELATION_B_IN_A;
            }
        }

        if ( op == INTERSECTION ) {
            if ( relation == NO_INT_RELATION_A_IN_B ) {
                outRes.merge(inSolidA);
            }
            else if ( relation == NO_INT_RELATION_B_IN_A ) {
                outRes.merge(inSolidB);
            }
            else if ( !cache.hasInteriorOverlap() ) {
                appendContactLaminae(inSolidA, inSolidB, outRes);
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
            else if ( relation == NO_INT_RELATION_TOUCHING ) {
                // Two shells sharing only an edge or a vertex: a
                // non-manifold result, rejected by design (B-04). Face
                // contact never gets here: the pipeline glues it.
                throw new NonManifoldBooleanResultException(
                    "UNION of solids touching only along edges or at " +
                    "vertices is non-manifold (two shells sharing a " +
                    "boundary point); rejected");
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

    /**
    Lamina policy for the intersection of solids that share part of a face
    and nothing else (STACKED_BLOCKS): the shared part is returned as
    laminae, zero volume, valid and easy to display, not as the regularised
    empty set. A contact whose area is below the collapsed face threshold
    gives no lamina: a collapsed lamina is not a valid result (see
    _GeometricCollapsedLaminaStrategy).

    <p>This is part of the "no intersections found" resolution of the
    pipeline ([MANT1988] 15.4). Until plan-stage11 Phase E a preflight
    answered these pairs before generate and classify ran.</p>
    */
    private static void appendContactLaminae(PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB, PolyhedralBoundedSolid outRes)
    {
        ArrayList<ArrayList<Vector3Dd>> contactPolygons =
            partialCoplanarFaceAreaOverlapPolygons(inSolidA, inSolidB);
        int i;

        for ( i = 0; i < contactPolygons.size(); i++ ) {
            if ( polygonArea(contactPolygons.get(i)) <
                 PolyhedralBoundedSolidGeometricValidator
                     .collapsedFaceAreaThreshold(numericContext) ) {
                continue;
            }
            PolyhedralBoundedSolid lamina =
                createLaminaFromPolygon(contactPolygons.get(i));
            if ( lamina.getPolygonsList().size() > 0 ) {
                outRes.merge(lamina);
            }
        }
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

    /**
    Precomputes the containing plane and the bounding box of every face of
    {@code solid}, indexed by face position. Point classification tests many
    points against the same unmutated solid, so computing each plane once
    (instead of once per point per face) removes the dominant
    Newell/tolerance-context recompute from the hot path, and the box skips
    the point-in-face test for points far from the face (Stage G). The
    returned array is only valid while {@code solid} is not mutated.
    */
    private static PolyhedralBoundedSolidFaceGeometry[] precomputeFacePlanes(
        PolyhedralBoundedSolid solid)
    {
        if ( solid == null ) {
            return new PolyhedralBoundedSolidFaceGeometry[0];
        }
        return PolyhedralBoundedSolidFaceGeometry.of(solid);
    }

    private static int classifyPointAgainstSolid(PolyhedralBoundedSolid solid,
        Vector3Dd point)
    {
        return classifyPointAgainstSolid(solid, precomputeFacePlanes(solid),
            point);
    }

    static int classifyPointAgainstSolid(PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidFaceGeometry[] facePlanes,
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
            InfinitePlane facePlane = facePlanes[i].getPlane();
            if ( facePlane == null ) {
                continue;
            }
            if ( Math.abs(facePlane.pointDistance(point)) <= eps &&
                 !facePlanes[i].projectionExcludes(point, eps) ) {
                if ( face.testPointInside(point, eps, facePlane) !=
                     Geometry.OUTSIDE ) {
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
                InfinitePlane facePlane = facePlanes[i].getPlane();
                if ( facePlane == null ) {
                    ambiguous = true;
                    break;
                }
                Ray rayHit = new Ray(ray);
                Ray hit = facePlane.doIntersectionFirstHit(rayHit);
                if ( hit == null ) {
                    continue;
                }
                if ( hit.getT() <= eps ) {
                    continue;
                }

                Vector3Dd pi = hit.getOrigin().add(
                    hit.getDirection().multiply(hit.getT()));
                if ( facePlanes[i].projectionExcludes(pi, eps) ) {
                    continue;
                }
                int status = face.testPointInside(pi, eps, facePlane);
                if ( status == Geometry.LIMIT ) {
                    ambiguous = true;
                    break;
                }
                if ( status == Geometry.INSIDE ) {
                    boolean duplicated = false;
                    int k;
                    for ( k = 0; k < distances.size(); k++ ) {
                        if ( Math.abs(distances.get(k).doubleValue() - hit.getT())
                             <= eps ) {
                            duplicated = true;
                            break;
                        }
                    }
                    if ( !duplicated ) {
                        distances.add(Double.valueOf(hit.getT()));
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

    /**
    Witness test used to tell a right empty result from a lost solid
    (Fable5.2Analysis.md Stage F step 4): true when some point is interior
    to both solids, so their intersection cannot be empty. The operands
    must be intact (not consumed by a set operation).
    */
    static boolean interiorsOverlap(PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB));
        return hasConfirmedInteriorOverlap(solidA, solidB);
    }

    /**
    Witness test for A \ B: true when a vertex or a face interior witness
    of `solidA` lies strictly outside `solidB`. A boundary point of A
    outside B has interior points of A around it that are outside B, so
    A \ B cannot be empty. (The converse does not hold, so a false answer
    proves nothing.)
    */
    static boolean hasBoundaryPointOutside(PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        PolyhedralBoundedSolidFaceGeometry[] planesB;
        int i;

        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB));
        planesB = precomputeFacePlanes(solidB);
        for ( i = 0; i < solidA.getVerticesList().size(); i++ ) {
            if ( classifyPointAgainstSolid(solidB, planesB,
                     solidA.getVerticesList().get(i).position) ==
                 Geometry.OUTSIDE ) {
                return true;
            }
        }
        return someFaceWitnessIsOutside(solidA, solidB, planesB);
    }

    /**
    True when some point is strictly inside both solids. Decided from the
    boundaries by {@link _PolyhedralBoundedSolidInteriorOverlap}, without
    sampling.
    */
    private static boolean hasConfirmedInteriorOverlap(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        return _PolyhedralBoundedSolidInteriorOverlap.interiorsOverlap(
            solidA, solidB);
    }

    /**
    Classifies solid A against solid B for the "no proper intersection"
    preflights (Fable5.2Analysis.md Stage D step 1, defect B-03).
    <ul>
    <li>INSIDE: A is contained in B. No vertex of A is outside B, at least
        one is strictly inside, and a witness point interior to every face of
        A is not outside B. The witness test separates tangent containment
        from a solid whose edges lie in a face of B while part of it sticks
        out (B-03): there every vertex is in or on B, yet a face is not.</li>
    <li>LIMIT: A touches B, or straddles its boundary (vertices on both
        sides, or a face witness outside). Callers must not take LIMIT as
        containment; the touching preflight additionally requires that the
        interiors do not overlap.</li>
    <li>OUTSIDE: every vertex of A is outside B.</li>
    </ul>
    Before this rule the first vertex found inside made A "contained", even
    with other vertices outside.
    */
    private static int classifySolidAgainstSolid(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        int i;
        int inside = 0;
        int limit = 0;
        int outside = 0;

        if ( solidA == null || solidA.getVerticesList().size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        PolyhedralBoundedSolidFaceGeometry[] planesB = precomputeFacePlanes(solidB);
        for ( i = 0; i < solidA.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solidA.getVerticesList().get(i);
            int status = classifyPointAgainstSolid(solidB, planesB, v.position);
            if ( status == Geometry.INSIDE ) {
                inside++;
            }
            else if ( status == Geometry.LIMIT ) {
                limit++;
            }
            else {
                outside++;
            }
        }

        if ( outside > 0 ) {
            return (inside > 0 || limit > 0) ? Geometry.LIMIT
                                             : Geometry.OUTSIDE;
        }
        if ( inside == 0 ) {
            return Geometry.LIMIT;
        }
        if ( someFaceWitnessIsOutside(solidA, solidB, planesB) ) {
            return Geometry.LIMIT;
        }
        return Geometry.INSIDE;
    }

    /**
    True when no vertex and no face witness of {@code inner} is outside
    {@code outer}, and at least one face witness is strictly inside it:
    containment with boundary contact.
    */
    private static boolean isTangentlyContained(PolyhedralBoundedSolid inner,
        PolyhedralBoundedSolid outer)
    {
        int i;
        boolean someInside = false;
        PolyhedralBoundedSolidFaceGeometry[] planes = precomputeFacePlanes(outer);

        for ( i = 0; i < inner.getVerticesList().size(); i++ ) {
            if ( classifyPointAgainstSolid(outer, planes,
                     inner.getVerticesList().get(i).position) ==
                 Geometry.OUTSIDE ) {
                return false;
            }
        }
        for ( i = 0; i < inner.getPolygonsList().size(); i++ ) {
            Vector3Dd witness = faceInteriorWitness(
                inner.getPolygonsList().get(i));
            if ( witness == null ) {
                continue;
            }
            int status = classifyPointAgainstSolid(outer, planes, witness);
            if ( status == Geometry.OUTSIDE ) {
                return false;
            }
            if ( status == Geometry.INSIDE ) {
                someInside = true;
            }
        }
        return someInside;
    }

    /**
    True when some vertex of `inner`, or a point inside one of its faces,
    lies on the boundary of `outer`. Meant for a solid already known to be
    contained in `outer`.
    */
    static boolean touchesBoundaryFromInside(PolyhedralBoundedSolid inner,
        PolyhedralBoundedSolid outer)
    {
        PolyhedralBoundedSolidFaceGeometry[] planes;
        int i;

        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inner, outer));
        planes = precomputeFacePlanes(outer);
        for ( i = 0; i < inner.getVerticesList().size(); i++ ) {
            if ( classifyPointAgainstSolid(outer, planes,
                     inner.getVerticesList().get(i).position) ==
                 Geometry.LIMIT ) {
                return true;
            }
        }
        for ( i = 0; i < inner.getPolygonsList().size(); i++ ) {
            Vector3Dd witness = faceInteriorWitness(
                inner.getPolygonsList().get(i));
            if ( witness != null &&
                 classifyPointAgainstSolid(outer, planes, witness) ==
                     Geometry.LIMIT ) {
                return true;
            }
        }
        return false;
    }

    private static boolean someFaceWitnessIsOutside(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB,
        PolyhedralBoundedSolidFaceGeometry[] planesB)
    {
        int i;

        for ( i = 0; i < solidA.getPolygonsList().size(); i++ ) {
            Vector3Dd witness = faceInteriorWitness(
                solidA.getPolygonsList().get(i));
            if ( witness != null &&
                 classifyPointAgainstSolid(solidB, planesB, witness) ==
                     Geometry.OUTSIDE ) {
                return true;
            }
        }
        return false;
    }

    /**
    A point strictly inside the face: the centroid of the first triangle of
    consecutive outer-loop vertices whose centroid the face classifies as
    INSIDE (holes and concave corners are rejected by that test).
    @return the witness, or null when no such triangle exists
    */
    static Vector3Dd faceInteriorWitness(_PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop loop;
        _PolyhedralBoundedSolidHalfEdge he;
        InfinitePlane plane;
        double eps = numericContext.bigEpsilon();

        if ( face == null || face.boundariesList.size() < 1 ) {
            return null;
        }
        plane = face.getContainingPlane();
        loop = face.boundariesList.get(0);
        if ( plane == null || loop == null ||
             loop.boundaryStartHalfEdge == null ) {
            return null;
        }
        he = loop.boundaryStartHalfEdge;
        do {
            _PolyhedralBoundedSolidHalfEdge n1 = he.next();
            if ( n1 == null || n1.next() == null ) {
                return null;
            }
            Vector3Dd c = he.startingVertex.position
                .add(n1.startingVertex.position)
                .add(n1.next().startingVertex.position)
                .multiply(1.0 / 3.0);
            if ( face.testPointInside(c, eps, plane) == Geometry.INSIDE ) {
                return c;
            }
            he = n1;
        } while ( he != loop.boundaryStartHalfEdge );
        return null;
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
        PolyhedralBoundedSolidFaceGeometry[] otherFaces;

        if ( current == null || other == null ) {
            return false;
        }

        otherFaces = precomputeFacePlanes(other);
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
                if ( face == null ) {
                    continue;
                }
                InfinitePlane facePlane = otherFaces[j].getPlane();
                if ( facePlane == null ) {
                    continue;
                }

                d1 = facePlane.pointDistance(v1.position);
                d2 = facePlane.pointDistance(v2.position);
                s1 = compareToZero(d1);
                s2 = compareToZero(d2);

                if ( !((s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1)) ) {
                    continue;
                }

                t = d1 / (d1 - d2);
                p = v1.position.add(
                    v2.position.subtract(v1.position).multiply(t));
                d3 = facePlane.pointDistance(p);
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

    /**
    True when a face of A and a face of B lie on the same plane with
    opposite orientation and share a region of positive area: the solids
    touch along a face (Fable5.2Analysis.md B-04). Two cubes sharing a whole
    face are detected through the interior witness of one face, since none
    of their vertices or edge midpoints is inside the other face.
    */
    static boolean hasOppositeCoplanarFaceAreaContact(
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB)
    {
        int i;
        int j;

        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(solidA, solidB));
        for ( i = 0; i < solidA.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace faceA = solidA.getPolygonsList().get(i);
            for ( j = 0; j < solidB.getPolygonsList().size(); j++ ) {
                _PolyhedralBoundedSolidFace faceB =
                    solidB.getPolygonsList().get(j);
                if ( !coplanarFaces(faceA, faceB) ||
                     faceA.getContainingPlane().getNormal().dotProduct(
                         faceB.getContainingPlane().getNormal()) >= 0.0 ) {
                    continue;
                }
                if ( partialCoplanarFaceAreaOverlap(faceA, faceB) ||
                     witnessInsideFace(faceA, faceB) ||
                     witnessInsideFace(faceB, faceA) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean witnessInsideFace(
        _PolyhedralBoundedSolidFace source,
        _PolyhedralBoundedSolidFace target)
    {
        Vector3Dd witness = faceInteriorWitness(source);

        return witness != null &&
            target.testPointInside(witness, numericContext.bigEpsilon()) ==
                Geometry.INSIDE;
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
        if ( faceA == null || faceB == null ) {
            return false;
        }
        InfinitePlane planeA = faceA.getContainingPlane();
        InfinitePlane planeB = faceB.getContainingPlane();
        if ( planeA == null || planeB == null ) {
            return false;
        }
        if ( !PolyhedralBoundedSolidNumericPolicy.unitVectorsParallel(
                 planeA.getNormal(), planeB.getNormal(), numericContext) ) {
            return false;
        }
        if ( faceB.boundariesList.size() < 1 ||
             faceB.boundariesList.get(0).boundaryStartHalfEdge == null ) {
            return false;
        }

        return Math.abs(planeA.pointDistance(
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
        Vector3Dd planeNormalA = faceA.getContainingPlane().getNormal();
        sortCoplanarPolygon(points, planeNormalA);

        if ( coplanarPolygonAreaMagnitude(points, planeNormalA) <=
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

    private static double polygonArea(ArrayList<Vector3Dd> points)
    {
        double nx = 0.0;
        double ny = 0.0;
        double nz = 0.0;
        int i;

        for ( i = 0; i < points.size(); i++ ) {
            Vector3Dd p = points.get(i);
            Vector3Dd q = points.get((i + 1) % points.size());
            nx += (p.y() - q.y()) * (p.z() + q.z());
            ny += (p.z() - q.z()) * (p.x() + q.x());
            nz += (p.x() - q.x()) * (p.y() + q.y());
        }
        return 0.5 * Math.sqrt(nx * nx + ny * ny + nz * nz);
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
