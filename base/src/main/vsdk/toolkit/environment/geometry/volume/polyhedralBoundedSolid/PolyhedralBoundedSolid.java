//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;
import java.io.Serial;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Solid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
This class encapsulates a polyhedral boundary representation for 2-manifold
solids, as presented in [MANT1988].

As noted in [MANT1988].6.2.1., a "polyhedral model" is a boundary model
that has only planar faces. So, the name of this class `PolyhedralBoundedSolid`
implies that its faces should be planar. However, some intermediate steps
in complex algorithms such as the splitter and the set operators, permits
the use of "special" non planar faces for "gluing".  Check [MANT1988] book
for more details on that.

As noted in [MANT1988].10.2.1, current implementation of the
`PolyhedralBoundedSolid` class uses a five-level hierarchical data
structure, consisting of:
  - PolyhedralBoundedSolid
  - _PolyhedralBoundedSolidFace
  - _PolyhedralBoundedSolidLoop
  - _PolyhedralBoundedSolidHalfEdge (and _PolyhedralBoundedSolidEdge)
  - _PolyhedralBoundedSolidVertex
Current class forms the root element (Facade) that gives access to faces,
edges and vertices of the model through agregations in
CircularDoubleLinkedList's.

Note that this is a quite complex datastructure. It implementation follows
the strategies outlined on book [MANT1988]. For the sake of clarity, it
was decided to keep most of its internal datastructures public, breaking
so the encapsulation concept. Note that if internal data structures are
made private and a accesing get/set methods are provided for them, then
the complexity of algorithms using current datastructure should become
unmanageable, both in terms of code verbosity and bad performance (time
complexity) due to extra calls to a lot of simple methods.
*/
public class PolyhedralBoundedSolid extends Solid {
    @Serial private static final long serialVersionUID = 20061118L;

    public static final int PLUS = 1;
    public static final int MINUS = 0;

    //= Main boundary representation solid data structure =============
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidFace> polygonsList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge> edgesList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex> verticesList;
    // Auxiliary data structures for storage of parcial results and 
    // preprocessing
    private double[] minMax;
    public int maxVertexId;
    public int maxFaceId;
    private boolean modelIsValid;

    //=================================================================
    public PolyhedralBoundedSolid()
    {
        polygonsList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidFace>();
        edgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>();
        verticesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>();
        minMax = null;
        maxVertexId = -1;
        maxFaceId = -1;
        modelIsValid = false;
    }

    //= SUPPORT MACROS FOR BASIC DATASTRUCTURE MANIPULATION ===========

