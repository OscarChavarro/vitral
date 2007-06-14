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

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;

/**
As noted in [MANT1988].10.2.1, a `_PolyhedralBoundedSolidHalfEdge` describes
one line segment inside a `_PolyhedralBoundedSolidLoop`. It has only a
reference to a vertex in a `PolyhedralBoundedSolid`.

Note that in the sake of simplify and eficiency current programming 
implementation this class exhibit public access attributes. It is important
to note that those attributes will only be accessed directly from related 
classes in the same package
(vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes) and
from methods in the `PolyhedralBoundedSolid` class, and that they should
not be used from outer classes.
*/
public class _PolyhedralBoundedSolidHalfEdge extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    /// Defined as presented in [MANT1988].10.2.1
    public _PolyhedralBoundedSolidLoop parentLoop;

    /// Defined as presented in [MANT1988].10.2.2. Note that as commented in
    /// [MANT1988].10.2.2, this reference can be `null` in the special case
    /// of empty loops.
    public _PolyhedralBoundedSolidEdge parentEdge;

    /// Defined as presented in [MANT1988].10.2.1
    public _PolyhedralBoundedSolidVertex startingVertex;

    //=================================================================
    public _PolyhedralBoundedSolidHalfEdge(_PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidLoop parentLoop,
        PolyhedralBoundedSolid parentSolid)
    {
    startingVertex = v;
    this.parentLoop = parentLoop;
        parentSolid.halfEdgesList.add(this);
    parentEdge = null;
    }

    /**
    Locates the previous halfedge in current list
    */
    public _PolyhedralBoundedSolidHalfEdge previous()
    {
        parentLoop.parentFace.parentSolid.halfEdgesList.locateWindowAtElem(this);
        parentLoop.parentFace.parentSolid.halfEdgesList.previous();
    return parentLoop.parentFace.parentSolid.halfEdgesList.getWindow();
    }

    /**
    Locates the previous halfedge in current list
    */
    public _PolyhedralBoundedSolidHalfEdge next()
    {
        parentLoop.parentFace.parentSolid.halfEdgesList.locateWindowAtElem(this);
        parentLoop.parentFace.parentSolid.halfEdgesList.next();
    return parentLoop.parentFace.parentSolid.halfEdgesList.getWindow();
    }

    /**
    Given current half edge, this method returns complementary half edge
    with respect to parent edge. Note that this code corresponds to macro
    `mate(he)`, defined in program 10.2, from [MANT1988].10.3.
    */
    public _PolyhedralBoundedSolidHalfEdge
    mirrorHalfEdge()
    {
        if ( parentEdge == null ) return null;

        if ( this == parentEdge.rightHalf ) {
            return parentEdge.leftHalf;
        }
        return parentEdge.rightHalf;
    }

    public String toString()
    {
        String msg;
        msg = "HalfEdge: parent loop id " + parentLoop.id + ", ";
        if ( parentEdge == null ) {
            msg = msg + "without parent edge. ";
      }
      else {
            msg = msg + "with parent edge. ";
    }
    msg = msg + "Starting at " + startingVertex;
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
