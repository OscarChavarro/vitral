//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Classification stages, connection stages, and no-intersection containment
logic for the Boolean set-operations pipeline of chapter [MANT1988].15.
*/
final class _PolyhedralBoundedSolidSetClassifier
    extends _PolyhedralBoundedSolidOperator
{
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_04_VERTEX_VERTEX_CLASSIFIER = 0x08;
    private static final int DEBUG_99_SHOW_OPERATIONS = 0x40;

    private static final int NO_INT_RELATION_DISJOINT = 0;
    private static final int NO_INT_RELATION_TOUCHING = 1;
    private static final int NO_INT_RELATION_A_IN_B = 2;
    private static final int NO_INT_RELATION_B_IN_A = 3;

    private static int debugFlags;

    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;

    static void runSetOpClassify(int op,
                                 PolyhedralBoundedSolid inSolidA,
                                 PolyhedralBoundedSolid inSolidB,
                                 int flags,
                                 ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> inSonvv,
                                 ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> inSonva,
                                 ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> inSonvb,
                                 ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSonea,
                                 ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSoneb)
    {
        debugFlags = flags;
        sonvv = inSonvv;
        sonva = inSonva;
        sonvb = inSonvb;
        sonea = inSonea;
        soneb = inSoneb;
        setOpClassify(op, inSolidA, inSolidB);
    }

    static PolyhedralBoundedSolid runSetOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op);
    }

    static boolean runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runTouchingOnlyPreflightCase(inSolidA, inSolidB);
    }

    private static int compareToZero(double value)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static int pointInFace(_PolyhedralBoundedSolidFace face, Vector3D point)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .pointInFace(face, point);
    }

    private static int resolveCoplanarVertexVertexClass(int op,
        boolean sameOrientation, boolean sideA)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .resolveCoplanarVertexVertexClass(op, sameOrientation, sideA);
    }

    private static int nextVertexId(PolyhedralBoundedSolid current,
                                    PolyhedralBoundedSolid other)
    {
        int a, b, m;

        a = current.getMaxVertexId();
        b = other.getMaxVertexId();
        m = a;
        if ( b > a ) {
            m = b;
        }

        return m+1;
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face. This adapts the sector
    bisector idea from problem [MANT1988].14.1 to the set-operations
    classifiers of chapter [MANT1988].15.
    */
    protected static Vector3D inside(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3D middle = null;
        Vector3D a, b, n;

        a = (he.next()).startingVertex.position.subtract(
            he.startingVertex.position);
        b = (he.previous()).startingVertex.position.subtract(
            he.startingVertex.position);
        a = a.normalized();
        b = b.normalized();

        n = he.parentLoop.parentFace.containingPlane.getNormal();

        middle = n.crossProduct(a);
        middle = middle.normalized();

        return middle;
    }

    private static boolean sctrwitthin(Vector3D dir, Vector3D ref1,
                            Vector3D ref2, Vector3D ref12)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sctrwitthin(dir, ref1, ref2, ref12);
    }

    /**
    Normalizes one endpoint for `separateEdgeSequence` when a previous null
    strut edge was already inserted on the same vertex neighborhood.
    */
    private static _PolyhedralBoundedSolidHalfEdge
    recoverEdgeSequenceEndpointFromStrut(
        _PolyhedralBoundedSolidHalfEdge endpoint,
        boolean isFromEndpoint)
    {
        _PolyhedralBoundedSolidHalfEdge prev;

        if ( endpoint == null ) {
            return null;
        }

        prev = endpoint.previous();
        if ( prev == null || prev.parentEdge == null ) {
            return endpoint;
        }

        if ( !nulledge(prev) || !strutnulledge(prev) ) {
            return endpoint;
        }

        if ( isFromEndpoint ) {
            if ( prev == prev.parentEdge.leftHalf ) {
                return prev.previous();
            }
        }
        else {
            if ( prev == prev.parentEdge.rightHalf ) {
                return prev.previous();
            }
        }
        return endpoint;
    }

    /**
    Following program [MANT1988].15.12.
    Taking in to account the updated version modifications from
    [.wMANT2008].
    */
    private static void separateEdgeSequence(_PolyhedralBoundedSolidHalfEdge from,
                               _PolyhedralBoundedSolidHalfEdge to,
                               int type,
                               PolyhedralBoundedSolid inSolidA,
                               PolyhedralBoundedSolid inSolidB)
    {
        //-----------------------------------------------------------------
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
            System.out.println("      SEPARATEEDGESEQUENCE " + type);
            System.out.println("        From: " + from);
            System.out.println("        To: " + to);
        }

        if ( from == null || to == null ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, "separateEdgeSequence", 
                "Unexpected case: null halfedges!");
        }

        PolyhedralBoundedSolid s;
        s = from.parentLoop.parentFace.parentSolid;

        if ( s != to.parentLoop.parentFace.parentSolid ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, "separateEdgeSequence", 
                "Unexpected case: halfedges on different solids!");
        }

        //-----------------------------------------------------------------
        // Recover from null edges already inserted.
        // This block fully resolves the old A-E unsupported branches by
        // canonicalizing endpoint selection until both halfedges share origin.
        int recoveryGuard = 0;
        boolean changed;
        do {
            changed = false;

            _PolyhedralBoundedSolidHalfEdge recoveredFrom;
            _PolyhedralBoundedSolidHalfEdge recoveredTo;

            recoveredFrom = recoverEdgeSequenceEndpointFromStrut(from, true);
            if ( recoveredFrom != from ) {
                from = recoveredFrom;
                changed = true;
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                    System.out.println("        Recovered edge sequence case A");
                }
            }

            recoveredTo = recoverEdgeSequenceEndpointFromStrut(to, false);
            if ( recoveredTo != to ) {
                to = recoveredTo;
                changed = true;
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                    System.out.println("        Recovered edge sequence case B");
                }
            }

            if ( from.startingVertex != to.startingVertex ) {
                _PolyhedralBoundedSolidHalfEdge fromPrev = from.previous();
                _PolyhedralBoundedSolidHalfEdge toPrev = to.previous();

                if ( fromPrev != null && toPrev != null &&
                     fromPrev.parentEdge != null && toPrev.parentEdge != null &&
                     fromPrev == toPrev.mirrorHalfEdge() ) {
                    from = fromPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case C");
                    }
                }
                else if ( fromPrev != null &&
                          fromPrev.startingVertex == to.startingVertex ) {
                    from = fromPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case D");
                    }
                }
                else if ( toPrev != null &&
                          toPrev.startingVertex == from.startingVertex ) {
                    to = toPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case E");
                    }
                }
            }

            recoveryGuard++;
            if ( recoveryGuard > 16 ) {
                break;
            }
        } while ( changed );

        if ( from.startingVertex != to.startingVertex ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, "separateEdgeSequence",
                "Unable to recover endpoint pairing after A-E normalization.");
            return;
        }

        //-----------------------------------------------------------------
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
            System.out.println("       -> LMEV (Separate edge sequence):");
            System.out.println("          . H1: " + to);
            System.out.println("          . H2: " + from);
            //from.startingVertex.debugColor = new ColorRgb(1, 0, 1);
        }

        int id = nextVertexId(inSolidA, inSolidB);

        s.lmev(to, from, id, to.startingVertex.position);

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
            System.out.println("          . New vertex: " + id);
        }

        if ( type == 0 ) {
            sonea.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(from.previous().parentEdge));
        }
        else {
            soneb.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(from.previous().parentEdge));
        }

    }

    /**
    Following program [MANT1988].15.12.
    Taking in to account the updated version modifications from
    [.wMANT2008].
    */
    private static void separateInterior(_PolyhedralBoundedSolidHalfEdge he,
                               int type,
                               boolean orient,
                               PolyhedralBoundedSolid inSolidA,
                               PolyhedralBoundedSolid inSolidB)
    {
        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
            System.out.println("      SEPARATEINTERIOR " + type);
            System.out.println("        From/To: " + he);
        }

        _PolyhedralBoundedSolidHalfEdge tmp;
