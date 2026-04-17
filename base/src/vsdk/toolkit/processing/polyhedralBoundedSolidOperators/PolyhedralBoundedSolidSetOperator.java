//= References:                                                             =
//= [MANT1986] Mantyla Martti. "Boolean Operations of 2-Manifolds through   =
//=     Vertex Neighborhood Classification". ACM Transactions on Graphics,  =
//=     Vol. 5, No. 1, January 1986, pp. 1-29.                              =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidOperator;

// Java classes
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.render.PolyhedralBoundedSolidDebugger;
import vsdk.toolkit.io.PersistenceElement;

/**
This class encapsulates the set operations algorithms for boundary
representation solids in VitralSDK. Basically, this class implements the
original algorithm published in the paper [MANT1986] and in the second
part of the book [MANT1988].
The algorithm is structured in 5 big phases:
  0. Calculate vertex/face and vertex/vertex crossings.
  1. Classify and split for vertex/face cases.
  2. Classify and split for vertex/vertex cases.
  3. Connect.
  4. Finish.
Note that each big phase is controlled in a method (mark as "big phase" in
its documentation).
*/
public class PolyhedralBoundedSolidSetOperator extends PolyhedralBoundedSolidOperator
{
    /**
    Debug flags.
    */
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_02_GENERATOR = 0x02;
    private static final int DEBUG_03_VERTEXFACECLASIFFIER = 0x04;
    private static final int DEBUG_04_VERTEXVERTEXCLASIFFIER = 0x08;
    private static final int DEBUG_05_CONNECT = 0x10;
    private static final int DEBUG_06_FINISH = 0x20;
    private static final int DEBUG_99_SHOWOPERATIONS = 0x40;

    /**
    The integer `debugFlags` is a bitwise combination of debugging flags
    used to control debug messages printed on the standard output.
    */
    private static int debugFlags = 0;

    /**
    Used for exporting internal state in graphical form.
    */
    private static PolyhedralBoundedSolidDebugger offlineRenderer = null;

    private static int compareToZero(double value)
    {
        return PolyhedralBoundedSolidNumericPolicy.compareToZero(value,
            numericContext);
    }

    private static int pointInFace(_PolyhedralBoundedSolidFace face, Vector3D point)
    {
        return face.testPointInside(point, numericContext.bigEpsilon());
    }

    private static void registerCoplanarRelation(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int index, int relation)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;