    /**
    Find the face identified with `id`. Returns null if face not found,
    or current founded face otherwise.
    Build based over function `fface` in program [MANT1988].11.9.
    @param id
    @return the id face
    */
    public _PolyhedralBoundedSolidFace
    findFace(int id)
    {
        int i;
        _PolyhedralBoundedSolidFace facei;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            facei = polygonsList.get(i);
            if ( facei.id == id ) {
                return facei;
            }
        }
        return null;
    }

    public _PolyhedralBoundedSolidVertex
    findVertex(int id)
    {
        int i;
        _PolyhedralBoundedSolidVertex v;

        for ( i = 0; i < verticesList.size(); i++ ) {
            v = verticesList.get(i);
            if ( v.id == id ) {
                return v;
            }
        }
        return null;
    }

    /**
    addhe: addHalfEdge.
    As described in section [MANT1988].11.2.2 and following the structure
    of sample program [MANT1988].11.3, there exist the need for a halfedge
    procedure creation where some special cases should be considered.
    */
    private _PolyhedralBoundedSolidHalfEdge addhe(
        _PolyhedralBoundedSolidEdge e,
        _PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidHalfEdge where,
        int sign
    )
    {
        _PolyhedralBoundedSolidHalfEdge he;

        if ( where == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "addhe",
            "Trying to build a halfedge from another, non-existing halfedge!");
        }
        if ( e == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "addhe",
            "Trying to associate a halfedge to a non-existing edge!");
        }

        if ( where != null && where.parentEdge == null ) {
            he = where;
          }
          else {
            he =
                new _PolyhedralBoundedSolidHalfEdge(v, where.parentLoop, this);
            where.parentLoop.halfEdgesList.insertBefore(he, where);
            he.startingVertex = v;
        }
        he.parentEdge = e;
        he.parentLoop = where.parentLoop;

        if ( sign == PolyhedralBoundedSolid.PLUS ) {
            e.leftHalf = he;
          }
          else {
            e.rightHalf = he;
        }

        return he;
    }

    //= LOW LEVEL EULER OPERATIONS ====================================

    /**
    mvfs: MakeVertexFaceSolid.
    Operator mvfs creates a new solid representation that consist of
    one face and one vertex with coordinates specified in `p`. This
    operator has one single level of implementation (no "low level"
    or "high level versions") as other operator has.

    As described in sections [MANT1988].9.2.2, [MANT1988].11.3.1 and
    [MANT1988].11.5.1; and following the structure of sample program
    [MANT1988].11.5, this method should be used as part of every
    PolyhedralBoundedSolid constructor process, to yield to an empty
    skeletal boundary representation solid.

    Note that all correctly builded solids are the result of a series of
    Euler operations over this generated skeleton, the "single skeletal
    plane model" ([MANT1988].9.2.2). The "solid" created here may not
    satisfy the intuitive notion of a solid object. Nevertheless, it is
    useful as the initial state of creating a boundary model with a sequence
    of Euler operations.
    @param p
    @param vertexId
    @param faceId
    */
    public void mvfs(Vector3D p, int vertexId, int faceId)
    {
        _PolyhedralBoundedSolidFace newFace;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidHalfEdge newHalfEdge;
        _PolyhedralBoundedSolidVertex newVertex;

        if ( vertexId > maxVertexId ) maxVertexId = vertexId;
        if ( faceId > maxFaceId ) maxFaceId = faceId;

        minMax = null;
        newFace = new _PolyhedralBoundedSolidFace(this, faceId);
        newLoop = new _PolyhedralBoundedSolidLoop(newFace);
        newVertex = new _PolyhedralBoundedSolidVertex(this, p, vertexId);
        newHalfEdge =
            new _PolyhedralBoundedSolidHalfEdge(newVertex, newLoop, this);
        newLoop.halfEdgesList.add(newHalfEdge);
        newLoop.boundaryStartHalfEdge = newHalfEdge;
    }

    /**
    kvfs: KillVertexFaceSolid.
    Operator kvfs is the inverse of mvfs, and removes the contents of
    current solid, given that it is the skeletal solid. The solid must
    consist of a single face and vertex only.

    As described in sections [MANT1988].9.2.2 and [MANT1988].11.5.1 method
    can be used as part of final PolyhedralBoundedSolid destructor process,
    starting from an empty skeletal boundary representation solid.

    Note that all correctly builded solids can be destroyed with a series of
    Euler operations over yielding to the "single skeletal plane model" 
    [MANT1988].9.2.2.
    */
    public void kvfs()
    {
        if ( polygonsList.size() != 1 ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "kvfs",
            "Not skeletal solid, not having exactly one loop!");
            return;
        }
        if ( edgesList.size() != 0 ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "kvfs",
            "Not skeletal solid, having some edges!");
            return;
        }
        if ( verticesList.size() != 1 ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "kvfs",
            "Not skeletal solid, not having exactly one vertex !");
            return;
        }
        polygonsList.remove(0);
        verticesList.remove(0);
    }

    /**
    lmev: LowlevelMakeEdgeVertex (vertex splitting operation).
    Operator lmev "splits" the vertex pointed at by `he1` and `he2`,
    and adds a new vertex and new edge between the resulting two vertices.
    The coordinates specified by `p` are assigned to the new vertex position.
    If `he1` and `he2` are the same halfedge, the new vertex and edge are
    added into the face of `he1`. The new edge is oriented from the new
    vertex to the old one.

    As described in sections [MANT1988].9.2.3, [MANT1988].11.3.2 and
    [MANT1988].11.5.1; and following the structure of sample program
    [MANT1988].11.6, this method has the effect of adding one new vertex
    and one new edge to the solid model.
    @param he1
    @param he2
    @param vertexID
    @param p
    */
    public void lmev(_PolyhedralBoundedSolidHalfEdge he1,
                     _PolyhedralBoundedSolidHalfEdge he2,
                     int vertexID, Vector3D p)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidVertex newVertex;
        _PolyhedralBoundedSolidEdge newEdge;
        boolean strutCase = false;

        if ( he1 == he2 ) {
            strutCase = true;
        }

        if ( he1 == null && he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lmev",
            "Calling with (both) empty halfedges!");
            return;
        }

        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lmev",
            "Calling with (first) empty halfedge!");
            return;
        }

        if ( he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lmev",
            "Calling with (second) empty halfedge!");
            return;
        }

        if ( he1.startingVertex != he2.startingVertex ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lmev",
            "Halfedges not starting at the same vertex. Not supported case!");
            return;
        }

        if ( vertexID > maxVertexId ) maxVertexId = vertexID;

        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);
        newVertex = new _PolyhedralBoundedSolidVertex(he1.parentLoop.parentFace.parentSolid, p, vertexID);
        minMax = null;

        //-----------------------------------------------------------------
        he = he1;
        while ( he != he2 ) {
            he.startingVertex = newVertex;
            he = (he.mirrorHalfEdge()).next();
        }

        //-----------------------------------------------------------------
        // Original [MANT1988] code...
        //addhe(newEdge, newVertex, he2, PLUS);
        //addhe(newEdge, he2.startingVertex, he1, MINUS);

        // Modified code...
        _PolyhedralBoundedSolidVertex oldVertex = he2.startingVertex;
        if ( strutCase ) {
            addhe(newEdge, oldVertex, he2, PLUS);
            addhe(newEdge, newVertex, he1, MINUS);
        }
        else {
            addhe(newEdge, newVertex, he2, PLUS);
            addhe(newEdge, he2.startingVertex, he1, MINUS);
        }

        //-----------------------------------------------------------------
        newVertex.emanatingHalfEdge = he2.previous();
        he2.startingVertex.emanatingHalfEdge = he2;
    }

    /**
    lkev: LowlevelKillEdgeVertex (vertex joining operation).
    Operator lkev is the inverse of lmev. It removes the edge pointed at
    by `he1` and `he2`, and "joins" the two vertices `he1.startingVertex`
    and `he2.startingVertex` which must be distinct (but can, of course,
    correspond with geometrically identical points). Vertex
    `he1->startingVertex` is removed.

    As described in sections [MANT1988].9.2.3, [MANT1988].11.3.4 and
    [MANT1988].11.5.1 this method has the effect of removing one vertex
    and one edge from the solid model.

    Current implementation is not explained on [MANT1988], but leaved as
    problem [MANT1988].11.3.
    @param he1
    @param he2
    */
    public void lkev(_PolyhedralBoundedSolidHalfEdge he1,
                     _PolyhedralBoundedSolidHalfEdge he2)
    {
        //-----------------------------------------------------------------
        if ( he1 == null || he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lkev",
            "Two halfedges are needed for this Euler operator two work!");
            return;
        }

        if ( he1.parentEdge != he2.parentEdge ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lkev",
            "Given halfedges must lie over the same edge!");
            return;
        }        

        if ( he1 == he2 ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lkev",
            "Given halfedges must be different!");
            return;
        }        

        //-----------------------------------------------------------------
        // Answer borrowed from [.wMANT2008]
        _PolyhedralBoundedSolidHalfEdge he, he2next;

        he = he2.next();
        while ( he != he1 ) {
            he.startingVertex = he2.startingVertex;
            he = he.mirrorHalfEdge().next();
        }

        he2next = he2.next();
        he1.parentLoop.unlistHalfEdge(he1);
        he2.parentLoop.unlistHalfEdge(he2);
        he2.startingVertex.emanatingHalfEdge = he2next;
        if ( he2.parentLoop.halfEdgesList.size() < 1 ) {
            he2.startingVertex.emanatingHalfEdge = null;
        }

        edgesList.locateWindowAtElem(he1.parentEdge);
        edgesList.removeElemAtWindow();
        verticesList.locateWindowAtElem(he1.startingVertex);
        verticesList.removeElemAtWindow();

        if ( he2.parentLoop.halfEdgesList.size() <= 0 ) {
            he2.parentEdge = null;
            he2.parentLoop.halfEdgesList.add(he2);
        }
    }

    /**
    lkef: Low Level Kill Edge Face
    Operator `lkef` is the inverse of `lmef`. It removes the edge of
    `he1` and `he2`, and "joins" the two adjacent faces by merging
    their loops. The face `he2.parentLoop.parentFace` is removed.
    `lkef` is applicable to the halves of an edge that occurs in two
    distinct faces. That is, it is assumed that:
      - `he1.parentEdge` == `he2.parentEdge`
      - `he1.parentLoop.parentFace` != `he2.parentLoop.parentFace`
    @param he1
    @param he2
    */
    public void lkef(_PolyhedralBoundedSolidHalfEdge he1,
                     _PolyhedralBoundedSolidHalfEdge he2)
    {
        if ( he1.parentEdge != he2.parentEdge ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lkef",
            "Given halfedges must lie over the same edge. Operation aborted.");
            return;
        }
        if ( he1.parentLoop.parentFace == he2.parentLoop.parentFace ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lkef",
            "Given halfedges must belong to different faces. Operation aborted.");
            return;
        }

        _PolyhedralBoundedSolidEdge edgeToBeKilled;
        _PolyhedralBoundedSolidLoop loopToBeKilled;
        _PolyhedralBoundedSolidFace faceToBeKilled;
        _PolyhedralBoundedSolidHalfEdge hepivot;

        hepivot = he1.next();
        edgeToBeKilled = he1.parentEdge;
        loopToBeKilled = he2.parentLoop;
        faceToBeKilled = loopToBeKilled.parentFace;

        ArrayList<_PolyhedralBoundedSolidHalfEdge> migratedHalfEdges;
        migratedHalfEdges = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        _PolyhedralBoundedSolidHalfEdge he;
        he = he2.next();
        int maxTraversal = he2.parentLoop.halfEdgesList.size() + 1;
        int traversed = 0;
        while ( he != he2 ) {
            if ( he == null || traversed > maxTraversal ) {
                VSDK.reportMessage(this, VSDK.WARNING, "lkef",
                    "Detected inconsistent next-chain; falling back to halfEdgesList order.");
                migratedHalfEdges.clear();
                int idx;
                for ( idx = 0; idx < he2.parentLoop.halfEdgesList.size(); idx++ ) {
                    _PolyhedralBoundedSolidHalfEdge candidate;
                    candidate = he2.parentLoop.halfEdgesList.get(idx);
                    if ( candidate != he2 ) {
                        migratedHalfEdges.add(candidate);
                    }
                }
                break;
            }
            migratedHalfEdges.add(he);
            he = he.next();
            traversed++;
        }

        he1.parentLoop.unlistHalfEdge(he1);
        he2.parentLoop.unlistHalfEdge(he2);
        edgesList.locateWindowAtElem(edgeToBeKilled);
        edgesList.removeElemAtWindow();

        faceToBeKilled.boundariesList.locateWindowAtElem(loopToBeKilled);
        faceToBeKilled.boundariesList.removeElemAtWindow();
        polygonsList.locateWindowAtElem(faceToBeKilled);
        polygonsList.removeElemAtWindow();

        int i;
        for ( i = migratedHalfEdges.size()-1; i >= 0; i-- ) {
            he = migratedHalfEdges.get(i);
            he.parentLoop = he1.parentLoop;
            he1.parentLoop.halfEdgesList.insertBefore(he, hepivot);
            hepivot = he;
        }
    }

    /**
    lmef: LowlevelMakeEdgeFace (face splitting operator).
    Operator lmef adds a new edge between `he1.startingVertex` and
    `he2.startingVertex`, and "splits" their common face into two faces
    such that `he1` will occur in the new face `newFaceId`, and `he2` 
    remains in the old face. The new edge is oriented from 
    `he1.startingVertex` to `he2.startingVertex`.
    Halfedges `he1` and `he2` must belong to the same loop (i.e.
    he1.parentLoop == he2.parentLoop ). They may be equal, in which case
    a "circular" face with just one edge is created. A pointer to the new
    face is returned.

    As described in sections [MANT1988].9.2.3, [MANT1988].11.3.3 and
    [MANT1988].11.5.1; and following the structure of sample program
    [MANT1988].11.7, this method is the dual to lmev. Note that creates
    the halfedges as usual, and then swaps them.
    @param he1
    @param he2
    @param newFaceId
    @return the newly generated face
    */
    public _PolyhedralBoundedSolidFace lmef(
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2,
         int newFaceId)
    {
        _PolyhedralBoundedSolidFace newFace;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidLoop oldLoop;
        _PolyhedralBoundedSolidEdge newEdge;
        _PolyhedralBoundedSolidHalfEdge he, nhe1, nhe2, temp;

        if ( newFaceId > maxFaceId ) maxFaceId = newFaceId;

        newFace = new _PolyhedralBoundedSolidFace(he1.parentLoop.parentFace.parentSolid, newFaceId);
        oldLoop = he1.parentLoop;
        newLoop = new _PolyhedralBoundedSolidLoop(newFace);
        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);

        ArrayList<_PolyhedralBoundedSolidHalfEdge> migratedHalfEdges;
        migratedHalfEdges = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        he = he1;
        while ( he != he2 ) {
            migratedHalfEdges.add(he);
            he = he.next();
            if ( he == he1 ) break;
        }

        int i;
        for ( i = 0; i < migratedHalfEdges.size(); i++ ) {
            he = migratedHalfEdges.get(i);
            he.parentLoop = newLoop;
            oldLoop.unlistHalfEdge(he);
            newLoop.halfEdgesList.add(he);
        }

        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lmef",
            "Non-existing halfedge 1!");
        }
        if ( he2 == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lmef",
            "Non-existing halfedge 2!");
        }

        nhe1 = addhe(newEdge, he2.startingVertex, he1, MINUS);
        nhe2 = addhe(newEdge, he1.startingVertex, he2, PLUS);

        newLoop.boundaryStartHalfEdge = nhe1;
        he2.parentLoop.boundaryStartHalfEdge = nhe2;

        return newFace;
    }

    /**
    lkemr: LowlevelKillEdgeMakeRing (loop splitting operator).
    Operator lkemr removes the edge of `he1` and `he2`, and divides their
    common loop into two components (i.e., it creates a new "ring").  If
    the original loop was "outer", the component of `he1.startingVertex`
    becomes the new "outer" loop. (If this default is inappropiate, you
    can simply swap the arguments of lkemr, or use lringmv to make the
    desired loop "outer").  It is assumed that
    he1.parentEdge == he2.parentEdge and 
    he1.parentLoop == he2.parentLoop.

    As described in section [MANT1988].9.2.3, the operator splits a loop
    into two new ones by removing and edge that appears twice in it. Hence
    the operator divides a connected bounding curve of a face into two
    bounding curves, and has the net effect of removing one edge and adding
    one ring to the PolyhedralBoundedSolid data structure. The special
    cases that one or both of the resulting loops are empty are also
    included. Note that current code follows section [MANT1988].11.3.4
    and the structure of sample program [MANT1988].11.8.
    @param he1
    @param he2
    */
    public void lkemr(
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge he3;
        _PolyhedralBoundedSolidHalfEdge he4;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidLoop oldLoop;
        _PolyhedralBoundedSolidEdge killedEdge;

        oldLoop = he1.parentLoop;
        newLoop = new _PolyhedralBoundedSolidLoop(oldLoop.parentFace);
        killedEdge = he1.parentEdge;

        //-----------------------------------------------------------------
        ArrayList<_PolyhedralBoundedSolidHalfEdge> migratedHalfEdges;
        migratedHalfEdges = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        he4 = he1.next();
        do {
            migratedHalfEdges.add(he4);
            if ( he4 == he2 ) break;
        } while ( (he4 = he4.next()) != he2 );

        //-----------------------------------------------------------------
        int i;

        for ( i = 0; i < migratedHalfEdges.size(); i++ ) {
            he3 = migratedHalfEdges.get(i);
            oldLoop.halfEdgesList.locateWindowAtElem(he3);
            oldLoop.halfEdgesList.removeElemAtWindow();
            newLoop.halfEdgesList.add(he3);
            he3.parentLoop = newLoop;
        }
        newLoop.boundaryStartHalfEdge = newLoop.halfEdgesList.get(0);

        //-----------------------------------------------------------------
        oldLoop.delhe(he1);
        oldLoop.delhe(he2);

        if ( newLoop.halfEdgesList.size() <= 1 ) {
            newLoop.boundaryStartHalfEdge.parentEdge = null;
            if ( newLoop.halfEdgesList.size() <= 0 ) {
                VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lkemr",
                "Case A Should not happen!");
            }
            newLoop.halfEdgesList.get(0).startingVertex.emanatingHalfEdge = null;
        }

        if ( oldLoop.halfEdgesList.size() <= 1 ) {
            oldLoop.boundaryStartHalfEdge.parentEdge = null;
            if ( oldLoop.halfEdgesList.size() <= 0 ) {
                VSDK.reportMessage(this, VSDK.FATAL_ERROR, "lkemr",
                "Case B Should not happen!");
            }
            oldLoop.halfEdgesList.get(0).startingVertex.emanatingHalfEdge = null;
        }

        edgesList.locateWindowAtElem(killedEdge);
        edgesList.removeElemAtWindow();
    }

    /**
    lkfmrh: LowlevelKillFaceMakeRingHoleinSameShell.
    Operator `lkfmrh` merges two faces `face1` and `face2` by making
    the loop of the latter a ring into the former. Face `face2` is hence
    removed.
    PRE: It is assumed that `face2` is simple, i.e., has just one loop.
    This method implements the low-level operator family from exercise
    [MANT1988].11.5, following the functional definition of section
    [MANT1988].11.5.1 for the same-shell case.
    @param face1
    @param face2
    */
    public void lkfmrh(
        _PolyhedralBoundedSolidFace face1,
        _PolyhedralBoundedSolidFace face2)
    {
        if ( face2.boundariesList.size() > 1 ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lkfmrh",
                "Internal face to form new loop must have just one boundary!");
            return;
        }

        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidLoop oldLoop;
        _PolyhedralBoundedSolidHalfEdge he;
        int i;

        oldLoop = face2.boundariesList.get(0);
        newLoop = new _PolyhedralBoundedSolidLoop(face1);

        for ( i = 0; i < oldLoop.halfEdgesList.size(); i++ ) {
            he = oldLoop.halfEdgesList.get(i);
            he.parentLoop = newLoop;
            newLoop.halfEdgesList.add(he);
        }
        newLoop.boundaryStartHalfEdge = newLoop.halfEdgesList.get(0);

        polygonsList.locateWindowAtElem(face2);
        polygonsList.removeElemAtWindow();
    }

    /**
    lmfkrh: LowLevelMakeFaceKillRingHole.
    Operator `lmfkrh` is the inverse of `lkfmrh`. It makes the loop `l`
    the outer loop of a new face `newFaceId`. It is assumed that `l` is an
    inner loop of its parent face.
    This method implements the inverse operator of the low-level family from
    exercise [MANT1988].11.5, following section [MANT1988].11.5.1.
    @param l
    @param newFaceId
    @return the newly generated face
    */
    public _PolyhedralBoundedSolidFace lmfkrh(_PolyhedralBoundedSolidLoop l, int newFaceId)
    {
        _PolyhedralBoundedSolidFace newFace;
        newFace = new _PolyhedralBoundedSolidFace(this, newFaceId);

        if ( newFaceId > maxFaceId ) maxFaceId = newFaceId;

        l.parentFace.boundariesList.locateWindowAtElem(l);
        l.parentFace.boundariesList.removeElemAtWindow();
        l.parentFace = newFace;
        newFace.boundariesList.add(l);

        return newFace;
    }

    /**
    lkimrh: LowLevelKillInnerMakeRingHole.
    Naming alias for the [MANT1988].11.5 low-level operator family.
    Current Vitral codebase uses the `lkfmrh` name for the same semantics.
    @param face1
    @param face2
    */
    public void lkimrh(
        _PolyhedralBoundedSolidFace face1,
        _PolyhedralBoundedSolidFace face2)
    {
        lkfmrh(face1, face2);
    }

    /**
    lmikrh: LowLevelMakeInnerKillRingHole.
    Naming alias for the [MANT1988].11.5 low-level inverse operator family.
    Current Vitral codebase uses the `lmfkrh` name for the same semantics.
    @param l
    @param newFaceId
    @return the newly generated face
    */
    public _PolyhedralBoundedSolidFace
    lmikrh(_PolyhedralBoundedSolidLoop l, int newFaceId)
    {
        return lmfkrh(l, newFaceId);
    }

    /**
    Method `lringmv` moves the loop `l` from its parent face to face `toFace`.
    If `setAsOuterLoop` is false, `l` becomes an "inner" loop of `toFace`.
    Otherwise, `l` is positioned as the outer loop (first boundary) of
    `toFace`.

    If `l.parentFace` == `toFace`, no topological move is performed; only loop
    ordering is adjusted to satisfy the requested inner/outer role.

    Current implementation validates non-null arguments, same-solid movement,
    source face non-emptiness after move, and that an inner loop is not added
    to a face without an outer loop.

    Note that `lringmv` is an addendum to `lmef`, and not an Euler operator.
    It is part of the practical support required by the operator family of
    exercise [MANT1988].11.5, and follows section [MANT1988].11.5.1.
    @param l loop to move or reorder
    @param toFace destination face
    @param setAsOuterLoop true to set `l` as outer loop, false to set it as inner loop
    @return true if move/reorder succeeds (or was already satisfied), false otherwise
    */
    public boolean lringmv(_PolyhedralBoundedSolidLoop l, _PolyhedralBoundedSolidFace toFace, boolean setAsOuterLoop)
    {
        _PolyhedralBoundedSolidFace fromfac;
        boolean sameFace;
        int nloops;

        if ( l == null || toFace == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                "Null input loop or destination face.");
            return false;
        }

        fromfac = l.parentFace;
        if ( fromfac == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                "Given loop does not have a parent face.");
            return false;
        }
        if ( fromfac.parentSolid != toFace.parentSolid ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                "Loop move across different solids is not supported.");
            return false;
        }

        sameFace = (fromfac == toFace);
        nloops = toFace.boundariesList.size();

        //-----------------------------------------------------------------
        // Same-face case: only reorder to satisfy outer/inner intent.
        if ( sameFace ) {
            if ( !toFace.boundariesList.locateWindowAtElem(l) ) {
                VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                    "Given loop was not found in its parent face boundaries.");
                return false;
            }

            if ( setAsOuterLoop ) {
                if ( nloops <= 0 || toFace.boundariesList.get(0) == l ) {
                    return true;
                }
                toFace.boundariesList.removeElemAtWindow();
                toFace.boundariesList.push(l);
                return true;
            }

            // setAsOuterLoop == false
            if ( nloops <= 1 ) {
                VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                    "Cannot mark the only boundary loop as inner.");
                return false;
            }
            if ( toFace.boundariesList.get(0) != l ) {
                return true;
            }
            toFace.boundariesList.removeElemAtWindow();
            toFace.boundariesList.add(l);
            return true;
        }

        //-----------------------------------------------------------------
        // Cross-face case: move loop and place it as outer/inner.
        if ( fromfac.boundariesList.size() <= 1 ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                "Cannot move the only boundary loop of a face.");
            return false;
        }
        if ( !setAsOuterLoop && toFace.boundariesList.size() <= 0 ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                "Cannot insert an inner loop into a face without outer loop.");
            return false;
        }
        if ( !fromfac.boundariesList.locateWindowAtElem(l) ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lringmv",
                "Given loop was not found in source face boundaries.");
            return false;
        }

        fromfac.boundariesList.removeElemAtWindow();
        l.parentFace = toFace;

        if ( setAsOuterLoop ) {
            toFace.boundariesList.push(l);
        }
        else {
            toFace.boundariesList.add(l);
        }
        return true;
    }

    /**
    lmekr: LowlevelMakeEdgeKillRing.    
    Operator `lmekr` is the inverse of `lkemr`. It inserts a new edge
    between the starting vertices of `he1` and `he2`, and merges the
    corresponding loops into one loop (i.e. removes a ring). The
    operator assumes that:
      - `he1.parentLoop` and `he2.parentLoop` are different, i.e.
        `he1` and `he2` belong to two distinct loops.
      - `he1.parentLoop.parentFace` and `he2.parentLoop.parentFace`
        are equal, i.e. they occur in a single face.
    Current implementation was programmed as an answer to exercise
    [MANT1988].11.4, and follows the signature from section
    [MANT1988].11.5.1.
     * @param he1
     * @param he2
    */
    public void lmekr(
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2)
    {
        //-----------------------------------------------------------------
        if ( he1.parentLoop == he2.parentLoop ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lmekr",
            "Given halfedges are on the same loop. Operation aborted.");
            return;
        }
        if ( he1.parentLoop.parentFace != he2.parentLoop.parentFace ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lmekr",
            "Given halfedges are not on the same face. Operation aborted.");
            return;
        }

        //-----------------------------------------------------------------
        ArrayList<_PolyhedralBoundedSolidHalfEdge> migratedHalfEdges;
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidLoop ringToKill;

        migratedHalfEdges = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        ringToKill = he2.parentLoop;

        he = he2;
        do {
            migratedHalfEdges.add(he);
            he = he.next();
        } while ( he != he2 );

        //-----------------------------------------------------------------
        int i;

        for ( i = 0; i < ringToKill.halfEdgesList.size(); i++ ) {
            ringToKill.halfEdgesList.remove(i);
        }
        ringToKill.parentFace.boundariesList.locateWindowAtElem(ringToKill);
        ringToKill.parentFace.boundariesList.removeElemAtWindow();

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidEdge newEdge;
        _PolyhedralBoundedSolidVertex v1;
        _PolyhedralBoundedSolidVertex v2;

        v1 = he1.startingVertex;
        v2 = he2.startingVertex;

        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);

        _PolyhedralBoundedSolidHalfEdge heLast;

        heLast = addhe(newEdge, v2, he1, MINUS);
        newEdge.rightHalf = addhe(newEdge, v1, heLast, MINUS);
        newEdge.leftHalf = heLast;

        // Alas! This rare condition of not adding a migrated half edges list
        // list of size 1 is to avoid adding an 0-length halfedge with no
        // parent edge and no mirror edge when loop is of size 1 vertex.
        for ( i = 0;
              i < migratedHalfEdges.size() && migratedHalfEdges.size() > 1;
              i++ ) {
            he = migratedHalfEdges.get(i);
            he.parentLoop = he1.parentLoop;
            he1.parentLoop.halfEdgesList.insertBefore(he, heLast);
        }
    }

    //= HIGH LEVEL EULER OPERATIONS ===================================

    /**
    smev: "Strut" or line-drawing "Simplified" version of mev operator.
    See mev method for a complete description.
    @param f1 existing face id (counted from 1) in current solid, where
           new edge and vertex will be created
    @param v1 existing vertex id (counted from 1) in current solid that
           will be taken as starting vertex for new edge
    @param v4 vertex id (counted from 1) for new vertex
    @param p coordinates for new vertex
    @return true on operation success, false other way.
    */
    public boolean smev(int f1, int v1, int v4, Vector3D p)
    {
        _PolyhedralBoundedSolidFace oldface1;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Face " + f1 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Edge " + v1 + " - * not found in face " + f1 + ".");
            return false;
        }
        lmev(he1, he1, v4, p);
        return true;
    }

    /**
    mev: (High level version) MakeEdgeVertex (vertex splitting operation).
    Operator `mev` divides the cycle of edges around the vertex `v1` so
    that all edges from `v1` -> `v2` (inclusive) to `v1` -> `v3` (exclusive)
    will become adjacent to a new vertex `v4`.  The vertices `v1` and `v4`
    are joined with a new edge.  Coordinates defined in `p` are assigned
    to `v4`.
    Similarly to the corresponding low level operator `lmev`, various
    special cases are possible.  Of particular use is the "line-drawing"
    case that `f1` = `f2` and `v2` = `v3`.  In this case, a new edge to a
    new vertex will be added within the face. We shall call such "dangling"
    edges <I>struts</I>.  This case occurs so frequently that it is included
    a "convenience" procedure `smev` that performs this operation.  The
    procedure assumes that `v1` appears just once in `f1`; hence, the
    argument `v2` can be left out.
    @param f1
    @param f2
    @param v1
    @param v2
    @param v3
    @param v4
    @param p
    @return true on operation success, false other way.
    */
    public boolean mev(int f1, int f2,
                   int v1, int v2, int v3, int v4, Vector3D p)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Face " + f2 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1, v2);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Edge " + v1 + " - " + v2 + " not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface2.findHalfEdge(v1, v3);
        if ( he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Edge " + v1 + " - " + v3 + " not found in face " + f2 + ".");
            return false;
        }
        lmev(he1, he2, v4, p);
        return true;
    }

    /**
    smef: simplified version of mef operator.
    See mef method for a complete description.
    @param f1
    @param v1
    @param v3
    @param newfaceid
    @return true on operation success, false other way.
    */
    public boolean smef(int f1, int v1, int v3, int newfaceid)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "smef",
            "Face " + f1 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "smef",
            "Edge " + v1 + " - * not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface1.findHalfEdge(v3);
        if ( he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "smef",
            "Edge " + v3 + " - * not found in face " + f1 + ".");
            return false;
        }
        lmef(he1, he2, newfaceid);
        return true;
    }

    /**
    mef: (High level version) MakeEdgeFace (face splitting operation).
    Operator `mef` connects the vertices `v1` and `v3` of face `f1` with
    a new edge, and creates a new face `f2`. Similarly to method `smev`,
    there is included a convenience procedure `smef` that leaves the arguments
    `v1` and `v4` out; that method should be applied only if `v1` and `v3`
    are known to occur just once in the face.
    Executes a `lmef` in halfedges v1-v2, v3-v4 in respective faces `f1`
    and `f2`, and assigns to the new face the `newfaceid`.
    @param f1
    @param f2
    @param v1
    @param v2
    @param v3
    @param v4
    @param newfaceid
    @return true on operation success, false other way.
    */
    public boolean mef(int f1, int f2,
                       int v1, int v2, int v3, int v4, int newfaceid)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Face " + f2 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1, v2);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Edge " + v1 + " - " + v2 + " not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface2.findHalfEdge(v3, v4);
        if ( he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Edge " + v3 + " - " + v4 + " not found in face " + f2 + ".");
            return false;
        }
        lmef(he1, he2, newfaceid);
        return true;
    }

    /**
    kemr: (High level version) KillEdgeMakeRing (loop splitting operation).
    Executes a `lkemr` in halfedges v1-v2, v3-v4 in respective faces `f1`
    and `f2`.
    @param f1
    @param f2
    @param v1
    @param v2
    @param v3
    @param v4
    @return true on operation success, false other way.
    */
    public boolean kemr(int f1, int f2,
                       int v1, int v2, int v3, int v4)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Face " + f2 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1, v2);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Edge " + v1 + " - " + v2 + " not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface2.findHalfEdge(v3, v4);
        if ( he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Edge " + v3 + " - " + v4 + " not found in face " + f2 + ".");
            return false;
        }
        lkemr(he1, he2);
        return true;
    }

    /**
    kfmrh: (High level version) KillFaceMakeRingHole
    (connected sum topological operation, global manipulation).
    Operator `kfmrhSameShell` "merges" two faces `f1` and `f2` by making
    the latter an interior loop of the former. Face `f2` is removed.
    As noted in section [MANT1988].9.2.4, the name `kfmrh` is actually a
    misnomer, because the operator does not necessarily create a "hole".
    Actualy, `kfmrh` creates a hole only if the two argument faces belong
    to the same shell.  This method implements that case, taking `this`
    solid as the only shell.
    This method is the high-level counterpart used in the [MANT1988].11.5
    operator family implementation for the same-shell case.
    @param f1
    @param f2
    @return true on operation success, false other way.
    */
    public boolean kfmrh(int f1, int f2)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kfmrh",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kfmrh",
            "Face " + f2 + " not found.");
            return false;
        }
        lkfmrh(oldface1, oldface2);
        return true;
    }

    //=================================================================

    /**
    This method gives access to the higher vertex id used in current solid
    model. This method is useful for higher level modeling operations, as
    noted in section [MANT1988].12.2. Current method (and method getMaxFaceId)
    is build after the function `getmaxnames` of program [MANT1988].12.1.
    @return the maximum id used in vertices set
    */
    public int getMaxVertexId()
    {
        return maxVertexId;
    }

    /**
    This method gives access to the higher face id used in current solid
    model. This method is useful for higher level modeling operations, as
    noted in section [MANT1988].12.2. Current method (and method
    getMaxVertexId) is build after the function `getmaxnames` of program
    [MANT1988].12.1.
    @return the maximum id used on faces set
    */
    public int getMaxFaceId()
    {
        return maxFaceId;
    }

    public void applyTransformation(Matrix4x4 T)
    {
        int i;

        minMax = null;
        for ( i = 0; i < verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = verticesList.get(i);
            v.position = T.multiply(v.position);
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay
    @return true if given ray intersects current PolyhedralBoundedSolid
    */
    public Ray doIntersection(Ray inOutRay) {
        RayHit hit = new RayHit(RayHit.DETAIL_NONE, true);
        if ( doIntersection(inOutRay, hit) ) {
            return hit.ray();
        }
        return null;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        synchronized ( this ) {
            int i;
            double min_t;         // Shortest distance founded so far
            PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
                PolyhedralBoundedSolidNumericPolicy.forSolid(this);

            // Initialization values for search algorithm
            min_t = Double.MAX_VALUE;
            RayHit bestInfo = null;
            Vector3D p;
            int pos;

            for ( i = 0; i < polygonsList.size(); i++ ) {
                Ray ray = new Ray(inRay);
                _PolyhedralBoundedSolidFace face = polygonsList.get(i);
                if ( face.containingPlane == null ) {
                    face.calculatePlane();
                    if ( face.containingPlane == null ) {
                        continue;
                    }
                }
                RayHit planeHit = new RayHit();
                if ( face.containingPlane.doIntersection(ray, planeHit) ) {
                    Ray hit = planeHit.ray();
                    if ( hit.t() < min_t ) {
                        hit = hit.withDirection(hit.direction().normalized());
                        p = hit.origin().add(hit.direction().multiply(hit.t()));
                        synchronized ( face ) {
                            pos = face.testPointInside(p, numericContext.bigEpsilon());
                        }
                        if ( pos == Geometry.INSIDE || pos == Geometry.LIMIT ) {
                            min_t = hit.t();
                            bestInfo = new RayHit(planeHit);
                        }
                    }
                }
            }

            if ( bestInfo == null ) {
                return false;
            }
            if ( outHit != null ) {
                outHit.clone(bestInfo);
                outHit.setRay(inRay.withT(min_t));
            }
            return true;
        }
    }

    public void doExtraInformation(Ray inRay, double inT, 
                                  RayHit outData) {
        if ( outData == null ) {
            return;
        }
        doIntersection(inRay.withT(inT), outData);
    }

    /** Needed for supplying the Geometry.getMinMax operation */
    private void calculateMinMaxPositions() {
        if ( minMax == null ) {
            minMax = new double[6];

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            int i;

            for ( i = 0; i < verticesList.size(); i++ ) {
                _PolyhedralBoundedSolidVertex v;
                v = verticesList.get(i);
                double x = v.position.x();
                double y = v.position.y();
                double z = v.position.z();

                if ( x < minX ) minX = x;
                if ( y < minY ) minY = y;
                if ( z < minZ ) minZ = z;
                if ( x > maxX ) maxX = x;
                if ( y > maxY ) maxY = y;
                if ( z > maxZ ) maxZ = z;
            }
            minMax[0] = minX;
            minMax[1] = minY;
            minMax[2] = minZ;
            minMax[3] = maxX;
            minMax[4] = maxY;
            minMax[5] = maxZ;
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax() {
        if ( minMax == null ) {
            calculateMinMaxPositions();
        }
        return minMax;
    }

    /**
    Returns true if the model was validated using
    `PolyhedralBoundedSolidValidationEngine.validateIntermediate` or
    `PolyhedralBoundedSolidValidationEngine.validateStrict`, and validation
    succeeded after the latest geometrical or topological operation.
    @return true if solid model is valid, false if not
    */
    public boolean isValid()
    {
        return modelIsValid;
    }

    void setValidationState(boolean flag)
    {
        modelIsValid = flag;
    }

    private void remakeLoopBoundaryStartHalfEdgesReferences()
    {
        int i;
        int j;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                loop.parentFace = face;
                if ( loop.halfEdgesList.size() > 0 ) {
                    loop.boundaryStartHalfEdge = loop.halfEdgesList.get(0);
                }
                else {
                    loop.boundaryStartHalfEdge = null;
                }
            }
        }
    }

    /**
    After section [MANT1988].12.4.2 and program [MANT1988].12.9.
    @param faceid
    */
    public void loopGlue(int faceid)
    {
        _PolyhedralBoundedSolidFace face;

        face = findFace(faceid);
        if ( face == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "loopGlue",
                "Face " + faceid + " not found.");
            return;
        }
        loopGlue(face);
    }

    public void loopGlue(_PolyhedralBoundedSolidFace face)
    {
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(this);

        if ( face == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "loopGlue",
                "Null face received.");
            return;
        }
        if ( face.boundariesList.size() < 2 ) {
            VSDK.reportMessage(this, VSDK.WARNING, "loopGlue",
                "Face " + face.id + " does not contain at least two loops.");
            return;
        }

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge h1, h2, h1next;

        h1 = face.boundariesList.get(0).boundaryStartHalfEdge;
        h2 = face.boundariesList.get(1).boundaryStartHalfEdge;
        if ( h1 == null || h2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "loopGlue",
                "Missing loop boundary start halfedge.");
            return;
        }

        _PolyhedralBoundedSolidHalfEdge h2start = h2;
        while ( !h1.vertexPositionMatch(h2, numericContext.bigEpsilon()) ) {
            h2 = h2.next();
            if ( h2 == h2start ) {
                VSDK.reportMessage(this, VSDK.WARNING, "loopGlue",
                    "No matching starting vertex found between candidate loops.");
                return;
            }
        }

        lmekr(h1, h2);
        lkev(h1.previous(), h2.previous());

        while ( h1.next() != h2 ) {
            h1next = h1.next();
            lmef(h1.next(), h1.previous(), maxFaceId+1);
            lkev(h1.next(), (h1.next()).mirrorHalfEdge());
            lkef(h1.mirrorHalfEdge(), h1);
            h1 = h1next;
        }
        lkef(h1.mirrorHalfEdge(), h1);
        remakeLoopBoundaryStartHalfEdgesReferences();
    }

    /**
    Given `this` and `other` solids, this method erases the `other` solid
    while appending its parts to current one as a new shell. This method
    follows section [MANT1988].12.4.1 and program [MANT1988].12.8.
    @param other
    */
    public void merge(PolyhedralBoundedSolid other)
    {
        //-----------------------------------------------------------------
        int offsetFacesId = getMaxFaceId();
        int offsetVertexId = getMaxVertexId();
        _PolyhedralBoundedSolidFace f;
        _PolyhedralBoundedSolidVertex v;

        //-----------------------------------------------------------------
        while ( other.polygonsList.size() > 0 ) {
            f = other.polygonsList.get(0);
            f.id += offsetFacesId;
            if ( f.id > maxFaceId ) maxFaceId = f.id;
            polygonsList.add(f);
            other.polygonsList.remove(0);
        }
        while ( other.edgesList.size() > 0 ) {
            edgesList.add(other.edgesList.get(0));
            other.edgesList.remove(0);
        }
        while ( other.verticesList.size() > 0 ) {
            v = other.verticesList.get(0);
            v.id += offsetVertexId;
            if ( v.id > maxVertexId ) maxVertexId = v.id;
            verticesList.add(v);
            other.verticesList.remove(0);
        }
    }

    /**
    For all given halfedges, this edge asign valid emanating half edges on
    valid vertexes.

    If all the operations in current implementation are perfectly done,
    this method should not be needed.
    */
    private void remakeEmanatingHalfedgesReferences()
    {
        _PolyhedralBoundedSolidTopologicalValidator
            .remakeEmanatingHalfedgesReferences(this);
    }
    //= TEXTUAL QUERY OPERATIONS ======================================

    private String intPreSpaces(int val, int fieldSize)
    {
        String cad;

        cad = VSDK.formatNumberWithinZeroes(val, 1);

        int remain = fieldSize - cad.length();

        for ( ; remain > 0; remain-- ) {
            cad = " " + cad;
        }
        return cad;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.computeQuantitativeInvisibility.

    This is not well understood for cases of intersection with face limits
    (vertices and edges). In some cases, computation of quantitative
    invisibility seems to be failing.
    \todo  check well all limiting cases.
    @return  the number of front facing surface elements (with
    respect to `origin`) between the `origin` point and the `p` point
    */
    @Override
    public int computeQuantitativeInvisibility(Vector3D origin, Vector3D p)
    {
        int qi = 0;
        int i, j;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(this);
        Vector3D d = p.subtract(origin);
        Vector3D pi;
        double t0 = d.length();
        d = d.normalized();
        int pos;
        double distances[] = new double[polygonsList.size()];
        int ndist = 0;

        Ray ray = new Ray(origin, d);
        RayHit info;
        info = new RayHit();

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            RayHit planeHit = new RayHit();
            if ( face.containingPlane.doIntersection(ray, planeHit) ) {
                Ray hit = planeHit.ray();
                if ( hit.t() < t0 - numericContext.epsilon() ) {
                    hit = hit.withDirection(hit.direction().normalized());
                    pi = hit.origin().add(hit.direction().multiply(hit.t()));
                    pos = face.testPointInside(pi, numericContext.bigEpsilon());
                    if ( pos == Geometry.INSIDE ||
                         (pos == Geometry.LIMIT &&
                          boundaryHitProducesInteriorPenetration(
                              pi, d, numericContext.bigEpsilon())) ) {
                        info = planeHit;
                        if ( info.n.dotProduct(d) < 0.0 ) {
                            boolean considerIt = true;
                            for ( j = 0; j < ndist; j++ ) {
                                if ( Math.abs(distances[j]-hit.t()) <
                                     numericContext.bigEpsilon() ) {
                                    considerIt = false;
                                    break;
                                }
                            }
                            if ( considerIt ) {
                                qi++;
                                distances[ndist] = hit.t();
                                ndist++;
                            }
                        }
                    }
                }
            }
        }

        return qi;
    }

    private boolean boundaryHitProducesInteriorPenetration(
        Vector3D hitPoint,
        Vector3D direction,
        double tolerance)
    {
        Vector3D afterHit = hitPoint.add(direction.multiply(4.0 * tolerance));
        boolean touchesBoundary = false;

        for ( int i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);

            if ( Math.abs(face.containingPlane.pointDistance(hitPoint)) >
                 tolerance ) {
                continue;
            }
            int pointStatus = face.testPointInside(hitPoint, tolerance);
            if ( pointStatus == Geometry.OUTSIDE ) {
                continue;
            }

            touchesBoundary = true;
            int halfSpaceStatus = face.containingPlane.doContainmentTestHalfSpace(
                afterHit, tolerance);
            if ( halfSpaceStatus != Geometry.INSIDE ) {
                return false;
            }
        }
        return touchesBoundary;
    }

    /**
    Utility routine used to compare floating values inside the boundary
    representation winged edge data structure, following procedure `comp`
    from program [MANT1988].13.2.
    @param a
    @param b
    @param tolerance
    @return 0 if two numbers are nearly equal, 1 if a > b, -1 if a < b
    */
    public static int compareValue(double a, double b, double tolerance)
    {
        double delta;

        delta = Math.abs(a - b);
        if ( delta < tolerance ) {
            return 0;
        }
        else if ( a > b ) {
            return 1;
        }
        return -1;
    }

    private static boolean planesCoincidentIgnoringOrientation(
        InfinitePlane a, InfinitePlane b, double tolerance)
    {
        double a1, b1, c1, d1;
        double a2, b2, c2, d2;
        double l1, l2;

        if ( a == null || b == null ) {
            return false;
        }

        a1 = a.getA();
        b1 = a.getB();
        c1 = a.getC();
        d1 = a.getD();
        a2 = b.getA();
        b2 = b.getB();
        c2 = b.getC();
        d2 = b.getD();

        l1 = Math.sqrt(a1*a1 + b1*b1 + c1*c1);
        l2 = Math.sqrt(a2*a2 + b2*b2 + c2*c2);
        if ( l1 <= tolerance || l2 <= tolerance ) {
            return false;
        }

        a1 /= l1;
        b1 /= l1;
        c1 /= l1;
        d1 /= l1;
        a2 /= l2;
        b2 /= l2;
        c2 /= l2;
        d2 /= l2;

        boolean sameOrientation =
            Math.abs(a2 - a1) <= tolerance &&
            Math.abs(b2 - b1) <= tolerance &&
            Math.abs(c2 - c1) <= tolerance &&
            Math.abs(d2 - d1) <= tolerance;

        boolean oppositeOrientation =
            Math.abs(a2 + a1) <= tolerance &&
            Math.abs(b2 + b1) <= tolerance &&
            Math.abs(c2 + c1) <= tolerance &&
            Math.abs(d2 + d1) <= tolerance;

        return sameOrientation || oppositeOrientation;
    }

    private static boolean loopsCoincidentFrom(
        _PolyhedralBoundedSolidHalfEdge startA,
        _PolyhedralBoundedSolidHalfEdge startB,
        boolean reverse,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        _PolyhedralBoundedSolidHalfEdge heA;
        _PolyhedralBoundedSolidHalfEdge heB;

        heA = startA;
        heB = startB;
        do {
            if ( !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                heA.startingVertex.position, heB.startingVertex.position,
                numericContext) ) {
                return false;
            }
            heA = heA.next();
            heB = reverse ? heB.previous() : heB.next();
        } while ( heA != startA );

        return true;
    }

    private static boolean loopsCoincident(
        _PolyhedralBoundedSolidLoop a,
        _PolyhedralBoundedSolidLoop b,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;
        _PolyhedralBoundedSolidHalfEdge startA;
        _PolyhedralBoundedSolidHalfEdge scanB;

        if ( a == null || b == null ||
             a.boundaryStartHalfEdge == null || b.boundaryStartHalfEdge == null ) {
            return false;
        }
        if ( a.halfEdgesList.size() != b.halfEdgesList.size() ) {
            return false;
        }

        startA = a.boundaryStartHalfEdge;
        scanB = b.boundaryStartHalfEdge;
        for ( i = 0; i < b.halfEdgesList.size(); i++ ) {
            if ( PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                startA.startingVertex.position, scanB.startingVertex.position,
                numericContext) ) {
                if ( loopsCoincidentFrom(startA, scanB, false, numericContext) ||
                     loopsCoincidentFrom(startA, scanB, true, numericContext) ) {
                    return true;
                }
            }
            scanB = scanB.next();
        }

        return false;
    }

    /**
    Detects the duplicated coplanar-ring configuration that can appear after
    boolean result integration. The reorganization is meant to expose the
    pair of coincident loops that `loopglue` consumes in section
    [MANT1988].12.4.2, as required by the maximal-face cleanup from section
    [MANT1988].15.5 and the finishing stage of program [MANT1988].15.15.
    */
    private boolean reduceCoincidentSimpleFaceOnMultiLoopFace(
        _PolyhedralBoundedSolidFace multiLoopFace,
        _PolyhedralBoundedSolidFace simpleFace,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        ArrayList<_PolyhedralBoundedSolidLoop> coincidentLoops;
        ArrayList<_PolyhedralBoundedSolidLoop> loopsToMove;
        _PolyhedralBoundedSolidLoop simpleLoop;
        _PolyhedralBoundedSolidLoop outerGlueLoop;
        _PolyhedralBoundedSolidLoop duplicateGlueLoop;
        int i;

        if ( multiLoopFace == null || simpleFace == null ||
             multiLoopFace == simpleFace ) {
            return false;
        }
        if ( multiLoopFace.boundariesList.size() < 2 ||
             simpleFace.boundariesList.size() != 1 ) {
            return false;
        }
        if ( multiLoopFace.containingPlane == null ) {
            multiLoopFace.calculatePlane();
        }
        if ( simpleFace.containingPlane == null ) {
            simpleFace.calculatePlane();
        }
        if ( !planesCoincidentIgnoringOrientation(multiLoopFace.containingPlane,
                simpleFace.containingPlane, numericContext.epsilon()) ) {
            return false;
        }

        simpleLoop = simpleFace.boundariesList.get(0);
        coincidentLoops = new ArrayList<_PolyhedralBoundedSolidLoop>();
        for ( i = 0; i < multiLoopFace.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop candidate;
            candidate = multiLoopFace.boundariesList.get(i);
            if ( loopsCoincident(candidate, simpleLoop, numericContext) ) {
                coincidentLoops.add(candidate);
            }
        }

        if ( coincidentLoops.size() < 2 ) {
            return false;
        }

        outerGlueLoop = coincidentLoops.get(0);
        duplicateGlueLoop = coincidentLoops.get(1);
        loopsToMove = new ArrayList<_PolyhedralBoundedSolidLoop>();
        for ( i = 0; i < multiLoopFace.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop candidate;
            candidate = multiLoopFace.boundariesList.get(i);
            if ( candidate != outerGlueLoop && candidate != duplicateGlueLoop ) {
                loopsToMove.add(candidate);
            }
        }

        for ( i = 0; i < loopsToMove.size(); i++ ) {
            if ( !lringmv(loopsToMove.get(i), simpleFace, false) ) {
                return false;
            }
        }

        if ( multiLoopFace.boundariesList.size() != 2 ) {
            return false;
        }

        lringmv(outerGlueLoop, multiLoopFace, true);
        loopGlue(multiLoopFace.id);
        return true;
    }

    /**
    This method get current solid in an "inverted" (geometrical sense) solid.
    Works on half edge data structure by inverting the order of each loop.
    This is an answer to problem [MANT1988].15.6.

    Current implementation does NOT correct face normals. Explicit model
    validation is encouraged after the application of this method.
    */
    public void revert()
    {
        int i;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            polygonsList.get(i).revert();
        }
    }

    /**
    Removes all "inessential" edges of current solid (i.e. edges that
    separates two coplanar faces, or that occurs just in a single face).
    This is an answer to problem [MANT1988].15.2.
    For some operations on solid polyhedra such as boolean set operations,
    it is required that faces of solids be "maximal", i.e. that all coplanar
    neighbor faces have been combined, and all "inessential" edges have been
    removed, as noted on section [MANT1988].15.5. Current implementation also
    performs an additional coplanar-ring reduction so that coincident sheets
    can be eliminated with `loopglue`, consistent with section
    [MANT1988].12.4.2 and the result-finishing strategy of program
    [MANT1988].15.15.
    */
    public void maximizeFaces()
    {
        int i;
        int j;
        int k;
        _PolyhedralBoundedSolidEdge e;
        _PolyhedralBoundedSolidHalfEdge he;
        InfinitePlane a, b;
        Vector3D p0, p1, p2;
        _PolyhedralBoundedSolidHalfEdge heStart;
        boolean restart;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext;

        restart = true;
        while ( restart ) {
            restart = false;
            numericContext = PolyhedralBoundedSolidNumericPolicy.forSolid(this);
            remakeEmanatingHalfedgesReferences();

            //- Eliminate null edges --------------------------------------
            for ( i = 0; i < edgesList.size(); i++ ) {
                e = edgesList.get(i);
                p1 = e.rightHalf.startingVertex.position;
                p2 = e.leftHalf.startingVertex.position;
                if ( PolyhedralBoundedSolidNumericPolicy
                    .pointsCoincident(p1, p2, numericContext) ) {
                    lkev(e.rightHalf, e.leftHalf);
                    restart = true;
                    break;
                }
            }
            if ( restart ) {
                continue;
            }

            //- Join coplanar faces ---------------------------------------
            for ( i = 0; i < edgesList.size(); i++ ) {
                e = edgesList.get(i);
                a = e.rightHalf.parentLoop.parentFace.containingPlane;
                b = e.leftHalf.parentLoop.parentFace.containingPlane;
                if ( e.rightHalf.parentLoop.parentFace ==
                     e.leftHalf.parentLoop.parentFace &&
                     e.rightHalf.parentLoop != e.leftHalf.parentLoop ) {
                    // Case 1: need to remove an edge separating to
                    // different coplanar faces (join faces). Order doesn't
                    // matter.
                    lkemr(e.rightHalf, e.leftHalf);
                    restart = true;
                    break;
                }
                else if ( a != null && b != null &&
                          a.overlapsWith(b, numericContext.epsilon()) &&
                          e.rightHalf.parentLoop != e.leftHalf.parentLoop ) {
                    // Case 2: Not tested!
                    lkef(e.rightHalf, e.leftHalf);
                    restart = true;
                    break;
                }
                else if ( e.rightHalf.parentLoop.parentFace ==
                          e.leftHalf.parentLoop.parentFace &&
                          e.rightHalf.parentLoop == e.leftHalf.parentLoop &&
                          (e.leftHalf == e.rightHalf.next() ||
                           e.rightHalf == e.leftHalf.next()) ) {
                    // Case 3:. Need to remove a dangling edge, with two
                    // halfedges lying over the same face. Do not remove any
                    // face, rather, remove the dangling edge and its dangling
                    // vertex. To test, use object from figure [MANT1988].15.1.
                    // or code from SimpleTestGeometryLibrary method
                    // createTestObjectPairMANT1988_15_1
                    if ( e.leftHalf == e.rightHalf.next() ) {
                        heStart = e.leftHalf;
                    }
                    else {
                        heStart = e.rightHalf;
                    }
                    lkev(heStart, heStart.mirrorHalfEdge());
                    restart = true;
                    break;
                }
                else if ( e.rightHalf.parentLoop.parentFace ==
                          e.leftHalf.parentLoop.parentFace &&
                          e.rightHalf.parentLoop == e.leftHalf.parentLoop &&
                          (e.leftHalf != e.rightHalf.next() &&
                           e.rightHalf != e.leftHalf.next()) ) {
                    // Case 4. Need to remove an edge on a self-intersecting
                    // loop, causing that loop to break on two rings. To test
                    // use "buildCsgTest4" pair on
                    // PolyhedralBoundedSolidModelingTools testsuite program
                    // (union of two L-shaped boxes to form a hollowed brick).
                    // It is important to break the loops in such a way that
                    // bigger loop be the first loop, and smaller loop is the
                    // inner ring.

                    // Estimate the size of semiloop starting at e.leftHalf
                    double minmax[] = getMinMax();
                    Vector3D min = new Vector3D(minmax[3], minmax[4], minmax[5]);
                    Vector3D max = new Vector3D(minmax[0], minmax[1], minmax[2]);
                    Vector3D p;
                    heStart = e.leftHalf;
                    he = heStart;
                    do {
                        he = he.next();
                        if ( he == null ) {
                            // Loop is not closed!
                            break;
                        }
                        p = he.startingVertex.position;
                        if ( p.x() > max.x() ) max = max.withX(p.x());
                        if ( p.y() > max.y() ) max = max.withY(p.y());
                        if ( p.z() > max.z() ) max = max.withZ(p.z());
                        if ( p.x() < min.x() ) min = min.withX(p.x());
                        if ( p.y() < min.y() ) min = min.withY(p.y());
                        if ( p.z() < min.z() ) min = min.withZ(p.z());
                    } while( he != heStart && he != e.rightHalf);
                    double leftDistance = VSDK.vectorDistance(min, max);

                    // Estimate the size of semiloop starting at e.rightHalf
                    min = new Vector3D(minmax[3], minmax[4], minmax[5]);
                    max = new Vector3D(minmax[0], minmax[1], minmax[2]);
                    heStart = e.rightHalf;
                    he = heStart;
                    do {
                        he = he.next();
                        if ( he == null ) {
                            // Loop is not closed!
                            break;
                        }
                        p = he.startingVertex.position;
                        if ( p.x() > max.x() ) max = max.withX(p.x());
                        if ( p.y() > max.y() ) max = max.withY(p.y());
                        if ( p.z() > max.z() ) max = max.withZ(p.z());
                        if ( p.x() < min.x() ) min = min.withX(p.x());
                        if ( p.y() < min.y() ) min = min.withY(p.y());
                        if ( p.z() < min.z() ) min = min.withZ(p.z());
                    } while( he != heStart && he != e.leftHalf);
                    double rightDistance = VSDK.vectorDistance(min, max);

                    // Determine outer loop acording to major extent
                    _PolyhedralBoundedSolidHalfEdge heOuter;
                    _PolyhedralBoundedSolidHalfEdge heInner;

                    if ( leftDistance > rightDistance ) {
                        heOuter = e.leftHalf;
                        heInner = e.rightHalf;
                    }
                    else {
                        heOuter = e.rightHalf;
                        heInner = e.leftHalf;
                    }
                    lkemr(heInner, heOuter);
                    restart = true;
                    break;
                }
            }
            if ( restart ) {
                continue;
            }

            //- Merge coplanar overlapping faces when one lies entirely over
            //- another that already carries rings. This completes the
            //- "maximal face" reduction expected by [MANT1988].15.5.
            for ( i = 0; i < polygonsList.size() && !restart; i++ ) {
                _PolyhedralBoundedSolidFace faceA = polygonsList.get(i);
                if ( faceA.containingPlane == null ) {
                    faceA.calculatePlane();
                }
                for ( j = i + 1; j < polygonsList.size(); j++ ) {
                    _PolyhedralBoundedSolidFace faceB = polygonsList.get(j);
                    if ( faceB.containingPlane == null ) {
                        faceB.calculatePlane();
                    }
                    if ( faceA.containingPlane == null ||
                         faceB.containingPlane == null ) {
                        continue;
                    }

                    if ( reduceCoincidentSimpleFaceOnMultiLoopFace(faceA, faceB,
                             numericContext) ||
                         reduceCoincidentSimpleFaceOnMultiLoopFace(faceB, faceA,
                             numericContext) ) {
                        restart = true;
                        break;
                    }
                }
            }
            if ( restart ) {
                continue;
            }

            //- Eliminate vertices between colinear edges -----------------
            _PolyhedralBoundedSolidHalfEdge heMirror;
            _PolyhedralBoundedSolidVertex v;
            int nedges;

            for ( i = 0; i < verticesList.size(); i++ ) {
                v = verticesList.get(i);
                heStart = v.emanatingHalfEdge;
                if ( heStart == null ) {
                    continue;
                }
                he = heStart;
                nedges = 0;
                j = 0;
                do {
                    nedges++;
                    if ( nedges > 2 ) break;

                    if ( he == null ) {
                        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "maximizeFaces",
                            "Inconsistent model! Null HalfEdge. Check.");
                    }

                    heMirror = he.mirrorHalfEdge();
                    if ( heMirror == null ) {
                        /*
                        VSDK.reportMessage(this, VSDK.WARNING, "maximizeFaces",
                            "Inconsistent model! halfedge " + he.id +
                            " of face " + he.parentLoop.parentFace.id +
                            " at position " + j + " emanating from \nvertex " +
                            v.id +
                            " without mirror halfedge. Check."
                        );
                        */
                        nedges = 0;
                        continue;
                    }

                    he = heMirror.next();
                    if ( he == null ) {
                        VSDK.reportMessage(this, VSDK.FATAL_ERROR, "maximizeFaces",
                            "Inconsistent model! HalfEdge without next. Check.");
                    }
                    j++;
                } while ( he != heStart );

                if ( nedges == 2 ) {
                    p0 = heStart.startingVertex.position;
                    p1 = heStart.next().startingVertex.position.subtract(p0);
                    p2 = heStart.previous().startingVertex.position.subtract(p0);
                    if ( PolyhedralBoundedSolidNumericPolicy
                        .vectorsColinear(p1, p2, numericContext) ) {
                        if ( p1.dotProduct(p2) < 0 ) {
                            lkev(heStart, heStart.mirrorHalfEdge());
                            restart = true;
                            break;
                        }
                    }
                }
            }
        }

        //- Eliminate rings with a single vertex --------------------------
        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            _PolyhedralBoundedSolidHalfEdge outerloophe;

            outerloophe = face.boundariesList.get(0).boundaryStartHalfEdge;

            for ( j = 1; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he.parentEdge == null ||
                     loop.halfEdgesList.size() == 1 ) {
                    // Kill ring
                    _PolyhedralBoundedSolidVertex vtodelete;
                    vtodelete = he.startingVertex;
                    lmekr(outerloophe, he);

                    // Kill edge and vertex
                    _PolyhedralBoundedSolidLoop newloop;
                    newloop = outerloophe.parentLoop;

                    _PolyhedralBoundedSolidHalfEdge hej;
                    hej = outerloophe;
                    do {
                        hej = hej.next();
                        if ( hej == null ) {
                            // Loop is not closed!
                            break;
                        }
                        if ( hej.startingVertex == vtodelete) {
                            lkev(hej, hej.mirrorHalfEdge());
                            break;
                        }
                    } while( hej != outerloophe );
                }
            }
        }
        // Here should be a code searching for faces inside faces ...
        remakeEmanatingHalfedgesReferences();
    }

    /**
    Modifies ids for current solid's vertices, edges, faces and halfedges to
    make them consecutive from 1. Note that ids does not impact current
    solid geometry or topology, as they are use only for debugging and
    furter construction / modification support.
    User of this class should keep in mind id changes when using this method.
    */
    public void compactIds()
    {
        int i, j;

        for ( i = 0; i < verticesList.size(); i++ ) {
            verticesList.get(i).id = i+1;
        }
        maxVertexId = i;
        for ( i = 0; i < edgesList.size(); i++ ) {
            edgesList.get(i).id = i+1;
        }

        int k = 1;
        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            face.id = i+1;
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                loop = face.boundariesList.get(j);

                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
                }
                heStart = he;
                do {
                    he.id = k;
                    k++;
                    he = he.next();
                    if ( he == null ) {
                        break;
                    }
                } while( he != heStart );
            }
        }
        maxFaceId = i;
    }

    @Override
    public String toString()
    {
        String msg = "";
        int i, j;

        msg += "= POLYHEDRAL BOUNDED SOLID STRUCTURE ==========================================\n";
        msg += "Solid with " + verticesList.size() + " vertices:\n";
        for ( i = 0; i < verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = verticesList.get(i);
            msg += "  - " + v + "\n";
        }

        msg += "Solid with " + edgesList.size() + " edges:\n";
        for ( i = 0; i < edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = edgesList.get(i);
            msg += "  - " + e + "\n";
        }
        msg += "Solid with " + polygonsList.size() + " faces:\n";

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            msg += "  - " + face + "\n";
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                msg += "    . Loop " + j + ", with halfedges: \n";
                loop = face.boundariesList.get(j);


                msg += "      | HeID  | StartVertex | End Vertex | nccw He | pccw He | parentEdge | mirror He | neighbor face\n";
                msg += "      +-------+-------------+------------+---------+---------+------------+-----------+-------------+\n";

                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    msg += "<Loop without starting halfedge!>\n";
                    continue;
                }
                heStart = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        msg += "      |  - (not closed loop)\n";
                        break;
                    }

                    msg += "      | " +
                        intPreSpaces(he.id, 4) + 
                        ((he == loop.boundaryStartHalfEdge)?"*":" ") +
                        " | " +
                        intPreSpaces(he.startingVertex.id, 11) + " | " +
                        intPreSpaces(he.next().startingVertex.id, 10) + " | " +
                        intPreSpaces(he.next().id, 7) + " | " +
                        intPreSpaces(he.previous().id, 7) + " | ";
                    msg += (he.parentEdge!=null)?
                        intPreSpaces(he.parentEdge.id, 10):"    <null>";
                    msg += " | ";
                    if ( he.mirrorHalfEdge() != null ) {
                        msg += intPreSpaces(he.mirrorHalfEdge().id, 9) + " | " +
                            intPreSpaces(he.mirrorHalfEdge().parentLoop.parentFace.id, 11) + " | ";
                    }
                    else {
                        msg += " No Mirror Half Edge!   | ";
                    }

                    msg += "\n";

                } while( he != heStart );
            }
        }
        msg += "= END OF POLYHEDRAL BOUNDED SOLID STRUCTURE ===================================\n";
        return msg;
    }

    @Override
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        return this;
    }

}
