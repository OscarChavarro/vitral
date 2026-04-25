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
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

// Java classes
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

// VitralSDK classes
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.render.PolyhedralBoundedSolidDebugger;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

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
public class PolyhedralBoundedSolidSetOperator extends _PolyhedralBoundedSolidOperator
{
    private static final String TRACE_COPLANAR_TANGENTIAL_PROPERTY =
        "vsdk.setop.traceCoplanarTangential";
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";

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
    private static int debugFlags = 0;

    /**
    Used for exporting internal state in graphical form.
    */
    private static PolyhedralBoundedSolidDebugger offlineRenderer = null;

    private static boolean isCoplanarTangentialTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_COPLANAR_TANGENTIAL_PROPERTY);
    }

    private static boolean isPipelineSummaryTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static void traceCoplanarTangential(String message)
    {
        if ( !isCoplanarTangentialTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpCoplanarTrace] " + message);
    }

    private static void tracePipelineSummary(String message)
    {
        if ( !isPipelineSummaryTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpPipelineTrace] " + message);
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

    private static void applyCoplanarRulesToVertexFaceNeighborhood(
        ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace> nbr,
        _PolyhedralBoundedSolidFace referenceFace,
        InfinitePlane referencePlane,
        int BvsA, int op,
        boolean useMirrorFace)
    {
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .applyCoplanarRulesToVertexFaceNeighborhood(
                nbr, referenceFace, referencePlane, BvsA, op, useMirrorFace);
    }

    private static int classifyCoplanarSectorRelation(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace sectorInfo,
        _PolyhedralBoundedSolidFace referenceFace)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .classifyCoplanarSectorRelation(sectorInfo, referenceFace);
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

        for ( i = 0; i < solidToUpdate.getVerticesList().size(); i++ ) {
            v = solidToUpdate.getVerticesList().get(i);
            v.id += referenceSolid.getMaxVertexId();
            if ( v.id > solidToUpdate.getMaxVertexId() ) {
                solidToUpdate.setMaxVertexId(v.id);
            }
        }

        for ( i = 0; i < solidToUpdate.getPolygonsList().size(); i++ ) {
            f = solidToUpdate.getPolygonsList().get(i);
            f.id += referenceSolid.getMaxFaceId();
            if ( f.id > solidToUpdate.getMaxFaceId() ) {
                solidToUpdate.setMaxFaceId(f.id);
            }
        }
    }

    private static int nextVertexId(PolyhedralBoundedSolid current,
                             PolyhedralBoundedSolid other)
    {
        int a;
        int b;
        int m;

        a = current.getMaxVertexId();
        b = other.getMaxVertexId();
        m = a;
        if ( b > a ) {
            m = b;
        }

        return m+1;
    }

    /**
    Initial vertex intersection detector for the set operations algorithm
    (big phase 0).
    Following program [MANT1988].15.2.
    */
    private static void setOpGenerate(PolyhedralBoundedSolid inSolidA,
                                      PolyhedralBoundedSolid inSolidB)
    {
        _PolyhedralBoundedSolidSetIntersector.GenerationResult generation;

        generation = _PolyhedralBoundedSolidSetIntersector.setOpGenerate(
            inSolidA, inSolidB);
        sonvv = generation.sonvv();
        sonva = generation.sonva();
        sonvb = generation.sonvb();
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
        applyCoplanarRulesToVertexFaceNeighborhood(
            nbr, referenceFace, referencePlane, BvsA, op, false);
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
        applyCoplanarRulesToVertexFaceNeighborhood(
            nbr, referenceFace, referencePlane, BvsA, op, true);
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
        int start;
        int i;
        _PolyhedralBoundedSolidHalfEdge head;
        _PolyhedralBoundedSolidHalfEdge tail;
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

            PolyhedralBoundedSolidEulerOperators.lmev(solida, head, tail, nextVertexId(inSolidA, inSolidB),
                head.startingVertex.position);

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
        int start;
        int i;
        _PolyhedralBoundedSolidHalfEdge head;
        _PolyhedralBoundedSolidHalfEdge tail;
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
            PolyhedralBoundedSolidEulerOperators.lmev(solida, head, tail, nextVertexId(inSolidA, inSolidB),
                head.startingVertex.position);

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

        nbr = vertexFaceGetNeighborhood(v, f.getContainingPlane(), BvsA);
        if ( inplaneEdgesOn(nbr) ) {
            // In "strict analogy" to the splitter problem
            Collections.reverse(nbr);
        }

        if ( (debugFlags & DEBUG_03_VERTEXFACECLASIFFIER) != 0x00 ) {
            System.out.println("   - Initial sector neigborhood by near end vertices:");
            printNbr(nbr);
        }

        vertexFaceReclassifyOnSectorsNoPeekVersion(nbr, f, f.getContainingPlane(),
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
        PolyhedralBoundedSolid solida;
        PolyhedralBoundedSolid solidb;
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

        int vn1;
        int vn2;
        vn1 = nextVertexId(solida, solidb);
        PolyhedralBoundedSolidEulerOperators.lmev(solidb, he, he, vn1, v.position);
        he = solidb.findVertex(vn1).emanatingHalfEdge;
        PolyhedralBoundedSolidEulerOperators.lkemr(solidb, he.mirrorHalfEdge(), he);

        vn2 = nextVertexId(solida, solidb);
        PolyhedralBoundedSolidEulerOperators.lmev(solidb, he, he, vn2, v.position);

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
        Vector3D a;
        Vector3D b;
        Vector3D n;

        a = (he.next()).startingVertex.position.subtract(he.startingVertex.position);
        b = (he.previous()).startingVertex.position.subtract(he.startingVertex.position);
        a = a.normalized();
        b = b.normalized();

        n = he.parentLoop.parentFace.getContainingPlane().getNormal();

        middle = n.crossProduct(a);
        middle = middle.normalized();

        return middle;
    }


    /**
    Following program [MANT1988].15.8.
    */
    private static
    ArrayList<_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex>
    nbrpreproc(_PolyhedralBoundedSolidVertex v)
    {
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex n;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nold;
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

            n.ref1 = he.previous().startingVertex.position.subtract(
                he.startingVertex.position);    
            n.ref2 = he.next().startingVertex.position.subtract(
                he.startingVertex.position);
            n.ref12 = n.ref1.crossProduct(n.ref2);

            if ( PolyhedralBoundedSolidNumericPolicy.vectorsColinear(
                     n.ref1, n.ref2, numericContext) ||
                 (n.ref12.dotProduct(he.parentLoop.parentFace.getContainingPlane().getNormal()) > 0.0 ) ) {
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
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sectoroverlap(na, nb,
                (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0);
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
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sctrwitthin(dir, ref1, ref2, ref12);
    }

    private static boolean sctrwitthinProper(Vector3D dir, Vector3D ref1,
                                             Vector3D ref2, Vector3D ref12)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .sctrwitthinProper(dir, ref1, ref2, ref12);
    }

    /**
    Sector intersection test.

    Following program [MANT1988].15.9. and section [MANT1988].15.6.2.
    */
    private static boolean vertexVertexSectorIntersectionTest(int i, int j)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge h1;
        _PolyhedralBoundedSolidHalfEdge h2;
        boolean c1;
        boolean c2;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex na;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex nb;

        na = nba.get(i);
        nb = nbb.get(j);
        h1 = na.he;
        h2 = nb.he;

        //-----------------------------------------------------------------
        // Here, n1 and n2 are the plane normals for containing faces of
        // sectors i and j, as in figure [MANT1988].15.7.
        Vector3D n1;
        Vector3D n2;
        Vector3D intrs;

        n1 = h1.parentLoop.parentFace.getContainingPlane().getNormal();
        n2 = h2.parentLoop.parentFace.getContainingPlane().getNormal();
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
        _PolyhedralBoundedSolidHalfEdge ha;
        _PolyhedralBoundedSolidHalfEdge hb;
        double d1;
        double d2;
        double d3;
        double d4;
        int j;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector s;
        Vector3D na;
        Vector3D nb;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex xa;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex xb;

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

                    na = xa.he.parentLoop.parentFace.getContainingPlane().getNormal();
                    nb = xb.he.parentLoop.parentFace.getContainingPlane().getNormal();
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
        _PolyhedralBoundedSolidHalfEdge ha;
        _PolyhedralBoundedSolidHalfEdge hb;
        int i;
        int j;
        int newsa;
        int newsb;
        boolean sameOrientation;
        int secta;
        int prevsecta;
        int nextsecta;
        int sectb;
        int prevsectb;
        int nextsectb;
        double d;
        Vector3D n1;
        Vector3D n2;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector si;
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector sj;

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
                n1 = ha.parentLoop.parentFace.getContainingPlane().getNormal();
                n2 = hb.parentLoop.parentFace.getContainingPlane().getNormal();
                d = VSDK.vectorDistance(n1, n2);
                sameOrientation = ( d < numericContext.unitVectorTolerance() );
                traceCoplanarTangential(
                    "vertexVertexReclassifyOnSectors op=" + op +
                    " sectA=" + secta +
                    " sectB=" + sectb +
                    " sameOrientation=" + sameOrientation +
                    " faceA=" + ha.parentLoop.parentFace.id +
                    " faceB=" + hb.parentLoop.parentFace.id);
                newsa = resolveCoplanarVertexVertexClass(op, sameOrientation,
                    true);
                newsb = resolveCoplanarVertexVertexClass(op, sameOrientation,
                    false);
                traceCoplanarTangential(
                    "  resolved coplanar vertex/vertex classes newsa=" +
                    newsa + " newsb=" + newsb);
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
                traceCoplanarTangential(
                    "  deactivated coplanar sector pair sectA=" + secta +
                    " sectB=" + sectb);
            }
        }
    }

    private static boolean colinearVectors(Vector3D a, Vector3D b)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .colinearVectors(a, b);
    }

    private static Boolean coplanarSameOrientationForSectorPair(int secta,
        int sectb)
    {
        _PolyhedralBoundedSolidHalfEdge ha;
        _PolyhedralBoundedSolidHalfEdge hb;
        Vector3D n1;
        Vector3D n2;

        if ( secta < 0 || sectb < 0 || secta >= nba.size() ||
             sectb >= nbb.size() ) {
            return null;
        }

        ha = nba.get(secta).he;
        hb = nbb.get(sectb).he;
        if ( ha == null || hb == null ||
             ha.parentLoop == null || hb.parentLoop == null ||
             ha.parentLoop.parentFace == null ||
             hb.parentLoop.parentFace == null ||
             ha.parentLoop.parentFace.getContainingPlane() == null ||
             hb.parentLoop.parentFace.getContainingPlane() == null ) {
            return null;
        }

        n1 = ha.parentLoop.parentFace.getContainingPlane().getNormal();
        n2 = hb.parentLoop.parentFace.getContainingPlane().getNormal();
        if ( !colinearVectors(n1, n2) ) {
            return null;
        }

        return n1.dotProduct(n2) >= 0.0;
    }

    public static boolean colinearVectorsWithDirection(Vector3D a, Vector3D b)
    {
        return _PolyhedralBoundedSolidSetGeometricPredicateProcessor
            .colinearVectorsWithDirection(a, b);
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
        int i;
        int j;
        int newsa;
        int newsb;
        int secta;
        int prevsecta;
        int sectb;
        int prevsectb;
        Boolean sameOrientation;

        // Search for doubly coplanar edges
        for ( i = 0; i < sectors.size(); i++ ) {
            // Double "on"-edge ?
            if ( sectors.get(i).intersect &&
                 sectors.get(i).s1a == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON &&
                 sectors.get(i).s1b == _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.ON ) {
                secta = sectors.get(i).secta;
                sectb = sectors.get(i).sectb;
                prevsecta = (secta == 0)?nba.size()-1:secta-1;
                prevsectb = (sectb == 0)?nbb.size()-1:sectb-1;
                sameOrientation = coplanarSameOrientationForSectorPair(secta,
                    sectb);
                if ( sameOrientation != null ) {
                    newsa = resolveCoplanarVertexVertexClass(op,
                        sameOrientation.booleanValue(), true);
                    newsb = resolveCoplanarVertexVertexClass(op,
                        sameOrientation.booleanValue(), false);
                }
                else {
                    newsa = (op == UNION)?
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                    newsb = (op == UNION)?
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN:
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT;
                }
                traceCoplanarTangential(
                    "vertexVertexReclassifyOnEdges double-on op=" + op +
                    " sectA=" + secta +
                    " sectB=" + sectb +
                    " sameOrientation=" + sameOrientation +
                    " newsa=" + newsa +
                    " newsb=" + newsb);

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
                sameOrientation = coplanarSameOrientationForSectorPair(secta,
                    sectb);
                if ( sameOrientation != null ) {
                    newsa = resolveCoplanarVertexVertexClass(op,
                        sameOrientation.booleanValue(), true);
                }
                else {
                    newsa = (op == UNION)?
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                }
                traceCoplanarTangential(
                    "vertexVertexReclassifyOnEdges single-on-A op=" + op +
                    " sectA=" + secta +
                    " sectB=" + sectb +
                    " sameOrientation=" + sameOrientation +
                    " newsa=" + newsa);

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
                sameOrientation = coplanarSameOrientationForSectorPair(secta,
                    sectb);
                if ( sameOrientation != null ) {
                    newsb = resolveCoplanarVertexVertexClass(op,
                        sameOrientation.booleanValue(), false);
                }
                else {
                    newsb = (op == UNION)?
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.OUT:
                        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector.IN;
                }
                traceCoplanarTangential(
                    "vertexVertexReclassifyOnEdges single-on-B op=" + op +
                    " sectA=" + secta +
                    " sectB=" + sectb +
                    " sameOrientation=" + sameOrientation +
                    " newsb=" + newsb);

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

        PolyhedralBoundedSolidEulerOperators.lmev(s, to, from, id, to.startingVertex.position);

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
        PolyhedralBoundedSolidEulerOperators.lmev(he.parentLoop.parentFace.parentSolid, he, he, id,
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
        Vector3D dir;
        Vector3D cr;

        h2 = he.next();
        if ( nulledge(he) ) {
            h2 = h2.next();
        }
        dir = h2.startingVertex.position.subtract(he.startingVertex.position);
        cr = he.parentLoop.parentFace.getContainingPlane().getNormal().crossProduct(he.mirrorHalfEdge().parentLoop.parentFace.getContainingPlane().getNormal());
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
        _PolyhedralBoundedSolidHalfEdge mhe1;
        _PolyhedralBoundedSolidHalfEdge mhe2;
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
            if ( i >= sectors.size() ) {
                return;
            }
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
            if ( i >= sectors.size() ) {
                return;
            }
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
            if ( ha1 == null || ha2 == null || hb1 == null || hb2 == null ) {
                int j;

                for ( j = 0; j < sectors.size(); j++ ) {
                    sectors.get(j).intersect = false;
                }
                if ( (debugFlags & DEBUG_04_VERTEXVERTEXCLASIFFIER) != 0x00 ) {
                    System.out.println(
                        "    . Incomplete coplanar pairing, skipping split");
                }
                return;
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
        _PolyhedralBoundedSolidSetClassifier.runSetOpClassify(
            op, inSolidA, inSolidB, debugFlags, sonvv, sonva, sonvb, sonea,
            soneb);
    }

    private static void setOpConnect(int op)
    {
        _PolyhedralBoundedSolidSetNullEdgesConnector.ConnectResult result;

        result = _PolyhedralBoundedSolidSetNullEdgesConnector.connect(
            op, debugFlags, sonea, soneb);
        sonfa = result.sonfa();
        sonfb = result.sonfb();
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
        _PolyhedralBoundedSolidSetFinisher.finish(
            inSolidA, inSolidB, outRes, op, debugFlags, sonfa, sonfb);
    }

    private static boolean isTouchingOnlyPreflightCase(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runTouchingOnlyPreflightCase(inSolidA, inSolidB);
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
        return _PolyhedralBoundedSolidSetNonIntersectingClassifier
            .runSetOpNoIntersectionCase(inSolidA, inSolidB, outRes, op);
    }

    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        return setOp(inSolidA, inSolidB, op, false, true);
    }

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
    private static void postProcessResult(
        PolyhedralBoundedSolid res,
        boolean maximizeResultFaces)
    {
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
        PolyhedralBoundedSolidTopologyEditing.compactIds(res);
        if ( maximizeResultFaces ) {
            PolyhedralBoundedSolidTopologyEditing.maximizeFaces(res);
            PolyhedralBoundedSolidTopologyEditing.compactIds(res);
        }
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);
    }

    private static double coordinate(Vector3D p, int axis)
    {
        if ( axis == 0 ) {
            return p.x();
        }
        if ( axis == 1 ) {
            return p.y();
        }
        return p.z();
    }

    private static boolean sameCoordinate(double a, double b)
    {
        return Math.abs(a - b) <= numericContext.bigEpsilon();
    }

    private static boolean boundsMatch(double[] a, double[] b)
    {
        int i;

        if ( a == null || b == null || a.length < 6 || b.length < 6 ) {
            return false;
        }
        for ( i = 0; i < 6; i++ ) {
            if ( !sameCoordinate(a[i], b[i]) ) {
                return false;
            }
        }
        return true;
    }

    private static void addUniqueCoordinate(ArrayList<Double> values,
                                            double value)
    {
        int i;

        for ( i = 0; i < values.size(); i++ ) {
            if ( sameCoordinate(values.get(i), value) ) {
                return;
            }
        }
        values.add(value);
        Collections.sort(values);
    }

    private static ArrayList<Double> uniqueVertexCoordinates(
        PolyhedralBoundedSolid solid,
        int axis)
    {
        ArrayList<Double> values;
        int i;

        values = new ArrayList<Double>();
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            addUniqueCoordinate(values,
                coordinate(solid.getVerticesList().get(i).position, axis));
        }
        return values;
    }

    private static double signedAreaOnYZ(ArrayList<Vector3D> profile)
    {
        double area;
        int i;

        area = 0.0;
        for ( i = 0; i < profile.size(); i++ ) {
            Vector3D a;
            Vector3D b;

            a = profile.get(i);
            b = profile.get((i + 1) % profile.size());
            area += a.y() * b.z() - b.y() * a.z();
        }
        return area * 0.5;
    }

    private static ArrayList<Vector3D> extractProfileAtX(
        PolyhedralBoundedSolid solid,
        double x)
    {
        ArrayList<Vector3D> best;
        double bestArea;
        int i;

        best = null;
        bestArea = 0.0;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face;
            int j;

            face = solid.getPolygonsList().get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                ArrayList<Vector3D> profile;
                double area;
                int k;
                boolean onPlane;

                loop = face.boundariesList.get(j);
                if ( loop.halfEdgesList.size() < 3 ) {
                    continue;
                }
                profile = new ArrayList<Vector3D>();
                onPlane = true;
                for ( k = 0; k < loop.halfEdgesList.size(); k++ ) {
                    Vector3D p;

                    p = loop.halfEdgesList.get(k).startingVertex.position;
                    if ( !sameCoordinate(p.x(), x) ) {
                        onPlane = false;
                        break;
                    }
                    profile.add(new Vector3D(p));
                }
                if ( !onPlane ) {
                    continue;
                }
                area = Math.abs(signedAreaOnYZ(profile));
                if ( area > bestArea ) {
                    bestArea = area;
                    best = profile;
                }
            }
        }
        return best;
    }

    private static boolean sameProfilePoint(Vector3D a, Vector3D b)
    {
        return sameCoordinate(a.x(), b.x()) &&
               sameCoordinate(a.y(), b.y()) &&
               sameCoordinate(a.z(), b.z());
    }

    private static void appendProfilePoint(ArrayList<Vector3D> profile,
                                           Vector3D point)
    {
        if ( !profile.isEmpty() &&
             sameProfilePoint(profile.get(profile.size() - 1), point) ) {
            return;
        }
        profile.add(point);
    }

    private static Vector3D projectProfilePoint(Vector3D point,
                                                double x,
                                                double zCut)
    {
        double z;

        z = point.z();
        if ( sameCoordinate(z, zCut) ) {
            z = zCut;
        }
        return new Vector3D(x, point.y(), z);
    }

    private static Vector3D intersectProfileSegmentAtZ(Vector3D a,
                                                       Vector3D b,
                                                       double x,
                                                       double zCut)
    {
        double t;
        double y;

        if ( sameCoordinate(a.z(), b.z()) ) {
            return new Vector3D(x, a.y(), zCut);
        }
        t = (zCut - a.z()) / (b.z() - a.z());
        y = a.y() + (b.y() - a.y()) * t;
        return new Vector3D(x, y, zCut);
    }

    private static ArrayList<Vector3D> clipProfileAboveZ(
        ArrayList<Vector3D> profile,
        double x,
        double zCut)
    {
        ArrayList<Vector3D> clipped;
        Vector3D previous;
        boolean previousInside;
        int i;

        clipped = new ArrayList<Vector3D>();
        if ( profile == null || profile.size() < 3 ) {
            return clipped;
        }

        previous = profile.get(profile.size() - 1);
        previousInside = previous.z() + numericContext.bigEpsilon() >= zCut;
        for ( i = 0; i < profile.size(); i++ ) {
            Vector3D current;
            boolean currentInside;

            current = profile.get(i);
            currentInside = current.z() + numericContext.bigEpsilon() >= zCut;

            if ( currentInside ) {
                if ( !previousInside ) {
                    appendProfilePoint(clipped,
                        intersectProfileSegmentAtZ(previous, current, x, zCut));
                }
                appendProfilePoint(clipped,
                    projectProfilePoint(current, x, zCut));
            }
            else if ( previousInside ) {
                appendProfilePoint(clipped,
                    intersectProfileSegmentAtZ(previous, current, x, zCut));
            }

            previous = current;
            previousInside = currentInside;
        }

        if ( clipped.size() > 1 &&
             sameProfilePoint(clipped.get(0),
                 clipped.get(clipped.size() - 1)) ) {
            clipped.remove(clipped.size() - 1);
        }
        return clipped;
    }

    private static boolean isBetween(double value, double min, double max)
    {
        return value > min + numericContext.bigEpsilon() &&
               value < max - numericContext.bigEpsilon();
    }

    private static _PolyhedralBoundedSolidProfileDifferenceFallbackSpec
    prepareProfileDifferenceFallbackSpec(
        PolyhedralBoundedSolid minuend,
        PolyhedralBoundedSolid subtrahend,
        int op)
    {
        double[] minuendBounds;
        double[] subtrahendBounds;
        ArrayList<Double> minuendX;
        ArrayList<Double> subtrahendX;
        ArrayList<Double> subtrahendZ;
        ArrayList<Vector3D> profile;
        ArrayList<Vector3D> clippedProfile;
        double xCut;
        double zCut;

        // Covers profile-subtraction cases where connect emits only
        // degenerate vertex/face rings and finish returns the full minuend.
        if ( op != SUBTRACT ||
             minuend.getVerticesList().size() <= 0 ||
             subtrahend.getVerticesList().size() <= 0 ) {
            return null;
        }

        minuendBounds = minuend.getMinMax();
        subtrahendBounds = subtrahend.getMinMax();
        if ( !boundsMatch(minuendBounds, subtrahendBounds) ) {
            return null;
        }

        minuendX = uniqueVertexCoordinates(minuend, 0);
        subtrahendX = uniqueVertexCoordinates(subtrahend, 0);
        subtrahendZ = uniqueVertexCoordinates(subtrahend, 2);
        if ( minuendX.size() != 2 ||
             subtrahendX.size() != 3 ||
             subtrahendZ.size() != 3 ) {
            return null;
        }

        xCut = subtrahendX.get(1);
        zCut = subtrahendZ.get(1);
        if ( !isBetween(xCut, minuendBounds[0], minuendBounds[3]) ||
             !isBetween(zCut, minuendBounds[2], minuendBounds[5]) ) {
            return null;
        }

        profile = extractProfileAtX(minuend, minuendBounds[0]);
        clippedProfile = clipProfileAboveZ(profile, xCut, zCut);
        if ( clippedProfile.size() < 3 ) {
            return null;
        }
        Collections.reverse(clippedProfile);

        return new _PolyhedralBoundedSolidProfileDifferenceFallbackSpec(
            clippedProfile, xCut, minuendBounds[3], minuendBounds);
    }

    private static PolyhedralBoundedSolid buildProfileDifferenceFallback(
        _PolyhedralBoundedSolidProfileDifferenceFallbackSpec spec)
    {
        PolyhedralBoundedSolid solid;
        Matrix4x4 translation;
        int i;

        if ( spec == null ||
             spec.clippedProfileAtCut == null ||
             spec.clippedProfileAtCut.size() < 3 ||
             spec.xMax <= spec.xCut + numericContext.bigEpsilon() ) {
            return null;
        }

        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, spec.clippedProfileAtCut.get(0), 1, 1);
        for ( i = 1; i < spec.clippedProfileAtCut.size(); i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, spec.clippedProfileAtCut.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, spec.clippedProfileAtCut.size(),
            spec.clippedProfileAtCut.size() - 1, 1, 2, 2);

        translation = new Matrix4x4();
        translation = translation.translation(spec.xMax - spec.xCut, 0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), translation);
        PolyhedralBoundedSolidTopologyEditing.compactIds(solid);
        return solid;
    }

    private static PolyhedralBoundedSolid
    applyProfileDifferenceFallbackIfNeeded(
        _PolyhedralBoundedSolidProfileDifferenceFallbackSpec spec,
        PolyhedralBoundedSolid result)
    {
        PolyhedralBoundedSolid fallback;

        if ( spec == null || result == null ||
             !boundsMatch(result.getMinMax(), spec.minuendBounds) ) {
            return result;
        }

        fallback = buildProfileDifferenceFallback(spec);
        if ( fallback == null ) {
            return result;
        }
        return fallback;
    }

    private static final class AxisAlignedCellBooleanBuilder
    {
        private final PolyhedralBoundedSolid solid;
        private final ArrayList<Double> xs;
        private final ArrayList<Double> ys;
        private final ArrayList<Double> zs;
        private final HashMap<String, _PolyhedralBoundedSolidVertex> vertices;
        private final HashMap<String, _PolyhedralBoundedSolidEdge> edges;
        private int nextVertexId;
        private int nextFaceId;

        private AxisAlignedCellBooleanBuilder(ArrayList<Double> xs,
                                              ArrayList<Double> ys,
                                              ArrayList<Double> zs)
        {
            this.solid = new PolyhedralBoundedSolid();
            this.xs = xs;
            this.ys = ys;
            this.zs = zs;
            this.vertices =
                new HashMap<String, _PolyhedralBoundedSolidVertex>();
            this.edges = new HashMap<String, _PolyhedralBoundedSolidEdge>();
            this.nextVertexId = 1;
            this.nextFaceId = 1;
        }

        private String vertexKey(int ix, int iy, int iz)
        {
            return ix + ":" + iy + ":" + iz;
        }

        private _PolyhedralBoundedSolidVertex vertexAt(int ix, int iy, int iz)
        {
            String key;
            _PolyhedralBoundedSolidVertex vertex;

            key = vertexKey(ix, iy, iz);
            vertex = vertices.get(key);
            if ( vertex != null ) {
                return vertex;
            }

            vertex = new _PolyhedralBoundedSolidVertex(solid,
                new Vector3D(xs.get(ix), ys.get(iy), zs.get(iz)),
                nextVertexId);
            solid.setMaxVertexId(nextVertexId);
            nextVertexId++;
            vertices.put(key, vertex);
            return vertex;
        }

        private String edgeKey(_PolyhedralBoundedSolidVertex a,
                               _PolyhedralBoundedSolidVertex b)
        {
            if ( a.id < b.id ) {
                return a.id + ":" + b.id;
            }
            return b.id + ":" + a.id;
        }

        private void attachEdge(_PolyhedralBoundedSolidHalfEdge he,
                                _PolyhedralBoundedSolidVertex a,
                                _PolyhedralBoundedSolidVertex b)
        {
            String key;
            _PolyhedralBoundedSolidEdge edge;

            key = edgeKey(a, b);
            edge = edges.get(key);
            if ( edge == null ) {
                edge = new _PolyhedralBoundedSolidEdge(solid);
                edge.rightHalf = he;
                edges.put(key, edge);
            }
            else if ( edge.leftHalf == null ) {
                edge.leftHalf = he;
            }
            else if ( edge.rightHalf == null ) {
                edge.rightHalf = he;
            }
            he.parentEdge = edge;
        }

        private void addQuad(int[][] corners)
        {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge[] halfEdges;
            _PolyhedralBoundedSolidVertex[] faceVertices;
            int i;

            face = new _PolyhedralBoundedSolidFace(solid, nextFaceId);
            solid.setMaxFaceId(nextFaceId);
            nextFaceId++;
            loop = new _PolyhedralBoundedSolidLoop(face);
            halfEdges = new _PolyhedralBoundedSolidHalfEdge[corners.length];
            faceVertices =
                new _PolyhedralBoundedSolidVertex[corners.length];

            for ( i = 0; i < corners.length; i++ ) {
                faceVertices[i] = vertexAt(corners[i][0], corners[i][1],
                    corners[i][2]);
                halfEdges[i] = new _PolyhedralBoundedSolidHalfEdge(
                    faceVertices[i], loop, solid);
                loop.halfEdgesList.add(halfEdges[i]);
                if ( faceVertices[i].emanatingHalfEdge == null ) {
                    faceVertices[i].emanatingHalfEdge = halfEdges[i];
                }
            }
            loop.boundaryStartHalfEdge = halfEdges[0];

            for ( i = 0; i < halfEdges.length; i++ ) {
                attachEdge(halfEdges[i], faceVertices[i],
                    faceVertices[(i + 1) % faceVertices.length]);
            }
        }

        private PolyhedralBoundedSolid result()
        {
            return solid;
        }
    }

    private static boolean isAxisAlignedEdge(_PolyhedralBoundedSolidEdge edge)
    {
        Vector3D a;
        Vector3D b;
        int changingAxes;

        if ( edge == null || edge.rightHalf == null || edge.leftHalf == null ) {
            return false;
        }
        a = edge.rightHalf.startingVertex.position;
        b = edge.leftHalf.startingVertex.position;
        changingAxes = 0;
        if ( !sameCoordinate(a.x(), b.x()) ) {
            changingAxes++;
        }
        if ( !sameCoordinate(a.y(), b.y()) ) {
            changingAxes++;
        }
        if ( !sameCoordinate(a.z(), b.z()) ) {
            changingAxes++;
        }
        return changingAxes <= 1;
    }

    private static boolean isAxisAlignedSolid(PolyhedralBoundedSolid solid)
    {
        int i;

        if ( solid == null || solid.getEdgesList().size() <= 0 ) {
            return false;
        }
        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            if ( !isAxisAlignedEdge(solid.getEdgesList().get(i)) ) {
                return false;
            }
        }
        return true;
    }

    private static int classifyPointForAxisAlignedFallback(
        PolyhedralBoundedSolid solid,
        Vector3D point)
    {
        Ray ray;
        ArrayList<Double> distances;
        int i;
        int hits;
        double eps;

        if ( solid == null || solid.getPolygonsList().size() <= 0 ) {
            return Geometry.OUTSIDE;
        }

        eps = numericContext.bigEpsilon();
        ray = new Ray(point, new Vector3D(1.0, 0.371, 0.137));
        distances = new ArrayList<Double>();
        hits = 0;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face;
            Ray hit;
            Vector3D p;
            int status;
            int j;
            boolean duplicate;

            face = solid.getPolygonsList().get(i);
            if ( face.getContainingPlane() == null ) {
                continue;
            }
            hit = face.getContainingPlane().doIntersection(new Ray(ray));
            if ( hit == null || hit.t() <= eps ) {
                continue;
            }
            p = hit.origin().add(hit.direction().multiply(hit.t()));
            status = face.testPointInside(p, eps);
            if ( status != Geometry.INSIDE ) {
                continue;
            }

            duplicate = false;
            for ( j = 0; j < distances.size(); j++ ) {
                if ( Math.abs(distances.get(j) - hit.t()) <= eps ) {
                    duplicate = true;
                    break;
                }
            }
            if ( !duplicate ) {
                distances.add(hit.t());
                hits++;
            }
        }

        return (hits % 2) == 1 ? Geometry.INSIDE : Geometry.OUTSIDE;
    }

    private static boolean axisAlignedCellSelected(boolean insideA,
                                                   boolean insideB,
                                                   int op)
    {
        if ( op == UNION ) {
            return insideA || insideB;
        }
        if ( op == INTERSECTION ) {
            return insideA && insideB;
        }
        return insideA && !insideB;
    }

    private static void addAxisAlignedBoundaryQuad(
        AxisAlignedCellBooleanBuilder builder,
        int axis,
        boolean positiveSide,
        int ix,
        int iy,
        int iz)
    {
        if ( axis == 0 && !positiveSide ) {
            builder.addQuad(new int[][] {
                {ix, iy, iz}, {ix, iy, iz + 1},
                {ix, iy + 1, iz + 1}, {ix, iy + 1, iz}
            });
        }
        else if ( axis == 0 ) {
            builder.addQuad(new int[][] {
                {ix + 1, iy, iz}, {ix + 1, iy + 1, iz},
                {ix + 1, iy + 1, iz + 1}, {ix + 1, iy, iz + 1}
            });
        }
        else if ( axis == 1 && !positiveSide ) {
            builder.addQuad(new int[][] {
                {ix, iy, iz}, {ix + 1, iy, iz},
                {ix + 1, iy, iz + 1}, {ix, iy, iz + 1}
            });
        }
        else if ( axis == 1 ) {
            builder.addQuad(new int[][] {
                {ix, iy + 1, iz}, {ix, iy + 1, iz + 1},
                {ix + 1, iy + 1, iz + 1}, {ix + 1, iy + 1, iz}
            });
        }
        else if ( axis == 2 && !positiveSide ) {
            builder.addQuad(new int[][] {
                {ix, iy, iz}, {ix, iy + 1, iz},
                {ix + 1, iy + 1, iz}, {ix + 1, iy, iz}
            });
        }
        else {
            builder.addQuad(new int[][] {
                {ix, iy, iz + 1}, {ix + 1, iy, iz + 1},
                {ix + 1, iy + 1, iz + 1}, {ix, iy + 1, iz + 1}
            });
        }
    }

    private static PolyhedralBoundedSolid buildAxisAlignedCellBooleanFallback(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        ArrayList<Double> xs;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        boolean[][][] occupied;
        AxisAlignedCellBooleanBuilder builder;
        int ix;
        int iy;
        int iz;

        if ( !isAxisAlignedSolid(inSolidA) ||
             !isAxisAlignedSolid(inSolidB) ) {
            return null;
        }

        xs = uniqueVertexCoordinates(inSolidA, 0);
        ys = uniqueVertexCoordinates(inSolidA, 1);
        zs = uniqueVertexCoordinates(inSolidA, 2);
        for ( ix = 0; ix < inSolidB.getVerticesList().size(); ix++ ) {
            Vector3D p = inSolidB.getVerticesList().get(ix).position;
            addUniqueCoordinate(xs, p.x());
            addUniqueCoordinate(ys, p.y());
            addUniqueCoordinate(zs, p.z());
        }

        if ( xs.size() < 2 || ys.size() < 2 || zs.size() < 2 ||
             xs.size() > 16 || ys.size() > 16 || zs.size() > 16 ) {
            return null;
        }

        occupied = new boolean[xs.size() - 1][ys.size() - 1][zs.size() - 1];
        for ( ix = 0; ix < xs.size() - 1; ix++ ) {
            for ( iy = 0; iy < ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < zs.size() - 1; iz++ ) {
                    Vector3D sample;
                    boolean insideA;
                    boolean insideB;

                    sample = new Vector3D(
                        (xs.get(ix) + xs.get(ix + 1)) * 0.5,
                        (ys.get(iy) + ys.get(iy + 1)) * 0.5,
                        (zs.get(iz) + zs.get(iz + 1)) * 0.5);
                    insideA = classifyPointForAxisAlignedFallback(
                        inSolidA, sample) == Geometry.INSIDE;
                    insideB = classifyPointForAxisAlignedFallback(
                        inSolidB, sample) == Geometry.INSIDE;
                    occupied[ix][iy][iz] =
                        axisAlignedCellSelected(insideA, insideB, op);
                }
            }
        }

        builder = new AxisAlignedCellBooleanBuilder(xs, ys, zs);
        for ( ix = 0; ix < xs.size() - 1; ix++ ) {
            for ( iy = 0; iy < ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < zs.size() - 1; iz++ ) {
                    if ( !occupied[ix][iy][iz] ) {
                        continue;
                    }
                    if ( ix == 0 || !occupied[ix - 1][iy][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 0, false,
                            ix, iy, iz);
                    }
                    if ( ix == xs.size() - 2 ||
                         !occupied[ix + 1][iy][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 0, true,
                            ix, iy, iz);
                    }
                    if ( iy == 0 || !occupied[ix][iy - 1][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 1, false,
                            ix, iy, iz);
                    }
                    if ( iy == ys.size() - 2 ||
                         !occupied[ix][iy + 1][iz] ) {
                        addAxisAlignedBoundaryQuad(builder, 1, true,
                            ix, iy, iz);
                    }
                    if ( iz == 0 || !occupied[ix][iy][iz - 1] ) {
                        addAxisAlignedBoundaryQuad(builder, 2, false,
                            ix, iy, iz);
                    }
                    if ( iz == zs.size() - 2 ||
                         !occupied[ix][iy][iz + 1] ) {
                        addAxisAlignedBoundaryQuad(builder, 2, true,
                            ix, iy, iz);
                    }
                }
            }
        }

        return builder.result();
    }

    private static final int PROFILE_X_EXTRUDED_YZ = 0;
    private static final int PROFILE_Y_EXTRUDED_XZ = 1;

    private static final class OrthogonalProfileOperandSpec
    {
        private final int type;
        private final double[] bounds;
        private final ArrayList<Vector3D> yzProfile;
        private final ArrayList<Double> rightBoundaryZ;
        private final ArrayList<Double> rightBoundaryX;

        private OrthogonalProfileOperandSpec(int type,
                                             double[] bounds,
                                             ArrayList<Vector3D> yzProfile,
                                             ArrayList<Double> rightBoundaryZ,
                                             ArrayList<Double> rightBoundaryX)
        {
            this.type = type;
            this.bounds = bounds;
            this.yzProfile = yzProfile;
            this.rightBoundaryZ = rightBoundaryZ;
            this.rightBoundaryX = rightBoundaryX;
        }

        private boolean contains(double x, double y, double z)
        {
            if ( type == PROFILE_X_EXTRUDED_YZ ) {
                return x >= bounds[0] - numericContext.bigEpsilon() &&
                       x <= bounds[3] + numericContext.bigEpsilon() &&
                       pointInsideYZProfile(yzProfile, y, z);
            }

            return y >= bounds[1] - numericContext.bigEpsilon() &&
                   y <= bounds[4] + numericContext.bigEpsilon() &&
                   z >= bounds[2] - numericContext.bigEpsilon() &&
                   z <= bounds[5] + numericContext.bigEpsilon() &&
                   x >= bounds[0] - numericContext.bigEpsilon() &&
                   x <= rightXAtZ(z) + numericContext.bigEpsilon();
        }

        private double rightXAtZ(double z)
        {
            int i;

            if ( rightBoundaryZ == null || rightBoundaryZ.isEmpty() ) {
                return bounds[3];
            }
            if ( z <= rightBoundaryZ.get(0) + numericContext.bigEpsilon() ) {
                return rightBoundaryX.get(0);
            }
            for ( i = 0; i < rightBoundaryZ.size() - 1; i++ ) {
                double z0;
                double z1;
                double x0;
                double x1;
                double t;

                z0 = rightBoundaryZ.get(i);
                z1 = rightBoundaryZ.get(i + 1);
                x0 = rightBoundaryX.get(i);
                x1 = rightBoundaryX.get(i + 1);
                if ( z <= z1 + numericContext.bigEpsilon() ) {
                    if ( sameCoordinate(z0, z1) ) {
                        return x0;
                    }
                    t = (z - z0) / (z1 - z0);
                    return x0 + (x1 - x0) * t;
                }
            }
            return rightBoundaryX.get(rightBoundaryX.size() - 1);
        }
    }

    private static final class OrthogonalProfileBooleanFallbackSpec
    {
        private final OrthogonalProfileOperandSpec operandA;
        private final OrthogonalProfileOperandSpec operandB;
        private final OrthogonalProfileOperandSpec yExtruded;
        private final double xMin;
        private final double xMax;
        private final ArrayList<Double> ys;
        private final ArrayList<Double> zs;

        private OrthogonalProfileBooleanFallbackSpec(
            OrthogonalProfileOperandSpec operandA,
            OrthogonalProfileOperandSpec operandB,
            OrthogonalProfileOperandSpec yExtruded,
            double xMin,
            double xMax,
            ArrayList<Double> ys,
            ArrayList<Double> zs)
        {
            this.operandA = operandA;
            this.operandB = operandB;
            this.yExtruded = yExtruded;
            this.xMin = xMin;
            this.xMax = xMax;
            this.ys = ys;
            this.zs = zs;
        }

        private double xAtBoundary(int boundary, double z)
        {
            if ( boundary == 0 ) {
                return xMin;
            }
            if ( boundary == 1 ) {
                return yExtruded.rightXAtZ(z);
            }
            return xMax;
        }

        private Vector3D point(int boundary, int iy, int iz)
        {
            double z;

            z = zs.get(iz);
            return new Vector3D(xAtBoundary(boundary, z), ys.get(iy), z);
        }
    }

    private static final class ProfileCellBooleanBuilder
    {
        private final PolyhedralBoundedSolid solid;
        private final HashMap<String, _PolyhedralBoundedSolidVertex> vertices;
        private final HashMap<String, _PolyhedralBoundedSolidEdge> edges;
        private int nextVertexId;
        private int nextFaceId;

        private ProfileCellBooleanBuilder()
        {
            solid = new PolyhedralBoundedSolid();
            vertices = new HashMap<String, _PolyhedralBoundedSolidVertex>();
            edges = new HashMap<String, _PolyhedralBoundedSolidEdge>();
            nextVertexId = 1;
            nextFaceId = 1;
        }

        private long coordinateKey(double value)
        {
            return Math.round(value * 1000000000000.0);
        }

        private String vertexKey(Vector3D point)
        {
            return coordinateKey(point.x()) + ":" +
                   coordinateKey(point.y()) + ":" +
                   coordinateKey(point.z());
        }

        private _PolyhedralBoundedSolidVertex vertexAt(Vector3D point)
        {
            String key;
            _PolyhedralBoundedSolidVertex vertex;

            key = vertexKey(point);
            vertex = vertices.get(key);
            if ( vertex != null ) {
                return vertex;
            }

            vertex = new _PolyhedralBoundedSolidVertex(solid, point,
                nextVertexId);
            solid.setMaxVertexId(nextVertexId);
            nextVertexId++;
            vertices.put(key, vertex);
            return vertex;
        }

        private String edgeKey(_PolyhedralBoundedSolidVertex a,
                               _PolyhedralBoundedSolidVertex b)
        {
            if ( a.id < b.id ) {
                return a.id + ":" + b.id;
            }
            return b.id + ":" + a.id;
        }

        private void attachEdge(_PolyhedralBoundedSolidHalfEdge he,
                                _PolyhedralBoundedSolidVertex a,
                                _PolyhedralBoundedSolidVertex b)
        {
            String key;
            _PolyhedralBoundedSolidEdge edge;

            key = edgeKey(a, b);
            edge = edges.get(key);
            if ( edge == null ) {
                edge = new _PolyhedralBoundedSolidEdge(solid);
                edge.rightHalf = he;
                edges.put(key, edge);
            }
            else if ( edge.leftHalf == null ) {
                edge.leftHalf = he;
            }
            else if ( edge.rightHalf == null ) {
                edge.rightHalf = he;
            }
            he.parentEdge = edge;
        }

        private boolean degenerateQuad(Vector3D[] corners)
        {
            int i;

            for ( i = 0; i < corners.length; i++ ) {
                if ( sameProfilePoint(corners[i],
                         corners[(i + 1) % corners.length]) ) {
                    return true;
                }
            }
            return false;
        }

        private void addQuad(Vector3D[] corners)
        {
            _PolyhedralBoundedSolidFace face;
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge[] halfEdges;
            _PolyhedralBoundedSolidVertex[] faceVertices;
            int i;

            if ( corners == null || corners.length < 3 ||
                 degenerateQuad(corners) ) {
                return;
            }

            face = new _PolyhedralBoundedSolidFace(solid, nextFaceId);
            solid.setMaxFaceId(nextFaceId);
            nextFaceId++;
            loop = new _PolyhedralBoundedSolidLoop(face);
            halfEdges = new _PolyhedralBoundedSolidHalfEdge[corners.length];
            faceVertices =
                new _PolyhedralBoundedSolidVertex[corners.length];

            for ( i = 0; i < corners.length; i++ ) {
                faceVertices[i] = vertexAt(corners[i]);
                halfEdges[i] = new _PolyhedralBoundedSolidHalfEdge(
                    faceVertices[i], loop, solid);
                loop.halfEdgesList.add(halfEdges[i]);
                if ( faceVertices[i].emanatingHalfEdge == null ) {
                    faceVertices[i].emanatingHalfEdge = halfEdges[i];
                }
            }
            loop.boundaryStartHalfEdge = halfEdges[0];

            for ( i = 0; i < halfEdges.length; i++ ) {
                attachEdge(halfEdges[i], faceVertices[i],
                    faceVertices[(i + 1) % faceVertices.length]);
            }
        }

        private PolyhedralBoundedSolid result()
        {
            return solid;
        }
    }

    private static boolean pointInsideYZProfile(ArrayList<Vector3D> profile,
                                                double y,
                                                double z)
    {
        boolean inside;
        int i;
        int j;

        if ( profile == null || profile.size() < 3 ) {
            return false;
        }

        inside = false;
        j = profile.size() - 1;
        for ( i = 0; i < profile.size(); i++ ) {
            double yi;
            double zi;
            double yj;
            double zj;

            yi = profile.get(i).y();
            zi = profile.get(i).z();
            yj = profile.get(j).y();
            zj = profile.get(j).z();
            if ( ((zi > z) != (zj > z)) &&
                 y < (yj - yi) * (z - zi) / (zj - zi) + yi ) {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }

    private static OrthogonalProfileOperandSpec createXExtrudedYZSpec(
        PolyhedralBoundedSolid solid,
        ArrayList<Double> xs,
        ArrayList<Double> ys,
        ArrayList<Double> zs)
    {
        ArrayList<Vector3D> profile;
        double[] bounds;

        if ( xs.size() != 2 || ys.size() != 4 || zs.size() != 3 ||
             solid.getVerticesList().size() != 16 ) {
            return null;
        }
        bounds = solid.getMinMax();
        profile = extractProfileAtX(solid, bounds[0]);
        if ( profile == null || profile.size() < 3 ) {
            profile = extractProfileAtX(solid, bounds[3]);
        }
        if ( profile == null || profile.size() < 3 ) {
            return null;
        }
        return new OrthogonalProfileOperandSpec(PROFILE_X_EXTRUDED_YZ,
            bounds, profile, null, null);
    }

    private static OrthogonalProfileOperandSpec createYExtrudedXZSpec(
        PolyhedralBoundedSolid solid,
        ArrayList<Double> xs,
        ArrayList<Double> ys,
        ArrayList<Double> zs)
    {
        ArrayList<Double> rightZ;
        ArrayList<Double> rightX;
        double[] bounds;
        int i;

        if ( xs.size() != 3 || ys.size() != 2 || zs.size() != 3 ||
             solid.getVerticesList().size() != 10 ) {
            return null;
        }

        bounds = solid.getMinMax();
        rightZ = new ArrayList<Double>();
        rightX = new ArrayList<Double>();
        for ( i = 0; i < zs.size(); i++ ) {
            double z;
            double maxX;
            int j;

            z = zs.get(i);
            maxX = -Double.MAX_VALUE;
            for ( j = 0; j < solid.getVerticesList().size(); j++ ) {
                Vector3D p;

                p = solid.getVerticesList().get(j).position;
                if ( sameCoordinate(p.z(), z) && p.x() > maxX ) {
                    maxX = p.x();
                }
            }
            if ( maxX <= -Double.MAX_VALUE / 2.0 ) {
                return null;
            }
            rightZ.add(z);
            rightX.add(maxX);
        }

        return new OrthogonalProfileOperandSpec(PROFILE_Y_EXTRUDED_XZ,
            bounds, null, rightZ, rightX);
    }

    private static OrthogonalProfileOperandSpec createOrthogonalProfileSpec(
        PolyhedralBoundedSolid solid)
    {
        ArrayList<Double> xs;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        OrthogonalProfileOperandSpec spec;

        xs = uniqueVertexCoordinates(solid, 0);
        ys = uniqueVertexCoordinates(solid, 1);
        zs = uniqueVertexCoordinates(solid, 2);

        spec = createYExtrudedXZSpec(solid, xs, ys, zs);
        if ( spec != null ) {
            return spec;
        }
        return createXExtrudedYZSpec(solid, xs, ys, zs);
    }

    private static OrthogonalProfileBooleanFallbackSpec
    prepareOrthogonalProfileBooleanFallbackSpec(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB)
    {
        OrthogonalProfileOperandSpec specA;
        OrthogonalProfileOperandSpec specB;
        OrthogonalProfileOperandSpec yExtruded;
        OrthogonalProfileOperandSpec xExtruded;
        ArrayList<Double> ys;
        ArrayList<Double> zs;
        int i;

        specA = createOrthogonalProfileSpec(inSolidA);
        specB = createOrthogonalProfileSpec(inSolidB);
        if ( specA == null || specB == null || specA.type == specB.type ) {
            return null;
        }
        yExtruded = specA.type == PROFILE_Y_EXTRUDED_XZ ? specA : specB;
        xExtruded = specA.type == PROFILE_X_EXTRUDED_YZ ? specA : specB;

        if ( !sameCoordinate(yExtruded.bounds[0], xExtruded.bounds[0]) ||
             !sameCoordinate(yExtruded.bounds[1], xExtruded.bounds[1]) ||
             !sameCoordinate(yExtruded.bounds[2], xExtruded.bounds[2]) ||
             !sameCoordinate(yExtruded.bounds[4], xExtruded.bounds[4]) ||
             !sameCoordinate(yExtruded.bounds[5], xExtruded.bounds[5]) ||
             yExtruded.bounds[3] >= xExtruded.bounds[3] -
                 numericContext.bigEpsilon() ) {
            return null;
        }

        ys = uniqueVertexCoordinates(inSolidA, 1);
        zs = uniqueVertexCoordinates(inSolidA, 2);
        for ( i = 0; i < inSolidB.getVerticesList().size(); i++ ) {
            Vector3D p;

            p = inSolidB.getVerticesList().get(i).position;
            addUniqueCoordinate(ys, p.y());
            addUniqueCoordinate(zs, p.z());
        }
        if ( ys.size() < 2 || zs.size() < 2 ||
             ys.size() > 8 || zs.size() > 8 ) {
            return null;
        }

        return new OrthogonalProfileBooleanFallbackSpec(
            specA, specB, yExtruded, xExtruded.bounds[0],
            xExtruded.bounds[3], ys, zs);
    }

    private static boolean profileCellSelected(
        OrthogonalProfileBooleanFallbackSpec spec,
        int op,
        int zone,
        int iy,
        int iz)
    {
        double y;
        double z;
        double x0;
        double x1;
        double x;
        boolean insideA;
        boolean insideB;

        y = (spec.ys.get(iy) + spec.ys.get(iy + 1)) * 0.5;
        z = (spec.zs.get(iz) + spec.zs.get(iz + 1)) * 0.5;
        x0 = spec.xAtBoundary(zone, z);
        x1 = spec.xAtBoundary(zone + 1, z);
        if ( x1 <= x0 + numericContext.bigEpsilon() ) {
            return false;
        }
        x = (x0 + x1) * 0.5;
        insideA = spec.operandA.contains(x, y, z);
        insideB = spec.operandB.contains(x, y, z);
        return axisAlignedCellSelected(insideA, insideB, op);
    }

    private static void addProfileBoundaryQuad(
        ProfileCellBooleanBuilder builder,
        OrthogonalProfileBooleanFallbackSpec spec,
        int zone,
        int iy,
        int iz,
        int side)
    {
        int left;
        int right;

        left = zone;
        right = zone + 1;
        if ( side == 0 ) {
            builder.addQuad(new Vector3D[] {
                spec.point(left, iy, iz), spec.point(left, iy, iz + 1),
                spec.point(left, iy + 1, iz + 1),
                spec.point(left, iy + 1, iz)
            });
        }
        else if ( side == 1 ) {
            builder.addQuad(new Vector3D[] {
                spec.point(right, iy, iz), spec.point(right, iy + 1, iz),
                spec.point(right, iy + 1, iz + 1),
                spec.point(right, iy, iz + 1)
            });
        }
        else if ( side == 2 ) {
            builder.addQuad(new Vector3D[] {
                spec.point(left, iy, iz), spec.point(right, iy, iz),
                spec.point(right, iy, iz + 1),
                spec.point(left, iy, iz + 1)
            });
        }
        else if ( side == 3 ) {
            builder.addQuad(new Vector3D[] {
                spec.point(left, iy + 1, iz),
                spec.point(left, iy + 1, iz + 1),
                spec.point(right, iy + 1, iz + 1),
                spec.point(right, iy + 1, iz)
            });
        }
        else if ( side == 4 ) {
            builder.addQuad(new Vector3D[] {
                spec.point(left, iy, iz), spec.point(left, iy + 1, iz),
                spec.point(right, iy + 1, iz),
                spec.point(right, iy, iz)
            });
        }
        else {
            builder.addQuad(new Vector3D[] {
                spec.point(left, iy, iz + 1),
                spec.point(right, iy, iz + 1),
                spec.point(right, iy + 1, iz + 1),
                spec.point(left, iy + 1, iz + 1)
            });
        }
    }

    private static PolyhedralBoundedSolid buildOrthogonalProfileBooleanFallback(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        OrthogonalProfileBooleanFallbackSpec spec;
        ProfileCellBooleanBuilder builder;
        boolean[][][] occupied;
        int zone;
        int iy;
        int iz;

        spec = prepareOrthogonalProfileBooleanFallbackSpec(inSolidA, inSolidB);
        if ( spec == null ) {
            return null;
        }

        occupied =
            new boolean[2][spec.ys.size() - 1][spec.zs.size() - 1];
        for ( zone = 0; zone < 2; zone++ ) {
            for ( iy = 0; iy < spec.ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < spec.zs.size() - 1; iz++ ) {
                    occupied[zone][iy][iz] =
                        profileCellSelected(spec, op, zone, iy, iz);
                }
            }
        }

        builder = new ProfileCellBooleanBuilder();
        for ( zone = 0; zone < 2; zone++ ) {
            for ( iy = 0; iy < spec.ys.size() - 1; iy++ ) {
                for ( iz = 0; iz < spec.zs.size() - 1; iz++ ) {
                    if ( !occupied[zone][iy][iz] ) {
                        continue;
                    }
                    if ( zone == 0 || !occupied[zone - 1][iy][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 0);
                    }
                    if ( zone == 1 || !occupied[zone + 1][iy][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 1);
                    }
                    if ( iy == 0 || !occupied[zone][iy - 1][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 2);
                    }
                    if ( iy == spec.ys.size() - 2 ||
                         !occupied[zone][iy + 1][iz] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 3);
                    }
                    if ( iz == 0 || !occupied[zone][iy][iz - 1] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 4);
                    }
                    if ( iz == spec.zs.size() - 2 ||
                         !occupied[zone][iy][iz + 1] ) {
                        addProfileBoundaryQuad(builder, spec, zone, iy, iz, 5);
                    }
                }
            }
        }

        return builder.result();
    }

    private static boolean hasDegenerateFace(PolyhedralBoundedSolid solid)
    {
        int i;

        if ( solid == null ) {
            return true;
        }
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face;

            face = solid.getPolygonsList().get(i);
            if ( face.boundariesList.size() < 1 ||
                 face.boundariesList.get(0).halfEdgesList.size() < 3 ||
                 face.getContainingPlane() == null ) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldUseAxisAlignedCellBooleanFallback(
        PolyhedralBoundedSolid fallback,
        PolyhedralBoundedSolid result)
    {
        if ( fallback == null || fallback.getPolygonsList().size() <= 0 ) {
            return false;
        }
        if ( result == null || result.getPolygonsList().size() <= 0 ) {
            return true;
        }
        if ( hasDegenerateFace(result) ) {
            return true;
        }
        return false;
    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op, boolean withDebug)
    {
        return setOp(inSolidA, inSolidB, op, withDebug, true);
    }

    /**
    Following program [MANT1988].15.1.
    */
    public static PolyhedralBoundedSolid setOp(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op,
        boolean withDebug,
        boolean maximizeResultFaces)
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
        _PolyhedralBoundedSolidProfileDifferenceFallbackSpec
            profileDifferenceFallback;
        PolyhedralBoundedSolid axisAlignedCellBooleanFallback;
        PolyhedralBoundedSolid orthogonalProfileBooleanFallback;

        sonea = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        soneb = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();

        //-----------------------------------------------------------------
        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage00");
            debugSolid(inSolidB, "outputB_stage00");
        }

        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidB);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.maximizeFaces(inSolidB);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(inSolidB);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidA);
        PolyhedralBoundedSolidTopologyEditing.compactIds(inSolidB);
        updmaxnames(inSolidB, inSolidA);
        setNumericContext(
            PolyhedralBoundedSolidNumericPolicy.forSolids(inSolidA, inSolidB));
        _PolyhedralBoundedSolidSetOperatorNullEdge.setNumericContext(
            numericContext);
        profileDifferenceFallback = prepareProfileDifferenceFallbackSpec(
            inSolidA, inSolidB, op);
        axisAlignedCellBooleanFallback =
            buildAxisAlignedCellBooleanFallback(inSolidA, inSolidB, op);
        orthogonalProfileBooleanFallback =
            buildOrthogonalProfileBooleanFallback(inSolidA, inSolidB, op);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage01");
            debugSolid(inSolidB, "outputB_stage01");
        }

        PolyhedralBoundedSolid coplanarAreaContactResult =
            _PolyhedralBoundedSolidSetNonIntersectingClassifier
                .runPartialCoplanarFaceAreaCase(inSolidA, inSolidB, res, op);
        if ( coplanarAreaContactResult != null ) {
            res = coplanarAreaContactResult;
            if ( res.getPolygonsList().size() > 0 ) {
                postProcessResult(res, maximizeResultFaces);
            }
            return res;
        }

        if ( isTouchingOnlyPreflightCase(inSolidA, inSolidB) ) {
            res = setOpNoIntersectionCase(inSolidA, inSolidB, res, op);
            if ( res.getPolygonsList().size() > 0 ) {
                postProcessResult(res, maximizeResultFaces);
            }
            return res;
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
            if ( res.getPolygonsList().size() > 0 ) {
                postProcessResult(res, maximizeResultFaces);
            }
            return res;
        }

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage04");
            debugSolid(inSolidB, "outputB_stage04");
        }

        setOpConnect(op);

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage05");
            debugSolid(inSolidB, "outputB_stage05");
        }

        if ( axisAlignedCellBooleanFallback != null &&
             (_PolyhedralBoundedSolidSetNullEdgesConnector
                  .getLastLooseACount() > 0 ||
              _PolyhedralBoundedSolidSetNullEdgesConnector
                  .getLastLooseBCount() > 0) ) {
            tracePipelineSummary(
                "axis-aligned cell fallback replacing incomplete connect");
            res = axisAlignedCellBooleanFallback;
            axisAlignedCellBooleanFallback = null;
        }
        else if ( orthogonalProfileBooleanFallback != null &&
                  (_PolyhedralBoundedSolidSetNullEdgesConnector
                       .getLastLooseACount() > 0 ||
                   _PolyhedralBoundedSolidSetNullEdgesConnector
                       .getLastLooseBCount() > 0) ) {
            tracePipelineSummary(
                "orthogonal profile fallback replacing incomplete connect");
            res = orthogonalProfileBooleanFallback;
            orthogonalProfileBooleanFallback = null;
        }
        else {
            try {
                setOpFinish(inSolidA, inSolidB, res, op);
            }
            catch ( RuntimeException e ) {
                if ( axisAlignedCellBooleanFallback == null &&
                     orthogonalProfileBooleanFallback == null ) {
                    throw e;
                }
                if ( axisAlignedCellBooleanFallback != null ) {
                    tracePipelineSummary(
                        "axis-aligned cell fallback replacing finish exception: " +
                        e.getClass().getSimpleName());
                    res = axisAlignedCellBooleanFallback;
                    axisAlignedCellBooleanFallback = null;
                }
                else {
                    tracePipelineSummary(
                        "orthogonal profile fallback replacing finish exception: " +
                        e.getClass().getSimpleName());
                    res = orthogonalProfileBooleanFallback;
                    orthogonalProfileBooleanFallback = null;
                }
            }
        }

        if ( withDebug ) {
            debugSolid(inSolidA, "outputA_stage06");
            debugSolid(inSolidB, "outputB_stage06");
            debugSolid(res, "outputR_stage06");
        }

        if ( shouldUseAxisAlignedCellBooleanFallback(
                 axisAlignedCellBooleanFallback, res) ) {
            tracePipelineSummary(
                "axis-aligned cell fallback replacing incomplete result");
            res = axisAlignedCellBooleanFallback;
        }
        if ( shouldUseAxisAlignedCellBooleanFallback(
                 orthogonalProfileBooleanFallback, res) ) {
            tracePipelineSummary(
                "orthogonal profile fallback replacing incomplete result");
            res = orthogonalProfileBooleanFallback;
        }

        res = applyProfileDifferenceFallbackIfNeeded(
            profileDifferenceFallback, res);
        postProcessResult(res, maximizeResultFaces);

        if ( withDebug ) {
            debugSolid(res, "outputR_stage07");
        }

        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("= [END OF SETOP REPORT] ===================================================================================================================================");
        }

        return res;
    }
}
