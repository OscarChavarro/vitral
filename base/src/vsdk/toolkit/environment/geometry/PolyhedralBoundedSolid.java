//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 18 2006 - Oscar Chavarro: Original base version              =
//= - January 3 2007 - Oscar Chavarro: First phase implementation           =
//= - April 21 2007 - Oscar Chavarro: First working version with basics     =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import java.text.DecimalFormat;
import java.text.FieldPosition;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

/**
This class encapsulates a polyhedral boundary representation for 2-manifold
solids, as presented in [MANT1988]. As noted in [MANT1988].10.2.1:
The `PolyhedralBoundedSolid` class uses a five-level hierarchic data structure,
consisting of:
  - PolyhedralBoundedSolid
  - _PolyhedralBoundedSolidFace
  - _PolyhedralBoundedSolidLoop
  - _PolyhedralBoundedSolidHalfEdge (and _PolyhedralBoundedSolidEdge)
  - _PolyhedralBoundedSolidVertex
Current class forms the root element that gives access to faces, edges and
vertices of the model through agregations in CircularDoubleLinkedList's.
*/
public class PolyhedralBoundedSolid extends Solid {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    public static final int PLUS = 1;
    public static final int MINUS = 0;

    //= Main boundary representation solid data structure =============
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidFace> polygonsList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge> edgesList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex> verticesList;
    // Auxiliary data structures for storage of parcial results and 
    // preprocessing
    private double[] minMax;
    private int maxVertexId;
    private int maxFaceId;
    private boolean modelIsValid;
    private GeometryIntersectionInformation lastInfo;

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
        lastInfo = new GeometryIntersectionInformation();
    }

    //= SUPPORT MACROS FOR BASIC DATASTRUCTURE MANIPULATION ===========

    /**
    Find the face identified with `id`. Returns null if face not found,
    or current founded face otherwise.
    Build based over function `fface` in program [MANT1988].11.9.
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

    public Vector3D getVertexPosition(int vertexid)
    {
        int i;

        for ( i = 0; i < verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = verticesList.get(i);
            if ( v.id == vertexid ) {
                return v.position;
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

        if ( where.parentEdge == null ) {
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
            "Calling with empty halfedges!");
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

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge heMirror;
        heMirror = he1.mirrorHalfEdge();
        if ( he1 == heMirror ) {
            heMirror.parentLoop.unlistHalfEdge(heMirror.next());
        }
        else {
            heMirror.parentLoop.unlistHalfEdge(heMirror);
        }

        edgesList.locateWindowAtElem(he1.parentEdge);
        edgesList.removeElemAtWindow();
        verticesList.locateWindowAtElem(he1.startingVertex);
        verticesList.removeElemAtWindow();
        he1.parentLoop.unlistHalfEdge(he1);

        if ( verticesList.size() == 1 ) {
            verticesList.get(0).emanatingHalfEdge = null;
            he2.parentEdge = null;
        }

        //-----------------------------------------------------------------
        // Brute force search for all edges starting at vertex to be deleted!
        int i;
        _PolyhedralBoundedSolidEdge e;

        for ( i = 0; i < edgesList.size(); i++ ) {
            e = edgesList.get(i);
            if ( e.leftHalf.startingVertex == he1.startingVertex ) {
                e.leftHalf.startingVertex = he2.startingVertex;
            }
            if ( e.rightHalf.startingVertex == he1.startingVertex ) {
                e.rightHalf.startingVertex = he2.startingVertex;
            }
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
        while ( he != he2 ) {
            migratedHalfEdges.add(he);
            he = he.next();
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
    */
    public void lkemr(
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge he3 = null, he4;
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
            newLoop.halfEdgesList.get(0).startingVertex.emanatingHalfEdge = null;
        }
        if ( oldLoop.halfEdgesList.size() <= 1 ) {
            oldLoop.boundaryStartHalfEdge.parentEdge = null;
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
    Note that this method is a partial solution to problem [MANT1988].11.5, 
    and follows the functional definition of section [MANT1988].11.5.2.
    This method should be shecket to see if it is managing only the
    "same shell" case, or if it is done.
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
    Note that this method is a partial solution to problem [MANT1988].11.5, 
    and follows the functional definition of section [MANT1988].11.5.2.
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
    Method `lringmv` moves the loop `l` from its parent face to face `tofac`.
    If `setAsOuterLoop` is false, `l` becomes an "inner" loop, and otherwise
    the outer loop of `tofac`. If `l.parentFace` == `tofac`, `lringmv` has no
    effect except for assigning the inner vs. outer information. Note that
    `lringmv` is an addendum to `lmef`, and not an Euler operator.
    This method follows the functional definition of section [MANT1988].11.5.2.
    */
    public void lringmv(_PolyhedralBoundedSolidLoop l, _PolyhedralBoundedSolidFace tofac, boolean setAsOuterLoop)
    {
        System.out.println("laringmv not implemented!");
        System.exit(1);
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
    Current implementation was builded as an answer to exercise
    [MANT1988].11.4, and follows the signature from section
    [MANT1988].11.5.2.
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

        for ( i = 0; i < migratedHalfEdges.size(); i++ ) {
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
    @return true if operation succeded, false otherway
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
        if ( he1 == null ) {
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
        if ( he1 == null ) {
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
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Edge " + v3 + " - " + v3 + " not found in face " + f2 + ".");
            return false;
        }
        lmef(he1, he2, newfaceid);
        return true;
    }

    /**
    kemr: (High level version) KillEdgeMakeRing (loop splitting operation).
    Executes a `lkemr` in halfedges v1-v2, v3-v4 in respective faces `f1`
    and `f2`.
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
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Edge " + v3 + " - " + v3 + " not found in face " + f2 + ".");
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
    This method should be shecket to see if it is managing only the
    "same shell" case, or if it is done.
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
    */
    public boolean doIntersection(Ray inOutRay) {
        int i;
        boolean intersection; // true if intersection founded
        double min_t;         // Shortest distance founded so far

        // Initialization values for search algorithm
        min_t = Double.MAX_VALUE;
        intersection = false;
        GeometryIntersectionInformation Info;
        Info = new GeometryIntersectionInformation();
        Vector3D p;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            Ray ray = new Ray(inOutRay);
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            if ( face.containingPlane.doIntersection(ray) ) {
                face.containingPlane.doExtraInformation(ray, 0.0, Info);
                if ( ray.t < min_t ) {
                    ray.direction.normalize();
                    p = ray.origin.add(ray.direction.multiply(ray.t));
                    if ( face.testPointInside(p) <= 0 ) {
                        min_t = ray.t;
                        // Stores standard doIntersection operation information
                        lastInfo.clone(Info);
                        inOutRay.t = ray.t;
                        intersection = true;
                    }
                }
            }
        }

        return intersection;
    }

    public void doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        outData.clone(lastInfo);
    }

    /** Needed for supplying the Geometry.getMinMax operation */
    private void calculateMinMaxPositions() {
        if ( minMax == null ) {
            minMax = new double[6];

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;
            double maxZ = Double.MIN_VALUE;
            int i;

            for ( i = 0; i < verticesList.size(); i++ ) {
                _PolyhedralBoundedSolidVertex v;
                v = verticesList.get(i);
                double x = v.position.x;
                double y = v.position.y;
                double z = v.position.z;

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
    */
    public double[] getMinMax() {
        if ( minMax == null ) {
            calculateMinMaxPositions();
        }
        return minMax;
    }

    /**
    Checks that:
    1. There are at least 3 points in set (no no-surface case)
    2. There are at least 2 different points in the set (no all-equal
       bad case)
    3. There are at least a third point, no colinear with the first 2
    4. All points in this set lies within a small tolerance over the
       surface of the plane
    If all four steps succeed, this method returns true. Otherwise returns
    false.
    */
    private boolean validateFacePointsAreCoplanar(ArrayList<Vector3D> points)
    {
        //- 1. Test no-surface case ---------------------------------------
        if ( points.size() < 3 ) {
            System.out.println("Reason 1");
            return false;
	}

        //- 2. Test all-equal case ----------------------------------------
        Vector3D p0, p1, p2;
        p0 = points.get(0);
        p1 = null;
        boolean test = false;

        int i;
        for ( i = 1; i < points.size(); i++ ) {
            p1 = points.get(i);
            if ( VSDK.vectorDistance(p0, p1) > 10 * VSDK.EPSILON ) {
                test = true;
                break;
            }
        }
        if ( !test ) {
            System.out.println("Reason 2");
            return false;
	}

        //- 3. Test co-linear case ----------------------------------------
        Vector3D a, b, n;
        double aDotB;
        InfinitePlane facePlane = null;
        int j, k;

        for ( i = 0; i < points.size(); i++ ) {
            for ( j = 0; j < points.size(); j++ ) {
                for ( k = 0; k < points.size(); k++ ) {
                    if ( i == j || i == k || j == k ) {
                        continue;
		    }
                    p0 = points.get(i);
                    p1 = points.get(j);
                    p2 = points.get(k);
                    if ( VSDK.vectorDistance(p0, p2) > 10 * VSDK.EPSILON &&
                         VSDK.vectorDistance(p1, p2) > 10 * VSDK.EPSILON ) {
                        a = p2.substract(p0);
                        b = p1.substract(p0);
                        a.normalize();
                        b.normalize();
                        aDotB = Math.abs(a.dotProduct(b));
                        if ( aDotB < 1 - 2*VSDK.EPSILON ) {
                            n = a.crossProduct(b);
                            n.normalize();
                            facePlane = new InfinitePlane(n, p0);
                        }
                        break;
                    }
                }
	    }
    	}
    
        if ( facePlane == null ) {
            System.out.println("Reason 3");
            return false;
        }

        //- 4. Check if all points are near face plane --------------------
        for ( i = 1; i < points.size(); i++ ) {
            p0 = points.get(i);
            if ( facePlane.doContainmentTest(p0, VSDK.EPSILON) != LIMIT ) {
                return false;
            }
        }

        //-----------------------------------------------------------------
        return true;
    }

    /**
    Returns true if the model was validated (using `validateModel` method)
    and validation succeed after any geometrical or topological operation.
    */
    public boolean isValid()
    {
        return modelIsValid;
    }

    private ArrayList<Vector3D> extractPointsFromFace(_PolyhedralBoundedSolidFace face)
    {
        boolean test = true;
        ArrayList<Vector3D> points;
        points = new ArrayList<Vector3D>();
        int j;

        for ( j = 0; j < face.boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop loop;
            _PolyhedralBoundedSolidHalfEdge he, heStart;

            loop = face.boundariesList.get(j);
            he = loop.boundaryStartHalfEdge;
            if ( he == null ) {
                //msg += "  - Loop without starting halfedge\n";
                test = false;
                break;
            }
            heStart = he;
            do {
                he = he.next();
                if ( he == null ) {
                    // Loop is not closed!
                    //msg += "  - Not closed loop\n";
                    test = false;
                    break;
                }
                points.add(he.startingVertex.position);
            } while( he != heStart );
        }

        if ( !test ) {
            return null;
        }
        return points;
    }

    /**
    Given a face inside current solid, this method returns true if face is
    planar, false otherwise.
    */
    public boolean validateFaceIsPlanar(_PolyhedralBoundedSolidFace face)
    {
        ArrayList<Vector3D> points = extractPointsFromFace(face);

        if ( (points != null) && validateFacePointsAreCoplanar(points) ) {
            return true;
        }
        return false;
    }

    /**
    After section [MANT1988].12.4.2 and program [MANT1988].12.9.
    */
    public void loopGlue(int faceid)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidFace face;

        face = findFace(faceid);

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge h1, h2, h1next;

        h1 = face.boundariesList.get(0).boundaryStartHalfEdge;
        h2 = face.boundariesList.get(1).boundaryStartHalfEdge;

        while ( !h1.vertexPositionMatch(h2, 10*VSDK.EPSILON) ) {
            h2 = h2.next();
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
    }

    /**
    Given `this` and `other` solid, this method erases the `other` solid
    while appending its parts to current one. This method follows section
    [MANT1988].12.4.1 and program [MANT1988].12.8.
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
    This method runs a set of validity tests to check te integrity of the
    data structure. If all of the tests goes well, this method returns true.
    If any of the test fails, this method return false.

    Current methods (already implemented):
      - All loops have an starting halfedge
      - All loops are closed
      - All faces contains its corresponding containing plane equation

    Current methods (not implemented yet):
      - All faces are co-planar
      - For faces with more than one loop, the loops are not intersecting
        nor self-intersecting
      - Topology test: euler formula gives a closed representation
        (no holes, no missing faces and no non-manifold solids like
        Klain bottles)
      - Geometric integrity: there are no intersecting faces
    */
    public boolean validateModel()
    {
        int i;
        String msg = "";
        boolean test = true;

        modelIsValid = false;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);

            if ( validateFaceIsPlanar(face) ) {
                face.calculatePlane();
            }
            else {
                msg += "  - Face [" + face.id + "] is not coplanar\n";
                test = false;
            }
        }

        if ( test ) {
            modelIsValid = true;
        }
        else {
            VSDK.reportMessage(this, VSDK.WARNING, "validateModel",
                "Validation test failed!:\n" + msg);
        }

        return test;
    }

    //= TEXTUAL QUERY OPERATIONS ======================================

    private String intPreSpaces(int val, int fieldSize)
    {
        DecimalFormat f = new DecimalFormat("0");
        String cad;

        cad = f.format(val, new StringBuffer(""), new FieldPosition(0)).toString();

        int remain = fieldSize - cad.length();

        for ( ; remain > 0; remain-- ) {
            cad = " " + cad;
        }
        return cad;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.computeQuantitativeInvisibility.
    */
    public int computeQuantitativeInvisibility(Vector3D origin, Vector3D p)
    {
        int qi = 0;
        int i;
        Vector3D d = p.substract(origin);
        Vector3D pi;
        double t0 = d.length();
        d.normalize();

        Ray ray = new Ray(origin, d);
        GeometryIntersectionInformation info;
        info = new GeometryIntersectionInformation();

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            if ( face.containingPlane.doIntersection(ray) ) {
                if ( ray.t < t0-VSDK.EPSILON ) {
                    ray.direction.normalize();
                    pi = ray.origin.add(ray.direction.multiply(ray.t));
                    if ( face.testPointInside(pi) < 0 ) {
                        face.containingPlane.doExtraInformation(ray, 0.0, info);
                        if ( info.n.dotProduct(d) < 0.0 ) {
                            qi ++;
                        }
                    }
                }
            }
        }

        return qi;
    }

    /**
    Utility routine used to compare floating values inside the boundary
    representation winged edge data structure, following procedure `comp`
    from program [MANT1988].13.2.
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

    /**
    Removes all "inessential" edges of current solid (i.e. edges that
    separates two coplanar faces, or that occurs just in a single face).
    This is an answer to problem [MANT1988].15.2.
    For some operations on solid polyhedra such as boolean set operations,
    it is required that faces of solids be "maximal", i.e. that all coplanar
    neighbor faces have been combined, and all "inessential" edges have been
    removed, as noted on section [MANT1988].15.5.
    @todo current method does not fix faces that lies entirely over other
    faces.
    */
    public void maximizeFaces()
    {
        int i;
        _PolyhedralBoundedSolidEdge e;
        InfinitePlane a, b;

	for ( i = 0; i < edgesList.size(); i++ ) {
	    e = edgesList.get(i);
            a = e.rightHalf.parentLoop.parentFace.containingPlane;
            b = e.leftHalf.parentLoop.parentFace.containingPlane;
            if ( e.rightHalf.parentLoop.parentFace ==
                 e.leftHalf.parentLoop.parentFace ) {
		System.out.println("PolyhedralBoundedSolid - maximizeFaces - untested case, please verify!");
		lkev(e.rightHalf, e.leftHalf);
	    }
	    else if ( a.coplanarWith(b, VSDK.EPSILON) ) {
                lkef(e.rightHalf, e.leftHalf);
                i = 0;
	    }
	}

        // Here should be a code searching for faces inside faces ...
    }

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


                msg += "      | HeID | StartVertex | End Vertex | nccw He | pccwHe | parentEdge\n";
                msg += "      +------+-------------+------------+---------+--------+-----------\n";

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

                    /*
                    System.out.printf("      %4d | %11d | %10d | %7d | %6d | %10d",
                        he.id, he.startingVertex.id,
                        he.next().startingVertex.id,
                        he.next().id, he.previous().id, he.parentEdge.id);
                    */
                    msg += "      | " +
                        intPreSpaces(he.id, 4) + " | " +
                        intPreSpaces(he.startingVertex.id, 11) + " | " +
                        intPreSpaces(he.next().startingVertex.id, 10) + " | " +
                        intPreSpaces(he.next().id, 7) + " | " +
                        intPreSpaces(he.previous().id, 6) + " | ";
                    msg += (he.parentEdge!=null)?
                        intPreSpaces(he.parentEdge.id, 10):"    <null>";
                    msg += "\n";

                } while( he != heStart );
            }
        }
        msg += "= END OF POLYHEDRAL BOUNDED SOLID STRUCTURE ===================================\n";
        return msg;
    }

    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        return this;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
