//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
Connect stage (big phase 3) for set operations: null-edges pairing and joins,
following section [MANT1988].15.7 and programs [MANT1988].15.13 and
[MANT1988].15.14.
*/
final class _PolyhedralBoundedSolidSetNullEdgesConnector
    extends _PolyhedralBoundedSolidOperator
{
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_05_CONNECT = 0x10;
    private static final int DEBUG_99_SHOWOPERATIONS = 0x40;

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

    private static int debugFlags;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsa;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsb;
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfa;
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfb;

    static ConnectResult connect(
        int flags,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSonea,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSoneb)
    {
        debugFlags = flags;
        sonea = inSonea;
        soneb = inSoneb;
        setOpConnect();
        return new ConnectResult(sonfa, sonfb);
    }

    private static void sortNullEdges()
    {
        // Keep the insertion order from the classifier. The vertex/vertex
        // pairing logic already resolves the coplanar matches before
        // emitting null edges, so re-sorting both solids independently tends
        // to destroy that correspondence in multi-branch cases.
    }

    /**
    Following section [MANT1988].15.7. and program [MANT1988].15.13.
    */
    private static _PolyhedralBoundedSolidHalfEdge[]
    canJoin(_PolyhedralBoundedSolidHalfEdge hea,
             _PolyhedralBoundedSolidHalfEdge heb)
    {
        int i;
        _PolyhedralBoundedSolidHalfEdge ret[];
        boolean condition1;
        boolean condition2;

        ret = new _PolyhedralBoundedSolidHalfEdge[2];

        for ( i = 0; i < endsa.size(); i++ ) {

            condition1 = neighbor(hea, endsa.get(i));
            condition2 = neighbor(heb, endsb.get(i));

            if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
                System.out.println("    . Testing for neighborhood A[" +
                   hea.startingVertex.id +
                   "/" +
                   hea.next().startingVertex.id +
                   "] vs. A[" +
                   endsa.get(i).startingVertex.id +
                   "/" +
                   endsa.get(i).next().startingVertex.id +
                   "]: " +
                   (condition1?"true":"false") +
                   " ParentFaces: " +
                   hea.parentLoop.parentFace.id +
                   " / " +
                   endsa.get(i).parentLoop.parentFace.id);

                System.out.println("    . Testing for neighborhood B[" +
                   heb.startingVertex.id +
                   "/" +
                   heb.next().startingVertex.id +
                   "] vs. B[" +
                   endsb.get(i).startingVertex.id +
                   "/" +
                   endsb.get(i).next().startingVertex.id +
                   "]: " +
                   (condition2?"true":"false") +
                   " ParentFaces: " +
                   heb.parentLoop.parentFace.id +
                   " / " +
                   endsb.get(i).parentLoop.parentFace.id);
            }

            if ( condition1 && condition2 ) {
                ret[0] = endsa.get(i);
                ret[1] = endsb.get(i);
                endsa.remove(i);
                endsb.remove(i);
                return ret;
            }
        }
        endsa.add(hea);
        endsb.add(heb);
        return null;
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

    private static void cutA(_PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        if ( withDebug ) {
            System.out.println("       -> CUTA:");
            System.out.println("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;

        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            sonfa.add(he.parentLoop.parentFace);
            s.lkemr(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        else {
            s.lkef(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
    }

    private static void cutB(_PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        if ( withDebug ) {
            System.out.println("       -> CUTB:");
            System.out.println("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;

        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            sonfb.add(he.parentLoop.parentFace);
            s.lkemr(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        else {
            s.lkef(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
    }

    private static void removeLooseEndsA(_PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidHalfEdge heStart;
        int i;

        heStart = he;
        do {
            for ( i = 0; i < endsa.size(); i++ ) {
                if ( endsa.get(i) == he ) {
                    endsa.remove(i);
                    break;
                }
            }
            he = he.next();
        } while ( he != heStart );
    }

    private static void removeLooseEndsB(_PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidHalfEdge heStart;
        int i;

        heStart = he;
        do {
            for ( i = 0; i < endsb.size(); i++ ) {
                if ( endsb.get(i) == he ) {
                    endsb.remove(i);
                    break;
                }
            }
            he = he.next();
        } while ( he != heStart );
    }

    /**
    Neighbor null edges connector for the set operations algorithm
    (big phase 3).
    Following section [MANT1988].15.7. and program [MANT1988].15.14.
    */
    private static void setOpConnect()
    {
        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------");
        }

        sortNullEdges();

        int i;

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("SORTED SET OF " + sonea.size() + " NULL EDGES PAIRS TO BE CONNECTED");
        }

        _PolyhedralBoundedSolidEdge nextedgea, nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null, h2a = null, h1b = null, h2b = null;
        _PolyhedralBoundedSolidHalfEdge r[];
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        int j;

        if ( sonea.size() != soneb.size() ) {
            System.out.println("**** Not paired null edges!");
        }

        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge ha, ham;
            _PolyhedralBoundedSolidHalfEdge hb, hbm;
            _PolyhedralBoundedSolidHalfEdge tmp;

            ha = sonea.get(i).e.rightHalf;
            ham = sonea.get(i).e.leftHalf;
            hb = soneb.get(i).e.rightHalf;
            hbm = soneb.get(i).e.leftHalf;

            if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
                System.out.println("  - " + (endsa.size()+endsb.size()) +
                    " = " + endsa.size() + "+" + endsb.size() +
                    " loose ends before processing pair [" + i + "]:");

                for ( j = 0; j < endsa.size(); j++ ) {
                    _PolyhedralBoundedSolidHalfEdge hat, hbt;
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

            boolean swap = false;
            nextedgea = sonea.get(i).e;
            nextedgeb = soneb.get(i).e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;
            if ( ha.startingVertex.id > ham.startingVertex.id ) {
                tmp = nextedgea.rightHalf;
                nextedgea.rightHalf = nextedgea.leftHalf;
                nextedgea.leftHalf = tmp;
                if ( hb.startingVertex.id > hbm.startingVertex.id ) {
                    tmp = nextedgeb.rightHalf;
                    nextedgeb.rightHalf = nextedgeb.leftHalf;
                    nextedgeb.leftHalf = tmp;
                }
            }

            r = canJoin(nextedgea.rightHalf, nextedgeb.leftHalf);
            if ( r != null ) {
                h1a = r[0];
                h2b = r[1];
                join(h1a, nextedgea.rightHalf, withDebug);
                removeLooseEndsA(h1a);
                if ( !isLooseA(h1a.mirrorHalfEdge()) ) {
                    cutA(h1a);
                }
                join(h2b, nextedgeb.leftHalf, withDebug);
                removeLooseEndsB(h2b);
                if ( !isLooseB(h2b.mirrorHalfEdge()) ) {
                    cutB(h2b);
                }
            }

            r = canJoin(nextedgea.leftHalf, nextedgeb.rightHalf);
            if ( r != null ) {
                h2a = r[0];
                h1b = r[1];
                join(h2a, nextedgea.leftHalf, withDebug);
                removeLooseEndsA(h2a);
                if ( !isLooseA(h2a.mirrorHalfEdge()) ) {
                    cutA(h2a);
                }
                join(h1b, nextedgeb.rightHalf, withDebug);
                removeLooseEndsB(h1b);
                if ( !isLooseB(h1b.mirrorHalfEdge()) ) {
                    cutB(h1b);
                }
            }

            if ( h1a != null && h1b != null && h2a != null && h2b != null ) {
                cutA(nextedgea.rightHalf);
                cutB(nextedgeb.rightHalf);
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
    }
}