        n = nbr.get(index);
        if ( relation > n.coplanarRelation ) {
            n.coplanarRelation = relation;
        }
    }

    private static boolean hasCoplanarOverlap(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr)
    {
        int i;

        for ( i = 0; i < nbr.size(); i++ ) {
            if ( nbr.get(i).coplanarRelation ==
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                    .COPLANAR_OVERLAP ) {
                return true;
            }
        }
        return false;
    }

    private static int classifyCoplanarSectorRelation(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace sectorInfo,
        _PolyhedralBoundedSolidFace referenceFace)
    {
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidHalfEdge heNext;
        Vector3D start;
        Vector3D direction;
        Vector3D probe;
        double edgeLength;
        double step;
        int status;

        if ( sectorInfo == null || referenceFace == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        he = sectorInfo.sector;
        if ( he == null || he.startingVertex == null ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_DISJOINT;
        }

        heNext = he.next();
        start = he.startingVertex.position;
        direction = inside(he);

        if ( (direction == null ||
              direction.length() <= numericContext.epsilon()) &&
             sectorInfo.position != null ) {
            direction = sectorInfo.position.substract(start);
        }
        if ( direction == null ||
             direction.length() <= numericContext.epsilon() ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_TOUCHING;
        }

        edgeLength = 0.0;
        if ( heNext != null && heNext.startingVertex != null ) {
            edgeLength = VSDK.vectorDistance(start, heNext.startingVertex.position);
        }

        step = Math.max(numericContext.bigEpsilon() * 4.0,
                        edgeLength * 1.0e-3);
        if ( edgeLength > numericContext.bigEpsilon() ) {
            step = Math.min(step, edgeLength * 0.25);
        }

        direction.normalize();
        probe = start.add(direction.multiply(step));
        status = pointInFace(referenceFace, probe);

        if ( status == Geometry.INSIDE ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_OVERLAP;
        }
        if ( status == Geometry.LIMIT ) {
            return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                .COPLANAR_TOUCHING;
        }
        return _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
            .COPLANAR_DISJOINT;
    }

    private static int resolveCoplanarSectorClass(int op, int BvsA,
                                                  boolean sameOrientation)
    {
        if ( BvsA != 0 ) {
            return (op == UNION) ?
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN :
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
        }

        if ( sameOrientation ) {
            return (op == UNION) ?
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT :
                _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
        }
        return (op == UNION) ?
            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN :
            _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
    }

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

    /**
    Following variable `endsb` from program [MANT1988].15.13.
    */
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsb;

    /**
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. This method increments the face and vertex
    identifiers of `solidToUpdate` so that they do not overlap with
    `referenceSolid` identifiers.
    */
    public static void updmaxnames(PolyhedralBoundedSolid solidToUpdate,
                                   PolyhedralBoundedSolid referenceSolid)
    {
        _PolyhedralBoundedSolidVertex v;
        _PolyhedralBoundedSolidFace f;
        int i;

        for ( i = 0; i < solidToUpdate.verticesList.size(); i++ ) {
            v = solidToUpdate.verticesList.get(i);
            v.id += referenceSolid.getMaxVertexId();
            if ( v.id > solidToUpdate.maxVertexId ) {
                solidToUpdate.maxVertexId = v.id;
            }
        }

        for ( i = 0; i < solidToUpdate.polygonsList.size(); i++ ) {
            f = solidToUpdate.polygonsList.get(i);
            f.id += referenceSolid.getMaxFaceId();
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
        if ( compareToZero(d) == 0 ) {
            cont = pointInFace(f, v.position);
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
    Following program [MANT1988].15.3.
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
        s1 = compareToZero(d1);
        s2 = compareToZero(d2);

        if ( (s1 == -1 && s2 == 1) || (s1 == 1 && s2 == -1) ) {
            t = d1 / (d1 - d2);
            p = v1.position.add(
                    (v2.position.substract(
                         v1.position)).multiply(t));

            d3 = f.containingPlane.pointDistance(p);
            if ( compareToZero(d3) == 0 ) {
                cont = pointInFace(f, p);

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
    Initial vertex intersection detector for the set operations algorithm
    (big phase 0).
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
        neighborSectorsInfo = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>();

        he = vtx.emanatingHalfEdge;
        do {
            c = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();
            c.sector = he;
            d = referencePlane.pointDistance((he.next()).startingVertex.position);
            c.cl = compareToZero(d);
            c.isWide = false;
            c.position = new Vector3D((he.next()).startingVertex.position);
            c.situation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.UNDEFINED;
            c.referencePlane = referencePlane;
            neighborSectorsInfo.add(c);
            if ( checkWideness(he) ) {
                bisect = inside(he).add(vtx.position);
                c.situation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.CROSSING_EDGE;

                c = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();
                c.sector = he;
                d = referencePlane.pointDistance(bisect);
                c.cl = compareToZero(d);
                c.isWide = true;
                c.position = new Vector3D(bisect);
                c.situation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.CROSSING_EDGE;
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
            if ( c.cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.ON && c.situation == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.UNDEFINED ) {
                c.situation = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.INPLANE_EDGE;
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
    private static void vertexFaceReclassifyOnSectorsNoPeekVersion(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane, int BvsA, int op)
    {
        _PolyhedralBoundedSolidFace localFace;
        Vector3D c;
        double d;
        int i;
        int nnbr = nbr.size();
        int relation;
        int resolvedClass;
        boolean sameOrientation;

        //-----------------------------------------------------------------
        // Backup neighborhood to prevent empty case
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> backup;

        backup = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>();
        for ( i = 0; i < nnbr; i++ ) {
            backup.add(new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace(nbr.get(i)));
        }

        //-----------------------------------------------------------------
        // Only will be activated if non empty case result (force to intersect)
        for ( i = 0; i < nnbr; i++ ) {
            // Test coplanarity
            localFace = nbr.get(i).sector.parentLoop.parentFace;
            c = localFace.containingPlane.getNormal().crossProduct(
                referencePlane.getNormal());
            d = c.dotProduct(c);
            if ( compareToZero(d) == 0 ) {
                relation = classifyCoplanarSectorRelation(nbr.get(i),
                    referenceFace);
                registerCoplanarRelation(nbr, i, relation);
                registerCoplanarRelation(nbr, (i+1)%nnbr, relation);

                if ( relation ==
                     _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                         .COPLANAR_OVERLAP ) {
                    d = localFace.containingPlane.getNormal().dotProduct(
                        referencePlane.getNormal());
                    sameOrientation = (compareToZero(d) == 1);
                    resolvedClass = resolveCoplanarSectorClass(op, BvsA,
                        sameOrientation);
                    nbr.get(i).cl = resolvedClass;
                    nbr.get((i+1)%nnbr).cl = resolvedClass;
                }
            }
        }

        //-----------------------------------------------------------------
        // Restore original neighborhood if result is empty case
        int ins = 0;
        int outs = 0;

        for ( i = 0; i < nnbr; i++ ) {
            if ( nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) {
                outs++;
            }
            else {
                ins++;
            }
        }
        if ( (outs == nnbr || ins == nnbr) &&
             !hasCoplanarOverlap(nbr) ) {
            //System.out.println("**** WRONG ON SECTOR RECLASSIFICATION! REVERSED!");
            for ( i = 0; i < nnbr; i++ ) {
                nbr.get(i).cl = backup.get(i).cl;
                //nbr.get(i).reverse = true;
            }
        }
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
    -----------------------------------------------------------------
    Reclassification procedure for "on"-sectors on the vertex/face clasiffier,
    Original answer from [.WMANT2008].
    */
    private static void vertexFaceReclassifyOnSectors(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane, int BvsA, int op)
    {
        _PolyhedralBoundedSolidFace localFace;
        Vector3D c;
        double d;
        int i;
        int nnbr = nbr.size();
        int relation;
        int resolvedClass;
        boolean sameOrientation;

        //-----------------------------------------------------------------
        // Backup neighborhood to prevent empty case
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> backup;

        backup = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace>();
        for ( i = 0; i < nnbr; i++ ) {
            backup.add(new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace(nbr.get(i)));
        }

        //-----------------------------------------------------------------
        // Only will be activated if non empty case result (force to intersect)
        for ( i = 0; i < nnbr; i++ ) {
            // Test coplanarity
            localFace = nbr.get(i).sector.mirrorHalfEdge().parentLoop.parentFace;
            c = localFace.containingPlane.getNormal().crossProduct(
                referencePlane.getNormal());
            d = c.dotProduct(c);
            if ( compareToZero(d) == 0 ) {
                relation = classifyCoplanarSectorRelation(nbr.get(i),
                    referenceFace);
                registerCoplanarRelation(nbr, i, relation);
                registerCoplanarRelation(nbr, (i+1)%nnbr, relation);

                if ( relation ==
                     _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
                         .COPLANAR_OVERLAP ) {
                    d = localFace.containingPlane.getNormal().dotProduct(
                        referencePlane.getNormal());
                    sameOrientation = (compareToZero(d) == 1);
                    resolvedClass = resolveCoplanarSectorClass(op, BvsA,
                        sameOrientation);
                    nbr.get(i).cl = resolvedClass;
                    nbr.get((i+1)%nnbr).cl = resolvedClass;
                }
            }
        }

        //-----------------------------------------------------------------
        // Restore original neighborhood if result is empty case
        int ins = 0;
        int outs = 0;

        for ( i = 0; i < nnbr; i++ ) {
            if ( nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) {
                outs++;
            }
            else {
                ins++;
            }
        }
        if ( (outs == nnbr || ins == nnbr) &&
             !hasCoplanarOverlap(nbr) ) {
            System.out.println("**** WRONG ON SECTOR RECLASSIFICATION! REVERSED!");
            for ( i = 0; i < nnbr; i++ ) {
                nbr.get(i).cl = backup.get(i).cl;
                //nbr.get(i).reverse = true;
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
            if ( nbr.get(i).situation == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.INPLANE_EDGE ) return true;
        }
        return false;
    }

    private static void vertexFaceReclassifyOnEdges(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int op, boolean useBorrowed)
    {
        if ( useBorrowed ) {
            vertexFaceReclassifyOnEdgesBorrowed(nbr, op);
          }
          else {
            vertexFaceReclassifyOnEdgesNoPeekVersion(nbr, op);
        }
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the reclassification rules.
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
    Current method implements the set of changes from table [MANT1988].15.3.
    for the reclassification rules.
    -----------------------------------------------------------------
    Original answer from [.WMANT2008].
    */
    private static void vertexFaceReclassifyOnEdgesBorrowed(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        int op)
    {
        int i;
        int nnbr = nbr.size();
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector ni;

        ni = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector();

        for ( i = 0; i < nnbr; i++ ) {
            if ( nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                if ( nbr.get((nnbr+i-1)%nnbr).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                    if ( nbr.get((i+1)%nnbr).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                        nbr.get(i).cl = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    }
                    else {
                        nbr.get(i).cl = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    }
                }
                else {
                    // OUT 
                    if ( nbr.get((i+1)%nnbr).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                        nbr.get(i).cl = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    }
                    else {
                        nbr.get(i).cl = _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
                    }
                }
            }
        }
    }

    private static void vertexFaceInsertNullEdges(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA, boolean useBorrowed, PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        if ( useBorrowed ) {
            vertexFaceInsertNullEdgesBorrowed(nbr, f, v, BvsA, inSolidA, inSolidB);
        }
        else {
            vertexFaceInsertNullEdgesNoPeekVersion(nbr, f, v, BvsA, inSolidA, inSolidB);
        }
    }

    /**
    This method implements the third stage of the vertex/face classifier:
    given the previously reclassified list of vertex neigbors, insert
    a new vertex (using operator lmev) in the direction of the last
    "in" before an "out" sector of the sequence.

    This implementation follows section [MANT1988].14,6,2 and program
    [MANT1988].14.7., but it is biased for set operations, as indicated on
    section [MANT1988].15.6.1.

    Taking in to account the updated version modifications from
    [.wMANT2008].
    */
    private static void vertexFaceInsertNullEdgesNoPeekVersion(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA, PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        int start, i;
        _PolyhedralBoundedSolidHalfEdge head, tail;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;
        PolyhedralBoundedSolid solida;
        int nnbr = nbr.size();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;

        solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;

        if ( nnbr <= 0 ) return;
        n = nbr.get(0);

        //- Locate the head of an ABOVE-sequence --------------------------
        i = 0;
        while ( !( (nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AinB || nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BinA) &&
                   ((nbr.get( (i+1)%nnbr ).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AoutB) ||
                     nbr.get( (i+1)%nnbr ).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BoutA))  ) {
            i++;
            if ( i >= nnbr ) {
                //System.out.println("**** EMPTY CASE!");
                return;
            }
        }
        start = i;
        head = nbr.get(i).sector;

        //-----------------------------------------------------------------
        while ( true ) {
            //- Locate the final sector of the sequence ------------------
            while ( !( (nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AoutB || nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BoutA) &&
                       (nbr.get( (i+1)%nnbr ).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AinB ||
                        nbr.get( (i+1)%nnbr ).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BinA) ) ) {
                i = (i+1) % nnbr;
            }
            tail = nbr.get(i).sector;

            //- Insert null edge -----------------------------------------
            if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 &&
                 (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
                System.out.println("       -> LMEV (Vertex/face split):");
                System.out.println("          . (" + start + ") H1: " + head);
                System.out.println("          . (" + i + ") H2: " + tail);
            }

            solida.lmev(head, tail, nextVertexId(inSolidA, inSolidB), head.startingVertex.position);

            if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 &&
                 (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
                //head.startingVertex.debugColor = new ColorRgb(0, 1, 0);
                System.out.println("          . New vertex: " + head.startingVertex.id);
            }

            if ( BvsA != 0 ) {
                sone = soneb;
              }
              else {
                sone = sonea;
            }
            sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(head.previous().parentEdge));

            //- Pierce face ---------------------------------------------------
            makering(f, v, BvsA, inSolidA, inSolidB);

            //- Locate the start of the next sequence --------------------
            while ( !( (nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AinB || nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BinA) &&
                       ((nbr.get( (i+1) % nnbr ).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.AoutB ||
                         nbr.get( (i+1) % nnbr ).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace.BoutA)) ) ) {
                i = (i+1) % nnbr;
                if ( i == start ) {
                    return;
                }
            }
        }

        //-----------------------------------------------------------------
    }

    private static void vertexFaceInsertNullEdgesBorrowed(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace f,
        _PolyhedralBoundedSolidVertex v,
        int BvsA, PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        int start, i;
        _PolyhedralBoundedSolidHalfEdge head, tail;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace n;
        PolyhedralBoundedSolid solida;
        int nnbr = nbr.size();
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector ni;
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sone = null;

        ni = new _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector();

        solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;

        if ( nnbr <= 0 ) return;
        n = nbr.get(0);

        //- Locate the head of an ABOVE-sequence --------------------------
        i = 0;
        while ( !( nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN &&
                   nbr.get((i+1)%nnbr).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) ) {
            i++;
            if ( i >= nnbr ) {
                //System.out.println("**** EMPTY CASE!");
                return;
            }
        }
        start = i;
        head = nbr.get(i).sector;

        //-----------------------------------------------------------------
        while ( true ) {
            //- Locate the final sector of the sequence ------------------
            while ( !( nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT &&
                       nbr.get((i+1)%nnbr).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) ) {
                i = (i+1) % nnbr;
            }
            tail = nbr.get(i).sector;

            //- Insert null edge -----------------------------------------
            if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 &&
                 (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
                System.out.println("       -> LMEV (Vertex/face split):");
                System.out.println("          . (" + start + ") H1: " + head);
                System.out.println("          . (" + i + ") H2: " + tail);
            }
            solida.lmev(head, tail, nextVertexId(inSolidA, inSolidB), head.startingVertex.position);

            if ( BvsA != 0 ) {
                sone = soneb;
              }
              else {
                sone = sonea;
            }
            sone.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(head.previous().parentEdge));

            //- Pierce face ---------------------------------------------------
            makering(f, v, BvsA, inSolidA, inSolidB);

            //- Locate the start of the next sequence --------------------
            while ( !( nbr.get(i).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN &&
                       nbr.get((i+1)%nnbr).cl == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) ) {
                i = (i+1) % nnbr;
                if ( i == start ) {
                    return;
                }
            }
        }

        //-----------------------------------------------------------------
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
        //- Following classification strategy from the splitter algorithm -
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr;

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 ) {
                System.out.print("  * ");
            }
            else {
                System.out.print("  - ");
            }
            System.out.println("Vertex/face pair V[" + v.id + "] / f[" + f.id + "]");
        }

        nbr = vertexFaceGetNeighborhood(v, f.containingPlane, BvsA);
        if ( inplaneEdgesOn(nbr) ) {
            // In "strict analogy" to the splitter problem
            Collections.reverse(nbr);
        }

        if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 ) {
            System.out.println("   - Initial sector neigborhood by near end vertices:");
            printNbr(nbr);
        }

        vertexFaceReclassifyOnSectorsNoPeekVersion(nbr, f, f.containingPlane,
            BvsA, op);

        //- Adjusting results for set operation interpretation ------------
        boolean borrowed = false;

        int i;
        for ( i = 0; !borrowed && i < nbr.size(); i++ ) {
            nbr.get(i).updateLabel(BvsA);
        }

        if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 ) {
            System.out.println("   - Sector neigborhood reclassified on sectors (8-way boundary classification):");
            printNbr(nbr);
        }

        vertexFaceReclassifyOnEdges(nbr, op, borrowed);

        if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 ) {
            System.out.println("   - Sector neigborhood reclassified on edges:");
            printNbr(nbr);
        }

        vertexFaceInsertNullEdges(nbr, f, v, BvsA, borrowed, inSolidA, inSolidB);
    }

    private static void makering(
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
        //solida = v.emanatingHalfEdge.parentLoop.parentFace.parentSolid;
        //solidb = f.parentSolid;

        he = f.boundariesList.get(0).boundaryStartHalfEdge;

        int vn1, vn2;
        vn1 = nextVertexId(solida, solidb);
        solidb.lmev(he, he, vn1, v.position);
        he = solidb.findVertex(vn1).emanatingHalfEdge;
        solidb.lkemr(he.mirrorHalfEdge(), he);

        vn2 = nextVertexId(solida, solidb);
        solidb.lmev(he, he, vn2, v.position);

        if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
            System.out.println("       -> MAKERING (Vertex/face pierce):");
            System.out.println("          . New vertexes: " + vn1 + "/" + vn2 + ".");
            //he.startingVertex.debugColor = new ColorRgb(0, 0, 1);
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

    /**
    Constructs a vector along the bisector of the sector defined by `he`.
    that points inward the he's containing face.
    */
    protected static Vector3D inside(_PolyhedralBoundedSolidHalfEdge he)
    {
        Vector3D middle = null;
        Vector3D a, b, n;

        a = (he.next()).startingVertex.position.substract(he.startingVertex.position);
        b = (he.previous()).startingVertex.position.substract(he.startingVertex.position);
        a.normalize();
        b.normalize();

        n = he.parentLoop.parentFace.containingPlane.getNormal();

        middle = n.crossProduct(a);
        middle.normalize();

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
                 (n.ref12.dotProduct(he.parentLoop.parentFace.containingPlane.getNormal()) > 0.0 ) ) {
                // Inside this conditional means: current vertex is a wide one
                if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                         n.ref1, n.ref2, numericContext) ) {
                    bisec = inside(he);
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

    private static double angleFromVectors(Vector3D u, Vector3D v, Vector3D a)
    {
        double x, y;
        double an;

        x = a.dotProduct(u);
        y = a.dotProduct(v);

        an = Math.acos(x);
        if ( y < 0 ) an *= -1;
        return an;
    }

    /**
    Given two coplanar sectors that share a common edge, this method determine
    if the sectors are edge neighbors (this case returs false) or overlaping
    sectors (this case returns true).
    */
    private static boolean sectorOverSector(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb,
        Vector3D commonEdge
    )
    {
        Vector3D boundingEdgeA = null;
        Vector3D boundingEdgeB = null;

        if ( colinearVectorsWithDirection(na.ref1, commonEdge) ) {
            boundingEdgeA = na.ref2;
        }
        else {
            boundingEdgeA = na.ref1;
        }

        if ( colinearVectorsWithDirection(nb.ref1, commonEdge) ) {
            boundingEdgeB = nb.ref2;
        }
        else {
            boundingEdgeB = nb.ref1;
        }

        if ( colinearVectorsWithDirection(boundingEdgeA, boundingEdgeB) ) {
            return true;
        }
        return false;
    }

    /**
    Checks if two coplanar sectors overlaps, by doing a "sector within" test
    for coplanar sectors: If the two given sectors are coplanar and with
    overlaping faces:
      - If sectors only intersects in one point returns false.
      - If sectors intersects on a line or area returns true.

    Following section [MANT1988].15.6.2. Note that this operation is not
    elaborated on [MANT1988], but left as an excercise.

    PRE: Given sectors are "coplanar".
    */
    private static boolean sectoroverlap(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb)
    {
        //- Convert side vectors of sectors into angles -------------------
        double a1, a2;
        double b1, b2;
        Vector3D u, v, a, b, c, n;

        n = na.he.parentLoop.parentFace.containingPlane.getNormal();
        u = new Vector3D(na.ref1);
        u.normalize();
        v = n.crossProduct(u);
        v.normalize();

        a = new Vector3D(na.ref2);
        a.normalize();
        b = new Vector3D(nb.ref1);
        b.normalize();
        c = new Vector3D(nb.ref2);
        c.normalize();

        a1 = angleFromVectors(u, v, u);
        a2 = angleFromVectors(u, v, a);
        b1 = angleFromVectors(u, v, b);
        b2 = angleFromVectors(u, v, c);

        //- Order the angles in ascending order angle intervals -----------
        // Given angles are between -180 and 180 degrees
        double t;

        if ( a1 > a2 ) {
            t = a1;
            a1 = a2;
            a2 = t;
        }
        if ( b1 > b2 ) {
            t = b1;
            b1 = b2;
            b2 = t;
        }

        //- Calculate interval intersection -------------------------------
        if ( PolyhedralBoundedSolidNumericPolicy.angleIntervalsOverlap(
                a2, b1, numericContext) ) {

            if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
                System.out.print(" <TRUE>");
            }

            return true;
        }

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
            System.out.print(" <FALSE>");
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
        if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
            dir, ref1, numericContext) ) {
            return (ref1.dotProduct(dir) > 0.0);
        }
        c2 = ref2.crossProduct(dir);
        if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
            ref2, dir, numericContext) ) {
            return (ref2.dotProduct(dir) > 0.0);
        }
        t1 = compareToZero(c1.dotProduct(ref12));
        t2 = compareToZero(c2.dotProduct(ref12));
        return ( t1 < 0.0 && t2 < 0.0 );
    }

    private static boolean sctrwitthinProper(Vector3D dir, Vector3D ref1,
                                             Vector3D ref2, Vector3D ref12)
    {
        if ( colinearVectors(dir, ref1) || colinearVectors(dir, ref2) ) {
            return false;
        }

        return sctrwitthin(dir, ref1, ref2, ref12);
    }

    /**
    Sector intersection test.

    Following program [MANT1988].15.9. and section [MANT1988].15.6.2.
    */
    private static boolean vertexVertexSectorIntersectionTest(int i, int j)
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
        if ( PolyhedralBoundedSolidNumericPolicy.unitVectorsParallel(
            n1, n2, numericContext) ) {
            if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
                System.out.print(" <coplanar>");
            }
            return sectoroverlap(na, nb);
        }

        //-----------------------------------------------------------------
        c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
        c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
        if ( c1 && c2 ) {
            if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
                System.out.print(" <TRUE>");
            }
            return true;
        }
        else {
            intrs = intrs.multiply(-1);
            c1 = sctrwitthin(intrs, na.ref1, na.ref2, na.ref12);
            c2 = sctrwitthin(intrs, nb.ref1, nb.ref2, nb.ref12);
            if ( c1 && c2 ) {
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
                    System.out.print(" <TRUE>");
                }
                return true;
            }
        }

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
            System.out.print(" <FALSE>");
        }

        return false;
    }

    /**
    Given a pair of coincident vertices `va` (on solid A) and `vb` (on solid
    B), this method creates the lists `nba`, `nbb` and `sectors`, as explained
    in section [MANT1988].15.6.2. and program [MANT1988].15.7.

    Note that from all possible sector pairs, this method does not include
    in the `sectors` set any sector pair that touches just in one point.
    */
    private static void vertexVertexGetNeighborhood(
        _PolyhedralBoundedSolidVertex va,
        _PolyhedralBoundedSolidVertex vb)
    {
        //-----------------------------------------------------------------
        int i;

        nba = nbrpreproc(va);
        nbb = nbrpreproc(vb);
        sectors = new ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector>();


        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
            System.out.println("   - NBA list of neighbor sectors for vertex on {A}:");
            for ( i = 0; i < nba.size(); i++ ) {
                System.out.println("    . A[" + (i+1) + "]: " + nba.get(i));
            }
            System.out.println("   - NBB list of neighbor sectors for vertex on {B}:");
            for ( i = 0; i < nbb.size(); i++ ) {
                System.out.println("    . B[" + (i+1) + "]: " + nbb.get(i));
            }
        }

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge ha, hb;
        double d1, d2, d3, d4;
        int j;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector s;
        Vector3D na, nb;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex xa, xb;

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
            System.out.println("   - Initial intersection tests between sectors (false intersections are sectors touching on a single point):");
        }

        for ( i = 0; i < nba.size(); i++ ) {
            for ( j = 0; j < nbb.size(); j++ ) {

                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
                    System.out.print("    . A[" + (i+1) + "] / B[" + (j+1) + "]:");
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

                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0 ) {
                    System.out.print("\n");
                }

            }
        }

    }

    /**
    Following section [MANT1988].15.6.2. and program [MANT1988].15.10.

    Note that this sector deactivates the intersection flag for non-
    interpenetrating coplanar sectors (those who touches just in an edge or
    common line) and its neighbors.
    */
    private static void vertexVertexReclassifyOnSectors(int op)
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
            if ( sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s2a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s2b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                // This condition means: "current sectors are coplanar"

                // Determine orientation for current sector pair
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
                nonopposite = ( d < numericContext.unitVectorTolerance() );
                if ( nonopposite ) {
                    newsa = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    newsb = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
                }
                else {
                    newsa = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
                    newsb = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
                }
                si = sectors.get(i);

                // Propagate to neigbor sectors
                for ( j = 0; j < sectors.size(); j++ ) {
                    sj = sectors.get(j);
                    if ( (sj.secta == prevsecta) && (sj.sectb == sectb) ) {
                        if ( sj.s1a != _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s2a = newsa;
                        }
                    }
                    if ( (sj.secta == nextsecta) && (sj.sectb == sectb) ) {
                        if ( sj.s2a != _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s1a = newsa;
                        }
                    }
                    if ( (sj.secta == secta) && (sj.sectb == prevsectb) ) {
                        if ( sj.s1b != _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s2b = newsb;
                        }
                    }
                    if ( (sj.secta == secta) && (sj.sectb == nextsectb) ) {
                        if ( sj.s2b != _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                            sj.s1b = newsb;
                        }
                    }
                    if ( (sj.s1a == sj.s2a) && 
                         (sj.s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN || sj.s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                        sj.intersect = false;
                    }
                    if ( (sj.s1b == sj.s2b) && 
                         (sj.s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN || sj.s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                        sj.intersect = false;
                    }
                }

                // End
                si.s1a = si.s2a = newsa;
                si.s1b = si.s2b = newsb;
                si.intersect = false;
            }
        }
    }

    private static boolean colinearVectors(Vector3D a, Vector3D b)
    {
        return PolyhedralBoundedSolidNumericPolicy
            .vectorsColinear(a, b, numericContext);
    }

    public static boolean colinearVectorsWithDirection(Vector3D a, Vector3D b)
    {
        if ( PolyhedralBoundedSolidNumericPolicy
            .vectorsColinear(a, b, numericContext) ) {
            if ( a.dotProduct(b) >= 0 ) return true;
        }
        return false;
    }

    private static void addNoRepeat(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex> list,
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex element)
    {
        int i;

        for ( i = 0; i < list.size(); i++ ) {
            if ( list.get(i) == element ) return;
        }
        list.add(element);
    }

    /**
    Reclassification procedure for "on"-edges on the vertex/vertex clasiffier,
    as expected to work from functional high level description on section
    [MANT1986].15.6.2.  Astonishingly, the descriptions given on [MANT1988].15.
    and figures [MANT1988].15.10., [MANT1988].15.11., and [MANT1988].15.12.
    does not provides enough information to lead to a complete implementation
    of the complex case analysis required for sectors on sector and sectors
    on edge intersections.
    Fortunately, the missing details can be found on [MANT1986].6.2.2.

    PRE: All detected sector pairs are complient with the following:
    - They are not coplanar
    - They intersect in a common line

    Given two intersecting sectors (the "test sectors"), this method seek if
    their common intersection line intersects with a third sector inside
    (the "reference sector on edge-sector coincidence") or if that line
    intersects with a common edge of a third and a fourth sectors (the
    "reference pair of sectors on edge-edge coincidence"), and calls the
    corresponding case management methods for each situation.
    -----------------------------------------------------------------
    Reclassification procedure for "on"-edges on the vertex/vertex clasiffier,
    Original answer from [.WMANT2008].
    */
    private static void vertexVertexReclassifyOnEdges(int op)
    {
        int i, j, newsa, newsb;
        int secta, prevsecta;
        int sectb, prevsectb;

        // Search for doubly coplanar edges
        for ( i = 0; i < sectors.size(); i++ ) {
            // Double "on"-edge ?
            if ( sectors.get(i).intersect &&
                 sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                // Figure out the new classifications for the "on"-edges
                newsa = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                newsb = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;

                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0)?nba.size()-1:secta-1;
                prevsectb = (sectb == 0)?nbb.size()-1:sectb-1;

                // Reclassify all instances of the situation
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
                            (sectors.get(j).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                             sectors.get(j).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                        if ( sectors.get(j).s1b == sectors.get(j).s2b &&
                             (sectors.get(j).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                            sectors.get(j).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
        }

        // Search for singly coplanar edges
        for ( i = 0; i < sectors.size(); i++ ) {
            if ( sectors.get(i).intersect && sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0)?nba.size()-1:secta-1;
                prevsectb = (sectb == 0)?nbb.size()-1:sectb-1;
                newsa = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;

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
                             (sectors.get(j).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                              sectors.get(j).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
            else if ( sectors.get(i).intersect && sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0)?nba.size()-1:secta-1;
                prevsectb = (sectb == 0)?nbb.size()-1:sectb-1;
                newsb = (op == UNION)?_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;

                for ( j=0; j < sectors.size(); j++ ) {
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
                             (sectors.get(j).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ||
                              sectors.get(j).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT) ) {
                            sectors.get(j).intersect = false;
                        }
                    }
                }
            }
        }
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
        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
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
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println("        Recovered edge sequence case A");
                }
            }

            recoveredTo = recoverEdgeSequenceEndpointFromStrut(to, false);
            if ( recoveredTo != to ) {
                to = recoveredTo;
                changed = true;
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
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
                    if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case C");
                    }
                }
                else if ( fromPrev != null &&
                          fromPrev.startingVertex == to.startingVertex ) {
                    from = fromPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                        System.out.println("        Recovered edge sequence case D");
                    }
                }
                else if ( toPrev != null &&
                          toPrev.startingVertex == from.startingVertex ) {
                    to = toPrev;
                    changed = true;
                    if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
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
        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS ) != 0x00 ) {
            System.out.println("       -> LMEV (Separate edge sequence):");
            System.out.println("          . H1: " + to);
            System.out.println("          . H2: " + from);
            //from.startingVertex.debugColor = new ColorRgb(1, 0, 1);
        }

        int id = nextVertexId(inSolidA, inSolidB);

        s.lmev(to, from, id, to.startingVertex.position);

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
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
        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
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

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 &&
             (debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x00 ) {
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
        Vector3D ref1, ref2, ref12;

        ref1 = he.previous().startingVertex.position.substract(he.startingVertex.position);
        ref2 = he.next().startingVertex.position.substract(he.startingVertex.position);
        ref12 = ref1.crossProduct(ref2);
        if ( PolyhedralBoundedSolidNumericPolicy
            .vectorsColinear(ref1, ref2, numericContext) ) {
            return true;
        }
        return ((ref12.dotProduct(he.parentLoop.parentFace.containingPlane.getNormal()) > 0.0) ? false : true );
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
    private static void vertexVertexInsertNullEdges(PolyhedralBoundedSolid inSolidA,
                                                    PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidHalfEdge ha1 = null;
        _PolyhedralBoundedSolidHalfEdge ha2 = null;
        _PolyhedralBoundedSolidHalfEdge hb1 = null;
        _PolyhedralBoundedSolidHalfEdge hb2 = null;
        int i;

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
            System.out.println("   - Null edges insertion:");
        }

        int count = 0;

        for ( i = 0; i < sectors.size(); i++ ) {
            if ( sectors.get(i).intersect ) count++;
        }

        if ( count == 0 && sectors.size() > 0 ) {
            ha1 = nba.get(sectors.get(0).secta).he;
            hb1 = nbb.get(sectors.get(0).sectb).he;
            //System.out.println("**** EMPTY CASE");
        }

        i = 0;
        while ( true ) {
            //-------------------------------------------------------------
            while ( !sectors.get(i).intersect ) {
                i++;
                if ( i == sectors.size() ) {
                    return;
                }
            }
            if ( sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) {
                ha1 = nba.get(sectors.get(i).secta).he;
            }
            else {
                ha2 = nba.get(sectors.get(i).secta).he;
            }
            if ( sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                hb1 = nbb.get(sectors.get(i).sectb).he;
                i++;
            }
            else {
                hb2 = nbb.get(sectors.get(i).sectb).he;
                i++;
            }

            //-------------------------------------------------------------
            while ( !sectors.get(i).intersect ) {
                i++;
                if ( i == sectors.size() ) {
                    return;
                }
            }
            if ( sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT ) {
                ha1 = nba.get(sectors.get(i).secta).he;
            }
            else {
                ha2 = nba.get(sectors.get(i).secta).he;
            }
            if ( sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN ) {
                hb1 = nbb.get(sectors.get(i).sectb).he;
                i++;
            }
            else {
                hb2 = nbb.get(sectors.get(i).sectb).he;
                i++;
            }

            //-------------------------------------------------------------
            if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                System.out.println("    . Deciding case:");
                System.out.println("      -> Ha1: " + ha1);
                System.out.println("      -> Ha2: " + ha2);
                System.out.println("      -> Hb1: " + hb1);
                System.out.println("      -> Hb2: " + hb2);
            }

            //-------------------------------------------------------------
            if ( ha1 == ha2 ) {
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println("    . STRUT A CASE");
                }
                separateInterior(ha1, 0, getOrientation(ha1, hb1, hb2), inSolidA, inSolidB);
                separateEdgeSequence(hb1, hb2, 1, inSolidA, inSolidB);
            }
            else if ( hb1 == hb2 ) {
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println("    . STRUT B CASE");
                }
                separateInterior(hb1, 1, getOrientation(hb1, ha2, ha1), inSolidA, inSolidB);
                separateEdgeSequence(ha2, ha1, 0, inSolidA, inSolidB);
            }
            else {
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println("    . PARALLEL CASE");
                }
                separateEdgeSequence(ha2, ha1, 0, inSolidA, inSolidB);
                separateEdgeSequence(hb1, hb2, 1, inSolidA, inSolidB);
            }
            if ( i == sectors.size() ) {
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
        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) == 0x00 ) {
                System.out.print("  - ");
            }
            else {
                System.out.print("  * ");
            }
            System.out.print("Vertex of {A} / Vertex of {B} pair: A[" + va.id + "] / B[" + vb.id + "]");
            if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) == 0x00 ) {
                System.out.println(".");
            }
            else {
                System.out.println(" ->");
            }
        }

        vertexVertexGetNeighborhood(va, vb);

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
            System.out.println("   - Initial sector/sector intersection candidates:");
            for ( int i = 0; i < sectors.size(); i++ ) {
                System.out.println("    . " + sectors.get(i));
            }
        }

        vertexVertexReclassifyOnSectors(op);

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
            System.out.println("   - On sector reclassified:");
            for ( int i = 0; i < sectors.size(); i++ ) {
                System.out.println("    . " + sectors.get(i));
            }
        }

        vertexVertexReclassifyOnEdges(op);

        if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
            System.out.println("   - On edges reclassified:");
            for ( int i = 0; i < sectors.size(); i++ ) {
                System.out.println("    . " + sectors.get(i));
            }
        }

        vertexVertexInsertNullEdges(inSolidA, inSolidB);
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
            vertexFaceClassify(sonva.get(i).v, sonva.get(i).f, op, 0, inSolidA, inSolidB);
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 1.B. ----------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("VERTICES OF {B} TOUCHING FACES ON {A} (sonvb array of " + sonvb.size() + " matches):");
        }

        for ( i = 0; i < sonvb.size(); i++ ) {
            vertexFaceClassify(sonvb.get(i).v, sonvb.get(i).f, op, 1, inSolidA, inSolidB);
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 2. ------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("VERTEX-VERTEX PAIRS (sonvv array of " + sonvv.size() + " pairs):");
        }

        for ( i = 0; i < sonvv.size(); i++ ) {
            vertexVertexClassify(sonvv.get(i).va, sonvv.get(i).vb, op, inSolidA, inSolidB);
        }
    }

    private static void sortNullEdges()
    {
        Collections.sort(sonea);
        Collections.sort(soneb);
    }

    /**
    Following section [MANT1988].15.7. and program [MANT1988].15.13.
    */
    private static _PolyhedralBoundedSolidHalfEdge[]
    canJoin(_PolyhedralBoundedSolidHalfEdge hea,
             _PolyhedralBoundedSolidHalfEdge heb)
    {
        int i, j;
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
        //-----------------------------------------------------------------
        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------");
        }

        sortNullEdges();

        int i;

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("SORTED SET OF " + sonea.size() + " NULL EDGES PAIRS TO BE CONNECTED");
        }

        //-----------------------------------------------------------------
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

            //-----------------------------------------------------------------
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
                    // Force halfedges to go from old vertex to new vertex,
                    // that is, from IN to OUT direction.
                    System.out.println("********* FORCING ORDER!");
                }

                System.out.println("  - Processing pair [" + i + "]: "+
                    "He(A1): " + ha.startingVertex.id + "/" + ha.next().startingVertex.id + 
                    " He(A2): " + ham.startingVertex.id + "/" + ham.next().startingVertex.id + 
                    " He(B1): " + hb.startingVertex.id + "/" + hb.next().startingVertex.id +
                    " He(B2): " + hbm.startingVertex.id + "/" + hbm.next().startingVertex.id);
            }

            //-----------------------------------------------------------------
            // This assumes that for each null edge on solid a there is
            // another on solid b.
            boolean swap = false;
            nextedgea = sonea.get(i).e;
            nextedgeb = soneb.get(i).e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;

            //----
            // Force correct order of neighborhoods: they always goes from
            // IN to OUT.
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
            //----

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

    /**
    Answer integrator for the set operations algorithm (big phase 4).
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
        outRes.compactIds();
    }

    /**
    Classifies a point against a solid using ray-parity. Returns INSIDE,
    OUTSIDE or LIMIT for ambiguous cases.
    */
    private static int classifyPointAgainstSolid(
        PolyhedralBoundedSolid solid,
        Vector3D point)
    {
        int i, j;
        _PolyhedralBoundedSolidFace face;
        double eps = numericContext.bigEpsilon();

        if ( solid == null || solid.polygonsList.size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        // Boundary quick check.
        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            face = solid.polygonsList.get(i);
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
                return ((hits % 2) == 1) ? Geometry.INSIDE : Geometry.OUTSIDE;
            }
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
        int sawLimit = 0;

        if ( solidA == null || solidA.verticesList.size() < 1 ) {
            return Geometry.OUTSIDE;
        }

        for ( i = 0; i < solidA.verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solidA.verticesList.get(i);
            int status = classifyPointAgainstSolid(solidB, v.position);
            if ( status == Geometry.INSIDE ) {
                return Geometry.INSIDE;
            }
            if ( status == Geometry.OUTSIDE ) {
                return Geometry.OUTSIDE;
            }
            sawLimit = 1;
        }

        return (sawLimit != 0) ? Geometry.LIMIT : Geometry.OUTSIDE;
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

        if ( op == INTERSECTION ) {
            if ( aInB == Geometry.INSIDE ) {
                outRes.merge(inSolidA);
            }
            else if ( bInA == Geometry.INSIDE ) {
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        if ( op == UNION ) {
            if ( aInB == Geometry.INSIDE ) {
                outRes.merge(inSolidB);
            }
            else if ( bInA == Geometry.INSIDE ) {
                outRes.merge(inSolidA);
            }
            else {
                outRes.merge(inSolidA);
                outRes.merge(inSolidB);
            }
            return outRes;
        }

        // DIFFERENCE
        if ( aInB == Geometry.INSIDE ) {
            return outRes;
        }
        if ( bInA == Geometry.INSIDE ) {
            outRes.merge(inSolidA);
            inSolidB.revert();
            outRes.merge(inSolidB);
            outRes.compactIds();
            return outRes;
        }
        outRes.merge(inSolidA);
        return outRes;
    }

    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        return setOp(inSolidA, inSolidB, op, false);
    }

    /**
    */
    private static void debugSolid(PolyhedralBoundedSolid solid, String pattern)
    {
        System.out.println("**** DEBUGGING SOLID INFORMATION WRITEN TO FILES " +
            pattern + " ****");
        try {
            File fd = new File(pattern + ".txt");
            FileOutputStream fos = new FileOutputStream(fd);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            if ( offlineRenderer != null ) {
                offlineRenderer.execute(solid, pattern + ".png");
            }

            PersistenceElement.writeAsciiLine(bos, solid.toString());
            bos.close();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op, boolean withDebug)
    {
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);

        if ( withDebug ) {
            debugFlags = 0
              | DEBUG_01_STRUCTURE
              | DEBUG_02_GENERATOR
              | DEBUG_03_VERTEXFACECLASIFFIER
              | DEBUG_04_VERTEXVERTEXCLASIFFIER
              | DEBUG_05_CONNECT
              | DEBUG_06_FINISH
              | DEBUG_99_SHOWOPERATIONS
              ;
            offlineRenderer = PolyhedralBoundedSolidDebugger.createOfflineRenderer();
        }
        else {
            debugFlags = 0;
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("= [START OF SETOP REPORT] =================================================================================================================================");
            System.out.println("Dumping debug log for PolyhedralBoundedSolidSetOperator.setOp.");
            System.out.println("The algorithm structure is:");
            System.out.println("  0. Calculate vertex/face and vertex/vertex crossings.");
            System.out.println("  1. Classify and split for vertex/face cases.");
            System.out.println("  2. Classify and split for vertex/vertex cases.");
            System.out.println("  3. Connect.");
            System.out.println("  4. Finish.");
        }

        //-----------------------------------------------------------------
        PolyhedralBoundedSolid res = new PolyhedralBoundedSolid();

        sonea = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        soneb = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();

        //-----------------------------------------------------------------
        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage00");
            debugSolid(inSolidB, "outputB_stage00");
        }

        inSolidA.compactIds();
        inSolidB.compactIds();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidB);
        inSolidA.maximizeFaces();
        inSolidB.maximizeFaces();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidB);
        inSolidA.compactIds();
        inSolidB.compactIds();
        updmaxnames(inSolidB, inSolidA);
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage01");
            debugSolid(inSolidB, "outputB_stage01");
        }

        setOpGenerate(inSolidA, inSolidB);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage02");
            debugSolid(inSolidB, "outputB_stage02");
        }

        setOpClassify(op, inSolidA, inSolidB);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage03");
            debugSolid(inSolidB, "outputB_stage03");
        }

        if ( sonea.isEmpty() && sonvv.isEmpty() ) {
            // No intersections found
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op);
            if ( res.polygonsList.size() > 0 ) {
                PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
                res.compactIds();
                res.maximizeFaces();
                res.compactIds();
                PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
            }
            return res;
        }

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage04");
            debugSolid(inSolidB, "outputB_stage04");
        }

        setOpConnect();

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage05");
            debugSolid(inSolidB, "outputB_stage05");
        }

        setOpFinish(inSolidA, inSolidB, res, op);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage06");
            debugSolid(inSolidB, "outputB_stage06");
            debugSolid(res, "outputR_stage06");
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
        res.compactIds();
        res.maximizeFaces();
        res.compactIds();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);

        if ( withDebug ) {
            debugSolid(res, "outputR_stage07");
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("= [END OF SETOP REPORT] ===================================================================================================================================");
        }

        return res;
    }
}
