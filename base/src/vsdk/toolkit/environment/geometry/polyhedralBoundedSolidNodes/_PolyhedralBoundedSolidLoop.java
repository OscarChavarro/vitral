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

package vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes;

import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.FundamentalEntity;

/**
As noted in [MANT1988].10.2.1, a `_PolyhedralBoundedSolidLoop` describes
one connected boundary inside a `_PolyhedralBoundedSolidFace`.

Note that in the sake of simplify and eficiency current programming 
implementation of this class exhibit public access attributes. It is important
to note that those attributes will only be accessed directly from related 
classes in the same package
(vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidLoop extends FundamentalEntity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    /// Defined as presented in [MANT1988].10.2.1
    public _PolyhedralBoundedSolidFace parentFace;

    /// As noted in [MANT1988].10.2.3, consider that there is a special
    /// case for empty loops. Note that this case doesn't affect this
    /// reference.
    public _PolyhedralBoundedSolidHalfEdge boundaryStartHalfEdge;

    public CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge> halfEdgesList;

    //=================================================================
    public _PolyhedralBoundedSolidLoop(_PolyhedralBoundedSolidFace parent)
    {
        parentFace = parent;
        parentFace.boundariesList.add(this);
        halfEdgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidHalfEdge>();
    }

    public void unlistHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        halfEdgesList.locateWindowAtElem(he);
        halfEdgesList.removeElemAtWindow();
        boundaryStartHalfEdge = halfEdgesList.get(0);
    }

    /** Locates a half edge that goes from vertex with id `a` to vertex with
    id `b`.  Returns null if no such half edge exists in this loop. */
    public _PolyhedralBoundedSolidHalfEdge halfEdgeVertices(int a, int b)
    {
        _PolyhedralBoundedSolidHalfEdge he, oldhe;
        he = boundaryStartHalfEdge;
        do {
            oldhe = he;
            he = he.next();
            if ( he == null ) {
                // Loop is not closed!
                break;
            }

            if ( oldhe.startingVertex.id == a && he.startingVertex.id == b) {
                return oldhe;
            }

        } while( he != boundaryStartHalfEdge );
        return null;
    }

    /** Locates a half edge that goes from vertex with id `a` to vertex with
    id `b`.  Returns null if no such half edge exists in this loop. */
    public _PolyhedralBoundedSolidHalfEdge firstHalfEdgeAtVertex(int a)
    {
        _PolyhedralBoundedSolidHalfEdge he, oldhe;
        he = boundaryStartHalfEdge;
        do {
            oldhe = he;
            he = he.next();
            if ( he == null ) {
                // Loop is not closed!
                break;
            }

            if ( oldhe.startingVertex.id == a ) {
                return oldhe;
            }

        } while( he != boundaryStartHalfEdge );
        return null;
    }

    /**
    Vitral SDK's current implementation of original `delhe` utility function
    presented at program [MANT1988].11.4. Note that current implementation
    is quite diferent from the original from [MANT1988]. This could lead to
    subtle problems! This method's functionality should be better understood!
    */
    public void delhe(_PolyhedralBoundedSolidHalfEdge he)
    {
        halfEdgesList.locateWindowAtElem(he);
        halfEdgesList.removeElemAtWindow();
        boundaryStartHalfEdge = halfEdgesList.get(0);
    }

    public String toString()
    {
        String msg;

        msg = "Loop, parent face " + parentFace.id;

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