/*
        // Recover from null edges inserted
        if ( nulledge(he.previous()) ) {
            if( ((he.previous() == he.previous().parentEdge.rightHalf) && orient) ||
                ((he.previous() == he.previous().parentEdge.leftHalf) && !orient) ) {
                he = he.previous();
            }
        }

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS ) != 0x00 ) {
                System.out.println("       -> LMEVSTRUT (Separate interior):");
                System.out.println("          . H1: " + he);
        }


*/

//      he = he.mirrorHalfEdge().next();


        int id = nextVertexId(inSolidA, inSolidB);
        he.parentLoop.parentFace.parentSolid.lmev(he, he, id,
            he.startingVertex.position);

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
            System.out.println("          . New vertex: " + id);
            //he.startingVertex.debugColor = new ColorRgb(0, 1, 1);
        }

        // A piece of Black Art: reverse orientation of the null edge
        if ( !orient ) {
            tmp = he.previous().parentEdge.rightHalf;
            he.previous().parentEdge.rightHalf = he.previous().parentEdge.leftHalf;
            he.previous().parentEdge.leftHalf = tmp;
        }

        if ( type == 0 ) {
            sonea.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(he.previous().parentEdge));
        }
        else {
            soneb.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(he.previous().parentEdge));
        }

    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static boolean nulledge(_PolyhedralBoundedSolidHalfEdge he)
    {
        return PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
            he.startingVertex.position, he.next().startingVertex.position,
            numericContext);
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static boolean strutnulledge(_PolyhedralBoundedSolidHalfEdge he)
    {
        if( he == he.mirrorHalfEdge().next() ||
            he == he.mirrorHalfEdge().previous() ) {
            return true;
        }
        return false;
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static boolean convexedg(_PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidHalfEdge h2;
        Vector3D dir, cr;

        h2 = he.next();
        if ( nulledge(he) ) {
            h2 = h2.next();
        }
        dir = h2.startingVertex.position.subtract(he.startingVertex.position);
        cr = he.parentLoop.parentFace.containingPlane.getNormal().crossProduct(he.mirrorHalfEdge().parentLoop.parentFace.containingPlane.getNormal());
        if ( cr.length() < numericContext.unitVectorTolerance() ) {
            return true;
        }
        return (dir.dotProduct(cr) < 0.0);
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static boolean sectorwide(_PolyhedralBoundedSolidHalfEdge he, int ind)
    {
        return checkWideness(he);
    }

    /**
    Borrowed from [.wMANT2008].
    */
    private static boolean getOrientation(
        _PolyhedralBoundedSolidHalfEdge ref,
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2)
    {
        _PolyhedralBoundedSolidHalfEdge mhe1, mhe2;
        boolean retcode = false;

        mhe1 = he1.mirrorHalfEdge().next();
        mhe2 = he2.mirrorHalfEdge().next();
        if ( mhe1 != he2 && mhe2 == he1 ) {
            retcode = convexedg(he2);
        }
        else {
            retcode = convexedg(he1);
        }
        if( sectorwide(mhe1, 0) && sectorwide(ref, 0) ) {
            retcode = !retcode;
        }

/*
        if ( retcode ) {
            ref.startingVertex.debugColor = new ColorRgb(0, 1, 0);
        }
        else {
            ref.startingVertex.debugColor = new ColorRgb(0, 0, 1);
        }
*/

        return !retcode;
    }

    /**
    Following section [MANT1988].15.6.2. and program [MANT1988].15.11.
    */
    private static void vertexVertexInsertNullEdges(
        _PolyhedralBoundedSolidSetVertexVertexClassifier.VertexVertexClassificationData data,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidHalfEdge ha1 = null;
        _PolyhedralBoundedSolidHalfEdge ha2 = null;
        _PolyhedralBoundedSolidHalfEdge hb1 = null;
        _PolyhedralBoundedSolidHalfEdge hb2 = null;
        int i;

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
            System.out.println("   - Null edges insertion:");
        }

        int count = 0;

        for ( i = 0; i < data.sectors.size(); i++ ) {
            if ( data.sectors.get(i).intersect ) count++;
        }

        if ( count == 0 && data.sectors.size() > 0 ) {
            ha1 = data.nba.get(data.sectors.get(0).secta).he;
            hb1 = data.nbb.get(data.sectors.get(0).sectb).he;
            //System.out.println("**** EMPTY CASE");
        }

        i = 0;
        while ( true ) {
            //-------------------------------------------------------------
            if ( i >= data.sectors.size() ) {
                return;
            }
            while ( !data.sectors.get(i).intersect ) {
                i++;
                if ( i == data.sectors.size() ) {
                    return;
                }
            }
            if ( data.sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) {
                ha1 = data.nba.get(data.sectors.get(i).secta).he;
            }
            else {
                ha2 = data.nba.get(data.sectors.get(i).secta).he;
            }
            if ( data.sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                hb1 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            }
            else {
                hb2 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            }

            //-------------------------------------------------------------
            if ( i >= data.sectors.size() ) {
                return;
            }
            while ( !data.sectors.get(i).intersect ) {
                i++;
                if ( i == data.sectors.size() ) {
                    return;
                }
            }
            if ( data.sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) {
                ha1 = data.nba.get(data.sectors.get(i).secta).he;
            }
            else {
                ha2 = data.nba.get(data.sectors.get(i).secta).he;
            }
            if ( data.sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                hb1 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            }
            else {
                hb2 = data.nbb.get(data.sectors.get(i).sectb).he;
                i++;
            }

            //-------------------------------------------------------------
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                System.out.println("    . Deciding case:");
                System.out.println("      -> Ha1: " + ha1);
                System.out.println("      -> Ha2: " + ha2);
                System.out.println("      -> Hb1: " + hb1);
                System.out.println("      -> Hb2: " + hb2);
            }

            //-------------------------------------------------------------
            if ( ha1 == null || ha2 == null || hb1 == null || hb2 == null ) {
                int j;

                for ( j = 0; j < data.sectors.size(); j++ ) {
                    data.sectors.get(j).intersect = false;
                }
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                    System.out.println(
                        "    . Incomplete coplanar pairing, skipping split");
                }
                return;
            }

            //-------------------------------------------------------------
            if ( ha1 == ha2 ) {
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                    System.out.println("    . STRUT A CASE");
                }
                separateInterior(ha1, 0, getOrientation(ha1, hb1, hb2), inSolidA, inSolidB);
                separateEdgeSequence(hb1, hb2, 1, inSolidA, inSolidB);
            }
            else if ( hb1 == hb2 ) {
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                    System.out.println("    . STRUT B CASE");
                }
                separateInterior(hb1, 1, getOrientation(hb1, ha2, ha1), inSolidA, inSolidB);
                separateEdgeSequence(ha2, ha1, 0, inSolidA, inSolidB);
            }
            else {
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
                    System.out.println("    . PARALLEL CASE");
                }
                separateEdgeSequence(ha2, ha1, 0, inSolidA, inSolidB);
                separateEdgeSequence(hb1, hb2, 1, inSolidA, inSolidB);
            }
            if ( i == data.sectors.size() ) {
                return;
            }
        }
    }

    /**
    Vertex/Vertex classifier for the set operations algorithm (big phase 2).
    Following program [MANT1988].15.6. Similar in structure to program
    [MANT1988].14.3.
    */
    private static void vertexVertexClassify(
        _PolyhedralBoundedSolidVertex va,
        _PolyhedralBoundedSolidVertex vb,
        int op,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidSetVertexVertexClassifier
            .VertexVertexClassificationData data;

        data = _PolyhedralBoundedSolidSetVertexVertexClassifier.classify(
            va, vb, op, debugFlags);
        vertexVertexInsertNullEdges(data, inSolidA, inSolidB);
    }

    /**
    Main control algorithm for the big phases 1 and 2. This calls the
    classifiers for vertex/face and vertex/vertex coincidences found on
    `setOpGenerate`.
    Following section [MANT1988].16.6.1. and program [MANT1988].15.5.
    */
    private static void setOpClassify(int op,
                                      PolyhedralBoundedSolid inSolidA,
                                      PolyhedralBoundedSolid inSolidB)
    {
        int i;

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 1.A. ----------------------------------------------------------------------------------------------------------------------------------------------------");
                System.out.println("VERTICES OF {A} TOUCHING FACES ON {B} (sonva array of " + sonva.size() + " matches)");
        }

        for ( i = 0; i < sonva.size(); i++ ) {
            _PolyhedralBoundedSolidSetVertexFaceClassifier.classify(
                sonva.get(i).v, sonva.get(i).f, op, 0, debugFlags,
                sonea, soneb, inSolidA, inSolidB);
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 1.B. ----------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("VERTICES OF {B} TOUCHING FACES ON {A} (sonvb array of " + sonvb.size() + " matches):");
        }

        for ( i = 0; i < sonvb.size(); i++ ) {
            _PolyhedralBoundedSolidSetVertexFaceClassifier.classify(
                sonvb.get(i).v, sonvb.get(i).f, op, 1, debugFlags,
                sonea, soneb, inSolidA, inSolidB);
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 2. ------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("VERTEX-VERTEX PAIRS (sonvv array of " + sonvv.size() + " pairs):");
        }

        for ( i = 0; i < sonvv.size(); i++ ) {
            vertexVertexClassify(sonvv.get(i).va, sonvv.get(i).vb, op, inSolidA, inSolidB);
        }
    }
}
