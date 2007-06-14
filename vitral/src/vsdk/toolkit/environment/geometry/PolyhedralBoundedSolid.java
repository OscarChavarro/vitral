//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 18 2006 - Oscar Chavarro: Original base version              =
//= - January 3 2007 - Oscar Chavarro: First phase implementation           =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
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
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge> halfEdgesList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge> edgesList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex> verticesList;

    //=================================================================
    public PolyhedralBoundedSolid(Vector3D firstPoint)
    {
        polygonsList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidFace>();
        halfEdgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge>();
        edgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>();
        verticesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>();
        mvfs(new Vector3D(firstPoint));
    }

    public PolyhedralBoundedSolid(double firstPointX, double firstPointY,
                                  double firstPointZ)
    {
        polygonsList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidFace>();
        halfEdgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge>();
        edgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>();
        verticesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>();
        Vector3D firstPoint;
        firstPoint = new Vector3D(firstPointX, firstPointY, firstPointZ);
        mvfs(firstPoint);
    }

    //= SUPPORT MACROS FOR BASIC DATASTRUCTURE MANIPULATION ===========

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

        }
        he.parentEdge = e;
        he.startingVertex = v;

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
    private void mvfs(Vector3D p)
    {
        _PolyhedralBoundedSolidFace newFace;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidHalfEdge newHalfEdge;
        _PolyhedralBoundedSolidVertex newVertex;

        newFace = new _PolyhedralBoundedSolidFace(this);
        newLoop = new _PolyhedralBoundedSolidLoop(newFace);
        newVertex = new _PolyhedralBoundedSolidVertex(this, p);
        newHalfEdge =
            new _PolyhedralBoundedSolidHalfEdge(newVertex, newLoop, this);
        newLoop.boundaryStartHalfEdge = newHalfEdge;
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
                     Vector3D p)
    {
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidVertex newVertex;
        _PolyhedralBoundedSolidEdge newEdge;

        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);
        newVertex = new _PolyhedralBoundedSolidVertex(he1.parentLoop.parentFace.parentSolid, p);

        he = he1;
        while ( he != he2 ) {
            he.startingVertex = newVertex;
            he = he.mirrorHalfEdge().next();
        }

    _PolyhedralBoundedSolidVertex oldVertex = he2.startingVertex;
        addhe(newEdge, oldVertex, he2, PLUS);
        addhe(newEdge, newVertex, he1, MINUS);

        newVertex.emanatingHalfEdge = he2.previous();
        he2.startingVertex.emanatingHalfEdge = he2;
    }

    /**
    lmef: LowlevelMakeEdgeFace (face splitting operator)
    Operator lmef adds a new edge between halfedges `he1` and `he2`,
    and "splits" their common face into two faces such that `he1` will
    occur in the new face `f`, and `he2` remains in the old face. The
    new edge is oriented from he1.startingVertex to he2.startingVertex.
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
        _PolyhedralBoundedSolidHalfEdge he2)
    {
        _PolyhedralBoundedSolidFace newFace;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidEdge newEdge;
        _PolyhedralBoundedSolidHalfEdge he, nhe1, nhe2, temp;

        newFace = new _PolyhedralBoundedSolidFace(he1.parentLoop.parentFace.parentSolid);
        newLoop = new _PolyhedralBoundedSolidLoop(newFace);
        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);

        he = he1;
        while ( he != he2 ) {
            he.parentLoop = newLoop;
            he = he.next();
        }

        nhe1 = addhe(newEdge, he2.startingVertex, he1, MINUS);
        nhe2 = addhe(newEdge, he1.startingVertex, he2, PLUS);

        halfEdgesList.swapElements(nhe1, nhe2);

        newLoop.boundaryStartHalfEdge = nhe1;
        he2.parentLoop.boundaryStartHalfEdge = nhe2;

        return newFace;
    }

    //=================================================================

    public boolean
    doIntersection(Ray inout_rayo) {
        VSDK.reportMessage(this, VSDK.WARNING, "doIntersection",
            "Method not implemented");
        return false;
    }

    public void
    doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        VSDK.reportMessage(this, VSDK.WARNING, "doExtraInformation",
            "Method not implemented");
    }

    public double[] getMinMax()
    {
        double minmax[] = new double[6];
        for ( int i = 0; i < 3; i++ ) {
            minmax[i] = -1.0;
        }
        for ( int i = 3; i < 6; i++ ) {
            minmax[i] = 1.0;
        }

        VSDK.reportMessage(this, VSDK.WARNING, "getMinMax",
            "Method not implemented");

        return minmax;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
