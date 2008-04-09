//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 1 2008 - Oscar Chavarro: Original base version                  =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.processing;

// Java classes
import java.util.ArrayList;
import java.util.Collections;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

/**
Class `_PolyhedralBoundedSolidSplitterNullEdge` plays a role of a decorator
design patern for class `_PolyhedralBoundedSolidEdge`, and adds sort-ability.
*/
class _PolyhedralBoundedSolidSetOperatorNullEdge extends PolyhedralBoundedSolidOperator implements Comparable <_PolyhedralBoundedSolidSetOperatorNullEdge>
{
    public _PolyhedralBoundedSolidEdge e;

    public _PolyhedralBoundedSolidSetOperatorNullEdge(_PolyhedralBoundedSolidEdge e)
    {
        this.e = e;
    }

    public int compareTo(_PolyhedralBoundedSolidSetOperatorNullEdge other)
    {
        Vector3D a;
        Vector3D b;

        a = this.e.rightHalf.startingVertex.position;
        b = other.e.rightHalf.startingVertex.position;

        if ( PolyhedralBoundedSolid.compareValue(a.x, b.x, 10*VSDK.EPSILON) != 0 ) {
            if ( a.x < b.x ) {
                return -1;
            }
            return 1;
        }
        else {
            if ( PolyhedralBoundedSolid.compareValue(a.y, b.y, 10*VSDK.EPSILON) != 0 ) {
                if ( a.y < b.y ) {
                    return -1;
                }
                return 1;
            }
            else {
                if ( a.z < b.z ) {
                    return -1;
                }
                return 1;
            }
        }
    }

}

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex extends PolyhedralBoundedSolidOperator
{
    public _PolyhedralBoundedSolidHalfEdge he;
    public Vector3D ref1;
    public Vector3D ref2;
    public Vector3D ref12;
    public boolean wide;
}

/**
This class is used to store sector / sector neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector extends PolyhedralBoundedSolidOperator
{
    public int secta;
    public int sectb;
    public int s1a;
    public int s2a;
    public int s1b;
    public int s2b;
    public boolean intersect;
    public _PolyhedralBoundedSolidHalfEdge hea;
    public _PolyhedralBoundedSolidHalfEdge heb;
    public boolean wa;
    public boolean wb;
    public static final int ON = 0;
    public static final int OUT = 1;
    public static final int IN = -1;

    private String label(int i)
    {
        String msg = "<Unknown>";
        switch ( i ) {
          case ON: msg = "on"; break;
          case OUT: msg = "OUT"; break;
          case IN: msg = "IN"; break;
        }
        return msg;
    }

    public String toString()
    {
        String msg = "Sector pair ";

        msg = msg + "VERTICES ( " + 
            hea.startingVertex.id + "-" + 
            (hea.next()).startingVertex.id + (wa?"(W)":"(nw)") + " / " + 
            heb.startingVertex.id + "-" +
            (heb.next()).startingVertex.id + (wb?"(W)":"(nw)") + " ) - ";
        msg = msg + "Indexes (" + secta + "/" + sectb + "): ";
        msg = msg + "[" + label(s1a) + "/" + label(s2a) + ", " + label(s1b) + "/" + label(s2b) + "] ";
        if ( intersect ) {
            msg = msg + "intersecting";
        }
        else {
            msg = msg + "not intersecting";
        }

        return msg;
    }
}

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/face classifier, in a similar fashion to as presented in section
[MANT1988].14.5, and program [MANT1988].14.3., but biased for the set
operation algorithm as proposed on section [MANT1988].15..1. and problem
[MANT1988].15.4.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace extends PolyhedralBoundedSolidOperator
{
    public static final int ABOVE = 1;
    public static final int BELOW = -1;
    public static final int ON = 0;

    public static final int AinB = 11;
    public static final int AoutB = 12;
    public static final int BinA = 13;
    public static final int BoutA = 14;
    public static final int AonBplus = 15;
    public static final int AonBminus = 16;
    public static final int BonAplus = 17;
    public static final int BonAminus = 18;

    public static final int COPLANAR_FACE = 10;
    public static final int INPLANE_EDGE = 20;
    public static final int CROSSING_EDGE = 30;
    public static final int UNDEFINED = 40;

    public _PolyhedralBoundedSolidHalfEdge sector;
    public InfinitePlane referencePlane;
    public int cl;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public boolean isWide = false;
    public Vector3D position;
    public int situation = UNDEFINED;

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the reclassification rules.
    */
    public void applyRules(int op)
    {
        if ( op == UNION ) {
            switch ( cl ) {
              case AonBplus:     cl = AoutB;    break;
              case AonBminus:    cl = AinB;    break;
              case BonAplus:     cl = BinA;    break;
              case BonAminus:    cl = BinA;    break;
            }
        }
        else if ( op == INTERSECTION ) {
            switch ( cl ) {
              case AonBplus:     cl = AinB;    break;
              case AonBminus:    cl = AoutB;    break;
              case BonAplus:     cl = BoutA;    break;
              case BonAminus:    cl = BoutA;    break;
            }
        }
        else if ( op == DIFFERENCE ) {
            switch ( cl ) {
              case AonBplus:     cl = AinB;    break;
              case AonBminus:    cl = AoutB;    break;
              case BonAplus:     cl = BoutA;    break;
              case BonAminus:    cl = BoutA;    break;
            }
        }
    }

    public void updateLabel(int BvsA)
    {
        InfinitePlane a = sector.parentLoop.parentFace.containingPlane;
        InfinitePlane b = referencePlane;

        if ( BvsA == 0 ) {
            switch ( cl ) {
              case ABOVE: cl = AoutB; break;
              case BELOW: cl = AinB; break;
              case ON:
                if ( a.overlapsWith(b, VSDK.EPSILON) ) {
                    cl = AonBplus;
                }
                else {
                    cl = AonBminus;
                }
                break;
            }
        }
        else {
            switch ( cl ) {
              case ABOVE: cl = BoutA; break;
              case BELOW: cl = BinA; break;
              case ON:
                if ( a.overlapsWith(b, VSDK.EPSILON) ) {
                    cl = BonAplus;
                }
                else {
                    cl = BonAminus;
                }
                break;
            }
        }
    }

    public String toString()
    {
        String msg = "{";
        msg = msg + sector;
        switch ( cl ) {
          case ABOVE: msg = msg + " ABOVE"; break;
          case BELOW: msg = msg + " BELOW"; break;
          case ON: msg = msg + " ON"; break;
          case AinB: msg = msg + "AinB"; break;
          case AoutB: msg = msg + "AoutB"; break;
          case BinA: msg = msg + "BinA"; break;
          case BoutA: msg = msg + "BoutA"; break;
          case AonBplus: msg = msg + "AonBplus"; break;
          case AonBminus: msg = msg + "AonBminus"; break;
          case BonAplus: msg = msg + "BonAplus"; break;
          case BonAminus: msg = msg + "BonAminus"; break;
          default: msg = msg + "<INVALID!>"; break;
        }
        msg = msg + " ";
        if ( isWide ) {
            msg = msg + "(W) ";
        }
        //msg = msg + ", pos: " + position;

        switch ( situation ) {
          case COPLANAR_FACE: msg = msg + "<COPLANAR_FACE>"; break;
          case INPLANE_EDGE: msg = msg + "<INPLANE_EDGE>"; break;
          case CROSSING_EDGE: msg = msg + "<CROSSING_EDGE>"; break;
          default: msg = msg + "<UNDEFINED>"; break;
        }

        msg = msg + "}";
        return msg;
    }
}

