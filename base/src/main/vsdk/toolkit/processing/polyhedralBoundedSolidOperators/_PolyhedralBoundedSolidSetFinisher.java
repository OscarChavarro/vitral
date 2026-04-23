//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

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
            boolean validA = faceA != null && faceA.boundariesList.size() >= 2;

            if ( !validA ) {
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
                validB = faceB != null && faceB.boundariesList.size() >= 2;
                if ( !validB ) {
                    continue;
                }
                pairIndexB = _PolyhedralBoundedSolidSetNullEdgesConnector
                    .getSonfbPairIndex(faceB);
                if ( pairIndexA != -1 && pairIndexA == pairIndexB ) {
                    matchedA.add(faceA);
                    matchedB.add(faceB);
                    usedB[j] = true;
                    foundMatch = true;
                    break;
                }
            }
        }

        sonfa.clear();
        sonfa.addAll(matchedA);
        sonfb.clear();
        sonfb.addAll(matchedB);
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
        int i, inda, indb;
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
            f = inSolidA.lmfkrh(sonfa.get(i).boundariesList.get(1),
                                inSolidA.getMaxFaceId()+1);
            sonfa.add(f);

            f = inSolidB.lmfkrh(sonfb.get(i).boundariesList.get(1),
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
            outRes.lkfmrh(sonfa.get(i+inda), sonfb.get(i+indb));
            outRes.loopGlue(sonfa.get(i+inda));
        }
        cleanup(outRes);
        outRes.compactIds();
        tracePipelineSummary(
            "finish end outRes faces=" + outRes.polygonsList.size() +
            " edges=" + outRes.edgesList.size() +
            " vertices=" + outRes.verticesList.size());
    }
}
