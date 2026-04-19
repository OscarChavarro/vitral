//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Geometric preprocessing and sector reclassification for vertex/vertex matches,
following section [MANT1988].15.6.2 and programs [MANT1988].15.7 through
[MANT1988].15.10.
*/
final class PolyhedralBoundedSolidSetVertexVertexClassifier
    extends PolyhedralBoundedSolidOperator
{
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_04_VERTEX_VERTEX_CLASSIFIER = 0x08;

    static final class VertexVertexClassificationData
    {
        final ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nba;
        final ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nbb;
        final ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector> sectors;

        VertexVertexClassificationData(
            ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> inNba,
            ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> inNbb,
            ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector> inSectors)
        {
            nba = inNba;
            nbb = inNbb;
            sectors = inSectors;
        }
    }

    private static int debugFlags;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nba;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nbb;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector> sectors;

    /**
    Vertex/Vertex classifier for the set operations algorithm (big phase 2).
    Following program [MANT1988].15.6. Similar in structure to program
    [MANT1988].14.3.
    */
    static VertexVertexClassificationData classify(
        _PolyhedralBoundedSolidVertex va,
        _PolyhedralBoundedSolidVertex vb,
        int op,
        int flags)
    {
        debugFlags = flags;

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) == 0x00 ) {
                System.out.print("  - ");
            }
            else {
                System.out.print("  * ");
            }
            System.out.print(
                "Vertex of {A} / Vertex of {B} pair: A[" + va.id + "] / B[" + vb.id + "]");
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) == 0x00 ) {
                System.out.println(".");
            }
            else {
                System.out.println(" ->");
            }
        }

        vertexVertexGetNeighborhood(va, vb);

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
            System.out.println("   - Initial sector/sector intersection candidates:");
            for ( int i = 0; i < sectors.size(); i++ ) {
                System.out.println("    . " + sectors.get(i));
            }
        }

        vertexVertexReclassifyOnSectors(op);

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
            System.out.println("   - On sector reclassified:");
            for ( int i = 0; i < sectors.size(); i++ ) {
                System.out.println("    . " + sectors.get(i));
            }
        }

        vertexVertexReclassifyOnEdges(op);

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0x00 ) {
            System.out.println("   - On edges reclassified:");
            for ( int i = 0; i < sectors.size(); i++ ) {
                System.out.println("    . " + sectors.get(i));
            }
        }

        return new VertexVertexClassificationData(nba, nbb, sectors);
    }

    /**
    Following program [MANT1988].15.8.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>
    nbrpreproc(_PolyhedralBoundedSolidVertex v)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex n, nold;
        Vector3D bisec;
        _PolyhedralBoundedSolidHalfEdge he;
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nb;

        nb = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>();

        he = v.emanatingHalfEdge;
        Vector3D oldref2;

        do {
            n = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
            n.he = he;
            n.wide = false;

            n.ref1 = he.previous().startingVertex.position.substract(
                he.startingVertex.position);
            n.ref2 = he.next().startingVertex.position.substract(
                he.startingVertex.position);
            n.ref12 = n.ref1.crossProduct(n.ref2);

            if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                     n.ref1, n.ref2, numericContext) ||
                 (n.ref12.dotProduct(
                     he.parentLoop.parentFace.containingPlane.getNormal()) > 0.0 ) ) {
                if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                         n.ref1, n.ref2, numericContext) ) {
                    bisec = PolyhedralBoundedSolidSetClassifier.inside(he);
                }
                else {
                    bisec = n.ref1.add(n.ref2);
                    bisec = bisec.multiply(-1);
                }
                oldref2 = n.ref2;
                n.ref2 = bisec;
                n.ref12 = n.ref1.crossProduct(n.ref2);
                nold = n;
                nb.add(n);

                n = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
                n.he = he;
                n.ref2 = oldref2;
                n.ref1 = bisec;
                n.ref12 = n.ref1.crossProduct(n.ref2);
                n.wide = true;
            }

            nb.add(n);

            he = (he.mirrorHalfEdge()).next();
        } while( he != v.emanatingHalfEdge );

        return nb;
    }

    /**
    Checks if two coplanar sectors overlap, by doing the coplanar
    sector-within test required by section [MANT1988].15.6.2.
    */
    private static boolean sectoroverlap(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sectoroverlap(na, nb,
                (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0);
    }

    /**
    Following program [MANT1988].15.9. According to the sector intersection
    test from section [MANT1988].15.6.2, the variables are interpreted as in
    figure [MANT1988].15.8 and equation [MANT1988].15.5.
    */
    private static boolean sctrwitthin(Vector3D dir, Vector3D ref1,
                                       Vector3D ref2, Vector3D ref12)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sctrwitthin(dir, ref1, ref2, ref12);
    }

    private static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .compareToZero(value);
    }

    private static int resolveCoplanarVertexVertexClass(int op,
                                                        boolean sameOrientation,
                                                        boolean sideA)
    {
        return PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .resolveCoplanarVertexVertexClass(op, sameOrientation, sideA);
    }

    /**
    Sector intersection test.
    Following program [MANT1988].15.9 and section [MANT1988].15.6.2.
    */
    private static boolean vertexVertexSectorIntersectionTest(int i, int j)
    {
        _PolyhedralBoundedSolidHalfEdge h1, h2;
        boolean c1, c2;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na, nb;

        na = nba.get(i);
        nb = nbb.get(j);
        h1 = na.he;
        h2 = nb.he;

        Vector3D n1, n2;
        Vector3D intrs;

        n1 = h1.parentLoop.parentFace.containingPlane.getNormal();
        n2 = h2.parentLoop.parentFace.containingPlane.getNormal();
        intrs = n1.crossProduct(n2);

        if ( PolyhedralBoundedSolidNumericPolicy.unitVectorsParallel(
            n1, n2, numericContext) ) {
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                System.out.print(" <coplanar>");
            }
            return sectoroverlap(na, nb);
        }

        c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
        c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
        if ( c1 && c2 ) {
            if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                System.out.print(" <TRUE>");
            }
            return true;
        }
        else {
            intrs = intrs.multiply(-1);
            c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
            c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
            if ( c1 && c2 ) {
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                    System.out.print(" <TRUE>");
                }
                return true;
            }
        }

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
            System.out.print(" <FALSE>");
        }

        return false;
    }

    /**
    Given a pair of coincident vertices `va` and `vb`, this method creates the
    lists `nba`, `nbb`, and `sectors` as explained in section
    [MANT1988].15.6.2 and program [MANT1988].15.7.
    */
    private static void vertexVertexGetNeighborhood(
        _PolyhedralBoundedSolidVertex va,
        _PolyhedralBoundedSolidVertex vb)
    {
        int i;

        nba = nbrpreproc(va);
        nbb = nbrpreproc(vb);
        sectors = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector>();

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
            System.out.println("   - NBA list of neighbor sectors for vertex on {A}:");
            for ( i = 0; i < nba.size(); i++ ) {
                System.out.println("    . A[" + (i + 1) + "]: " + nba.get(i));
            }
            System.out.println("   - NBB list of neighbor sectors for vertex on {B}:");
            for ( i = 0; i < nbb.size(); i++ ) {
                System.out.println("    . B[" + (i + 1) + "]: " + nbb.get(i));
            }
        }

        _PolyhedralBoundedSolidHalfEdge ha, hb;
        double d1, d2, d3, d4;
        int j;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector s;
        Vector3D na, nb;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex xa, xb;

        if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
            System.out.println(
                "   - Initial intersection tests between sectors (false intersections are sectors touching on a single point):");
        }

        for ( i = 0; i < nba.size(); i++ ) {
            for ( j = 0; j < nbb.size(); j++ ) {
                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                    System.out.print("    . A[" + (i + 1) + "] / B[" + (j + 1) + "]:");
                }

                if ( vertexVertexSectorIntersectionTest(i, j) ) {
                    s = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector();
                    s.secta = i;
                    s.sectb = j;
                    xa = nba.get(i);
                    xb = nbb.get(j);
                    s.hea = xa.he;
                    s.heb = xb.he;
                    s.wa = xa.wide;
                    s.wb = xb.wide;

                    na = xa.he.parentLoop.parentFace.containingPlane.getNormal();
                    nb = xb.he.parentLoop.parentFace.containingPlane.getNormal();
                    d1 = nb.dotProduct(xa.ref1);
                    d2 = nb.dotProduct(xa.ref2);
                    d3 = na.dotProduct(xb.ref1);
                    d4 = na.dotProduct(xb.ref2);
                    s.s1a = compareToZero(d1);
                    s.s2a = compareToZero(d2);
                    s.s1b = compareToZero(d3);
                    s.s2b = compareToZero(d4);
                    s.intersect = true;
                    sectors.add(s);
                }

                if ( (debugFlags & DEBUG_04_VERTEX_VERTEX_CLASSIFIER) != 0 ) {
                    System.out.print("\n");
                }
            }
        }
    }

    /**
    Following section [MANT1988].15.6.2 and program [MANT1988].15.10.
    */
    private static void vertexVertexReclassifyOnSectors(int op)
    {
        _PolyhedralBoundedSolidHalfEdge ha, hb;
        int i, j, newsa, newsb;
        boolean sameOrientation;
        int secta, prevsecta, nextsecta;
        int sectb, prevsectb, nextsectb;
        double d;
        Vector3D n1, n2;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector si, sj;

        for ( i = 0; i < sectors.size(); i++ ) {
            if ( sectors.get(i).s1a ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s2a ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s1b ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s2b ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0) ? nba.size() - 1 : secta - 1;
                prevsectb = (sectb == 0) ? nbb.size() - 1 : sectb - 1;
                nextsecta = (secta == nba.size() - 1) ? 0 : secta + 1;
                nextsectb = (sectb == nbb.size() - 1) ? 0 : sectb + 1;
                ha = nba.get(secta).he;
                hb = nbb.get(sectb).he;
                n1 = ha.parentLoop.parentFace.containingPlane.getNormal();
                n2 = hb.parentLoop.parentFace.containingPlane.getNormal();
                d = VSDK.vectorDistance(n1, n2);
                sameOrientation = ( d < numericContext.unitVectorTolerance() );
                newsa = resolveCoplanarVertexVertexClass(op, sameOrientation, true);
                newsb = resolveCoplanarVertexVertexClass(op, sameOrientation, false);
                si = sectors.get(i);

                for ( j = 0; j < sectors.size(); j++ ) {
                    sj = sectors.get(j);
                    if ( (sj.secta == prevsecta) && (sj.sectb == sectb) ) {
                        if ( sj.s1a !=
                             _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s2a = newsa;
                        }
                    }
                    if ( (sj.secta == nextsecta) && (sj.sectb == sectb) ) {
                        if ( sj.s2a !=
                             _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s1a = newsa;
                        }
                    }
                    if ( (sj.secta == secta) && (sj.sectb == prevsectb) ) {
                        if ( sj.s1b !=
                             _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s2b = newsb;
                        }
                    }
                    if ( (sj.secta == secta) && (sj.sectb == nextsectb) ) {
                        if ( sj.s2b !=
                             _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s1b = newsb;
                        }
                    }
                    if ( (sj.s1a == sj.s2a) &&
                         (sj.s1a ==
                          _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                          sj.s1a ==
                          _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                        sj.intersect = false;
                    }
                    if ( (sj.s1b == sj.s2b) &&
                         (sj.s1b ==
                          _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                          sj.s1b ==
                          _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                        sj.intersect = false;
                    }
                }

                si.s1a = si.s2a = newsa;
                si.s1b = si.s2b = newsb;
                si.intersect = false;
            }
        }
    }

    /**
    Reclassification procedure for "on"-edges on the vertex/vertex classifier,
    as expected from the high-level description of section [MANT1988].15.6.2
    and the case structures of figures [MANT1988].15.10, [MANT1988].15.11,
    and [MANT1988].15.12.
    */
    private static void vertexVertexReclassifyOnEdges(int op)
    {
        int i, j, newsa, newsb;
        int secta, prevsecta;
        int sectb, prevsectb;

        for ( i = 0; i < sectors.size(); i++ ) {
            if ( sectors.get(i).intersect &&
                 sectors.get(i).s1a ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s1b ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                newsa = (op == PolyhedralBoundedSolidSetClassifier.UNION) ?
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT :
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                newsb = (op == PolyhedralBoundedSolidSetClassifier.UNION) ?
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN :
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;

                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0) ? nba.size() - 1 : secta - 1;
                prevsectb = (sectb == 0) ? nbb.size() - 1 : sectb - 1;

                for ( j = 0; j < sectors.size(); j++ ) {
                    if ( sectors.get(j).intersect ) {
                        if ( (sectors.get(j).secta == secta) &&
                             (sectors.get(j).sectb == sectb) ) {
                            sectors.get(j).s1a = newsa;
                            sectors.get(j).s1b = newsb;
                        }

                        if ( (sectors.get(j).secta == prevsecta) &&
                             (sectors.get(j).sectb == sectb) ) {
                            sectors.get(j).s2a = newsa;
                            sectors.get(j).s1b = newsb;
                        }

                        if ( (sectors.get(j).secta == secta) &&
                             (sectors.get(j).sectb == prevsectb) ) {
                            sectors.get(j).s1a = newsa;
                            sectors.get(j).s2b = newsb;
                        }

                        if ( (sectors.get(j).secta == prevsecta) &&
                             (sectors.get(j).sectb == prevsectb) ) {
                            sectors.get(j).s2a = newsa;
                            sectors.get(j).s2b = newsb;
                        }

                        if ( sectors.get(j).s1a == sectors.get(j).s2a &&
                             (sectors.get(j).s1a ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                              sectors.get(j).s1a ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                        if ( sectors.get(j).s1b == sectors.get(j).s2b &&
                             (sectors.get(j).s1b ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                              sectors.get(j).s1b ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
        }

        for ( i = 0; i < sectors.size(); i++ ) {
            if ( sectors.get(i).intersect &&
                 sectors.get(i).s1a ==
                 _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0) ? nba.size() - 1 : secta - 1;
                prevsectb = (sectb == 0) ? nbb.size() - 1 : sectb - 1;
                newsa = (op == PolyhedralBoundedSolidSetClassifier.UNION) ?
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT :
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;

                for ( j = 0; j < sectors.size(); j++ ) {
                    if ( sectors.get(j).intersect ) {
                        if ( (sectors.get(j).secta == secta) &&
                             (sectors.get(j).sectb == sectb) ) {
                            sectors.get(j).s1a = newsa;
                        }

                        if ( (sectors.get(j).secta == prevsecta) &&
                             (sectors.get(j).sectb == sectb) ) {
                            sectors.get(j).s2a = newsa;
                        }

                        if ( sectors.get(j).s1a == sectors.get(j).s2a &&
                             (sectors.get(j).s1a ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                              sectors.get(j).s1a ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
            else if ( sectors.get(i).intersect &&
                      sectors.get(i).s1b ==
                      _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0) ? nba.size() - 1 : secta - 1;
                prevsectb = (sectb == 0) ? nbb.size() - 1 : sectb - 1;
                newsb = (op == PolyhedralBoundedSolidSetClassifier.UNION) ?
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT :
                    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;

                for ( j = 0; j < sectors.size(); j++ ) {
                    if ( sectors.get(j).intersect ) {
                        if ( (sectors.get(j).secta == secta) &&
                             (sectors.get(j).sectb == sectb) ) {
                            sectors.get(j).s1b = newsb;
                        }

                        if ( (sectors.get(j).secta == secta) &&
                             (sectors.get(j).sectb == prevsectb) ) {
                            sectors.get(j).s2b = newsb;
                        }

                        if ( sectors.get(j).s1b == sectors.get(j).s2b &&
                             (sectors.get(j).s1b ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                              sectors.get(j).s1b ==
                              _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
        }
    }
}