class _PolyhedralBoundedSolidSetOperatorVertexVertex extends PolyhedralBoundedSolidOperator
{
    public _PolyhedralBoundedSolidVertex va;
    public _PolyhedralBoundedSolidVertex vb;

    public String toString() {
        String msg = "(" + va + ") / (" + vb + "}";
        return msg;
    }
}

class _PolyhedralBoundedSolidSetOperatorVertexFace extends PolyhedralBoundedSolidOperator
{
    public _PolyhedralBoundedSolidVertex v;
    public _PolyhedralBoundedSolidFace f;

    public String toString() {
        String msg = "{" + v + " / " + f + "}";
        return msg;
    }
}

public class PolyhedralBoundedSolidSetOperator extends PolyhedralBoundedSolidOperator
{
    /**
    Following variable `sonvv` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv;

    /**
    Following variable `sonva` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva;

    /**
    Following variable `sonvb` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb;

    /**
    Following variable `sonea` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;

    /**
    Following variable `soneb` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;

    /**
    Following variable `sonfa` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfa;

    /**
    Following variable `sonfb` from program [MANT1988].15.1.
    */
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfb;

    /**
    Following variable `nba` from program [MANT1988].15.6.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nba;

    /**
    Following variable `nba` from program [MANT1988].15.6.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nbb;

    /**
    Following variable `sectors` from program [MANT1988].15.6.
    */
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector> sectors;

    /**
    Following variable `endsa` from program [MANT1988].15.13.
    */
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsa;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> tiedsa;

    /**
    Following variable `endsb` from program [MANT1988].15.13.
    */
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsb;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> tiedsb;

    /**
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. This method increments the face and vertex
    identifiers of `solidToUpdate` so that they do not overlap with
    `referenceSolid` identifiers.
    */
    private static void updmaxnames(PolyhedralBoundedSolid solidToUpdate,
                             PolyhedralBoundedSolid referenceSolid)
    {
        _PolyhedralBoundedSolidVertex v;
        _PolyhedralBoundedSolidFace f;
        int i;

        for ( i = 0; i < solidToUpdate.verticesList.size(); i++ ) {
            v = solidToUpdate.verticesList.get(i);
            v.id += referenceSolid.getMaxVertexId()+1;
            if ( v.id > solidToUpdate.maxVertexId ) {
                solidToUpdate.maxVertexId = v.id;
            }
        }

        for ( i = 0; i < solidToUpdate.polygonsList.size(); i++ ) {
            f = solidToUpdate.polygonsList.get(i);
            f.id += referenceSolid.getMaxFaceId()+1;
            if ( f.id > solidToUpdate.maxFaceId ) {
                solidToUpdate.maxFaceId = f.id;
            }
        }
    }

    /**
    */
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
    */
    private static void addsovf(_PolyhedralBoundedSolidHalfEdge he,
                                _PolyhedralBoundedSolidFace f, int BvsA)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidSetOperatorVertexFace elem;
        ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonv;

        if ( BvsA == 0 ) {
            sonv = sonva;
        }
        else {
            sonv = sonvb;
        }

        int i;

        for ( i = 0; i < sonv.size(); i++ ) {
            elem = sonv.get(i);
            if ( elem.v == he.startingVertex && elem.f == f ) {
                return;
            }
        }

