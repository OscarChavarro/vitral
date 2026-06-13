//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import vsdk.toolkit.common.statistics.PolyhedralBoundedSolidStatistics;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Connect stage (big phase 3) for set operations: null-edges pairing and joins,
following section [MANT1988].15.7 and programs [MANT1988].15.13 and
[MANT1988].15.14.
*/
final class _PolyhedralBoundedSolidSetNullEdgesConnector
    extends _PolyhedralBoundedSolidOperator
{
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";
    private static final String KEEP_INSERTION_ORDER_PROPERTY =
        "vsdk.setop.connect.keepInsertionOrder";
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_05_CONNECT = 0x10;
    private static final int DEBUG_99_SHOWOPERATIONS = 0x40;
    private static final int ENDPOINT_SOLID_A = 0;
    private static final int ENDPOINT_SOLID_B = 1;

    static final class ConnectResult
    {
        private final ArrayList<_PolyhedralBoundedSolidFace> sonfa;
        private final ArrayList<_PolyhedralBoundedSolidFace> sonfb;

        private ConnectResult(ArrayList<_PolyhedralBoundedSolidFace> sonfa,
                              ArrayList<_PolyhedralBoundedSolidFace> sonfb)
        {
            this.sonfa = sonfa;
            this.sonfb = sonfb;
        }

        ArrayList<_PolyhedralBoundedSolidFace> sonfa()
        {
            return sonfa;
        }

        ArrayList<_PolyhedralBoundedSolidFace> sonfb()
        {
            return sonfb;
        }
    }

    /**
    Mutable carrier for the (nea, neb) pair returned by
    {@link #sgetnextnulledge(NullEdgePair)}, mirroring the out-param
    style used by [MANT1988] Program 15.14 in C.
    */
    private static final class NullEdgePair
    {
        _PolyhedralBoundedSolidSetOperatorNullEdge nea;
        _PolyhedralBoundedSolidSetOperatorNullEdge neb;
        int pairIndex;
    }

    private static int debugFlags;
    private static int operation;
    private int nextNullEdgeIndex;
    private ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;
    private ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsa;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsb;
    private ArrayList<_PolyhedralBoundedSolidFace> sonfa;
    private ArrayList<_PolyhedralBoundedSolidFace> sonfb;
    private static Map<Integer, Integer> sonfaPairIndexByFaceId;
    private static Map<Integer, Integer> sonfbPairIndexByFaceId;
    private static int lastLooseACount;
    private static int lastLooseBCount;
    private static int lastSonfaCount;
    private static int lastSonfbCount;
    private static int lastPairCount;
    private static int currentConnectPairIndex;
    private static int nextSyntheticPairIndex;

    /**
    Structural report of the intersection curves reconstructed from the
    sonea/soneb pairs of the most recent {@link #connect} call. Captured
    unconditionally (the reconstruction is cheap) so tests and diagnostics
    can audit curve closure without enabling pipeline traces.
    */
    static _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report
        lastCurveReport;

    /**
    Test-only injection point: when non-null and matching the pair count,
    {@link #sortNullEdges()} reorders sonea/soneb by this permutation
    (entry {@code position -> originalIndex}) instead of the production
    ordering. Lets contract tests probe the connect stage's sensitivity to
    processing order without any production flag. Always null in production;
    cleared by the caller after use.
    */
    static int[] testOnlyForcedConnectOrder;

    /**
    True when {@link #sortNullEdges()} already oriented every strut along
    the intersection curves (mythosPlan §5.3). The connect loop must then
    respect that orientation instead of re-normalizing edge halves by
    vertex-id comparison (the legacy rule, which encodes classifier emission
    order and breaks when that order is not the curve order).
    */
    private static boolean curveOrientationApplied;

    /**
    Junction adjacency in processing-position space when the curve order is
    active (from
    {@code _PolyhedralBoundedSolidSetIntersectionCurveBuilder}); null
    otherwise. Used to restrict the near-miss ring rescue to true curve
    neighbors.
    */
    private static int[][] curveNeighborPositions;

    /**
    Processing pair index that pushed each loose entry, parallel to
    {@code endsa}/{@code endsb}.
    */
    private static ArrayList<Integer> endsPairIndex;

    private static boolean isPipelineSummaryTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static boolean isKeepInsertionOrderEnabled()
    {
        String propertyValue;

        propertyValue = System.getProperty(KEEP_INSERTION_ORDER_PROPERTY);
        if ( propertyValue == null ) {
            return true;
        }
        return Boolean.parseBoolean(propertyValue);
    }

    private static void tracePipelineSummary(String message)
    {
        if ( !isPipelineSummaryTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpPipelineTrace] " + message);
    }

    private static String summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he == null ) {
            return "null";
        }

        String from = "?";
        String to = "?";
        String face = "?";

        if ( he.startingVertex != null ) {
            from = Integer.toString(he.startingVertex.id);
        }
        if ( he.parentLoop != null ) {
            _PolyhedralBoundedSolidHalfEdge next = he.next();
            if ( next != null && next.startingVertex != null ) {
                to = Integer.toString(next.startingVertex.id);
            }
            if ( he.parentLoop.parentFace != null ) {
                face = Integer.toString(he.parentLoop.parentFace.id);
            }
        }
        String fromPoint = "?";
        String toPoint = "?";

        if ( he.startingVertex != null ) {
            fromPoint = he.startingVertex.position.toString();
        }
        if ( he.parentLoop != null ) {
            _PolyhedralBoundedSolidHalfEdge next = he.next();
            if ( next != null && next.startingVertex != null ) {
                toPoint = next.startingVertex.position.toString();
            }
        }
        return "he(v=" + from + "->" + to + ",f=" + face +
            ",p=" + fromPoint + "->" + toPoint + ")";
    }

    private static String summarizeNullEdge(
        _PolyhedralBoundedSolidSetOperatorNullEdge edge)
    {
        if ( edge == null || edge.e == null ) {
            return "null";
        }
        return summarizeHalfEdge(edge.e.rightHalf) + " | " +
            summarizeHalfEdge(edge.e.leftHalf);
    }

    private static void setCurrentConnectContext(int pairIndex)
    {
        currentConnectPairIndex = pairIndex;
    }

    private static int allocateSyntheticPairIndex()
    {
        int pairIndex;

        pairIndex = nextSyntheticPairIndex;
        nextSyntheticPairIndex++;
        return pairIndex;
    }

    static int getSonfaPairIndex(_PolyhedralBoundedSolidFace face)
    {
        Integer pairIndex;

        if ( face == null || sonfaPairIndexByFaceId == null ) {
            return -1;
        }
        pairIndex = sonfaPairIndexByFaceId.get(Integer.valueOf(face.id));
        return pairIndex != null ? pairIndex.intValue() : -1;
    }

    static int getSonfbPairIndex(_PolyhedralBoundedSolidFace face)
    {
        Integer pairIndex;

        if ( face == null || sonfbPairIndexByFaceId == null ) {
            return -1;
        }
        pairIndex = sonfbPairIndexByFaceId.get(Integer.valueOf(face.id));
        return pairIndex != null ? pairIndex.intValue() : -1;
    }

    private static String summarizeLooseEnds(
        ArrayList<_PolyhedralBoundedSolidHalfEdge> endsA,
        ArrayList<_PolyhedralBoundedSolidHalfEdge> endsB)
    {
        StringBuilder out = new StringBuilder();
        int i;

        out.append("pairs=").append(endsA.size()).append(" [");
        for ( i = 0; i < endsA.size() && i < endsB.size(); i++ ) {
            if ( i > 0 ) {
                out.append(" | ");
            }
            out.append(i)
               .append(":A=")
               .append(summarizeHalfEdge(endsA.get(i)))
               .append(",B=")
               .append(summarizeHalfEdge(endsB.get(i)));
        }
        out.append("]");
        return out.toString();
    }


    private static boolean isSamePoint(_PolyhedralBoundedSolidHalfEdge first,
                                       _PolyhedralBoundedSolidHalfEdge second)
    {
        if ( first == null || second == null ||
             first.startingVertex == null || second.startingVertex == null ) {
            return false;
        }
        return PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
            first.startingVertex.position,
            second.startingVertex.position,
            numericContext);
    }

    private static boolean canCutCoincidentHalfEdge(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidLoop loop;

        if ( he == null ) {
            return false;
        }
        edge = he.parentEdge;
        loop = he.parentLoop;
        if ( edge == null || loop == null ) {
            return false;
        }
        if ( edge.rightHalf == null || edge.leftHalf == null ) {
            return false;
        }
        if ( edge.rightHalf.parentLoop != edge.leftHalf.parentLoop ) {
            return true;
        }
        return loop.halfEdgesList.size() > 2;
    }

    private static boolean hasReusableCoincidentCutFace(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidFace face;

        if ( he == null || he.parentLoop == null ) {
            return false;
        }
        face = he.parentLoop.parentFace;
        return face != null && face.boundariesList.size() > 1;
    }

    private static boolean canCutCoincidentFinishFace(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidLoop loop;

        if ( he == null ) {
            return false;
        }
        edge = he.parentEdge;
        loop = he.parentLoop;
        if ( edge == null || loop == null ||
             edge.rightHalf == null || edge.leftHalf == null ||
             edge.rightHalf.parentLoop == null ||
             edge.leftHalf.parentLoop == null ) {
            return false;
        }
        // Classic case: both halves in the same loop and loop large enough for
        // an interior cut.
        if ( edge.rightHalf.parentLoop == edge.leftHalf.parentLoop ) {
            return loop.halfEdgesList.size() > 2;
        }
        // Cross-loop intersection edge whose halves still share the same face
        // (typical for null-edges produced by the intersect+classify pipeline
        // on tessellated curved surfaces). Allowing this case prevents
        // `closeLegacyCoincidentLooseEnds` from rejecting legitimate pairs and
        // leaving the integration ring incomplete.
        return edge.rightHalf.parentLoop.parentFace ==
               edge.leftHalf.parentLoop.parentFace;
    }

    private static _PolyhedralBoundedSolidFace registerCoincidentCutFace(
        _PolyhedralBoundedSolidHalfEdge he,
        ArrayList<_PolyhedralBoundedSolidFace> target,
        String label)
    {
        _PolyhedralBoundedSolidFace face;

        if ( he == null || he.parentLoop == null ) {
            return null;
        }
        face = he.parentLoop.parentFace;
        if ( face == null || face.boundariesList.size() <= 1 ) {
            return null;
        }
        target.add(face);
        return face;
    }



    private static boolean canFinalizeCoincidentLooseA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return canCutCoincidentFinishFace(he) ||
            hasReusableCoincidentCutFace(he);
    }

    private static boolean canFinalizeCoincidentLooseB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return canCutCoincidentFinishFace(he) ||
            hasReusableCoincidentCutFace(he);
    }

    private static boolean isPointLikeHalfEdge(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return isLiveHalfEdge(he) &&
            he.next() != null &&
            he.next().startingVertex != null &&
            isSamePoint(he, he.next());
    }

    private static boolean isDegenerateCoincidentLooseClosure(
        _PolyhedralBoundedSolidHalfEdge firstA,
        _PolyhedralBoundedSolidHalfEdge secondA,
        _PolyhedralBoundedSolidHalfEdge firstB,
        _PolyhedralBoundedSolidHalfEdge secondB)
    {
        return isPointLikeHalfEdge(firstA) &&
            isPointLikeHalfEdge(secondA) &&
            isPointLikeHalfEdge(firstB) &&
            isPointLikeHalfEdge(secondB);
    }

    private void finalizeCoincidentLooseA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( canCutCoincidentFinishFace(he) ) {
            cutA(he);
        }
        else {
            registerCoincidentCutFace(he, sonfa, "reuse-sonfa");
        }
    }

    private void finalizeCoincidentLooseB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( canCutCoincidentFinishFace(he) ) {
            cutB(he);
        }
        else {
            registerCoincidentCutFace(he, sonfb, "reuse-sonfb");
        }
    }

    private static void removeLoosePair(int index)
    {
        endsa.remove(index);
        endsb.remove(index);
    }


    static int getLastLooseACount()
    {
        return lastLooseACount;
    }

    static int getLastLooseBCount()
    {
        return lastLooseBCount;
    }

    static int getLastSonfaCount()
    {
        return lastSonfaCount;
    }

    static int getLastSonfbCount()
    {
        return lastSonfbCount;
    }

    static int getLastPairCount()
    {
        return lastPairCount;
    }

    private void updateLastSnapshot()
    {
        lastLooseACount = (endsa != null) ? endsa.size() : 0;
        lastLooseBCount = (endsb != null) ? endsb.size() : 0;
        lastSonfaCount = (sonfa != null) ? sonfa.size() : 0;
        lastSonfbCount = (sonfb != null) ? sonfb.size() : 0;
    }

    private static boolean isLiveHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        return he != null &&
            he.parentEdge != null &&
            he.parentLoop != null &&
            he.parentLoop.parentFace != null;
    }

    private static boolean sharesParentFace(
        _PolyhedralBoundedSolidHalfEdge first,
        _PolyhedralBoundedSolidHalfEdge second)
    {
        return isLiveHalfEdge(first) &&
            isLiveHalfEdge(second) &&
            first.parentLoop.parentFace == second.parentLoop.parentFace;
    }


    private static boolean isOppositeHalfEdgeSide(
        _PolyhedralBoundedSolidHalfEdge first,
        _PolyhedralBoundedSolidHalfEdge second)
    {
        if ( first == null || second == null ||
             first.parentEdge == null || second.parentEdge == null ) {
            return false;
        }

        return (first == first.parentEdge.rightHalf &&
                second == second.parentEdge.leftHalf) ||
               (first == first.parentEdge.leftHalf &&
                second == second.parentEdge.rightHalf);
    }

    private static _PolyhedralBoundedSolidFace
    findUniqueClassicRebindTargetFace(
        _PolyhedralBoundedSolidHalfEdge currentTargetFirst,
        _PolyhedralBoundedSolidHalfEdge currentTargetSecond,
        _PolyhedralBoundedSolidHalfEdge currentReferenceFirst,
        _PolyhedralBoundedSolidHalfEdge currentReferenceSecond,
        ArrayList<_PolyhedralBoundedSolidHalfEdge> targetEnds,
        ArrayList<_PolyhedralBoundedSolidHalfEdge> referenceEnds)
    {
        _PolyhedralBoundedSolidFace candidateFace = null;
        int i;
        int j;
        int pairCount;

        if ( !isLiveHalfEdge(currentTargetFirst) ||
             !isLiveHalfEdge(currentTargetSecond) ||
             !isLiveHalfEdge(currentReferenceFirst) ||
             !isLiveHalfEdge(currentReferenceSecond) ||
             targetEnds == null || referenceEnds == null ) {
            return null;
        }

        pairCount = Math.min(targetEnds.size(), referenceEnds.size());
        for ( i = 0; i < pairCount; i++ ) {
            _PolyhedralBoundedSolidHalfEdge looseTargetFirst;
            _PolyhedralBoundedSolidHalfEdge looseReferenceFirst;

            looseTargetFirst = targetEnds.get(i);
            looseReferenceFirst = referenceEnds.get(i);
            if ( !isLiveHalfEdge(looseTargetFirst) ||
                 !isLiveHalfEdge(looseReferenceFirst) ||
                 !isOppositeHalfEdgeSide(
                     currentTargetFirst, looseTargetFirst) ||
                 !neighbor(currentReferenceFirst, looseReferenceFirst) ) {
                continue;
            }

            for ( j = 0; j < pairCount; j++ ) {
                _PolyhedralBoundedSolidHalfEdge looseTargetSecond;
                _PolyhedralBoundedSolidHalfEdge looseReferenceSecond;
                _PolyhedralBoundedSolidFace looseFace;

                if ( i == j ) {
                    continue;
                }

                looseTargetSecond = targetEnds.get(j);
                looseReferenceSecond = referenceEnds.get(j);
                if ( !isLiveHalfEdge(looseTargetSecond) ||
                     !isLiveHalfEdge(looseReferenceSecond) ||
                     !isOppositeHalfEdgeSide(
                         currentTargetSecond, looseTargetSecond) ||
                     !neighbor(currentReferenceSecond,
                         looseReferenceSecond) ) {
                    continue;
                }

                looseFace = looseTargetFirst.parentLoop.parentFace;
                if ( looseFace != looseTargetSecond.parentLoop.parentFace ||
                     looseFace == currentTargetFirst.parentLoop.parentFace ) {
                    continue;
                }

                if ( candidateFace == null ) {
                    candidateFace = looseFace;
                }
                else if ( candidateFace != looseFace ) {
                    return null;
                }
            }
        }
        return candidateFace;
    }

    private static boolean isStrictRebindTargetFace(
        _PolyhedralBoundedSolidFace targetFace,
        _PolyhedralBoundedSolidHalfEdge currentTarget)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext;
        Vector3Dd point;

        if ( targetFace == null ||
             !isLiveHalfEdge(currentTarget) ||
             currentTarget.startingVertex == null ||
             targetFace.getContainingPlane() == null ) {
            return false;
        }

        numericContext = PolyhedralBoundedSolidNumericPolicy.forFace(targetFace);
        point = currentTarget.startingVertex.position;
        if ( Math.abs(targetFace.getContainingPlane().pointDistance(point)) >
             numericContext.bigEpsilon() ) {
            return false;
        }

        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .pointInFace(targetFace, point) == Geometry.INSIDE;
    }

    private static void rebindClassicCurrentNullEdgeIfNeeded(
        _PolyhedralBoundedSolidHalfEdge currentTargetFirst,
        _PolyhedralBoundedSolidHalfEdge currentTargetSecond,
        _PolyhedralBoundedSolidHalfEdge currentReferenceFirst,
        _PolyhedralBoundedSolidHalfEdge currentReferenceSecond,
        ArrayList<_PolyhedralBoundedSolidHalfEdge> targetEnds,
        ArrayList<_PolyhedralBoundedSolidHalfEdge> referenceEnds,
        String label)
    {
        _PolyhedralBoundedSolidFace targetFace;
        _PolyhedralBoundedSolidFace sourceFace;
        _PolyhedralBoundedSolidLoop sourceLoop;
        PolyhedralBoundedSolid solid;

        if ( operation != SUBTRACT ||
             !isLiveHalfEdge(currentTargetFirst) ||
             !isLiveHalfEdge(currentTargetSecond) ||
             currentTargetFirst.parentLoop != currentTargetSecond.parentLoop ) {
            return;
        }

        sourceLoop = currentTargetFirst.parentLoop;
        sourceFace = sourceLoop.parentFace;
        if ( sourceLoop.halfEdgesList.size() != 2 ) {
            return;
        }

        targetFace = findUniqueClassicRebindTargetFace(
            currentTargetFirst, currentTargetSecond,
            currentReferenceFirst, currentReferenceSecond,
            targetEnds, referenceEnds);
        if ( targetFace == null ||
             targetFace == sourceFace ||
             !isStrictRebindTargetFace(targetFace, currentTargetFirst) ) {
            return;
        }

        solid = currentTargetFirst.parentLoop.parentFace.parentSolid;
        if ( !PolyhedralBoundedSolidEulerOperators.lringmv(
                 solid, sourceLoop, targetFace, false) ) {
            tracePipelineSummary(
                "connect rebind" + label + " failed sourceFace=" +
                sourceFace.id + " targetFace=" +
                targetFace.id + " edge=" +
                summarizeHalfEdge(currentTargetFirst));
            return;
        }
        tracePipelineSummary(
            "connect rebind" + label + " sourceFace=" +
            sourceFace.id + " targetFace=" +
            targetFace.id + " edge=" +
            summarizeHalfEdge(currentTargetFirst));
    }

    private static void rebindClassicCurrentNullEdgesIfNeeded(
        _PolyhedralBoundedSolidHalfEdge currentARight,
        _PolyhedralBoundedSolidHalfEdge currentALeft,
        _PolyhedralBoundedSolidHalfEdge currentBLeft,
        _PolyhedralBoundedSolidHalfEdge currentBRight)
    {
        rebindClassicCurrentNullEdgeIfNeeded(
            currentARight, currentALeft,
            currentBLeft, currentBRight,
            endsa, endsb, "A");
        rebindClassicCurrentNullEdgeIfNeeded(
            currentBLeft, currentBRight,
            currentARight, currentALeft,
            endsb, endsa, "B");
    }


    ConnectResult connect(
        int op,
        int flags,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSonea,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSoneb)
    {
        operation = op;
        debugFlags = flags;
        sonea = inSonea;
        soneb = inSoneb;
        lastPairCount = Math.min(sonea.size(), soneb.size());
        lastLooseACount = 0;
        lastLooseBCount = 0;
        lastSonfaCount = 0;
        lastSonfbCount = 0;
        nextSyntheticPairIndex = lastPairCount;
        // [MANT1988] §15.7 Program 15.14: single Connect implementation.
        // The "flexibleEndpointChains" alternative path was removed in §6.1
        // of plan-csg-boolean-fix-stage2 because it duplicated setOpConnect
        // with extra heuristics that the book does not require.
        setOpConnect();
        return new ConnectResult(sonfa, sonfb);
    }

    private void sortNullEdges()
    {
        curveOrientationApplied = false;
        curveNeighborPositions = null;
        if ( testOnlyForcedConnectOrder != null &&
             testOnlyForcedConnectOrder.length == sonea.size() &&
             testOnlyForcedConnectOrder.length == soneb.size() ) {
            applyOrderPermutation(testOnlyForcedConnectOrder);
            lastCurveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
                .build(sonea, soneb,
                    PolyhedralBoundedSolidNumericPolicy.defaultContext()
                        .unitVectorTolerance());
            tracePipelineSummary("connect TEST-ONLY forced order applied");
            return;
        }

        // Always group null-edges by topological ring before any further
        // processing. Without this, the connect loop pairs null-edges from
        // different intersection curves (e.g., the outer and inner boundary
        // circles of a spherical shell), producing non-coplanar faces.
        // Ring grouping is safe for the single-ring case (it is a no-op).
        groupNullEdgesByRing();

        if ( isKeepInsertionOrderEnabled() ) {
            tracePipelineSummary("connect sort skipped; using insertion order");
            return;
        }

        // Geometric sort within each ring, enabled only when keepInsertionOrder
        // is explicitly disabled via the system property.
        Collections.sort(sonea);
        Collections.sort(soneb);
    }

    private static void dbgDumpNullEdges(String label,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone)
    {
        int k;
        for ( k = 0; k < sone.size(); k++ ) {
            _PolyhedralBoundedSolidSetOperatorNullEdge ne = sone.get(k);
            _PolyhedralBoundedSolidHalfEdge rh = ne.e.rightHalf;
            _PolyhedralBoundedSolidHalfEdge lh = ne.e.leftHalf;
            System.out.println("[DBG-ne] " + label + "[" + k + "] "
                + "R{v=" + rh.startingVertex.id
                + " f=" + rh.parentLoop.parentFace.id
                + " p=(" + String.format("%.4f,%.4f,%.4f",
                    rh.startingVertex.position.x(),
                    rh.startingVertex.position.y(),
                    rh.startingVertex.position.z()) + ")}"
                + " L{v=" + lh.startingVertex.id
                + " f=" + lh.parentLoop.parentFace.id
                + " p=(" + String.format("%.4f,%.4f,%.4f",
                    lh.startingVertex.position.x(),
                    lh.startingVertex.position.y(),
                    lh.startingVertex.position.z()) + ")}");
        }
    }

    /**
    Reorders sonea/soneb in lockstep by the given permutation
    ({@code position -> originalIndex}).
    @param order permutation covering every index exactly once
    */
    private void applyOrderPermutation(int[] order)
    {
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> orderedA;
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> orderedB;
        int i;

        orderedA = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        orderedB = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        for ( i = 0; i < order.length; i++ ) {
            orderedA.add(sonea.get(order[i]));
            orderedB.add(soneb.get(order[i]));
        }
        sonea.clear();
        sonea.addAll(orderedA);
        soneb.clear();
        soneb.addAll(orderedB);
    }

    private static int curveComponentFind(int[] parent, int x)
    {
        while ( parent[x] != x ) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static long[] nullEdgeFaceIds(
        _PolyhedralBoundedSolidSetOperatorNullEdge ne)
    {
        return new long[] {
            ne.e.rightHalf.parentLoop.parentFace.id,
            ne.e.leftHalf.parentLoop.parentFace.id
        };
    }

    private static boolean nullEdgesShareFace(long[] a, long[] b)
    {
        return a[0] == b[0] || a[0] == b[1] || a[1] == b[0] || a[1] == b[1];
    }

    /**
    Reorders {@code sonea}/{@code soneb} so that null-edge pairs belonging to the
    same intersection curve are contiguous and ordered for {@code scanjoin}, and
    distinct curves are separated.

    <p>Each intersection curve is recovered as a connected component over the
    paired null-edge indices: pairs {@code k}, {@code k'} are adjacent when their
    A-null-edges share a face <b>or</b> their B-null-edges share a face — pure
    topological adjacency along the curve as it crosses from face to face. The
    null-edges are zero-length struts whose two endpoints sit at the same point,
    so consecutive struts share a <em>face</em> (not a vertex id and not a
    position); face adjacency is therefore the correct connectivity.</p>

    <p>Within each component the classifier's emission order is preserved — that
    order is the curve-traversal order {@code scanjoin} (Program 15.13) requires.
    Components are emitted by ascending lowest member index (stable). Pairs stay
    index-aligned ({@code sonea[k]} with {@code soneb[k]}) because the same index
    permutation is applied to both lists.</p>

    <p>This replaces the former vertex-id ring partition plus spatial signature
    sort: the struts never share a vertex id, so that partition produced only
    singleton rings and the signature sort then scrambled the curve order.</p>
    */
    private void groupNullEdgesByRing()
    {
        if ( isPipelineSummaryTraceEnabled() ) {
            dbgDumpNullEdges("A", sonea);
            dbgDumpNullEdges("B", soneb);
        }

        // Reconstruct the intersection curves for diagnosis (§5 of
        // doc/mythosPlan.md). The report is captured before any reordering
        // decision so it always describes the classifier's raw emission.
        lastCurveReport = _PolyhedralBoundedSolidSetIntersectionCurveBuilder
            .build(sonea, soneb,
                PolyhedralBoundedSolidNumericPolicy.defaultContext()
                    .unitVectorTolerance());
        tracePipelineSummary("connect " + lastCurveReport.summarize());

        int n = Math.min(sonea.size(), soneb.size());
        if ( n != sonea.size() || n != soneb.size() || n < 2 ) {
            return;
        }

        // mythosPlan §5.3 (Phase 2): when every null-edge pair lies on a
        // cleanly closed intersection curve, reorder pairs along each curve
        // AND orient every strut consistently with the traversal. Probe
        // evidence (mythosPlan §9, 2026-06-10): order alone is insufficient
        // — the legacy in-loop vertex-id normalization encodes classifier
        // emission order, so curve order with mismatched strut orientation
        // leaves loose ends at the seams. With both applied, scanjoin closes
        // every consecutive pair by construction (derivation in
        // _PolyhedralBoundedSolidSetIntersectionCurveBuilder.applyCurveOrientation).
        // On any anomaly (open chain, isolated node, pinch, odd/degenerate
        // face-pair group) fall through to the legacy ordering below.
        if ( lastCurveReport.isCleanlyClosed() ) {
            int[] curveOrder =
                _PolyhedralBoundedSolidSetIntersectionCurveBuilder
                    .orderAndOrientAlongCurves(lastCurveReport.cycles, n,
                        sonea, soneb);
            if ( curveOrder != null ) {
                applyOrderPermutation(curveOrder);
                curveOrientationApplied = true;
                curveNeighborPositions =
                    _PolyhedralBoundedSolidSetIntersectionCurveBuilder
                        .lastTraversalNeighborPositions;
                tracePipelineSummary("connect curve-order applied: cycles="
                    + lastCurveReport.cycles.size());
                return;
            }
        }

        // When all null-edge rings are singletons (every pair is a zero-length
        // strut), there is no ring structure to separate, and a spatial
        // signature sort only scrambles the classifier's already-valid emission
        // order. Preserve insertion order in that case (fixes all 20 star motifs).
        // For cases where at least one proper ring exists (e.g. the shell-cylinder
        // intersection), fall through to the connected-component sort.
        long[][] faceIdsA = new long[n][];
        long[][] faceIdsB = new long[n][];
        int k;
        for ( k = 0; k < n; k++ ) {
            faceIdsA[k] = nullEdgeFaceIds(sonea.get(k));
            faceIdsB[k] = nullEdgeFaceIds(soneb.get(k));
        }

        int[] parent = new int[n];
        for ( k = 0; k < n; k++ ) {
            parent[k] = k;
        }
        int i;
        int j;
        for ( i = 0; i < n; i++ ) {
            for ( j = i + 1; j < n; j++ ) {
                if ( nullEdgesShareFace(faceIdsA[i], faceIdsA[j]) ||
                     nullEdgesShareFace(faceIdsB[i], faceIdsB[j]) ) {
                    parent[curveComponentFind(parent, i)] =
                        curveComponentFind(parent, j);
                }
            }
        }

        // Count distinct curve components.
        java.util.HashSet<Integer> roots =
            new java.util.HashSet<Integer>();
        for ( k = 0; k < n; k++ ) {
            roots.add(curveComponentFind(parent, k));
        }
        int componentCount = roots.size();

        // Decide whether to reorder.
        // Two null-edges that share a vertex ID are topologically adjacent on
        // the same intersection curve (the vertex is the meeting point of two
        // consecutive null-edges). When ALL null-edges are isolated (no shared
        // vertex IDs — they are zero-length struts each at a distinct vertex),
        // the classifier's emission order is already the best available curve
        // order and reordering would only scramble it (this is the case for all
        // star-prism motifs AND for most moon motifs). Only reorder when at
        // least one shared vertex exists, which signals a multi-curve
        // intersection where distinct curves must be kept contiguous (e.g.
        // the shell-cylinder construction).
        //
        // NOTE: this check deliberately uses vertex IDs (not positions) because
        // after weldCoincidentVertices the coincident pairs are already merged;
        // two null-edges that are truly adjacent on the curve share the same
        // vertex object, not just the same position.
        java.util.HashSet<Integer> vidsA =
            new java.util.HashSet<Integer>();
        boolean hasSharedA = false;
        int ki;
        for ( ki = 0; ki < n && !hasSharedA; ki++ ) {
            int v1 = sonea.get(ki).e.rightHalf.startingVertex.id;
            int v2 = sonea.get(ki).e.leftHalf.startingVertex.id;
            if ( !vidsA.add(v1) || (v1 != v2 && !vidsA.add(v2)) ) {
                hasSharedA = true;
            }
        }
        boolean hasSharedB = false;
        if ( !hasSharedA ) {
            java.util.HashSet<Integer> vidsB =
                new java.util.HashSet<Integer>();
            for ( ki = 0; ki < n && !hasSharedB; ki++ ) {
                int v1 = soneb.get(ki).e.rightHalf.startingVertex.id;
                int v2 = soneb.get(ki).e.leftHalf.startingVertex.id;
                if ( !vidsB.add(v1) || (v1 != v2 && !vidsB.add(v2)) ) {
                    hasSharedB = true;
                }
            }
        }
        if ( !hasSharedA && !hasSharedB ) {
            tracePipelineSummary("connect ring-group: all singletons; "
                + "preserving insertion order");
            return;
        }

        // Multiple pairs share face-adjacency → reconstruct curve order.
        LinkedHashMap<Integer, ArrayList<Integer>> components =
            new LinkedHashMap<Integer, ArrayList<Integer>>();
        for ( k = 0; k < n; k++ ) {
            int root = curveComponentFind(parent, k);
            components.computeIfAbsent(root,
                r -> new ArrayList<Integer>()).add(k);
        }

        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> orderedA =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> orderedB =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        for ( ArrayList<Integer> component : components.values() ) {
            for ( int idx : component ) {
                orderedA.add(sonea.get(idx));
                orderedB.add(soneb.get(idx));
            }
        }
        sonea.clear();
        sonea.addAll(orderedA);
        soneb.clear();
        soneb.addAll(orderedB);

        tracePipelineSummary(
            "connect curve-components: count=" + componentCount);
    }

    /**
    Implements sgetnextnulledge per [MANT1988] §15.7, Program 15.14.

    <p>Advances the internal cursor over {@code sonea}/{@code soneb} and
    fills {@code out} with the next (nea, neb) pair, mirroring the
    out-param style of the C original. Returns {@code true} while pairs
    remain; {@code false} signals the {@code while} loop body to exit.</p>

    <p>The cursor is reset to {@code 0} in {@link #setOpConnect()} before
    the loop starts. Order is the parametric ordering produced by
    {@link #sortNullEdges()} (§4.3 of plan-csg-boolean-fix-stage2).</p>
    */
    private boolean sgetnextnulledge(NullEdgePair out)
    {
        if ( nextNullEdgeIndex >= sonea.size() ||
             nextNullEdgeIndex >= soneb.size() ) {
            return false;
        }
        out.nea = sonea.get(nextNullEdgeIndex);
        out.neb = soneb.get(nextNullEdgeIndex);
        out.pairIndex = nextNullEdgeIndex;
        nextNullEdgeIndex++;
        return true;
    }

    /**
    Implements scanjoin per [MANT1988] §15.7, Program 15.13.

    <p>Returns {@code {ha, hb}} (matched halves from {@code endsa[i]}/{@code endsb[i]})
    only when there is a single index {@code i} such that <b>both</b>
    {@code hea} is a neighbor of {@code endsa.get(i)} AND {@code heb} is a
    neighbor of {@code endsb.get(i)} — i.e., both null-edges can close to
    the <em>same</em> previously-loose pair. The matched pair is removed
    from {@code endsa}/{@code endsb} and returned.</p>

    <p>If no such index exists, {@code hea} and {@code heb} are appended to
    the loose lists (becoming candidates for future pairings) and
    {@code null} is returned, signalling that the caller must not perform
    {@code join}/{@code cut} for this pair.</p>
    */
    private _PolyhedralBoundedSolidHalfEdge[]
    scanjoin(_PolyhedralBoundedSolidHalfEdge hea,
             _PolyhedralBoundedSolidHalfEdge heb)
    {
        int i;
        boolean condition1;
        boolean condition2;

        for ( i = 0; i < endsa.size(); i++ ) {
            condition1 = neighbor(hea, endsa.get(i));
            condition2 = neighbor(heb, endsb.get(i));

            if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
                System.out.println("    . Testing for neighborhood A[" +
                   hea.startingVertex.id + "/" +
                   hea.next().startingVertex.id + "] vs. A[" +
                   endsa.get(i).startingVertex.id + "/" +
                   endsa.get(i).next().startingVertex.id + "]: " +
                   (condition1?"true":"false") +
                   " ParentFaces: " +
                   hea.parentLoop.parentFace.id + " / " +
                   endsa.get(i).parentLoop.parentFace.id);
                System.out.println("    . Testing for neighborhood B[" +
                   heb.startingVertex.id + "/" +
                   heb.next().startingVertex.id + "] vs. B[" +
                   endsb.get(i).startingVertex.id + "/" +
                   endsb.get(i).next().startingVertex.id + "]: " +
                   (condition2?"true":"false") +
                   " ParentFaces: " +
                   heb.parentLoop.parentFace.id + " / " +
                   endsb.get(i).parentLoop.parentFace.id);
            }

            if ( condition1 && condition2 ) {
                _PolyhedralBoundedSolidHalfEdge[] ret =
                    new _PolyhedralBoundedSolidHalfEdge[2];
                ret[0] = endsa.get(i);
                ret[1] = endsb.get(i);
                endsa.remove(i);
                endsb.remove(i);
                if ( i < endsPairIndex.size() ) {
                    endsPairIndex.remove(i);
                }
                return ret;
            }
        }

        // mythosPlan Phase 3 (curve-ordered path only): rescue a unique
        // near-miss before declaring this pair loose. When a face is
        // crossed by several chords of the intersection curve, an earlier
        // division can re-parent a pending strut ring away from the face
        // where its junction partner waits; the junction then fails the
        // neighbor face-equality forever (the EMPTY/BLACK_FACES cusp
        // moons). If exactly one loose entry matches on one solid and
        // differs ONLY by parent face on the other — with a two-half-edge
        // strut ring on the mismatched side — re-parenting that ring to
        // the partner's face restores the junction the curve order
        // guarantees. Topological information only; no geometry.
        if ( curveOrientationApplied ) {
            _PolyhedralBoundedSolidHalfEdge[] rescued =
                rescueRingFaceNearMiss(hea, heb);
            if ( rescued != null ) {
                return rescued;
            }
        }

        endsa.add(hea);
        endsb.add(heb);
        endsPairIndex.add(Integer.valueOf(currentConnectPairIndex));
        return null;
    }

    private static boolean rolesOpposite(
        _PolyhedralBoundedSolidHalfEdge h1,
        _PolyhedralBoundedSolidHalfEdge h2)
    {
        if ( h1 == null || h2 == null ||
             h1.parentEdge == null || h2.parentEdge == null ) {
            return false;
        }
        return (h1 == h1.parentEdge.rightHalf &&
                h2 == h2.parentEdge.leftHalf) ||
               (h1 == h1.parentEdge.leftHalf &&
                h2 == h2.parentEdge.rightHalf);
    }

    /**
    True when the half-edge dangles in a pending two-half-edge strut ring
    (an inner loop holding only the null edge, as created by the vertex/face
    classifier's makeRing) — the only configuration this rescue may
    re-parent.
    @param he half-edge to inspect
    @return true for a pending strut ring's half-edge
    */
    private static boolean isPendingStrutRing(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he == null || he.parentLoop == null ||
             he.parentLoop.halfEdgesList == null ||
             he.parentLoop.parentFace == null ||
             he.parentLoop.parentFace.boundariesList.size() == 0 ) {
            return false;
        }
        if ( he.parentLoop.halfEdgesList.size() != 2 ) {
            return false;
        }
        return he.parentLoop !=
            he.parentLoop.parentFace.boundariesList.get(0);
    }

    /**
    Attempts the near-miss rescue described at the scanjoin call site:
    finds the unique loose index that matches on one solid and fails only
    the face equality on the other, with a pending strut ring on the
    mismatched side; re-parents that ring and completes the match.
    @param hea query half on solid A
    @param heb query half on solid B
    @return the matched loose pair after the rescue, or null
    */
    private _PolyhedralBoundedSolidHalfEdge[]
    rescueRingFaceNearMiss(_PolyhedralBoundedSolidHalfEdge hea,
        _PolyhedralBoundedSolidHalfEdge heb)
    {
        int candidate = -1;
        boolean mismatchOnA = false;
        int i;

        for ( i = 0; i < endsa.size(); i++ ) {
            // Only true curve neighbors of the current pair may be
            // rescued: the loose entry must have been pushed by one of the
            // two cycle-adjacent pairs. Without this guard the rescue can
            // stitch a cycle seed to a leftover of another curve
            // (regressed MANT1988_15_2_HOLED, mythosPlan §9).
            if ( curveNeighborPositions == null ||
                 currentConnectPairIndex < 0 ||
                 currentConnectPairIndex >= curveNeighborPositions.length ||
                 curveNeighborPositions[currentConnectPairIndex] == null ||
                 i >= endsPairIndex.size() ) {
                continue;
            }
            int pusherPosition = endsPairIndex.get(i).intValue();
            if ( pusherPosition !=
                     curveNeighborPositions[currentConnectPairIndex][0] &&
                 pusherPosition !=
                     curveNeighborPositions[currentConnectPairIndex][1] ) {
                continue;
            }
            boolean aOk = neighbor(hea, endsa.get(i));
            boolean bOk = neighbor(heb, endsb.get(i));
            boolean nearMissA = !aOk && bOk &&
                rolesOpposite(hea, endsa.get(i)) &&
                (isPendingStrutRing(hea) ||
                 isPendingStrutRing(endsa.get(i)));
            boolean nearMissB = aOk && !bOk &&
                rolesOpposite(heb, endsb.get(i)) &&
                (isPendingStrutRing(heb) ||
                 isPendingStrutRing(endsb.get(i)));
            if ( nearMissA || nearMissB ) {
                if ( candidate >= 0 ) {
                    tracePipelineSummary(
                        "connect ring-rescue ambiguous; skipped");
                    return null;
                }
                candidate = i;
                mismatchOnA = nearMissA;
            }
        }
        if ( candidate < 0 ) {
            return null;
        }

        _PolyhedralBoundedSolidHalfEdge query =
            mismatchOnA ? hea : heb;
        _PolyhedralBoundedSolidHalfEdge stored = mismatchOnA
            ? endsa.get(candidate) : endsb.get(candidate);
        _PolyhedralBoundedSolidHalfEdge ringSide;
        _PolyhedralBoundedSolidHalfEdge anchorSide;
        if ( isPendingStrutRing(query) ) {
            ringSide = query;
            anchorSide = stored;
        }
        else {
            ringSide = stored;
            anchorSide = query;
        }
        _PolyhedralBoundedSolidFace targetFace =
            anchorSide.parentLoop.parentFace;
        if ( targetFace == null || targetFace.parentSolid == null ) {
            return null;
        }
        if ( !PolyhedralBoundedSolidEulerOperators.lringmv(
                 targetFace.parentSolid, ringSide.parentLoop,
                 targetFace, false) ) {
            return null;
        }
        if ( !neighbor(hea, endsa.get(candidate)) ||
             !neighbor(heb, endsb.get(candidate)) ) {
            tracePipelineSummary(
                "connect ring-rescue re-parent did not complete the match");
            return null;
        }
        tracePipelineSummary("connect ring-rescue applied: ring v="
            + (ringSide.startingVertex == null ? "?"
               : Integer.toString(ringSide.startingVertex.id))
            + " -> face " + targetFace.id);
        _PolyhedralBoundedSolidHalfEdge[] ret =
            new _PolyhedralBoundedSolidHalfEdge[2];
        ret[0] = endsa.get(candidate);
        ret[1] = endsb.get(candidate);
        endsa.remove(candidate);
        endsb.remove(candidate);
        if ( candidate < endsPairIndex.size() ) {
            endsPairIndex.remove(candidate);
        }
        return ret;
    }

    private static boolean isLooseA(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        for ( i = 0; i < endsa.size(); i++ ) {
            if ( he == endsa.get(i) ) return true;
        }

        return false;
    }

    private static boolean isLooseB(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        for ( i = 0; i < endsb.size(); i++ ) {
            if ( he == endsb.get(i) ) return true;
        }

        return false;
    }

    private _PolyhedralBoundedSolidFace cutA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;
        _PolyhedralBoundedSolidFace addedFace = null;
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        if ( withDebug ) {
            System.out.println("       -> CUTA:");
            System.out.println("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;
        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            addedFace = he.parentLoop.parentFace;
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
            if ( addedFace.boundariesList.size() >= 2 ) {
                sonfa.add(addedFace);
                if ( sonfaPairIndexByFaceId != null ) {
                    sonfaPairIndexByFaceId.put(Integer.valueOf(addedFace.id),
                        Integer.valueOf(currentConnectPairIndex));
                }
            }
            else {
                addedFace = null;
            }
        }
        else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        return addedFace;
    }

    private _PolyhedralBoundedSolidFace cutB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;
        _PolyhedralBoundedSolidFace addedFace = null;
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        if ( withDebug ) {
            System.out.println("       -> CUTB:");
            System.out.println("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;
        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            addedFace = he.parentLoop.parentFace;
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
            if ( addedFace.boundariesList.size() >= 2 ) {
                sonfb.add(addedFace);
                if ( sonfbPairIndexByFaceId != null ) {
                    sonfbPairIndexByFaceId.put(Integer.valueOf(addedFace.id),
                        Integer.valueOf(currentConnectPairIndex));
                }
            }
            else {
                addedFace = null;
            }
        }
        else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        return addedFace;
    }

    private static void removeLastCutFaceIfSame(
        ArrayList<_PolyhedralBoundedSolidFace> faces,
        _PolyhedralBoundedSolidFace face)
    {
        int last;

        if ( faces == null || face == null || faces.isEmpty() ) {
            return;
        }
        last = faces.size() - 1;
        if ( faces.get(last) == face ) {
            faces.remove(last);
        }
    }



    /**
    Neighbor null edges connector for the set operations algorithm
    (big phase 3).
    Following section [MANT1988].15.7. and program [MANT1988].15.14.
    */
    private void setOpConnect()
    {
        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------");
        }

        sortNullEdges();

        int i;

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("SORTED SET OF " + sonea.size() + " NULL EDGES PAIRS TO BE CONNECTED");
        }
        tracePipelineSummary(
            "connect start pairsA=" + sonea.size() +
            " pairsB=" + soneb.size());
        if ( isPipelineSummaryTraceEnabled() ) {
            int sameLoopA = 0;
            int diffLoopA = 0;
            for (_PolyhedralBoundedSolidSetOperatorNullEdge ne : sonea) {
                if ( ne.e.rightHalf != null && ne.e.leftHalf != null &&
                     ne.e.rightHalf.parentLoop == ne.e.leftHalf.parentLoop ) {
                    sameLoopA++;
                } else {
                    diffLoopA++;
                }
            }
            int sameLoopB = 0;
            int diffLoopB = 0;
            for (_PolyhedralBoundedSolidSetOperatorNullEdge ne : soneb) {
                if ( ne.e.rightHalf != null && ne.e.leftHalf != null &&
                     ne.e.rightHalf.parentLoop == ne.e.leftHalf.parentLoop ) {
                    sameLoopB++;
                } else {
                    diffLoopB++;
                }
            }
            tracePipelineSummary(
                "connect null-edge-loops A:sameLoop=" + sameLoopA +
                " diffLoop=" + diffLoopA +
                " B:sameLoop=" + sameLoopB +
                " diffLoop=" + diffLoopB);
        }

        _PolyhedralBoundedSolidEdge nextedgea;
        _PolyhedralBoundedSolidEdge nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null;
        _PolyhedralBoundedSolidHalfEdge h2a = null;
        _PolyhedralBoundedSolidHalfEdge h1b = null;
        _PolyhedralBoundedSolidHalfEdge h2b = null;
        _PolyhedralBoundedSolidHalfEdge r[];
        boolean allowRingMoveOnAJoin = (operation == INTERSECTION);
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsPairIndex = new ArrayList<Integer>();

        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfaPairIndexByFaceId = new HashMap<Integer, Integer>();
        sonfbPairIndexByFaceId = new HashMap<Integer, Integer>();
        setCurrentConnectContext(-1);
        int j;

        if ( sonea.size() != soneb.size() ) {
            PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
            System.out.println("**** Not paired null edges!");
        }

        // [MANT1988] Program 15.14:
        //   while (sgetnextnulledge(&nea, &neb)) { ... }
        // The cursor is set to 0 here so each call to setOpConnect()
        // restarts iteration from the first pair of the sorted set.
        nextNullEdgeIndex = 0;
        NullEdgePair pair = new NullEdgePair();
        while ( sgetnextnulledge(pair) ) {
            _PolyhedralBoundedSolidSetOperatorNullEdge nea = pair.nea;
            _PolyhedralBoundedSolidSetOperatorNullEdge neb = pair.neb;
            i = pair.pairIndex;
            _PolyhedralBoundedSolidHalfEdge ha;
            _PolyhedralBoundedSolidHalfEdge ham;
            _PolyhedralBoundedSolidHalfEdge hb;
            _PolyhedralBoundedSolidHalfEdge hbm;
            _PolyhedralBoundedSolidHalfEdge tmp;

            ha = nea.e.rightHalf;
            ham = nea.e.leftHalf;
            hb = neb.e.rightHalf;
            hbm = neb.e.leftHalf;
            tracePipelineSummary(
                "connect pair[" + i + "] A{" + summarizeNullEdge(nea) +
                "} B{" + summarizeNullEdge(neb) + "}");

            if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
                System.out.println("  - " + (endsa.size()+endsb.size()) +
                    " = " + endsa.size() + "+" + endsb.size() +
                    " loose ends before processing pair [" + i + "]:");

                for ( j = 0; j < endsa.size(); j++ ) {
                    _PolyhedralBoundedSolidHalfEdge hat;
                    _PolyhedralBoundedSolidHalfEdge hbt;
                    hat = endsa.get(j);
                    hbt = endsb.get(j);
                    System.out.println("    . [" + j + "]: He(A): " +
                                       hat.startingVertex.id +
                                       "/" + hat.next().startingVertex.id +
                                       " | He(B): " + hbt.startingVertex.id +
                                       "/" + hbt.next().startingVertex.id);
                }

                if ( ha.startingVertex.id > ham.startingVertex.id ) {
                    System.out.println("********* FORCING ORDER!");
                }

                System.out.println("  - Processing pair [" + i + "]: "+
                    "He(A1): " + ha.startingVertex.id + "/" + ha.next().startingVertex.id +
                    " He(A2): " + ham.startingVertex.id + "/" + ham.next().startingVertex.id +
                    " He(B1): " + hb.startingVertex.id + "/" + hb.next().startingVertex.id +
                    " He(B2): " + hbm.startingVertex.id + "/" + hbm.next().startingVertex.id);
            }

            nextedgea = nea.e;
            nextedgeb = neb.e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;
            // Legacy strut orientation: vertex-id order encodes classifier
            // emission order. When sortNullEdges already oriented the struts
            // along the intersection curves (mythosPlan §5.3), that
            // orientation must be respected — re-normalizing by id here
            // would undo it and reintroduce seam mismatches in scanjoin.
            if ( !curveOrientationApplied ) {
                if ( ha.startingVertex.id > ham.startingVertex.id ) {
                    tmp = nextedgea.rightHalf;
                    nextedgea.rightHalf = nextedgea.leftHalf;
                    nextedgea.leftHalf = tmp;
                }
                if ( hb.startingVertex.id > hbm.startingVertex.id ) {
                    tmp = nextedgeb.rightHalf;
                    nextedgeb.rightHalf = nextedgeb.leftHalf;
                    nextedgeb.leftHalf = tmp;
                }
            }

            rebindClassicCurrentNullEdgesIfNeeded(
                nextedgea.rightHalf,
                nextedgea.leftHalf,
                nextedgeb.leftHalf,
                nextedgeb.rightHalf);

            setCurrentConnectContext(i);
            r = scanjoin(nextedgea.rightHalf, nextedgeb.leftHalf);
            if ( r != null ) {
                h1a = r[0];
                h2b = r[1];
                join(h1a, nextedgea.rightHalf, withDebug,
                    allowRingMoveOnAJoin);
                if ( !isLooseA(h1a.mirrorHalfEdge()) ) {
                    cutA(h1a);
                }
                join(h2b, nextedgeb.leftHalf, withDebug);
                if ( !isLooseB(h2b.mirrorHalfEdge()) ) {
                    cutB(h2b);
                }
            }

            setCurrentConnectContext(i);
            r = scanjoin(nextedgea.leftHalf, nextedgeb.rightHalf);
            if ( r != null ) {
                h2a = r[0];
                h1b = r[1];
                join(h2a, nextedgea.leftHalf, withDebug,
                    allowRingMoveOnAJoin);
                if ( !isLooseA(h2a.mirrorHalfEdge()) ) {
                    cutA(h2a);
                }
                join(h1b, nextedgeb.rightHalf, withDebug);
                if ( !isLooseB(h1b.mirrorHalfEdge()) ) {
                    cutB(h1b);
                }
            }

            if ( h1a != null && h1b != null && h2a != null && h2b != null ) {
                cutA(nextedgea.rightHalf);
                cutB(nextedgeb.rightHalf);
                tracePipelineSummary(
                    "connect pair[" + i + "] produced cuts h1a=" +
                    summarizeHalfEdge(h1a) + " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) + " h2b=" +
                    summarizeHalfEdge(h2b));
            }
            else {
                PolyhedralBoundedSolidStatistics.recordJoinIncompleteCase();
                PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
                tracePipelineSummary(
                    "connect pair[" + i + "] incomplete h1a=" +
                    summarizeHalfEdge(h1a) + " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) + " h2b=" +
                    summarizeHalfEdge(h2b) + " looseA=" + endsa.size() +
                    " looseB=" + endsb.size());
            }
        }

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("  . Pending null edges to connect:");
            for ( i = 0; i < endsa.size(); i++ ) {
                System.out.println("    . A[" + (i+1) + "]: " + endsa.get(i));
            }
            for ( i = 0; i < endsb.size(); i++ ) {
                System.out.println("    . B[" + (i+1) + "]: " + endsb.get(i));
            }
        }
        tracePipelineSummary(
            "connect end sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size() +
            " looseA=" + endsa.size() +
            " looseB=" + endsb.size());
        // §6.1-B: post-loop safety nets removed.
        // Per [MANT1988] Program 15.14, the main loop must leave
        // looseA == looseB == 0 by itself. Any survivor for
        // MANT1988_15_1 INTERSECTION/SUBTRACT (looseA=4) is the
        // visible symptom of the §5.2 deferred sectoroverlap fix
        // upstream — see plan §6.1-C analysis. A post-pass closure
        // (pairLatentLooseEnds) was attempted but turned out to fuse
        // legitimately-separate shells in cases like HOLLOW_BRICK, so
        // it was retired. The right fix is upstream, not here.
        tracePipelineSummary(
            "connect post-pass sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size() +
            " looseA=" + endsa.size() +
            " looseB=" + endsb.size());
        updateLastSnapshot();

        for ( i = 0; i < endsa.size() && i < endsb.size(); i++ ) {
            tracePipelineSummary(
                "connect loose[" + i + "] A=" + summarizeHalfEdge(endsa.get(i)) +
                " B=" + summarizeHalfEdge(endsb.get(i)));
        }
    }
}
