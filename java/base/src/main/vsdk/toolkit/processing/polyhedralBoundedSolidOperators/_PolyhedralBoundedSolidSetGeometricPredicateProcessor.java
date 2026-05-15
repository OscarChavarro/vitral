//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Geometric predicates and coplanar-angle algebra used by the set-operations
classifiers from sections [MANT1988].14.5, [MANT1988].15.6.1, and
[MANT1988].15.6.2.
*/
final class _PolyhedralBoundedSolidSetGeometricPredicateProcessor
    extends _PolyhedralBoundedSolidOperator
{
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final String TRACE_COPLANAR_TANGENTIAL_PROPERTY =
        "vsdk.setop.traceCoplanarTangential";

    /**
    Structured trace of a single {@code sectoroverlap} invocation captured
    when the static collector is active (see §7.3.1 of
    plan-csg-boolean-fix-stage2). Mutable POJO by design so the test can
    introspect every field without reflection.
    */
    static final class SectoroverlapTraceEntry
    {
        int callIndex;
        int faceA, faceB;
        int vertexAFrom, vertexATo;
        int vertexBFrom, vertexBTo;
        double a1, a2, b1, b2;
        double diffA2B1;
        double diffB2A1;
        boolean boundaryRayContact;
        boolean decision;
    }

    private static ArrayList<SectoroverlapTraceEntry> sectoroverlapTrace = null;
    private static int sectoroverlapCallCounter = 0;

    static void enableSectoroverlapTrace()
    {
        sectoroverlapTrace = new ArrayList<SectoroverlapTraceEntry>();
        sectoroverlapCallCounter = 0;
    }

    static void disableSectoroverlapTrace()
    {
        sectoroverlapTrace = null;
    }

    static ArrayList<SectoroverlapTraceEntry> getSectoroverlapTrace()
    {
        return sectoroverlapTrace;
    }

    private static void recordSectoroverlapCall(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb,
        double a1, double a2, double b1, double b2, boolean decision)
    {
        if ( sectoroverlapTrace == null ) {
            return;
        }
        SectoroverlapTraceEntry entry = new SectoroverlapTraceEntry();
        entry.callIndex = sectoroverlapCallCounter++;
        entry.faceA = (na != null && na.he != null && na.he.parentLoop != null
                       && na.he.parentLoop.parentFace != null)
            ? na.he.parentLoop.parentFace.id : -1;
        entry.faceB = (nb != null && nb.he != null && nb.he.parentLoop != null
                       && nb.he.parentLoop.parentFace != null)
            ? nb.he.parentLoop.parentFace.id : -1;
        entry.vertexAFrom = (na != null && na.he != null
                             && na.he.startingVertex != null)
            ? na.he.startingVertex.id : -1;
        entry.vertexATo = -1;
        if ( na != null && na.he != null && na.he.next() != null
             && na.he.next().startingVertex != null ) {
            entry.vertexATo = na.he.next().startingVertex.id;
        }
        entry.vertexBFrom = (nb != null && nb.he != null
                             && nb.he.startingVertex != null)
            ? nb.he.startingVertex.id : -1;
        entry.vertexBTo = -1;
        if ( nb != null && nb.he != null && nb.he.next() != null
             && nb.he.next().startingVertex != null ) {
            entry.vertexBTo = nb.he.next().startingVertex.id;
        }
        entry.a1 = a1; entry.a2 = a2;
        entry.b1 = b1; entry.b2 = b2;
        entry.diffA2B1 = a2 - b1;
        entry.diffB2A1 = b2 - a1;
        entry.boundaryRayContact = Math.abs(entry.diffA2B1) < 1.0e-12
            || Math.abs(entry.diffB2A1) < 1.0e-12;
        entry.decision = decision;
        sectoroverlapTrace.add(entry);
    }

    private static final class CoplanarAngleBasis
    {
        private Vector3Dd normal;
        private Vector3Dd u;
        private Vector3Dd v;
    }

    private static final class CoplanarAngularInterval
    {
        private double start;
        private double end;
        private double interior;
    }

    private static final int COPLANAR_OP_UNION = 0;
    private static final int COPLANAR_OP_INTERSECTION = 1;
    private static final int COPLANAR_OP_DIFFERENCE = 2;

    private static final int COPLANAR_SIDE_A_VS_B = 0;
    private static final int COPLANAR_SIDE_B_VS_A = 1;

    private static final int COPLANAR_ORIENTATION_SAME = 0;
    private static final int COPLANAR_ORIENTATION_OPPOSITE = 1;

    private static boolean isCoplanarTangentialTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_COPLANAR_TANGENTIAL_PROPERTY);
    }

    private static void traceCoplanarTangential(String message)
    {
        if ( !isCoplanarTangentialTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpCoplanarTrace] " + message);
    }

    /*
    Coplanar overlap decision table for vertex/face classifier.
    */
    private static final int[][][] COPLANAR_VERTEX_FACE_CLASS_TABLE = {
        {
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN
            },
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN
            }
        },
        {
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            },
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            }
        },
        {
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            },
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            }
        }
    };

    /*
    Coplanar overlap decision table for vertex/vertex classifier.
    */
    private static final int[][][] COPLANAR_VERTEX_VERTEX_CLASS_TABLE = {
        {
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN
            },
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN
            }
        },
        {
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            },
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            }
        },
        {
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            },
            {
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT,
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT
            }
        }
    };

    static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value,
            numericContext);
    }

    static int pointInFace(_PolyhedralBoundedSolidFace face, Vector3Dd point)
    {
        return face.testPointInside(point, numericContext.bigEpsilon());
    }

    static _PolyhedralBoundedSolidFace.PointInsideResult
    pointInFaceDetailed(_PolyhedralBoundedSolidFace face, Vector3Dd point)
    {
        return face.testPointInsideDetailed(point, numericContext.bigEpsilon());
    }

    static boolean colinearVectors(Vector3Dd a, Vector3Dd b)
    {
        return PolyhedralBoundedSolidNumericPolicy
            .vectorsColinear(a, b, numericContext);
    }

    static boolean colinearVectorsWithDirection(Vector3Dd a, Vector3Dd b)
    {
        if ( PolyhedralBoundedSolidNumericPolicy
            .vectorsColinear(a, b, numericContext) ) {
            if ( a.dotProduct(b) >= 0 ) return true;
        }
        return false;
    }

    /**
    Following program [MANT1988].15.9. According to the sector intersection
    test from section [MANT1988].15.6.2, the variables are interpreted as in
    figure [MANT1988].15.8 and equation [MANT1988].15.5.
    */
    static boolean sctrwitthin(Vector3Dd dir, Vector3Dd ref1,
                               Vector3Dd ref2, Vector3Dd ref12)
    {
        Vector3Dd c1;
        Vector3Dd c2;
        int t1;
        int t2;

        c1 = dir.crossProduct(ref1);
        if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
            dir, ref1, numericContext) ) {
            return (ref1.dotProduct(dir) > 0.0);
        }
        c2 = ref2.crossProduct(dir);
        if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
            ref2, dir, numericContext) ) {
            return (ref2.dotProduct(dir) > 0.0);
        }
        t1 = compareToZero(c1.dotProduct(ref12));
        t2 = compareToZero(c2.dotProduct(ref12));
        return ( t1 < 0.0 && t2 < 0.0 );
    }

    /**
    Strict version of the sector-within test from section [MANT1988].15.6.2
    that excludes the boundary-line cases implicit in figure [MANT1988].15.8
    and equation [MANT1988].15.5.
    */
    static boolean sctrwitthinProper(Vector3Dd dir, Vector3Dd ref1,
                                     Vector3Dd ref2, Vector3Dd ref12)
    {
        if ( colinearVectors(dir, ref1) || colinearVectors(dir, ref2) ) {
            return false;
        }

        return sctrwitthin(dir, ref1, ref2, ref12);
    }

    /**
    Checks overlap of two coplanar sectors for the vertex/vertex classifier.
    Following section [MANT1988].15.6.2, where the operation is required but
    left implicit after program [MANT1988].15.9.
    */
    static boolean sectoroverlap(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb,
        boolean withDebug)
    {
        double a1;
        double a2;
        double b1;
        double b2;
        Vector3Dd u;
        Vector3Dd v;
        Vector3Dd a;
        Vector3Dd b;
        Vector3Dd c;
        Vector3Dd n;

        n = na.he.parentLoop.parentFace.getContainingPlane().getNormal();
        u = new Vector3Dd(na.ref1).normalized();
        v = n.crossProduct(u).normalized();

        a = new Vector3Dd(na.ref2).normalized();
        b = new Vector3Dd(nb.ref1).normalized();
        c = new Vector3Dd(nb.ref2).normalized();

        a1 = angleFromVectors(u, v, u);
        a2 = angleFromVectors(u, v, a);
        b1 = angleFromVectors(u, v, b);
        b2 = angleFromVectors(u, v, c);

        if ( a1 > a2 ) {
            double t = a1;
            a1 = a2;
            a2 = t;
        }
        if ( b1 > b2 ) {
            double t = b1;
            b1 = b2;
            b2 = t;
        }

        // Closed-set (epsilon-tolerant) interval-overlap check.
        // Intentionally returns true for touching sectors (a2 ≈ b1) because the
        // setop pipeline requires a null-edge strut even at coplanar boundary-ray
        // contact — a strict open-set check breaks MANT1988 §15.1 reference geometry.
        // The symmetric disjoint case (B entirely left of A) is not yet fixed here;
        // that requires a deeper restructuring of the coplanar V/V path.
        boolean decision = (a2 + VSDK.EPSILON > b1 - VSDK.EPSILON);
        recordSectoroverlapCall(na, nb, a1, a2, b1, b2, decision);
        if ( withDebug ) {
            System.out.print(decision ? " <TRUE>" : " <FALSE>");
        }
        return decision;
    }

    private static double angleFromVectors(Vector3Dd u, Vector3Dd v, Vector3Dd a)
    {
        double x;
        double y;
        double angle;

        x = a.dotProduct(u);
        y = a.dotProduct(v);
        if ( x > 1.0 ) {
            x = 1.0;
        }
        else if ( x < -1.0 ) {
            x = -1.0;
        }

        angle = Math.acos(x);
        if ( y < 0 ) {
            angle *= -1;
        }
        return angle;
    }

    /**
    Resolves the class to propagate for coplanar sector pairs when applying the
    8-way boundary-classification logic of section [MANT1988].15.3 and the
    vertex/vertex classifier of section [MANT1988].15.6.2.
    */
    static int resolveCoplanarVertexVertexClass(int op,
        boolean sameOrientation, boolean sideA)
    {
        int sideIndex = sideA ? 0 : 1;
        return COPLANAR_VERTEX_VERTEX_CLASS_TABLE[coplanarOpIndex(op)]
            [coplanarOrientationIndex(sameOrientation)]
            [sideIndex];
    }

    /**
    Applies the coplanar reclassification rules for the vertex/face classifier,
    starting from sections [MANT1988].14.5.1 and [MANT1988].14.5.2 and biased
    toward set operations as proposed in [MANT1988].15.6.1 and problem
    [MANT1988].15.4.
    */
    static void applyCoplanarRulesToVertexFaceNeighborhood(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane,
        int BvsA, int op,
        boolean useMirrorFace)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace current;
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidFace localFace;
        Vector3Dd c;
        double d;
        int i;
        int nnbr = nbr.size();
        int relation;
        int resolvedClass;
        boolean sameOrientation;

        for ( i = 0; i < nnbr; i++ ) {
            current = nbr.get(i);
            he = current.sector;
            if ( he == null || he.parentLoop == null ||
                 he.parentLoop.parentFace == null ) {
                continue;
            }

            if ( useMirrorFace ) {
                _PolyhedralBoundedSolidHalfEdge mirror = he.mirrorHalfEdge();
                if ( mirror == null || mirror.parentLoop == null ||
                     mirror.parentLoop.parentFace == null ) {
                    continue;
                }
                localFace = mirror.parentLoop.parentFace;
            }
            else {
                localFace = he.parentLoop.parentFace;
            }
            if ( localFace == null || localFace.getContainingPlane() == null ||
                 referencePlane == null ) {
                continue;
            }

            c = localFace.getContainingPlane().getNormal().crossProduct(
                referencePlane.getNormal());
            d = c.dotProduct(c);
            if ( compareToZero(d) != 0 ) {
                continue;
            }

            relation = classifyCoplanarSectorRelation(current, referenceFace);
            registerCoplanarRelation(nbr, i, relation);
            registerCoplanarRelation(nbr, (i+1)%nnbr, relation);
            traceCoplanarTangential(
                "vertexFace coplanar relation op=" + op +
                " side=" + BvsA +
                " face=" + referenceFace.id +
                " localFace=" + localFace.id +
                " sectorIndex=" + i +
                " relation=" + relation);

            if ( relation ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                     .COPLANAR_OVERLAP ) {
                d = localFace.getContainingPlane().getNormal().dotProduct(
                    referencePlane.getNormal());
                sameOrientation = (compareToZero(d) == 1);
                resolvedClass = resolveCoplanarSectorClass(op, BvsA,
                    sameOrientation);
                traceCoplanarTangential(
                    "  resolved vertexFace coplanar overlap sameOrientation=" +
                    sameOrientation + " class=" + resolvedClass);
                nbr.get(i).cl = resolvedClass;
                nbr.get((i+1)%nnbr).cl = resolvedClass;
            }
        }
    }

    private static Vector3Dd normalizedDirection(Vector3Dd direction)
    {
        Vector3Dd out;

        if ( direction == null ) {
            return null;
        }

        out = new Vector3Dd(direction);
        if ( out.length() <= numericContext.unitVectorTolerance() ) {
            return null;
        }
        out = out.normalized();
        return out;
    }

    private static CoplanarAngleBasis buildCoplanarAngleBasis(
        Vector3Dd planeNormal,
        Vector3Dd preferredDirection,
        Vector3Dd fallbackDirection)
    {
        CoplanarAngleBasis basis;
        Vector3Dd normal;
        Vector3Dd u;
        Vector3Dd v;

        normal = normalizedDirection(planeNormal);
        if ( normal == null ) {
            return null;
        }

        u = normalizedDirection(preferredDirection);
        if ( u == null || normal.crossProduct(u).length() <=
             numericContext.unitVectorTolerance() ) {
            u = normalizedDirection(fallbackDirection);
        }
        if ( u == null || normal.crossProduct(u).length() <=
             numericContext.unitVectorTolerance() ) {
            if ( Math.abs(normal.x()) < 0.9 ) {
                u = normalizedDirection(new Vector3Dd(1.0, 0.0, 0.0)
                    .subtract(normal.multiply(normal.x())));
            }
            else {
                u = normalizedDirection(new Vector3Dd(0.0, 1.0, 0.0)
                    .subtract(normal.multiply(normal.y())));
            }
        }
        if ( u == null ) {
            return null;
        }

        v = normal.crossProduct(u);
        v = normalizedDirection(v);
        if ( v == null ) {
            return null;
        }

        basis = new CoplanarAngleBasis();
        basis.normal = normal;
        basis.u = u;
        basis.v = v;
        return basis;
    }

    private static double angleOnBasis(CoplanarAngleBasis basis,
                                       Vector3Dd direction)
    {
        Vector3Dd d;

        d = normalizedDirection(direction);
        if ( basis == null || d == null ) {
            return 0.0;
        }
        return Math.atan2(d.dotProduct(basis.v), d.dotProduct(basis.u));
    }

    private static double unwrapAngleNear(double angle, double reference)
    {
        while ( angle - reference <= -Math.PI ) {
            angle += TWO_PI;
        }
        while ( angle - reference > Math.PI ) {
            angle -= TWO_PI;
        }
        return angle;
    }

    private static boolean sectorContainsDirectionInclusive(
        Vector3Dd dir,
        Vector3Dd ref1,
        Vector3Dd ref2,
        Vector3Dd ref12)
    {
        if ( colinearVectorsWithDirection(dir, ref1) ||
             colinearVectorsWithDirection(dir, ref2) ) {
            return true;
        }
        return sctrwitthin(dir, ref1, ref2, ref12);
    }

    private static Vector3Dd acceptSectorInteriorProbe(
        Vector3Dd candidate,
        Vector3Dd ref1,
        Vector3Dd ref2,
        Vector3Dd ref12)
    {
        Vector3Dd normalized;

        normalized = normalizedDirection(candidate);
        if ( normalized == null ) {
            return null;
        }
        if ( sctrwitthinProper(normalized, ref1, ref2, ref12) ) {
            return normalized;
        }
        if ( !colinearVectors(normalized, ref1) &&
             !colinearVectors(normalized, ref2) &&
             sectorContainsDirectionInclusive(normalized, ref1, ref2, ref12) ) {
            return normalized;
        }
        return null;
    }

    private static Vector3Dd selectSectorInteriorProbe(
        Vector3Dd ref1,
        Vector3Dd ref2,
        Vector3Dd ref12,
        Vector3Dd fallbackProbe)
    {
        Vector3Dd probe;
        Vector3Dd bisector;

        bisector = ref1.add(ref2);
        probe = acceptSectorInteriorProbe(bisector, ref1, ref2, ref12);
        if ( probe != null ) {
            return probe;
        }

        probe = acceptSectorInteriorProbe(bisector.multiply(-1.0), ref1, ref2,
            ref12);
        if ( probe != null ) {
            return probe;
        }

        probe = acceptSectorInteriorProbe(ref12.crossProduct(ref1), ref1, ref2,
            ref12);
        if ( probe != null ) {
            return probe;
        }

        probe = acceptSectorInteriorProbe(ref2.crossProduct(ref12), ref1, ref2,
            ref12);
        if ( probe != null ) {
            return probe;
        }

        return acceptSectorInteriorProbe(fallbackProbe, ref1, ref2, ref12);
    }

    private static CoplanarAngularInterval buildCoplanarAngularInterval(
        CoplanarAngleBasis basis,
        Vector3Dd boundary1,
        Vector3Dd boundary2,
        Vector3Dd interiorProbe)
    {
        CoplanarAngularInterval interval;
        Vector3Dd probe;
        double t;

        if ( basis == null || normalizedDirection(boundary1) == null ||
             normalizedDirection(boundary2) == null ) {
            return null;
        }

        probe = normalizedDirection(interiorProbe);
        if ( probe == null ) {
            return null;
        }

        interval = new CoplanarAngularInterval();
        interval.interior = angleOnBasis(basis, probe);
        interval.start = unwrapAngleNear(angleOnBasis(basis, boundary1),
            interval.interior);
        interval.end = unwrapAngleNear(angleOnBasis(basis, boundary2),
            interval.interior);

        if ( interval.start > interval.end ) {
            t = interval.start;
            interval.start = interval.end;
            interval.end = t;
        }

        return interval;
    }

    private static CoplanarAngularInterval alignCoplanarInterval(
        CoplanarAngularInterval source,
        double referenceInterior)
    {
        CoplanarAngularInterval aligned;
        double newInterior;
        double delta;

        if ( source == null ) {
            return null;
        }

        newInterior = unwrapAngleNear(source.interior, referenceInterior);
        delta = newInterior - source.interior;
        aligned = new CoplanarAngularInterval();
        aligned.start = source.start + delta;
        aligned.end = source.end + delta;
        aligned.interior = newInterior;
        return aligned;
    }

    private static int classifyCoplanarIntervalRelation(
        CoplanarAngularInterval a,
        CoplanarAngularInterval b)
    {
        CoplanarAngularInterval alignedB;
        double overlap;

        if ( a == null || b == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        alignedB = alignCoplanarInterval(b, a.interior);
        overlap = Math.min(a.end, alignedB.end) -
            Math.max(a.start, alignedB.start);

        if ( overlap > numericContext.angleTolerance() ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_OVERLAP;
        }
        if ( overlap >= -numericContext.angleTolerance() ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_TOUCHING;
        }
        return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
            .COPLANAR_DISJOINT;
    }

    private static CoplanarAngularInterval buildIntervalForHalfEdgeSector(
        CoplanarAngleBasis basis,
        _PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3Dd ref1;
        Vector3Dd ref2;
        Vector3Dd probe;

        if ( he == null || he.startingVertex == null || he.previous() == null ||
             he.next() == null || he.previous().startingVertex == null ||
             he.next().startingVertex == null ) {
            return null;
        }

        ref1 = he.previous().startingVertex.position.subtract(
            he.startingVertex.position);
        ref2 = he.next().startingVertex.position.subtract(
            he.startingVertex.position);
        probe = PolyhedralBoundedSolidSetOperator.inside(he);
        if ( probe == null || probe.length() <=
             numericContext.unitVectorTolerance() ) {
            probe = selectSectorInteriorProbe(ref1, ref2, ref1.crossProduct(ref2),
                null);
        }
        return buildCoplanarAngularInterval(basis, ref1, ref2, probe);
    }

    private static CoplanarAngularInterval buildIntervalForVertexSector(
        CoplanarAngleBasis basis,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex sector)
    {
        Vector3Dd fallbackProbe;
        Vector3Dd probe;

        if ( sector == null ) {
            return null;
        }

        fallbackProbe = null;
        if ( sector.he != null ) {
            fallbackProbe = PolyhedralBoundedSolidSetOperator.inside(sector.he);
        }
        probe = selectSectorInteriorProbe(sector.ref1, sector.ref2,
            sector.ref12, fallbackProbe);
        return buildCoplanarAngularInterval(basis, sector.ref1, sector.ref2,
            probe);
    }

    private static CoplanarAngularInterval buildIntervalForCoplanarEdge(
        CoplanarAngleBasis basis,
        _PolyhedralBoundedSolidHalfEdge edge,
        Vector3Dd faceNormal)
    {
        Vector3Dd edgeDirection;
        Vector3Dd inward;

        if ( edge == null || edge.startingVertex == null || edge.next() == null ||
             edge.next().startingVertex == null ) {
            return null;
        }

        edgeDirection = edge.next().startingVertex.position.subtract(
            edge.startingVertex.position);
        inward = faceNormal.crossProduct(edgeDirection);
        return buildCoplanarAngularInterval(basis, edgeDirection,
            edgeDirection.multiply(-1.0), inward);
    }

    private static int classifySectorAgainstReferenceVertex(
        CoplanarAngularInterval currentInterval,
        CoplanarAngleBasis basis,
        _PolyhedralBoundedSolidFace referenceFace,
        _PolyhedralBoundedSolidVertex referenceVertex)
    {
        int i;
        int j;
        int bestRelation;
        CoplanarAngularInterval referenceInterval;

        if ( currentInterval == null || referenceFace == null ||
             referenceVertex == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        bestRelation =
            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;

        for ( i = 0; i < referenceFace.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge he;
            _PolyhedralBoundedSolidHalfEdge heStart;

            heStart = referenceFace.boundariesList.get(i).boundaryStartHalfEdge;
            if ( heStart == null ) {
                continue;
            }

            he = heStart;
            do {
                if ( he.startingVertex == referenceVertex ) {
                    referenceInterval = buildIntervalForHalfEdgeSector(basis,
                        he);
                    j = classifyCoplanarIntervalRelation(currentInterval,
                        referenceInterval);
                    if ( j > bestRelation ) {
                        bestRelation = j;
                    }
                    if ( bestRelation ==
                         _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                             .COPLANAR_OVERLAP ) {
                        return bestRelation;
                    }
                }
                he = he.next();
            } while ( he != heStart );
        }

        return bestRelation;
    }

    private static void registerCoplanarRelation(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int index, int relation)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;

        n = nbr.get(index);
        if ( relation > n.coplanarRelation ) {
            n.coplanarRelation = relation;
        }
    }

    private static int coplanarOpIndex(int op)
    {
        if ( op == UNION ) {
            return COPLANAR_OP_UNION;
        }
        if ( op == SUBTRACT) {
            return COPLANAR_OP_DIFFERENCE;
        }
        return COPLANAR_OP_INTERSECTION;
    }

    private static int coplanarSideIndex(int BvsA)
    {
        return (BvsA == 0) ? COPLANAR_SIDE_A_VS_B : COPLANAR_SIDE_B_VS_A;
    }

    private static int coplanarOrientationIndex(boolean sameOrientation)
    {
        return sameOrientation ? COPLANAR_ORIENTATION_SAME :
            COPLANAR_ORIENTATION_OPPOSITE;
    }

    static int classifyCoplanarSectorRelation(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace sectorInfo,
        _PolyhedralBoundedSolidFace referenceFace)
    {
        _PolyhedralBoundedSolidHalfEdge he;
        Vector3Dd start;
        CoplanarAngleBasis basis;
        CoplanarAngularInterval currentInterval;
        int status;
        _PolyhedralBoundedSolidFace.PointInsideResult containment;
        _PolyhedralBoundedSolidHalfEdge intersectedHalfedge;
        _PolyhedralBoundedSolidVertex intersectedVertex;

        if ( sectorInfo == null || referenceFace == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        he = sectorInfo.sector;
        if ( he == null || he.startingVertex == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        start = he.startingVertex.position;
        containment = pointInFaceDetailed(referenceFace, start);
        status = containment.status();

        if ( status == Geometry.INSIDE ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_OVERLAP;
        }
        if ( status != Geometry.LIMIT ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        basis = buildCoplanarAngleBasis(
            he.parentLoop.parentFace.getContainingPlane().getNormal(),
            he.next().startingVertex.position.subtract(start),
            he.previous().startingVertex.position.subtract(start));
        currentInterval = buildIntervalForHalfEdgeSector(basis, he);
        if ( currentInterval == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_TOUCHING;
        }

        intersectedHalfedge = containment.intersectedHalfedge();
        if ( intersectedHalfedge != null ) {
            return classifyCoplanarIntervalRelation(currentInterval,
                buildIntervalForCoplanarEdge(basis,
                    intersectedHalfedge,
                    referenceFace.getContainingPlane().getNormal()));
        }
        intersectedVertex = containment.intersectedVertex();
        if ( intersectedVertex != null ) {
            status = classifySectorAgainstReferenceVertex(currentInterval, basis,
                referenceFace, intersectedVertex);
            if ( status !=
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                     .COPLANAR_DISJOINT ) {
                return status;
            }
        }
        return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
            .COPLANAR_TOUCHING;
    }

    private static int resolveCoplanarSectorClass(int op, int BvsA,
                                                  boolean sameOrientation)
    {
        return COPLANAR_VERTEX_FACE_CLASS_TABLE[coplanarOpIndex(op)]
            [coplanarSideIndex(BvsA)]
            [coplanarOrientationIndex(sameOrientation)];
    }
}
