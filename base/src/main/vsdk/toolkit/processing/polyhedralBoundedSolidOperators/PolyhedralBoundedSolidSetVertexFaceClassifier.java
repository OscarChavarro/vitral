//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Classification stages for vertex/face coincidences during set operations,
starting from the splitting-classifier rules of [MANT1988].14.5 and adapted
to boundary classification by [MANT1988].15.6.1 and problem [MANT1988].15.4.
*/
final class PolyhedralBoundedSolidSetVertexFaceClassifier
    extends PolyhedralBoundedSolidOperator
{
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_03_VERTEX_FACE_CLASSIFIER = 0x04;
    private static final int DEBUG_99_SHOW_OPERATIONS = 0x40;

    private static int debugFlags;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;

    /**
    Vertex/Face classifier for the set operations algorithm (big phase 1).
    Answer to problem [MANT1988].15.4.
    */
    static void classify(_PolyhedralBoundedSolidVertex v,
                         _PolyhedralBoundedSolidFace f,
                         int op,
                         int BvsA,
                         int flags,
                         ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSonea,
                         ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSoneb,
                         PolyhedralBoundedSolid inSolidA,
                         PolyhedralBoundedSolid inSolidB)
    {
        debugFlags = flags;
        sonea = inSonea;
        soneb = inSoneb;
        vertexFaceClassify(v, f, op, BvsA, inSolidA, inSolidB);
    }

    private static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static void applyCoplanarRulesToVertexFaceNeighborhood(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane,
        int BvsA,
        int op,
        boolean useMirrorFace)
    {
        PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .applyCoplanarRulesToVertexFaceNeighborhood(
                nbr, referenceFace, referencePlane, BvsA, op, useMirrorFace);
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

        return m + 1;
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    This is the inward-oriented variant of the bisector from problem
    [MANT1988].14.1 used by the vertex/face classifier.
    */
    private static Vector3D inside(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3D middle;
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

    /**
    Current method is the first step for the initial vertex/face
    classification of sectors (vertex neighborhood) for `vtx`, as indicated on
    section [MANT1988].14.5.2 and program [MANT1988].14.4, but biased towards
    the set operator classifier as proposed on section [MANT1988].15.6.1 and
    problem [MANT1988].15.4.
    */
    private static
    ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>
    vertexFaceGetNeighborhood(
        _PolyhedralBoundedSolidVertex vtx,
        InfinitePlane referencePlane,
        int BvsA)
    {
        _PolyhedralBoundedSolidHalfEdge he;
        Vector3D bisect;
        double d;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace c;

        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> neighborSectorsInfo;
        neighborSectorsInfo =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>();

        he = vtx.emanatingHalfEdge;
        do {
            c = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();
            c.sector = he;
            d = referencePlane.pointDistance((he.next()).startingVertex.position);
            c.cl = compareToZero(d);
            c.isWide = false;
            c.position = new Vector3D((he.next()).startingVertex.position);
            c.situation =
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.UNDEFINED;
            c.referencePlane = referencePlane;
            neighborSectorsInfo.add(c);
            if ( checkWideness(he) ) {
                bisect = inside(he).add(vtx.position);
                c.situation =
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.CROSSING_EDGE;

                c = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();
                c.sector = he;
                d = referencePlane.pointDistance(bisect);
                c.cl = compareToZero(d);
                c.isWide = true;
                c.position = new Vector3D(bisect);
                c.situation =
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.CROSSING_EDGE;
                c.referencePlane = referencePlane;
                neighborSectorsInfo.add(c);
            }
            he = (he.mirrorHalfEdge()).next();
        } while ( he != vtx.emanatingHalfEdge );

        int i;

        for ( i = 0; i < neighborSectorsInfo.size(); i++ ) {
            c = neighborSectorsInfo.get(i);
            if ( c.cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.ON &&
                 c.situation == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.UNDEFINED ) {
                c.situation =
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.INPLANE_EDGE;
            }
        }

        return neighborSectorsInfo;
    }

    /**
    Current method applies the first reclassification rule presented at
    sections [MANT1988].14.5.1 and [MANT1988].14.5.2, but biased towards the
    set operator classifier as proposed on section [MANT1988].15.6.1 and
    problem [MANT1988].15.4. Following program [MANT1988].14.5.
    */
    private static void vertexFaceReclassifyOnSectorsNoPeekVersion(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane,
        int BvsA,
        int op)
    {
        applyCoplanarRulesToVertexFaceNeighborhood(
            nbr, referenceFace, referencePlane, BvsA, op, false);
    }

    /**
    Current method applies the first reclassification rule presented at
    sections [MANT1988].14.5.1 and [MANT1988].14.5.2, but biased towards the
    set operator classifier as proposed on section [MANT1988].15.6.1 and
    problem [MANT1988].15.4. Following program [MANT1988].14.5.
    */
    private static void vertexFaceReclassifyOnSectors(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane,
        int BvsA,
        int op)
    {
        applyCoplanarRulesToVertexFaceNeighborhood(
            nbr, referenceFace, referencePlane, BvsA, op, true);
    }

    private static void printNbr(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> neighborSectorsInfo)
    {
        int i;

        for ( i = 0; i < neighborSectorsInfo.size(); i++ ) {
            System.out.println("    . " + neighborSectorsInfo.get(i));
        }
    }

    private static boolean inplaneEdgesOn(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr)
    {
        int i;

        for ( i = 0; i < nbr.size(); i++ ) {
            if ( nbr.get(i).situation ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.INPLANE_EDGE ) {
                return true;
            }
        }
        return false;
    }

    private static void vertexFaceReclassifyOnEdges(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int op,
        boolean useBorrowed)
    {
        if ( useBorrowed ) {
            vertexFaceReclassifyOnEdgesBorrowed(nbr, op);
        }
        else {
            vertexFaceReclassifyOnEdgesNoPeekVersion(nbr, op);
        }
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3
    for the edge reclassification rules.
    */
    private static void vertexFaceReclassifyOnEdgesNoPeekVersion(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int op)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace l;
        int i;

        for ( i = 0; i < nbr.size(); i++ ) {
            l = nbr.get(i);
            l.applyRules(op);
        }
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3
    for the edge reclassification rules.
    */
    private static void vertexFaceReclassifyOnEdgesBorrowed(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int op)
    {
        int i;
        int nnbr = nbr.size();

        for ( i = 0; i < nnbr; i++ ) {
            if ( nbr.get(i).cl ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                if ( nbr.get((nnbr + i - 1) % nnbr).cl ==
                     _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                    if ( nbr.get((i + 1) % nnbr).cl ==
                         _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                        nbr.get(i).cl =
                            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    }
                    else {
                        nbr.get(i).cl =
                            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    }
                }
                else {
                    if ( nbr.get((i + 1) % nnbr).cl ==
                         _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                        nbr.get(i).cl =
                            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    }
                    else {
                        nbr.get(i).cl =
                            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
                    }
                }
            }
        }
    }

    private static void vertexFaceInsertNullEdges(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA,
        boolean useBorrowed,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        if ( useBorrowed ) {
            vertexFaceInsertNullEdgesBorrowed(
                nbr, f, v, BvsA, inSolidA, inSolidB);
        }
        else {
            vertexFaceInsertNullEdgesNoPeekVersion(
                nbr, f, v, BvsA, inSolidA, inSolidB);
        }
    }

    /**
    This method implements the third stage of the vertex/face classifier:
    given the previously reclassified list of vertex neighbors, insert a new
    vertex in the direction of the last "in" before an "out" sector of the
    sequence. This follows section [MANT1988].14.6.2 and program
    [MANT1988].14.7, biased for set operations as indicated by
    [MANT1988].15.6.1.
    */
    private static void vertexFaceInsertNullEdgesNoPeekVersion(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        int start, i;
        _PolyhedralBoundedSolidHalfEdge head, tail;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;
        PolyhedralBoundedSolid solida;
        int nnbr = nbr.size();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;

        solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;

        if ( nnbr <= 0 ) {
            return;
        }
        n = nbr.get(0);

        i = 0;
        while ( !((nbr.get(i).cl ==
                   _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AinB ||
                   nbr.get(i).cl ==
                   _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BinA) &&
                  ((nbr.get((i + 1) % nnbr).cl ==
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AoutB) ||
                   nbr.get((i + 1) % nnbr).cl ==
                   _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BoutA)) ) {
            i++;
            if ( i >= nnbr ) {
                return;
            }
        }
        start = i;
        head = nbr.get(i).sector;

        while ( true ) {
            while ( !((nbr.get(i).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AoutB ||
                       nbr.get(i).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BoutA) &&
                      (nbr.get((i + 1) % nnbr).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AinB ||
                       nbr.get((i + 1) % nnbr).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BinA)) ) {
                i = (i + 1) % nnbr;
            }
            tail = nbr.get(i).sector;

            if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 &&
                 (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
                System.out.println("       -> LMEV (Vertex/face split):");
                System.out.println("          . (" + start + ") H1: " + head);
                System.out.println("          . (" + i + ") H2: " + tail);
            }

            solida.lmev(head, tail, nextVertexId(inSolidA, inSolidB),
                head.startingVertex.position);

            if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 &&
                 (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
                System.out.println("          . New vertex: " + head.startingVertex.id);
            }

            if ( BvsA != 0 ) {
                sone = soneb;
            }
            else {
                sone = sonea;
            }
            sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(
                head.previous().parentEdge));

            makeRing(f, v, BvsA, inSolidA, inSolidB);

            while ( !((nbr.get(i).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AinB ||
                       nbr.get(i).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BinA) &&
                      ((nbr.get((i + 1) % nnbr).cl ==
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AoutB) ||
                       nbr.get((i + 1) % nnbr).cl ==
                       _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BoutA)) ) {
                i = (i + 1) % nnbr;
                if ( i == start ) {
                    return;
                }
            }
        }
    }

    private static void vertexFaceInsertNullEdgesBorrowed(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        int start, i;
        _PolyhedralBoundedSolidHalfEdge head, tail;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;
        PolyhedralBoundedSolid solida;
        int nnbr = nbr.size();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;

        solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;

        if ( nnbr <= 0 ) {
            return;
        }
        n = nbr.get(0);

        i = 0;
        while ( !(nbr.get(i).cl ==
                  _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN &&
                  nbr.get((i + 1) % nnbr).cl ==
                  _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
            i++;
            if ( i >= nnbr ) {
                return;
            }
        }
        start = i;
        head = nbr.get(i).sector;

        while ( true ) {
            while ( !(nbr.get(i).cl ==
                      _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT &&
                      nbr.get((i + 1) % nnbr).cl ==
                      _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN) ) {
                i = (i + 1) % nnbr;
            }
            tail = nbr.get(i).sector;

            if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 &&
                 (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
                System.out.println("       -> LMEV (Vertex/face split):");
                System.out.println("          . (" + start + ") H1: " + head);
                System.out.println("          . (" + i + ") H2: " + tail);
            }
            solida.lmev(head, tail, nextVertexId(inSolidA, inSolidB),
                head.startingVertex.position);

            if ( BvsA != 0 ) {
                sone = soneb;
            }
            else {
                sone = sonea;
            }
            sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(
                head.previous().parentEdge));

            makeRing(f, v, BvsA, inSolidA, inSolidB);

            while ( !(nbr.get(i).cl ==
                      _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN &&
                      nbr.get((i + 1) % nnbr).cl ==
                      _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                i = (i + 1) % nnbr;
                if ( i == start ) {
                    return;
                }
            }
        }
    }

    /**
    Vertex/Face classifier for the set operations algorithm (big phase 1).
    Answer to problem [MANT1988].15.4.
    */
    private static void vertexFaceClassify(
        _PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidFace f,
        int op,
        int BvsA,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr;

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 ) {
                System.out.print("  * ");
            }
            else {
                System.out.print("  - ");
            }
            System.out.println("Vertex/face pair V[" + v.id + "] / f[" + f.id + "]");
        }

        nbr = vertexFaceGetNeighborhood(v, f.containingPlane, BvsA);
        if ( inplaneEdgesOn(nbr) ) {
            Collections.reverse(nbr);
        }

        if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 ) {
            System.out.println(
                "   - Initial sector neigborhood by near end vertices:");
            printNbr(nbr);
        }

        vertexFaceReclassifyOnSectorsNoPeekVersion(
            nbr, f, f.containingPlane, BvsA, op);

        boolean borrowed = false;

        int i;
        for ( i = 0; !borrowed && i < nbr.size(); i++ ) {
            nbr.get(i).updateLabel(BvsA);
        }

        if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 ) {
            System.out.println(
                "   - Sector neigborhood reclassified on sectors (8-way boundary classification):");
            printNbr(nbr);
        }

        vertexFaceReclassifyOnEdges(nbr, op, borrowed);

        if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 ) {
            System.out.println("   - Sector neigborhood reclassified on edges:");
            printNbr(nbr);
        }

        vertexFaceInsertNullEdges(nbr, f, v, BvsA, borrowed, inSolidA, inSolidB);
    }

    private static void makeRing(
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int type,
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        PolyhedralBoundedSolid solida, solidb;
        _PolyhedralBoundedSolidHalfEdge he;

        solida = inSolidA;
        solidb = inSolidB;
        if ( type == 1 ) {
            solida = inSolidB;
            solidb = inSolidA;
        }

        he = f.boundariesList.get(0).boundaryStartHalfEdge;

        int vn1, vn2;
        vn1 = nextVertexId(solida, solidb);
        solidb.lmev(he, he, vn1, v.position);
        he = solidb.findVertex(vn1).emanatingHalfEdge;
        solidb.lkemr(he.mirrorHalfEdge(), he);

        vn2 = nextVertexId(solida, solidb);
        solidb.lmev(he, he, vn2, v.position);

        if ( (debugFlags & DEBUG_03_VERTEX_FACE_CLASSIFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOW_OPERATIONS) != 0x00 ) {
            System.out.println("       -> MAKE_RING (Vertex/face pierce):");
            System.out.println("          . New vertexes: " + vn1 + "/" + vn2 + ".");
        }

        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;
        if ( type == 1 ) {
            sone = sonea;
        }
        else {
            sone = soneb;
        }
        sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(he.parentEdge));
    }
}
