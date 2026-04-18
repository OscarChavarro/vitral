//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Classification stages, connection stages, and no-intersection containment
logic for the Boolean set-operations pipeline of chapter [MANT1988].15.
*/
final class PolyhedralBoundedSolidSetClassifier
    extends PolyhedralBoundedSolidOperator
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
        return setOpNoIntersectionCase(inSolidA, inSolidB, outRes, op);
    }

    static boolean runTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return isTouchingOnlyPreflightCase(inSolidA, inSolidB);
    }

    private static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static int pointInFace(_PolyhedralBoundedSolidFace face, Vector3D point)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .pointInFace(face, point);
    }

    private static int resolveCoplanarVertexVertexClass(int op,
        boolean sameOrientation, boolean sideA)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
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

        a = (he.next()).startingVertex.position.substract(
            he.startingVertex.position);
        b = (he.previous()).startingVertex.position.substract(
            he.startingVertex.position);
        a.normalize();
        b.normalize();

        n = he.parentLoop.parentFace.containingPlane.getNormal();

        middle = n.crossProduct(a);
        middle.normalize();

        return middle;
    }

    private static boolean sctrwitthin(Vector3D dir, Vector3D ref1,
                            Vector3D ref2, Vector3D ref12)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
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
        dir = h2.startingVertex.position.substract(he.startingVertex.position);
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
        PolyhedralBoundedSolidSetVertexVertexClassifier.VertexVertexClassificationData data,
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
        PolyhedralBoundedSolidSetVertexVertexClassifier
            .VertexVertexClassificationData data;

        data = PolyhedralBoundedSolidSetVertexVertexClassifier.classify(
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
            PolyhedralBoundedSolidSetVertexFaceClassifier.classify(
                sonva.get(i).v, sonva.get(i).f, op, 0, debugFlags,
                sonea, soneb, inSolidA, inSolidB);
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 1.B. ----------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("VERTICES OF {B} TOUCHING FACES ON {A} (sonvb array of " + sonvb.size() + " matches):");
        }

        for ( i = 0; i < sonvb.size(); i++ ) {
            PolyhedralBoundedSolidSetVertexFaceClassifier.classify(
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
    private static int classifyPointAgainstSolid(
        PolyhedralBoundedSolid solid,
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

        // Boundary quick check.
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

        // Retry with a few skewed directions to avoid degenerate rays.
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
                if ( !face.containingPlane.doIntersection(rayHit) ) {
                    continue;
                }
                if ( rayHit.t <= eps ) {
                    continue;
                }

                Vector3D pi = rayHit.origin.add(
                    rayHit.direction.multiply(rayHit.t));
                int status = face.testPointInside(pi, eps);
                if ( status == Geometry.LIMIT ) {
                    ambiguous = true;
                    break;
                }
                if ( status == Geometry.INSIDE ) {
                    boolean duplicated = false;
                    int k;
                    for ( k = 0; k < distances.size(); k++ ) {
                        if ( Math.abs(distances.get(k).doubleValue() - rayHit.t)
                             <= eps ) {
                            duplicated = true;
                            break;
                        }
                    }
                    if ( !duplicated ) {
                        distances.add(Double.valueOf(rayHit.t));
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
    Classifies if at least one non-boundary vertex of `solidA` lies inside
    `solidB`.
    */
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

    /*
    No-intersection classifier policy:
      - INSIDE means one solid is volumetrically contained in the other.
      - LIMIT means touching-only (point/edge/line contact, non-volumetric).
      - OUTSIDE means disjoint.
    */
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

    /**
    Preflight detector for proper edge/face crossings. This mirrors the
    geometric test from [MANT1988].15.3 without mutating topology, so the
    touching-only no-intersection policy can be applied before reduction.
    */
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
            if ( edge == null || edge.rightHalf == null || edge.leftHalf == null ) {
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
                    v2.position.substract(v1.position).multiply(t));
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

    private static boolean isTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        int aInB = classifySolidAgainstSolid(inSolidA, inSolidB);
        int bInA = classifySolidAgainstSolid(inSolidB, inSolidA);
        int relation = classifyNoIntersectionRelation(aInB, bInA);

        if ( relation != NO_INT_RELATION_TOUCHING ) {
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

    /**
    Handles no-intersection cases (book problem [MANT1988].15.1).
    */
    private static PolyhedralBoundedSolid setOpNoIntersectionCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op)
    {
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
            // Touching-only and disjoint return empty intersection.
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
                // Touching-only and disjoint both merge the two solids.
                outRes.merge(inSolidA);
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        // DIFFERENCE
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
        // Touching-only and disjoint keep minuend unchanged.
        outRes.merge(inSolidA);
        return outRes;
    }
}
