//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

/**
Finish stage (big phase 4) for set operations, corresponding to the answer
integration step of program [MANT1988].15.15.
*/
final class _PolyhedralBoundedSolidSetFinisher
    extends _PolyhedralBoundedSolidOperator
{
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_06_FINISH = 0x20;

    private static boolean isPipelineSummaryTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static void tracePipelineSummary(String message)
    {
        if ( !isPipelineSummaryTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpPipelineTrace] " + message);
    }

    private static boolean hasUsableIntegrationRing(
        _PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop ring;
        _PolyhedralBoundedSolidHalfEdge start;
        _PolyhedralBoundedSolidHalfEdge current;
        Vector3D reference;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context;
        int guard;

        if ( face == null ||
             !hasCompleteHalfEdgeConnectivity(face) ||
             face.boundariesList.size() < 2 ||
             face.boundariesList.get(1) == null ||
             face.boundariesList.get(1).halfEdgesList == null ) {
            return false;
        }
        ring = face.boundariesList.get(1);
        if ( ring.halfEdgesList.size() < 2 ) {
            return false;
        }
        start = ring.boundaryStartHalfEdge;
        if ( start == null || start.startingVertex == null ||
             start.startingVertex.position == null ) {
            return false;
        }
        reference = start.startingVertex.position;
        context = PolyhedralBoundedSolidNumericPolicy.forFace(face);
        current = start.next();
        guard = 0;
        while ( current != null && current != start &&
                guard <= ring.halfEdgesList.size() ) {
            if ( current.startingVertex != null &&
                 current.startingVertex.position != null &&
                 !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                     reference, current.startingVertex.position, context) ) {
                return true;
            }
            current = current.next();
            guard++;
        }
        return false;
    }

    private static boolean hasCompleteHalfEdgeConnectivity(
        _PolyhedralBoundedSolidFace face)
    {
        int i;

        if ( face == null || face.boundariesList == null ) {
            return false;
        }
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            _PolyhedralBoundedSolidHalfEdge start;
            _PolyhedralBoundedSolidHalfEdge current;
            int guard;

            if ( loop == null ||
                 loop.halfEdgesList == null ||
                 loop.boundaryStartHalfEdge == null ) {
                return false;
            }
            start = loop.boundaryStartHalfEdge;
            current = start;
            guard = 0;
            do {
                if ( current == null ||
                     current.parentEdge == null ||
                     current.parentLoop == null ||
                     current.startingVertex == null ||
                     current.mirrorHalfEdge() == null ||
                     current.next() == null ||
                     current.previous() == null ) {
                    return false;
                }
                current = current.next();
                guard++;
            } while ( current != start &&
                      guard <= loop.halfEdgesList.size() + 1 );
            if ( current != start ) {
                return false;
            }
        }
        return true;
    }

    private static String integrationRingSummary(
        _PolyhedralBoundedSolidFace face)
    {
        if ( face == null ) {
            return "null";
        }
        if ( face.boundariesList.size() < 2 ||
             face.boundariesList.get(1) == null ||
             face.boundariesList.get(1).halfEdgesList == null ) {
            return "face=" + face.id + " boundaries=" +
                face.boundariesList.size();
        }
        return "face=" + face.id + " ringSize=" +
            face.boundariesList.get(1).halfEdgesList.size() +
            " usable=" + hasUsableIntegrationRing(face) +
            " connected=" + hasCompleteHalfEdgeConnectivity(face) +
            " pair=" +
            _PolyhedralBoundedSolidSetNullEdgesConnector
                .getSonfaPairIndex(face) +
            "/" +
            _PolyhedralBoundedSolidSetNullEdgesConnector
                .getSonfbPairIndex(face);
    }

    private static int sanitizePairedFaces(
        ArrayList<_PolyhedralBoundedSolidFace> sonfa,
        ArrayList<_PolyhedralBoundedSolidFace> sonfb)
    {
        ArrayList<_PolyhedralBoundedSolidFace> matchedA;
        ArrayList<_PolyhedralBoundedSolidFace> matchedB;
        boolean[] usedB;
        int i;
        int j;

        if ( sonfa == null || sonfb == null ) {
            return 0;
        }

        matchedA = new ArrayList<_PolyhedralBoundedSolidFace>();
        matchedB = new ArrayList<_PolyhedralBoundedSolidFace>();
        usedB = new boolean[sonfb.size()];

        for ( i = 0; i < sonfa.size(); i++ ) {
            _PolyhedralBoundedSolidFace faceA = sonfa.get(i);
            int pairIndexA;
            boolean foundMatch = false;
            boolean validA = hasUsableIntegrationRing(faceA);

            if ( !validA ) {
                tracePipelineSummary(
                    "finish sanitize skip A " +
                    integrationRingSummary(faceA));
                continue;
            }

            pairIndexA = _PolyhedralBoundedSolidSetNullEdgesConnector
                .getSonfaPairIndex(faceA);
            for ( j = 0; j < sonfb.size(); j++ ) {
                _PolyhedralBoundedSolidFace faceB = sonfb.get(j);
                int pairIndexB;
                boolean validB;

                if ( usedB[j] ) {
                    continue;
                }
                validB = hasUsableIntegrationRing(faceB);
                if ( !validB ) {
                    tracePipelineSummary(
                        "finish sanitize skip B " +
                        integrationRingSummary(faceB));
                    continue;
                }
                pairIndexB = _PolyhedralBoundedSolidSetNullEdgesConnector
                    .getSonfbPairIndex(faceB);
                if ( pairIndexA != -1 && pairIndexA == pairIndexB ) {
                    tracePipelineSummary(
                        "finish sanitize match A " +
                        integrationRingSummary(faceA) +
                        " B " +
                        integrationRingSummary(faceB));
                    matchedA.add(faceA);
                    matchedB.add(faceB);
                    usedB[j] = true;
                    foundMatch = true;
                    break;
                }
            }
        }

        if ( matchedA.isEmpty() && !sonfa.isEmpty() &&
             sonfa.size() == sonfb.size() ) {
            tracePipelineSummary(
                "finish sanitize kept legacy ordering");
            return sonfa.size();
        }

        sonfa.clear();
        sonfa.addAll(matchedA);
        sonfb.clear();
        sonfb.addAll(matchedB);
        tracePipelineSummary(
            "finish sanitize matched=" + matchedA.size());
        return matchedA.size();
    }

    /**
    Answer integrator for the set-operations pipeline.
    Following program [MANT1988].15.15.
    */
    static void finish(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op,
        int debugFlags,
        ArrayList<_PolyhedralBoundedSolidFace> sonfa,
        ArrayList<_PolyhedralBoundedSolidFace> sonfb)
    {
        int i;
        int inda;
        int indb;
        _PolyhedralBoundedSolidFace f;

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 4. ------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("setOpFinish");
        }

        if ( (debugFlags & DEBUG_06_FINISH) != 0x00 ) {
            System.out.println("TESTING FINISH: " + sonfa.size());
        }
        tracePipelineSummary(
            "finish start op=" + op +
            " sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size());

        int oldsize = sanitizePairedFaces(sonfa, sonfb);
        inda = (op == INTERSECTION) ? sonfa.size() : 0;
        indb = (op == UNION) ? 0 : sonfb.size();

        for ( i = 0; i < oldsize; i++ ) {
            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolidA, sonfa.get(i).boundariesList.get(1),
                                inSolidA.getMaxFaceId()+1);
            sonfa.add(f);

            f = PolyhedralBoundedSolidEulerOperators.lmfkrh(inSolidB, sonfb.get(i).boundariesList.get(1),
                                inSolidB.getMaxFaceId()+1);
            sonfb.add(f);
        }

        if ( op == SUBTRACT) {
            inSolidB.revert();
        }

        for ( i = 0; i < oldsize; i++ ) {
            movefac(sonfa.get(i+inda), outRes);
            movefac(sonfb.get(i+indb), outRes);
        }

        cleanup(outRes);

        for ( i = 0; i < oldsize; i++ ) {
            PolyhedralBoundedSolidEulerOperators.lkfmrh(outRes, sonfa.get(i+inda), sonfb.get(i+indb));
            PolyhedralBoundedSolidTopologyEditing.loopGlue(outRes, sonfa.get(i+inda));
        }
        cleanup(outRes);
        PolyhedralBoundedSolidTopologyEditing.compactIds(outRes);
        tracePipelineSummary(
            "finish end outRes faces=" + outRes.getPolygonsList().size() +
            " edges=" + outRes.getEdgesList().size() +
            " vertices=" + outRes.getVerticesList().size());
    }
}
