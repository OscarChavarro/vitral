//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

/**
Finish stage (big phase 4) for set operations, corresponding to the answer
integration step of program [MANT1988].15.15.
*/
final class PolyhedralBoundedSolidSetFinisher
    extends PolyhedralBoundedSolidOperator
{
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_06_FINISH = 0x20;

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

        inda = (op == INTERSECTION) ? sonfa.size() : 0;
        indb = (op == UNION) ? 0 : sonfb.size();

        int oldsize = sonfa.size();

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
            outRes.loopGlue(sonfa.get(i+inda).id);
        }
        outRes.compactIds();
    }
}