        //-----------------------------------------------------------------
        elem = new _PolyhedralBoundedSolidSetOperatorVertexFace();
        elem.v = he.startingVertex;
        elem.f = f;
        sonv.add(elem);
    }

    /**
    */
    private static void addsovv(_PolyhedralBoundedSolidVertex a,
                                _PolyhedralBoundedSolidVertex b, int BvsA)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidSetOperatorVertexVertex elem;
        int i;

        for ( i = 0; i < sonvv.size(); i++ ) {
            elem = sonvv.get(i);
            if ( BvsA == 0 && elem.va == a && elem.vb == b ||
                 BvsA != 0 && elem.va == b && elem.vb == a ) {
                return;
            }
        }

        //-----------------------------------------------------------------
        elem = new _PolyhedralBoundedSolidSetOperatorVertexVertex();
        if ( BvsA == 0 ) {
            elem.va = a;
            elem.vb = b;
        }
        else {
            elem.va = b;
            elem.vb = a;
        }
        sonvv.add(elem);
    }

    /**
    Following [MANT1988].15.4.
    */
    private static void doVertexOnFace(
        _PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidFace f,
        int BvsA,
        PolyhedralBoundedSolid current,
        PolyhedralBoundedSolid other)
    {
        int cont;
        double d;

        d = f.containingPlane.pointDistance(v.position);
        if ( PolyhedralBoundedSolid.compareValue(d, 0.0, VSDK.EPSILON) == 0 ) {
            cont = cont = f.testPointInside(v.position, VSDK.EPSILON);
            if ( cont == Geometry.INSIDE ) {
                addsovf(v.emanatingHalfEdge, f, BvsA);
            }
            else if ( cont == Geometry.LIMIT &&
                      f.lastIntersectedHalfedge != null ) {
                current.lmev(
                    f.lastIntersectedHalfedge,
                    f.lastIntersectedHalfedge.mirrorHalfEdge().next(),
                    nextVertexId(current, other), v.position);
                addsovv(v, f.lastIntersectedHalfedge.startingVertex, BvsA);
            }
            else if ( cont == Geometry.LIMIT &&
                      f.lastIntersectedVertex != null ) {
                addsovv(v, f.lastIntersectedVertex, BvsA);
            }
        }
    }

    /**
    Following program [MANT1988].3.
    */
    private static void doSetOpGenerate(
        _PolyhedralBoundedSolidEdge e,
        _PolyhedralBoundedSolidFace f,
        int BvsA,
        PolyhedralBoundedSolid current,
        PolyhedralBoundedSolid other)
    {
        _PolyhedralBoundedSolidVertex v1, v2;
        double d1, d2, d3, t;
        Vector3D p;
        int s1, s2, cont;

        v1 = e.rightHalf.startingVertex;
        v2 = e.leftHalf.startingVertex;
        d1 = f.containingPlane.pointDistance(v1.position);
        d2 = f.containingPlane.pointDistance(v2.position);
        s1 = PolyhedralBoundedSolid.compareValue(d1, 0.0, VSDK.EPSILON);
        s2 = PolyhedralBoundedSolid.compareValue(d2, 0.0, VSDK.EPSILON);

        if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
            t = d1 / (d1 - d2);
            p = v1.position.add(
                    (v2.position.substract(
                         v1.position)).multiply(t));

            d3 = f.containingPlane.pointDistance(p);
            if ( PolyhedralBoundedSolid.compareValue(d3, 0.0, VSDK.EPSILON) == 0 ) {
                cont = f.testPointInside(p, VSDK.EPSILON);

                if ( cont != Geometry.OUTSIDE ) {
                    current.lmev(e.rightHalf, e.leftHalf.next(),
                                       nextVertexId(current, other), p);

                    if ( cont == Geometry.INSIDE ) {
                        // Reduction step phase 5/6: Edge crosses inside a face
                        // No subdivide?
                        // Reduction step phase 7/8: stop vertex/face
                        addsovf(e.rightHalf, f, BvsA);
                    }
                    else if ( cont == Geometry.LIMIT &&
                              f.lastIntersectedHalfedge != null ) {
                        // Reduction step phase 1: Edge crosses other edge
                        // Subdivide both edges (here one of them, the other
                        // is this same code but when called from other solid),
                        // at their intersection point (`p`), i.e. replace
                        // each edge by two edges and a new vertex lying at `p`.
                        current.lmev(f.lastIntersectedHalfedge,
                            f.lastIntersectedHalfedge.mirrorHalfEdge().next(),
                                     nextVertexId(current, other), p);
                        // Reduction step phase 4: store vertex/vertex
                        addsovv(e.rightHalf.startingVertex, f.lastIntersectedHalfedge.startingVertex, BvsA);
                    }
                    else if ( cont == Geometry.LIMIT &&
                              f.lastIntersectedVertex != null ) {
                        // Reduction step phase 2/3: Edge touches vertex
                        // No subdivide?
                        // Reduction step phase 4: store vertex/vertex
                        addsovv(e.rightHalf.startingVertex, f.lastIntersectedVertex, BvsA);

                    }
                    processEdge(e.rightHalf.previous().parentEdge, current, BvsA, other);
                }

            }
        }
        else {
            if ( s1 == 0 ) {
                doVertexOnFace(v1, f, BvsA, current, other);
            }
            if ( s2 == 0 ) {
                doVertexOnFace(v2, f, BvsA, current, other);
            }
        }

    }

    /**
    Following program [MANT1988].15.2.
    */
    private static void processEdge(_PolyhedralBoundedSolidEdge e,
                                    PolyhedralBoundedSolid s,
                                    int BvsA,
                                    PolyhedralBoundedSolid other)
    {
        _PolyhedralBoundedSolidFace f;
        int i;

        for ( i = 0; i < s.polygonsList.size(); i++ ) {
            f = s.polygonsList.get(i);
            doSetOpGenerate(e, f, BvsA, s, other);
        }
    }

    /**
    Following program [MANT1988].15.2.
    */
    private static void setOpGenerate(PolyhedralBoundedSolid inSolidA,
                                      PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidEdge e;

        sonvv = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex>();
        sonva = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();
        sonvb = new ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace>();

        int i;

        for ( i = 0; i < inSolidA.edgesList.size(); i++ ) {
            e = inSolidA.edgesList.get(i);
            processEdge(e, inSolidB, 0, inSolidA);
        }
        for ( i = 0; i < inSolidB.edgesList.size(); i++ ) {
            e = inSolidB.edgesList.get(i);
            processEdge(e, inSolidA, 1, inSolidB);
        }
    }

    /**
    Current method is the first step for the initial vertex/face classification
    of sectors (vertex neighborhood) for `vtx`, as indicated on section
    [MANT1988].14.5.2. and program [MANT1988].14.4., but biased towards the
    set operator classifier, as proposed on section [MANT1988].15.6.1. and
    problem [MANT1988].15.4.

    Vitral SDK's implementation of this procedure extends the original from
    [MANT1988] by adding extra information flags to sector classifications
    `.isWide`, `.position` and `.situation`. Those flags are an additional
    aid for debugging purposes and specifically the `situation` flag will be
    later used on `splitClassify` to correct the ordering of sectors in order
    to keep consistency with Vitral SDK's interpretation of coordinate system.
    */
    private static
    ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>
    getNeighborhood(
        _PolyhedralBoundedSolidVertex vtx,
        InfinitePlane referencePlane,
        int BvsA)
    {
        _PolyhedralBoundedSolidHalfEdge he;
        Vector3D bisect;
        double d;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace c;

        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> neighborSectorsInfo;
        neighborSectorsInfo = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>();
        he = vtx.emanatingHalfEdge;

        do {
            c = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();
            c.sector = he;
            d = referencePlane.pointDistance((he.next()).startingVertex.position);
            c.cl = PolyhedralBoundedSolid.compareValue(d, 0.0, VSDK.EPSILON);
            c.isWide = false;
            c.position = new Vector3D((he.next()).startingVertex.position);
            c.situation = c.UNDEFINED;
            c.referencePlane = referencePlane;
            neighborSectorsInfo.add(c);
            if ( checkWideness(he) ) {
                bisect = bisector(he);
                c.situation = c.CROSSING_EDGE;

                c = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();
                c.sector = he;
                d = referencePlane.pointDistance(bisect);
                c.cl = PolyhedralBoundedSolid.compareValue(d, 0.0, VSDK.EPSILON);
                c.isWide = true;
                c.position = new Vector3D(bisect);
                c.situation = c.CROSSING_EDGE;
                c.referencePlane = referencePlane;
                neighborSectorsInfo.add(c);
            }
            he = (he.mirrorHalfEdge()).next();
        } while ( he != vtx.emanatingHalfEdge );

        //-----------------------------------------------------------------
        // Extra pass, not from original [MANT1988] code
        int i;

        for ( i = 0; i < neighborSectorsInfo.size(); i++ ) {
            c = neighborSectorsInfo.get(i);
            if ( c.cl == c.ON && c.situation == c.UNDEFINED ) {
                c.situation = c.INPLANE_EDGE;
            }
        }

        return neighborSectorsInfo;
    }

    /**
    Current method applies the first reclassification rule presented at
    sections [MANT1988].14.5.1 and [MANT1988].14.5.2., but biased towards the
    set operator classifier, as proposed on section [MANT1988].15.6.1. and
    problem [MANT1988].15.4.:
    For the given vertex neigborhood, classify each edge according to whether
    its final vertex lies above (out), on or below (in) the `referencePlane`.
    Tag the edge with the corresponding label ABOVE, ON or BELOW.
    Following program [MANT1988].14.5.
    */
    private static void reclassifyOnSectors(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        InfinitePlane referencePlane)
    {
        _PolyhedralBoundedSolidFace f;
        Vector3D c;
        double d;
        int i;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace l;

        for ( i = 0; i < nbr.size(); i++ ) {
            l = nbr.get(i);
            f = l.sector.parentLoop.parentFace;
            c = f.containingPlane.getNormal().crossProduct(referencePlane.getNormal());
            d = c.dotProduct(c);
            if ( PolyhedralBoundedSolid.compareValue(d, 0.0, VSDK.EPSILON) == 0 ) {
                // Entering this means "faces are coplanar"
                d = f.containingPlane.getNormal().dotProduct(referencePlane.getNormal());
                if ( PolyhedralBoundedSolid.compareValue(d, 0.0, VSDK.EPSILON) == 1 ) {
                    //l.cl = l.BELOW;
                    l.situation = l.COPLANAR_FACE;
                    //nbr.get((i+1)%nbr.size()).cl = l.BELOW;
                }
                else {
                    //l.cl = l.ABOVE;
                    l.situation = l.COPLANAR_FACE;
                    //nbr.get((i+1)%nbr.size()).cl = l.ABOVE;
                }
            }
        }
    }

    private static void printNbr(ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> neighborSectorsInfo)
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
            if ( nbr.get(i).situation == nbr.get(i).INPLANE_EDGE ) return true;
        }
        return false;
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the reclassification rules.
    */
    private static void reclassifyOnEdges(
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
    Following section [MANT1988].14,6,2 and program [MANT1988].14.7., but
    biased for set operations, as indicated on section [MANT1988].15.6.1.
    */
    private static void insertNullEdges(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA)
    {
        int start, i;
        _PolyhedralBoundedSolidHalfEdge head, tail;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;
        PolyhedralBoundedSolid solida;
        int nnbr = nbr.size();

        solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;

        if ( nnbr <= 0 ) return;
        n = nbr.get(0);

        //- Locate the head of an ABOVE-sequence --------------------------
        i = 0;
        while ( !( 
                   (nbr.get(i).cl == n.AinB || nbr.get(i).cl == n.BinA) &&
                   ((nbr.get( (i+1)%nnbr ).cl == n.AoutB) ||
                     nbr.get( (i+1)%nnbr ).cl == n.BoutA))  ) {
            i++;
            if ( i >= nnbr ) {
                return;
            }
        }
        start = i;
        head = nbr.get(i).sector;

        //-----------------------------------------------------------------
        while ( true ) {
            //- Locate the final sector of the sequence ------------------
            while ( !( (nbr.get(i).cl == n.AoutB || nbr.get(i).cl == n.BoutA) &&
                       (nbr.get( (i+1)%nnbr ).cl == n.AinB ||
                        nbr.get( (i+1)%nnbr ).cl == n.BinA) ) ) {
                i = (i+1) % nnbr;
            }
            tail = nbr.get(i).sector;

            //- Insert null edge -----------------------------------------
            //System.out.println("LMEV:");
            //System.out.println("  - (" + start + ") H1: " + head);
            //System.out.println("  - (" + i + ") H2: " + tail);
            solida.lmev(head, tail, solida.getMaxVertexId()+1, head.startingVertex.position);

            ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;
            if ( BvsA == 0 ) {
                sone = sonea;
            }
            else {
                sone = soneb;
            }
            sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(head.previous().parentEdge));

            //- Locate the start of the next sequence --------------------
            while ( !( (nbr.get(i).cl == n.AinB || nbr.get(i).cl == n.BinA) &&
                       ((nbr.get( (i+1) % nnbr ).cl == n.AoutB ||
                         nbr.get( (i+1) % nnbr ).cl == n.BoutA)) ) ) {
                i = (i+1) % nnbr;
                if ( i == start ) {
                    return;
                }
            }
        }

        //-----------------------------------------------------------------
    }

    /**
    Answer to problem [MANT1988].15.4.
    */
    private static void vtxFacClassify(
        _PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidFace f,
        int op,
        int BvsA)
    {
        //- Following classification strategy from the splitter algorithm -
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr;

        //System.out.println("  - " + v + " / " + f);

        nbr = getNeighborhood(v, f.containingPlane, BvsA);
        if ( inplaneEdgesOn(nbr) ) {
            Collections.reverse(nbr);
        }
        reclassifyOnSectors(nbr, f.containingPlane);

        //- Adjusting results for set operation interpretation ------------
        int i;
        for ( i = 0; i < nbr.size(); i++ ) {
            nbr.get(i).updateLabel(BvsA);
        }
        reclassifyOnEdges(nbr, op);
        printNbr(nbr);

        insertNullEdges(nbr, f, v, BvsA);

        //- Pierce face ---------------------------------------------------
        PolyhedralBoundedSolid solida, solidb;
        _PolyhedralBoundedSolidHalfEdge he;

        solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;
        solidb = f.parentSolid;

        he = f.boundariesList.get(0).boundaryStartHalfEdge;

        int vn = nextVertexId(solida, solidb);
        solidb.lmev(he, he, vn, v.position);
        he = solidb.findVertex(vn).emanatingHalfEdge;
        solidb.lkemr(he.mirrorHalfEdge(), he);
        vn = nextVertexId(solida, solidb);
        solidb.lmev(he, he, vn, v.position);
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;
        if ( BvsA == 1 ) {
            sone = sonea;
        }
        else {
            sone = soneb;
        }
        sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(he.parentEdge));
    }

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face.
    */
    protected static Vector3D inside(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3D middle;
        Vector3D a, b, c;

        a = (he.next()).startingVertex.position.substract(he.startingVertex.position);
        b = (he.previous()).startingVertex.position.substract(he.startingVertex.position);
        a.normalize();
        b.normalize();
        c = (a.add(b)).multiply(0.5);

        middle = he.startingVertex.position.add(c);

        if ( he.parentLoop.parentFace.testPointInside(middle, VSDK.EPSILON) ==
                                                      Geometry.OUTSIDE ) {
            c = c.multiply(-1);
            middle = he.startingVertex.position.add(c);
        }

        return middle;
    }


    /**
    Following program [MANT1988].15.8.
    */
    private static
    ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>
    nbrpreproc(_PolyhedralBoundedSolidVertex v)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex n, nold;
        Vector3D bisec;
        _PolyhedralBoundedSolidHalfEdge he;
        int i;
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> nb;

        nb = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>();

        he = v.emanatingHalfEdge;

        do {
            n = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
            n.he = he;
            n.wide = false;

            n.ref1 = he.previous().startingVertex.position.substract(
                he.startingVertex.position);    
            n.ref2 = he.next().startingVertex.position.substract(
                he.startingVertex.position);
            n.ref12 = n.ref1.crossProduct(n.ref2);

            if ( (n.ref12.length() < VSDK.EPSILON) ||
                 (n.ref12.dotProduct(he.parentLoop.parentFace.containingPlane.getNormal()) > 0.0 ) ) {
                // Inside this conditional means: current vertex is a wide one
                if ( (n.ref12.length() < VSDK.EPSILON) ) {
                    bisec = inside(he);
                }
                else {
                    bisec = n.ref1.add(n.ref2);
                    bisec = bisec.multiply(-1);
                }
                n.ref2 = bisec;
                n.ref12 = n.ref1.crossProduct(n.ref2);
                nold = n;
                nb.add(n);

                n = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();
                n.he = he;
                n.ref2 = nold.ref2;
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
    Checks if two coplanar sectors overlaps.
    Following program [MANT1988].15.9. and section [MANT1988].15.6.2.
    */
    private static boolean sectoroverlap(_PolyhedralBoundedSolidHalfEdge h1,
                                         _PolyhedralBoundedSolidHalfEdge h2)
    {
        InfinitePlane a, b, c;
        Vector3D n;
        double d;

        a = h1.parentLoop.parentFace.containingPlane;
        b = h2.parentLoop.parentFace.containingPlane;
        n = b.getNormal();
        d = b.getD();
        n = n.multiply(-1);
        c = new InfinitePlane(n.x, n.y, n.z, d);

        if ( a.overlapsWith(b, VSDK.EPSILON) ||
             a.overlapsWith(c, VSDK.EPSILON) ) {
            return true;
        }
        return false;
    }

    /**
    Following program [MANT1988].15.9. According to the sector intersection
    test from section [MANT1988].15.6.2, the variables (with respect to
    the central vertex on a given sector) are:
      - dir is the vector from the starting vertex of the sector, pointing
        on the direction of the intersection line with another sector, or
        `int` in figure [MANT1988].15.8. and equation [MANT1988].15.5.
      - ref1 and ref2 are the same as in figure [MANT1988].15.8. and
        equation [MANT1988].15.5.
      - ref12 is the cross product of ref1 and ref2, or `ref` in figure
        [MANT1988].15.8. and equation [MANT1988].15.5.
      - c1 is the cross product of ref1 and dir, or `test1` in figure
        [MANT1988].15.8. and equation [MANT1988].15.5.
      - c2 is the cross product of dir and ref2, or `test2` in figure
        [MANT1988].15.8. and equation [MANT1988].15.5.
    */
    private static boolean sctrwitthin(Vector3D dir, Vector3D ref1,
                            Vector3D ref2, Vector3D ref12)
    {
        Vector3D c1, c2;
        int t1, t2;

        c1 = dir.crossProduct(ref1);
        if ( c1.length() < VSDK.EPSILON ) {
            return (ref1.dotProduct(dir) > 0.0);
        }
        c2 = ref2.crossProduct(dir);
        if ( c2.length() < VSDK.EPSILON ) {
            return (ref2.dotProduct(dir) > 0.0);
        }
        t1 = PolyhedralBoundedSolid.compareValue(c1.dotProduct(ref12), 0.0, VSDK.EPSILON);
        t2 = PolyhedralBoundedSolid.compareValue(c2.dotProduct(ref12), 0.0, VSDK.EPSILON);
        return ( t1 < 0.0 && t2 < 0.0 );
    }

    /**
    Sector intersection test.

    Following program [MANT1988].15.9. and section [MANT1988].15.6.2.
    */
    private static boolean sectortest(int i, int j)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge h1, h2;
        boolean c1, c2;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na, nb;

        na = nba.get(i);
        nb = nbb.get(j);
        h1 = na.he;
        h2 = nb.he;

        //-----------------------------------------------------------------
        // Here, n1 and n2 are the plane normals for containing faces of
        // sectors i and j, as in figure [MANT1988].15.7.
        Vector3D n1, n2;
        Vector3D intrs;

        n1 = h1.parentLoop.parentFace.containingPlane.getNormal();
        n2 = h2.parentLoop.parentFace.containingPlane.getNormal();
        intrs = n1.crossProduct(n2);

        //-----------------------------------------------------------------
        if ( intrs.length() < VSDK.EPSILON ) {
            return sectoroverlap(h1, h2);
        }

        //-----------------------------------------------------------------
        c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
        c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
        if ( c1 && c2 ) {
            return true;
        }
        else {
            intrs = intrs.multiply(-1);
            c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
            c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
            if ( c1 && c2 ) {
                return true;
            }
        }

        return false;
    }

    /**
    Following program [MANT1988].15.7.
    */
    private static void setopgetneighborhood(
        _PolyhedralBoundedSolidVertex va,
        _PolyhedralBoundedSolidVertex vb)
    {
        _PolyhedralBoundedSolidHalfEdge ha, hb;
        double d1, d2, d3, d4;
        int na, nb, i, j;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector s;
        Vector3D n1, n2;

        nba = nbrpreproc(va);
        nbb = nbrpreproc(vb);
        sectors = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector>();
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex xa, xb;

        for ( i = 0; i < nba.size(); i++ ) {
            for ( j = 0; j < nbb.size(); j++ ) {
                if ( sectortest(i, j) ) {
                    s = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector();
                    s.secta = i;
                    s.sectb = j;
                    xa = nba.get(i);
                    xb = nbb.get(j);
                    s.hea = xa.he;
                    s.heb = xb.he;
                    s.wa = xa.wide;
                    s.wb = xb.wide;

                    n1 = xa.he.parentLoop.parentFace.containingPlane.getNormal();
                    n2 = xb.he.parentLoop.parentFace.containingPlane.getNormal();
                    d1 = n2.dotProduct(xa.ref1);
                    d2 = n2.dotProduct(xa.ref2);
                    d3 = n1.dotProduct(xb.ref1);
                    d4 = n1.dotProduct(xb.ref2);
                    s.s1a = PolyhedralBoundedSolid.compareValue(d1, 0.0, VSDK.EPSILON);
                    s.s2a = PolyhedralBoundedSolid.compareValue(d2, 0.0, VSDK.EPSILON);
                    s.s1b = PolyhedralBoundedSolid.compareValue(d3, 0.0, VSDK.EPSILON);
                    s.s2b = PolyhedralBoundedSolid.compareValue(d4, 0.0, VSDK.EPSILON);
                    s.intersect = true;
                    sectors.add(s);
                }
            }
        }

    }

    /**
    Following section [MANT1988].15.6.2. and program [MANT1988].15.10.
    */
    private static void sreclsectors(int op)
    {
        _PolyhedralBoundedSolidHalfEdge ha, hb;
        int i, j, newsa, newsb;
        boolean nonopposite;
        int secta, prevsecta, nextsecta;
        int sectb, prevsectb, nextsectb;
        double d;
        Vector3D n1, n2;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector si, sj;

        for ( i = 0; i < sectors.size(); i++ ) {
            if ( sectors.get(i).s1a == sectors.get(i).ON &&
                 sectors.get(i).s2a == sectors.get(i).ON &&
                 sectors.get(i).s1b == sectors.get(i).ON &&
                 sectors.get(i).s2b == sectors.get(i).ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0)?nba.size()-1:secta-1;
                prevsectb = (sectb == 0)?nbb.size()-1:sectb-1;
                nextsecta = (secta == nba.size()-1)?0:secta+1;
                nextsectb = (sectb == nbb.size()-1)?0:sectb+1;
                ha = nba.get(secta).he;
                hb = nbb.get(sectb).he;
                n1 = ha.parentLoop.parentFace.containingPlane.getNormal();
                n2 = hb.parentLoop.parentFace.containingPlane.getNormal();
                d = VSDK.vectorDistance(n1, n2);
                nonopposite = ( d < VSDK.EPSILON );
                if ( nonopposite ) {
                    newsa = (op == UNION)?sectors.get(i).OUT:sectors.get(i).IN;
                    newsb = (op == UNION)?sectors.get(i).IN:sectors.get(i).OUT;
                }
                else {
                    newsa = (op == UNION)?sectors.get(i).IN:sectors.get(i).OUT;
                    newsb = (op == UNION)?sectors.get(i).IN:sectors.get(i).OUT;
                }
                si = sectors.get(i);
                for ( j = 0; j < sectors.size(); j++ ) {
                    sj = sectors.get(j);
                    if ( (sj.secta == prevsecta) && (sj.sectb == sectb) ) {
                        if ( sj.s1a != si.ON ) {
                            sj.s2a = newsa;
                        }
                    }
                    if ( (sj.secta == nextsecta) && (sj.sectb == sectb) ) {
                        if ( sj.s2a != si.ON ) {
                            sj.s1a = newsa;
                        }
                    }
                    if ( (sj.secta == secta) && (sj.sectb == prevsectb) ) {
                        if ( sj.s1b != si.ON ) {
                            sj.s2b = newsb;
                        }
                    }
                    if ( (sj.secta == secta) && (sj.sectb == nextsectb) ) {
                        if ( sj.s2b != si.ON ) {
                            sj.s1b = newsb;
                        }
                    }
                    if ( (sj.s1a == sj.s2a) && 
                         (sj.s1a == si.IN || sj.s1a == si.OUT) ) {
                        sj.intersect = false;
                    }
                    if ( (sj.s1b == sj.s2b) && 
                         (sj.s1b == si.IN || sj.s1b == si.OUT) ) {
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
    Reclassification procedure for "on"-edges on the vertex/vertex clasiffier.
    Following section [MANT1986].15.6.2.
    */
    private static void srecledges(int op)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector si;
        int i;

        for ( i = 0; i < sectors.size(); i++ ) {
            si = sectors.get(i);
            if ( si.s1a == si.ON || si.s2a == si.ON ||
                 si.s1b == si.ON || si.s2b == si.ON ) {
/*
                if ( si.s1a == si.ON ) si.s1a = -2;
                if ( si.s2a == si.ON ) si.s2a = -2;
                if ( si.s1b == si.ON ) si.s1b = -2;
                if ( si.s2b == si.ON ) si.s2b = -2;
*/
            }
        }
    }

    /**
    Following program [MANT1988].15.6. Similar in structure to program
    [MANT1988].14.3.
    */
    private static void vtxVtxClassify(
        _PolyhedralBoundedSolidVertex va,
        _PolyhedralBoundedSolidVertex vb,
        int op)
    {
        System.out.println("VERTEX/VERTEX PAIR (A/B): " + va.id + " / " + vb.id);

        setopgetneighborhood(va, vb);

        System.out.println("  - Initial sector candidates:");
        for ( int i = 0; i < sectors.size(); i++ ) {
            System.out.println("   . " + sectors.get(i));
        }

        sreclsectors(op);

        System.out.println("  - On sector reclassified:");
        for ( int i = 0; i < sectors.size(); i++ ) {
            System.out.println("   . " + sectors.get(i));
        }

        srecledges(op);

        System.out.println("  - On edges reclassified:");
        for ( int i = 0; i < sectors.size(); i++ ) {
            System.out.println("   . " + sectors.get(i));
        }

        //sinsertnulledges();
    }

    /**
    Following program [MANT1988].15.5.
    */
    private static void setOpClassify(int op)
    {
        int i;

        System.out.println("---------------------------------------------------------------------------");
        System.out.println("Vertices on A touching faces on B:");
        for ( i = 0; i < sonva.size(); i++ ) {
            vtxFacClassify(sonva.get(i).v, sonva.get(i).f, op, 0);
        }

        System.out.println("---------------------------------------------------------------------------");
        System.out.println("Vertices on B touching faces on A:");
        for ( i = 0; i < sonvb.size(); i++ ) {
            vtxFacClassify(sonvb.get(i).v, sonvb.get(i).f, op, 1);
        }

        System.out.println("---------------------------------------------------------------------------");
        System.out.println("Vertex-Vertex pairs:");
        for ( i = 0; i < sonvv.size(); i++ ) {
            vtxVtxClassify(sonvv.get(i).va, sonvv.get(i).vb, op);
        }
    }

    private static void ssortnulledges()
    {
        Collections.sort(sonea);
        Collections.sort(soneb);
    }


    /**
    Following section [MANT1988].14.7.1 and program [MANT1988].14.8.
    */
    private static boolean neighbor(_PolyhedralBoundedSolidHalfEdge h1, _PolyhedralBoundedSolidHalfEdge h2)
    {
        boolean condition1;
        boolean condition2;
        boolean condition3;

        if ( h1 == null || h2 == null ||
             h1.parentEdge == null || h2.parentEdge == null ) {
            return false;
        }

        condition1 = ( h1.parentLoop.parentFace == h2.parentLoop.parentFace );

        condition2 = ( (
              h1 == h1.parentEdge.rightHalf && h2 == h2.parentEdge.leftHalf
              ) || 
              (
              h1 == h1.parentEdge.leftHalf && h2 == h2.parentEdge.rightHalf
               ) );

        condition3 = (h1.parentEdge != h2.parentEdge);

        return condition1 && condition2 && condition3;
    }

    /**
    Following section [MANT1988].15.7. and program [MANT1988].15.13.
    */
    private static _PolyhedralBoundedSolidHalfEdge[]
    scanjoin(_PolyhedralBoundedSolidHalfEdge hea,
             _PolyhedralBoundedSolidHalfEdge heb)
    {
        int i, j;
        _PolyhedralBoundedSolidHalfEdge ret[];

        ret = new _PolyhedralBoundedSolidHalfEdge[2];

        for ( i = 0; i < endsa.size(); i++ ) {
            if ( neighbor(hea, endsa.get(i)) &&
                 neighbor(heb, endsb.get(i)) ) {
                ret[0] = endsa.get(i);
                ret[1] = endsb.get(i);
                endsa.remove(i);
                endsb.remove(i);
                tiedsa.add(ret[0]);
                tiedsb.add(ret[1]);
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

        for ( i = 0; i < tiedsa.size(); i++ ) {
            if ( he == tiedsa.get(i) ) return false;
        }

        return true;
    }

    private static boolean isLooseB(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        for ( i = 0; i < tiedsb.size(); i++ ) {
            if ( he == tiedsb.get(i) ) return false;
        }

        return true;
    }

    private static void cuta(_PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;

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

    private static void cutb(_PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;

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

    /**
    Following section [MANT1988].15.7. and program [MANT1988].15.14.
    */
    private static void setOpConnect()
    {
        System.out.println("---------------------------------------------------------------------------");
        System.out.println("setOpConnect");

        _PolyhedralBoundedSolidEdge nextedgea, nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null, h2a = null, h1b = null, h2b = null;
        _PolyhedralBoundedSolidHalfEdge r[];

        endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        tiedsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        tiedsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        ssortnulledges();

        int i;

        for ( i = 0; i < sonea.size(); i++ ) {
            // This assumes that for each null edge on solid a there is
            // another on solid b.
            nextedgea = sonea.get(i).e;
            nextedgeb = soneb.get(i).e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;

            r = scanjoin(nextedgea.rightHalf, nextedgeb.leftHalf);
            if ( r != null ) {
                h1a = r[0];
                h2b = r[1];
                join(h1a, nextedgea.rightHalf);
                if ( !isLooseA(h1a.mirrorHalfEdge()) ) {
                    cuta(h1a);
                }
                join(h2b, nextedgeb.leftHalf);
                if ( !isLooseB(h2b.mirrorHalfEdge()) ) {
                    cutb(h2b);
                }
            }

            r = scanjoin(nextedgea.leftHalf, nextedgeb.rightHalf);
            if ( r != null ) {
                h2a = r[0];
                h1b = r[1];
                join(h2a, nextedgea.leftHalf);
                if ( !isLooseA(h2a.mirrorHalfEdge()) ) {
                    cuta(h2a);
                }
                join(h1b, nextedgeb.rightHalf);
                if ( !isLooseB(h1b.mirrorHalfEdge()) ) {
                    cutb(h1b);
                }
            }

            if ( h1a != null && h1b != null && h2a != null && h2b != null ) {
                cuta(nextedgea.rightHalf);
                cutb(nextedgeb.rightHalf);
            }
        }

    }

    /**
    Following program [MANT1988].15.15.
    */
    private static void setOpFinish(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        PolyhedralBoundedSolid outRes,
        int op
    )
    {
        int i, j, inda, indb;
        _PolyhedralBoundedSolidFace f;

        System.out.println("---------------------------------------------------------------------------");
        System.out.println("setOpFinish");

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

        if ( op == DIFFERENCE ) {
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

        outRes.validateModel();

    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        //-----------------------------------------------------------------
        PolyhedralBoundedSolid res = new PolyhedralBoundedSolid();

        sonea = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        soneb = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();

        //-----------------------------------------------------------------
        inSolidA.validateModel();
        inSolidB.validateModel();
        inSolidA.maximizeFaces();
        inSolidB.maximizeFaces();
        inSolidA.validateModel();
        inSolidB.validateModel();

        updmaxnames(inSolidB, inSolidA);

        setOpGenerate(inSolidA, inSolidB);

        setOpClassify(op);
        if ( sonea.size() == 0 ) {
            // No intersections found
            System.out.println("Empty sonea.");
            return inSolidA;
        }

        setOpConnect();
        setOpFinish(inSolidA, inSolidB, res, op);

        return res;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
